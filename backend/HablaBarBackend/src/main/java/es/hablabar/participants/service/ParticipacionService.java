package es.hablabar.participants.service;

import es.hablabar.participants.entity.Participacion;
import es.hablabar.participants.repository.ParticipacionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Punto de entrada público del módulo participants. Registra entradas y salidas
 * y responde a consultas sobre quién está en cada chat. No aplica reglas de
 * negocio de los chats (proximidad, aforo, caducidad): de eso se encarga ChatService.
 */
@Service
public class ParticipacionService {

    private final ParticipacionRepository participacionRepository;

    public ParticipacionService(ParticipacionRepository participacionRepository) {
        this.participacionRepository = participacionRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Participacion> activa(UUID usuarioId, UUID chatId) {
        return participacionRepository.findByUsuarioIdAndChatIdAndFechaSalidaIsNull(usuarioId, chatId);
    }

    @Transactional(readOnly = true)
    public boolean participa(UUID usuarioId, UUID chatId) {
        return activa(usuarioId, chatId).isPresent();
    }

    @Transactional(readOnly = true)
    public long contarActivas(UUID chatId) {
        return participacionRepository.countByChatIdAndFechaSalidaIsNull(chatId);
    }

    /** Identificadores de los usuarios que están dentro del chat, por orden de entrada. */
    @Transactional(readOnly = true)
    public List<UUID> participantesActivos(UUID chatId) {
        return participacionRepository.findByChatIdAndFechaSalidaIsNullOrderByFechaEntradaAsc(chatId)
                .stream()
                .map(Participacion::getUsuarioId)
                .toList();
    }

    /** Chats en los que el usuario tiene una participación activa. */
    @Transactional(readOnly = true)
    public List<Participacion> activasDelUsuario(UUID usuarioId) {
        return participacionRepository.findByUsuarioIdAndFechaSalidaIsNull(usuarioId);
    }

    
        /**
     * Usuario de cada participación, indexado por el identificador de la participación.
     * Lo usa el módulo de mensajes para saber quién escribió cada mensaje.
     */
    @Transactional(readOnly = true)
    public Map<UUID, UUID> usuariosPorParticipacion(Collection<UUID> participacionIds) {
        return participacionRepository.findAllById(participacionIds).stream()
                .collect(Collectors.toMap(Participacion::getId, Participacion::getUsuarioId));
    }
    
    
    /**
     * Crea una participación nueva. saveAndFlush hace que, si dos peticiones intentan
     * crear a la vez la misma participación activa, el índice único de la BD lo
     * detecte aquí y se responda con 409.
     */
    @Transactional
    public Participacion registrarEntrada(UUID usuarioId, UUID chatId, Instant fecha) {
        return participacionRepository.saveAndFlush(new Participacion(usuarioId, chatId, fecha));
    }

    /** Cierra la participación activa, si existe. Salir dos veces no es un error. */
    @Transactional
    public void registrarSalida(UUID usuarioId, UUID chatId, Instant fecha) {
        activa(usuarioId, chatId).ifPresent(participacion -> participacion.registrarSalida(fecha));
    }
}