package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaAprobada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaEnRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaPublicada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaRechazada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.DictamenEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EntradaDeHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EvaluacionEnHistorial;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.Pregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.DecisionRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.FormatoDeEvaluacion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ResultadoDictamen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de los servicios de dominio del Taller 1, sección 10.2 a 10.4.
 */
@DisplayName("Servicios de dominio")
class ServiciosDeDominioTest {

    private final AsignadorRevisoresServicio asignador = new AsignadorRevisoresServicio();
    private final ResolutorDictamenServicio resolutor = new ResolutorDictamenServicio();
    private final PublicadorPreguntaServicio publicador = new PublicadorPreguntaServicio();

    private Pregunta preguntaPendiente() {
        Pregunta pregunta = preguntaEnConstruccion();
        pregunta.enviarARevision(AUTOR, fecha(1));
        pregunta.extraerEventos();
        return pregunta;
    }

    private ProcesoDeRevision asignar(Pregunta pregunta, UsuarioId... revisores) {
        return asignador.asignar(pregunta, ProcesoDeRevisionId.generar(), List.of(revisores), ADMINISTRADOR, fecha(2));
    }

    private void evaluarYResolver(ProcesoDeRevision proceso, Pregunta pregunta, UsuarioId revisor, DecisionRevision decision) {
        FormatoDeEvaluacion formato = proceso.registrarEvaluacion(revisor, criteriosValidos(), observaciones(), decision, fecha(3));
        resolutor.resolver(proceso, formato, pregunta, revisor, fecha(3));
    }

    @Nested
    @DisplayName("AsignadorRevisoresServicio (10.2, D-14)")
    class Asignador {

        @Test
        @DisplayName("Abre el proceso y pasa la pregunta a EN_REVISION en un solo hecho")
        void abreProcesoEIniciaRevision() {
            Pregunta pregunta = preguntaPendiente();

            ProcesoDeRevision proceso = asignar(pregunta, REVISOR_1, REVISOR_2);

            assertThat(proceso.getPreguntaId()).isEqualTo(pregunta.getId());
            assertThat(proceso.getAutorId()).isEqualTo(AUTOR);
            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_REVISION);
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(PreguntaEnRevision.class);
        }

        @Test
        @DisplayName("Si los revisores no son válidos la pregunta no cambia de estado")
        void revisoresInvalidosNoTocanLaPregunta() {
            Pregunta pregunta = preguntaPendiente();

            lanzaConCodigo(() -> asignar(pregunta, AUTOR, REVISOR_1), "AUTOR_NO_PUEDE_SER_REVISOR");

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
        }

