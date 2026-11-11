package es.hablabar.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj del servidor. Se inyecta en lugar de llamar a Instant.now() para que las
 * reglas que dependen del tiempo (caducidad de ubicaciones y chats) se puedan probar.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}