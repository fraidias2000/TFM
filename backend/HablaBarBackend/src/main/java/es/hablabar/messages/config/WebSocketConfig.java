package es.hablabar.messages.config;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal WebSocket para recibir mensajes y avisos en tiempo real (RF-06, RF-08, RNF-05).
 *
 * <p>El cliente abre una conexión en /ws, se autentica en el mensaje CONNECT con su
 * token y se suscribe a sus propios destinos: /user/queue/mensajes, con los mensajes
 * de los chats en los que participa, y /user/queue/avisos, con los avisos de nuevos
 * chats cercanos. Los mensajes se envían mediante la API REST, no por WebSocket.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Destino, relativo a cada usuario, en el que se reciben los mensajes. */
    public static final String DESTINO_MENSAJES = "/queue/mensajes";

    /** Destino, relativo a cada usuario, en el que se reciben los avisos de nuevos chats. */
    public static final String DESTINO_AVISOS = "/queue/avisos";

    /** Destinos a los que puede suscribirse el cliente: solo los suyos. */
    public static final Set<String> SUSCRIPCIONES_PERMITIDAS =
            Set.of("/user" + DESTINO_MENSAJES, "/user" + DESTINO_AVISOS);

    private final StompAutenticacionInterceptor autenticacion;
    private final String[] origenesPermitidos;

    public WebSocketConfig(
            StompAutenticacionInterceptor autenticacion,
            @Value("${app.cors.allowed-origins}") String origenesPermitidos) {
        this.autenticacion = autenticacion;
        this.origenesPermitidos = Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Mismos orígenes que la configuración CORS de la API.
        registry.addEndpoint("/ws").setAllowedOrigins(origenesPermitidos);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(autenticacion);
    }
}
