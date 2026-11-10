package es.hablabar.auth.exception;

import es.hablabar.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ApiException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS",
                "El correo o la contraseña no son correctos");
    }
}