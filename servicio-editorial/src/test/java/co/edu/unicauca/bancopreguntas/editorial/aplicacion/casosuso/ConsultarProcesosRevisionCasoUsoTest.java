package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.REVISOR_1;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.REVISOR_2;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ConsultarProcesosRevisionCasoUso} ({@code GET /procesos-revision?revisorId&estado=ABIERTO}).
 */
@DisplayName("ConsultarProcesosRevisionCasoUso (CU-06)")
class ConsultarProcesosRevisionCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Un revisor ve solo sus procesos abiertos")
    void revisorVeSusProcesosAbiertos() {
        String abierto = escenario.asignarDosRevisores(escenario.crearPreguntaPendiente());
        escenario.crearPreguntaAprobada();

        assertThat(escenario.consultarProcesos.ejecutar(COMO_REVISOR_1, new ConsultarProcesosConsulta(null, "ABIERTO", null, null))
                .contenido()).extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(abierto);
        assertThat(escenario.consultarProcesos.ejecutar(COMO_REVISOR_3, new ConsultarProcesosConsulta(null, null, null, null))
                .contenido()).isEmpty();
    }

    @Test
    @DisplayName("Un revisor no puede consultar los procesos de otro; el administrador sí")
    void restriccionPorRevisor() {
        String abierto = escenario.asignarDosRevisores(escenario.crearPreguntaPendiente());

        lanzaConCodigo(() -> escenario.consultarProcesos.ejecutar(COMO_REVISOR_3,
                new ConsultarProcesosConsulta(REVISOR_1.toString(), "ABIERTO", null, null)), "ACCESO_DENEGADO");
        assertThat(escenario.consultarProcesos.ejecutar(COMO_ADMINISTRADOR,
                new ConsultarProcesosConsulta(REVISOR_2.toString(), "ABIERTO", null, null)).contenido())
                .extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(abierto);
    }

    @Test
    @DisplayName("Rol incorrecto, estado distinto de ABIERTO o administrador sin revisorId se rechazan")
    void solicitudesRechazadas() {
        lanzaConCodigo(() -> escenario.consultarProcesos.ejecutar(COMO_AUTOR,
                new ConsultarProcesosConsulta(null, null, null, null)), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> escenario.consultarProcesos.ejecutar(COMO_REVISOR_1,
                new ConsultarProcesosConsulta(null, "CERRADO", null, null)), "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> escenario.consultarProcesos.ejecutar(COMO_ADMINISTRADOR,
                new ConsultarProcesosConsulta(null, "ABIERTO", null, null)), "SOLICITUD_INVALIDA");
    }
}
