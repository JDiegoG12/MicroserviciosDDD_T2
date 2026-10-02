package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Elemento de la página de {@code GET /preguntas} ({@code PreguntaResumen}, CONTRATOS.md 8.1).
 *
 * @param preguntaId         UUID de la Pregunta
 * @param autorId            UUID del Autor
 * @param preguntaDirecta    Pregunta directa
 * @param estado             estado del ciclo de vida
 * @param clasificacion      identificadores del Catálogo
 * @param nivelDificultad    nivel de dificultad
 * @param fechaActualizacion instante del último cambio
 */
@Schema(name = "PreguntaResumen")
public record PreguntaResumenJson(String preguntaId, String autorId, String preguntaDirecta, String estado,
                                  ClasificacionJson clasificacion, String nivelDificultad, Instant fechaActualizacion) {
}
