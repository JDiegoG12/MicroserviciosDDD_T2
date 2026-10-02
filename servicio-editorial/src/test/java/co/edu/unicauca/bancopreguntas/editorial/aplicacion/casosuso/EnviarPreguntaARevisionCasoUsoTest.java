package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaSometidaARevision;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link EnviarPreguntaARevisionCasoUso} (CU-07).
 */
@DisplayName("EnviarPreguntaARevisionCasoUso (CU-07)")
class EnviarPreguntaARevisionCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("El autor somete su pregunta EN_CONSTRUCCION: queda PENDIENTE_REVISION y se publica el evento")
    void sometePregunta() {
        String preguntaId = escenario.crearPreguntaCompleta();

        assertThat(escenario.enviarARevision.ejecutar(COMO_AUTOR, preguntaId).estado()).isEqualTo("PENDIENTE_REVISION");
        assertThat(escenario.publicador.publicadosDeTipo(PreguntaSometidaARevision.class)).hasSize(1);
    }

    @Test
    @DisplayName("Una pregunta en BORRADOR no se puede someter: TRANSICION_NO_PERMITIDA")
    void borradorNoSeSomete() {
        String preguntaId = escenario.crearPregunta.ejecutar(COMO_AUTOR, datosIncompletos()).preguntaId();
        lanzaConCodigo(() -> escenario.enviarARevision.ejecutar(COMO_AUTOR, preguntaId), "TRANSICION_NO_PERMITIDA");
    }

    @Test
    @DisplayName("Rol incorrecto u otro autor: ACCESO_DENEGADO")
    void accesoDenegado() {
        String preguntaId = escenario.crearPreguntaCompleta();
        lanzaConCodigo(() -> escenario.enviarARevision.ejecutar(COMO_ADMINISTRADOR, preguntaId), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> escenario.enviarARevision.ejecutar(COMO_OTRO_AUTOR, preguntaId), "ACCESO_DENEGADO");
    }
}
