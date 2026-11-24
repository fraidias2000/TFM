package es.hablabar.chats.exception;

import es.hablabar.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Errores de negocio del módulo de chats. */
public class ChatException extends ApiException {

    private ChatException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static ChatException noEncontrado() {
        return new ChatException(HttpStatus.NOT_FOUND, "CHAT_NO_ENCONTRADO",
                "El chat no existe.");
    }

    /** RF-13: un chat caducado rechaza entradas y consultas. */
    public static ChatException finalizado() {
        return new ChatException(HttpStatus.GONE, "CHAT_FINALIZADO",
                "El chat ha finalizado.");
    }

    /** RN-02: la distancia al punto de origen debe ser inferior a 100 m. */
    public static ChatException fueraDeRadio() {
        return new ChatException(HttpStatus.FORBIDDEN, "FUERA_DE_RADIO",
                "Estás demasiado lejos del chat. Debes estar a menos de 100 metros para entrar.");
    }

    /** RN-01: máximo de 20 participantes simultáneos. */
    public static ChatException completo() {
        return new ChatException(HttpStatus.CONFLICT, "CHAT_COMPLETO",
                "El chat está completo. Inténtalo de nuevo cuando alguien salga.");
    }

    /** RF-11: sin participación activa no se puede consultar el chat. */
    public static ChatException noParticipa() {
        return new ChatException(HttpStatus.FORBIDDEN, "NO_PARTICIPA",
                "No participas en este chat. Solicita entrar de nuevo.");
    }
}
