package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.EventoDeDominio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaArchivada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaCreada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaModificada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.PreguntaPublicada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.eventos.ValidacionEstructuralSuperada;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.Dictamen;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision.ProcesoDeRevisionId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.repositorios.PreguntaRepositorio;
import co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ErrorDeValidacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del agregado Pregunta: invariantes INV-01 a INV-14, propiedad del autor y eventos emitidos.
 */
@DisplayName("Agregado Pregunta")
class PreguntaTest {

    private static Pregunta crearCon(ContenidoDePregunta contenido) {
        return Pregunta.crear(PreguntaId.generar(), AUTOR, contenido, FECHA);
    }

    private static List<String> reglas(Pregunta pregunta) {
        return pregunta.erroresValidacion().stream().map(ErrorDeValidacion::regla).toList();
    }

    /** Comprueba que la Pregunta quedó retenida en BORRADOR por la regla dada y no puede someterse (INV-11). */
    private static void quedaRetenidaEnBorradorPor(Pregunta pregunta, String regla) {
        assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
        assertThat(reglas(pregunta)).contains(regla);
        lanzaConCodigo(() -> pregunta.enviarARevision(AUTOR, fecha(1)), "TRANSICION_NO_PERMITIDA");
    }

    @Nested
    @DisplayName("crear (CU-04)")
    class Crear {

