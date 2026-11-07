package es.hablabar.shared;

import java.time.Instant;

public record HealthResponse(String status, String application, Instant timestamp) {
}
