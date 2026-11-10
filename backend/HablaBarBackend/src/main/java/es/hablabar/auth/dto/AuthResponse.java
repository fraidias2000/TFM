package es.hablabar.auth.dto;

import es.hablabar.users.dto.UsuarioResponse;
import java.time.Instant;

/**
 * Respuesta de registro e inicio de sesión: el token que el cliente enviará
 * en la cabecera "Authorization: Bearer ...", su caducidad y el perfil del usuario.
 */
public record AuthResponse(
        String token,
        String tipo,
        Instant expiraEn,
        UsuarioResponse usuario
) {

    @Override
    public String toString() {
        return "AuthResponse[token protegido, usuario=" + usuario.id() + "]";
    }
}