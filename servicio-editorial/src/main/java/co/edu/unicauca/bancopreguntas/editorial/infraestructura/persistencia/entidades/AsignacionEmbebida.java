package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de {@code proceso_asignacion}: una AsignacionDeRevisor.
 */
@Embeddable
public class AsignacionEmbebida {

    @Column(name = "revisor_id", nullable = false)
    private UUID revisorId;

    @Column(name = "fecha_asignacion", nullable = false)
    private Instant fechaAsignacion;

    /**
     * Constructor que exige JPA.
     */
    protected AsignacionEmbebida() {
    }

    /**
     * Crea la fila.
     *
     * @param revisorId       Revisor
     * @param fechaAsignacion instante de la asignación
     */
    public AsignacionEmbebida(UUID revisorId, Instant fechaAsignacion) {
        this.revisorId = revisorId;
        this.fechaAsignacion = fechaAsignacion;
    }

    /** @return Revisor */
    public UUID getRevisorId() {
        return revisorId;
    }

    /** @return instante de la asignación */
    public Instant getFechaAsignacion() {
        return fechaAsignacion;
    }
}
