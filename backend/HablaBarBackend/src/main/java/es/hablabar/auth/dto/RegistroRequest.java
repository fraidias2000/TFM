package es.hablabar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank(message = "El alias es obligatorio")
        @Size(max = 50, message = "El alias debe tener como máximo 50 caracteres")
        String alias,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido")
        @Size(max = 254, message = "El correo debe tener como máximo 254 caracteres")
        String correo,

        // BCrypt solo tiene en cuenta los primeros 72 bytes; se limita a 64 caracteres
        // para no superar ese límite con caracteres acentuados.
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String password

) {
    /** Quita espacios al principio y al final antes de validar (autocompletado del teclado). */
    public RegistroRequest {
        alias = alias == null ? null : alias.trim();
        correo = correo == null ? null : correo.trim();
    }

    @Override
    public String toString() {
        return "RegistroRequest[datos protegidos]";
    }
}