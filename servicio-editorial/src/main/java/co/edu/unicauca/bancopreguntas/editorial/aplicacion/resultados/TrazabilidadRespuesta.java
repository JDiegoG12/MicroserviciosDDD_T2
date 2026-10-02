package co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados;

import java.time.Instant;
import java.util.List;

/**
 * Resultado con la forma de {@code TrazabilidadRespuesta} (CU-18; CONTRATOS.md 8.1).
 *
 * @param preguntaId          UUID de la Pregunta
 * @param registros           registros de trazabilidad en orden cronológico
 * @param historialRevisiones evaluaciones y dictámenes de todos los procesos, en orden cronológico
 */
public record TrazabilidadRespuesta(String preguntaId, List<RegistroRespuesta> registros,
                                    List<EntradaHistorialRespuesta> historialRevisiones) {

    /**
     * Registro de trazabilidad ({@code {fecha, usuarioId, tipo, estadoAnterior, estadoNuevo, detalle}}).
     *
     * @param fecha          instante del cambio
     * @param usuarioId      UUID del responsable
     * @param tipo           {@code CREACION}, {@code MODIFICACION} o {@code TRANSICION}
     * @param estadoAnterior estado previo o {@code null}
     * @param estadoNuevo    estado posterior
     * @param detalle        descripción
     */
    public record RegistroRespuesta(Instant fecha, String usuarioId, String tipo, String estadoAnterior,
                                    String estadoNuevo, String detalle) {
    }

    /**
     * Entrada del historial de revisiones.
     *
     * <p>Forma exacta de CONTRATOS.md 8.1 ({@code historialRevisiones}): {@code evaluacion} es nulo si el tipo es
     * {@code DICTAMEN} y {@code dictamen} es nulo si el tipo es {@code EVALUACION}.</p>
     *
     * @param tipo       {@code EVALUACION} o {@code DICTAMEN}
     * @param procesoId  UUID del Proceso de origen
     * @param fecha      instante de emisión
     * @param evaluacion detalle de la evaluación o {@code null}
     * @param dictamen   detalle del dictamen o {@code null}
     */
    public record EntradaHistorialRespuesta(String tipo, String procesoId, Instant fecha,
                                            ProcesoRevisionRespuesta.EvaluacionRespuesta evaluacion,
                                            ProcesoRevisionRespuesta.DictamenRespuesta dictamen) {
    }
}
