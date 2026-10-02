package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.Rol;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.DOCENTE;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ObtenerPreguntaCasoUso} (CU-06 para una Pregunta).
 */
@DisplayName("ObtenerPreguntaCasoUso (CU-06)")
class ObtenerPreguntaCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("El autor y el administrador ven una pregunta en construcción; el docente no")
    void visibilidadDeUnaPreguntaEnConstruccion() {
        String preguntaId = escenario.crearPreguntaCompleta();

        assertThat(escenario.obtenerPregunta.ejecutar(COMO_AUTOR, preguntaId).preguntaId()).isEqualTo(preguntaId);
        assertThat(escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, preguntaId).estado()).isEqualTo("EN_CONSTRUCCION");
        lanzaConCodigo(() -> escenario.obtenerPregunta.ejecutar(COMO_DOCENTE, preguntaId), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> escenario.obtenerPregunta.ejecutar(COMO_OTRO_AUTOR, preguntaId), "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("El revisor asignado la ve; uno no asignado no")
    void visibilidadParaRevisores() {
        String preguntaId = escenario.crearPreguntaPendiente();
        escenario.asignarDosRevisores(preguntaId);

        assertThat(escenario.obtenerPregunta.ejecutar(COMO_REVISOR_1, preguntaId).estado()).isEqualTo("EN_REVISION");
        lanzaConCodigo(() -> escenario.obtenerPregunta.ejecutar(COMO_REVISOR_3, preguntaId), "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("El docente ve una pregunta publicada; basta con uno de sus roles")
    void docenteVePublicada() {
        String preguntaId = escenario.crearPreguntaPublicada();
        UsuarioActual docenteYAutor = UsuarioActual.de(DOCENTE, Rol.DOCENTE, Rol.AUTOR);

        assertThat(escenario.obtenerPregunta.ejecutar(COMO_DOCENTE, preguntaId).estado()).isEqualTo("PUBLICADA");
        assertThat(escenario.obtenerPregunta.ejecutar(docenteYAutor, preguntaId).estado()).isEqualTo("PUBLICADA");
    }

    @Test
    @DisplayName("Pregunta inexistente: PREGUNTA_NO_ENCONTRADA; id mal formado: SOLICITUD_INVALIDA")
    void noEncontrada() {
        lanzaConCodigo(() -> escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, UUID.randomUUID().toString()),
                "PREGUNTA_NO_ENCONTRADA");
        lanzaConCodigo(() -> escenario.obtenerPregunta.ejecutar(COMO_ADMINISTRADOR, "no-es-uuid"), "SOLICITUD_INVALIDA");
    }
}
