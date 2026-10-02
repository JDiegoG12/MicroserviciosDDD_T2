package co.edu.unicauca.bancopreguntas.editorial.integracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.CatalogoNoDisponibleExcepcion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La API completa sobre el contexto real (PostgreSQL y RabbitMQ en contenedores): documentación OpenAPI en
 * {@code /openapi.json} y {@code /docs}, el flujo de punta a punta por HTTP (CONTRATOS.md 10) y el 503 cuando Catálogo
 * no responde.
 */
@AutoConfigureMockMvc
@DisplayName("API completa sobre el contexto real (Testcontainers)")
class ApiCompletaIT extends PruebaDeIntegracion {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String AUTOR = "11111111-1111-4111-8111-000000000002";
    private static final String ADMIN = "11111111-1111-4111-8111-000000000001";
    private static final String REVISOR_1 = "11111111-1111-4111-8111-000000000003";
    private static final String REVISOR_2 = "11111111-1111-4111-8111-000000000004";

    @Autowired
    MockMvc mvc;

    private static String cuerpoPregunta(String competenciaId) {
        return """
                {
                  "contexto": "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.",
                  "preguntaDirecta": "¿Cuál es la moda del conjunto de notas?",
                  "opciones": [
                    { "letra": "A", "texto": "3,0", "esCorrecta": false },
                    { "letra": "B", "texto": "3,8", "esCorrecta": false },
                    { "letra": "C", "texto": "4,0", "esCorrecta": true },
                    { "letra": "D", "texto": "4,5", "esCorrecta": false }
                  ],
                  "justificacion": "La moda es el valor que más se repite: 4,0 aparece dos veces.",
                  "bibliografia": ["Walpole, R. Probabilidad y estadística."],
                  "clasificacion": { "competenciaId": "%s", "temaId": "22222222-2222-4222-8222-000000000201",
                                     "subtemaId": "22222222-2222-4222-8222-000000000301" },
                  "nivelDificultad": "BAJO"
                }""".formatted(competenciaId);
    }

    private static final String EVALUACION_APROBATORIA = """
            {"criterios":[{"criterio":"PEDAGOGICO","valoracion":4},{"criterio":"TECNICO","valoracion":5},
            {"criterio":"ESTRUCTURAL","valoracion":4}],"observaciones":["Bien"],"decision":"APROBATORIA"}""";

