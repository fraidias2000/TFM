package es.hablabar.location;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Pruebas de integración del endpoint de ubicación.
 * Usan la base de datos de desarrollo solo para crear el usuario de prueba;
 * la transacción se deshace al terminar cada test.
 */
@SpringBootTest
@Transactional
class UbicacionControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String respuesta = mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"alias":"Ari","correo":"test.ubicacion@hablabar.es","password":"contrasena123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        token = JsonPath.read(respuesta, "$.token");
    }

    @Test
    @DisplayName("CP-LOC-01: ubicación válida devuelve hasta cuándo sirve y ninguna coordenada")
    void ubicacionValida() throws Exception {
        enviar(40.4168, -3.7038, 10.0, Instant.now())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validaHasta").isNotEmpty())
                // RNF-03: la respuesta no devuelve coordenadas.
                .andExpect(jsonPath("$.latitud").doesNotExist())
                .andExpect(jsonPath("$.longitud").doesNotExist());
    }

    @Test
    @DisplayName("CP-LOC-02: precisión peor de 20 m se rechaza con 422")
    void ubicacionImprecisa() throws Exception {
        enviar(40.4168, -3.7038, 50.0, Instant.now())
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("UBICACION_IMPRECISA"));
    }

    @Test
    @DisplayName("CP-LOC-03: ubicación de hace más de 30 s se rechaza con 422")
    void ubicacionAntigua() throws Exception {
        enviar(40.4168, -3.7038, 10.0, Instant.now().minusSeconds(60))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("UBICACION_ANTIGUA"));
    }

    @Test
    @DisplayName("CP-LOC-08: coordenadas fuera de rango se rechazan con 400")
    void coordenadasFueraDeRango() throws Exception {
        enviar(95.0, -3.7038, 10.0, Instant.now())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles.latitud").exists());
    }

    @Test
    @DisplayName("CP-LOC-09: sin token se rechaza con 401")
    void sinToken() throws Exception {
        mvc.perform(put("/api/v1/ubicacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(40.4168, -3.7038, 10.0, Instant.now())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CP-LOC-06: eliminar la ubicación responde 204")
    void eliminarUbicacion() throws Exception {
        enviar(40.4168, -3.7038, 10.0, Instant.now()).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/ubicacion").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private ResultActions enviar(double latitud, double longitud, double precision, Instant captura)
            throws Exception {
        return mvc.perform(put("/api/v1/ubicacion")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(latitud, longitud, precision, captura)));
    }

    private static String json(double latitud, double longitud, double precision, Instant captura) {
        return """
                {"latitud":%s,"longitud":%s,"precision":%s,"fechaCaptura":"%s"}
                """.formatted(latitud, longitud, precision, captura);
    }
}