        @Test
        @DisplayName("Una pregunta completa nace en BORRADOR y pasa a EN_CONSTRUCCION en el mismo paso")
        void completaPasaAEnConstruccion() {
            Pregunta pregunta = crearCon(contenidoValido());

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
            assertThat(pregunta.erroresValidacion()).isEmpty();
            assertThat(pregunta.getTrazabilidad()).extracting(RegistroDeTrazabilidad::tipo)
                    .containsExactly(TipoDeRegistro.CREACION, TipoDeRegistro.TRANSICION);
            RegistroDeTrazabilidad transicion = pregunta.getTrazabilidad().get(1);
            assertThat(transicion.estadoAnterior()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(transicion.estadoNuevo()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(
                    PreguntaCreada.class, ValidacionEstructuralSuperada.class);
        }

        @Test
        @DisplayName("Una pregunta incompleta queda en BORRADOR con sus errores de validación (D-02)")
        void incompletaQuedaEnBorrador() {
            Pregunta pregunta = crearCon(contenidoIncompleto());

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(reglas(pregunta)).containsExactly("RF-08");
            assertThat(pregunta.getTrazabilidad()).extracting(RegistroDeTrazabilidad::tipo)
                    .containsExactly(TipoDeRegistro.CREACION);
            assertThat(pregunta.getTrazabilidad().get(0).estadoAnterior()).isNull();
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(PreguntaCreada.class);
        }

        @Test
        @DisplayName("Los eventos se extraen una sola vez")
        void losEventosSeExtraenUnaVez() {
            Pregunta pregunta = crearCon(contenidoValido());
            assertThat(pregunta.extraerEventos()).isNotEmpty();
            assertThat(pregunta.extraerEventos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invariantes estructurales INV-01 a INV-08")
    class InvariantesEstructurales {

        @Test
        @DisplayName("INV-01: con tres opciones queda en BORRADOR por RF-10")
        void inv01CuatroOpciones() {
            List<OpcionDeRespuesta> tres = opcionesValidas().subList(0, 3);
            quedaRetenidaEnBorradorPor(crearCon(contenido().conOpciones(tres).construir()), "RF-10");
        }

        @Test
        @DisplayName("INV-02: con dos opciones correctas queda en BORRADOR por RF-11")
        void inv02UnaSolaCorrecta() {
            List<OpcionDeRespuesta> dosCorrectas = List.of(
                    new OpcionDeRespuesta("A", "3,0", true),
                    new OpcionDeRespuesta("B", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", true),
                    new OpcionDeRespuesta("D", "4,5", false));
            quedaRetenidaEnBorradorPor(crearCon(contenido().conOpciones(dosCorrectas).construir()), "RF-11");
        }

        @Test
        @DisplayName("INV-03: sin pregunta directa queda en BORRADOR por RF-09, y una nula se rechaza")
        void inv03UnaPreguntaDirecta() {
            quedaRetenidaEnBorradorPor(crearCon(contenido().conPreguntaDirecta("").construir()), "RF-09");
            lanzaConCodigo(() -> new PreguntaDirecta(null), "SOLICITUD_INVALIDA");
        }

        @Test
        @DisplayName("INV-04: sin justificación o sin bibliografía queda en BORRADOR")
        void inv04ComponentesObligatorios() {
            quedaRetenidaEnBorradorPor(crearCon(contenido().conJustificacion("").construir()), "INV-04");
            quedaRetenidaEnBorradorPor(crearCon(contenido().conBibliografia(List.of()).construir()), "INV-04");
            quedaRetenidaEnBorradorPor(crearCon(contenido().conContexto(" ").construir()), "RF-08");
        }

        @Test
        @DisplayName("INV-05: una opción con \"ninguna de las anteriores\" queda en BORRADOR por RF-12")
        void inv05FrasesProhibidas() {
            List<OpcionDeRespuesta> conFrase = List.of(
                    new OpcionDeRespuesta("A", "Ninguna de las anteriores", false),
                    new OpcionDeRespuesta("B", "Promedio de las notas", false),
                    new OpcionDeRespuesta("C", "Valor que más se repite", true),
                    new OpcionDeRespuesta("D", "Valor central ordenado", false));
            quedaRetenidaEnBorradorPor(crearCon(contenido().conOpciones(conFrase).construir()), "RF-12");
        }

        @Test
        @DisplayName("INV-06: un distractor demasiado largo queda en BORRADOR por RF-13")
        void inv06LongitudDeDistractores() {
            List<OpcionDeRespuesta> desproporcionadas = List.of(
                    new OpcionDeRespuesta("A", "Un distractor larguísimo que no guarda proporción", false),
                    new OpcionDeRespuesta("B", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", true),
                    new OpcionDeRespuesta("D", "4,5", false));
            quedaRetenidaEnBorradorPor(crearCon(contenido().conOpciones(desproporcionadas).construir()), "RF-13");
        }

        @Test
        @DisplayName("INV-07: la clasificación exige los tres identificadores UUID")
        void inv07ClasificacionCompleta() {
            lanzaConCodigo(() -> ClasificacionAcademica.de("22222222-2222-4222-8222-000000000101", null,
                    "22222222-2222-4222-8222-000000000301"), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> ClasificacionAcademica.de("no-es-uuid", "22222222-2222-4222-8222-000000000201",
                    "22222222-2222-4222-8222-000000000301"), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> contenido().conClasificacion(null).construir(), "SOLICITUD_INVALIDA");
        }

        @Test
        @DisplayName("INV-08: el nivel de dificultad pertenece al conjunto cerrado {BAJO, MEDIO, ALTO}")
        void inv08NivelCerrado() {
            assertThat(NivelDeDificultad.desdeNombre("ALTO")).isEqualTo(NivelDeDificultad.ALTO);
            lanzaConCodigo(() -> NivelDeDificultad.desdeNombre("MUY_ALTO"), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> NivelDeDificultad.desdeNombre("bajo"), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> NivelDeDificultad.desdeNombre(null), "SOLICITUD_INVALIDA");
        }
    }

    @Nested
    @DisplayName("Ciclo de vida INV-09 a INV-11")
    class CicloDeVida {

        @Test
        @DisplayName("INV-09: una transición no declarada se rechaza y no cambia nada")
        void inv09TransicionNoDeclarada() {
            Pregunta pregunta = preguntaEnConstruccion();
            int registrosAntes = pregunta.getTrazabilidad().size();

            lanzaConCodigo(() -> pregunta.publicar(ADMINISTRADOR, fecha(1)), "TRANSICION_NO_PERMITIDA");

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
            assertThat(pregunta.getTrazabilidad()).hasSize(registrosAntes);
            assertThat(pregunta.extraerEventos()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = EstadoPregunta.class, names = {"PENDIENTE_REVISION", "EN_REVISION", "APROBADA", "PUBLICADA", "ARCHIVADA"})
        @DisplayName("INV-10: fuera de BORRADOR y EN_CONSTRUCCION no se puede modificar")
        void inv10SoloEditableEnEstadosEditables(EstadoPregunta estado) {
            Pregunta pregunta = preguntaEnEstado(estado);
            lanzaConCodigo(() -> pregunta.modificar(AUTOR, contenidoValido(), fecha(1)), "PREGUNTA_NO_EDITABLE");
            assertThat(pregunta.getEstado()).isEqualTo(estado);
        }

        @Test
        @DisplayName("INV-11: no sale de BORRADOR mientras no supere la validación; al completarse pasa a EN_CONSTRUCCION")
        void inv11NoSaleDeBorradorSinValidacion() {
            Pregunta pregunta = crearCon(contenidoIncompleto());

            pregunta.modificar(AUTOR, contenido().conJustificacion("").construir(), fecha(1));
            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);

            pregunta.modificar(AUTOR, contenidoValido(), fecha(2));
            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
        }

        @Test
        @DisplayName("Una modificación que deja incompleta una pregunta EN_CONSTRUCCION la devuelve a BORRADOR")
        void modificacionIncompletaVuelveABorrador() {
            Pregunta pregunta = preguntaEnConstruccion();

            pregunta.modificar(AUTOR, contenidoIncompleto(), fecha(1));

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.BORRADOR);
            List<RegistroDeTrazabilidad> registros = pregunta.getTrazabilidad();
            assertThat(registros.get(registros.size() - 2).tipo()).isEqualTo(TipoDeRegistro.MODIFICACION);
            assertThat(registros.get(registros.size() - 1).estadoNuevo()).isEqualTo(EstadoPregunta.BORRADOR);
            assertThat(pregunta.extraerEventos()).hasExactlyElementsOfTypes(PreguntaModificada.class);
        }

        @Test
        @DisplayName("Solo el autor modifica y somete su pregunta (ACCESO_DENEGADO)")
        void soloElAutor() {
            Pregunta pregunta = preguntaEnConstruccion();
            lanzaConCodigo(() -> pregunta.modificar(REVISOR_1, contenidoValido(), fecha(1)), "ACCESO_DENEGADO");
            lanzaConCodigo(() -> pregunta.enviarARevision(ADMINISTRADOR, fecha(1)), "ACCESO_DENEGADO");
        }

        @Test
        @DisplayName("Rechazar pasa por RECHAZADA y queda EN_CONSTRUCCION, editable otra vez (D-07)")
        void rechazarVuelveAEnConstruccion() {
            Pregunta pregunta = preguntaEnRevision();

            pregunta.rechazar(REVISOR_2, fecha(5));

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
            List<RegistroDeTrazabilidad> registros = pregunta.getTrazabilidad();
            assertThat(registros.subList(registros.size() - 2, registros.size()))
                    .extracting(RegistroDeTrazabilidad::estadoAnterior, RegistroDeTrazabilidad::estadoNuevo)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(EstadoPregunta.EN_REVISION, EstadoPregunta.RECHAZADA),
                            org.assertj.core.groups.Tuple.tuple(EstadoPregunta.RECHAZADA, EstadoPregunta.EN_CONSTRUCCION));
            pregunta.modificar(AUTOR, contenidoValido(), fecha(6));
            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.EN_CONSTRUCCION);
        }

        @Test
        @DisplayName("El dictamen automático registra al revisor que lo disparó con el detalle exacto de 11.1")
        void detalleDelDictamenAutomatico() {
            String detalleEsperado = "Dictamen automático (CU-12) disparado por la evaluación del revisor " + REVISOR_2;
            Pregunta rechazada = preguntaEnRevision();
            Pregunta aprobada = preguntaEnRevision();

            rechazada.rechazar(REVISOR_2, fecha(5));
            aprobada.aprobar(REVISOR_2, fecha(5));

            List<RegistroDeTrazabilidad> deRechazo = rechazada.getTrazabilidad();
            assertThat(deRechazo.subList(deRechazo.size() - 2, deRechazo.size())).allSatisfy(registro -> {
                assertThat(registro.usuarioId()).isEqualTo(REVISOR_2);
                assertThat(registro.detalle()).isEqualTo(detalleEsperado);
            });
            assertThat(aprobada.getTrazabilidad()).last().satisfies(registro -> {
                assertThat(registro.usuarioId()).isEqualTo(REVISOR_2);
                assertThat(registro.detalle()).isEqualTo(detalleEsperado);
            });
        }
    }

    @Nested
    @DisplayName("Trazabilidad e historial INV-12 a INV-14")
    class TrazabilidadEHistorial {

        @Test
        @DisplayName("INV-12: el repositorio no ofrece ningún método para borrar preguntas")
        void inv12SinBorradoFisico() {
            List<String> metodos = Arrays.stream(PreguntaRepositorio.class.getMethods())
                    .map(Method::getName)
                    .map(nombre -> nombre.toLowerCase(Locale.ROOT))
                    .toList();
            assertThat(metodos).noneMatch(nombre -> nombre.contains("borrar") || nombre.contains("eliminar")
                    || nombre.contains("delete") || nombre.contains("remove"));
        }

        @Test
        @DisplayName("INV-12: archivar conserva la pregunta, su trazabilidad y su historial")
        void inv12ArchivarConserva() {
            Pregunta pregunta = preguntaEnEstado(EstadoPregunta.PUBLICADA);

            pregunta.archivar(new MotivoDeArchivado("Contenido desactualizado"), ADMINISTRADOR, fecha(1));

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.ARCHIVADA);
            assertThat(pregunta.getContenido()).isEqualTo(contenidoValido());
            assertThat(pregunta.getTrazabilidad()).last().extracting(RegistroDeTrazabilidad::estadoNuevo)
                    .isEqualTo(EstadoPregunta.ARCHIVADA);
            PreguntaArchivada evento = (PreguntaArchivada) pregunta.extraerEventos().get(0);
            assertThat(evento.motivo()).isEqualTo("Contenido desactualizado");
            assertThat(evento.fechaArchivado()).isEqualTo(fecha(1));
        }

        @Test
        @DisplayName("INV-13: cada cambio anexa un registro con fecha y usuario, sin reescribir los anteriores")
        void inv13TrazabilidadSoloAnexado() {
            Pregunta pregunta = crearCon(contenidoIncompleto());
            List<RegistroDeTrazabilidad> antes = pregunta.getTrazabilidad();

            pregunta.modificar(AUTOR, contenidoValido(), fecha(1));
            pregunta.enviarARevision(AUTOR, fecha(2));

            List<RegistroDeTrazabilidad> despues = pregunta.getTrazabilidad();
            assertThat(despues).startsWith(antes.toArray(new RegistroDeTrazabilidad[0]));
            assertThat(despues).extracting(RegistroDeTrazabilidad::tipo).containsExactly(
                    TipoDeRegistro.CREACION, TipoDeRegistro.MODIFICACION, TipoDeRegistro.TRANSICION, TipoDeRegistro.TRANSICION);
            assertThat(despues).extracting(RegistroDeTrazabilidad::fecha).containsExactly(FECHA, fecha(1), fecha(1), fecha(2));
            assertThat(despues).allSatisfy(registro -> assertThat(registro.usuarioId()).isEqualTo(AUTOR));
            assertThat(pregunta.getFechaActualizacion()).isEqualTo(fecha(2));
        }

        @Test
        @DisplayName("INV-13: la trazabilidad expuesta no se puede alterar desde fuera")
        void inv13TrazabilidadInmutable() {
            Pregunta pregunta = preguntaEnConstruccion();
            assertThatThrownBy(() -> pregunta.getTrazabilidad().clear()).isInstanceOf(UnsupportedOperationException.class);
            assertThat(pregunta.getTrazabilidad()).isNotEmpty();
        }

        @Test
        @DisplayName("INV-14: el historial es de solo anexado y no se altera desde fuera")
        void inv14HistorialSoloAnexado() {
            HistorialDeRevisiones vacio = HistorialDeRevisiones.vacio();
            DictamenEnHistorial entrada = new DictamenEnHistorial(ProcesoDeRevisionId.generar(), Dictamen.calcular(2, 2, FECHA));

            HistorialDeRevisiones conEntrada = vacio.anexar(entrada);

            assertThat(vacio.entradas()).isEmpty();
            assertThat(conEntrada.entradas()).containsExactly(entrada);
            assertThatThrownBy(() -> conEntrada.entradas().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("INV-14: el historial se conserva íntegro tras el rechazo")
        void inv14HistorialSeConservaTrasRechazo() {
            Pregunta pregunta = preguntaEnRevision();
            DictamenEnHistorial entrada = new DictamenEnHistorial(ProcesoDeRevisionId.generar(), Dictamen.calcular(1, 2, fecha(3)));
            pregunta.registrarEnHistorial(entrada);

            pregunta.rechazar(REVISOR_1, fecha(3));

            assertThat(pregunta.getHistorialRevisiones().entradas()).containsExactly(entrada);
        }

        @Test
        @DisplayName("Solo se anexa al historial mientras la pregunta está EN_REVISION")
        void historialSoloEnRevision() {
            Pregunta pregunta = preguntaEnConstruccion();
            DictamenEnHistorial entrada = new DictamenEnHistorial(ProcesoDeRevisionId.generar(), Dictamen.calcular(2, 2, FECHA));
            lanzaConCodigo(() -> pregunta.registrarEnHistorial(entrada), "TRANSICION_NO_PERMITIDA");
        }
    }

    @Nested
    @DisplayName("Publicar y archivar (CU-08, CU-09)")
    class PublicarYArchivar {

        @Test
        @DisplayName("Publicar emite PreguntaPublicada con todos los datos de CONTRATOS 7.4")
        void publicarEmiteEventoCompleto() {
            Pregunta pregunta = preguntaEnEstado(EstadoPregunta.APROBADA);

            pregunta.publicar(ADMINISTRADOR, fecha(9));

            assertThat(pregunta.getEstado()).isEqualTo(EstadoPregunta.PUBLICADA);
            List<EventoDeDominio> eventos = pregunta.extraerEventos();
            assertThat(eventos).hasExactlyElementsOfTypes(PreguntaPublicada.class);
            PreguntaPublicada evento = (PreguntaPublicada) eventos.get(0);
            assertThat(evento.preguntaId()).isEqualTo(pregunta.getId());
            assertThat(evento.contexto()).isEqualTo(contenidoValido().contexto().texto());
            assertThat(evento.preguntaDirecta()).isEqualTo(contenidoValido().preguntaDirecta().texto());
            assertThat(evento.opciones()).extracting(PreguntaPublicada.OpcionPublicada::letra).containsExactly("A", "B", "C", "D");
            assertThat(evento.opciones()).extracting(PreguntaPublicada.OpcionPublicada::texto).containsExactly("3,0", "3,8", "4,0", "4,5");
            assertThat(evento.letraCorrecta()).isEqualTo("C");
            assertThat(evento.clasificacion()).isEqualTo(CLASIFICACION);
            assertThat(evento.nivelDificultad()).isEqualTo(NivelDeDificultad.BAJO);
            assertThat(evento.fechaPublicacion()).isEqualTo(fecha(9));
        }

        @Test
        @DisplayName("El motivo de archivado es obligatorio y tiene de 1 a 500 caracteres")
        void motivoDeArchivado() {
            assertThat(new MotivoDeArchivado("x".repeat(500)).texto()).hasSize(500);
            assertThat(new MotivoDeArchivado("  " + "x".repeat(500) + "  ").texto()).isNotBlank();
            lanzaConCodigo(() -> new MotivoDeArchivado("x".repeat(501)), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> new MotivoDeArchivado("   "), "SOLICITUD_INVALIDA");
            lanzaConCodigo(() -> new MotivoDeArchivado(null), "SOLICITUD_INVALIDA");
        }
    }
}
