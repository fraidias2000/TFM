package es.hablabar.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Mensaje que se quiere enviar. El cliente genera idCliente y lo repite si reenvía
 * el mismo mensaje, para que no se guarde dos veces (RNF-06). El contenido se
 * guarda tal como lo escribe el usuario, sin recortar espacios.
 */
public record EnviarMensajeRequest(

        @NotNull(message = "El identificador del mensaje es obligatorio")
        UUID idCliente,

        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 1000, message = "El mensaje debe tener como máximo 1000 caracteres")
        String contenido

) {
}