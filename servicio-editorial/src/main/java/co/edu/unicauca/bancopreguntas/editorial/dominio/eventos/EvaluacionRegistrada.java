package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;

/**
 * Un Revisor registró su Formato de evaluación (CU-11; Taller 1, tabla 11.2).
 *
 * @param procesoId       proceso
 * @param preguntaId      pregunta evaluada
 * @param formatoId       formato emitido
 * @param revisorId       Revisor que lo emitió
 * @param decision        decisión del Revisor
 * @param fechaOcurrencia instante de emisión
 */
public record EvaluacionRegistrada(ProcesoDeRevisionId procesoId, PreguntaId preguntaId, FormatoId formatoId,
                                   UsuarioId revisorId, DecisionRevision decision,
                                   Instant fechaOcurrencia) implements EventoDeDominio {
}
