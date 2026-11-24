package es.hablabar.users;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

/** Pruebas de integración de la modificación del perfil propio (RF-15, RN-10). */
@SpringBootTest
@Transactional
class UsuarioControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("CP-USR-01: cambiar el alias; se guarda sin espacios sobrantes")
    void cambiarAlias() throws Exception {
        String token = registrar("Ana");

        actualizar(token, "{\"alias\":\"  Ana María  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("Ana María"))
                .andExpect(jsonPath("$.avisosActivados").value(true));

        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.alias").value("Ana María"));
    }

    @Test
    @DisplayName("CP-USR-02: desactivar y volver a activar los avisos; la preferencia queda guardada")
    void preferenciaDeAvisos() throws Exception {
        String token = registrar("Ana");

        actualizar(token, "{\"avisosActivados\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avisosActivados").value(false))
                .andExpect(jsonPath("$.alias").value("Ana"));
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.avisosActivados").value(false));

        actualizar(token, "{\"avisosActivados\":true}")
                .andExpect(jsonPath("$.avisosActivados").value(true));
    }

    @Test
    @DisplayName("CP-USR-03: alias vacío o de más de 50 caracteres se rechaza con 400")
    void aliasNoValido() throws Exception {
        String token = registrar("Ana");

        actualizar(token, "{\"alias\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles.alias").exists());
        actualizar(token, "{\"alias\":\"" + "a".repeat(51) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CP-USR-04: un cuerpo vacío no cambia nada y sin token se responde 401")
    void sinCambiosYSinToken() throws Exception {
        String token = registrar("Ana");

        actualizar(token, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alias").value("Ana"))
                .andExpect(jsonPath("$.avisosActivados").value(true));
        mvc.perform(patch("/api/v1/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alias\":\"Otro\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String registrar(String alias) throws Exception {
        String json = """
                {"alias":"%s","correo":"test.perfil@hablabar.es","password":"contrasena123"}
                """.formatted(alias);
        MvcResult resultado = mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.token");
    }

    private ResultActions actualizar(String token, String json) throws Exception {
        return mvc.perform(patch("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
