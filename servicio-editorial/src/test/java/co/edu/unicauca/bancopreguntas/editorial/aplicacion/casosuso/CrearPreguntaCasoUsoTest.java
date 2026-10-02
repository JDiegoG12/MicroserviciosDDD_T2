package co.edu.unicauca.bancopreguntas.editorial.aplicacion.casosuso;

import co.edu.unicauca.bancopreguntas.editorial.aplicacion.comandos.DatosDePreguntaComando;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones.ClasificacionInvalidaExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.aplicacion.resultados.PreguntaRespuesta;
import co.edu.unicauca.bancopreguntas.editorial.dobles.CatalogoAcademicoFalso;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaCreada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.ValidacionEstructuralSuperada;
import co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.AUTOR;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.FECHA;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Escenario.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Pruebas de {@link CrearPreguntaCasoUso} (CU-04).
 */
@DisplayName("CrearPreguntaCasoUso (CU-04)")
class CrearPreguntaCasoUsoTest {

    private final Escenario escenario = new Escenario();

    @Test
    @DisplayName("Catálogo válido: guarda la pregunta EN_CONSTRUCCION y publica sus eventos")
    void catalogoValido() {
        PreguntaRespuesta respuesta = escenario.crearPregunta.ejecutar(COMO_AUTOR, datosValidos());

        assertThat(respuesta.estado()).isEqualTo("EN_CONSTRUCCION");
        assertThat(respuesta.autorId()).isEqualTo(AUTOR.toString());
        assertThat(respuesta.erroresValidacion()).isEmpty();
        assertThat(respuesta.fechaCreacion()).isEqualTo(FECHA);
        assertThat(respuesta.clasificacion().subtemaId()).isEqualTo("22222222-2222-4222-8222-000000000301");
        assertThat(escenario.preguntas.cantidad()).isEqualTo(1);
        assertThat(escenario.catalogo.cantidadDeLlamadas()).isEqualTo(1);
        assertThat(escenario.publicador.publicados()).hasExactlyElementsOfTypes(
                PreguntaCreada.class, ValidacionEstructuralSuperada.class);
    }

    @Test
    @DisplayName("Una pregunta incompleta se guarda en BORRADOR con sus errores (D-02)")
    void incompletaQuedaEnBorrador() {
        PreguntaRespuesta respuesta = escenario.crearPregunta.ejecutar(COMO_AUTOR, datosIncompletos());

        assertThat(respuesta.estado()).isEqualTo("BORRADOR");
        assertThat(respuesta.erroresValidacion()).extracting(PreguntaRespuesta.ErrorValidacionRespuesta::regla)
                .contains("RF-08", "RF-09", "RF-10", "RF-11", "INV-04");
        assertThat(escenario.preguntas.cantidad()).isEqualTo(1);
    }

    @Test
    @DisplayName("Catálogo inválido: CLASIFICACION_INVALIDA con el detalle y no se guarda nada")
    void catalogoInvalido() {
        escenario.catalogo.cambiarA(CatalogoAcademicoFalso.Modo.INVALIDO);

        ClasificacionInvalidaExcepcion error = catchThrowableOfType(ClasificacionInvalidaExcepcion.class,
                () -> escenario.crearPregunta.ejecutar(COMO_AUTOR, datosValidos()));

        assertThat(error.getCodigo()).isEqualTo("CLASIFICACION_INVALIDA");
        assertThat(error.getMessage()).isEqualTo(CatalogoAcademicoFalso.DETALLE_INVALIDO);
        assertThat(error.getMotivo()).isEqualTo("SUBTEMA_NO_PERTENECE_A_TEMA");
        assertThat(escenario.preguntas.cantidadDeGuardados()).isZero();
        assertThat(escenario.publicador.publicados()).isEmpty();
    }

    @Test
    @DisplayName("Catálogo caído: CATALOGO_NO_DISPONIBLE y no se guarda nada")
    void catalogoCaido() {
        escenario.catalogo.cambiarA(CatalogoAcademicoFalso.Modo.CAIDO);

        lanzaConCodigo(() -> escenario.crearPregunta.ejecutar(COMO_AUTOR, datosValidos()), "CATALOGO_NO_DISPONIBLE");

        assertThat(escenario.preguntas.cantidadDeGuardados()).isZero();
        assertThat(escenario.publicador.publicados()).isEmpty();
    }

    @Test
    @DisplayName("Rol incorrecto: ACCESO_DENEGADO sin consultar el catálogo")
    void rolIncorrecto() {
        lanzaConCodigo(() -> escenario.crearPregunta.ejecutar(COMO_DOCENTE, datosValidos()), "ACCESO_DENEGADO");
        lanzaConCodigo(() -> escenario.crearPregunta.ejecutar(COMO_ADMINISTRADOR, datosValidos()), "ACCESO_DENEGADO");
        assertThat(escenario.catalogo.cantidadDeLlamadas()).isZero();
    }

    @Test
    @DisplayName("Sin clasificación o con nivel desconocido: SOLICITUD_INVALIDA (400)")
    void datosObligatorios() {
        DatosDePreguntaComando sinTema = new DatosDePreguntaComando("C", "P", null, null, null,
                "22222222-2222-4222-8222-000000000101", null, "22222222-2222-4222-8222-000000000301", "BAJO");
        DatosDePreguntaComando nivelDesconocido = new DatosDePreguntaComando("C", "P", null, null, null,
                "22222222-2222-4222-8222-000000000101", "22222222-2222-4222-8222-000000000201",
                "22222222-2222-4222-8222-000000000301", "FACIL");

        lanzaConCodigo(() -> escenario.crearPregunta.ejecutar(COMO_AUTOR, sinTema), "SOLICITUD_INVALIDA");
        lanzaConCodigo(() -> escenario.crearPregunta.ejecutar(COMO_AUTOR, nivelDesconocido), "SOLICITUD_INVALIDA");
        assertThat(escenario.preguntas.cantidadDeGuardados()).isZero();
    }
}
