package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.CriterioEvaluado;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoDeEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Observacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;
import java.util.List;

/**
 * Copia inmutable de un Formato de evaluación dentro del Historial de la Pregunta (D-15, RF-19).
 *
 * @param procesoId     proceso del que proviene
 * @param revisorId     Revisor que la emitió (RF-18)
 * @param criterios     valoraciones de los tres criterios
 * @param observaciones observaciones del Revisor
 * @param decision      decisión aprobatoria o reprobatoria
 * @param fecha         instante de emisión
 */
public record EvaluacionEnHistorial(
        ProcesoDeRevisionId procesoId,
        UsuarioId revisorId,
        List<CriterioEvaluado> criterios,
        List<Observacion> observaciones,
        DecisionRevision decision,
        Instant fecha) implements EntradaDeHistorial {

    /**
     * Valida los datos y copia las listas.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta un dato
     */
    public EvaluacionEnHistorial {
        Validaciones.requerirNoNulo(procesoId, "procesoId");
        Validaciones.requerirNoNulo(revisorId, "revisorId");
        criterios = List.copyOf(Validaciones.requerirNoNulo(criterios, "criterios"));
        observaciones = List.copyOf(Validaciones.requerirNoNulo(observaciones, "observaciones"));
        Validaciones.requerirNoNulo(decision, "decision");
        Validaciones.requerirNoNulo(fecha, "fecha");
    }

    /**
     * Copia un Formato de evaluación para trasladarlo al Historial (D-15).
     *
     * @param procesoId proceso del que proviene
     * @param formato   formato emitido
     * @return la entrada del historial
     */
    public static EvaluacionEnHistorial desde(ProcesoDeRevisionId procesoId, FormatoDeEvaluacion formato) {
        return new EvaluacionEnHistorial(procesoId, formato.getRevisorId(), formato.getCriterios(),
                formato.getObservaciones(), formato.getDecision(), formato.getFechaEmision());
    }
}
