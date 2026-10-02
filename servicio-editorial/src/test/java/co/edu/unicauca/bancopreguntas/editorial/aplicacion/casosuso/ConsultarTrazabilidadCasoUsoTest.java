package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ArchivarPreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.TrazabilidadRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ConsultarTrazabilidadCasoUso} (CU-18).
 */
@DisplayName("ConsultarTrazabilidadCasoUso (CU-18)")
class ConsultarTrazabilidadCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Devuelve el ciclo de vida completo en orden cronológico, con responsables y el historial de revisión")
    void cicloCompletoEnOrdenCronologico() {
        String preguntaId = escenario.crearPreguntaPublicada();
        escenario.archivarPregunta.ejecutar(COMO_ADMINISTRADOR, new ArchivarPreguntaComando(preguntaId, "Desactualizada"));

        TrazabilidadRespuesta trazabilidad = escenario.consultarTrazabilidad.ejecutar(COMO_ADMINISTRADOR, preguntaId);

        List<TrazabilidadRespuesta.RegistroRespuesta> registros = trazabilidad.registros();
        assertThat(registros).extracting(TrazabilidadRespuesta.RegistroRespuesta::fecha)
                .isSortedAccordingTo(Comparator.<Instant>naturalOrder());
        assertThat(registros).extracting(TrazabilidadRespuesta.RegistroRespuesta::tipo)
                .containsExactly("CREACION", "TRANSICION", "TRANSICION", "TRANSICION", "TRANSICION", "TRANSICION", "TRANSICION");
        assertThat(registros).extracting(TrazabilidadRespuesta.RegistroRespuesta::estadoNuevo)
                .containsExactly("BORRADOR", "EN_CONSTRUCCION", "PENDIENTE_REVISION", "EN_REVISION", "APROBADA",
                        "PUBLICADA", "ARCHIVADA");
        assertThat(registros).extracting(TrazabilidadRespuesta.RegistroRespuesta::usuarioId)
                .containsExactly(AUTOR.toString(), AUTOR.toString(), AUTOR.toString(), ADMINISTRADOR.toString(),
                        REVISOR_2.toString(), ADMINISTRADOR.toString(), ADMINISTRADOR.toString());
        assertThat(registros.get(0).estadoAnterior()).isNull();

        assertThat(trazabilidad.historialRevisiones()).extracting(TrazabilidadRespuesta.EntradaHistorialRespuesta::tipo)
                .containsExactly("EVALUACION", "EVALUACION", "DICTAMEN");
        assertThat(trazabilidad.historialRevisiones()).extracting(TrazabilidadRespuesta.EntradaHistorialRespuesta::fecha)
                .isSortedAccordingTo(Comparator.<Instant>naturalOrder());
        assertThat(trazabilidad.historialRevisiones().get(2).dictamen().resultado()).isEqualTo("APROBADA");
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO; pregunta inexistente: PREGUNTA_NO_ENCONTRADA")
    void errores() {
        String preguntaId = escenario.crearPreguntaCompleta();
        lanzaConCodigo(() -> escenario.consultarTrazabilidad.ejecutar(COMO_AUTOR, preguntaId), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> escenario.consultarTrazabilidad.ejecutar(COMO_ADMINISTRADOR,
                "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f"), "PREGUNTA_NO_ENCONTRADA");
    }
}
