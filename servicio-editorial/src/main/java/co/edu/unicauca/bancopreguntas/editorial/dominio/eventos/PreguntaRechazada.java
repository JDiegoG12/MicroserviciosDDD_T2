package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * La Pregunta no alcanzó el umbral de aprobación; pasó por {@code RECHAZADA} y volvió a
 * {@code EN_CONSTRUCCION} (CU-12, D-07; MODELO-DOMINIO A.1, E-2).
 *
 * @param preguntaId      pregunta rechazada
 * @param fechaOcurrencia instante del rechazo
 */
public record PreguntaRechazada(PreguntaId preguntaId, Instant fechaOcurrencia) implements EventoDeDominio {
}
