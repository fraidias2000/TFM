package es.hablabar.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Prueba de extremo a extremo de la entrega en tiempo real (RF-08, RF-11, RNF-01,
 * RNF-05): arranca el servidor en un puerto real, conecta clientes WebSocket y
 * envía mensajes mediante la API REST.
 *
 * <p>A diferencia del resto de pruebas, los datos se confirman en la BD (la entrega
 * se produce tras confirmar la transacción), así que se eliminan al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MensajesWebSocketTest {

    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;
    private static final String PREFIJO_CORREO = "test.ws.";

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<StompSession> sesiones = new ArrayList<>();
    private WebSocketStompClient stomp;

    @BeforeEach
    void setUp() {
        stomp = new WebSocketStompClient(new StandardWebSocketClient());
    }

    @AfterEach
    void limpiar() {
        sesiones.forEach(sesion -> {
            if (sesion.isConnected()) {
                sesion.disconnect();
            }
        });
        stomp.stop();
        // Borrar los chats elimina en cascada sus participaciones y mensajes.
        String usuariosDePrueba = "SELECT id FROM usuarios WHERE correo LIKE '" + PREFIJO_CORREO + "%'";
        jdbc.update("DELETE FROM chats WHERE creador_id IN (" + usuariosDePrueba + ")");
        jdbc.update("DELETE FROM participaciones WHERE usuario_id IN (" + usuariosDePrueba + ")");
        jdbc.update("DELETE FROM usuarios WHERE correo LIKE '" + PREFIJO_CORREO + "%'");
    }

    @Test
    @DisplayName("CP-MSG-11: los participantes conectados reciben el mensaje en menos de 2 s; quien no participa o ha salido, no")
    void entregaEnTiempoReal() throws Exception {
        String ana = usuario("Ana");
        String bea = usuario("Bea");
        String carlos = usuario("Carlos");
        String chatId = crearChat(ana);
        entrar(bea, chatId);
        entrar(carlos, chatId);
        salir(carlos, chatId);

        BlockingQueue<String> recibidosBea = suscribir(conectar(bea));
        BlockingQueue<String> recibidosCarlos = suscribir(conectar(carlos));
        // La suscripción se registra de forma asíncrona en el servidor.
        Thread.sleep(500);

        long inicio = System.nanoTime();
        HttpResponse<String> respuesta = enviar(ana, chatId, "Hola a todos");
        assertThat(respuesta.statusCode()).isEqualTo(201);

        String recibido = recibidosBea.poll(2, TimeUnit.SECONDS);
        long milisegundos = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(recibido).as("Bea debe recibir el mensaje").isNotNull();
        assertThat(milisegundos).isLessThan(2000);
        assertThat((String) JsonPath.read(recibido, "$.contenido")).isEqualTo("Hola a todos");
        assertThat((String) JsonPath.read(recibido, "$.alias")).isEqualTo("Ana");
        assertThat((String) JsonPath.read(recibido, "$.chatId")).isEqualTo(chatId);
        // RF-11: Carlos ha salido del chat y no recibe nada aunque siga conectado.
        assertThat(recibidosCarlos.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    @DisplayName("CP-MSG-12: sin un token válido no se establece la conexión WebSocket")
    void conexionSinToken() {
        assertThatThrownBy(() -> conectar("token-no-valido"))
                .isInstanceOf(Exception.class);
    }

    // ---------------------------------------------------------------- Ayudas

    private StompSession conectar(String token) throws Exception {
        StompHeaders cabeceras = new StompHeaders();
        cabeceras.add("Authorization", "Bearer " + token);
        StompSession sesion = stomp.connectAsync(
                        "ws://localhost:" + puerto + "/ws",
                        new WebSocketHttpHeaders(),
                        cabeceras,
                        new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);
        sesiones.add(sesion);
        return sesion;
    }

    private static BlockingQueue<String> suscribir(StompSession sesion) {
        BlockingQueue<String> recibidos = new LinkedBlockingQueue<>();
        sesion.subscribe("/user/queue/mensajes", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recibidos.add(new String((byte[]) payload, StandardCharsets.UTF_8));
            }
        });
        return recibidos;
    }

    private String usuario(String alias) throws Exception {
        String correo = PREFIJO_CORREO + UUID.randomUUID() + "@hablabar.es";
        HttpResponse<String> registro = peticion("POST", "/api/v1/auth/registro", null, """
                {"alias":"%s","correo":"%s","password":"contrasena123"}
                """.formatted(alias, correo));
        assertThat(registro.statusCode()).isEqualTo(201);
        String token = JsonPath.read(registro.body(), "$.token");

        HttpResponse<String> ubicacion = peticion("PUT", "/api/v1/ubicacion", token, """
                {"latitud":%s,"longitud":%s,"precision":10,"fechaCaptura":"%s"}
                """.formatted(LAT, LON, Instant.now()));
        assertThat(ubicacion.statusCode()).isEqualTo(200);
        return token;
    }

    private String crearChat(String token) throws Exception {
        HttpResponse<String> respuesta = peticion("POST", "/api/v1/chats", token, "{\"nombre\":\"Afterwork\"}");
        assertThat(respuesta.statusCode()).isEqualTo(201);
        return JsonPath.read(respuesta.body(), "$.id");
    }

    private void entrar(String token, String chatId) throws Exception {
        assertThat(peticion("POST", "/api/v1/chats/" + chatId + "/participacion", token, null).statusCode())
                .isEqualTo(200);
    }

    private void salir(String token, String chatId) throws Exception {
        assertThat(peticion("DELETE", "/api/v1/chats/" + chatId + "/participacion", token, null).statusCode())
                .isEqualTo(204);
    }

    private HttpResponse<String> enviar(String token, String chatId, String contenido) throws Exception {
        return peticion("POST", "/api/v1/chats/" + chatId + "/mensajes", token, """
                {"idCliente":"%s","contenido":"%s"}
                """.formatted(UUID.randomUUID(), contenido));
    }

    private HttpResponse<String> peticion(String metodo, String ruta, String token, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .timeout(Duration.ofSeconds(10))
                .method(metodo, json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
