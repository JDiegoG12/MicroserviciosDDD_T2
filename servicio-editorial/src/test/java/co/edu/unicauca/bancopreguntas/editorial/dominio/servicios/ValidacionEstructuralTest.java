package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.OpcionDeRespuesta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.contenido;
import static co.edu.unicauca.bancopreguntas.editorial.fabricas.DatosDePrueba.contenidoValido;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la validación estructural: RF-08 a RF-13 e INV-04, con los valores exactos de CONTRATOS.md 11.1.
 */
@DisplayName("ValidacionEstructural (RF-08 a RF-13, INV-04)")
class ValidacionEstructuralTest {

    private static List<String> reglasIncumplidas(ContenidoDePregunta contenido) {
        return ValidacionEstructural.validar(contenido).stream().map(ErrorDeValidacion::regla).toList();
    }

    private static List<OpcionDeRespuesta> opciones(String a, String b, String c, String d) {
        return List.of(
                new OpcionDeRespuesta("A", a, false),
                new OpcionDeRespuesta("B", b, false),
                new OpcionDeRespuesta("C", c, true),
                new OpcionDeRespuesta("D", d, false));
    }

    @Test
    @DisplayName("Una pregunta completa supera la validación")
    void preguntaCompletaSuperaLaValidacion() {
        assertThat(ValidacionEstructural.validar(contenidoValido())).isEmpty();
        assertThat(ValidacionEstructural.esSuperada(contenidoValido())).isTrue();
    }

    @Nested
    @DisplayName("RF-08 contexto")
    class Contexto {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\n\t"})
        @DisplayName("Vacío o solo espacios incumple RF-08")
        void vacioIncumple(String texto) {
            assertThat(reglasIncumplidas(contenido().conContexto(texto).construir())).containsExactly("RF-08");
        }

        @Test
        @DisplayName("2000 caracteres cumple y 2001 incumple RF-08")
        void limiteDeLongitud() {
            assertThat(reglasIncumplidas(contenido().conContexto("x".repeat(2000)).construir())).isEmpty();
            assertThat(reglasIncumplidas(contenido().conContexto("x".repeat(2001)).construir())).containsExactly("RF-08");
        }
    }

    @Nested
    @DisplayName("RF-09 pregunta directa (INV-03)")
    class PreguntaDirecta {

        @Test
        @DisplayName("Vacía incumple RF-09")
        void vaciaIncumple() {
            assertThat(reglasIncumplidas(contenido().conPreguntaDirecta(" ").construir())).containsExactly("RF-09");
        }

        @Test
        @DisplayName("500 caracteres cumple y 501 incumple RF-09")
        void limiteDeLongitud() {
            assertThat(reglasIncumplidas(contenido().conPreguntaDirecta("x".repeat(500)).construir())).isEmpty();
            assertThat(reglasIncumplidas(contenido().conPreguntaDirecta("x".repeat(501)).construir())).containsExactly("RF-09");
        }
    }

    @Nested
    @DisplayName("RF-10 / D-01 cuatro opciones A–D (INV-01)")
    class CantidadDeOpciones {

