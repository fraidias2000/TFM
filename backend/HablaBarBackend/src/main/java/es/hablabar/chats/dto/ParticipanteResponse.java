package es.hablabar.chats.dto;

import java.util.UUID;

/**
 * Participante visible dentro de un chat: solo su identificador y su alias.
 * El correo no se muestra a otros participantes (RF-09, RN-10).
 */
public record ParticipanteResponse(UUID id, String alias, boolean esCreador) {
}