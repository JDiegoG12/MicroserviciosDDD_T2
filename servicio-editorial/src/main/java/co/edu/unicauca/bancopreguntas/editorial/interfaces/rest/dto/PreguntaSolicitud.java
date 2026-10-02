package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cuerpo de {@code POST /preguntas} y {@code PUT /preguntas/{preguntaId}} ({@code PreguntaSolicitud},
 * CONTRATOS.md 8.1).
 *
 * <p>{@code clasificacion} y {@code nivelDificultad} son obligatorios (400 si faltan). El resto puede venir
 * incompleto: la pregunta queda en {@code BORRADOR} (D-02).</p>
 *
 * @param contexto        Contexto
 * @param preguntaDirecta Pregunta directa
 * @param opciones        Opciones de respuesta
 * @param justificacion   Justificación
 * @param bibliografia    referencias bibliográficas
 * @param clasificacion   identificadores del Catálogo (obligatorio)
 * @param nivelDificultad {@code BAJO}, {@code MEDIO} o {@code ALTO} (obligatorio)
 */
@Schema(name = "PreguntaSolicitud", description = "Componentes de la pregunta (CU-04, CU-05).")
public record PreguntaSolicitud(
        @Schema(example = "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.") String contexto,
        @Schema(example = "¿Cuál es la moda del conjunto de notas?") String preguntaDirecta,
        List<@Valid OpcionSolicitud> opciones,
        @Schema(example = "La moda es el valor que más se repite: 4,0 aparece dos veces.") String justificacion,
        List<String> bibliografia,
        @NotNull(message = "La clasificación es obligatoria.") @Valid ClasificacionSolicitud clasificacion,
        @NotNull(message = "El nivel de dificultad es obligatorio.")
        @Schema(allowableValues = {"BAJO", "MEDIO", "ALTO"}, example = "BAJO") String nivelDificultad) {

    /**
     * Opción de respuesta.
     *
     * @param letra      letra ({@code A}–{@code D})
     * @param texto      texto
     * @param esCorrecta si es la Respuesta correcta
     */
    @Schema(name = "OpcionSolicitud")
    public record OpcionSolicitud(@Schema(example = "C") String letra, @Schema(example = "4,0") String texto,
                                  @Schema(example = "true") Boolean esCorrecta) {
    }

    /**
     * Clasificación académica (solo identificadores, D-13).
     *
     * @param competenciaId UUID de la Competencia
     * @param temaId        UUID del Tema
     * @param subtemaId     UUID del Subtema
     */
    @Schema(name = "ClasificacionSolicitud")
    public record ClasificacionSolicitud(
            @NotNull(message = "competenciaId es obligatorio.") @Schema(example = "22222222-2222-4222-8222-000000000101") String competenciaId,
            @NotNull(message = "temaId es obligatorio.") @Schema(example = "22222222-2222-4222-8222-000000000201") String temaId,
            @NotNull(message = "subtemaId es obligatorio.") @Schema(example = "22222222-2222-4222-8222-000000000301") String subtemaId) {
    }
}
