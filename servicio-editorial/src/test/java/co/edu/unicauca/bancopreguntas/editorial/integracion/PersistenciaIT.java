package co.edu.unicauca.bancopreguntas.editorial.integracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarPreguntasConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConversorDeComandos;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarPreguntas;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarProcesosRevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ConsultarTrazabilidad;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.ModificarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaResumen;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.TrazabilidadRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EvaluacionEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.EstadoProceso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.ProcesoDeRevisionRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.PreguntaJpaSpring;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.entidades.PreguntaEntidad;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.transaccion.EjecutorTransaccional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso.ModificarPreguntaCasoUso;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.PublicadorEventosPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.RelojPuerto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Paginacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.AlcanceDeVisibilidad;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.CriteriosBusquedaPregunta;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.ManejadorGlobalErrores;
import co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores.ProblemaJson;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Repositorios JPA contra PostgreSQL real: esquema de Flyway validado por Hibernate ({@code ddl-auto=validate}),
 * ida y vuelta de los dos agregados completos, filtros y paginación de 8.1 en SQL (con la unión de roles),
 * procesos por estado y revisor, y bloqueo optimista.
 */
@DisplayName("Persistencia JPA + Flyway contra PostgreSQL 16 (Testcontainers)")
class PersistenciaIT extends PruebaDeIntegracion {

    @Autowired
    PreguntaRepositorio preguntaRepositorio;
    @Autowired
    ProcesoDeRevisionRepositorio procesoRepositorio;
    @Autowired
    EjecutorTransaccional transaccion;
    @Autowired
    ConsultarPreguntas consultarPreguntas;
    @Autowired
    ConsultarProcesosRevision consultarProcesos;
    @Autowired
    ConsultarTrazabilidad consultarTrazabilidad;
    @Autowired
    ModificarPregunta modificarPregunta;
    @Autowired
    PreguntaJpaSpring preguntasJpa;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PublicadorEventosPuerto publicadorEventos;
    @Autowired
    RelojPuerto reloj;

    @Test
    @DisplayName("Flyway aplicó V1 y Hibernate validó el esquema (el contexto arrancó con ddl-auto=validate)")
    void esquemaMigradoYValidado() {
        Integer migraciones = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
        assertThat(migraciones).isEqualTo(1);
    }

