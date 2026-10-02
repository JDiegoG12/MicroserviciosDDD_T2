package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.RegistrarEvaluacionComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.DictamenEmitido;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EvaluacionRegistrada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaAprobada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaRechazada;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link RegistrarEvaluacionCasoUso} (CU-11 con CU-12 automático).
 */
@DisplayName("RegistrarEvaluacionCasoUso (CU-11 + CU-12)")
class RegistrarEvaluacionCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Con una evaluación pendiente no hay dictamen y la pregunta sigue EN_REVISION")
    void sinDictamenConPendientes() {
        String preguntaId = escenario.crearPreguntaPendiente();
        String procesoId = escenario.asignarDosRevisores(preguntaId);

        ProcesoRevisionRespuesta proceso = escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_1,
                evaluacion(procesoId, "APROBATORIA"));

        assertThat(proceso.dictamen()).isNull();
        assertThat(proceso.estado()).isEqualTo("ABIERTO");
        assertThat(proceso.evaluaciones()).singleElement().satisfies(evaluacion -> {
            assertThat(evaluacion.decision()).isEqualTo("APROBATORIA");
            assertThat(evaluacion.criterios()).hasSize(3);
            assertThat(evaluacion.observaciones()).containsExactly("El distractor B es poco plausible");
        });
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId).estado()).isEqualTo("EN_REVISION");
        assertThat(escenario.publicador.publicadosDeTipo(EvaluacionRegistrada.class)).hasSize(1);
    }

    @Test
    @DisplayName("2 de 2 aprobatorias: dictamen APROBADA y la pregunta queda APROBADA")
    void dictamenAprobatorio() {
        String preguntaId = escenario.crearPreguntaPendiente();
        String procesoId = escenario.asignarDosRevisores(preguntaId);

        escenario.evaluarPorAmbos(procesoId, "APROBATORIA", "APROBATORIA");

        ProcesoRevisionRespuesta proceso = escenario.obtenerProceso.ejecutar(COMO_ADMINISTRADOR, procesoId);
        assertThat(proceso.estado()).isEqualTo("CERRADO");
        assertThat(proceso.dictamen().resultado()).isEqualTo("APROBADA");
        assertThat(proceso.dictamen().porcentajeAprobacion()).isEqualTo(new BigDecimal("100.00"));
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId).estado()).isEqualTo("APROBADA");
        assertThat(escenario.publicador.publicadosDeTipo(DictamenEmitido.class)).hasSize(1);
        assertThat(escenario.publicador.publicadosDeTipo(PreguntaAprobada.class)).hasSize(1);
    }

    @Test
    @DisplayName("Rechazo: la pregunta vuelve a EN_CONSTRUCCION, conserva el historial y es editable otra vez (D-07)")
    void rechazoVuelveAConstruccion() {
        String preguntaId = escenario.crearPreguntaPendiente();
        String procesoId = escenario.asignarDosRevisores(preguntaId);

        escenario.evaluarPorAmbos(procesoId, "APROBATORIA", "REPROBATORIA");

        assertThat(escenario.obtenerProceso.ejecutar(COMO_ADMINISTRADOR, procesoId).dictamen().resultado())
                .isEqualTo("RECHAZADA");
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_AUTOR, preguntaId).estado()).isEqualTo("EN_CONSTRUCCION");
        assertThat(escenario.publicador.publicadosDeTipo(PreguntaRechazada.class)).hasSize(1);
        assertThat(escenario.consultarTrazabilidad.ejecutar(COMO_ADMINISTRADOR, preguntaId).historialRevisiones())
                .hasSize(3);

        escenario.modificarPregunta.ejecutar(COMO_AUTOR, new ModificarPreguntaComando(preguntaId, datosCon("Contexto corregido.")));
        assertThat(escenario.consultarTrazabilidad.ejecutar(COMO_ADMINISTRADOR, preguntaId).historialRevisiones())
                .hasSize(3);
    }

    @Test
    @DisplayName("Revisor no asignado, evaluación repetida y proceso cerrado se rechazan con su código")
    void reglasDelProceso() {
        String preguntaId = escenario.crearPreguntaPendiente();
        String procesoId = escenario.asignarDosRevisores(preguntaId);

        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_3, evaluacion(procesoId, "APROBATORIA")),
                "REVISOR_NO_ASIGNADO");
        escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_1, evaluacion(procesoId, "APROBATORIA"));
        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_1, evaluacion(procesoId, "APROBATORIA")),
                "EVALUACION_YA_REGISTRADA");
        escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_2, evaluacion(procesoId, "APROBATORIA"));
        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_2, evaluacion(procesoId, "APROBATORIA")),
                "PROCESO_CERRADO");
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO aunque el usuario esté asignado")
    void rolIncorrecto() {
        String procesoId = escenario.asignarDosRevisores(escenario.crearPreguntaPendiente());
        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_ADMINISTRADOR, evaluacion(procesoId, "APROBATORIA")),
                "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("Decisión o criterios mal formados: SOLICITUD_INVALIDA; proceso inexistente: 404")
    void datosInvalidos() {
        String procesoId = escenario.asignarDosRevisores(escenario.crearPreguntaPendiente());
        RegistrarEvaluacionComando sinCriterios = new RegistrarEvaluacionComando(procesoId, List.of(), List.of(), "APROBATORIA");

        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_1, evaluacion(procesoId, "QUIZAS")),
                "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_1, sinCriterios), "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> escenario.registrarEvaluacion.ejecutar(COMO_REVISOR_1,
                evaluacion(UUID.randomUUID().toString(), "APROBATORIA")), "PROCESO_REVISION_NO_ENCONTRADO");
    }
}
