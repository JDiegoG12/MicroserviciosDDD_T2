package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.NivelDeDificultad;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de base de datos del agregado Pregunta (tabla {@code pregunta} y sus tablas hijas). Solo lo usan el
 * repositorio JPA y su mapper: nunca sale de la infraestructura (CONTRATOS.md 3.3.2).
 */
@Entity
@Table(name = "pregunta")
public class PreguntaEntidad {

    @Id
    private UUID id;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "contexto", nullable = false, columnDefinition = "text")
    private String contexto;

    @Column(name = "pregunta_directa", nullable = false, columnDefinition = "text")
    private String preguntaDirecta;

    @Column(name = "justificacion", nullable = false, columnDefinition = "text")
    private String justificacion;

    @Column(name = "competencia_id", nullable = false)
    private UUID competenciaId;

    @Column(name = "tema_id", nullable = false)
    private UUID temaId;

    @Column(name = "subtema_id", nullable = false)
    private UUID subtemaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificultad", nullable = false, length = 10)
    private NivelDeDificultad nivelDificultad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPregunta estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    /** Bloqueo optimista (CONTRATOS.md 11.1). Nulo mientras la entidad no se ha guardado. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection
    @CollectionTable(name = "pregunta_opcion", joinColumns = @JoinColumn(name = "pregunta_id"))
    @OrderColumn(name = "posicion")
    private List<OpcionEmbebida> opciones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pregunta_bibliografia", joinColumns = @JoinColumn(name = "pregunta_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "referencia", nullable = false, columnDefinition = "text")
    private List<String> bibliografia = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "pregunta_trazabilidad", joinColumns = @JoinColumn(name = "pregunta_id"))
    @OrderColumn(name = "secuencia")
    private List<RegistroTrazabilidadEmbebido> trazabilidad = new ArrayList<>();

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL)
    @OrderBy("secuencia ASC")
    private List<EntradaHistorialEntidad> historial = new ArrayList<>();

    /**
     * Constructor que exige JPA.
     */
    protected PreguntaEntidad() {
    }

    /**
     * Crea una entidad nueva con su identidad.
     *
     * @param id identidad de la Pregunta
     */
    public PreguntaEntidad(UUID id) {
        this.id = id;
    }

    /** @return identidad */
    public UUID getId() {
        return id;
    }

    /** @return Autor */
    public UUID getAutorId() {
        return autorId;
    }

    /** @param autorId Autor */
    public void setAutorId(UUID autorId) {
        this.autorId = autorId;
    }

    /** @return Contexto */
    public String getContexto() {
        return contexto;
    }

    /** @param contexto Contexto */
    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    /** @return Pregunta directa */
    public String getPreguntaDirecta() {
        return preguntaDirecta;
    }

    /** @param preguntaDirecta Pregunta directa */
    public void setPreguntaDirecta(String preguntaDirecta) {
        this.preguntaDirecta = preguntaDirecta;
    }

    /** @return Justificación */
    public String getJustificacion() {
        return justificacion;
    }

    /** @param justificacion Justificación */
    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }

    /** @return Competencia */
    public UUID getCompetenciaId() {
        return competenciaId;
    }

    /** @param competenciaId Competencia */
    public void setCompetenciaId(UUID competenciaId) {
        this.competenciaId = competenciaId;
    }

    /** @return Tema */
    public UUID getTemaId() {
        return temaId;
    }

    /** @param temaId Tema */
    public void setTemaId(UUID temaId) {
        this.temaId = temaId;
    }

    /** @return Subtema */
    public UUID getSubtemaId() {
        return subtemaId;
    }

    /** @param subtemaId Subtema */
    public void setSubtemaId(UUID subtemaId) {
        this.subtemaId = subtemaId;
    }

    /** @return nivel de dificultad */
    public NivelDeDificultad getNivelDificultad() {
        return nivelDificultad;
    }

    /** @param nivelDificultad nivel de dificultad */
    public void setNivelDificultad(NivelDeDificultad nivelDificultad) {
        this.nivelDificultad = nivelDificultad;
    }

    /** @return estado */
    public EstadoPregunta getEstado() {
        return estado;
    }

    /** @param estado estado */
    public void setEstado(EstadoPregunta estado) {
        this.estado = estado;
    }

    /** @return fecha de creación */
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    /** @param fechaCreacion fecha de creación */
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    /** @return fecha del último cambio */
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }

    /** @param fechaActualizacion fecha del último cambio */
    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    /** @return versión de bloqueo optimista */
    public Long getVersion() {
        return version;
    }

    /** @return opciones (lista mutable de la entidad) */
    public List<OpcionEmbebida> getOpciones() {
        return opciones;
    }

    /** @return bibliografía (lista mutable de la entidad) */
    public List<String> getBibliografia() {
        return bibliografia;
    }

    /** @return trazabilidad (lista mutable de la entidad; solo se anexa) */
    public List<RegistroTrazabilidadEmbebido> getTrazabilidad() {
        return trazabilidad;
    }

    /** @return historial (lista mutable de la entidad; solo se anexa) */
    public List<EntradaHistorialEntidad> getHistorial() {
        return historial;
    }
}
