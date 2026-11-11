package es.hablabar.location.exception;

import es.hablabar.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * La ubicación no cumple RN-03. Se responde con 422: la petición es correcta,
 * pero los datos no permiten comprobar la proximidad y hay que actualizarlos.
 */
public class UbicacionNoValidaException extends ApiException {

    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    private UbicacionNoValidaException(String codigo, String mensaje) {
        super(UNPROCESSABLE, codigo, mensaje);
    }

    public static UbicacionNoValidaException imprecisa() {
        return new UbicacionNoValidaException("UBICACION_IMPRECISA",
                "La ubicación no es suficientemente precisa. Se necesita una precisión de 20 m o mejor.");
    }

    public static UbicacionNoValidaException antigua() {
        return new UbicacionNoValidaException("UBICACION_ANTIGUA",
                "La ubicación está desactualizada. Debe tener menos de 30 segundos.");
    }

    public static UbicacionNoValidaException fechaNoValida() {
        return new UbicacionNoValidaException("UBICACION_FECHA_NO_VALIDA",
                "La fecha de la ubicación no es válida. Comprueba la hora del dispositivo.");
    }

    public static UbicacionNoValidaException noDisponible() {
        return new UbicacionNoValidaException("UBICACION_NO_DISPONIBLE",
                "No hay una ubicación reciente. Actualiza tu ubicación e inténtalo de nuevo.");
    }
}