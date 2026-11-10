package es.hablabar.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "alias", nullable = false, columnDefinition = "text")
    private String alias;

    @Column(name = "correo", nullable = false, columnDefinition = "text")
    private String correo;

    @Column(
            name = "password_hash",
            nullable = false,
            columnDefinition = "text"
    )
    private String passwordHash;

    @Column(name = "avisos_activados", nullable = false)
    private boolean avisosActivados = true;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private Instant fechaAlta;

    /**
     * Constructor requerido por JPA.
     */
    protected Usuario() {
    }

    /**
     * Crea un usuario con la contraseña previamente convertida en hash.
     */
    public Usuario(String alias, String correo, String passwordHash) {
        this.alias = alias;
        this.correo = correo;
        this.passwordHash = passwordHash;
    }

    /**
     * Asigna la fecha de alta antes de insertar el usuario.
     */
    @PrePersist
    private void asignarFechaAlta() {
        if (fechaAlta == null) {
            fechaAlta = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isAvisosActivados() {
        return avisosActivados;
    }

    public Instant getFechaAlta() {
        return fechaAlta;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setAvisosActivados(boolean avisosActivados) {
        this.avisosActivados = avisosActivados;
    }
}
