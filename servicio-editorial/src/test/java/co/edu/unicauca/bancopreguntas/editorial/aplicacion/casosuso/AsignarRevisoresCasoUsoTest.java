package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaEnRevision;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.ProcesoDeRevisionAbierto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.RevisoresAsignados;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link AsignarRevisoresCasoUso} (CU-10).
 */
@DisplayName("AsignarRevisoresCasoUso (CU-10)")
class AsignarRevisoresCasoUsoTest {

    private final Escenario escenario = new Escenario();

    private AsignarRevisoresComando comando(String preguntaId, String... revisores) {
        return new AsignarRevisoresComando(preguntaId, List.of(revisores));
    }

    @Test
    @DisplayName("Abre el proceso, pasa la pregunta a EN_REVISION y publica los tres eventos")
    void abreElProceso() {
        String preguntaId = escenario.crearPreguntaPendiente();
        escenario.publicador.limpiar();

        ProcesoRevisionRespuesta proceso = escenario.asignarRevisores.ejecutar(COMO_ADMINISTRADOR,
                comando(preguntaId, REVISOR_1.toString(), REVISOR_2.toString()));

        assertThat(proceso.estado()).isEqualTo("ABIERTO");
        assertThat(proceso.preguntaId()).isEqualTo(preguntaId);
        assertThat(proceso.dictamen()).isNull();
        assertThat(proceso.asignaciones()).extracting(ProcesoRevisionRespuesta.AsignacionRespuesta::revisorId)
                .containsExactly(REVISOR_1.toString(), REVISOR_2.toString());
        assertThat(proceso.asignaciones()).noneMatch(ProcesoRevisionRespuesta.AsignacionRespuesta::evaluacionRegistrada);
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId).estado()).isEqualTo("EN_REVISION");
        assertThat(escenario.publicador.publicados()).hasExactlyElementsOfTypes(
                ProcesoDeRevisionAbierto.class, RevisoresAsignados.class, PreguntaEnRevision.class);
    }

    @Test
    @DisplayName("Violaciones de INV-15, INV-16 e INV-17 con su código; no se guarda nada")
    void invariantesDeAsignacion() {
        String preguntaId = escenario.crearPreguntaPendiente();

        lanzaConCodigo(() -> escenario.asignarRevisores.ejecutar(COMO_ADMINISTRADOR,
                comando(preguntaId, REVISOR_1.toString())), "REVISORES_INSUFICIENTES");
        lanzaConCodigo(() -> escenario.asignarRevisores.ejecutar(COMO_ADMINISTRADOR,
                comando(preguntaId, REVISOR_1.toString(), AUTOR.toString())), "AUTOR_NO_PUEDE_SER_REVISOR");
        lanzaConCodigo(() -> escenario.asignarRevisores.ejecutar(COMO_ADMINISTRADOR,
                comando(preguntaId, REVISOR_1.toString(), REVISOR_1.toString())), "REVISOR_DUPLICADO");

        assertThat(escenario.procesos.cantidad()).isZero();
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId).estado()).isEqualTo("PENDIENTE_REVISION");
    }

    @Test
    @DisplayName("Una pregunta que no está PENDIENTE_REVISION: TRANSICION_NO_PERMITIDA")
    void preguntaNoPendiente() {
        String preguntaId = escenario.crearPreguntaCompleta();
        lanzaConCodigo(() -> escenario.asignarRevisores.ejecutar(COMO_ADMINISTRADOR,
                comando(preguntaId, REVISOR_1.toString(), REVISOR_2.toString())), "TRANSICION_NO_PERMITIDA");
        assertThat(escenario.procesos.cantidad()).isZero();
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO")
    void rolIncorrecto() {
        String preguntaId = escenario.crearPreguntaPendiente();
        lanzaConCodigo(() -> escenario.asignarRevisores.ejecutar(COMO_AUTOR,
                comando(preguntaId, REVISOR_1.toString(), REVISOR_2.toString())), "ACCESO_DENEGADO");
    }
}
