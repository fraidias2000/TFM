package es.hablabar.participants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrada de un usuario en un chat. Cada entrada crea una participación nueva;
 * al salir se registra la fecha de salida. Mientras fecha_salida es nula, la
 * participación está activa (RF-11, RF-12).
 *
 * <p>Se guardan los identificadores del usuario y del chat, no las entidades,
 * para que el módulo participants no dependa de los módulos users y chats.
 */
@Entity
@Table(name = "participaciones")
public class Participacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private UUID chatId;

    @Column(name = "fecha_entrada", nullable = false, updatable = false)
    private Instant fechaEntrada;

    @Column(name = "fecha_salida")
    private Instant fechaSalida;

    /** Constructor requerido por JPA. */
    protected Participacion() {
    }

    public Participacion(UUID usuarioId, UUID chatId, Instant fechaEntrada) {
        this.usuarioId = usuarioId;
        this.chatId = chatId;
        this.fechaEntrada = fechaEntrada;
    }

    public void registrarSalida(Instant fecha) {
        this.fechaSalida = fecha;
    }

    public boolean estaActiva() {
        return fechaSalida == null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getChatId() {
        return chatId;
    }

    public Instant getFechaEntrada() {
        return fechaEntrada;
    }

    public Instant getFechaSalida() {
        return fechaSalida;
    }
}
