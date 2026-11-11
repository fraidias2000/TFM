package es.hablabar.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.hablabar.location.dto.UbicacionRequest;
import es.hablabar.location.exception.UbicacionNoValidaException;
import es.hablabar.location.model.UbicacionReciente;
import es.hablabar.location.service.UbicacionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas de validez de la ubicación (RN-03).
 * Usan un reloj controlado para simular el paso del tiempo sin esperas.
 */
class UbicacionServiceTest {

    private static final double LAT = 40.4168;
    private static final double LON = -3.7038;

    private RelojControlado reloj;
    private UbicacionService servicio;
    private UUID usuario;

    @BeforeEach
    void setUp() {
        reloj = new RelojControlado(Instant.parse("2026-11-11T18:00:00Z"));
        servicio = new UbicacionService(reloj);
        usuario = UUID.randomUUID();
    }

    @Test
    @DisplayName("CP-LOC-01: ubicación reciente y precisa se acepta y caduca a los 30 s de su captura")
    void ubicacionValida() {
        Instant captura = reloj.instant().minusSeconds(5);

        UbicacionReciente ubicacion = servicio.registrar(usuario, peticion(10.0, captura));

        assertThat(ubicacion.validaHasta()).isEqualTo(captura.plusSeconds(30));
        assertThat(servicio.obtenerValida(usuario)).isPresent();
    }

    @Test
    @DisplayName("CP-LOC-02: precisión de exactamente 20 m se acepta; peor de 20 m se rechaza")
    void limiteDePrecision() {
        servicio.registrar(usuario, peticion(20.0, reloj.instant()));

        assertThatThrownBy(() -> servicio.registrar(usuario, peticion(20.5, reloj.instant())))
                .isInstanceOf(UbicacionNoValidaException.class)
                .hasFieldOrPropertyWithValue("codigo", "UBICACION_IMPRECISA");
    }

    @Test
    @DisplayName("CP-LOC-03: ubicación de 29 s se acepta; de 30 s o más se rechaza")
    void limiteDeAntiguedad() {
        servicio.registrar(usuario, peticion(10.0, reloj.instant().minusSeconds(29)));

        assertThatThrownBy(() -> servicio.registrar(usuario, peticion(10.0, reloj.instant().minusSeconds(30))))
                .isInstanceOf(UbicacionNoValidaException.class)
                .hasFieldOrPropertyWithValue("codigo", "UBICACION_ANTIGUA");
    }

    @Test
    @DisplayName("CP-LOC-04: reloj del dispositivo algo adelantado se tolera; muy adelantado se rechaza")
    void relojDelDispositivo() {
        UbicacionReciente ubicacion = servicio.registrar(usuario, peticion(10.0, reloj.instant().plusSeconds(10)));
        // Se toma la hora del servidor, no la del dispositivo.
        assertThat(ubicacion.validaHasta()).isEqualTo(reloj.instant().plusSeconds(30));

        assertThatThrownBy(() -> servicio.registrar(usuario, peticion(10.0, reloj.instant().plusSeconds(60))))
                .isInstanceOf(UbicacionNoValidaException.class)
                .hasFieldOrPropertyWithValue("codigo", "UBICACION_FECHA_NO_VALIDA");
    }

    @Test
    @DisplayName("CP-LOC-05: pasados 30 s desde la captura la ubicación deja de ser válida")
    void caducidad() {
        servicio.registrar(usuario, peticion(10.0, reloj.instant()));

        reloj.avanzar(Duration.ofSeconds(29));
        assertThat(servicio.obtenerValida(usuario)).isPresent();

        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(servicio.obtenerValida(usuario)).isEmpty();
        assertThatThrownBy(() -> servicio.exigirValida(usuario))
                .isInstanceOf(UbicacionNoValidaException.class)
                .hasFieldOrPropertyWithValue("codigo", "UBICACION_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("CP-LOC-06: solo se conserva la última ubicación y se puede eliminar")
    void ultimaUbicacionYEliminacion() {
        servicio.registrar(usuario, peticion(10.0, reloj.instant()));
        servicio.registrar(usuario, new UbicacionRequest(LAT + 0.001, LON, 5.0, reloj.instant()));

        assertThat(servicio.exigirValida(usuario).latitud()).isEqualTo(LAT + 0.001);

        servicio.eliminar(usuario);
        assertThat(servicio.obtenerValida(usuario)).isEmpty();
    }

    @Test
    @DisplayName("La limpieza periódica borra las ubicaciones caducadas")
    void limpiezaPeriodica() {
        UUID otro = UUID.randomUUID();
        servicio.registrar(usuario, peticion(10.0, reloj.instant()));
        reloj.avanzar(Duration.ofSeconds(20));
        servicio.registrar(otro, peticion(10.0, reloj.instant()));
        reloj.avanzar(Duration.ofSeconds(15));

        servicio.eliminarCaducadas();

        assertThat(servicio.obtenerValida(usuario)).isEmpty();
        assertThat(servicio.obtenerValida(otro)).isPresent();
    }

    private static UbicacionRequest peticion(double precision, Instant captura) {
        return new UbicacionRequest(LAT, LON, precision, captura);
    }

    /** Reloj cuyo instante se fija y se avanza manualmente en las pruebas. */
    private static final class RelojControlado extends Clock {
        private Instant ahora;

        RelojControlado(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}