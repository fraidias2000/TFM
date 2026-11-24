package es.hablabar.notifications.model;

import java.util.UUID;

/** Usuario seleccionado para recibir un aviso y su distancia exacta al chat (solo en el servidor). */
public record Destinatario(UUID usuarioId, double distanciaMetros) {
}
