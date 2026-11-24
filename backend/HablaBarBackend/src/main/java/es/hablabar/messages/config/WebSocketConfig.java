package es.hablabar.messages.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal WebSocket para recibir mensajes en tiempo real (RF-08, RNF-05).
 *
 * <p>El cliente abre una conexión en /ws, se autentica en el mensaje CONNECT con su
 * token y se suscribe a /user/queue/mensajes. El servidor envía a ese destino los
 * mensajes de todos los chats en los que el usuario participa. Los mensajes se
 * envían mediante la API REST, no por WebSocket.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Destino, relativo a cada usuario, en el que se reciben los mensajes. */
    public static final String DESTINO_MENSAJES = "/queue/mensajes";

    /** Destino al que se suscribe el cliente. */
    public static final String SUSCRIPCION_MENSAJES = "/user" + DESTINO_MENSAJES;

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