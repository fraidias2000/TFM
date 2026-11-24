package es.hablabar.users.controller;

import es.hablabar.users.dto.ActualizarUsuarioRequest;
import es.hablabar.users.dto.UsuarioResponse;
import es.hablabar.users.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Devuelve el perfil del usuario identificado por el token. */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        return usuarioService.buscarPorId(id)
                .map(UsuarioResponse::from)
                // Token válido de un usuario que ya no existe.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** Cambia el alias o la preferencia de avisos del usuario identificado por el token (RF-15). */
    @PatchMapping("/me")
    public UsuarioResponse actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        UUID id = UUID.fromString(jwt.getSubject());
        return usuarioService.actualizar(id, request.alias(), request.avisosActivados())
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
