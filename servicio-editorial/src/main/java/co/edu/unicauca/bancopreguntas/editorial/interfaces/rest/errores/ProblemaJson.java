package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Cuerpo de error único {@code application/problem+json} (RFC 7807; CONTRATOS.md 5.2).
 *
 * @param type          URI del tipo de error ({@code https://banco-preguntas/errores/<codigo>})
 * @param title         título legible
 * @param status        estado HTTP
 * @param detail        explicación concreta
 * @param instance      ruta de la petición
 * @param codigo        código de contrato (tabla 5.3)
 * @param idCorrelacion correlación de la petición
 * @param errores       errores de campo (vacío si no aplica)
 */
@Schema(name = "Problema", description = "Error único application/problem+json (CONTRATOS.md 5.2).")
public record ProblemaJson(String type, String title, int status, String detail, String instance, String codigo,
                           String idCorrelacion, List<ErrorDeCampo> errores) {

    /** Prefijo de {@code type} (CONTRATOS.md 5.2). */
    public static final String PREFIJO_TIPO = "https://banco-preguntas/errores/";

    /**
     * Error de un campo concreto de la solicitud.
     *
     * @param campo   nombre o ruta del campo
     * @param mensaje explicación
     */
    @Schema(name = "ErrorDeCampo")
    public record ErrorDeCampo(String campo, String mensaje) {
    }
}
