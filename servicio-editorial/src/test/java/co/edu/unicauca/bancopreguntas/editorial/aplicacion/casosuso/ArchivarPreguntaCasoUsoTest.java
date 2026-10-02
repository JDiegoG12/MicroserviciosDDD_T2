package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaArchivada;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ArchivarPreguntaCasoUso} (CU-09).
 */
@DisplayName("ArchivarPreguntaCasoUso (CU-09)")
class ArchivarPreguntaCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Archiva una pregunta PUBLICADA y emite PreguntaArchivada con su motivo")
    void archivaYEmiteEvento() {
        String preguntaId = escenario.crearPreguntaPublicada();
        escenario.publicador.limpiar();

        assertThat(escenario.archivarPregunta.ejecutar(COMO_ADMINISTRADOR,
                new ArchivarPreguntaComando(preguntaId, "Contenido desactualizado")).estado()).isEqualTo("ARCHIVADA");

        assertThat(escenario.publicador.publicados()).singleElement().isInstanceOfSatisfying(PreguntaArchivada.class, evento -> {
            assertThat(evento.preguntaId().toString()).isEqualTo(preguntaId);
            assertThat(evento.motivo()).isEqualTo("Contenido desactualizado");
            assertThat(evento.fechaArchivado()).isEqualTo(escenario.reloj.ahora());
        });
        // INV-12: sigue existiendo y se puede consultar.
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId).estado()).isEqualTo("ARCHIVADA");
    }

    @Test
    @DisplayName("Sin motivo: SOLICITUD_INVALIDA antes de buscar la pregunta (400)")
    void motivoObligatorio() {
        String inexistente = UUID.randomUUID().toString();
        lanzaConCodigo(() -> escenario.archivarPregunta.ejecutar(COMO_ADMINISTRADOR,
                new ArchivarPreguntaComando(inexistente, null)), "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> escenario.archivarPregunta.ejecutar(COMO_ADMINISTRADOR,
                new ArchivarPreguntaComando(inexistente, "x".repeat(501))), "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> escenario.archivarPregunta.ejecutar(COMO_ADMINISTRADOR,
                new ArchivarPreguntaComando(inexistente, "Motivo")), "PREGUNTA_NO_ENCONTRADA");
    }

    @Test
    @DisplayName("Una pregunta no publicada no se archiva: TRANSICION_NO_PERMITIDA")
    void noPublicadaNoSeArchiva() {
        String preguntaId = escenario.crearPreguntaAprobada();
        lanzaConCodigo(() -> escenario.archivarPregunta.ejecutar(COMO_ADMINISTRADOR,
                new ArchivarPreguntaComando(preguntaId, "Motivo")), "TRANSICION_NO_PERMITIDA");
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO")
    void rolIncorrecto() {
        String preguntaId = escenario.crearPreguntaPublicada();
        lanzaConCodigo(() -> escenario.archivarPregunta.ejecutar(COMO_DOCENTE,
                new ArchivarPreguntaComando(preguntaId, "Motivo")), "ACCESO_DENEGADO");
    }
}
