package es.hablabar.notifications.service;

import es.hablabar.chats.model.ChatCreado;
import es.hablabar.chats.service.ChatService;
import es.hablabar.location.model.UbicacionReciente;
import es.hablabar.location.service.DistanciaService;
import es.hablabar.location.service.UbicacionService;
import es.hablabar.messages.config.WebSocketConfig;
import es.hablabar.notifications.dto.AvisoChatCercanoResponse;
import es.hablabar.notifications.model.Destinatario;
import es.hablabar.users.service.UsuarioService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisos de nuevos chats cercanos (RF-06, RN-08).
 *
 * <p>Cuando se confirma la creación de un chat, se seleccionan los destinatarios y
 * se les envía un aviso por WebSocket. Como el evento se publica una sola vez por
 * chat, cada usuario recibe como mucho un aviso de cada chat. El aviso no da
 * acceso: al entrar, el servidor vuelve a comprobar todas las condiciones (RF-07).
 */
@Service
public class AvisoService {

    private static final Logger log = LoggerFactory.getLogger(AvisoService.class);

    private final UbicacionService ubicacionService;
    private final DistanciaService distanciaService;
    private final UsuarioService usuarioService;
    private final SimpMessagingTemplate mensajeria;

    public AvisoService(
            UbicacionService ubicacionService,
            DistanciaService distanciaService,
            UsuarioService usuarioService,
            SimpMessagingTemplate mensajeria) {
        this.ubicacionService = ubicacionService;
        this.distanciaService = distanciaService;
        this.usuarioService = usuarioService;
        this.mensajeria = mensajeria;
    }

    /**
     * Se ejecuta después de confirmar la creación del chat, en una transacción de
     * solo lectura propia (la del chat ya ha terminado).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void chatCreado(ChatCreado chat) {
        List<Destinatario> destinatarios = destinatarios(chat);
        for (Destinatario destinatario : destinatarios) {
            mensajeria.convertAndSendToUser(
                    destinatario.usuarioId().toString(),
                    WebSocketConfig.DESTINO_AVISOS,
                    aviso(chat, destinatario));
        }
        // Solo se registra el número de avisos, nunca quién los recibe ni dónde está (RNF-03).
        log.info("Avisos enviados para el chat {}: {}", chat.chatId(), destinatarios.size());
    }

    /**
     * RN-08: usuarios con ubicación válida (RN-03) a menos de 100 m del punto de
     * origen (RN-02) y con los avisos activados (RF-15), excluido el creador.
     * Ordenados del más cercano al más lejano.
     */
    public List<Destinatario> destinatarios(ChatCreado chat) {
        Map<UUID, Double> cercanos = new HashMap<>();
        ubicacionService.validas().forEach((usuarioId, ubicacion) -> {
            if (usuarioId.equals(chat.creadorId())) {
                return;
            }
            double distancia = distancia(chat, ubicacion);
            if (distancia < ChatService.RADIO_METROS) {
                cercanos.put(usuarioId, distancia);
            }
        });

        Set<UUID> conAvisos = usuarioService.conAvisosActivados(cercanos.keySet());
        return cercanos.entrySet().stream()
                .filter(entrada -> conAvisos.contains(entrada.getKey()))
                .map(entrada -> new Destinatario(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparingDouble(Destinatario::distanciaMetros))
                .toList();
    }

    private double distancia(ChatCreado chat, UbicacionReciente ubicacion) {
        return distanciaService.metros(
                ubicacion.latitud(), ubicacion.longitud(), chat.latitudOrigen(), chat.longitudOrigen());
    }

    private static AvisoChatCercanoResponse aviso(ChatCreado chat, Destinatario destinatario) {
        return new AvisoChatCercanoResponse(
                chat.chatId(),
                chat.nombre(),
                chat.tema(),
                ChatService.distanciaAproximada(destinatario.distanciaMetros()),
                // Recién creado, solo participa el creador.
                1,
                chat.capacidad(),
                chat.expiraEn());
    }
}
