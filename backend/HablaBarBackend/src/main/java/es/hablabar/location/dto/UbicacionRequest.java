package es.hablabar.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Ubicación enviada por el dispositivo: coordenadas en grados decimales,
 * precisión horizontal declarada en metros y momento en que se obtuvo.
 */
public record UbicacionRequest(

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "La latitud debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "La latitud debe estar entre -90 y 90")
        Double latitud,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "La longitud debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "La longitud debe estar entre -180 y 180")
        Double longitud,

        @NotNull(message = "La precisión es obligatoria")
        @Positive(message = "La precisión debe ser mayor que 0")
        Double precision,

        @NotNull(message = "La fecha de captura es obligatoria")
        Instant fechaCaptura

) {

    @Override
    public String toString() {
        // Las coordenadas no se escriben en los registros técnicos (RNF-03).
        return "UbicacionRequest[precision=" + precision + ", fechaCaptura=" + fechaCaptura + "]";
    }
}