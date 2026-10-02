package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fila de {@code formato_evaluacion}: la entidad interna FormatoDeEvaluacion, inmutable una vez emitida (INV-18).
 */
@Entity
@Table(name = "formato_evaluacion")
public class FormatoEvaluacionEntidad {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proceso_id", nullable = false)
    private ProcesoRevisionEntidad proceso;

    @Column(name = "posicion", nullable = false)
    private int posicion;

    @Column(name = "revisor_id", nullable = false)
    private UUID revisorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 15)
    private DecisionRevision decision;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @ElementCollection
    @CollectionTable(name = "formato_criterio", joinColumns = @JoinColumn(name = "formato_id"))
    @OrderColumn(name = "posicion")
    private List<CriterioEmbebido> criterios = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "formato_observacion", joinColumns = @JoinColumn(name = "formato_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "texto", nullable = false, columnDefinition = "text")
    private List<String> observaciones = new ArrayList<>();

    /**
     * Constructor que exige JPA.
     */
    protected FormatoEvaluacionEntidad() {
    }

    /**
     * Crea la fila con todos sus datos.
     *
     * @param id            identidad del formato
     * @param proceso       Proceso dueño
     * @param posicion      orden de registro dentro del Proceso
     * @param revisorId     Revisor
     * @param decision      decisión
     * @param fechaEmision  instante de emisión
     * @param criterios     criterios evaluados
     * @param observaciones observaciones
     */
    public FormatoEvaluacionEntidad(UUID id, ProcesoRevisionEntidad proceso, int posicion, UUID revisorId,
                                    DecisionRevision decision, Instant fechaEmision, List<CriterioEmbebido> criterios,
                                    List<String> observaciones) {
        this.id = id;
        this.proceso = proceso;
        this.posicion = posicion;
        this.revisorId = revisorId;
        this.decision = decision;
        this.fechaEmision = fechaEmision;
        this.criterios.addAll(criterios);
        this.observaciones.addAll(observaciones);
    }

    /** @return identidad */
    public UUID getId() {
        return id;
    }

    /** @return Revisor */
    public UUID getRevisorId() {
        return revisorId;
    }

    /** @return decisión */
    public DecisionRevision getDecision() {
        return decision;
    }

    /** @return instante de emisión */
    public Instant getFechaEmision() {
        return fechaEmision;
    }

    /** @return criterios */
    public List<CriterioEmbebido> getCriterios() {
        return criterios;
    }

    /** @return observaciones */
    public List<String> getObservaciones() {
        return observaciones;
    }
}
