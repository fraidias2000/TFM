package es.hablabar.chats.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Chat disponible en el entorno del usuario (RF-05). No expone mensajes ni
 * coordenadas. La distancia se da por tramos (25, 50 o 100 m) en lugar de en
 * metros exactos, para que no pueda deducirse la posición del creador
 * combinando consultas desde varios puntos (RNF-03).
 */
public record ChatCercanoResponse(
        UUID id,
        String nombre,
        String tema,
        int participantes,
        int capacidad,
        Instant expiraEn,
        int distanciaMaximaMetros,
        boolean participando
) {
}