        @Test
        @DisplayName("Una pregunta que no está PENDIENTE_REVISION no puede pasar a revisión")
        void preguntaNoPendiente() {
            lanzaConCodigo(() -> asignar(preguntaEnConstruccion(), REVISOR_1, REVISOR_2), "TRANSICION_NO_PERMITIDA");
        }
    }

    @Nested
    @DisplayName("ResolutorDictamenServicio (10.3, D-15)")
    class Resolutor {

        @Test
        @DisplayName("Cada evaluación pasa al historial; sin dictamen la pregunta sigue EN_REVISION")
        void evaluacionAlHistorial() {
            Pregunta pregunta = preguntaPendiente();
            ProcesoDeRevision proceso = asignar(pregunta, REVISOR_1, REVISOR_2);

            evaluarYResolver(proceso, pregunta, REVISOR_1, DecisionRevision.APROBATORIA);

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_REVISION);
            assertThat(pregunta.getHistorialRevisiones().entradas()).singleElement()
                    .isInstanceOfSatisfying(EvaluacionEnHistorial.class, entrada -> {
                        assertThat(entrada.revisorId()).isEqualTo(REVISOR_1);
                        assertThat(entrada.procesoId()).isEqualTo(proceso.getId());
                    });
        }

        @Test
        @DisplayName("Con dictamen APROBADA anexa el dictamen y aprueba la pregunta")
        void dictamenAprobatorio() {
            Pregunta pregunta = preguntaPendiente();
            ProcesoDeRevision proceso = asignar(pregunta, REVISOR_1, REVISOR_2);
            pregunta.extraerEventos();

            evaluarYResolver(proceso, pregunta, REVISOR_1, DecisionRevision.APROBATORIA);
            evaluarYResolver(proceso, pregunta, REVISOR_2, DecisionRevision.APROBATORIA);

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.APROBADA);
            assertThat(pregunta.getHistorialRevisiones().entradas()).hasExactlyElementsOfTypes(
                    EvaluacionEnHistorial.class, EvaluacionEnHistorial.class, DictamenEnHistorial.class);
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(PreguntaAprobada.class);
        }

        @Test
        @DisplayName("Rechazo: la pregunta queda EN_CONSTRUCCION con el historial intacto y es editable otra vez (D-07)")
        void rechazoConservaHistorialYEsEditable() {
            Pregunta pregunta = preguntaPendiente();
            ProcesoDeRevision proceso = asignar(pregunta, REVISOR_1, REVISOR_2);
            pregunta.extraerEventos();

            evaluarYResolver(proceso, pregunta, REVISOR_1, DecisionRevision.APROBATORIA);
            evaluarYResolver(proceso, pregunta, REVISOR_2, DecisionRevision.REPROBATORIA);
            List<EntradaDeHistorial> historialTrasRechazo = pregunta.getHistorialRevisiones().entradas();

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
            assertThat(historialTrasRechazo).hasSize(3);
            assertThat(((DictamenEnHistorial) historialTrasRechazo.get(2)).dictamen().resultado())
                    .isEqualTo(ResultadoDictamen.RECHAZADA);
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(PreguntaRechazada.class);

            pregunta.modificar(AUTOR, contenido().conJustificacion("Justificación corregida tras la revisión.").construir(), fecha(4));
            pregunta.enviarARevision(AUTOR, fecha(5));

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.PENDIENTE_REVISION);
            assertThat(pregunta.getHistorialRevisiones().entradas()).isEqualTo(historialTrasRechazo);
        }
    }

    @Nested
    @DisplayName("PublicadorPreguntaServicio (10.4)")
    class Publicador {

        @Test
        @DisplayName("Publica si el proceso vigente tiene dictamen APROBADA")
        void publicaConDictamenFavorable() {
            Pregunta pregunta = preguntaPendiente();
            ProcesoDeRevision proceso = asignar(pregunta, REVISOR_1, REVISOR_2);
            evaluarYResolver(proceso, pregunta, REVISOR_1, DecisionRevision.APROBATORIA);
            evaluarYResolver(proceso, pregunta, REVISOR_2, DecisionRevision.APROBATORIA);
            pregunta.extraerEventos();

            publicador.publicar(pregunta, Optional.of(proceso), ADMINISTRADOR, fecha(6));

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(PreguntaPublicada.class);
        }

        @Test
        @DisplayName("Sin proceso vigente o sin dictamen favorable lanza TRANSICION_NO_PERMITIDA")
        void noPublicaSinDictamenFavorable() {
            Pregunta aprobadaSinProceso = preguntaEnEstado(EstadoPregunta.APROBADA);
            lanzaConCodigo(() -> publicador.publicar(aprobadaSinProceso, Optional.empty(), ADMINISTRADOR, fecha(6)),
                    "TRANSICION_NO_PERMITIDA");
            assertThat(aprobadaSinProceso.getEstado()).isEqualTo(EstadoPregunta.APROBADA);

            Pregunta pregunta = preguntaPendiente();
            ProcesoDeRevision abierto = asignar(pregunta, REVISOR_1, REVISOR_2);
            lanzaConCodigo(() -> publicador.publicar(pregunta, Optional.of(abierto), ADMINISTRADOR, fecha(6)),
                    "TRANSICION_NO_PERMITIDA");
        }
    }
}
