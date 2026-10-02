package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

import java.util.List;

/**
 * Solicitud de asignación de Revisores (CU-10; {@code POST /preguntas/{preguntaId}/procesos-revision}).
 *
 * @param preguntaId   UUID de la Pregunta
 * @param revisoresIds UUID de los Revisores propuestos
 */
public record AsignarRevisoresComando(String preguntaId, List<String> revisoresIds) {
}
