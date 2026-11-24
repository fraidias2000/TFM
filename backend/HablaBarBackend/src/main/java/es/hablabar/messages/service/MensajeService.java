package es.hablabar.messages.service;

import es.hablabar.chats.service.ChatService;
import es.hablabar.messages.dto.EnviarMensajeRequest;
import es.hablabar.messages.dto.MensajeResponse;
import es.hablabar.messages.entity.Mensaje;
import es.hablabar.messages.exception.MensajeException;
import es.hablabar.messages.model.MensajeEnviado;
import es.hablabar.messages.repository.MensajeRepository;
import es.hablabar.participants.entity.Participacion;
import es.hablabar.participants.service.ParticipacionService;
import es.hablabar.users.service.UsuarioService;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de los mensajes: enviar y consultar el historial (RF-08, RN-07).
 * El acceso se comprueba en el servidor con las mismas reglas que el detalle del
 * chat: el chat debe estar activo y el usuario debe participar (RF-11, RF-13, RNF-01).
 */
@Service
public class MensajeService {

    /** Resultado de un envío: el mensaje y si se ha guardado ahora o ya existía. */
    public record Resultado(MensajeResponse mensaje, boolean nuevo) {
    }

    private final MensajeRepository mensajeRepository;
    private final ChatService chatService;
    private final ParticipacionService participacionService;
    private final UsuarioService usuarioService;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public MensajeService(
            MensajeRepository mensajeRepository,
            ChatService chatService,
            ParticipacionService participacionService,
            UsuarioService usuarioService,
            ApplicationEventPublisher eventos,
            Clock clock) {
        this.mensajeRepository = mensajeRepository;
        this.chatService = chatService;
        this.participacionService = participacionService;
        this.usuarioService = usuarioService;
        this.eventos = eventos;
        this.clock = clock;
    }

    /**
     * RF-08: guarda el mensaje y lo hace llegar a los participantes activos. La
     * proximidad no se exige: alejarse no impide seguir hablando (RN-05). Si ya
     * existe un mensaje con el mismo identificador de cliente, se devuelve ese
     * mensaje sin guardarlo ni enviarlo otra vez (RNF-06).
     */
    @Transactional
    public Resultado enviar(UUID usuarioId, UUID chatId, EnviarMensajeRequest request) {
        Participacion participacion = chatService.exigirParticipacionActiva(usuarioId, chatId);
        String alias = alias(usuarioId);

        Optional<Mensaje> previo = mensajeRepository.findByIdCliente(request.idCliente());
        if (previo.isPresent()) {
            Mensaje mensaje = previo.get();
            if (!mensaje.getParticipacionId().equals(participacion.getId())) {
                throw MensajeException.idClienteEnUso();
            }
            return new Resultado(respuesta(mensaje, chatId, usuarioId, alias), false);
        }

        Mensaje mensaje = mensajeRepository.saveAndFlush(new Mensaje(
                participacion.getId(), request.contenido(), clock.instant(), request.idCliente()));
        MensajeResponse respuesta = respuesta(mensaje, chatId, usuarioId, alias);

        // DifusorMensajes lo envía por WebSocket cuando la transacción se confirma.
        eventos.publishEvent(new MensajeEnviado(respuesta, participacionService.participantesActivos(chatId)));
        return new Resultado(respuesta, true);
    }

    /**
     * RN-07: historial completo del chat mientras está activo, en orden de envío.
     * Permite recuperar los mensajes no recibidos tras una desconexión (RF-14).
     */
    @Transactional(readOnly = true)
    public List<MensajeResponse> historial(UUID usuarioId, UUID chatId) {
        chatService.exigirParticipacionActiva(usuarioId, chatId);

        List<Mensaje> mensajes = mensajeRepository.historial(chatId);
        Set<UUID> participaciones = mensajes.stream()
                .map(Mensaje::getParticipacionId)
                .collect(Collectors.toSet());
        Map<UUID, UUID> autorPorParticipacion = participacionService.usuariosPorParticipacion(participaciones);
        Map<UUID, String> aliasPorUsuario = usuarioService.aliasPorId(new HashSet<>(autorPorParticipacion.values()));

        return mensajes.stream()
                .map(mensaje -> {
                    UUID autorId = autorPorParticipacion.get(mensaje.getParticipacionId());
                    return respuesta(mensaje, chatId, autorId, aliasPorUsuario.get(autorId));
                })
                .toList();
    }

    private String alias(UUID usuarioId) {
        return usuarioService.aliasPorId(List.of(usuarioId)).get(usuarioId);
    }

    private static MensajeResponse respuesta(Mensaje mensaje, UUID chatId, UUID autorId, String alias) {
        return new MensajeResponse(
                mensaje.getId(),
                mensaje.getIdCliente(),
                chatId,
                autorId,
                alias,
                mensaje.getContenido(),
                mensaje.getFechaEnvio());
    }
}