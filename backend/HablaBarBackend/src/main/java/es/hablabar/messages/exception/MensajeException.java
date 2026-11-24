package es.hablabar.messages.exception;

import es.hablabar.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Errores de negocio del módulo de mensajes. */
public class MensajeException extends ApiException {

    private MensajeException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** El identificador de cliente ya se usó para otro mensaje de otra participación. */
    public static MensajeException idClienteEnUso() {
        return new MensajeException(HttpStatus.CONFLICT, "ID_CLIENTE_EN_USO",
                "El identificador del mensaje ya está en uso.");
    }
}