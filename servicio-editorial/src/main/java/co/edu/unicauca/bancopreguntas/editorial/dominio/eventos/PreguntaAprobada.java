package co.edu.unicauca.bancopreguntas.editorial.dominio.eventos;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;

import java.time.Instant;

/**
 * La Pregunta superó la Revisión por pares y quedó {@code APROBADA} (CU-12, INV-20; MODELO-DOMINIO A.1, E-2).
 *
 * @param preguntaId      pregunta aprobada
 * @param fechaOcurrencia instante de la aprobación
 */
public record PreguntaAprobada(PreguntaId preguntaId, Instant fechaOcurrencia) implements EventoDeDominio {
}
