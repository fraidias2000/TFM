package es.hablabar.notifications;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Prueba de extremo a extremo de los avisos de nuevos chats (RF-06, RF-15, RN-08):
 * arranca el servidor en un puerto real, conecta clientes WebSocket y crea un chat
 * mediante la API. Los datos se confirman en la BD y se eliminan al terminar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AvisosWebSocketTest {

    // Puerta del Sol (Madrid). 0,0003° de latitud son unos 33 m y 0,009°, aproximadamente 1 km.
    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;
    private static final double A_33_M = 0.0003;
    private static final double A_1_KM = 0.009;
    private static final String PREFIJO_CORREO = "test.wsavisos.";

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
    @DisplayName("CP-AVISO-05: al crear un chat, solo los usuarios elegibles conectados reciben el aviso en menos de 2 s")
    void avisoEnTiempoReal() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String bea = usuario("Bea", LAT + A_33_M, LON);
        String carlos = usuario("Carlos", LAT + A_1_KM, LON);
        String dani = usuario("Dani", LAT + A_33_M, LON);
        assertThat(peticion("PATCH", "/api/v1/usuarios/me", dani, "{\"avisosActivados\":false}").statusCode())
                .isEqualTo(200);

        BlockingQueue<String> avisosAna = suscribir(conectar(ana));
        BlockingQueue<String> avisosBea = suscribir(conectar(bea));
        BlockingQueue<String> avisosCarlos = suscribir(conectar(carlos));
        BlockingQueue<String> avisosDani = suscribir(conectar(dani));
        // La suscripción se registra de forma asíncrona en el servidor.
        Thread.sleep(500);

        long inicio = System.nanoTime();
        HttpResponse<String> creado = peticion("POST", "/api/v1/chats", ana,
                "{\"nombre\":\"Afterwork\",\"tema\":\"Barrio\"}");
        assertThat(creado.statusCode()).isEqualTo(201);
        String chatId = JsonPath.read(creado.body(), "$.id");

        String aviso = avisosBea.poll(2, TimeUnit.SECONDS);
        long milisegundos = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(aviso).as("Bea, a 33 m y con avisos activados, recibe el aviso").isNotNull();
        assertThat(milisegundos).isLessThan(2000);
        assertThat((String) JsonPath.read(aviso, "$.chatId")).isEqualTo(chatId);
        assertThat((String) JsonPath.read(aviso, "$.nombre")).isEqualTo("Afterwork");
        assertThat((Integer) JsonPath.read(aviso, "$.distanciaMaximaMetros")).isEqualTo(50);
        // RNF-03: el aviso no incluye coordenadas.
        assertThat(aviso).doesNotContain("latitud").doesNotContain("longitud");

        // Ni el creador, ni quien está a 1 km, ni quien tiene los avisos desactivados.
        assertThat(avisosAna.poll(1, TimeUnit.SECONDS)).isNull();
        assertThat(avisosCarlos.poll(100, TimeUnit.MILLISECONDS)).isNull();
        assertThat(avisosDani.poll(100, TimeUnit.MILLISECONDS)).isNull();
        // RN-08: un único aviso por chat.
        assertThat(avisosBea.poll(500, TimeUnit.MILLISECONDS)).isNull();
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
        sesion.subscribe("/user/queue/avisos", new StompFrameHandler() {
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

    private String usuario(String alias, double latitud, double longitud) throws Exception {
        String correo = PREFIJO_CORREO + UUID.randomUUID() + "@hablabar.es";
        HttpResponse<String> registro = peticion("POST", "/api/v1/auth/registro", null, """
                {"alias":"%s","correo":"%s","password":"contrasena123"}
                """.formatted(alias, correo));
        assertThat(registro.statusCode()).isEqualTo(201);
        String token = JsonPath.read(registro.body(), "$.token");

        HttpResponse<String> ubicacion = peticion("PUT", "/api/v1/ubicacion", token, """
                {"latitud":%s,"longitud":%s,"precision":10,"fechaCaptura":"%s"}
                """.formatted(latitud, longitud, Instant.now()));
        assertThat(ubicacion.statusCode()).isEqualTo(200);
        return token;
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
