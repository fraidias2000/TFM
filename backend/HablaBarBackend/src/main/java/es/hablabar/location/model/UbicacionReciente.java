package es.hablabar.location.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Última ubicación conocida de un usuario. Solo se conserva en memoria y caduca
 * a los 30 segundos de su captura (RN-03); no se guarda historial (RNF-03).
 */
public record UbicacionReciente(
        double latitud,
        double longitud,
        double precision,
        Instant capturadaEn,
        Duration vigencia
) {

    public Instant validaHasta() {
        return capturadaEn.plus(vigencia);
    }

    public boolean esValidaEn(Instant instante) {
        return instante.isBefore(validaHasta());
    }

    @Override
    public String toString() {
        // Las coordenadas no se escriben en los registros técnicos (RNF-03).
        return "UbicacionReciente[capturadaEn=" + capturadaEn + ", precision=" + precision + "]";
    }
}