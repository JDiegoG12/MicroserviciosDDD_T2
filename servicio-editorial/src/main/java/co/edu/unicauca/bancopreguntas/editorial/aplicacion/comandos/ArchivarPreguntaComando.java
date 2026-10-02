package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

/**
 * Solicitud de archivado (CU-09; {@code POST /preguntas/{preguntaId}/archivado} con cuerpo {@code { "motivo" }}).
 *
 * @param preguntaId UUID de la Pregunta
 * @param motivo     motivo obligatorio de 1 a 500 caracteres
 */
public record ArchivarPreguntaComando(String preguntaId, String motivo) {
}
