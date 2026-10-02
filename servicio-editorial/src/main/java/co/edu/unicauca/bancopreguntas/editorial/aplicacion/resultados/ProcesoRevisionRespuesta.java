package co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Resultado con la forma de {@code ProcesoRevisionRespuesta} (CONTRATOS.md 8.1).
 *
 * @param procesoId    UUID del Proceso
 * @param preguntaId   UUID de la Pregunta
 * @param estado       {@code ABIERTO} o {@code CERRADO}
 * @param asignaciones Asignaciones de revisor
 * @param evaluaciones Formatos de evaluación emitidos
 * @param dictamen     dictamen o {@code null}
 */
public record ProcesoRevisionRespuesta(String procesoId, String preguntaId, String estado,
                                       List<AsignacionRespuesta> asignaciones, List<EvaluacionRespuesta> evaluaciones,
                                       DictamenRespuesta dictamen) {

    /**
     * Asignación de revisor ({@code {revisorId, fechaAsignacion, evaluacionRegistrada}}).
     *
     * @param revisorId            UUID del Revisor
     * @param fechaAsignacion      instante de la asignación
     * @param evaluacionRegistrada si ya evaluó
     */
    public record AsignacionRespuesta(String revisorId, Instant fechaAsignacion, boolean evaluacionRegistrada) {
    }

    /**
     * Formato de evaluación.
     *
     * <p>DUDA: CONTRATOS 8.1 no detalla los campos de {@code evaluaciones}; se usan los de la solicitud de
     * registro más el revisor y la fecha.</p>
     *
     * @param revisorId     UUID del Revisor
     * @param criterios     valoraciones
     * @param observaciones observaciones
     * @param decision      {@code APROBATORIA} o {@code REPROBATORIA}
     * @param fechaEmision  instante de emisión
     */
    public record EvaluacionRespuesta(String revisorId, List<CriterioRespuesta> criterios, List<String> observaciones,
                                      String decision, Instant fechaEmision) {
    }

    /**
     * Valoración de un criterio.
     *
     * @param criterio   nombre del criterio
     * @param valoracion entero de 1 a 5
     */
    public record CriterioRespuesta(String criterio, int valoracion) {
    }

    /**
     * Dictamen ({@code { "resultado", "porcentajeAprobacion", "fechaEmision" }}).
     *
     * @param resultado            {@code APROBADA} o {@code RECHAZADA}
     * @param porcentajeAprobacion porcentaje con 2 decimales
     * @param fechaEmision         instante de emisión
     */
    public record DictamenRespuesta(String resultado, BigDecimal porcentajeAprobacion, Instant fechaEmision) {
    }
}
