package es.hablabar.shared.error;

import java.time.Instant;
import java.util.Map;

/**
 * Formato común de los errores de la API (RNF-04): un código estable para el
 * frontend, un mensaje comprensible y, si procede, el error de cada campo.
 */
public record ErrorResponse(
        String codigo,
        String mensaje,
        Map<String, String> detalles,
        Instant timestamp
) {

    public static ErrorResponse of(String codigo, String mensaje) {
        return new ErrorResponse(codigo, mensaje, Map.of(), Instant.now());
    }

    public static ErrorResponse of(String codigo, String mensaje, Map<String, String> detalles) {
        return new ErrorResponse(codigo, mensaje, detalles, Instant.now());
    }
}