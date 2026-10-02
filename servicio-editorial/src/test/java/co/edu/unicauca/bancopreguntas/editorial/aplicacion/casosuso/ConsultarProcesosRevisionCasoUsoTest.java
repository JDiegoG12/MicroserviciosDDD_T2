package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.AsignarRevisoresComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarProcesosConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.ProcesoRevisionRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.REVISOR_1;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.REVISOR_2;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.REVISOR_3;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ConsultarProcesosRevisionCasoUso} ({@code GET /procesos-revision?revisorId&estado}; CONTRATOS.md 8.1).
 */
@DisplayName("ConsultarProcesosRevisionCasoUso (CU-06)")
class ConsultarProcesosRevisionCasoUsoTest {

    private final Escenario escenario = new Escenario();
    private String abierto;
    private String cerrado;

    @BeforeEach
    void prepararProcesos() {
        abierto = escenario.asignarDosRevisores(escenario.crearPreguntaPendiente());
        String preguntaAprobada = escenario.crearPreguntaPendiente();
        cerrado = escenario.asignarDosRevisores(preguntaAprobada);
        escenario.evaluarPorAmbos(cerrado, "APROBATORIA", "APROBATORIA");
    }

    private Pagina<ProcesoRevisionRespuesta> consultar(UsuarioActual usuario,
                                                       String revisorId, String estado) {
        return escenario.consultarProcesos.ejecutar(usuario, new ConsultarProcesosConsulta(revisorId, estado, null, null));
    }

    @Test
    @DisplayName("Sin estado se usa ABIERTO; con CERRADO se ven los cerrados")
    void estadoPorDefectoYCerrado() {
        assertThat(consultar(COMO_REVISOR_1, null, null).contenido())
                .extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(abierto);
        assertThat(consultar(COMO_REVISOR_1, null, "CERRADO").contenido())
                .extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(cerrado);
    }

    @Test
    @DisplayName("Un revisor sin revisorId ve los suyos; uno sin procesos no ve nada")
    void revisorVeLosSuyos() {
        assertThat(consultar(COMO_REVISOR_2, REVISOR_2.toString(), "ABIERTO").contenido())
                .extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(abierto);
        assertThat(consultar(COMO_REVISOR_3, null, null).contenido()).isEmpty();
    }

    @Test
    @DisplayName("Un revisor que pide los procesos de otro recibe ACCESO_DENEGADO")
    void revisorNoVeLosDeOtro() {
        lanzaConCodigo(() -> consultar(COMO_REVISOR_3, REVISOR_1.toString(), "ABIERTO"), "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("El administrador puede omitir revisorId y ve todos los procesos de ese estado")
    void administradorVeTodos() {
        escenario.asignarRevisores.ejecutar(COMO_ADMINISTRADOR, new AsignarRevisoresComando(
                escenario.crearPreguntaPendiente(), List.of(REVISOR_2.toString(),
                        REVISOR_3.toString())));

        assertThat(consultar(COMO_ADMINISTRADOR, null, "ABIERTO").contenido()).hasSize(2);
        assertThat(consultar(COMO_ADMINISTRADOR, null, "CERRADO").contenido())
                .extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(cerrado);
        assertThat(consultar(COMO_ADMINISTRADOR, REVISOR_1.toString(), null).contenido())
                .extracting(ProcesoRevisionRespuesta::procesoId).containsExactly(abierto);
    }

    @Test
    @DisplayName("Paginación según 5.1")
    void paginacion() {
        Pagina<ProcesoRevisionRespuesta> pagina = escenario.consultarProcesos.ejecutar(COMO_ADMINISTRADOR,
                new ConsultarProcesosConsulta(null, "ABIERTO", 0, 1));
        assertThat(pagina.totalElementos()).isEqualTo(1);
        lanzaConCodigo(() -> escenario.consultarProcesos.ejecutar(COMO_ADMINISTRADOR,
                new ConsultarProcesosConsulta(null, "ABIERTO", -1, 10)), "SOLICITUD_INVALIDA");
    }

    @Test
    @DisplayName("Rol incorrecto → 403; estado desconocido → 400")
    void solicitudesRechazadas() {
        lanzaConCodigo(() -> consultar(COMO_AUTOR, null, null), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> consultar(COMO_REVISOR_1, null, "PENDIENTE"), "SOLICITUD_INVALIDA");
    }
}
