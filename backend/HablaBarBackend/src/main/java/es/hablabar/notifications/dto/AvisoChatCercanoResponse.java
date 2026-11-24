package es.hablabar.notifications.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Aviso de un nuevo chat cercano (RF-06). Como en la lista de chats cercanos, la
 * distancia se da por tramos (25, 50 o 100 m) y no se incluyen coordenadas ni
 * datos del creador (RNF-03).
 */
public record AvisoChatCercanoResponse(
        UUID chatId,
        String nombre,
        String tema,
        int distanciaMaximaMetros,
        int participantes,
        int capacidad,
        Instant expiraEn
) {
}
