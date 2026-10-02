package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * El Autor modificó el contenido de la Pregunta (CU-05; Taller 1, tabla 11.1).
 *
 * @param preguntaId       pregunta modificada
 * @param usuarioId        Autor que la modificó
 * @param estadoResultante estado tras revalidar ({@code BORRADOR} o {@code EN_CONSTRUCCION})
 * @param fechaOcurrencia  instante de la modificación
 */
public record PreguntaModificada(PreguntaId preguntaId, UsuarioId usuarioId, EstadoPregunta estadoResultante,
                                 Instant fechaOcurrencia) implements EventoDeDominio {
}
