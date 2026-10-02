package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaPublicada;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.CLASIFICACION;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link PublicarPreguntaCasoUso} (CU-08).
 */
@DisplayName("PublicarPreguntaCasoUso (CU-08)")
class PublicarPreguntaCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Publica una pregunta APROBADA y emite PreguntaPublicada con los datos de CONTRATOS 7.4")
    void publicaYEmiteEvento() {
        String preguntaId = escenario.crearPreguntaAprobada();
        escenario.publicador.limpiar();

        PreguntaRespuesta respuesta = escenario.publicarPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId);

        assertThat(respuesta.estado()).isEqualTo("PUBLICADA");
        List<PreguntaPublicada> eventos = escenario.publicador.publicadosDeTipo(PreguntaPublicada.class);
        assertThat(escenario.publicador.publicados()).hasSize(1);
        PreguntaPublicada evento = eventos.get(0);
        assertThat(evento.preguntaId().toString()).isEqualTo(preguntaId);
        assertThat(evento.contexto()).isEqualTo(datosValidos().contexto());
        assertThat(evento.preguntaDirecta()).isEqualTo(datosValidos().preguntaDirecta());
        assertThat(evento.opciones()).containsExactly(
                new PreguntaPublicada.OpcionPublicada("A", "3,0"),
                new PreguntaPublicada.OpcionPublicada("B", "3,8"),
                new PreguntaPublicada.OpcionPublicada("C", "4,0"),
                new PreguntaPublicada.OpcionPublicada("D", "4,5"));
        assertThat(evento.letraCorrecta()).isEqualTo("C");
        assertThat(evento.clasificacion()).isEqualTo(CLASIFICACION);
        assertThat(evento.nivelDificultad().name()).isEqualTo("BAJO");
        assertThat(evento.fechaPublicacion()).isEqualTo(escenario.reloj.ahora());
        assertThat(evento.fechaOcurrencia()).isEqualTo(evento.fechaPublicacion());
    }

    @Test
    @DisplayName("Una pregunta no aprobada no se publica: TRANSICION_NO_PERMITIDA y sin eventos")
    void noAprobadaNoSePublica() {
        String preguntaId = escenario.crearPreguntaCompleta();
        escenario.publicador.limpiar();

        lanzaConCodigo(() -> escenario.publicarPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId), "TRANSICION_NO_PERMITIDA");
        assertThat(escenario.publicador.publicados()).isEmpty();
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO")
    void rolIncorrecto() {
        String preguntaId = escenario.crearPreguntaAprobada();
        lanzaConCodigo(() -> escenario.publicarPregunta.ejecutar(COMO_AUTOR, preguntaId), "ACCESO_DENEGADO");
    }
}
