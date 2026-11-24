package es.hablabar.messages.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Controla el acceso al canal WebSocket (RNF-01):
 * <ul>
 *   <li>CONNECT: exige un token válido en la cabecera Authorization y asocia el
 *       usuario a la conexión.</li>
 *   <li>SUBSCRIBE: solo se permite la suscripción a los destinos propios
 *       (/user/queue/mensajes y /user/queue/avisos) y con un usuario autenticado.</li>
 *   <li>SEND: se rechaza, porque los mensajes se envían mediante la API REST, que
 *       aplica todas las validaciones.</li>
 * </ul>
 */
@Component
public class StompAutenticacionInterceptor implements ChannelInterceptor {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public StompAutenticacionInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT, STOMP -> accessor.setUser(autenticar(accessor.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> {
                String destino = accessor.getDestination();
                if (accessor.getUser() == null
                        || destino == null
                        || !WebSocketConfig.SUSCRIPCIONES_PERMITIDAS.contains(destino)) {
                    throw new MessageDeliveryException("Suscripción no permitida");
                }
            }
            case SEND -> throw new MessageDeliveryException("Los mensajes se envían mediante la API REST");
            default -> {
                // DISCONNECT, UNSUBSCRIBE, etc. no necesitan comprobaciones.
            }
        }
        return message;
    }

    private JwtAuthenticationToken autenticar(String cabecera) {
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            throw new MessageDeliveryException("Falta el token de acceso");
        }
        try {
            Jwt jwt = jwtDecoder.decode(cabecera.substring(PREFIJO_BEARER.length()));
            // getName() devuelve el "sub" del token, que es el identificador del usuario.
            return new JwtAuthenticationToken(jwt);
        } catch (JwtException e) {
            throw new MessageDeliveryException("Token de acceso no válido");
        }
    }
}
