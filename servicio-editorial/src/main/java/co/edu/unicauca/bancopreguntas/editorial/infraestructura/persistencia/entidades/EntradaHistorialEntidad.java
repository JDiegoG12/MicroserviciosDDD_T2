package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ResultadoDictamen;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fila de {@code pregunta_historial}: una evaluación o un dictamen del HistorialDeRevisiones (D-15, INV-14).
 *
 * <p>Si {@code tipo} es {@code EVALUACION} se llenan {@code revisorId}, {@code decision}, los criterios y las
 * observaciones; si es {@code DICTAMEN}, {@code resultado} y {@code porcentajeAprobacion}.</p>
 */
@Entity
@Table(name = "pregunta_historial")
public class EntradaHistorialEntidad {

    /** Tipo de una evaluación. */
    public static final String TIPO_EVALUACION = "EVALUACION";
    /** Tipo de un dictamen. */
    public static final String TIPO_DICTAMEN = "DICTAMEN";

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private PreguntaEntidad pregunta;

    @Column(name = "secuencia", nullable = false)
    private int secuencia;

    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "proceso_id", nullable = false)
    private UUID procesoId;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "revisor_id")
    private UUID revisorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 15)
    private DecisionRevision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 10)
    private ResultadoDictamen resultado;

    @Column(name = "porcentaje_aprobacion", precision = 5, scale = 2)
    private BigDecimal porcentajeAprobacion;

    @ElementCollection
    @CollectionTable(name = "pregunta_historial_criterio", joinColumns = @JoinColumn(name = "entrada_id"))
    @OrderColumn(name = "posicion")
    private List<CriterioEmbebido> criterios = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pregunta_historial_observacion", joinColumns = @JoinColumn(name = "entrada_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "texto", nullable = false, columnDefinition = "text")
    private List<String> observaciones = new ArrayList<>();

    /**
     * Constructor que exige JPA.
     */
    protected EntradaHistorialEntidad() {
    }

    /**
     * Crea una entrada nueva.
     *
     * @param pregunta  Pregunta dueña
     * @param secuencia posición en el historial
     * @param tipo      {@link #TIPO_EVALUACION} o {@link #TIPO_DICTAMEN}
     * @param procesoId proceso de origen
     * @param fecha     instante de emisión
     */
    public EntradaHistorialEntidad(PreguntaEntidad pregunta, int secuencia, String tipo, UUID procesoId, Instant fecha) {
        this.id = UUID.randomUUID();
        this.pregunta = pregunta;
        this.secuencia = secuencia;
        this.tipo = tipo;
        this.procesoId = procesoId;
        this.fecha = fecha;
    }

    /**
     * Completa los datos de una evaluación.
     *
     * @param revisor               revisor
     * @param decisionDelRevisor    decisión
     * @param criteriosEvaluados    criterios
     * @param observacionesDelRevisor observaciones
     */
    public void completarEvaluacion(UUID revisor, DecisionRevision decisionDelRevisor, List<CriterioEmbebido> criteriosEvaluados,
                                    List<String> observacionesDelRevisor) {
        this.revisorId = revisor;
        this.decision = decisionDelRevisor;
        this.criterios.addAll(criteriosEvaluados);
        this.observaciones.addAll(observacionesDelRevisor);
    }

    /**
     * Completa los datos de un dictamen.
     *
     * @param resultadoDelDictamen resultado
     * @param porcentaje           porcentaje de aprobación
     */
    public void completarDictamen(ResultadoDictamen resultadoDelDictamen, BigDecimal porcentaje) {
        this.resultado = resultadoDelDictamen;
        this.porcentajeAprobacion = porcentaje;
    }

    /** @return posición en el historial */
    public int getSecuencia() {
        return secuencia;
    }

    /** @return tipo */
    public String getTipo() {
        return tipo;
    }

    /** @return proceso de origen */
    public UUID getProcesoId() {
        return procesoId;
    }

    /** @return instante */
    public Instant getFecha() {
        return fecha;
    }

    /** @return revisor (evaluación) */
    public UUID getRevisorId() {
        return revisorId;
    }

    /** @return decisión (evaluación) */
    public DecisionRevision getDecision() {
        return decision;
    }

    /** @return resultado (dictamen) */
    public ResultadoDictamen getResultado() {
        return resultado;
    }

    /** @return porcentaje (dictamen) */
    public BigDecimal getPorcentajeAprobacion() {
        return porcentajeAprobacion;
    }

    /** @return criterios (evaluación) */
    public List<CriterioEmbebido> getCriterios() {
        return criterios;
    }

    /** @return observaciones (evaluación) */
    public List<String> getObservaciones() {
        return observaciones;
    }
}
