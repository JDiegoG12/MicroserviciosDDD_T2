package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.DictamenEmitido;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EvaluacionRegistrada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.ProcesoDeRevisionAbierto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.RevisoresAsignados;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.PreguntaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision.APROBATORIA;
import static co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision.REPROBATORIA;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas del agregado Proceso de revisión: INV-15 a INV-21 y el cálculo del Dictamen (D-04, D-05).
 */
@DisplayName("Agregado ProcesoDeRevision")
class ProcesoDeRevisionTest {

    private static ProcesoDeRevision abrirCon(UsuarioId... revisores) {
        return ProcesoDeRevision.abrir(ProcesoDeRevisionId.generar(), PreguntaId.generar(), AUTOR, List.of(revisores), FECHA);
    }

    private static void evaluar(ProcesoDeRevision proceso, UsuarioId revisor, DecisionRevision decision) {
        proceso.registrarEvaluacion(revisor, criteriosValidos(), observaciones(), decision, fecha(10));
    }

    @Nested
    @DisplayName("Apertura (CU-10)")
    class Apertura {

        @Test
        @DisplayName("Abre el proceso ABIERTO con sus asignaciones y emite sus dos eventos")
        void abreElProceso() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);

            assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ABIERTO);
            assertThat(proceso.getAsignaciones()).extracting(AsignacionDeRevisor::revisorId).containsExactly(REVISOR_1, REVISOR_2);
            assertThat(proceso.getDictamen()).isEmpty();
            assertThat(proceso.extraerEventos()).hasExactlyElementsOfTypes(ProcesoDeRevisionAbierto.class, RevisoresAsignados.class);
        }

        @Test
        @DisplayName("INV-15: con menos de dos revisores lanza REVISORES_INSUFICIENTES")
        void inv15MinimoDosRevisores() {
            lanzaConCodigo(() -> abrirCon(REVISOR_1), "REVISORES_INSUFICIENTES");
            lanzaConCodigo(() -> abrirCon(), "REVISORES_INSUFICIENTES");
        }

        @Test
        @DisplayName("INV-16: si el autor está entre los revisores lanza AUTOR_NO_PUEDE_SER_REVISOR")
        void inv16AutorNoEsRevisor() {
            lanzaConCodigo(() -> abrirCon(REVISOR_1, AUTOR), "AUTOR_NO_PUEDE_SER_REVISOR");
        }

        @Test
        @DisplayName("INV-17: un revisor repetido lanza REVISOR_DUPLICADO")
        void inv17RevisorSinRepetir() {
            lanzaConCodigo(() -> abrirCon(REVISOR_1, REVISOR_2, REVISOR_1), "REVISOR_DUPLICADO");
        }
    }

    @Nested
    @DisplayName("Evaluaciones (CU-11)")
    class Evaluaciones {

        @Test
        @DisplayName("Registra la evaluación de un revisor asignado y emite EvaluacionRegistrada")
        void registraEvaluacion() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);
            proceso.extraerEventos();

            evaluar(proceso, REVISOR_1, APROBATORIA);

            assertThat(proceso.getFormatos()).hasSize(1);
            assertThat(proceso.yaEvaluo(REVISOR_1)).isTrue();
            assertThat(proceso.extraerEventos()).hasExactlyElementsOfTypes(EvaluacionRegistrada.class);
        }

        @Test
        @DisplayName("Un usuario no asignado lanza REVISOR_NO_ASIGNADO")
        void revisorNoAsignado() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);
            lanzaConCodigo(() -> evaluar(proceso, REVISOR_3, APROBATORIA), "REVISOR_NO_ASIGNADO");
        }

        @Test
        @DisplayName("INV-18: una segunda evaluación del mismo revisor lanza EVALUACION_YA_REGISTRADA")
        void inv18UnFormatoPorRevisor() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2, REVISOR_3);
            evaluar(proceso, REVISOR_1, APROBATORIA);

            lanzaConCodigo(() -> evaluar(proceso, REVISOR_1, REPROBATORIA), "EVALUACION_YA_REGISTRADA");
            assertThat(proceso.getFormatos()).singleElement()
                    .extracting(FormatoDeEvaluacion::getDecision).isEqualTo(APROBATORIA);
        }

        @Test
        @DisplayName("Los tres criterios son obligatorios, sin repetir y con valoración de 1 a 5")
        void criteriosObligatorios() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);
            List<CriterioEvaluado> sinEstructural = criteriosValidos().subList(0, 2);
            List<CriterioEvaluado> repetidos = List.of(new CriterioEvaluado(TipoCriterio.TECNICO, 3),
                    new CriterioEvaluado(TipoCriterio.TECNICO, 3), new CriterioEvaluado(TipoCriterio.PEDAGOGICO, 3));

            lanzaConCodigo(() -> proceso.registrarEvaluacion(REVISOR_1, sinEstructural, observaciones(), APROBATORIA, FECHA),
                    "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> proceso.registrarEvaluacion(REVISOR_1, repetidos, observaciones(), APROBATORIA, FECHA),
                    "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> new CriterioEvaluado(TipoCriterio.TECNICO, 0), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> new CriterioEvaluado(TipoCriterio.TECNICO, 6), "SOLICITUD_INVALIDA");
            assertThat(proceso.getFormatos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Dictamen (CU-12, INV-19 a INV-21, D-04, D-05)")
    class DictamenDelProceso {

        @Test
        @DisplayName("INV-19: no se calcula mientras haya evaluaciones pendientes")
        void inv19NoSeCalculaConPendientes() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2, REVISOR_3);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, APROBATORIA);

            assertThat(proceso.getDictamen()).isEmpty();
            assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ABIERTO);
            assertThat(proceso.cantidadDeEvaluacionesPendientes()).isEqualTo(1);
            assertThat(proceso.extraerEventos()).noneMatch(DictamenEmitido.class::isInstance);
        }

        @Test
        @DisplayName("2 de 2 aprobatorias → APROBADA con 100,00 %")
        void dosDeDos() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, APROBATORIA);

            afirmarDictamen(proceso, ResultadoDictamen.APROBADA, "100.00");
            assertThat(proceso.extraerEventos()).last().isInstanceOf(DictamenEmitido.class);
        }

        @Test
        @DisplayName("1 de 2 aprobatorias → RECHAZADA con 50,00 %")
        void unoDeDos() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, REPROBATORIA);

            afirmarDictamen(proceso, ResultadoDictamen.RECHAZADA, "50.00");
        }

        @Test
        @DisplayName("2 de 3 aprobatorias → RECHAZADA con 66,67 % (D-05: equivale a unanimidad)")
        void dosDeTres() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2, REVISOR_3);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, APROBATORIA);
            evaluar(proceso, REVISOR_3, REPROBATORIA);

            afirmarDictamen(proceso, ResultadoDictamen.RECHAZADA, "66.67");
        }

        @Test
        @DisplayName("3 de 3 aprobatorias → APROBADA")
        void tresDeTres() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2, REVISOR_3);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, APROBATORIA);
            evaluar(proceso, REVISOR_3, APROBATORIA);

            afirmarDictamen(proceso, ResultadoDictamen.APROBADA, "100.00");
        }

        @Test
        @DisplayName("3 de 4 aprobatorias → APROBADA con 75,00 %")
        void tresDeCuatro() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2, REVISOR_3, REVISOR_4);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, APROBATORIA);
            evaluar(proceso, REVISOR_3, REPROBATORIA);
            evaluar(proceso, REVISOR_4, APROBATORIA);

            afirmarDictamen(proceso, ResultadoDictamen.APROBADA, "75.00");
        }

        @Test
        @DisplayName("INV-20: exactamente 70 % no alcanza; se exige superarlo estrictamente")
        void inv20UmbralEstricto() {
            assertThat(Dictamen.calcular(7, 10, FECHA).resultado()).isEqualTo(ResultadoDictamen.RECHAZADA);
            assertThat(Dictamen.calcular(7, 10, FECHA).porcentajeAprobacion()).isEqualByComparingTo("70.00");
            assertThat(Dictamen.calcular(71, 100, FECHA).resultado()).isEqualTo(ResultadoDictamen.APROBADA);
        }

        @Test
        @DisplayName("INV-21: con dictamen emitido el proceso queda CERRADO y rechaza cambios con PROCESO_CERRADO")
        void inv21ProcesoCerrado() {
            ProcesoDeRevision proceso = abrirCon(REVISOR_1, REVISOR_2);
            evaluar(proceso, REVISOR_1, APROBATORIA);
            evaluar(proceso, REVISOR_2, REPROBATORIA);
            Dictamen emitido = proceso.getDictamen().orElseThrow();

            lanzaConCodigo(() -> evaluar(proceso, REVISOR_1, APROBATORIA), "PROCESO_CERRADO");

            assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.CERRADO);
            assertThat(proceso.getDictamen()).contains(emitido);
        }

        private void afirmarDictamen(ProcesoDeRevision proceso, ResultadoDictamen resultado, String porcentaje) {
            Dictamen dictamen = proceso.getDictamen().orElseThrow();
            assertThat(dictamen.resultado()).isEqualTo(resultado);
            assertThat(dictamen.porcentajeAprobacion()).isEqualTo(new BigDecimal(porcentaje));
            assertThat(dictamen.fechaEmision()).isEqualTo(fecha(10));
            assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.CERRADO);
            assertThat(proceso.tieneDictamenAprobatorio()).isEqualTo(resultado == ResultadoDictamen.APROBADA);
        }
    }
}
