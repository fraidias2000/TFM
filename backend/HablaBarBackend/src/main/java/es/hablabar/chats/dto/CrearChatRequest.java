package es.hablabar.chats.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un chat. El creador, el punto de origen y la caducidad
 * los establece el servidor, no el cliente.
 */
public record CrearChatRequest(

        @NotBlank(message = "El nombre del chat es obligatorio")
        @Size(max = 50, message = "El nombre debe tener como máximo 50 caracteres")
        String nombre,

        @Size(max = 100, message = "El tema debe tener como máximo 100 caracteres")
        String tema

) {

    /** Quita espacios sobrantes; un tema vacío se guarda como ausente. */
    public CrearChatRequest {
        nombre = nombre == null ? null : nombre.trim();
        tema = tema == null || tema.isBlank() ? null : tema.trim();
    }
}
