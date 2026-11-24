package es.hablabar.messages.model;

import es.hablabar.messages.dto.MensajeResponse;
import java.util.List;
import java.util.UUID;

/**
 * Evento interno que se publica al guardar un mensaje nuevo. Contiene el mensaje
 * y los usuarios que participaban en el chat en ese momento, que son quienes
 * deben recibirlo en tiempo real.
 */
public record MensajeEnviado(MensajeResponse mensaje, List<UUID> destinatarios) {
}