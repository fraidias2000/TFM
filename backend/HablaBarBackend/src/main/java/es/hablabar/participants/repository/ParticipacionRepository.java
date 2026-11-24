package es.hablabar.participants.repository;

import es.hablabar.participants.entity.Participacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipacionRepository extends JpaRepository<Participacion, UUID> {

    /** Participación activa (sin fecha de salida) de un usuario en un chat. */
    Optional<Participacion> findByUsuarioIdAndChatIdAndFechaSalidaIsNull(UUID usuarioId, UUID chatId);

    /** Número de participantes activos de un chat (RN-01). */
    long countByChatIdAndFechaSalidaIsNull(UUID chatId);

    /** Participaciones activas de un chat, por orden de entrada. */
    List<Participacion> findByChatIdAndFechaSalidaIsNullOrderByFechaEntradaAsc(UUID chatId);

    /** Participaciones activas de un usuario en cualquier chat. */
    List<Participacion> findByUsuarioIdAndFechaSalidaIsNull(UUID usuarioId);
}
