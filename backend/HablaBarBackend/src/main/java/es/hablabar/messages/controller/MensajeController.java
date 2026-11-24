package es.hablabar.messages.controller;

import es.hablabar.messages.dto.EnviarMensajeRequest;
import es.hablabar.messages.dto.MensajeResponse;
import es.hablabar.messages.service.MensajeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/mensajes")
public class MensajeController {

    private final MensajeService mensajeService;

    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    /**
     * Envía un mensaje. Responde 201 si se ha guardado ahora y 200 si ya se había
     * recibido antes con el mismo identificador de cliente.
     */
    @PostMapping
    public ResponseEntity<MensajeResponse> enviar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID chatId,
            @Valid @RequestBody EnviarMensajeRequest request) {
        MensajeService.Resultado resultado = mensajeService.enviar(usuarioId(jwt), chatId, request);
        return ResponseEntity.status(resultado.nuevo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(resultado.mensaje());
    }

    /** Historial del chat, en orden de envío. */
    @GetMapping
    public List<MensajeResponse> historial(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId) {
        return mensajeService.historial(usuarioId(jwt), chatId);
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}