package co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados;

import java.time.Instant;

/**
 * Elemento de la página de {@code GET /preguntas} (CU-06; CONTRATOS.md 8.1, "página de {@code PreguntaResumen}").
 *
 * <p>DUDA: CONTRATOS.md nombra {@code PreguntaResumen} pero no define sus campos; se eligen los mínimos
 * para listar y filtrar, sin las opciones ni la justificación.</p>
 *
 * @param preguntaId         UUID de la Pregunta
 * @param autorId            UUID del Autor
 * @param preguntaDirecta    Pregunta directa
 * @param estado             estado del ciclo de vida
 * @param clasificacion      identificadores del Catálogo
 * @param nivelDificultad    nivel de dificultad
 * @param fechaActualizacion instante del último cambio
 */
public record PreguntaResumen(String preguntaId, String autorId, String preguntaDirecta, String estado,
                              ClasificacionRespuesta clasificacion, String nivelDificultad, Instant fechaActualizacion) {
}
