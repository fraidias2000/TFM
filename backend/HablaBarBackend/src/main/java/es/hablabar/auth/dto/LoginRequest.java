package es.hablabar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico debe tener un formato válido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 64, message = "La contraseña debe tener como máximo 64 caracteres")
        String password

) {

    /** Quita espacios al principio y al final del correo antes de validar. */
    public LoginRequest {
        correo = correo == null ? null : correo.trim();
    }
    
    @Override
    public String toString() {
        return "LoginRequest[datos protegidos]";
    }
}