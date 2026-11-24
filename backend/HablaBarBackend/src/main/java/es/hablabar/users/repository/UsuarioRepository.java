package es.hablabar.users.repository;

import es.hablabar.users.entity.Usuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("""
            SELECT u
            FROM Usuario u
            WHERE lower(trim(u.correo)) = lower(trim(:correo))
            """)
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);

    /** De los usuarios indicados, los que tienen activados los avisos de nuevos chats (RF-15). */
    @Query("""
            SELECT u.id
            FROM Usuario u
            WHERE u.id IN :ids
              AND u.avisosActivados = true
            """)
    List<UUID> idsConAvisosActivados(@Param("ids") Collection<UUID> ids);
}
