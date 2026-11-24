package es.hablabar.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.hablabar.messages.config.StompAutenticacionInterceptor;
import java.security.Principal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Pruebas unitarias del control de acceso al canal WebSocket (RNF-01). */
class StompAutenticacionInterceptorTest {

    private static final String USUARIO = "6f1c2a3b-0000-4000-8000-000000000001";

    /** Decodificador de prueba: solo acepta el token "valido". */
    private final JwtDecoder decoder = token -> {
        if (!"valido".equals(token)) {
            throw new BadJwtException("Token no válido");
        }
        return Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .subject(USUARIO)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    };

    private final StompAutenticacionInterceptor interceptor = new StompAutenticacionInterceptor(decoder);

    @Test
    @DisplayName("CP-MSG-13: CONNECT con token válido asocia el usuario a la conexión")
    void connectValido() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valido");

        interceptor.preSend(mensaje(accessor), null);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo(USUARIO);
    }

    @Test
    @DisplayName("CP-MSG-13: CONNECT sin token o con token no válido se rechaza")
    void connectSinToken() {
        assertThatThrownBy(() -> interceptor.preSend(mensaje(accessor(StompCommand.CONNECT)), null))
                .isInstanceOf(MessageDeliveryException.class);

        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer manipulado");
        assertThatThrownBy(() -> interceptor.preSend(mensaje(accessor), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("CP-MSG-14: solo se permite suscribirse a los mensajes propios y no se aceptan SEND")
    void suscripcionesYEnvios() {
        Principal usuario = () -> USUARIO;

        StompHeaderAccessor propia = accessor(StompCommand.SUBSCRIBE);
        propia.setUser(usuario);
        propia.setDestination("/user/queue/mensajes");
        assertThat(interceptor.preSend(mensaje(propia), null)).isNotNull();

        StompHeaderAccessor ajena = accessor(StompCommand.SUBSCRIBE);
        ajena.setUser(usuario);
        ajena.setDestination("/queue/mensajes-user123");
        assertThatThrownBy(() -> interceptor.preSend(mensaje(ajena), null))
                .isInstanceOf(MessageDeliveryException.class);

        StompHeaderAccessor anonima = accessor(StompCommand.SUBSCRIBE);
        anonima.setDestination("/user/queue/mensajes");
        assertThatThrownBy(() -> interceptor.preSend(mensaje(anonima), null))
                .isInstanceOf(MessageDeliveryException.class);

        StompHeaderAccessor envio = accessor(StompCommand.SEND);
        envio.setUser(usuario);
        envio.setDestination("/app/chats");
        assertThatThrownBy(() -> interceptor.preSend(mensaje(envio), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private static StompHeaderAccessor accessor(StompCommand comando) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(comando);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private static Message<byte[]> mensaje(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
