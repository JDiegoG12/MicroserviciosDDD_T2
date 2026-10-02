package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /preguntas/{preguntaId}/archivado} (CU-09; CONTRATOS.md 8.1). Ese {@code motivo} viaja en el
 * evento {@code PreguntaArchivada} (7.5).
 *
 * @param motivo motivo obligatorio de 1 a 500 caracteres
 */
@Schema(name = "ArchivadoSolicitud")
public record ArchivadoSolicitud(
        @NotNull(message = "motivo es obligatorio.")
        @Schema(minLength = 1, maxLength = 500, example = "Contenido desactualizado") String motivo) {
}
