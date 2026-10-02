package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores;

import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;

/**
 * Traducción de cada {@code codigo} de error al estado HTTP y al título de la tabla 5.3 de CONTRATOS.md.
 *
 * <p>Incluye todos los códigos que puede lanzar Editorial: los del dominio, los de la aplicación y los propios de
 * la API ({@code NO_AUTENTICADO}, {@code BASE_DE_DATOS_NO_DISPONIBLE}, {@code ERROR_INTERNO}).</p>
 */
public final class MapaCodigosHttp {

    /** Código de una solicitud mal formada (400). */
    public static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";
    /** Código sin encabezados de identidad (401). */
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    /** Código de la base de datos caída (503). */
    public static final String BASE_DE_DATOS_NO_DISPONIBLE = "BASE_DE_DATOS_NO_DISPONIBLE";
    /** Código de un error inesperado (500). */
    public static final String ERROR_INTERNO = "ERROR_INTERNO";
    /** Código de un conflicto por estado (409). */
    public static final String TRANSICION_NO_PERMITIDA = "TRANSICION_NO_PERMITIDA";
    /** Código del bloqueo optimista (409, CONTRATOS.md 5.3 v1.10). */
    public static final String CONFLICTO_DE_CONCURRENCIA = "CONFLICTO_DE_CONCURRENCIA";
    /** Ruta inexistente (404, CONTRATOS.md 5.3 v1.10). */
    public static final String RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";
    /** Método HTTP no permitido (405, CONTRATOS.md 5.3 v1.10). */
    public static final String METODO_NO_PERMITIDO = "METODO_NO_PERMITIDO";
    /** Tipo de contenido no soportado (415, CONTRATOS.md 5.3 v1.10). */
    public static final String TIPO_DE_CONTENIDO_NO_SOPORTADO = "TIPO_DE_CONTENIDO_NO_SOPORTADO";

    /**
     * Estado HTTP y título de un código.
     *
     * @param estado estado HTTP
     * @param titulo título legible en español
     */
    public record Traduccion(HttpStatus estado, String titulo) {
    }

    private static final Map<String, Traduccion> CODIGOS = Map.ofEntries(
            Map.entry(SOLICITUD_INVALIDA, new Traduccion(HttpStatus.BAD_REQUEST, "Solicitud inválida")),
            Map.entry(NO_AUTENTICADO, new Traduccion(HttpStatus.UNAUTHORIZED, "Faltan los encabezados de identidad")),
            Map.entry("ACCESO_DENEGADO", new Traduccion(HttpStatus.FORBIDDEN, "Acceso denegado")),
            Map.entry("REVISOR_NO_ASIGNADO", new Traduccion(HttpStatus.FORBIDDEN, "Revisor no asignado al proceso")),
            Map.entry("PREGUNTA_NO_ENCONTRADA", new Traduccion(HttpStatus.NOT_FOUND, "Pregunta no encontrada")),
            Map.entry("PROCESO_REVISION_NO_ENCONTRADO", new Traduccion(HttpStatus.NOT_FOUND, "Proceso de revisión no encontrado")),
            Map.entry(TRANSICION_NO_PERMITIDA, new Traduccion(HttpStatus.CONFLICT, "Transición de estado no permitida")),
            Map.entry("PREGUNTA_NO_EDITABLE", new Traduccion(HttpStatus.CONFLICT, "La pregunta no es editable")),
            Map.entry("EVALUACION_YA_REGISTRADA", new Traduccion(HttpStatus.CONFLICT, "Evaluación ya registrada")),
            Map.entry("PROCESO_CERRADO", new Traduccion(HttpStatus.CONFLICT, "Proceso de revisión cerrado")),
            Map.entry(CONFLICTO_DE_CONCURRENCIA, new Traduccion(HttpStatus.CONFLICT, "Conflicto de concurrencia")),
            Map.entry(RECURSO_NO_ENCONTRADO, new Traduccion(HttpStatus.NOT_FOUND, "Recurso no encontrado")),
            Map.entry(METODO_NO_PERMITIDO, new Traduccion(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido")),
            Map.entry(TIPO_DE_CONTENIDO_NO_SOPORTADO, new Traduccion(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado")),
            Map.entry("CLASIFICACION_INVALIDA", new Traduccion(HttpStatus.UNPROCESSABLE_CONTENT, "Clasificación académica inválida")),
            Map.entry("REVISORES_INSUFICIENTES", new Traduccion(HttpStatus.UNPROCESSABLE_CONTENT, "Revisores insuficientes")),
            Map.entry("AUTOR_NO_PUEDE_SER_REVISOR", new Traduccion(HttpStatus.UNPROCESSABLE_CONTENT, "El autor no puede ser revisor")),
            Map.entry("REVISOR_DUPLICADO", new Traduccion(HttpStatus.UNPROCESSABLE_CONTENT, "Revisor duplicado")),
            Map.entry("CATALOGO_NO_DISPONIBLE", new Traduccion(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo Académico no disponible")),
            Map.entry(BASE_DE_DATOS_NO_DISPONIBLE, new Traduccion(HttpStatus.SERVICE_UNAVAILABLE, "Base de datos no disponible")),
            Map.entry(ERROR_INTERNO, new Traduccion(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno")));

    private MapaCodigosHttp() {
    }

    /**
     * Busca la traducción de un código.
     *
     * @param codigo código de contrato
     * @return estado y título, o vacío si el código no está en la tabla
     */
    public static Optional<Traduccion> traducir(String codigo) {
        return Optional.ofNullable(CODIGOS.get(codigo));
    }

    /**
     * Todos los códigos conocidos (para las pruebas del mapeo).
     *
     * @return mapa inmutable código → traducción
     */
    public static Map<String, Traduccion> todos() {
        return CODIGOS;
    }
}
