package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;

import java.time.Instant;

/**
 * Se abrió un Proceso de revisión sobre una Pregunta (CU-10, D-14; Taller 1, tabla 11.2).
 *
 * @param procesoId       proceso abierto
 * @param preguntaId      pregunta evaluada
 * @param autorId         Autor de la pregunta
 * @param fechaOcurrencia instante de apertura
 */
public record ProcesoDeRevisionAbierto(ProcesoDeRevisionId procesoId, PreguntaId preguntaId, UsuarioId autorId,
                                       Instant fechaOcurrencia) implements EventoDeDominio {
}
