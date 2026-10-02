package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Clasificación académica en las respuestas: solo identificadores (D-13).
 *
 * @param competenciaId UUID de la Competencia
 * @param temaId        UUID del Tema
 * @param subtemaId     UUID del Subtema
 */
@Schema(name = "Clasificacion")
public record ClasificacionJson(String competenciaId, String temaId, String subtemaId) {
}
