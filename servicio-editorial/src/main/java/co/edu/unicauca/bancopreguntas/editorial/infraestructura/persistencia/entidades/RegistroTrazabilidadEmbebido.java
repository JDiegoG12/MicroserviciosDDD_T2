package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.TipoDeRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de {@code pregunta_trazabilidad}: un RegistroDeTrazabilidad (RF-29, RF-30, INV-13). Solo se inserta.
 */
@Embeddable
public class RegistroTrazabilidadEmbebido {

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoDeRegistro tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPregunta estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoPregunta estadoNuevo;

    @Column(name = "detalle", nullable = false, columnDefinition = "text")
    private String detalle;

    /**
     * Constructor que exige JPA.
     */
    protected RegistroTrazabilidadEmbebido() {
    }

    /**
     * Crea la fila.
     *
     * @param fecha          instante
     * @param usuarioId      responsable
     * @param tipo           tipo de registro
     * @param estadoAnterior estado anterior o {@code null}
     * @param estadoNuevo    estado nuevo
     * @param detalle        detalle
     */
    public RegistroTrazabilidadEmbebido(Instant fecha, UUID usuarioId, TipoDeRegistro tipo, EstadoPregunta estadoAnterior,
                                        EstadoPregunta estadoNuevo, String detalle) {
        this.fecha = fecha;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.detalle = detalle;
    }

    /** @return instante */
    public Instant getFecha() {
        return fecha;
    }

    /** @return responsable */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /** @return tipo */
    public TipoDeRegistro getTipo() {
        return tipo;
    }

    /** @return estado anterior o {@code null} */
    public EstadoPregunta getEstadoAnterior() {
        return estadoAnterior;
    }

    /** @return estado nuevo */
    public EstadoPregunta getEstadoNuevo() {
        return estadoNuevo;
    }

    /** @return detalle */
    public String getDetalle() {
        return detalle;
    }
}
