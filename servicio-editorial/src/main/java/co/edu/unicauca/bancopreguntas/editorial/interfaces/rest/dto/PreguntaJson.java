package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta {@code PreguntaRespuesta} (CONTRATOS.md 8.1): los campos de la solicitud más {@code preguntaId},
 * {@code autorId}, {@code estado}, {@code erroresValidacion}, {@code fechaCreacion} y {@code fechaActualizacion}.
 *
 * @param preguntaId         UUID de la Pregunta
 * @param autorId            UUID del Autor
 * @param contexto           Contexto
 * @param preguntaDirecta    Pregunta directa
 * @param opciones           Opciones de respuesta
 * @param justificacion      Justificación
 * @param bibliografia       referencias
 * @param clasificacion      identificadores del Catálogo
 * @param nivelDificultad    nivel de dificultad
 * @param estado             estado del ciclo de vida
 * @param erroresValidacion  reglas incumplidas (vacía si supera la validación)
 * @param fechaCreacion      instante de creación (ISO-8601 UTC)
 * @param fechaActualizacion instante del último cambio (ISO-8601 UTC)
 */
@Schema(name = "PreguntaRespuesta")
public record PreguntaJson(String preguntaId, String autorId, String contexto, String preguntaDirecta,
                           List<OpcionJson> opciones, String justificacion, List<String> bibliografia,
                           ClasificacionJson clasificacion, String nivelDificultad, String estado,
                           List<ErrorValidacionJson> erroresValidacion, Instant fechaCreacion, Instant fechaActualizacion) {

    /**
     * Opción de respuesta.
     *
     * @param letra      letra
     * @param texto      texto
     * @param esCorrecta si es la Respuesta correcta
     */
    @Schema(name = "OpcionRespuesta")
    public record OpcionJson(String letra, String texto, boolean esCorrecta) {
    }

    /**
     * Regla de la validación estructural incumplida.
     *
     * @param regla   identificador de la regla ({@code RF-08} … {@code RF-13}, {@code INV-04})
     * @param mensaje explicación
     */
    @Schema(name = "ErrorValidacion")
    public record ErrorValidacionJson(String regla, String mensaje) {
    }
}
