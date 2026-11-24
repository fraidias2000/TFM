package es.hablabar.chats.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Información de un chat para quien participa en él. No incluye coordenadas
 * (RNF-03). El cliente calcula el tiempo restante a partir de expiraEn (RF-10).
 */
public record ChatResponse(
        UUID id,
        String nombre,
        String tema,
        int participantes,
        int capacidad,
        Instant creadoEn,
        Instant expiraEn,
        boolean creadoPorMi,
        boolean participando,
        Instant fechaEntrada
) {
}