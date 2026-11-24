package es.hablabar.messages.service;

import es.hablabar.messages.config.WebSocketConfig;
import es.hablabar.messages.model.MensajeEnviado;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía cada mensaje nuevo por WebSocket a los participantes activos del chat
 * (RF-08, RNF-05). Se ejecuta después de confirmar la transacción, para no
 * anunciar un mensaje que finalmente no se haya guardado.
 *
 * <p>Cada usuario recibe los mensajes en su propio destino (/user/queue/mensajes).
 * Como los destinatarios se calculan en el servidor en el momento del envío, quien
 * ha salido del chat deja de recibir mensajes aunque mantenga la conexión (RF-11).
 */
@Component
public class DifusorMensajes {

    private final SimpMessagingTemplate mensajeria;

    public DifusorMensajes(SimpMessagingTemplate mensajeria) {
        this.mensajeria = mensajeria;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void difundir(MensajeEnviado evento) {
        for (UUID destinatario : evento.destinatarios()) {
            mensajeria.convertAndSendToUser(
                    destinatario.toString(), WebSocketConfig.DESTINO_MENSAJES, evento.mensaje());
        }
    }
}