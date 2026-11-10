package es.hablabar.users.repository;

import es.hablabar.users.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("""
            SELECT u
            FROM Usuario u
            WHERE lower(trim(u.correo)) = lower(trim(:correo))
            """)
    Optional<Usuario> buscarPorCorreo(@Param("correo") String correo);
}
