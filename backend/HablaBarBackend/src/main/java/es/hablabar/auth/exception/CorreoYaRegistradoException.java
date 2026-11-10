package es.hablabar.auth.exception;

import es.hablabar.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class CorreoYaRegistradoException extends ApiException {

    public CorreoYaRegistradoException() {
        super(HttpStatus.CONFLICT, "CORREO_REGISTRADO",
                "Ya existe una cuenta con ese correo electrónico");
    }
}