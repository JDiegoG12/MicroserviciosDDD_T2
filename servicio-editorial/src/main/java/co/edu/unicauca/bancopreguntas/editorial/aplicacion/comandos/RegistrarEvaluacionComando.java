package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

import java.util.List;

/**
 * Solicitud de registro del Formato de evaluación (CU-11; {@code POST /procesos-revision/{procesoId}/evaluaciones}).
 *
 * @param procesoId     UUID del Proceso de revisión
 * @param criterios     valoraciones de {@code PEDAGOGICO}, {@code TECNICO} y {@code ESTRUCTURAL}
 * @param observaciones observaciones del Revisor
 * @param decision      {@code APROBATORIA} o {@code REPROBATORIA}
 */
public record RegistrarEvaluacionComando(String procesoId, List<CriterioComando> criterios, List<String> observaciones,
                                         String decision) {

    /**
     * Valoración de un criterio.
     *
     * @param criterio   nombre del criterio
     * @param valoracion entero de 1 a 5
     */
    public record CriterioComando(String criterio, Integer valoracion) {
    }
}
