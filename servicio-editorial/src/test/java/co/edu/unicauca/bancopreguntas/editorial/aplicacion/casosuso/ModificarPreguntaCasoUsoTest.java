package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ModificarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dobles.CatalogoAcademicoFalso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaModificada;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ModificarPreguntaCasoUso} (CU-05).
 */
@DisplayName("ModificarPreguntaCasoUso (CU-05)")
class ModificarPreguntaCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Catálogo válido: modifica, revalida y publica PreguntaModificada")
    void catalogoValido() {
        String preguntaId = escenario.crearPregunta.ejecutar(COMO_AUTOR, datosIncompletos()).preguntaId();
        escenario.publicador.limpiar();

        PreguntaRespuesta respuesta = escenario.modificarPregunta.ejecutar(COMO_AUTOR,
                new ModificarPreguntaComando(preguntaId, datosValidos()));

        assertThat(respuesta.estado()).isEqualTo("EN_CONSTRUCCION");
        assertThat(respuesta.erroresValidacion()).isEmpty();
        assertThat(escenario.publicador.publicadosDeTipo(PreguntaModificada.class)).hasSize(1);
    }

    @Test
    @DisplayName("Catálogo inválido o caído: no se guarda la modificación")
    void catalogoInvalidoOCaido() {
        String preguntaId = escenario.crearPreguntaCompleta();
        int guardadosAntes = escenario.preguntas.cantidadDeGuardados();
        ModificarPreguntaComando comando = new ModificarPreguntaComando(preguntaId, datosCon("Otro contexto válido."));

        escenario.catalogo.cambiarA(CatalogoAcademicoFalso.Modo.INVALIDO);
        lanzaConCodigo(() -> escenario.modificarPregunta.ejecutar(COMO_AUTOR, comando), "CLASIFICACION_INVALIDA");
        escenario.catalogo.cambiarA(CatalogoAcademicoFalso.Modo.CAIDO);
        lanzaConCodigo(() -> escenario.modificarPregunta.ejecutar(COMO_AUTOR, comando), "CATALOGO_NO_DISPONIBLE");

        assertThat(escenario.preguntas.cantidadDeGuardados()).isEqualTo(guardadosAntes);
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_AUTOR, preguntaId).contexto())
                .isEqualTo(datosValidos().contexto());
    }

    @Test
    @DisplayName("Otro autor recibe ACCESO_DENEGADO antes de consultar el catálogo")
    void soloElDueno() {
        String preguntaId = escenario.crearPreguntaCompleta();
        int llamadasAntes = escenario.catalogo.cantidadDeLlamadas();

        lanzaConCodigo(() -> escenario.modificarPregunta.ejecutar(COMO_OTRO_AUTOR,
                new ModificarPreguntaComando(preguntaId, datosValidos())), "ACCESO_DENEGADO");

        assertThat(escenario.catalogo.cantidadDeLlamadas()).isEqualTo(llamadasAntes);
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO")
    void rolIncorrecto() {
        String preguntaId = escenario.crearPreguntaCompleta();
        lanzaConCodigo(() -> escenario.modificarPregunta.ejecutar(COMO_REVISOR_1,
                new ModificarPreguntaComando(preguntaId, datosValidos())), "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("Fuera de los estados editables: PREGUNTA_NO_EDITABLE (INV-10)")
    void noEditable() {
        String preguntaId = escenario.crearPreguntaPendiente();
        lanzaConCodigo(() -> escenario.modificarPregunta.ejecutar(COMO_AUTOR,
                new ModificarPreguntaComando(preguntaId, datosValidos())), "PREGUNTA_NO_EDITABLE");
    }

    @Test
    @DisplayName("Pregunta inexistente: PREGUNTA_NO_ENCONTRADA")
    void noEncontrada() {
        lanzaConCodigo(() -> escenario.modificarPregunta.ejecutar(COMO_AUTOR,
                new ModificarPreguntaComando(UUID.randomUUID().toString(), datosValidos())), "PREGUNTA_NO_ENCONTRADA");
    }
}