    @Test
    @DisplayName("Ida y vuelta del agregado Pregunta: contenido, trazabilidad e historial con criterios y observaciones")
    void idaYVueltaDePregunta() {
        UsuarioActual autor = usuarioNuevo(Rol.AUTOR);
        UsuarioActual revisor1 = usuarioNuevo(Rol.REVISOR);
        UsuarioActual revisor2 = usuarioNuevo(Rol.REVISOR);
        String preguntaId = crearPendiente(autor, UUID.randomUUID());
        String procesoId = asignar(preguntaId, revisor1, revisor2);
        registrarEvaluacion.ejecutar(revisor1, evaluacion(procesoId, "APROBATORIA"));
        registrarEvaluacion.ejecutar(revisor2, evaluacion(procesoId, "REPROBATORIA"));

        Pregunta leida = transaccion.leer(() -> preguntaRepositorio.obtenerPorId(PreguntaId.de(preguntaId)).orElseThrow());

        assertThat(leida.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
        assertThat(leida.getAutorId()).isEqualTo(autor.id());
        assertThat(leida.getContenido()).isEqualTo(
                ConversorDeComandos.aContenido(
                        datosCompletos(leida.getContenido().clasificacion().competenciaId())));
        assertThat(leida.erroresValidacion()).isEmpty();
        assertThat(leida.getTrazabilidad()).extracting(registro -> registro.estadoNuevo()).containsExactly(
                EstadoPregunta.BORRADOR, EstadoPregunta.EN_CONSTRUCCION, EstadoPregunta.PENDIENTE_REVISION,
                EstadoPregunta.EN_REVISION, EstadoPregunta.RECHAZADA, EstadoPregunta.EN_CONSTRUCCION);
        assertThat(leida.getTrazabilidad().get(4).detalle())
                .isEqualTo("Dictamen automático (CU-12) disparado por la evaluación del revisor " + revisor2.id());
        assertThat(leida.getHistorialRevisiones().entradas()).hasSize(3);
        EvaluacionEnHistorial primera = (EvaluacionEnHistorial) leida.getHistorialRevisiones().entradas().get(0);
        assertThat(primera.revisorId()).isEqualTo(revisor1.id());
        assertThat(primera.criterios()).hasSize(3);
        assertThat(primera.observaciones()).extracting(observacion -> observacion.texto()).containsExactly("Observación de APROBATORIA");
    }

    @Test
    @DisplayName("Ida y vuelta del agregado ProcesoDeRevision: asignaciones, formatos y dictamen con 2 decimales")
    void idaYVueltaDeProceso() {
        UsuarioActual revisor1 = usuarioNuevo(Rol.REVISOR);
        UsuarioActual revisor2 = usuarioNuevo(Rol.REVISOR);
        String procesoId = asignar(crearPendiente(usuarioNuevo(Rol.AUTOR), UUID.randomUUID()), revisor1, revisor2);
        registrarEvaluacion.ejecutar(revisor1, evaluacion(procesoId, "APROBATORIA"));
        registrarEvaluacion.ejecutar(revisor2, evaluacion(procesoId, "REPROBATORIA"));

        ProcesoDeRevision leido = transaccion.leer(() ->
                procesoRepositorio.obtenerPorId(ProcesoDeRevisionId.de(procesoId)).orElseThrow());

        assertThat(leido.getEstado()).isEqualTo(EstadoProceso.CERRADO);
        assertThat(leido.getAsignaciones()).extracting(asignacion -> asignacion.revisorId())
                .containsExactly(revisor1.id(), revisor2.id());
        assertThat(leido.getFormatos()).hasSize(2);
        assertThat(leido.getFormatos().get(0).getCriterios()).hasSize(3);
        assertThat(leido.getDictamen().orElseThrow().porcentajeAprobacion()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Trazabilidad e historial solo se anexan: una modificación no reescribe las filas anteriores")
    void soloAnexado() {
        UsuarioActual autor = usuarioNuevo(Rol.AUTOR);
        String preguntaId = crearPregunta.ejecutar(autor, datosCompletos(UUID.randomUUID())).preguntaId();
        TrazabilidadRespuesta antes = consultarTrazabilidad.ejecutar(ADMINISTRADOR, preguntaId);

        modificarPregunta.ejecutar(autor, new ModificarPreguntaComando(preguntaId, datosCompletos(UUID.randomUUID())));
        TrazabilidadRespuesta despues = consultarTrazabilidad.ejecutar(ADMINISTRADOR, preguntaId);

        assertThat(despues.registros()).startsWith(antes.registros().toArray(new TrazabilidadRespuesta.RegistroRespuesta[0]));
        assertThat(despues.registros()).hasSize(antes.registros().size() + 1);
    }

    @Test
    @DisplayName("GET /preguntas en SQL: unión de roles AUTOR + DOCENTE, filtros y paginación")
    void consultaPorCriteriosEnBaseDeDatos() {
        UUID competencia = UUID.randomUUID();
        UsuarioActual autor = usuarioNuevo(Rol.AUTOR, Rol.DOCENTE);
        UsuarioActual otroAutor = usuarioNuevo(Rol.AUTOR);
        String propia = crearPregunta.ejecutar(autor, datosCompletos(competencia)).preguntaId();
        String ajenaEnConstruccion = crearPregunta.ejecutar(otroAutor, datosCompletos(competencia)).preguntaId();
        String ajenaPublicada = crearAprobada(otroAutor, competencia);
        publicarPregunta.ejecutar(ADMINISTRADOR, ajenaPublicada);
        ConsultarPreguntasConsulta deLaCompetencia = new ConsultarPreguntasConsulta(competencia.toString(), null, null, null,
                null, null, null, null);

        Pagina<PreguntaResumen> union = consultarPreguntas.ejecutar(autor, deLaCompetencia);
        Pagina<PreguntaResumen> todas = consultarPreguntas.ejecutar(ADMINISTRADOR, deLaCompetencia);
        Pagina<PreguntaResumen> primeraPagina = consultarPreguntas.ejecutar(ADMINISTRADOR,
                new ConsultarPreguntasConsulta(competencia.toString(), null, null, null, null, null, 0, 2));
        Pagina<PreguntaResumen> segundaPagina = consultarPreguntas.ejecutar(ADMINISTRADOR,
                new ConsultarPreguntasConsulta(competencia.toString(), null, null, null, null, null, 1, 2));
        Pagina<PreguntaResumen> publicadas = consultarPreguntas.ejecutar(ADMINISTRADOR,
                new ConsultarPreguntasConsulta(competencia.toString(), null, null, "BAJO", "PUBLICADA", null, null, null));

        assertThat(union.contenido()).extracting(PreguntaResumen::preguntaId).containsExactly(propia, ajenaPublicada);
        assertThat(todas.contenido()).extracting(PreguntaResumen::preguntaId)
                .containsExactly(propia, ajenaEnConstruccion, ajenaPublicada);
        assertThat(primeraPagina.contenido()).extracting(PreguntaResumen::preguntaId).containsExactly(propia, ajenaEnConstruccion);
        assertThat(primeraPagina.totalElementos()).isEqualTo(3);
        assertThat(primeraPagina.totalPaginas()).isEqualTo(2);
        assertThat(segundaPagina.contenido()).extracting(PreguntaResumen::preguntaId).containsExactly(ajenaPublicada);
        assertThat(publicadas.contenido()).extracting(PreguntaResumen::preguntaId).containsExactly(ajenaPublicada);
    }

    @Test
    @DisplayName("GET /preguntas en SQL: un REVISOR ve las preguntas de sus procesos ABIERTOS (EXISTS sobre asignaciones)")
    void revisorVeSusAsignadas() {
        UUID competencia = UUID.randomUUID();
        UsuarioActual revisor = usuarioNuevo(Rol.REVISOR);
        UsuarioActual otroRevisor = usuarioNuevo(Rol.REVISOR);
        String asignada = crearPendiente(usuarioNuevo(Rol.AUTOR), competencia);
        asignar(asignada, revisor, otroRevisor);
        crearPendiente(usuarioNuevo(Rol.AUTOR), competencia);

        Pagina<PreguntaResumen> visibles = consultarPreguntas.ejecutar(revisor, new ConsultarPreguntasConsulta(
                competencia.toString(), null, null, null, null, null, null, null));

        assertThat(visibles.contenido()).extracting(PreguntaResumen::preguntaId).containsExactly(asignada);
    }

    @Test
    @DisplayName("GET /procesos-revision en SQL: por estado y revisor opcional, paginado")
    void procesosPorEstadoYRevisor() {
        UsuarioActual revisor = usuarioNuevo(Rol.REVISOR);
        String abierto = asignar(crearPendiente(usuarioNuevo(Rol.AUTOR), UUID.randomUUID()), revisor, usuarioNuevo(Rol.REVISOR));
        UsuarioActual otro = usuarioNuevo(Rol.REVISOR);
        String cerrado = asignar(crearPendiente(usuarioNuevo(Rol.AUTOR), UUID.randomUUID()), revisor, otro);
        registrarEvaluacion.ejecutar(revisor, evaluacion(cerrado, "APROBATORIA"));
        registrarEvaluacion.ejecutar(otro, evaluacion(cerrado, "APROBATORIA"));

        Pagina<ProcesoRevisionRespuesta> abiertos = consultarProcesos.ejecutar(revisor, new ConsultarProcesosConsulta(null, null, null, null));
        Pagina<ProcesoRevisionRespuesta> cerrados = consultarProcesos.ejecutar(revisor, new ConsultarProcesosConsulta(null, "CERRADO", null, null));
        Pagina<ProcesoRevisionRespuesta> todosLosAbiertos = consultarProcesos.ejecutar(ADMINISTRADOR,
                new ConsultarProcesosConsulta(null, "ABIERTO", 0, 100));

        assertThat(abiertos.contenido()).extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(abierto);
        assertThat(cerrados.contenido()).extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(cerrado);
        assertThat(todosLosAbiertos.contenido()).extracting(ProcesoRevisionRespuesta::procesoId).contains(abierto)
                .doesNotContain(cerrado);
    }

    @Test
    @DisplayName("Dos modificaciones concurrentes de la misma pregunta: una se confirma y la otra → 409 CONFLICTO_DE_CONCURRENCIA")
    void modificacionesConcurrentes() throws Exception {
        UsuarioActual autor = usuarioNuevo(Rol.AUTOR);
        String preguntaId = crearPregunta.ejecutar(autor, datosCompletos(UUID.randomUUID())).preguntaId();
        CountDownLatch primeraCargada = new CountDownLatch(1);
        CountDownLatch segundaConfirmada = new CountDownLatch(1);
        // La primera modificación carga la pregunta y espera a que la segunda se confirme antes de continuar.
        PreguntaRepositorio queEspera = new PreguntaRepositorioQueEsperaAlCargar(preguntaRepositorio, primeraCargada, segundaConfirmada);
        ModificarPreguntaCasoUso primera = new ModificarPreguntaCasoUso(queEspera, catalogo, publicadorEventos, reloj);
        ModificarPreguntaComando comando = new ModificarPreguntaComando(preguntaId, datosCompletos(UUID.randomUUID()));

        try (ExecutorService hilo = Executors.newSingleThreadExecutor()) {
            Future<?> primeraEnCurso = hilo.submit(() -> transaccion.escribir(() -> primera.ejecutar(autor, comando)));
            assertThat(primeraCargada.await(10, TimeUnit.SECONDS)).isTrue();

            modificarPregunta.ejecutar(autor, comando);
            segundaConfirmada.countDown();

            Throwable error = catchThrowable(primeraEnCurso::get);
            assertThat(error).isInstanceOf(ExecutionException.class).hasCauseInstanceOf(OptimisticLockingFailureException.class);
            ResponseEntity<ProblemaJson> respuesta = new ManejadorGlobalErrores().conflictoDeConcurrencia(
                    (OptimisticLockingFailureException) error.getCause(), new MockHttpServletRequest("PUT", "/api/v1/preguntas/" + preguntaId));
            assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
            assertThat(respuesta.getBody().codigo()).isEqualTo("CONFLICTO_DE_CONCURRENCIA");
        }
        TrazabilidadRespuesta trazabilidad = consultarTrazabilidad.ejecutar(ADMINISTRADOR, preguntaId);
        assertThat(trazabilidad.registros()).filteredOn(registro -> registro.tipo().equals("MODIFICACION")).hasSize(1);
    }

    /** Decorador que, al cargar la pregunta, avisa y espera a que otra transacción la modifique primero. */
    private static final class PreguntaRepositorioQueEsperaAlCargar implements PreguntaRepositorio {
        private final PreguntaRepositorio real;
        private final CountDownLatch cargada;
        private final CountDownLatch continuar;

        PreguntaRepositorioQueEsperaAlCargar(PreguntaRepositorio real, CountDownLatch cargada, CountDownLatch continuar) {
            this.real = real;
            this.cargada = cargada;
            this.continuar = continuar;
        }

        @Override
        public void guardar(Pregunta pregunta) {
            real.guardar(pregunta);
        }

        @Override
        public Optional<Pregunta> obtenerPorId(PreguntaId preguntaId) {
            Optional<Pregunta> pregunta = real.obtenerPorId(preguntaId);
            cargada.countDown();
            try {
                continuar.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException interrupcion) {
                Thread.currentThread().interrupt();
            }
            return pregunta;
        }

        @Override
        public Pagina<Pregunta> buscarPorCriterios(CriteriosBusquedaPregunta criterios, AlcanceDeVisibilidad alcance,
                                                   Paginacion paginacion) {
            return real.buscarPorCriterios(criterios, alcance, paginacion);
        }

        @Override
        public List<Pregunta> buscarPorIds(Collection<PreguntaId> preguntaIds) {
            return real.buscarPorIds(preguntaIds);
        }
    }

    @Test
    @DisplayName("Bloqueo optimista: guardar una versión vieja de la pregunta falla")
    void bloqueoOptimista() {
        UsuarioActual autor = usuarioNuevo(Rol.AUTOR);
        String preguntaId = crearPregunta.ejecutar(autor, datosCompletos(UUID.randomUUID())).preguntaId();
        PreguntaEntidad vieja = transaccion.leer(() -> preguntasJpa.findById(UUID.fromString(preguntaId)).orElseThrow());

        enviarPreguntaARevision.ejecutar(autor, preguntaId);
        vieja.setContexto("Cambio concurrente sobre una versión vieja");

        assertThatThrownBy(() -> transaccion.escribir(() -> preguntasJpa.saveAndFlush(vieja)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        Long version = jdbc.queryForObject("select version from pregunta where id = ?", Long.class, UUID.fromString(preguntaId));
        assertThat(version).isGreaterThan(vieja.getVersion());
    }
}
