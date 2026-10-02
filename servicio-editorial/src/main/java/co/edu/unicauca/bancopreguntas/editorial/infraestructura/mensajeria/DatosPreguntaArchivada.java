package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import java.time.Instant;

/**
 * Campo {@code datos} del evento {@code PreguntaArchivada} (CONTRATOS.md 7.5).
 *
 * @param preguntaId     UUID de la Pregunta
 * @param motivo         motivo del archivado, de 1 a 500 caracteres
 * @param fechaArchivado instante del archivado
 */
public record DatosPreguntaArchivada(String preguntaId, String motivo, Instant fechaArchivado) {
}
