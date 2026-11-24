package es.hablabar.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.hablabar.messages.service.LimpiezaMensajes;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas de integración del envío y la consulta de mensajes (RF-08, RF-11, RF-13,
 * RF-16; RN-05, RN-07; RNF-01, RNF-06).
 *
 * <p>Usan la base de datos de desarrollo; cada test se ejecuta en una transacción
 * que se deshace al terminar. La entrega en tiempo real se prueba aparte, en
 * MensajesWebSocketTest, porque necesita que los datos se confirmen.
 */
@SpringBootTest
@Transactional
class MensajeControllerTest {

    // Puerta del Sol (Madrid). 0,009° de latitud son aproximadamente 1 km.
    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;
    private static final double A_1_KM = 0.009;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LimpiezaMensajes limpieza;

    private MockMvc mvc;
    private int contadorUsuarios;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("CP-MSG-01: un participante envía un mensaje y los demás lo ven en el historial con su alias")
    void enviarYConsultar() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);
        String bea = usuario("Bea");
        entrar(bea, chatId);

        enviar(ana, chatId, UUID.randomUUID(), "Hola, ¿alguien por aquí?")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value("Ana"))
                .andExpect(jsonPath("$.contenido").value("Hola, ¿alguien por aquí?"))
                .andExpect(jsonPath("$.chatId").value(chatId))
                .andExpect(jsonPath("$.correo").doesNotExist());
        enviar(bea, chatId, UUID.randomUUID(), "¡Yo!").andExpect(status().isCreated());

        historial(bea, chatId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].alias").value("Ana"))
                .andExpect(jsonPath("$[1].alias").value("Bea"))
                .andExpect(jsonPath("$[1].contenido").value("¡Yo!"));
    }

    @Test
    @DisplayName("CP-MSG-02: un mensaje vacío o solo con espacios se rechaza con 400")
    void mensajeVacio() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);

        enviar(ana, chatId, UUID.randomUUID(), "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles.contenido").exists());
    }

    @Test
    @DisplayName("CP-MSG-03: se aceptan 1000 caracteres y se rechazan 1001")
    void longitudMaxima() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);

        enviar(ana, chatId, UUID.randomUUID(), "a".repeat(1000)).andExpect(status().isCreated());
        enviar(ana, chatId, UUID.randomUUID(), "a".repeat(1001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.contenido").exists());
    }

    @Test
    @DisplayName("CP-MSG-04: repetir el envío con el mismo identificador no duplica el mensaje")
    void reenvioIdempotente() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);
        UUID idCliente = UUID.randomUUID();

        String id = campo(enviar(ana, chatId, idCliente, "Hola")
                .andExpect(status().isCreated()).andReturn(), "$.id");
        enviar(ana, chatId, idCliente, "Hola")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        historial(ana, chatId).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("CP-MSG-05: quien no participa no puede enviar ni consultar (403)")
    void noParticipa() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);
        String carlos = usuario("Carlos");

        enviar(carlos, chatId, UUID.randomUUID(), "Hola")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_PARTICIPA"));
        historial(carlos, chatId)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-MSG-06: tras salir del chat no se puede enviar ni consultar el historial")
    void despuesDeSalir() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);
        String bea = usuario("Bea");
        entrar(bea, chatId);
        mvc.perform(delete("/api/v1/chats/" + chatId + "/participacion").header("Authorization", "Bearer " + bea))
                .andExpect(status().isNoContent());

        enviar(bea, chatId, UUID.randomUUID(), "Hola")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_PARTICIPA"));
        historial(bea, chatId).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CP-MSG-07: en un chat caducado se rechazan mensajes y consultas con 410")
    void chatCaducado() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);
        enviar(ana, chatId, UUID.randomUUID(), "Hola").andExpect(status().isCreated());
        caducar(chatId);

        enviar(ana, chatId, UUID.randomUUID(), "¿Sigue abierto?")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value("CHAT_FINALIZADO"));
        historial(ana, chatId).andExpect(status().isGone());
    }

    @Test
    @DisplayName("CP-MSG-08: alejarse del chat no impide seguir enviando mensajes")
    void alejarse() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);
        ubicar(ana, LAT + A_1_KM, LON);

        enviar(ana, chatId, UUID.randomUUID(), "Sigo aquí aunque me haya ido")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CP-MSG-09: la limpieza elimina los mensajes de chats caducados y conserva los activos")
    void limpieza() throws Exception {
        String ana = usuario("Ana");
        String caducado = crearChat(ana);
        String activo = crearChat(ana);
        enviar(ana, caducado, UUID.randomUUID(), "Mensaje antiguo").andExpect(status().isCreated());
        enviar(ana, activo, UUID.randomUUID(), "Mensaje actual").andExpect(status().isCreated());
        caducar(caducado);

        assertThat(limpieza.eliminarMensajesCaducados()).isGreaterThanOrEqualTo(1);

        assertThat(contarMensajes(caducado)).isZero();
        assertThat(contarMensajes(activo)).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-MSG-10: sin identificador de mensaje o con chat inexistente se rechaza")
    void erroresDeEntrada() throws Exception {
        String ana = usuario("Ana");
        String chatId = crearChat(ana);

        mvc.perform(post("/api/v1/chats/" + chatId + "/mensajes").header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenido\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.idCliente").exists());
        enviar(ana, UUID.randomUUID().toString(), UUID.randomUUID(), "Hola")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CHAT_NO_ENCONTRADO"));
        mvc.perform(get("/api/v1/chats/" + chatId + "/mensajes"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- Ayudas

    /** Registra un usuario, envía su ubicación en el punto de referencia y devuelve su token. */
    private String usuario(String alias) throws Exception {
        contadorUsuarios++;
        String json = """
                {"alias":"%s","correo":"test.mensajes.%d@hablabar.es","password":"contrasena123"}
                """.formatted(alias, contadorUsuarios);
        MvcResult resultado = mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String token = campo(resultado, "$.token");
        ubicar(token, LAT, LON);
        return token;
    }

    private void ubicar(String token, double latitud, double longitud) throws Exception {
        String json = """
                {"latitud":%s,"longitud":%s,"precision":10,"fechaCaptura":"%s"}
                """.formatted(latitud, longitud, Instant.now());
        mvc.perform(put("/api/v1/ubicacion").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    private String crearChat(String token) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/chats").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Afterwork\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return campo(resultado, "$.id");
    }

    private void entrar(String token, String chatId) throws Exception {
        mvc.perform(post("/api/v1/chats/" + chatId + "/participacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private ResultActions enviar(String token, String chatId, UUID idCliente, String contenido) throws Exception {
        String json = """
                {"idCliente":"%s","contenido":"%s"}
                """.formatted(idCliente, contenido);
        return mvc.perform(post("/api/v1/chats/" + chatId + "/mensajes").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions historial(String token, String chatId) throws Exception {
        return mvc.perform(get("/api/v1/chats/" + chatId + "/mensajes").header("Authorization", "Bearer " + token));
    }

    /** Mueve las fechas del chat al pasado manteniendo la duración de 1 hora (RN-06). */
    private void caducar(String chatId) {
        entityManager.flush();
        jdbc.update("""
                UPDATE chats
                SET fecha_creacion = now() - INTERVAL '2 hours',
                    fecha_caducidad = now() - INTERVAL '1 hour'
                WHERE id = ?
                """, UUID.fromString(chatId));
        entityManager.clear();
    }

    private int contarMensajes(String chatId) {
        Integer total = jdbc.queryForObject("""
                SELECT count(*)
                FROM mensajes m
                JOIN participaciones p ON p.id = m.participacion_id
                WHERE p.chat_id = ?
                """, Integer.class, UUID.fromString(chatId));
        return total == null ? 0 : total;
    }

    private static String campo(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}
