package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * El Autor sometió la Pregunta a revisión; queda en {@code PENDIENTE_REVISION} (CU-07).
 *
 * <p>No abre ningún Proceso: eso ocurre al asignar Revisores (D-14; MODELO-DOMINIO A.1, errata E-3).</p>
 *
 * @param preguntaId      pregunta sometida
 * @param autorId         Autor que la sometió
 * @param fechaOcurrencia instante del envío
 */
public record PreguntaSometidaARevision(PreguntaId preguntaId, UsuarioId autorId,
                                        Instant fechaOcurrencia) implements EventoDeDominio {
}
