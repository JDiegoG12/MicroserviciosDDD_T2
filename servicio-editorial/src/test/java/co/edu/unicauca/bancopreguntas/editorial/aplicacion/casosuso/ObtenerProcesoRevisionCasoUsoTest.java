package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ObtenerProcesoRevisionCasoUso} ({@code GET /procesos-revision/{procesoId}}).
 */
@DisplayName("ObtenerProcesoRevisionCasoUso")
class ObtenerProcesoRevisionCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("El administrador y los revisores asignados lo ven; los demás reciben ACCESO_DENEGADO")
    void visibilidad() {
        String procesoId = escenario.asignarDosRevisores(escenario.crearPreguntaPendiente());

        assertThat(escenario.obtenerProceso.ejecutar(COMO_ADMINISTRADOR, procesoId).procesoId()).isEqualTo(procesoId);
        assertThat(escenario.obtenerProceso.ejecutar(COMO_REVISOR_2, procesoId).estado()).isEqualTo("ABIERTO");
        lanzaConCodigo(() -> escenario.obtenerProceso.ejecutar(COMO_REVISOR_3, procesoId), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> escenario.obtenerProceso.ejecutar(COMO_AUTOR, procesoId), "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("Proceso inexistente: PROCESO_REVISION_NO_ENCONTRADO")
    void noEncontrado() {
        lanzaConCodigo(() -> escenario.obtenerProceso.ejecutar(COMO_ADMINISTRADOR, UUID.randomUUID().toString()),
                "PROCESO_REVISION_NO_ENCONTRADO");
    }
}
