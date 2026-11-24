package es.hablabar.messages.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Mensaje de texto enviado en un chat. El autor y el chat se obtienen a través
 * de la participación desde la que se envió (RF-08).
 */
@Entity
@Table(name = "mensajes")
public class Mensaje {

    /** RF-08: longitud máxima del contenido. */
    public static final int LONGITUD_MAXIMA = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "participacion_id", nullable = false, updatable = false)
    private UUID participacionId;

    @Column(name = "contenido", nullable = false, updatable = false, length = LONGITUD_MAXIMA)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private Instant fechaEnvio;

    /** Identificador generado por el cliente para no duplicar un reenvío (RNF-06). */
    @Column(name = "id_cliente", nullable = false, updatable = false)
    private UUID idCliente;

    /** Constructor requerido por JPA. */
    protected Mensaje() {
    }

    public Mensaje(UUID participacionId, String contenido, Instant fechaEnvio, UUID idCliente) {
        this.participacionId = participacionId;
        this.contenido = contenido;
        // PostgreSQL guarda microsegundos: se trunca para que la fecha devuelta
        // coincida con la que se lee después de la BD.
        this.fechaEnvio = fechaEnvio.truncatedTo(ChronoUnit.MICROS);
        this.idCliente = idCliente;
    }

    public UUID getId() {
        return id;
    }

    public UUID getParticipacionId() {
        return participacionId;
    }

    public String getContenido() {
        return contenido;
    }

    public Instant getFechaEnvio() {
        return fechaEnvio;
    }

    public UUID getIdCliente() {
        return idCliente;
    }

    @Override
    public String toString() {
        // El contenido no se escribe en los registros técnicos (RNF-03).
        return "Mensaje[id=" + id + ", fechaEnvio=" + fechaEnvio + "]";
    }
}