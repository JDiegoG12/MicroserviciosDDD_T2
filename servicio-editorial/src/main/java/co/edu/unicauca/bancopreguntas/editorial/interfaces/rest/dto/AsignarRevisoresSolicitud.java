package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Cuerpo de {@code POST /preguntas/{preguntaId}/procesos-revision} (CU-10; CONTRATOS.md 8.1).
 *
 * @param revisoresIds UUID de los Revisores (mínimo dos, sin repetir, ninguno igual al Autor)
 */
@Schema(name = "AsignarRevisoresSolicitud")
public record AsignarRevisoresSolicitud(
        @NotNull(message = "revisoresIds es obligatorio.")
        @ArraySchema(schema = @Schema(example = "11111111-1111-4111-8111-000000000003")) List<String> revisoresIds) {
}
