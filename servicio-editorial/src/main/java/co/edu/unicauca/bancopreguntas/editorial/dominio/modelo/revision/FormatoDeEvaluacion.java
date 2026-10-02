package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad interna del Proceso de revisión: el instrumento que diligencia un Revisor (Taller 1, sección 5).
 *
 * <p>Tiene identidad propia porque cada Revisor produce exactamente uno, pero es inmutable una vez
 * emitido (INV-18): no ofrece ningún método que lo cambie.</p>
 */
public final class FormatoDeEvaluacion {

    private final FormatoId id;
    private final UsuarioId revisorId;
    private final List<CriterioEvaluado> criterios;
    private final List<Observacion> observaciones;
    private final DecisionRevision decision;
    private final Instant fechaEmision;

    /**
     * Crea el formato validando que califique los tres criterios exactamente una vez (CU-11).
     *
     * @param id            identidad del formato
     * @param revisorId     Revisor que lo emite
     * @param criterios     valoraciones de {@code PEDAGOGICO}, {@code TECNICO} y {@code ESTRUCTURAL}
     * @param observaciones observaciones del Revisor (puede estar vacía)
     * @param decision      decisión aprobatoria o reprobatoria
     * @param fechaEmision  instante de emisión
     * @throws DatoInvalidoExcepcion si falta un dato o los criterios no son exactamente los tres obligatorios
     */
    public FormatoDeEvaluacion(FormatoId id, UsuarioId revisorId, List<CriterioEvaluado> criterios,
                               List<Observacion> observaciones, DecisionRevision decision, Instant fechaEmision) {
        this.id = Validaciones.requerirNoNulo(id, "formatoId");
        this.revisorId = Validaciones.requerirNoNulo(revisorId, "revisorId");
        this.criterios = List.copyOf(Validaciones.requerirNoNulo(criterios, "criterios"));
        this.observaciones = List.copyOf(Validaciones.requerirNoNulo(observaciones, "observaciones"));
        this.decision = Validaciones.requerirNoNulo(decision, "decision");
        this.fechaEmision = Validaciones.requerirNoNulo(fechaEmision, "fechaEmision");
        exigirLosTresCriterios(this.criterios);
    }

    // CU-11 y CONTRATOS 8.1: PEDAGOGICO, TECNICO y ESTRUCTURAL son obligatorios, una vez cada uno.
    // DUDA: CONTRATOS 8.1 no asigna código a criterios faltantes o repetidos; se usa SOLICITUD_INVALIDA (400).
    private static void exigirLosTresCriterios(List<CriterioEvaluado> criterios) {
        Set<TipoCriterio> presentes = EnumSet.noneOf(TipoCriterio.class);
        criterios.forEach(criterio -> presentes.add(criterio.criterio()));
        if (criterios.size() != TipoCriterio.values().length || presentes.size() != TipoCriterio.values().length) {
            throw new DatoInvalidoExcepcion(
                    "El formato de evaluación debe calificar una vez cada criterio: PEDAGOGICO, TECNICO y ESTRUCTURAL.");
        }
    }

    /**
     * Devuelve la identidad del formato.
     *
     * @return identificador
     */
    public FormatoId getId() {
        return id;
    }

    /**
     * Devuelve el Revisor que emitió el formato.
     *
     * @return identificador del Revisor
     */
    public UsuarioId getRevisorId() {
        return revisorId;
    }

    /**
     * Devuelve las valoraciones de los tres criterios.
     *
     * @return lista inmutable
     */
    public List<CriterioEvaluado> getCriterios() {
        return criterios;
    }

    /**
     * Devuelve las observaciones del Revisor (RF-18).
     *
     * @return lista inmutable
     */
    public List<Observacion> getObservaciones() {
        return observaciones;
    }

    /**
     * Devuelve la decisión del Revisor.
     *
     * @return {@code APROBATORIA} o {@code REPROBATORIA}
     */
    public DecisionRevision getDecision() {
        return decision;
    }

    /**
     * Devuelve el instante de emisión.
     *
     * @return fecha UTC
     */
    public Instant getFechaEmision() {
        return fechaEmision;
    }

    /**
     * Indica si la decisión es aprobatoria.
     *
     * @return {@code true} si es {@code APROBATORIA}
     */
    public boolean esAprobatorio() {
        return decision == DecisionRevision.APROBATORIA;
    }

    /**
     * Dos formatos son el mismo si tienen la misma identidad (entidad).
     *
     * @param otro objeto a comparar
     * @return {@code true} si comparten identidad
     */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof FormatoDeEvaluacion formato && id.equals(formato.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
