package es.hablabar.chats.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento interno que se publica al crear un chat. Lo escucha el módulo de avisos
 * para notificar a los usuarios cercanos (RF-06), sin que el módulo de chats
 * dependa de él. Incluye el punto de origen para calcular distancias, pero ese
 * dato no sale nunca del servidor (RNF-03).
 */
public record ChatCreado(
        UUID chatId,
        UUID creadorId,
        String nombre,
        String tema,
        double latitudOrigen,
        double longitudOrigen,
        int capacidad,
        Instant expiraEn
) {

    @Override
    public String toString() {
        // Las coordenadas no se escriben en los registros técnicos (RNF-03).
        return "ChatCreado[chatId=" + chatId + "]";
    }
}
