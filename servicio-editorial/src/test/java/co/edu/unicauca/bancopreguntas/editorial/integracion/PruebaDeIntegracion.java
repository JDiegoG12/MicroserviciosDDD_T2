package co.edu.unicauca.bancopreguntas.editorial.integracion;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.AsignarRevisores;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.CrearPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.EnviarPreguntaARevision;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.PublicarPregunta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.entrada.RegistrarEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.CatalogoAcademicoPuerto;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida.ResultadoValidacionClasificacion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia.EstadoDelEsquema;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Base de las pruebas de integración de los adaptadores con contenedores reales (Testcontainers): PostgreSQL 16 y
 * RabbitMQ 3.13, las mismas imágenes de CONTRATOS.md 9.1. Los contenedores se inician una sola vez y los comparten
 * todas las clases {@code *IT}.
 *
 * <p>Usa el perfil {@code pruebas}: Flyway migra al iniciar el contexto y Hibernate valida las entidades contra ese
 * esquema ({@code ddl-auto=validate}). Es la validación del esquema que CONTRATOS.md 9.3.6 (v1.10) pide en las
 * pruebas y no al arrancar.</p>
 *
 * <p>Catálogo se reemplaza por un doble (su adaptador gRPC tiene su propia prueba con un servidor en proceso). Para
 * que las pruebas no se interfieran entre sí sobre la base de datos compartida, cada una usa autores, revisores y
 * competencias con UUID nuevos.</p>
 */
@SpringBootTest
@ActiveProfiles("pruebas")
abstract class PruebaDeIntegracion {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management");

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }

    @MockitoBean
    CatalogoAcademicoPuerto catalogo;

    @Autowired
    CrearPregunta crearPregunta;
    @Autowired
    EnviarPreguntaARevision enviarPreguntaARevision;
    @Autowired
    AsignarRevisores asignarRevisores;
    @Autowired
    RegistrarEvaluacion registrarEvaluacion;
    @Autowired
    PublicarPregunta publicarPregunta;

    /** Administrador de prueba (CONTRATOS.md 4.2). */
    static final UsuarioActual ADMINISTRADOR = UsuarioActual.de(UsuarioId.de("11111111-1111-4111-8111-000000000001"),
            Rol.ADMINISTRADOR);

    @Autowired
    EstadoDelEsquema estadoDelEsquema;

    @BeforeEach
    void catalogoAceptaTodo() {
        when(catalogo.validarClasificacion(any())).thenReturn(ResultadoValidacionClasificacion.valido());
    }

    /**
     * El migrador corre en segundo plano (CONTRATOS.md 9.3.6): se espera a que marque el esquema como listo, porque
     * hasta entonces la API responde 503.
     *
     * @throws InterruptedException si se interrumpe la espera
     */
    @BeforeEach
    void esperarEsquemaListo() throws InterruptedException {
        long limite = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (!estadoDelEsquema.esquemaListo() && System.nanoTime() < limite) {
            Thread.sleep(100);
        }
        assertThat(estadoDelEsquema.esquemaListo()).as("el migrador debe dejar el esquema listo").isTrue();
    }

    /**
     * Usuario nuevo con los roles indicados.
     *
     * @param roles roles
     * @return usuario con UUID aleatorio
     */
    static UsuarioActual usuarioNuevo(Rol... roles) {
        return UsuarioActual.de(new UsuarioId(UUID.randomUUID()), roles);
    }

    /**
     * Datos de una pregunta completa clasificada en la competencia indicada.
     *
     * @param competenciaId competencia (UUID nuevo por prueba para aislar las consultas)
     * @return datos válidos
     */
    static DatosDePreguntaComando datosCompletos(UUID competenciaId) {
        return new DatosDePreguntaComando(
                "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.",
                "¿Cuál es la moda del conjunto de notas?",
                List.of(new DatosDePreguntaComando.OpcionComando("A", "3,0", false),
                        new DatosDePreguntaComando.OpcionComando("B", "3,8", false),
                        new DatosDePreguntaComando.OpcionComando("C", "4,0", true),
                        new DatosDePreguntaComando.OpcionComando("D", "4,5", false)),
                "La moda es el valor que más se repite: 4,0 aparece dos veces.",
                List.of("Walpole, R. Probabilidad y estadística.", "Montgomery, D. Estadística aplicada."),
                competenciaId.toString(),
                "22222222-2222-4222-8222-000000000201",
                "22222222-2222-4222-8222-000000000301",
                "BAJO");
    }

    /**
     * Evaluación con los tres criterios.
     *
     * @param procesoId UUID del proceso
     * @param decision  decisión
     * @return comando
     */
    static RegistrarEvaluacionComando evaluacion(String procesoId, String decision) {
        return new RegistrarEvaluacionComando(procesoId,
                List.of(new RegistrarEvaluacionComando.CriterioComando("PEDAGOGICO", 4),
                        new RegistrarEvaluacionComando.CriterioComando("TECNICO", 5),
                        new RegistrarEvaluacionComando.CriterioComando("ESTRUCTURAL", 3)),
                List.of("Observación de " + decision), decision);
    }

    /**
     * Crea una pregunta y la lleva hasta {@code PENDIENTE_REVISION}.
     *
     * @param autor         autor
     * @param competenciaId competencia
     * @return UUID de la pregunta
     */
    String crearPendiente(UsuarioActual autor, UUID competenciaId) {
        String preguntaId = crearPregunta.ejecutar(autor, datosCompletos(competenciaId)).preguntaId();
        enviarPreguntaARevision.ejecutar(autor, preguntaId);
        return preguntaId;
    }

    /**
     * Asigna dos revisores a una pregunta pendiente.
     *
     * @param preguntaId pregunta
     * @param revisor1   primer revisor
     * @param revisor2   segundo revisor
     * @return UUID del proceso
     */
    String asignar(String preguntaId, UsuarioActual revisor1, UsuarioActual revisor2) {
        return asignarRevisores.ejecutar(ADMINISTRADOR, new AsignarRevisoresComando(preguntaId,
                List.of(revisor1.id().toString(), revisor2.id().toString()))).procesoId();
    }

    /**
     * Crea una pregunta, la revisa con dos aprobaciones y la deja {@code APROBADA}.
     *
     * @param autor         autor
     * @param competenciaId competencia
     * @return UUID de la pregunta
     */
    String crearAprobada(UsuarioActual autor, UUID competenciaId) {
        String preguntaId = crearPendiente(autor, competenciaId);
        UsuarioActual revisor1 = usuarioNuevo(Rol.REVISOR);
        UsuarioActual revisor2 = usuarioNuevo(Rol.REVISOR);
        String procesoId = asignar(preguntaId, revisor1, revisor2);
        registrarEvaluacion.ejecutar(revisor1, evaluacion(procesoId, "APROBATORIA"));
        registrarEvaluacion.ejecutar(revisor2, evaluacion(procesoId, "APROBATORIA"));
        return preguntaId;
    }
}
