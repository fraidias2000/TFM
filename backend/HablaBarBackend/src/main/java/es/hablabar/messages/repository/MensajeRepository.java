package es.hablabar.messages.repository;

import es.hablabar.messages.entity.Mensaje;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    /** Mensaje ya guardado con ese identificador de cliente, si existe (RNF-06). */
    Optional<Mensaje> findByIdCliente(UUID idCliente);

    /** Historial de un chat en orden de envío (RN-07). */
    @Query("""
            SELECT m
            FROM Mensaje m
            WHERE m.participacionId IN (
                SELECT p.id FROM Participacion p WHERE p.chatId = :chatId)
            ORDER BY m.fechaEnvio ASC, m.id ASC
            """)
    List<Mensaje> historial(@Param("chatId") UUID chatId);

    /**
     * Elimina los mensajes de los chats que han caducado antes del instante indicado
     * (RF-16, RN-07). Devuelve el número de mensajes eliminados.
     */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM mensajes m
            USING participaciones p, chats c
            WHERE m.participacion_id = p.id
              AND p.chat_id = c.id
              AND c.fecha_caducidad <= :limite
            """, nativeQuery = true)
    int eliminarDeChatsCaducados(@Param("limite") Instant limite);
}