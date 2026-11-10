package es.hablabar.users.dto;

import java.time.Instant;
import java.util.UUID;
import es.hablabar.users.entity.Usuario;

public record UsuarioResponse(
        UUID id,
        String alias,
        String correo,
        boolean avisosActivados,
        Instant fechaAlta
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getAlias(),
                usuario.getCorreo(),
                usuario.isAvisosActivados(),
                usuario.getFechaAlta()
        );
    }
}
