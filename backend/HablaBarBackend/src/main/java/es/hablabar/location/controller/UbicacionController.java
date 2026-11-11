package es.hablabar.location.controller;

import es.hablabar.location.dto.UbicacionRequest;
import es.hablabar.location.dto.UbicacionResponse;
import es.hablabar.location.model.UbicacionReciente;
import es.hablabar.location.service.UbicacionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ubicacion")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    /** Actualiza la ubicación del usuario autenticado. Sustituye a la anterior. */
    @PutMapping
    public UbicacionResponse actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UbicacionRequest request) {
        UbicacionReciente ubicacion = ubicacionService.registrar(usuarioId(jwt), request);
        return new UbicacionResponse(ubicacion.validaHasta());
    }

    /** Elimina la ubicación del usuario autenticado (por ejemplo, al cerrar sesión). */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Jwt jwt) {
        ubicacionService.eliminar(usuarioId(jwt));
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}