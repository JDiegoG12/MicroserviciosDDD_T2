package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.ConsultarPreguntasConsulta;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaResumen;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad.UsuarioActual;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Pagina;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.AUTOR;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.REVISOR_3;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de {@link ConsultarPreguntasCasoUso} (CU-06), con la restricción por rol.
 */
@DisplayName("ConsultarPreguntasCasoUso (CU-06)")
class ConsultarPreguntasCasoUsoTest {

    private final Escenario escenario = new Escenario();
    private String borrador;
    private String enRevision;
    private String publicada;
    private String deOtroAutor;

    @BeforeEach
    void prepararBanco() {
        borrador = escenario.crearPregunta.ejecutar(COMO_AUTOR, datosIncompletos()).preguntaId();
        String pendiente = escenario.crearPreguntaPendiente();
        escenario.asignarDosRevisores(pendiente);
        enRevision = pendiente;
        publicada = escenario.crearPreguntaPublicada();
        deOtroAutor = escenario.crearPregunta.ejecutar(COMO_OTRO_AUTOR, datosValidos()).preguntaId();
    }

    private Pagina<PreguntaResumen> consultar(UsuarioActual usuario, ConsultarPreguntasConsulta consulta) {
        return escenario.consultarPreguntas.ejecutar(usuario, consulta);
    }

    @Test
    @DisplayName("ADMINISTRADOR ve todas")
    void administradorVeTodas() {
        assertThat(consultar(COMO_ADMINISTRADOR, ConsultarPreguntasConsulta.sinFiltros()).contenido())
                .extracting(PreguntaResumen::preguntaId)
                .containsExactlyInAnyOrder(borrador, enRevision, publicada, deOtroAutor);
    }

    @Test
    @DisplayName("AUTOR ve solo las suyas, aunque filtre por otro autor")
    void autorVeLasSuyas() {
        assertThat(consultar(COMO_AUTOR, ConsultarPreguntasConsulta.sinFiltros()).contenido())
                .extracting(PreguntaResumen::preguntaId)
                .containsExactlyInAnyOrder(borrador, enRevision, publicada);
        ConsultarPreguntasConsulta deOtro = new ConsultarPreguntasConsulta(null, null, null, null, null,
                REVISOR_3.toString(), null, null);
        assertThat(consultar(COMO_AUTOR, deOtro).contenido()).isEmpty();
    }

    @Test
    @DisplayName("REVISOR ve solo las de sus procesos abiertos")
    void revisorVeLasAsignadas() {
        assertThat(consultar(COMO_REVISOR_1, ConsultarPreguntasConsulta.sinFiltros()).contenido())
                .extracting(PreguntaResumen::preguntaId)
                .containsExactly(enRevision);
        assertThat(consultar(COMO_REVISOR_3, ConsultarPreguntasConsulta.sinFiltros()).contenido()).isEmpty();
    }

    @Test
    @DisplayName("DOCENTE ve solo las PUBLICADA")
    void docenteVeLasPublicadas() {
        assertThat(consultar(COMO_DOCENTE, ConsultarPreguntasConsulta.sinFiltros()).contenido())
                .extracting(PreguntaResumen::preguntaId)
                .containsExactly(publicada);
        ConsultarPreguntasConsulta borradores = new ConsultarPreguntasConsulta(null, null, null, null, "BORRADOR",
                null, null, null);
        assertThat(consultar(COMO_DOCENTE, borradores).contenido()).isEmpty();
    }

    @Test
    @DisplayName("Rol sin acceso a preguntas (ESTUDIANTE): ACCESO_DENEGADO")
    void estudianteSinAcceso() {
        lanzaConCodigo(() -> consultar(COMO_ESTUDIANTE, ConsultarPreguntasConsulta.sinFiltros()), "ACCESO_DENEGADO");
    }

    @Test
    @DisplayName("Aplica los filtros y la paginación de CONTRATOS 5.1")
    void filtrosYPaginacion() {
        ConsultarPreguntasConsulta medio = new ConsultarPreguntasConsulta(null, null, null, "MEDIO", null,
                AUTOR.toString(), null, null);
        assertThat(consultar(COMO_ADMINISTRADOR, medio).contenido()).extracting(PreguntaResumen::preguntaId)
                .containsExactly(borrador);

        Pagina<PreguntaResumen> pagina = consultar(COMO_ADMINISTRADOR,
                new ConsultarPreguntasConsulta(null, null, null, null, null, null, 1, 3));
        assertThat(pagina.contenido()).hasSize(1);
        assertThat(pagina.totalElementos()).isEqualTo(4);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
        assertThat(pagina.pagina()).isEqualTo(1);

        lanzaConCodigo(() -> consultar(COMO_ADMINISTRADOR,
                new ConsultarPreguntasConsulta(null, null, null, null, null, null, 0, 101)), "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> consultar(COMO_ADMINISTRADOR,
                new ConsultarPreguntasConsulta(null, null, null, null, "PERDIDA", null, null, null)), "SOLICITUD_INVALIDA");
    }
}
