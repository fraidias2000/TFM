package es.hablabar.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.hablabar.chats.model.ChatCreado;
import es.hablabar.notifications.model.Destinatario;
import es.hablabar.notifications.service.AvisoService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas de la selección de destinatarios de los avisos (RF-06, RF-15, RN-02,
 * RN-03, RN-08). La entrega por WebSocket se prueba en AvisosWebSocketTest.
 *
 * <p>Cada test se ejecuta en una transacción que se deshace al terminar, por lo que
 * el aviso no llega a enviarse; aquí se comprueba el evento publicado y a quién se
 * enviaría.
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class AvisoServiceTest {

    // Puerta del Sol (Madrid). 0,0003° de latitud son unos 33 m y 0,0009°, unos 100,08 m.
    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;
    private static final double A_33_M = 0.0003;
    private static final double A_100_M = 0.0009;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AvisoService avisoService;

    @Autowired
    private ApplicationEvents eventos;

    private MockMvc mvc;
    private int contadorUsuarios;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("CP-AVISO-01: crear un chat publica un único evento con los datos del chat")
    void eventoAlCrear() throws Exception {
        Usuario ana = usuario("Ana", LAT, LON);

        String chatId = crearChat(ana);

        List<ChatCreado> publicados = eventos.stream(ChatCreado.class).toList();
        assertThat(publicados).hasSize(1);
        assertThat(publicados.get(0).chatId()).hasToString(chatId);
        assertThat(publicados.get(0).creadorId()).isEqualTo(ana.id());
        assertThat(publicados.get(0).nombre()).isEqualTo("Afterwork");
    }

    @Test
    @DisplayName("CP-AVISO-02: se avisa a quien está a menos de 100 m; no al creador ni a quien está a 100,08 m")
    void destinatariosPorDistancia() throws Exception {
        Usuario ana = usuario("Ana", LAT, LON);
        Usuario bea = usuario("Bea", LAT + A_33_M, LON);
        Usuario carlos = usuario("Carlos", LAT + A_100_M, LON);

        List<Destinatario> destinatarios = avisoService.destinatarios(chatCreado(ana));

        assertThat(ids(destinatarios)).contains(bea.id()).doesNotContain(ana.id(), carlos.id());
        Destinatario aBea = destinatarios.stream().filter(d -> d.usuarioId().equals(bea.id())).findFirst().orElseThrow();
        assertThat(aBea.distanciaMetros()).isBetween(30.0, 36.0);
    }

    @Test
    @DisplayName("CP-AVISO-03: no se avisa a quien tiene los avisos desactivados")
    void avisosDesactivados() throws Exception {
        Usuario ana = usuario("Ana", LAT, LON);
        Usuario bea = usuario("Bea", LAT + A_33_M, LON);
        mvc.perform(patch("/api/v1/usuarios/me").header("Authorization", "Bearer " + bea.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"avisosActivados\":false}"))
                .andExpect(status().isOk());

        assertThat(ids(avisoService.destinatarios(chatCreado(ana)))).doesNotContain(bea.id());
    }

    @Test
    @DisplayName("CP-AVISO-04: no se avisa a quien no tiene una ubicación válida")
    void sinUbicacion() throws Exception {
        Usuario ana = usuario("Ana", LAT, LON);
        Usuario eva = usuarioSinUbicacion("Eva");

        assertThat(ids(avisoService.destinatarios(chatCreado(ana)))).doesNotContain(eva.id());
    }

    // ---------------------------------------------------------------- Ayudas

    private record Usuario(UUID id, String token) {
    }

    /** Crea un chat con la API y devuelve el evento que se ha publicado. */
    private ChatCreado chatCreado(Usuario creador) throws Exception {
        String chatId = crearChat(creador);
        return eventos.stream(ChatCreado.class)
                .filter(evento -> evento.chatId().toString().equals(chatId))
                .findFirst()
                .orElseThrow();
    }

    private String crearChat(Usuario creador) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/chats").header("Authorization", "Bearer " + creador.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Afterwork\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return campo(resultado, "$.id");
    }

    private Usuario usuario(String alias, double latitud, double longitud) throws Exception {
        Usuario usuario = usuarioSinUbicacion(alias);
        String json = """
                {"latitud":%s,"longitud":%s,"precision":10,"fechaCaptura":"%s"}
                """.formatted(latitud, longitud, Instant.now());
        mvc.perform(put("/api/v1/ubicacion").header("Authorization", "Bearer " + usuario.token())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
        return usuario;
    }

    private Usuario usuarioSinUbicacion(String alias) throws Exception {
        contadorUsuarios++;
        String json = """
                {"alias":"%s","correo":"test.avisos.%d@hablabar.es","password":"contrasena123"}
                """.formatted(alias, contadorUsuarios);
        MvcResult registro = mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        String token = campo(registro, "$.token");
        MvcResult perfil = mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return new Usuario(UUID.fromString(campo(perfil, "$.id")), token);
    }

    private static List<UUID> ids(List<Destinatario> destinatarios) {
        return destinatarios.stream().map(Destinatario::usuarioId).toList();
    }

    private static String campo(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}
