package es.hablabar.auth.service;

import es.hablabar.users.entity.Usuario;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Genera los tokens de acceso. El "subject" del token es el id del usuario;
 * a partir de él el servidor identifica quién hace cada petición.
 */
@Service
public class TokenService {

    private static final String EMISOR = "hablabar";

    private final JwtEncoder jwtEncoder;
    private final Duration duracion;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.duracion = Duration.ofMinutes(expiracionMinutos);
    }

    public TokenGenerado generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(duracion);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .subject(usuario.getId().toString())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();

        String valor = jwtEncoder
                .encode(JwtEncoderParameters.from(cabecera, claims))
                .getTokenValue();
        return new TokenGenerado(valor, expiraEn);
    }

    public record TokenGenerado(String valor, Instant expiraEn) {
    }
}
