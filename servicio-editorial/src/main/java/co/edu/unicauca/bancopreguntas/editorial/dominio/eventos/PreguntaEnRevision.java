package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * La Pregunta pasó a {@code EN_REVISION} al abrirse su Proceso de revisión (CU-10, D-14).
 *
 * @param preguntaId      pregunta en revisión
 * @param usuarioId       Administrador que asignó los Revisores
 * @param fechaOcurrencia instante de la transición
 */
public record PreguntaEnRevision(PreguntaId preguntaId, UsuarioId usuarioId,
                                 Instant fechaOcurrencia) implements EventoDeDominio {
}
