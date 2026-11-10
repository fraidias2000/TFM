package es.hablabar.users.service;

import es.hablabar.users.entity.Usuario;
import es.hablabar.users.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
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
     * Crea el usuario. saveAndFlush fuerza el INSERT en este momento para que,
     * si dos registros simultáneos usan el mismo correo, el índice único de la BD
     * lo detecte aquí y se responda con 409.
     */
    @Transactional
    public Usuario crear(String alias, String correo, String passwordHash) {
        return usuarioRepository.saveAndFlush(new Usuario(alias, correo, passwordHash));
    }
}