        @Test
        @DisplayName("Tres opciones incumplen RF-10")
        void tresOpcionesIncumplen() {
            List<OpcionDeRespuesta> tres = List.of(
                    new OpcionDeRespuesta("A", "3,0", false),
                    new OpcionDeRespuesta("B", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", true));
            assertThat(reglasIncumplidas(contenido().conOpciones(tres).construir())).containsExactly("RF-10");
        }

        @Test
        @DisplayName("Cinco opciones incumplen RF-10")
        void cincoOpcionesIncumplen() {
            List<OpcionDeRespuesta> cinco = new java.util.ArrayList<>(opciones("3,0", "3,8", "4,0", "4,5"));
            cinco.add(new OpcionDeRespuesta("E", "5,0", false));
            assertThat(reglasIncumplidas(contenido().conOpciones(cinco).construir())).containsExactly("RF-10");
        }

        @Test
        @DisplayName("Letras repetidas incumplen RF-10")
        void letrasRepetidasIncumplen() {
            List<OpcionDeRespuesta> repetidas = List.of(
                    new OpcionDeRespuesta("A", "3,0", false),
                    new OpcionDeRespuesta("A", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", true),
                    new OpcionDeRespuesta("D", "4,5", false));
            assertThat(reglasIncumplidas(contenido().conOpciones(repetidas).construir())).containsExactly("RF-10");
        }

        @Test
        @DisplayName("Una letra fuera de A–D incumple RF-10")
        void letraFueraDeRangoIncumple() {
            List<OpcionDeRespuesta> conE = List.of(
                    new OpcionDeRespuesta("A", "3,0", false),
                    new OpcionDeRespuesta("B", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", true),
                    new OpcionDeRespuesta("E", "4,5", false));
            assertThat(reglasIncumplidas(contenido().conOpciones(conE).construir())).containsExactly("RF-10");
        }
    }

    @Nested
    @DisplayName("RF-11 exactamente una correcta (INV-02)")
    class UnicaCorrecta {

        @Test
        @DisplayName("Ninguna correcta incumple RF-11")
        void ningunaCorrecta() {
            List<OpcionDeRespuesta> sinCorrecta = List.of(
                    new OpcionDeRespuesta("A", "3,0", false),
                    new OpcionDeRespuesta("B", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", false),
                    new OpcionDeRespuesta("D", "4,5", false));
            assertThat(reglasIncumplidas(contenido().conOpciones(sinCorrecta).construir())).containsExactly("RF-11");
        }

        @Test
        @DisplayName("Dos correctas incumplen RF-11")
        void dosCorrectas() {
            List<OpcionDeRespuesta> dosCorrectas = List.of(
                    new OpcionDeRespuesta("A", "3,0", true),
                    new OpcionDeRespuesta("B", "3,8", false),
                    new OpcionDeRespuesta("C", "4,0", true),
                    new OpcionDeRespuesta("D", "4,5", false));
            assertThat(reglasIncumplidas(contenido().conOpciones(dosCorrectas).construir())).containsExactly("RF-11");
        }
    }

    @Nested
    @DisplayName("RF-12 frases prohibidas (INV-05)")
    class FrasesProhibidas {

        @ParameterizedTest
        @ValueSource(strings = {
                "Todas las anteriores",
                "Ninguna de las anteriores",
                "Todas las opciones anteriores",
                "Ninguna de las opciones anteriores",
                "NINGUNA DE LAS ANTERIÓRES",
                "Ningúna  de   las anteriores",
                "Creo que todas las anteriores"})
        @DisplayName("Se detectan sin importar mayúsculas, tildes ni espacios repetidos")
        void seDetectanSinMayusculasNiTildes(String textoProhibido) {
            // La opción prohibida tiene una longitud parecida a la correcta para no disparar la proporción de RF-13.
            ContenidoDePregunta contenido = contenido()
                    .conOpciones(opciones(textoProhibido, "Valor intermedio ok", "Valor correcto exacto", "Valor del extremo"))
                    .construir();
            assertThat(reglasIncumplidas(contenido)).contains("RF-12");
        }

        @Test
        @DisplayName("Un texto parecido pero permitido no incumple RF-12")
        void textoPermitido() {
            ContenidoDePregunta contenido = contenido()
                    .conOpciones(opciones("Todas son pares", "Ninguna es impar", "Algunas son primas", "Las dos primeras"))
                    .construir();
            assertThat(reglasIncumplidas(contenido)).doesNotContain("RF-12");
        }

        @Test
        @DisplayName("La normalización quita tildes y pasa a minúsculas")
        void normalizacion() {
            assertThat(ValidacionEstructural.normalizar("  NINGÚNA   de las ANTERIÓRES ")).isEqualTo("ninguna de las anteriores");
        }
    }

    @Nested
    @DisplayName("RF-13 forma y longitud de las opciones (INV-06)")
    class FormaDeOpciones {

        @Test
        @DisplayName("Una opción vacía incumple RF-13")
        void opcionVacia() {
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("", "3,8", "4,0", "4,5")).construir()))
                    .contains("RF-13");
        }

        @Test
        @DisplayName("300 caracteres cumple y 301 incumple RF-13")
        void limiteDeLongitud() {
            String largo300 = "X".repeat(300);
            String largo301 = "X".repeat(301);
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones(largo300, largo300, largo300, largo300)).construir()))
                    .isEmpty();
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones(largo301, largo300, largo300, largo300)).construir()))
                    .containsExactly("RF-13");
        }

        @Test
        @DisplayName("Empezar por minúscula incumple RF-13; por mayúscula con tilde o dígito cumple")
        void empiezaPorMayusculaODigito() {
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("media", "Moda", "Árbol", "4,5")).construir()))
                    .containsExactly("RF-13");
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("Media", "Moda", "Árbol", "4,5")).construir()))
                    .isEmpty();
        }

        @Test
        @DisplayName("Un distractor de menos de la mitad de la correcta incumple RF-13")
        void distractorMuyCorto() {
            // Correcta de 10 caracteres: el mínimo es 5.
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("Cinco", "Seis 6", "Diez letra", "Siete 7")).construir()))
                    .isEmpty();
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("Cua4", "Seis 6", "Diez letra", "Siete 7")).construir()))
                    .containsExactly("RF-13");
        }

        @Test
        @DisplayName("Un distractor de más del doble de la correcta incumple RF-13")
        void distractorMuyLargo() {
            // Correcta de 5 caracteres: el máximo es 10.
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("Diez letra", "Seis 6", "Cinco", "Siete 7")).construir()))
                    .isEmpty();
            assertThat(reglasIncumplidas(contenido().conOpciones(opciones("Once letras", "Seis 6", "Cinco", "Siete 7")).construir()))
                    .containsExactly("RF-13");
        }
    }

    @Nested
    @DisplayName("INV-04 justificación y bibliografía")
    class JustificacionYBibliografia {

        @Test
        @DisplayName("Justificación vacía incumple INV-04")
        void justificacionVacia() {
            assertThat(reglasIncumplidas(contenido().conJustificacion("  ").construir())).containsExactly("INV-04");
        }

        @Test
        @DisplayName("Bibliografía sin entradas o en blanco incumple INV-04")
        void bibliografiaVacia() {
            assertThat(reglasIncumplidas(contenido().conBibliografia(List.of()).construir())).containsExactly("INV-04");
            assertThat(reglasIncumplidas(contenido().conBibliografia(List.of(" ")).construir())).containsExactly("INV-04");
        }
    }

    @Test
    @DisplayName("Se informan todas las reglas incumplidas a la vez")
    void seInformanTodasLasReglas() {
        ContenidoDePregunta vacio = contenido().conContexto("").conPreguntaDirecta("").conOpciones(List.of())
                .conJustificacion("").conBibliografia(List.of()).construir();
        assertThat(reglasIncumplidas(vacio)).contains("RF-08", "RF-09", "RF-10", "RF-11", "INV-04");
    }
}
