package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarPreguntasConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caso feliz de cada endpoint de CONTRATOS.md 8.1, más {@code /salud}: método, ruta, código de éxito, encabezado
 * {@code Location} en los {@code POST} que crean, forma del JSON y fechas en texto ISO-8601 UTC con {@code Z}.
 */
@DisplayName("API REST · casos felices (CONTRATOS.md 8.1)")
class ApiCasosFelicesTest extends PruebaDeApi {

    @Test
    @DisplayName("POST /preguntas → 201 con Location y PreguntaRespuesta; arma el comando y el UsuarioActual")
    void crearPregunta() throws Exception {
        when(crearPregunta.ejecutar(any(), any())).thenReturn(pregunta("EN_CONSTRUCCION"));

        mvc.perform(post("/api/v1/preguntas").headers(identidad(AUTOR, "AUTOR, DOCENTE"))
                        .contentType("application/json").content(cuerpoPregunta()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/preguntas/" + PREGUNTA_ID))
                .andExpect(header().string("X-Id-Correlacion", CORRELACION))
                .andExpect(jsonPath("$.preguntaId").value(PREGUNTA_ID))
                .andExpect(jsonPath("$.estado").value("EN_CONSTRUCCION"))
                .andExpect(jsonPath("$.opciones[2].esCorrecta").value(true))
                .andExpect(jsonPath("$.clasificacion.temaId").value("22222222-2222-4222-8222-000000000201"))
                .andExpect(jsonPath("$.erroresValidacion").isArray())
                .andExpect(jsonPath("$.fechaCreacion").value("2026-10-01T15:30:00Z"));

        ArgumentCaptor<UsuarioActual> usuario = ArgumentCaptor.forClass(UsuarioActual.class);
        ArgumentCaptor<DatosDePreguntaComando> datos = ArgumentCaptor.forClass(DatosDePreguntaComando.class);
        verify(crearPregunta).ejecutar(usuario.capture(), datos.capture());
        assertThat(usuario.getValue().id().toString()).isEqualTo(AUTOR);
        assertThat(usuario.getValue().roles()).isEqualTo(Set.of(Rol.AUTOR, Rol.DOCENTE));
        assertThat(datos.getValue().opciones()).hasSize(4);
        assertThat(datos.getValue().nivelDificultad()).isEqualTo("BAJO");
    }

    @Test
    @DisplayName("PUT /preguntas/{id} → 200")
    void modificarPregunta() throws Exception {
        when(modificarPregunta.ejecutar(any(), any())).thenReturn(pregunta("EN_CONSTRUCCION"));

        mvc.perform(put("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR, "AUTOR"))
                        .contentType("application/json").content(cuerpoPregunta()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntaId").value(PREGUNTA_ID));

        ArgumentCaptor<ModificarPreguntaComando> comando = ArgumentCaptor.forClass(ModificarPreguntaComando.class);
        verify(modificarPregunta).ejecutar(any(), comando.capture());
        assertThat(comando.getValue().preguntaId()).isEqualTo(PREGUNTA_ID);
    }

    @Test
    @DisplayName("GET /preguntas → 200 con la página de 5.1 y los filtros de 8.1")
    void consultarPreguntas() throws Exception {
        when(consultarPreguntas.ejecutar(any(), any())).thenReturn(new Pagina<>(List.of(resumen()), 1, 5, 6, 2));

        mvc.perform(get("/api/v1/preguntas").headers(identidad(ADMINISTRADOR, "ADMINISTRADOR"))
                        .param("estado", "PUBLICADA").param("competenciaId", "22222222-2222-4222-8222-000000000101")
                        .param("pagina", "1").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].preguntaId").value(PREGUNTA_ID))
                .andExpect(jsonPath("$.contenido[0].fechaActualizacion").value("2026-10-01T15:30:00Z"))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamano").value(5))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        ArgumentCaptor<ConsultarPreguntasConsulta> consulta = ArgumentCaptor.forClass(ConsultarPreguntasConsulta.class);
        verify(consultarPreguntas).ejecutar(any(), consulta.capture());
        assertThat(consulta.getValue().estado()).isEqualTo("PUBLICADA");
        assertThat(consulta.getValue().pagina()).isEqualTo(1);
        assertThat(consulta.getValue().tamano()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /preguntas/{id} → 200")
    void obtenerPregunta() throws Exception {
        when(obtenerPregunta.ejecutar(any(), eq(PREGUNTA_ID))).thenReturn(pregunta("PUBLICADA"));

        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID).headers(identidad(AUTOR, "DOCENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADA"));
    }

    @Test
    @DisplayName("POST /preguntas/{id}/envio-revision → 200")
    void enviarARevision() throws Exception {
        when(enviarPreguntaARevision.ejecutar(any(), eq(PREGUNTA_ID))).thenReturn(pregunta("PENDIENTE_REVISION"));

        mvc.perform(post("/api/v1/preguntas/" + PREGUNTA_ID + "/envio-revision").headers(identidad(AUTOR, "AUTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    @Test
    @DisplayName("POST /preguntas/{id}/procesos-revision → 201 con Location del proceso")
    void asignarRevisores() throws Exception {
        when(asignarRevisores.ejecutar(any(), any())).thenReturn(proceso());

        mvc.perform(post("/api/v1/preguntas/" + PREGUNTA_ID + "/procesos-revision")
                        .headers(identidad(ADMINISTRADOR, "ADMINISTRADOR")).contentType("application/json")
                        .content("{\"revisoresIds\":[\"" + REVISOR + "\",\"11111111-1111-4111-8111-000000000004\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/procesos-revision/" + PROCESO_ID))
                .andExpect(jsonPath("$.procesoId").value(PROCESO_ID));

        ArgumentCaptor<AsignarRevisoresComando> comando = ArgumentCaptor.forClass(AsignarRevisoresComando.class);
        verify(asignarRevisores).ejecutar(any(), comando.capture());
        assertThat(comando.getValue().revisoresIds()).hasSize(2);
        assertThat(comando.getValue().preguntaId()).isEqualTo(PREGUNTA_ID);
    }

    @Test
    @DisplayName("POST /preguntas/{id}/publicacion → 200")
    void publicar() throws Exception {
        when(publicarPregunta.ejecutar(any(), eq(PREGUNTA_ID))).thenReturn(pregunta("PUBLICADA"));

        mvc.perform(post("/api/v1/preguntas/" + PREGUNTA_ID + "/publicacion").headers(identidad(ADMINISTRADOR, "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADA"));
    }

    @Test
    @DisplayName("POST /preguntas/{id}/archivado → 200 y el motivo llega al comando")
    void archivar() throws Exception {
        when(archivarPregunta.ejecutar(any(), any())).thenReturn(pregunta("ARCHIVADA"));

        mvc.perform(post("/api/v1/preguntas/" + PREGUNTA_ID + "/archivado").headers(identidad(ADMINISTRADOR, "ADMINISTRADOR"))
                        .contentType("application/json").content("{\"motivo\":\"Contenido desactualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ARCHIVADA"));

        ArgumentCaptor<ArchivarPreguntaComando> comando = ArgumentCaptor.forClass(ArchivarPreguntaComando.class);
        verify(archivarPregunta).ejecutar(any(), comando.capture());
        assertThat(comando.getValue().motivo()).isEqualTo("Contenido desactualizado");
    }

    @Test
    @DisplayName("GET /preguntas/{id}/trazabilidad → 200 con registros e historial (evaluacion null en un DICTAMEN)")
    void trazabilidad() throws Exception {
        when(consultarTrazabilidad.ejecutar(any(), eq(PREGUNTA_ID))).thenReturn(trazabilidadDeEjemplo());

        mvc.perform(get("/api/v1/preguntas/" + PREGUNTA_ID + "/trazabilidad").headers(identidad(ADMINISTRADOR, "ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registros[0].tipo").value("CREACION"))
                .andExpect(jsonPath("$.registros[0].estadoAnterior").isEmpty())
                .andExpect(jsonPath("$.historialRevisiones[0].tipo").value("DICTAMEN"))
                .andExpect(jsonPath("$.historialRevisiones[0].evaluacion").isEmpty())
                .andExpect(jsonPath("$.historialRevisiones[0].dictamen.porcentajeAprobacion").value(100.00));
    }

    @Test
    @DisplayName("GET /procesos-revision/{id} → 200 con dictamen y porcentaje de 2 decimales")
    void obtenerProceso() throws Exception {
        when(obtenerProcesoRevision.ejecutar(any(), eq(PROCESO_ID))).thenReturn(proceso());

        mvc.perform(get("/api/v1/procesos-revision/" + PROCESO_ID).headers(identidad(REVISOR, "REVISOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.asignaciones[0].evaluacionRegistrada").value(true))
                .andExpect(jsonPath("$.evaluaciones[0].criterios[0].criterio").value("PEDAGOGICO"))
                .andExpect(jsonPath("$.dictamen.resultado").value("APROBADA"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"porcentajeAprobacion\":100.00")));
    }

    @Test
    @DisplayName("GET /procesos-revision → 200 paginado")
    void consultarProcesos() throws Exception {
        when(consultarProcesosRevision.ejecutar(any(), any())).thenReturn(new Pagina<>(List.of(proceso()), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/procesos-revision").headers(identidad(REVISOR, "REVISOR")).param("estado", "CERRADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].procesoId").value(PROCESO_ID))
                .andExpect(jsonPath("$.totalElementos").value(1));

        ArgumentCaptor<ConsultarProcesosConsulta> consulta = ArgumentCaptor.forClass(ConsultarProcesosConsulta.class);
        verify(consultarProcesosRevision).ejecutar(any(), consulta.capture());
        assertThat(consulta.getValue().estado()).isEqualTo("CERRADO");
    }

    @Test
    @DisplayName("POST /procesos-revision/{id}/evaluaciones → 201 con Location del proceso")
    void registrarEvaluacion() throws Exception {
        when(registrarEvaluacion.ejecutar(any(), any())).thenReturn(proceso());

        mvc.perform(post("/api/v1/procesos-revision/" + PROCESO_ID + "/evaluaciones").headers(identidad(REVISOR, "REVISOR"))
                        .contentType("application/json").content("""
                                {"criterios":[{"criterio":"PEDAGOGICO","valoracion":4},{"criterio":"TECNICO","valoracion":5},
                                {"criterio":"ESTRUCTURAL","valoracion":4}],"observaciones":["Bien"],"decision":"APROBATORIA"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/procesos-revision/" + PROCESO_ID));

        ArgumentCaptor<RegistrarEvaluacionComando> comando = ArgumentCaptor.forClass(RegistrarEvaluacionComando.class);
        verify(registrarEvaluacion).ejecutar(any(), comando.capture());
        assertThat(comando.getValue().procesoId()).isEqualTo(PROCESO_ID);
        assertThat(comando.getValue().criterios()).hasSize(3);
    }

    @Test
    @DisplayName("GET /docs → 200 con la página de Swagger UI (sin redirección) apuntando a /openapi.json")
    void docs() throws Exception {
        mvc.perform(get("/docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("url: \"/openapi.json\"")));
    }

    @Test
    @DisplayName("GET /salud → 200 sin encabezados de identidad")
    void salud() throws Exception {
        mvc.perform(get("/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.servicio").value("servicio-editorial"));
    }

    @Test
    @DisplayName("Sin X-Id-Correlacion el servicio genera uno y lo devuelve")
    void generaCorrelacion() throws Exception {
        mvc.perform(get("/salud"))
                .andExpect(header().string("X-Id-Correlacion",
                        org.hamcrest.Matchers.matchesPattern("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")));
    }
}
