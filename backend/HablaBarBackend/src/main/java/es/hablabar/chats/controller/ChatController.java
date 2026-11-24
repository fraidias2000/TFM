package es.hablabar.chats.controller;

import es.hablabar.chats.dto.ChatCercanoResponse;
import es.hablabar.chats.dto.ChatResponse;
import es.hablabar.chats.dto.CrearChatRequest;
import es.hablabar.chats.dto.ParticipanteResponse;
import es.hablabar.chats.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Crea un chat en la ubicación actual del usuario. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearChatRequest request) {
        return chatService.crear(usuarioId(jwt), request);
    }

    /** Chats disponibles a menos de 100 m de la ubicación actual del usuario. */
    @GetMapping("/cercanos")
    public List<ChatCercanoResponse> cercanos(@AuthenticationPrincipal Jwt jwt) {
        return chatService.cercanos(usuarioId(jwt));
    }

    /** Chats activos en los que el usuario participa. */
    @GetMapping("/participando")
    public List<ChatResponse> participando(@AuthenticationPrincipal Jwt jwt) {
        return chatService.participando(usuarioId(jwt));
    }

    /** Detalle de un chat en el que el usuario participa. */
    @GetMapping("/{chatId}")
    public ChatResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId) {
        return chatService.detalle(usuarioId(jwt), chatId);
    }

    /** Participantes activos de un chat en el que el usuario participa. */
    @GetMapping("/{chatId}/participantes")
    public List<ParticipanteResponse> participantes(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId) {
        return chatService.participantes(usuarioId(jwt), chatId);
    }

    /** Entra en un chat. Si ya participa, devuelve su participación actual. */
    @PostMapping("/{chatId}/participacion")
    public ChatResponse entrar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId) {
        return chatService.entrar(usuarioId(jwt), chatId);
    }

    /** Sale de un chat. */
    @DeleteMapping("/{chatId}/participacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chatId) {
        chatService.salir(usuarioId(jwt), chatId);
    }

    private static UUID usuarioId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}