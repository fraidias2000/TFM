package es.hablabar.location.service;

import es.hablabar.location.dto.UbicacionRequest;
import es.hablabar.location.exception.UbicacionNoValidaException;
import es.hablabar.location.model.UbicacionReciente;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Recibe y conserva temporalmente la última ubicación de cada usuario (RF-03, RN-03).
 *
 * <p>Las ubicaciones se guardan solo en memoria, una por usuario, y caducan a los
 * 30 segundos de su captura. No se almacenan en la base de datos ni se conserva
 * un historial de posiciones (RNF-03).
 */
@Service
public class UbicacionService {

    /** RN-03: antigüedad máxima de la ubicación. */
    public static final Duration ANTIGUEDAD_MAXIMA = Duration.ofSeconds(30);

    /** RN-03: precisión horizontal máxima admitida, en metros. */
    public static final double PRECISION_MAXIMA_METROS = 20.0;

    /** Margen para relojes de dispositivo ligeramente adelantados respecto al servidor. */
    private static final Duration MARGEN_RELOJ = Duration.ofSeconds(30);

    private final Map<UUID, UbicacionReciente> ubicaciones = new ConcurrentHashMap<>();
    private final Clock clock;

    public UbicacionService(Clock clock) {
        this.clock = clock;
    }

    /** Valida la ubicación recibida y la guarda como la última del usuario. */
    public UbicacionReciente registrar(UUID usuarioId, UbicacionRequest request) {
        Instant ahora = clock.instant();

        if (request.precision() > PRECISION_MAXIMA_METROS) {
            throw UbicacionNoValidaException.imprecisa();
        }
        if (request.fechaCaptura().isAfter(ahora.plus(MARGEN_RELOJ))) {
            throw UbicacionNoValidaException.fechaNoValida();
        }
        // Si el reloj del dispositivo va algo adelantado, se toma la hora del servidor.
        Instant capturadaEn = request.fechaCaptura().isAfter(ahora) ? ahora : request.fechaCaptura();
        if (Duration.between(capturadaEn, ahora).compareTo(ANTIGUEDAD_MAXIMA) >= 0) {
            throw UbicacionNoValidaException.antigua();
        }

        UbicacionReciente ubicacion = new UbicacionReciente(
                request.latitud(),
                request.longitud(),
                request.precision(),
                capturadaEn,
                ANTIGUEDAD_MAXIMA);
        ubicaciones.put(usuarioId, ubicacion);
        return ubicacion;
    }

    /** Devuelve la última ubicación del usuario si todavía es válida. */
    public Optional<UbicacionReciente> obtenerValida(UUID usuarioId) {
        UbicacionReciente ubicacion = ubicaciones.get(usuarioId);
        if (ubicacion == null || !ubicacion.esValidaEn(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(ubicacion);
    }

    /**
     * Igual que {@link #obtenerValida(UUID)}, pero lanza una excepción si no hay
     * ubicación válida. La usarán los módulos de chats para crear, descubrir y entrar.
     */
    public UbicacionReciente exigirValida(UUID usuarioId) {
        return obtenerValida(usuarioId).orElseThrow(UbicacionNoValidaException::noDisponible);
    }

    /** Olvida la ubicación del usuario, por ejemplo al cerrar sesión. */
    public void eliminar(UUID usuarioId) {
        ubicaciones.remove(usuarioId);
    }

    /** Borra periódicamente las ubicaciones caducadas para no retenerlas en memoria. */
    @Scheduled(fixedDelay = 60_000)
    public void eliminarCaducadas() {
        Instant ahora = clock.instant();
        ubicaciones.values().removeIf(ubicacion -> !ubicacion.esValidaEn(ahora));
    }
}
