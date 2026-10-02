package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.openapi;

import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.ProblemaJson;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Documentación OpenAPI del servicio (CONTRATOS.md 3.2 y 5.1: Swagger UI en {@code /docs} y JSON en
 * {@code /openapi.json}).
 *
 * <p>A cada operación de {@code /api/v1} le agrega los encabezados de identidad y correlación (4.1) y las respuestas
 * de error comunes a todos los endpoints (400, 401, 500 y 503 de base de datos), con el cuerpo
 * {@code application/problem+json}. Los errores propios de cada endpoint se documentan en su controlador.</p>
 */
@Configuration
public class ConfiguracionOpenApi {

    private static final String PREFIJO_API = "/api/v1";
    private static final String TIPO_PROBLEMA = "application/problem+json";
    private static final String REF_PROBLEMA = "#/components/schemas/Problema";

    /**
     * Información general del API.
     *
     * @return documento OpenAPI base
     */
    @Bean
    public OpenAPI apiEditorial() {
        Components componentes = new Components();
        // Registra el esquema "Problema" (y su "ErrorDeCampo") para referenciarlo desde todas las respuestas de error.
        ModelConverters.getInstance().readAll(ProblemaJson.class).forEach(componentes::addSchemas);
        return new OpenAPI()
                .info(new Info()
                        .title("servicio-editorial · Gestión Editorial de Preguntas")
                        .version("v1")
                        .description("API REST de CONTRATOS.md 8.1: ciclo de vida de las preguntas, revisión por pares, "
                                + "publicación y archivado. Errores en application/problem+json (5.2) con el campo codigo."))
                .components(componentes);
    }

    /**
     * Agrega los encabezados de identidad y las respuestas de error comunes a cada operación de {@code /api/v1}.
     *
     * @return personalizador de springdoc
     */
    @Bean
    public OpenApiCustomizer encabezadosYErroresComunes() {
        return api -> api.getPaths().forEach((ruta, item) -> {
            if (!ruta.startsWith(PREFIJO_API)) {
                return;
            }
            item.readOperations().forEach(operacion -> {
                operacion.addParametersItem(encabezado("X-Usuario-Id", true,
                        "UUID del usuario que llama (CONTRATOS.md 4.1).", "11111111-1111-4111-8111-000000000002"));
                operacion.addParametersItem(encabezado("X-Roles", true,
                        "Roles del usuario separados por comas: ADMINISTRADOR, AUTOR, REVISOR, DOCENTE, ESTUDIANTE.", "AUTOR"));
                operacion.addParametersItem(encabezado("X-Id-Correlacion", false,
                        "UUID de correlación; si no llega, el servicio lo genera y lo devuelve.", "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b"));
                ApiResponses respuestas = operacion.getResponses();
                agregarSiFalta(respuestas, "400", "SOLICITUD_INVALIDA: JSON ilegible, campo obligatorio ausente, UUID o enum mal escrito, o paginación fuera de rango.");
                agregarSiFalta(respuestas, "401", "NO_AUTENTICADO: faltan X-Usuario-Id o X-Roles.");
                agregarSiFalta(respuestas, "500", "ERROR_INTERNO: error inesperado (sin trazas).");
                agregarSiFalta(respuestas, "503", "BASE_DE_DATOS_NO_DISPONIBLE: la base de datos no responde o sus "
                        + "migraciones aún no terminan.");
            });
            documentarConflictoDeConcurrencia(ruta, item);
        });
    }

    // CONTRATOS 5.3 (v1.10): los endpoints que modifican una Pregunta o un ProcesoDeRevision existente pueden responder
    // 409 CONFLICTO_DE_CONCURRENCIA (bloqueo optimista). Crear una pregunta (POST /preguntas) no modifica nada existente.
    private static void documentarConflictoDeConcurrencia(String ruta, PathItem item) {
        String descripcion = "CONFLICTO_DE_CONCURRENCIA: otra petición modificó el mismo recurso al mismo tiempo; puede reintentar.";
        Stream.of(item.getPut(), ruta.equals(PREFIJO_API + "/preguntas") ? null : item.getPost())
                .filter(Objects::nonNull)
                .forEach(operacion -> {
                    ApiResponse existente = operacion.getResponses().get("409");
                    if (existente == null) {
                        agregarSiFalta(operacion.getResponses(), "409", descripcion);
                    } else {
                        existente.setDescription(existente.getDescription() + " · " + descripcion);
                    }
                });
    }

    private static HeaderParameter encabezado(String nombre, boolean obligatorio, String descripcion, String ejemplo) {
        HeaderParameter parametro = new HeaderParameter();
        parametro.setName(nombre);
        parametro.setRequired(obligatorio);
        parametro.setDescription(descripcion);
        parametro.setSchema(new StringSchema());
        parametro.setExample(ejemplo);
        return parametro;
    }

    private static void agregarSiFalta(ApiResponses respuestas, String estado, String descripcion) {
        if (respuestas.containsKey(estado)) {
            return;
        }
        Schema<?> esquema = new Schema<>().$ref(REF_PROBLEMA);
        MediaType tipo = new MediaType().schema(esquema)
                .examples(Map.of("problema", new Example().value(EjemplosOpenApi.PROBLEMA)));
        respuestas.addApiResponse(estado, new ApiResponse().description(descripcion)
                .content(new Content().addMediaType(TIPO_PROBLEMA, tipo)));
    }
}
