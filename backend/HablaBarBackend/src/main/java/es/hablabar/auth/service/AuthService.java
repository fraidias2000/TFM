package es.hablabar.auth.service;

import es.hablabar.auth.dto.AuthResponse;
import es.hablabar.auth.dto.LoginRequest;
import es.hablabar.auth.dto.RegistroRequest;
import es.hablabar.auth.exception.CorreoYaRegistradoException;
import es.hablabar.auth.exception.CredencialesInvalidasException;
import es.hablabar.users.dto.UsuarioResponse;
import es.hablabar.users.entity.Usuario;
import es.hablabar.users.service.UsuarioService;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * Hash de una contraseña que no pertenece a nadie. Se compara contra él cuando
     * el correo no existe, para que la respuesta tarde lo mismo exista o no la cuenta
     * y no se pueda averiguar qué correos están registrados.
     */
    private final String hashFicticio;

    public AuthService(
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashFicticio = passwordEncoder.encode("hash-ficticio-para-igualar-tiempos");
    }

    /** RF-01: registra una cuenta y devuelve directamente la sesión iniciada. */
    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String correo = normalizarCorreo(request.correo());
        if (usuarioService.existeCorreo(correo)) {
            throw new CorreoYaRegistradoException();
        }
        Usuario usuario = usuarioService.crear(
                request.alias().trim(),
                correo,
                passwordEncoder.encode(request.password()));
        return crearRespuesta(usuario);
    }

    /** RF-02: comprueba las credenciales y emite un token de acceso. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<Usuario> usuario = usuarioService.buscarPorCorreo(normalizarCorreo(request.correo()));

        String hash = usuario.map(Usuario::getPasswordHash).orElse(hashFicticio);
        boolean passwordCorrecta = passwordEncoder.matches(request.password(), hash);

        // Mismo error si el correo no existe o la contraseña falla.
        if (usuario.isEmpty() || !passwordCorrecta) {
            throw new CredencialesInvalidasException();
        }
        return crearRespuesta(usuario.get());
    }

    private AuthResponse crearRespuesta(Usuario usuario) {
        TokenService.TokenGenerado token = tokenService.generar(usuario);
        return new AuthResponse(
                token.valor(),
                "Bearer",
                token.expiraEn(),
                UsuarioResponse.from(usuario));
    }

    private static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
