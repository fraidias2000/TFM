package es.hablabar.chats.repository;

import es.hablabar.chats.entity.Chat;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /**
     * Chats activos cuyo punto de origen está dentro de un rectángulo de coordenadas.
     * Es un filtro previo y amplio; la distancia exacta se comprueba después en Java.
     */
    @Query("""
            SELECT c
            FROM Chat c
            WHERE c.fechaCaducidad > :ahora
              AND c.latitudOrigen BETWEEN :latitudMin AND :latitudMax
              AND c.longitudOrigen BETWEEN :longitudMin AND :longitudMax
            """)
    List<Chat> buscarActivosEnZona(
            @Param("ahora") Instant ahora,
            @Param("latitudMin") double latitudMin,
            @Param("latitudMax") double latitudMax,
            @Param("longitudMin") double longitudMin,
            @Param("longitudMax") double longitudMax);

    /**
     * Obtiene el chat bloqueando su fila hasta el final de la transacción. Así, si
     * varias personas intentan entrar a la vez, se atienden de una en una y no se
     * supera la capacidad (RN-01).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Chat c WHERE c.id = :id")
    Optional<Chat> bloquearPorId(@Param("id") UUID id);
}