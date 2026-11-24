package es.hablabar.chats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Conversación temporal asociada a un punto fijo. El punto de origen es la
 * ubicación del creador en el momento de crearla y no cambia aunque se desplace
 * (RN-04). Caduca una hora después de su creación (RN-06).
 */
@Entity
@Table(name = "chats")
public class Chat {

    /** RN-06: duración fija de una hora, sin ampliaciones. */
    public static final Duration DURACION = Duration.ofHours(1);

    /** RN-01: capacidad máxima de participantes simultáneos, incluido el creador. */
    public static final short CAPACIDAD_MAXIMA = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "creador_id", nullable = false, updatable = false)
    private UUID creadorId;

    @Column(name = "nombre", nullable = false, columnDefinition = "text")
    private String nombre;

    @Column(name = "tema", columnDefinition = "text")
    private String tema;

    @Column(name = "latitud_origen", nullable = false, updatable = false)
    private double latitudOrigen;

    @Column(name = "longitud_origen", nullable = false, updatable = false)
    private double longitudOrigen;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_caducidad", nullable = false, updatable = false)
    private Instant fechaCaducidad;

    @Column(name = "capacidad_maxima", nullable = false, updatable = false)
    private short capacidadMaxima;

    /** Constructor requerido por JPA. */
    protected Chat() {
    }

    public Chat(UUID creadorId, String nombre, String tema,
                double latitudOrigen, double longitudOrigen, Instant ahora) {
        this.creadorId = creadorId;
        this.nombre = nombre;
        this.tema = tema;
        this.latitudOrigen = latitudOrigen;
        this.longitudOrigen = longitudOrigen;
        // PostgreSQL guarda microsegundos: se trunca para que la comprobación
        // fecha_caducidad = fecha_creacion + 1 hora se cumpla exactamente.
        this.fechaCreacion = ahora.truncatedTo(ChronoUnit.MICROS);
        this.fechaCaducidad = this.fechaCreacion.plus(DURACION);
        this.capacidadMaxima = CAPACIDAD_MAXIMA;
    }

    /** El chat está activo hasta su fecha de caducidad, según la hora del servidor. */
    public boolean estaActivoEn(Instant instante) {
        return instante.isBefore(fechaCaducidad);
    }

    public boolean esCreador(UUID usuarioId) {
        return creadorId.equals(usuarioId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreadorId() {
        return creadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTema() {
        return tema;
    }

    public double getLatitudOrigen() {
        return latitudOrigen;
    }

    public double getLongitudOrigen() {
        return longitudOrigen;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaCaducidad() {
        return fechaCaducidad;
    }

    public short getCapacidadMaxima() {
        return capacidadMaxima;
    }

    @Override
    public String toString() {
        // Las coordenadas no se escriben en los registros técnicos (RNF-03).
        return "Chat[id=" + id + ", caducidad=" + fechaCaducidad + "]";
    }
}