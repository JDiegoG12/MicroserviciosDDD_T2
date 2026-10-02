package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ExcepcionDeAplicacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.ExcepcionDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.correlacion.ContextoCorrelacion;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.util.List;

/**
 * Manejador global de errores: produce siempre {@code application/problem+json} con {@code codigo} e
 * {@code idCorrelacion} (CONTRATOS.md 5.2) y traduce cada código a su estado HTTP (tabla 5.3). Nunca expone trazas.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    /**
     * Excepciones del dominio (invariantes, transiciones, propiedad, formato de value objects).
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return problema con el estado de la tabla 5.3
     */
    @ExceptionHandler(ExcepcionDeDominio.class)
    public ResponseEntity<ProblemaJson> deDominio(ExcepcionDeDominio error, HttpServletRequest peticion) {
        return responder(error.getCodigo(), error.getMessage(), peticion, List.of());
    }

    /**
     * Excepciones de la aplicación (recursos inexistentes, Catálogo).
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return problema con el estado de la tabla 5.3
     */
    @ExceptionHandler(ExcepcionDeAplicacion.class)
    public ResponseEntity<ProblemaJson> deAplicacion(ExcepcionDeAplicacion error, HttpServletRequest peticion) {
        return responder(error.getCodigo(), error.getMessage(), peticion, List.of());
    }

    /**
     * Faltan {@code X-Usuario-Id} o {@code X-Roles} (CONTRATOS.md 4.1).
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return 401 {@code NO_AUTENTICADO}
     */
    @ExceptionHandler(NoAutenticadoExcepcion.class)
    public ResponseEntity<ProblemaJson> noAutenticado(NoAutenticadoExcepcion error, HttpServletRequest peticion) {
        return responder(MapaCodigosHttp.NO_AUTENTICADO, error.getMessage(), peticion, List.of());
    }

    /**
     * Cuerpo que no cumple la validación de forma (campos obligatorios ausentes).
     *
     * @param error   excepción de Jakarta Validation
     * @param peticion petición HTTP
     * @return 400 {@code SOLICITUD_INVALIDA} con la lista {@code errores}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemaJson> cuerpoInvalido(MethodArgumentNotValidException error, HttpServletRequest peticion) {
        List<ProblemaJson.ErrorDeCampo> errores = error.getBindingResult().getFieldErrors().stream()
                .map(ManejadorGlobalErrores::aErrorDeCampo)
                .toList();
        return responder(MapaCodigosHttp.SOLICITUD_INVALIDA, "La solicitud tiene campos inválidos.", peticion, errores);
    }

    /**
     * JSON ilegible, tipo incorrecto o cuerpo ausente.
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return 400 {@code SOLICITUD_INVALIDA}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemaJson> cuerpoIlegible(HttpMessageNotReadableException error, HttpServletRequest peticion) {
        return responder(MapaCodigosHttp.SOLICITUD_INVALIDA,
                "El cuerpo de la solicitud falta o no es un JSON válido para este recurso.", peticion, List.of());
    }

    /**
     * Parámetro de consulta o de ruta con un tipo incorrecto (por ejemplo, {@code pagina=abc}).
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return 400 {@code SOLICITUD_INVALIDA}
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemaJson> tipoIncorrecto(MethodArgumentTypeMismatchException error, HttpServletRequest peticion) {
        return responder(MapaCodigosHttp.SOLICITUD_INVALIDA, "El parámetro '" + error.getName() + "' tiene un formato inválido.",
                peticion, List.of(new ProblemaJson.ErrorDeCampo(error.getName(), "Formato inválido.")));
    }

    /**
     * Encabezado obligatorio ausente.
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return 401 si es un encabezado de identidad, 400 en otro caso
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemaJson> encabezadoAusente(MissingRequestHeaderException error, HttpServletRequest peticion) {
        return responder(MapaCodigosHttp.NO_AUTENTICADO, "Falta el encabezado " + error.getHeaderName() + ".", peticion, List.of());
    }

    /**
     * Base de datos caída: el proceso sigue vivo y responde 503 en vez de 500 (CONTRATOS.md 9.3.6).
     *
     * @param error   excepción de acceso a datos
     * @param peticion petición HTTP
     * @return 503 {@code BASE_DE_DATOS_NO_DISPONIBLE}
     */
    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class,
            JDBCConnectionException.class})
    public ResponseEntity<ProblemaJson> baseDeDatosCaida(Exception error, HttpServletRequest peticion) {
        LOG.error("La base de datos no responde: {}", error.getMessage());
        return responder(MapaCodigosHttp.BASE_DE_DATOS_NO_DISPONIBLE, "La base de datos no está disponible.", peticion, List.of());
    }

    /**
     * Las migraciones aún no terminan (CONTRATOS.md 9.3.6): 503 sin trazas, con un aviso en el log.
     *
     * @param error   excepción del interceptor de esquema
     * @param peticion petición HTTP
     * @return 503 {@code BASE_DE_DATOS_NO_DISPONIBLE}
     */
    @ExceptionHandler(BaseDeDatosNoDisponibleExcepcion.class)
    public ResponseEntity<ProblemaJson> esquemaNoListo(BaseDeDatosNoDisponibleExcepcion error, HttpServletRequest peticion) {
        LOG.warn("Petición rechazada: el esquema de la base de datos aún no está listo.");
        return responder(MapaCodigosHttp.BASE_DE_DATOS_NO_DISPONIBLE, error.getMessage(), peticion, List.of());
    }

    /**
     * Dos peticiones modificaron el mismo agregado a la vez (bloqueo optimista, CONTRATOS.md 11.1).
     *
     * <p>CONTRATOS 5.3 (v1.10): 409 {@code CONFLICTO_DE_CONCURRENCIA}; el cliente puede reintentar.</p>
     *
     * @param error   excepción de bloqueo optimista
     * @param peticion petición HTTP
     * @return 409 {@code CONFLICTO_DE_CONCURRENCIA}
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemaJson> conflictoDeConcurrencia(OptimisticLockingFailureException error,
                                                                HttpServletRequest peticion) {
        LOG.warn("Conflicto de concurrencia: {}", error.getMessage());
        return responder(MapaCodigosHttp.CONFLICTO_DE_CONCURRENCIA,
                "Otra operación modificó el recurso al mismo tiempo. Puede volver a intentarlo.", peticion, List.of());
    }

    /**
     * Errores de protocolo de Spring MVC (CONTRATOS 5.3 v1.10): ruta inexistente → 404 {@code RECURSO_NO_ENCONTRADO},
     * método no permitido → 405 {@code METODO_NO_PERMITIDO}, tipo de contenido no soportado → 415
     * {@code TIPO_DE_CONTENIDO_NO_SOPORTADO}. Cualquier otro error de protocolo no previsto conserva el estado que
     * propone Spring con el código {@code SOLICITUD_INVALIDA}.
     *
     * @param error   excepción de Spring con estado HTTP
     * @param peticion petición HTTP
     * @return problema en {@code application/problem+json}
     */
    private static ResponseEntity<ProblemaJson> deProtocolo(ErrorResponse error, HttpServletRequest peticion) {
        HttpStatus estado = HttpStatus.valueOf(error.getStatusCode().value());
        String codigo = switch (estado) {
            case NOT_FOUND -> MapaCodigosHttp.RECURSO_NO_ENCONTRADO;
            case METHOD_NOT_ALLOWED -> MapaCodigosHttp.METODO_NO_PERMITIDO;
            case UNSUPPORTED_MEDIA_TYPE -> MapaCodigosHttp.TIPO_DE_CONTENIDO_NO_SOPORTADO;
            default -> MapaCodigosHttp.SOLICITUD_INVALIDA;
        };
        String titulo = MapaCodigosHttp.traducir(codigo)
                .filter(traduccion -> traduccion.estado() == estado)
                .map(MapaCodigosHttp.Traduccion::titulo)
                .orElse(estado.getReasonPhrase());
        ProblemaJson problema = construir(codigo, estado, titulo, detalleEnEspanol(estado, error, peticion), peticion, List.of());
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }

    // CONTRATOS 3.1: los textos para el usuario van en español (el detalle de Spring viene en inglés).
    private static String detalleEnEspanol(HttpStatus estado, ErrorResponse error, HttpServletRequest peticion) {
        return switch (estado) {
            case NOT_FOUND -> "La ruta " + peticion.getRequestURI() + " no existe.";
            case METHOD_NOT_ALLOWED -> "El método " + peticion.getMethod() + " no está permitido en " + peticion.getRequestURI() + ".";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido " + peticion.getContentType()
                    + " no está soportado; use application/json.";
            default -> error.getBody().getDetail();
        };
    }

    /**
     * Cualquier otro error: 500 sin traza en la respuesta, con traza en el log.
     *
     * @param error   excepción
     * @param peticion petición HTTP
     * @return 500 {@code ERROR_INTERNO}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemaJson> inesperado(Exception error, HttpServletRequest peticion) {
        if (esFallaDeConexionConLaBaseDeDatos(error)) {
            return baseDeDatosCaida(error, peticion);
        }
        if (error instanceof ErrorResponse deSpring) {
            return deProtocolo(deSpring, peticion);
        }
        LOG.error("Error inesperado atendiendo {} {}", peticion.getMethod(), peticion.getRequestURI(), error);
        return responder(MapaCodigosHttp.ERROR_INTERNO, "Ocurrió un error inesperado.", peticion, List.of());
    }

    private static ResponseEntity<ProblemaJson> responder(String codigo, String detalle, HttpServletRequest peticion,
                                                          List<ProblemaJson.ErrorDeCampo> errores) {
        MapaCodigosHttp.Traduccion traduccion = MapaCodigosHttp.traducir(codigo)
                .orElseGet(() -> MapaCodigosHttp.traducir(MapaCodigosHttp.ERROR_INTERNO).orElseThrow());
        ProblemaJson problema = construir(codigo, traduccion.estado(), traduccion.titulo(), detalle, peticion, errores);
        return ResponseEntity.status(traduccion.estado()).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }

    private static ProblemaJson construir(String codigo, HttpStatus estado, String titulo, String detalle,
                                          HttpServletRequest peticion, List<ProblemaJson.ErrorDeCampo> errores) {
        return new ProblemaJson(ProblemaJson.PREFIJO_TIPO + codigo, titulo, estado.value(), detalle,
                peticion.getRequestURI(), codigo, ContextoCorrelacion.actual().orElse(null), errores);
    }

    // Hibernate o Spring pueden envolver la falla de conexión; se revisa toda la cadena de causas.
    private static boolean esFallaDeConexionConLaBaseDeDatos(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof JDBCConnectionException || causa instanceof SQLTransientConnectionException
                    || causa instanceof SQLNonTransientConnectionException
                    || causa instanceof CannotCreateTransactionException
                    || causa instanceof DataAccessResourceFailureException) {
                return true;
            }
        }
        return false;
    }

    private static ProblemaJson.ErrorDeCampo aErrorDeCampo(FieldError error) {
        return new ProblemaJson.ErrorDeCampo(error.getField(), error.getDefaultMessage());
    }
}
