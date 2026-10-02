package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta {@code TrazabilidadRespuesta} (CU-18; CONTRATOS.md 8.1).
 *
 * @param preguntaId          UUID de la Pregunta
 * @param registros           registros en orden cronológico
 * @param historialRevisiones evaluaciones y dictámenes de todos los procesos, en orden cronológico
 */
@Schema(name = "TrazabilidadRespuesta")
public record TrazabilidadJson(String preguntaId, List<RegistroJson> registros, List<EntradaHistorialJson> historialRevisiones) {

    /**
     * Registro de trazabilidad.
     *
     * @param fecha          instante
     * @param usuarioId      responsable
     * @param tipo           {@code CREACION}, {@code MODIFICACION} o {@code TRANSICION}
     * @param estadoAnterior estado anterior o {@code null}
     * @param estadoNuevo    estado nuevo
     * @param detalle        detalle
     */
    @Schema(name = "RegistroTrazabilidad")
    public record RegistroJson(Instant fecha, String usuarioId, String tipo, String estadoAnterior, String estadoNuevo,
                               String detalle) {
    }

    /**
     * Entrada del historial de revisiones: {@code evaluacion} es nulo si el tipo es {@code DICTAMEN} y
     * {@code dictamen} es nulo si el tipo es {@code EVALUACION}.
     *
     * @param tipo       {@code EVALUACION} o {@code DICTAMEN}
     * @param procesoId  UUID del Proceso
     * @param fecha      instante
     * @param evaluacion evaluación o {@code null}
     * @param dictamen   dictamen o {@code null}
     */
    @Schema(name = "EntradaHistorial")
    public record EntradaHistorialJson(String tipo, String procesoId, Instant fecha, ProcesoRevisionJson.EvaluacionJson evaluacion,
                                       ProcesoRevisionJson.DictamenJson dictamen) {
    }
}
