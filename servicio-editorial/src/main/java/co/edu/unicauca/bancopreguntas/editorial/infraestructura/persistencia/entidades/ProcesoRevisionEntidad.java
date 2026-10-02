package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ResultadoDictamen;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de base de datos del agregado Proceso de revisión (tabla {@code proceso_revision} y sus tablas hijas).
 */
@Entity
@Table(name = "proceso_revision")
public class ProcesoRevisionEntidad {

    @Id
    private UUID id;

    @Column(name = "pregunta_id", nullable = false)
    private UUID preguntaId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoProceso estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "dictamen_resultado", length = 10)
    private ResultadoDictamen dictamenResultado;

    @Column(name = "dictamen_porcentaje", precision = 5, scale = 2)
    private BigDecimal dictamenPorcentaje;

    @Column(name = "dictamen_fecha")
    private Instant dictamenFecha;

    /** Bloqueo optimista (CONTRATOS.md 11.1). Nulo mientras la entidad no se ha guardado. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "proceso_asignacion", joinColumns = @JoinColumn(name = "proceso_id"))
    @OrderColumn(name = "posicion")
    private List<AsignacionEmbebida> asignaciones = new ArrayList<>();

    @OneToMany(mappedBy = "proceso", cascade = CascadeType.ALL)
    @OrderBy("posicion ASC")
    private List<FormatoEvaluacionEntidad> formatos = new ArrayList<>();

    /**
     * Constructor que exige JPA.
     */
    protected ProcesoRevisionEntidad() {
    }

    /**
     * Crea una entidad nueva con sus datos de apertura, que no cambian.
     *
     * @param id            identidad
     * @param preguntaId    Pregunta evaluada
     * @param autorId       Autor de la Pregunta
     * @param fechaApertura instante de apertura
     */
    public ProcesoRevisionEntidad(UUID id, UUID preguntaId, UUID autorId, Instant fechaApertura) {
        this.id = id;
        this.preguntaId = preguntaId;
        this.autorId = autorId;
        this.fechaApertura = fechaApertura;
    }

    /** @return identidad */
    public UUID getId() {
        return id;
    }

    /** @return Pregunta evaluada */
    public UUID getPreguntaId() {
        return preguntaId;
    }

    /** @return Autor de la Pregunta */
    public UUID getAutorId() {
        return autorId;
    }

    /** @return instante de apertura */
    public Instant getFechaApertura() {
        return fechaApertura;
    }

    /** @return estado */
    public EstadoProceso getEstado() {
        return estado;
    }

    /** @param estado estado */
    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    /** @return resultado del dictamen o {@code null} */
    public ResultadoDictamen getDictamenResultado() {
        return dictamenResultado;
    }

    /** @return porcentaje del dictamen o {@code null} */
    public BigDecimal getDictamenPorcentaje() {
        return dictamenPorcentaje;
    }

    /** @return fecha del dictamen o {@code null} */
    public Instant getDictamenFecha() {
        return dictamenFecha;
    }

    /**
     * Fija el dictamen (INV-21: una vez emitido no cambia).
     *
     * @param resultado  resultado
     * @param porcentaje porcentaje con 2 decimales
     * @param fecha      instante de emisión
     */
    public void fijarDictamen(ResultadoDictamen resultado, BigDecimal porcentaje, Instant fecha) {
        this.dictamenResultado = resultado;
        this.dictamenPorcentaje = porcentaje;
        this.dictamenFecha = fecha;
    }

    /** @return versión de bloqueo optimista */
    public Long getVersion() {
        return version;
    }

    /** @return asignaciones (lista mutable de la entidad) */
    public List<AsignacionEmbebida> getAsignaciones() {
        return asignaciones;
    }

    /** @return formatos (lista mutable de la entidad; solo se anexa) */
    public List<FormatoEvaluacionEntidad> getFormatos() {
        return formatos;
    }
}
