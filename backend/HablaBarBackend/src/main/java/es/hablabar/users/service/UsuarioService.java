package es.hablabar.users.service;

import es.hablabar.users.entity.Usuario;
import es.hablabar.users.repository.UsuarioRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Punto de entrada público del módulo users. Los demás módulos (auth, chats...)
 * usan este servicio y nunca acceden directamente a UsuarioRepository.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        return usuarioRepository.buscarPorCorreo(correo).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.buscarPorCorreo(correo);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioRepository.findById(id);
    }

    /**
     * Alias de varios usuarios a la vez, indexados por su identificador.
     * Lo usa el módulo de chats para mostrar quién participa sin exponer el correo (RN-10).
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> aliasPorId(Collection<UUID> ids) {
        return usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getAlias));
    }

    /**
     * De los usuarios indicados, los que tienen activados los avisos de nuevos chats.
     * Lo usa el módulo de avisos para seleccionar los destinatarios (RN-08, RF-15).
     */
    @Transactional(readOnly = true)
    public Set<UUID> conAvisosActivados(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(usuarioRepository.idsConAvisosActivados(ids));
    }

    /**
     * Crea el usuario. saveAndFlush fuerza el INSERT en este momento para que,
     * si dos registros simultáneos usan el mismo correo, el índice único de la BD
     * lo detecte aquí y se responda con 409.
     */
    @Transactional
    public Usuario crear(String alias, String correo, String passwordHash) {
        return usuarioRepository.saveAndFlush(new Usuario(alias, correo, passwordHash));
    }

    /**
     * Cambia el alias y la preferencia de avisos del usuario (RF-15). Un valor nulo
     * deja el dato como estaba. Devuelve vacío si el usuario ya no existe.
     */
    @Transactional
    public Optional<Usuario> actualizar(UUID id, String alias, Boolean avisosActivados) {
        return usuarioRepository.findById(id).map(usuario -> {
            if (alias != null) {
                usuario.setAlias(alias);
            }
            if (avisosActivados != null) {
                usuario.setAvisosActivados(avisosActivados);
            }
            return usuario;
        });
    }
}
