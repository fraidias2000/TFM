package es.hablabar.users.dto;

import jakarta.validation.constraints.Size;

/**
 * Cambios del perfil propio. Los dos campos son opcionales: solo se modifica lo
 * que se envía. El alias se guarda sin espacios al principio ni al final.
 */
public record ActualizarUsuarioRequest(

        @Size(min = 1, max = 50, message = "El alias debe tener entre 1 y 50 caracteres")
        String alias,

        /** RF-15: preferencia de avisos de nuevos chats cercanos. */
        Boolean avisosActivados

) {

    public ActualizarUsuarioRequest {
        alias = alias == null ? null : alias.trim();
    }
}
