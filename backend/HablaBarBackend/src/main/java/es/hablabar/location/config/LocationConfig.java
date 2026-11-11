package es.hablabar.location.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas, como la limpieza de ubicaciones caducadas. */
@Configuration
@EnableScheduling
public class LocationConfig {
}