    private JsonNode json(MvcResult resultado) throws Exception {
        return JSON.readTree(resultado.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("/openapi.json documenta los 12 endpoints, los encabezados de identidad y los errores problem+json")
    void openApi() throws Exception {
        JsonNode api = json(mvc.perform(get("/openapi.json")).andExpect(status().isOk()).andReturn());

        JsonNode rutas = api.get("paths");
        assertThat(rutas.has("/api/v1/preguntas")).isTrue();
        assertThat(rutas.has("/api/v1/preguntas/{preguntaId}/procesos-revision")).isTrue();
        assertThat(rutas.has("/api/v1/procesos-revision/{procesoId}/evaluaciones")).isTrue();
        assertThat(rutas.has("/salud")).isTrue();
        JsonNode crear = rutas.at("/~1api~1v1~1preguntas/post");
        assertThat(crear.get("parameters").toString()).contains("X-Usuario-Id", "X-Roles", "X-Id-Correlacion");
        assertThat(crear.get("responses").propertyNames()).contains("201", "400", "401", "403", "422", "503", "500");
        assertThat(crear.at("/responses/422/content").has("application/problem+json")).isTrue();
        assertThat(api.at("/components/schemas").has("Problema")).isTrue();
        // CONTRATOS 5.3 v1.10: 409 CONFLICTO_DE_CONCURRENCIA en los endpoints que modifican agregados existentes.
        assertThat(rutas.at("/~1api~1v1~1preguntas~1{preguntaId}/put/responses/409/description").asString())
                .contains("CONFLICTO_DE_CONCURRENCIA");
        assertThat(rutas.at("/~1api~1v1~1procesos-revision~1{procesoId}~1evaluaciones/post/responses/409/description").asString())
                .contains("CONFLICTO_DE_CONCURRENCIA");
        assertThat(rutas.at("/~1api~1v1~1preguntas~1{preguntaId}~1envio-revision/post/responses/409/description").asString())
                .contains("CONFLICTO_DE_CONCURRENCIA");
        assertThat(crear.has("responses") && crear.get("responses").has("409")).isFalse();
    }

    @Test
    @DisplayName("/docs sirve Swagger UI con 200 (sin redirección) y sus recursos existen")
    void swaggerUi() throws Exception {
        String pagina = mvc.perform(get("/docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(pagina).contains("/openapi.json", "/swagger-ui/swagger-ui-bundle.js");
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/swagger-ui.css")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Flujo de punta a punta por HTTP: crear, enviar, asignar, evaluar ×2, publicar y consultar la trazabilidad")
    void flujoCompletoPorHttp() throws Exception {
        String competencia = UUID.randomUUID().toString();
        JsonNode creada = json(mvc.perform(post("/api/v1/preguntas").header("X-Usuario-Id", AUTOR).header("X-Roles", "AUTOR")
                        .contentType("application/json").content(cuerpoPregunta(competencia)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("EN_CONSTRUCCION")).andReturn());
        String preguntaId = creada.get("preguntaId").asString();
        assertThat(creada.get("fechaCreacion").asString()).endsWith("Z");

        mvc.perform(post("/api/v1/preguntas/" + preguntaId + "/envio-revision").header("X-Usuario-Id", AUTOR).header("X-Roles", "AUTOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));

        JsonNode proceso = json(mvc.perform(post("/api/v1/preguntas/" + preguntaId + "/procesos-revision")
                        .header("X-Usuario-Id", ADMIN).header("X-Roles", "ADMINISTRADOR").contentType("application/json")
                        .content("{\"revisoresIds\":[\"" + REVISOR_1 + "\",\"" + REVISOR_2 + "\"]}"))
                .andExpect(status().isCreated()).andReturn());
        String procesoId = proceso.get("procesoId").asString();

        for (String revisor : new String[]{REVISOR_1, REVISOR_2}) {
            mvc.perform(post("/api/v1/procesos-revision/" + procesoId + "/evaluaciones").header("X-Usuario-Id", revisor)
                            .header("X-Roles", "REVISOR").contentType("application/json").content(EVALUACION_APROBATORIA))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/v1/procesos-revision/" + procesoId).header("X-Usuario-Id", ADMIN).header("X-Roles", "ADMINISTRADOR"))
                .andExpect(jsonPath("$.dictamen.resultado").value("APROBADA"))
                .andExpect(jsonPath("$.dictamen.porcentajeAprobacion").value(100.0));

        mvc.perform(post("/api/v1/preguntas/" + preguntaId + "/publicacion").header("X-Usuario-Id", ADMIN).header("X-Roles", "ADMINISTRADOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"));

        mvc.perform(get("/api/v1/preguntas/" + preguntaId + "/trazabilidad").header("X-Usuario-Id", ADMIN).header("X-Roles", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registros.length()").value(6))
                .andExpect(jsonPath("$.historialRevisiones.length()").value(3))
                .andExpect(jsonPath("$.historialRevisiones[2].tipo").value("DICTAMEN"));

        mvc.perform(post("/api/v1/preguntas/" + preguntaId + "/publicacion").header("X-Usuario-Id", ADMIN).header("X-Roles", "ADMINISTRADOR"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TRANSICION_NO_PERMITIDA"));
    }

    @Test
    @DisplayName("Catálogo caído → 503 CATALOGO_NO_DISPONIBLE y la pregunta no se guarda")
    void catalogoCaido() throws Exception {
        when(catalogo.validarClasificacion(any())).thenThrow(new CatalogoNoDisponibleExcepcion("sin respuesta", null));
        String competencia = UUID.randomUUID().toString();

        mvc.perform(post("/api/v1/preguntas").header("X-Usuario-Id", AUTOR).header("X-Roles", "AUTOR")
                        .contentType("application/json").content(cuerpoPregunta(competencia)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("CATALOGO_NO_DISPONIBLE"));
        mvc.perform(get("/api/v1/preguntas").param("competenciaId", competencia).header("X-Usuario-Id", ADMIN)
                        .header("X-Roles", "ADMINISTRADOR"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }
}
