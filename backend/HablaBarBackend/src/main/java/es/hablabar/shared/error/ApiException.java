package es.hablabar.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Excepción base para los errores de negocio. Cada módulo define sus propias
 * excepciones heredando de esta, y GlobalExceptionHandler las convierte en
 * ErrorResponse sin necesidad de conocerlas una a una.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}