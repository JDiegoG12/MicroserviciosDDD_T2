package co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados;

/**
 * Clasificación académica en las respuestas: solo identificadores (D-13; CONTRATOS.md 8.1).
 *
 * @param competenciaId UUID de la Competencia
 * @param temaId        UUID del Tema
 * @param subtemaId     UUID del Subtema
 */
public record ClasificacionRespuesta(String competenciaId, String temaId, String subtemaId) {
}
