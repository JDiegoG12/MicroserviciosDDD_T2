package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

/**
 * Solicitud de modificación de una Pregunta (CU-05; {@code PUT /preguntas/{preguntaId}}).
 *
 * @param preguntaId UUID de la Pregunta
 * @param datos      componentes nuevos
 */
public record ModificarPreguntaComando(String preguntaId, DatosDePreguntaComando datos) {
}
