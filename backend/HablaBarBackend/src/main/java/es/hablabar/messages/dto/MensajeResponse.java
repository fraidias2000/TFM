package es.hablabar.messages.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje tal como lo reciben los participantes: se identifica al autor por su
 * identificador y su alias, sin correo ni ubicación (RN-10, RNF-03).
 */
public record MensajeResponse(
        UUID id,
        UUID idCliente,
        UUID chatId,
        UUID autorId,
        String alias,
        String contenido,
        Instant fechaEnvio
) {
}