package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta {@code ProcesoRevisionRespuesta} (CONTRATOS.md 8.1).
 *
 * @param procesoId    UUID del Proceso
 * @param preguntaId   UUID de la Pregunta
 * @param estado       {@code ABIERTO} o {@code CERRADO}
 * @param asignaciones asignaciones de revisor
 * @param evaluaciones formatos de evaluación emitidos
 * @param dictamen     dictamen o {@code null}
 */
@Schema(name = "ProcesoRevisionRespuesta")
public record ProcesoRevisionJson(String procesoId, String preguntaId, String estado, List<AsignacionJson> asignaciones,
                                  List<EvaluacionJson> evaluaciones, DictamenJson dictamen) {

    /**
     * Asignación de revisor.
     *
     * @param revisorId            UUID del Revisor
     * @param fechaAsignacion      instante de la asignación
     * @param evaluacionRegistrada si ya evaluó
     */
    @Schema(name = "Asignacion")
    public record AsignacionJson(String revisorId, Instant fechaAsignacion, boolean evaluacionRegistrada) {
    }

    /**
     * Formato de evaluación.
     *
     * @param revisorId     UUID del Revisor
     * @param criterios     valoraciones
     * @param observaciones observaciones
     * @param decision      {@code APROBATORIA} o {@code REPROBATORIA}
     * @param fechaEmision  instante de emisión
     */
    @Schema(name = "Evaluacion")
    public record EvaluacionJson(String revisorId, List<CriterioJson> criterios, List<String> observaciones,
                                 String decision, Instant fechaEmision) {
    }

    /**
     * Valoración de un criterio.
     *
     * @param criterio   nombre del criterio
     * @param valoracion entero de 1 a 5
     */
    @Schema(name = "Criterio")
    public record CriterioJson(String criterio, int valoracion) {
    }

    /**
     * Dictamen.
     *
     * @param resultado            {@code APROBADA} o {@code RECHAZADA}
     * @param porcentajeAprobacion porcentaje con 2 decimales
     * @param fechaEmision         instante de emisión
     */
    @Schema(name = "Dictamen")
    public record DictamenJson(String resultado, BigDecimal porcentajeAprobacion, Instant fechaEmision) {
    }
}
