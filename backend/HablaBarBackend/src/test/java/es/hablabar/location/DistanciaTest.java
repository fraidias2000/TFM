package es.hablabar.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import es.hablabar.location.service.DistanciaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias del cálculo de distancias (RN-02). */
class DistanciaTest {

    // Puerta del Sol (Madrid)
    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;

    private final DistanciaService distancia = new DistanciaService();

    @Test
    @DisplayName("CP-LOC-07: la distancia de un punto a sí mismo es 0")
    void mismoPunto() {
        assertThat(distancia.metros(LAT, LON, LAT, LON)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-LOC-07: 0,0009° de latitud equivalen a unos 100 m")
    void cienMetros() {
        assertThat(distancia.metros(LAT, LON, LAT + 0.0009, LON)).isCloseTo(100.08, within(0.5));
    }

    @Test
    @DisplayName("CP-LOC-07: 0,0008° de latitud quedan dentro del radio de 100 m")
    void dentroDelRadio() {
        assertThat(distancia.metros(LAT, LON, LAT + 0.0008, LON)).isLessThan(100.0);
    }

    @Test
    @DisplayName("CP-LOC-07: distancia larga de referencia (Madrid-Barcelona, unos 505 km)")
    void distanciaLarga() {
        assertThat(distancia.metros(LAT, LON, 41.3874, 2.1686)).isCloseTo(505_096, within(500.0));
    }

    @Test
    @DisplayName("CP-LOC-07: la distancia es simétrica")
    void simetrica() {
        double ida = distancia.metros(LAT, LON, 40.41553, -3.70742);
        double vuelta = distancia.metros(40.41553, -3.70742, LAT, LON);
        assertThat(ida).isCloseTo(vuelta, within(1e-9));
    }
}