package es.hablabar.messages.service;

import es.hablabar.messages.repository.MensajeRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Elimina periódicamente los mensajes de los chats caducados (RF-16). Se ejecuta
 * cada 10 minutos, muy por debajo del plazo máximo de 24 horas de RN-07. El
 * historial deja de ser accesible en el momento de la caducidad, aunque los
 * mensajes se borren unos minutos después.
 */
@Component
public class LimpiezaMensajes {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaMensajes.class);

    private final MensajeRepository mensajeRepository;
    private final Clock clock;

    public LimpiezaMensajes(MensajeRepository mensajeRepository, Clock clock) {
        this.mensajeRepository = mensajeRepository;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 600_000)
    public void programada() {
        eliminarMensajesCaducados();
    }

    /** Devuelve el número de mensajes eliminados. Solo se registra la cantidad, nunca el contenido. */
    public int eliminarMensajesCaducados() {
        int eliminados = mensajeRepository.eliminarDeChatsCaducados(clock.instant());
        if (eliminados > 0) {
            log.info("Eliminados {} mensajes de chats caducados", eliminados);
        }
        return eliminados;
    }
}