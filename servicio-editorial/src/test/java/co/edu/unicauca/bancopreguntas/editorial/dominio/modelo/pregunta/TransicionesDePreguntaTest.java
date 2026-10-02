package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.EstadoPregunta.*;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.Afirmaciones.lanzaConCodigo;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Pruebas de la máquina de estados de la Pregunta: el diagrama de CONTRATOS.md 11.1 (INV-09, RF-15).
 */
@DisplayName("Tabla de transiciones de la Pregunta (INV-09)")
class TransicionesDePreguntaTest {

    /** Cada transición del diagrama, con la operación del lenguaje ubicuo que la produce. */
    static Stream<Arguments> transicionesValidas() {
        return Stream.of(
                arguments(BORRADOR, EN_CONSTRUCCION, "modificar con contenido completo",
                        (Consumer<Pregunta>) p -> p.modificar(AUTOR, contenidoValido(), fecha(1))),
                arguments(EN_CONSTRUCCION, BORRADOR, "modificar dejándola incompleta",
                        (Consumer<Pregunta>) p -> p.modificar(AUTOR, contenidoIncompleto(), fecha(1))),
                arguments(EN_CONSTRUCCION, PENDIENTE_REVISION, "enviarARevision",
                        (Consumer<Pregunta>) p -> p.enviarARevision(AUTOR, fecha(1))),
                arguments(PENDIENTE_REVISION, EN_REVISION, "iniciarRevision",
                        (Consumer<Pregunta>) p -> p.iniciarRevision(ADMINISTRADOR, fecha(1))),
                arguments(EN_REVISION, APROBADA, "aprobar",
                        (Consumer<Pregunta>) p -> p.aprobar(REVISOR_1, fecha(1))),
                arguments(APROBADA, PUBLICADA, "publicar",
                        (Consumer<Pregunta>) p -> p.publicar(ADMINISTRADOR, fecha(1))),
                arguments(PUBLICADA, ARCHIVADA, "archivar",
                        (Consumer<Pregunta>) p -> p.archivar(new MotivoDeArchivado("Desactualizada"), ADMINISTRADOR, fecha(1))));
    }

    @ParameterizedTest(name = "{0} → {1} con {2}")
    @MethodSource("transicionesValidas")
    @DisplayName("Cada transición válida funciona y queda en la trazabilidad")
    void transicionValidaFunciona(EstadoPregunta origen, EstadoPregunta destino, String operacion,
                                  Consumer<Pregunta> accion) {
        Pregunta pregunta = preguntaEnEstado(origen);

        accion.accept(pregunta);

        assertThat(pregunta.getEstado()).isEqualTo(destino);
        RegistroDeTrazabilidad ultimo = pregunta.getTrazabilidad().get(pregunta.getTrazabilidad().size() - 1);
        assertThat(ultimo.tipo()).isEqualTo(TipoDeRegistro.TRANSICION);
        assertThat(ultimo.estadoAnterior()).isEqualTo(origen);
        assertThat(ultimo.estadoNuevo()).isEqualTo(destino);
    }

    @Test
    @DisplayName("EN_REVISION → RECHAZADA → EN_CONSTRUCCION con rechazar (D-07)")
    void rechazoPasaPorRechazada() {
        Pregunta pregunta = preguntaEnEstado(EN_REVISION);
        pregunta.rechazar(REVISOR_1, fecha(1));
        assertThat(pregunta.getTrazabilidad()).extracting(RegistroDeTrazabilidad::estadoNuevo)
                .containsExactly(RECHAZADA, EN_CONSTRUCCION);
        assertThat(pregunta.getEstado()).isEqualTo(EN_CONSTRUCCION);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "BORRADOR, EN_CONSTRUCCION", "EN_CONSTRUCCION, BORRADOR", "EN_CONSTRUCCION, PENDIENTE_REVISION",
            "PENDIENTE_REVISION, EN_REVISION", "EN_REVISION, APROBADA", "EN_REVISION, RECHAZADA",
            "RECHAZADA, EN_CONSTRUCCION", "APROBADA, PUBLICADA", "PUBLICADA, ARCHIVADA"})
    @DisplayName("La tabla declara las flechas del diagrama")
    void laTablaDeclaraLasFlechas(EstadoPregunta origen, EstadoPregunta destino) {
        assertThat(TransicionesDePregunta.estaPermitida(origen, destino)).isTrue();
    }

    @Test
    @DisplayName("La tabla no declara ninguna otra flecha")
    void laTablaNoTieneOtrasFlechas() {
        long totalDeFlechas = Arrays.stream(EstadoPregunta.values())
                .mapToLong(origen -> TransicionesDePregunta.destinosDesde(origen).size())
                .sum();
        assertThat(totalDeFlechas).isEqualTo(9);
        assertThat(TransicionesDePregunta.destinosDesde(ARCHIVADA)).isEmpty();
    }

    /** Muestra representativa de transiciones inválidas, intentadas con la operación correspondiente. */
    static Stream<Arguments> transicionesInvalidas() {
        return Stream.of(
                arguments(BORRADOR, "enviarARevision", (Consumer<Pregunta>) p -> p.enviarARevision(AUTOR, fecha(1))),
                arguments(EN_CONSTRUCCION, "iniciarRevision", (Consumer<Pregunta>) p -> p.iniciarRevision(ADMINISTRADOR, fecha(1))),
                arguments(EN_CONSTRUCCION, "publicar", (Consumer<Pregunta>) p -> p.publicar(ADMINISTRADOR, fecha(1))),
                arguments(PENDIENTE_REVISION, "aprobar", (Consumer<Pregunta>) p -> p.aprobar(REVISOR_1, fecha(1))),
                arguments(PENDIENTE_REVISION, "enviarARevision", (Consumer<Pregunta>) p -> p.enviarARevision(AUTOR, fecha(1))),
                arguments(EN_REVISION, "publicar", (Consumer<Pregunta>) p -> p.publicar(ADMINISTRADOR, fecha(1))),
                arguments(APROBADA, "archivar", (Consumer<Pregunta>) p -> p.archivar(new MotivoDeArchivado("X"), ADMINISTRADOR, fecha(1))),
                arguments(APROBADA, "rechazar", (Consumer<Pregunta>) p -> p.rechazar(REVISOR_1, fecha(1))),
                arguments(PUBLICADA, "iniciarRevision", (Consumer<Pregunta>) p -> p.iniciarRevision(ADMINISTRADOR, fecha(1))),
                arguments(PUBLICADA, "aprobar", (Consumer<Pregunta>) p -> p.aprobar(REVISOR_1, fecha(1))),
                arguments(ARCHIVADA, "publicar", (Consumer<Pregunta>) p -> p.publicar(ADMINISTRADOR, fecha(1))),
                arguments(ARCHIVADA, "archivar", (Consumer<Pregunta>) p -> p.archivar(new MotivoDeArchivado("X"), ADMINISTRADOR, fecha(1))));
    }

    @ParameterizedTest(name = "{1} desde {0}")
    @MethodSource("transicionesInvalidas")
    @DisplayName("Una transición no declarada lanza TRANSICION_NO_PERMITIDA y no cambia el estado")
    void transicionInvalidaSeRechaza(EstadoPregunta origen, String operacion, Consumer<Pregunta> accion) {
        Pregunta pregunta = preguntaEnEstado(origen);

        lanzaConCodigo(() -> accion.accept(pregunta), "TRANSICION_NO_PERMITIDA");

        assertThat(pregunta.getEstado()).isEqualTo(origen);
        assertThat(pregunta.getTrazabilidad()).isEmpty();
    }
}
