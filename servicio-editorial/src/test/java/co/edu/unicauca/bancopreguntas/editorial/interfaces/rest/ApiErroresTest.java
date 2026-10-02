package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ExcepcionDeAplicacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.AccesoDenegadoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.ExcepcionDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.MapaCodigosHttp;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Errores de la API: 401 sin encabezados, 403 por rol, 400 por cuerpo o parámetros inválidos, formato
 * {@code application/problem+json} con {@code codigo} (CONTRATOS.md 5.2) y el mapeo de cada código a su HTTP (5.3).
 */
@DisplayName("API REST · errores (CONTRATOS.md 4.1, 5.2 y 5.3)")
class ApiErroresTest extends PruebaDeApi {

    @Test
    @DisplayName("Sin X-Usuario-Id o sin X-Roles → 401 NO_AUTENTICADO en problem+json, sin llamar al caso de uso")
    void sinEncabezados() throws Exception {
        mvc.perform(get("/api/v1/preguntas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/v1/preguntas").header("X-Usuario-Id", AUTOR))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/preguntas").header("X-Roles", "AUTOR"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(consultarPreguntas);
    }

    @Test
    @DisplayName("Rol incorrecto → 403 ACCESO_DENEGADO con todos los campos de 5.2")
    void rolIncorrecto() throws Exception {
        when(publicarPregunta.ejecutar(any(), any())).thenThrow(new AccesoDenegadoExcepcion("Requiere ADMINISTRADOR."));

        mvc.perform(post("/api/v1/preguntas/" + PREGUNTA_ID + "/publicacion").headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://banco-preguntas/errores/ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Requiere ADMINISTRADOR."))
                .andExpect(jsonPath("$.instance").value("/api/v1/preguntas/" + PREGUNTA_ID + "/publicacion"))
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.idCorrelacion").value(CORRELACION))
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    @DisplayName("X-Usuario-Id que no es UUID o rol desconocido → 400 SOLICITUD_INVALIDA")
    void encabezadosMalFormados() throws Exception {
        mvc.perform(get("/api/v1/preguntas").headers(identidad("no-es-uuid", "AUTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        mvc.perform(get("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR,COORDINADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("Cuerpo sin clasificación ni nivel → 400 con la lista errores (campo, mensaje)")
    void cuerpoSinCamposObligatorios() throws Exception {
        mvc.perform(post("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR")).contentType("application/json")
                        .content("{\"contexto\":\"Solo contexto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'clasificacion')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nivelDificultad')]").exists());
        verifyNoInteractions(crearPregunta);
    }

    @Test
    @DisplayName("JSON ilegible, cuerpo ausente o parámetro de tipo incorrecto → 400 SOLICITUD_INVALIDA")
    void solicitudesMalFormadas() throws Exception {
        mvc.perform(post("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR")).contentType("application/json").content("{ no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.errores[0].campo").value("cuerpo"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("El cuerpo de la solicitud no es un JSON válido"));
        mvc.perform(post("/api/v1/preguntas/" + PREGUNTA_ID + "/archivado").headers(identidad(ADMINISTRADOR, "ADMINISTRADOR"))
                        .contentType("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        mvc.perform(get("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR")).param("pagina", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("pagina"));
    }

    @Test
    @DisplayName("Un dato inválido que detecta el dominio (por ejemplo tamano > 100) → 400")
    void datoInvalidoDelDominio() throws Exception {
        when(consultarPreguntas.ejecutar(any(), any())).thenThrow(new DatoInvalidoExcepcion("El tamaño de página debe estar entre 1 y 100."));

        mvc.perform(get("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR")).param("tamano", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "SOLICITUD_INVALIDA, 400",
            "ACCESO_DENEGADO, 403",
            "REVISOR_NO_ASIGNADO, 403",
            "PREGUNTA_NO_ENCONTRADA, 404",
            "PROCESO_REVISION_NO_ENCONTRADO, 404",
            "TRANSICION_NO_PERMITIDA, 409",
            "PREGUNTA_NO_EDITABLE, 409",
            "EVALUACION_YA_REGISTRADA, 409",
            "PROCESO_CERRADO, 409",
            "CLASIFICACION_INVALIDA, 422",
            "REVISORES_INSUFICIENTES, 422",
            "AUTOR_NO_PUEDE_SER_REVISOR, 422",
            "REVISOR_DUPLICADO, 422",
            "CATALOGO_NO_DISPONIBLE, 503",
            "BASE_DE_DATOS_NO_DISPONIBLE, 503",
            "ERROR_INTERNO, 500"})
    @DisplayName("Cada código del dominio o de la aplicación se traduce al HTTP de la tabla 5.3")
    void mapeoDeCodigos(String codigo, int estadoEsperado) throws Exception {
        RuntimeException error = codigo.startsWith("PREGUNTA_NO_ENC") || codigo.startsWith("PROCESO_REVISION")
                || codigo.startsWith("CLASIFICACION") || codigo.startsWith("CATALOGO")
                ? new ExcepcionDeAplicacion(codigo, "detalle") { }
                : new ExcepcionDeDominio(codigo, "detalle") { };
        when(obtenerPregunta.ejecutar(any(), any())).thenThrow(error);

        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().is(estadoEsperado))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.status").value(estadoEsperado));
    }

    @Test
    @DisplayName("La tabla de códigos cubre exactamente los 21 códigos que puede emitir Editorial")
    void tablaCompleta() {
        assertThat(MapaCodigosHttp.todos()).hasSize(21).containsKeys("NO_AUTENTICADO", "CONFLICTO_DE_CONCURRENCIA",
                "RECURSO_NO_ENCONTRADO", "METODO_NO_PERMITIDO", "TIPO_DE_CONTENIDO_NO_SOPORTADO");
    }

    @Test
    @DisplayName("Esquema no listo (migraciones pendientes) → 503 BASE_DE_DATOS_NO_DISPONIBLE; /salud sigue en 200")
    void esquemaNoListo() throws Exception {
        when(estadoDelEsquema.esquemaListo()).thenReturn(false);

        mvc.perform(get("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("BASE_DE_DATOS_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.idCorrelacion").value(CORRELACION));
        mvc.perform(get("/salud")).andExpect(status().isOk());
        verifyNoInteractions(consultarPreguntas);
    }

    @Test
    @DisplayName("X-Roles vacío o solo con comas → 401 NO_AUTENTICADO (CONTRATOS 4.1 v1.9)")
    void rolesVacios() throws Exception {
        mvc.perform(get("/api/v1/preguntas").header("X-Usuario-Id", AUTOR).header("X-Roles", ""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(get("/api/v1/preguntas").header("X-Usuario-Id", AUTOR).header("X-Roles", " , ,"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("X-Id-Correlacion inválido → nunca es error: se genera uno nuevo y se devuelve (CONTRATOS 4.1 v1.10)")
    void correlacionInvalida() throws Exception {
        mvc.perform(get("/salud").header("X-Id-Correlacion", "no-es-un-uuid"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Id-Correlacion",
                        Matchers.matchesPattern("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")));
    }

    @Test
    @DisplayName("UUID: mayúsculas se aceptan y normalizan; llaves, urn:uuid: o sin guiones → 400 (CONTRATOS 4 v1.9)")
    void formasDeUuid() throws Exception {
        when(obtenerPregunta.ejecutar(any(), any())).thenReturn(pregunta("PUBLICADA"));
        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR.toUpperCase(), "DOCENTE")))
                .andExpect(status().isOk());
        ArgumentCaptor<UsuarioActual> usuario =
                ArgumentCaptor.forClass(UsuarioActual.class);
        verify(obtenerPregunta).ejecutar(usuario.capture(), any());
        assertThat(usuario.getValue().id().toString()).isEqualTo(AUTOR);

        for (String invalido : new String[]{"{" + AUTOR + "}", "urn:uuid:" + AUTOR, AUTOR.replace("-", "")}) {
            mvc.perform(get("/api/v1/preguntas").headers(identidad(invalido, "AUTOR")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        }
    }

    @Test
    @DisplayName("Base de datos caída → 503 BASE_DE_DATOS_NO_DISPONIBLE")
    void baseDeDatosCaida() throws Exception {
        when(obtenerPregunta.ejecutar(any(), any())).thenThrow(new CannotCreateTransactionException("sin conexión"));
        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("BASE_DE_DATOS_NO_DISPONIBLE"));

        doThrow(new RuntimeException("envoltorio", new DataAccessResourceFailureException("sin conexión")))
                .when(obtenerPregunta).ejecutar(any(), any());
        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Conflicto de bloqueo optimista → 409 CONFLICTO_DE_CONCURRENCIA (CONTRATOS 5.3 v1.10)")
    void conflictoDeConcurrencia() throws Exception {
        when(modificarPregunta.ejecutar(any(), any())).thenThrow(new ObjectOptimisticLockingFailureException("PreguntaEntidad", PREGUNTA_ID));
        mvc.perform(put("/api/v1/preguntas/" + PREGUNTA_ID)
                        .headers(identidad(AUTOR, "AUTOR")).contentType("application/json").content(cuerpoPregunta()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_DE_CONCURRENCIA"))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("intentarlo")));
    }

    @Test
    @DisplayName("Error inesperado → 500 ERROR_INTERNO sin trazas en la respuesta")
    void errorInesperado() throws Exception {
        when(obtenerPregunta.ejecutar(any(), any())).thenThrow(new IllegalStateException("detalle interno secreto"));

        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(content().string(Matchers.not(Matchers.containsString("secreto"))))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Exception"))));
    }

    @Test
    @DisplayName("Ruta inexistente → 404 RECURSO_NO_ENCONTRADO (CONTRATOS 5.3 v1.10)")
    void rutaInexistente() throws Exception {
        mvc.perform(get("/api/v1/no-existe").headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.idCorrelacion").value(CORRELACION));
    }

    @Test
    @DisplayName("Método no permitido → 405 METODO_NO_PERMITIDO (CONTRATOS 5.3 v1.10)")
    void metodoNoPermitido() throws Exception {
        mvc.perform(delete("/api/v1/preguntas/" + PREGUNTA_ID)
                        .headers(identidad(ADMINISTRADOR, "ADMINISTRADOR")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"))
                .andExpect(jsonPath("$.idCorrelacion").value(CORRELACION));
    }

    @Test
    @DisplayName("Tipo de contenido no soportado → 415 TIPO_DE_CONTENIDO_NO_SOPORTADO (CONTRATOS 5.3 v1.10)")
    void tipoDeContenidoNoSoportado() throws Exception {
        mvc.perform(post("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR")).contentType("text/plain").content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.codigo").value("TIPO_DE_CONTENIDO_NO_SOPORTADO"))
                .andExpect(jsonPath("$.idCorrelacion").value(CORRELACION));
        verifyNoInteractions(crearPregunta);
    }
}
