package co.edu.unicauca.bancopreguntas.editorial.infraestructura.mensajeria;

import java.time.Instant;
import java.util.List;

/**
 * Campo {@code datos} del evento {@code PreguntaPublicada} (CONTRATOS.md 7.4), con exactamente los campos del
 * contrato y en su orden.
 *
 * @param preguntaId       UUID de la Pregunta
 * @param contexto         Contexto
 * @param preguntaDirecta  Pregunta directa
 * @param opciones         las cuatro opciones A–D, sin la marca de correcta
 * @param letraCorrecta    letra de la Respuesta correcta
 * @param clasificacion    identificadores del Catálogo
 * @param nivelDificultad  {@code BAJO}, {@code MEDIO} o {@code ALTO}
 * @param fechaPublicacion instante de la publicación
 */
public record DatosPreguntaPublicada(String preguntaId, String contexto, String preguntaDirecta, List<Opcion> opciones,
                                     String letraCorrecta, Clasificacion clasificacion, String nivelDificultad,
                                     Instant fechaPublicacion) {

    /**
     * Opción tal como la ve Evaluación.
     *
     * @param letra letra
     * @param texto texto
     */
    public record Opcion(String letra, String texto) {
    }

    /**
     * Clasificación académica, solo identificadores (D-13).
     *
     * @param competenciaId UUID de la Competencia
     * @param temaId        UUID del Tema
     * @param subtemaId     UUID del Subtema
     */
    public record Clasificacion(String competenciaId, String temaId, String subtemaId) {
    }
}
