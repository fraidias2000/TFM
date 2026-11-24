package es.hablabar.chats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.hablabar.chats.service.ChatService;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
 * Pruebas de integración del módulo de chats (RF-04, RF-05, RF-07, RF-09, RF-11
 * a RF-14; RN-01, RN-02, RN-04 a RN-06, RN-09, RN-10).
 *
 * <p>Usan la base de datos de desarrollo; cada test se ejecuta en una transacción
 * que se deshace al terminar. Para simular la caducidad o un aforo reducido se
 * modifican los datos del chat directamente con SQL.
 */
@SpringBootTest
@Transactional
class ChatControllerTest {

    // Puerta del Sol (Madrid). 0,0003° de latitud son unos 33 m y 0,0009°, unos 100,08 m.
    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;
    private static final double A_33_M = 0.0003;
    private static final double A_100_M = 0.0009;
    private static final double A_1_KM = 0.009;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mvc;
    private int contadorUsuarios;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ------------------------------------------------------------------ Crear

    @Test
    @DisplayName("CP-CHAT-01: crear con ubicación válida; caduca en 1 h y el creador queda dentro")
    void crearChat() throws Exception {
        String ana = usuario("Ana", LAT, LON);

        MvcResult resultado = crear(ana, "Afterwork", "Charla tranquila")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Afterwork"))
                .andExpect(jsonPath("$.tema").value("Charla tranquila"))
                .andExpect(jsonPath("$.participantes").value(1))
                .andExpect(jsonPath("$.capacidad").value(20))
                .andExpect(jsonPath("$.creadoPorMi").value(true))
                .andExpect(jsonPath("$.participando").value(true))
                // RNF-03: no se devuelven coordenadas.
                .andExpect(jsonPath("$.latitudOrigen").doesNotExist())
                .andExpect(jsonPath("$.longitudOrigen").doesNotExist())
                .andReturn();

        Instant creado = Instant.parse(campo(resultado, "$.creadoEn"));
        Instant expira = Instant.parse(campo(resultado, "$.expiraEn"));
        assertThat(Duration.between(creado, expira)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("CP-CHAT-02: crear sin ubicación válida se rechaza con 422")
    void crearSinUbicacion() throws Exception {
        String ana = usuarioSinUbicacion("Ana");

        crear(ana, "Afterwork", null)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("UBICACION_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("CP-CHAT-03: crear con nombre vacío se rechaza con 400")
    void crearNombreVacio() throws Exception {
        String ana = usuario("Ana", LAT, LON);

        crear(ana, "   ", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles.nombre").exists());
    }

    // ------------------------------------------------------------- Descubrir

    @Test
    @DisplayName("CP-CHAT-04: a 33 m el chat aparece por tramos y sin coordenadas; a 100,08 m no aparece")
    void chatsCercanos() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuario("Bea", LAT + A_33_M, LON);
        String carlos = usuario("Carlos", LAT + A_100_M, LON);

        cercanos(bea)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chatId))
                .andExpect(jsonPath("$[0].distanciaMaximaMetros").value(50))
                .andExpect(jsonPath("$[0].participando").value(false))
                .andExpect(jsonPath("$[0].latitudOrigen").doesNotExist());

        // RN-02: distancia estrictamente inferior a 100 m.
        cercanos(carlos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ----------------------------------------------------------------- Entrar

    @Test
    @DisplayName("CP-CHAT-05: entrar dentro del radio; entrar dos veces no duplica la participación")
    void entrarDentroDelRadio() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuario("Bea", LAT + A_33_M, LON);

        entrar(bea, chatId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participando").value(true))
                .andExpect(jsonPath("$.creadoPorMi").value(false))
                .andExpect(jsonPath("$.participantes").value(2));

        entrar(bea, chatId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantes").value(2));
    }

    @Test
    @DisplayName("CP-CHAT-06: entrar a 100,08 m se rechaza con 403 FUERA_DE_RADIO")
    void entrarFueraDelRadio() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String carlos = usuario("Carlos", LAT + A_100_M, LON);

        entrar(carlos, chatId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_RADIO"));
    }

    @Test
    @DisplayName("CP-CHAT-07: chat completo rechaza la entrada con 409 y no aparece en la lista")
    void chatCompleto() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        // Se reduce el aforo a 2 para no tener que crear 20 usuarios.
        sql("UPDATE chats SET capacidad_maxima = 2 WHERE id = ?", chatId);
        entrar(usuario("Bea", LAT, LON), chatId).andExpect(status().isOk());
        String carlos = usuario("Carlos", LAT, LON);

        entrar(carlos, chatId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CHAT_COMPLETO"));
        cercanos(carlos).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("CP-CHAT-08: chat caducado rechaza entradas y consultas con 410 y no aparece en la lista")
    void chatCaducado() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        caducar(chatId);
        String bea = usuario("Bea", LAT, LON);

        entrar(bea, chatId)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.codigo").value("CHAT_FINALIZADO"));
        detalle(ana, chatId)
                .andExpect(status().isGone());
        cercanos(bea).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("CP-CHAT-09: entrar sin ubicación válida se rechaza con 422")
    void entrarSinUbicacion() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuarioSinUbicacion("Bea");

        entrar(bea, chatId)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("UBICACION_NO_DISPONIBLE"));
    }

    // ------------------------------------------------- Salir y continuidad

    @Test
    @DisplayName("CP-CHAT-10: tras salir no se puede consultar; volver a entrar crea otra participación")
    void salirYVolver() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuario("Bea", LAT, LON);
        entrar(bea, chatId).andExpect(status().isOk());

        salir(bea, chatId).andExpect(status().isNoContent());
        detalle(bea, chatId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("NO_PARTICIPA"));

        entrar(bea, chatId).andExpect(status().isOk());
        Integer filas = jdbc.queryForObject(
                "SELECT count(*) FROM participaciones WHERE chat_id = ?::uuid", Integer.class, chatId);
        assertThat(filas).isEqualTo(3); // Ana + dos entradas de Bea
    }

    @Test
    @DisplayName("CP-CHAT-11: alejarse no revoca la participación; salir y volver desde lejos sí exige proximidad")
    void continuidadAlAlejarse() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuario("Bea", LAT, LON);
        entrar(bea, chatId).andExpect(status().isOk());

        ubicar(bea, LAT + A_1_KM, LON);
        detalle(bea, chatId).andExpect(status().isOk());

        salir(bea, chatId).andExpect(status().isNoContent());
        entrar(bea, chatId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_RADIO"));
    }

    @Test
    @DisplayName("CP-CHAT-12: si el creador sale, el chat sigue y los demás continúan")
    void salidaDelCreador() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuario("Bea", LAT, LON);
        entrar(bea, chatId).andExpect(status().isOk());

        salir(ana, chatId).andExpect(status().isNoContent());

        detalle(bea, chatId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantes").value(1));
    }

    // ----------------------------------------------------- Consultas

    @Test
    @DisplayName("CP-CHAT-13: los participantes se muestran por alias, sin correo")
    void participantes() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String bea = usuario("Bea", LAT, LON);
        entrar(bea, chatId).andExpect(status().isOk());

        mvc.perform(get("/api/v1/chats/" + chatId + "/participantes").header("Authorization", "Bearer " + bea))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].alias").value("Ana"))
                .andExpect(jsonPath("$[0].esCreador").value(true))
                .andExpect(jsonPath("$[1].alias").value("Bea"))
                .andExpect(jsonPath("$[0].correo").doesNotExist());
    }

    @Test
    @DisplayName("CP-CHAT-14: se recuperan los chats activos en los que se participa")
    void participando() throws Exception {
        String ana = usuario("Ana", LAT, LON);
        String chatId = idDe(crear(ana, "Afterwork", null));
        String otroId = idDe(crear(ana, "Otro", null));
        caducar(otroId);

        mvc.perform(get("/api/v1/chats/participando").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(chatId));
    }

    @Test
    @DisplayName("CP-CHAT-15: chat inexistente devuelve 404; identificador mal formado, 400; sin token, 401")
    void erroresDeAcceso() throws Exception {
        String ana = usuario("Ana", LAT, LON);

        entrar(ana, UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("CHAT_NO_ENCONTRADO"));
        entrar(ana, "no-es-un-uuid")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_NO_VALIDO"));
        mvc.perform(get("/api/v1/chats/cercanos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Los tramos de distancia son 25, 50 y 100 m")
    void tramosDeDistancia() {
        assertThat(List.of(0.0, 24.9, 25.0, 49.9, 50.0, 99.9).stream()
                .map(ChatService::distanciaAproximada).toList())
                .containsExactly(25, 25, 50, 50, 100, 100);
    }

    // ---------------------------------------------------------------- Ayudas

    /** Registra un usuario, envía su ubicación y devuelve su token. */
    private String usuario(String alias, double latitud, double longitud) throws Exception {
        String token = usuarioSinUbicacion(alias);
        ubicar(token, latitud, longitud);
        return token;
    }

    private String usuarioSinUbicacion(String alias) throws Exception {
        contadorUsuarios++;
        String json = """
                {"alias":"%s","correo":"test.chats.%d@hablabar.es","password":"contrasena123"}
                """.formatted(alias, contadorUsuarios);
        MvcResult resultado = mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return campo(resultado, "$.token");
    }

    private void ubicar(String token, double latitud, double longitud) throws Exception {
        String json = """
                {"latitud":%s,"longitud":%s,"precision":10,"fechaCaptura":"%s"}
                """.formatted(latitud, longitud, Instant.now());
        mvc.perform(put("/api/v1/ubicacion").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    private ResultActions crear(String token, String nombre, String tema) throws Exception {
        String json = tema == null
                ? """
                  {"nombre":"%s"}
                  """.formatted(nombre)
                : """
                  {"nombre":"%s","tema":"%s"}
                  """.formatted(nombre, tema);
        return mvc.perform(post("/api/v1/chats").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions cercanos(String token) throws Exception {
        return mvc.perform(get("/api/v1/chats/cercanos").header("Authorization", "Bearer " + token));
    }

    private ResultActions entrar(String token, String chatId) throws Exception {
        return mvc.perform(post("/api/v1/chats/" + chatId + "/participacion")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions salir(String token, String chatId) throws Exception {
        return mvc.perform(delete("/api/v1/chats/" + chatId + "/participacion")
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions detalle(String token, String chatId) throws Exception {
        return mvc.perform(get("/api/v1/chats/" + chatId).header("Authorization", "Bearer " + token));
    }

    /** Mueve las fechas del chat al pasado manteniendo la duración de 1 hora (RN-06). */
    private void caducar(String chatId) {
        sql("""
                UPDATE chats
                SET fecha_creacion = now() - INTERVAL '2 hours',
                    fecha_caducidad = now() - INTERVAL '1 hour'
                WHERE id = ?
                """, chatId);
    }

    /**
     * Ejecuta SQL directamente sobre la BD. Antes se vuelcan los cambios pendientes de
     * JPA y después se vacía su caché, para que las siguientes peticiones lean los datos
     * actualizados.
     */
    private void sql(String sentencia, String chatId) {
        entityManager.flush();
        jdbc.update(sentencia, UUID.fromString(chatId));
        entityManager.clear();
    }

    private static String idDe(ResultActions resultado) throws Exception {
        return campo(resultado.andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private static String campo(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}