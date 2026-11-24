package es.hablabar.shared.error;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones a respuestas ErrorResponse, sin exponer detalles internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de negocio lanzados por los servicios (409, 401, 404...). */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> negocio(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCodigo(), ex.getMessage()));
    }

    /** Campos que no cumplen las anotaciones @NotBlank, @Email, @Size... de los DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDACION", "Hay campos incorrectos", detalles));
    }

    /** JSON mal formado o ausente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("PETICION_MALFORMADA", "El cuerpo de la petición no es válido"));
    }

    /** Restricciones de la BD (por ejemplo, dos registros simultáneos con el mismo correo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICTO", "La operación entra en conflicto con datos existentes"));
    }

    /** Parámetro de la ruta con formato incorrecto (por ejemplo, un identificador que no es un UUID). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> parametroNoValido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("PARAMETRO_NO_VALIDO", "El parámetro '" + ex.getName() + "' no es válido"));
    }
}