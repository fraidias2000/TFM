package es.hablabar.location.dto;

import java.time.Instant;

/**
 * Confirmación de que la ubicación es válida. No devuelve coordenadas: solo hasta
 * cuándo puede usarse, para que el cliente sepa cuándo debe enviar una nueva.
 */
public record UbicacionResponse(Instant validaHasta) {
}