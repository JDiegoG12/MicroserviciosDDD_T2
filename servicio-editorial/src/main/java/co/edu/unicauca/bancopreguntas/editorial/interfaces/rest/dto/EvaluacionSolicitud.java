package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cuerpo de {@code POST /procesos-revision/{procesoId}/evaluaciones} (CU-11; CONTRATOS.md 8.1).
 *
 * @param criterios     valoraciones de {@code PEDAGOGICO}, {@code TECNICO} y {@code ESTRUCTURAL} (las tres)
 * @param observaciones observaciones del Revisor
 * @param decision      {@code APROBATORIA} o {@code REPROBATORIA}
 */
@Schema(name = "EvaluacionSolicitud")
public record EvaluacionSolicitud(
        @NotNull(message = "criterios es obligatorio.") List<@Valid CriterioSolicitud> criterios,
        List<String> observaciones,
        @NotNull(message = "decision es obligatoria.")
        @Schema(allowableValues = {"APROBATORIA", "REPROBATORIA"}, example = "APROBATORIA") String decision) {

    /**
     * Valoración de un criterio.
     *
     * @param criterio   {@code PEDAGOGICO}, {@code TECNICO} o {@code ESTRUCTURAL}
     * @param valoracion entero de 1 a 5
     */
    @Schema(name = "CriterioSolicitud")
    public record CriterioSolicitud(
            @NotNull(message = "criterio es obligatorio.")
            @Schema(allowableValues = {"PEDAGOGICO", "TECNICO", "ESTRUCTURAL"}, example = "PEDAGOGICO") String criterio,
            @NotNull(message = "valoracion es obligatoria.") @Schema(minimum = "1", maximum = "5", example = "4") Integer valoracion) {
    }
}
