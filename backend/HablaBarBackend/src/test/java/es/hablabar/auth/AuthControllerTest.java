package es.hablabar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.hablabar.users.entity.Usuario;
import es.hablabar.users.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas de integración del módulo de autenticación (Tabla 14 de la memoria).
 * Usan la base de datos de desarrollo: cada test se ejecuta en una transacción
 * que se deshace al terminar, por lo que no deja datos guardados.
 */
@SpringBootTest
@Transactional
class AuthControllerTest {

    private static final String ALIAS = "Ari";
    private static final String CORREO = "test.auth@hablabar.es";
    private static final String PASSWORD = "contrasena123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ---------------------------------------------------------------- Registro

    @Test
    @DisplayName("CP-AUTH-01: registro con datos válidos")
    void registroValido() throws Exception {
        registrar(ALIAS, CORREO, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.alias").value(ALIAS))
                .andExpect(jsonPath("$.usuario.correo").value(CORREO))
                // La respuesta nunca incluye la contraseña ni su hash.
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());

        // RNF-02: en la BD solo se guarda el hash BCrypt, no la contraseña.
        Usuario guardado = usuarioRepository.buscarPorCorreo(CORREO).orElseThrow();
        assertThat(guardado.getPasswordHash())
                .startsWith("$2")
                .isNotEqualTo(PASSWORD);
    }

    @Test
    @DisplayName("CP-AUTH-02: registro con correo ya registrado (también con mayúsculas)")
    void registroCorreoRepetido() throws Exception {
        registrar(ALIAS, CORREO, PASSWORD).andExpect(status().isCreated());

        registrar("Otro", "  TEST.Auth@HablaBar.es ", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_REGISTRADO"));
    }

    @Test
    @DisplayName("CP-AUTH-03: registro con datos no válidos")
    void registroDatosNoValidos() throws Exception {
        registrar("", "malo", "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles.alias").exists())
                .andExpect(jsonPath("$.detalles.correo").exists())
                .andExpect(jsonPath("$.detalles.password").exists());
    }

    @Test
    @DisplayName("Registro con JSON mal formado")
    void registroJsonMalFormado() throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_MALFORMADA"));
    }

    // ------------------------------------------------------------------- Login

    @Test
    @DisplayName("CP-AUTH-04: login con credenciales válidas")
    void loginValido() throws Exception {
        registrar(ALIAS, CORREO, PASSWORD).andExpect(status().isCreated());

        login(CORREO, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.correo").value(CORREO));
    }

    @Test
    @DisplayName("CP-AUTH-05: login con contraseña incorrecta")
    void loginPasswordIncorrecta() throws Exception {
        registrar(ALIAS, CORREO, PASSWORD).andExpect(status().isCreated());

        login(CORREO, "otraContrasena")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("CP-AUTH-06: login con correo inexistente devuelve el mismo error")
    void loginCorreoInexistente() throws Exception {
        registrar(ALIAS, CORREO, PASSWORD).andExpect(status().isCreated());

        MvcResult passwordMal = login(CORREO, "otraContrasena")
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult correoMal = login("no.existe@hablabar.es", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andReturn();

        // RNF-03: la respuesta no revela si el correo está registrado.
        assertThat(campo(correoMal, "$.mensaje")).isEqualTo(campo(passwordMal, "$.mensaje"));
    }

    // ------------------------------------------------------- Rutas protegidas

    @Test
    @DisplayName("CP-AUTH-07: perfil con token válido")
    void perfilConToken() throws Exception {
        String token = campo(registrar(ALIAS, CORREO, PASSWORD).andReturn(), "$.token");

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value(ALIAS))
                .andExpect(jsonPath("$.correo").value(CORREO));
    }

    @Test
    @DisplayName("CP-AUTH-08: perfil sin token")
    void perfilSinToken() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CP-AUTH-08b: perfil con token manipulado")
    void perfilTokenManipulado() throws Exception {
        String token = campo(registrar(ALIAS, CORREO, PASSWORD).andReturn(), "$.token");

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- Ayudas

    private ResultActions registrar(String alias, String correo, String password) throws Exception {
        String json = """
                {"alias":"%s","correo":"%s","password":"%s"}
                """.formatted(alias, correo, password);
        return mvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions login(String correo, String password) throws Exception {
        String json = """
                {"correo":"%s","password":"%s"}
                """.formatted(correo, password);
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String campo(MvcResult resultado, String ruta) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), ruta);
    }
}