package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ArchivarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.AsignarRevisores;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarPreguntas;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarProcesosRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.CrearPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.EnviarPreguntaARevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ModificarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ObtenerProcesoRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.PublicarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.RegistrarEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ClasificacionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaResumen;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.TrazabilidadRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.EstadoDelEsquema;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Base de las pruebas de la API REST: levanta solo la capa web ({@code @WebMvcTest}) con los puertos de entrada
 * simulados, sin base de datos, broker ni Catálogo (Etapa 2, "Pruebas de la API").
 */
@WebMvcTest(controllers = {PreguntaControlador.class, ProcesoRevisionControlador.class, SaludControlador.class,
        DocumentacionControlador.class})
abstract class PruebaDeApi {

    /** UUID de la pregunta de ejemplo. */
    static final String PREGUNTA_ID = "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f";
    /** UUID del proceso de ejemplo. */
    static final String PROCESO_ID = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";
    /** Autor de prueba (CONTRATOS.md 4.2). */
    static final String AUTOR = "11111111-1111-4111-8111-000000000002";
    /** Administrador de prueba. */
    static final String ADMINISTRADOR = "11111111-1111-4111-8111-000000000001";
    /** Revisor 1 de prueba. */
    static final String REVISOR = "11111111-1111-4111-8111-000000000003";
    /** Correlación de prueba. */
    static final String CORRELACION = "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b";
    /** Instante de ejemplo. */
    static final Instant FECHA = Instant.parse("2026-10-01T15:30:00Z");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CrearPregunta crearPregunta;
    @MockitoBean
    ModificarPregunta modificarPregunta;
    @MockitoBean
    ConsultarPreguntas consultarPreguntas;
    @MockitoBean
    ObtenerPregunta obtenerPregunta;
    @MockitoBean
    EnviarPreguntaARevision enviarPreguntaARevision;
    @MockitoBean
    AsignarRevisores asignarRevisores;
    @MockitoBean
    PublicarPregunta publicarPregunta;
    @MockitoBean
    ArchivarPregunta archivarPregunta;
    @MockitoBean
    ConsultarTrazabilidad consultarTrazabilidad;
    @MockitoBean
    ObtenerProcesoRevision obtenerProcesoRevision;
    @MockitoBean
    ConsultarProcesosRevision consultarProcesosRevision;
    @MockitoBean
    RegistrarEvaluacion registrarEvaluacion;
    @MockitoBean
    EstadoDelEsquema estadoDelEsquema;

    /**
     * Por defecto las migraciones ya terminaron; las pruebas del 503 lo cambian (CONTRATOS.md 9.3.6).
     */
    @BeforeEach
    void esquemaListoPorDefecto() {
        when(estadoDelEsquema.esquemaListo()).thenReturn(true);
    }

    /**
     * Encabezados de identidad y correlación.
     *
     * @param usuarioId UUID del usuario
     * @param roles     roles separados por comas
     * @return encabezados HTTP
     */
    static HttpHeaders identidad(String usuarioId, String roles) {
        HttpHeaders encabezados = new HttpHeaders();
        encabezados.add("X-Usuario-Id", usuarioId);
        encabezados.add("X-Roles", roles);
        encabezados.add("X-Id-Correlacion", CORRELACION);
        return encabezados;
    }

    /** @return cuerpo válido de {@code PreguntaSolicitud} (CONTRATOS.md 8.1) */
    static String cuerpoPregunta() {
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
                  "justificacion": "La moda es el valor que más se repite.",
                  "bibliografia": ["Walpole"],
                  "clasificacion": {
                    "competenciaId": "22222222-2222-4222-8222-000000000101",
                    "temaId": "22222222-2222-4222-8222-000000000201",
                    "subtemaId": "22222222-2222-4222-8222-000000000301"
                  },
                  "nivelDificultad": "BAJO"
                }""";
    }

    /**
     * Resultado de pregunta de ejemplo.
     *
     * @param estado estado del ciclo de vida
     * @return resultado del caso de uso
     */
    static PreguntaRespuesta pregunta(String estado) {
        return new PreguntaRespuesta(PREGUNTA_ID, AUTOR, "Contexto", "¿Pregunta?",
                List.of(new PreguntaRespuesta.OpcionRespuesta("A", "3,0", false),
                        new PreguntaRespuesta.OpcionRespuesta("B", "3,8", false),
                        new PreguntaRespuesta.OpcionRespuesta("C", "4,0", true),
                        new PreguntaRespuesta.OpcionRespuesta("D", "4,5", false)),
                "Justificación", List.of("Walpole"), clasificacion(), "BAJO", estado, List.of(), FECHA, FECHA);
    }

    /** @return resumen de pregunta de ejemplo */
    static PreguntaResumen resumen() {
        return new PreguntaResumen(PREGUNTA_ID, AUTOR, "¿Pregunta?", "PUBLICADA", clasificacion(), "BAJO", FECHA);
    }

    /** @return proceso de ejemplo con dictamen */
    static ProcesoRevisionRespuesta proceso() {
        return new ProcesoRevisionRespuesta(PROCESO_ID, PREGUNTA_ID, "CERRADO",
                List.of(new ProcesoRevisionRespuesta.AsignacionRespuesta(REVISOR, FECHA, true)),
                List.of(new ProcesoRevisionRespuesta.EvaluacionRespuesta(REVISOR,
                        List.of(new ProcesoRevisionRespuesta.CriterioRespuesta("PEDAGOGICO", 4)),
                        List.of("Bien"), "APROBATORIA", FECHA)),
                new ProcesoRevisionRespuesta.DictamenRespuesta("APROBADA", new BigDecimal("100.00"), FECHA));
    }

    /** @return trazabilidad de ejemplo */
    static TrazabilidadRespuesta trazabilidadDeEjemplo() {
        return new TrazabilidadRespuesta(PREGUNTA_ID,
                List.of(new TrazabilidadRespuesta.RegistroRespuesta(FECHA, AUTOR, "CREACION", null, "BORRADOR", "Creación")),
                List.of(new TrazabilidadRespuesta.EntradaHistorialRespuesta("DICTAMEN", PROCESO_ID, FECHA, null,
                        new ProcesoRevisionRespuesta.DictamenRespuesta("APROBADA", new BigDecimal("100.00"), FECHA))));
    }

    private static ClasificacionRespuesta clasificacion() {
        return new ClasificacionRespuesta("22222222-2222-4222-8222-000000000101", "22222222-2222-4222-8222-000000000201",
                "22222222-2222-4222-8222-000000000301");
    }
}
