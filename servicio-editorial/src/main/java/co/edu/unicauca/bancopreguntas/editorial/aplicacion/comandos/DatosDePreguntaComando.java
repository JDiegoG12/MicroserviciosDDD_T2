package co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos;

import java.util.List;

/**
 * Componentes de una Pregunta tal como llegan en {@code PreguntaSolicitud} (CONTRATOS.md 8.1), para
 * crearla (CU-04) o modificarla (CU-05).
 *
 * <p>Los textos pueden venir nulos o incompletos: la Pregunta queda en {@code BORRADOR} (D-02). La
 * clasificación y el nivel de dificultad son obligatorios.</p>
 *
 * @param contexto        Contexto
 * @param preguntaDirecta Pregunta directa
 * @param opciones        Opciones de respuesta
 * @param justificacion   Justificación
 * @param bibliografia    referencias bibliográficas
 * @param competenciaId   UUID de la Competencia
 * @param temaId          UUID del Tema
 * @param subtemaId       UUID del Subtema
 * @param nivelDificultad {@code BAJO}, {@code MEDIO} o {@code ALTO}
 */
public record DatosDePreguntaComando(
        String contexto,
        String preguntaDirecta,
        List<OpcionComando> opciones,
        String justificacion,
        List<String> bibliografia,
        String competenciaId,
        String temaId,
        String subtemaId,
        String nivelDificultad) {

    /**
     * Opción de respuesta de la solicitud.
     *
     * @param letra      letra de la opción
     * @param texto      texto de la opción
     * @param esCorrecta si es la Respuesta correcta; nulo equivale a {@code false}
     */
    public record OpcionComando(String letra, String texto, Boolean esCorrecta) {
    }
}
