package co.edu.unicauca.bancopreguntas.editorial.dominio.servicios;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.LongitudDeTexto;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.ContenidoDePregunta;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta.OpcionDeRespuesta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Validación estructural de la Pregunta: RF-08 a RF-13 e INV-04, con los valores exactos de la tabla
 * de CONTRATOS.md 11.1. Aplica las invariantes INV-01 a INV-06 tal como indica MODELO-DOMINIO A.2.
 *
 * <p>No lanza excepciones: devuelve la lista de reglas incumplidas. Una Pregunta en {@code BORRADOR}
 * puede incumplirlas, pero nunca sale de {@code BORRADOR} sin cumplirlas (INV-11).</p>
 */
public final class ValidacionEstructural {

    /** RF-08: longitud máxima del Contexto. */
    public static final int LONGITUD_MAXIMA_CONTEXTO = 2000;

    /** RF-09: longitud máxima de la Pregunta directa. */
    public static final int LONGITUD_MAXIMA_PREGUNTA_DIRECTA = 500;

    /** RF-10 / D-01: cantidad exacta de Opciones de respuesta. */
    public static final int CANTIDAD_OPCIONES = 4;

    /** RF-10 / D-01: letras válidas, sin repetir. */
    public static final Set<String> LETRAS_VALIDAS = Set.of("A", "B", "C", "D");

    /** RF-11: cantidad exacta de opciones marcadas como correctas. */
    public static final int CANTIDAD_RESPUESTAS_CORRECTAS = 1;

    /** RF-12: frases prohibidas, ya escritas en minúsculas y sin tildes. */
    public static final List<String> FRASES_PROHIBIDAS = List.of(
            "todas las anteriores",
            "ninguna de las anteriores",
            "todas las opciones anteriores",
            "ninguna de las opciones anteriores");

    /** RF-13: longitud mínima del texto de cada opción. */
    public static final int LONGITUD_MINIMA_OPCION = 1;

    /** RF-13: longitud máxima del texto de cada opción. */
    public static final int LONGITUD_MAXIMA_OPCION = 300;

    /** RF-13: un distractor mide al menos esta fracción de la Respuesta correcta. */
    public static final double FACTOR_MINIMO_DISTRACTOR = 0.5;

    /** RF-13: un distractor mide como máximo este múltiplo de la Respuesta correcta. */
    public static final double FACTOR_MAXIMO_DISTRACTOR = 2.0;

    private ValidacionEstructural() {
    }

    /**
     * Ejecuta todas las reglas de la validación estructural.
     *
     * @param contenido componentes de la Pregunta
     * @return reglas incumplidas; vacía si la Pregunta supera la validación
     */
    public static List<ErrorDeValidacion> validar(ContenidoDePregunta contenido) {
        List<ErrorDeValidacion> errores = new ArrayList<>();
        validarContexto(contenido.contexto().texto(), errores);
        validarPreguntaDirecta(contenido.preguntaDirecta().texto(), errores);
        validarCantidadYLetras(contenido.opciones(), errores);
        validarUnicaCorrecta(contenido.opciones(), errores);
        validarFrasesProhibidas(contenido.opciones(), errores);
        validarFormaDeOpciones(contenido.opciones(), errores);
        validarProporcionDeDistractores(contenido, errores);
        validarJustificacionYBibliografia(contenido, errores);
        return List.copyOf(errores);
    }

    /**
     * Indica si el contenido supera íntegramente la validación (INV-11).
     *
     * @param contenido componentes de la Pregunta
     * @return {@code true} si no incumple ninguna regla
     */
    public static boolean esSuperada(ContenidoDePregunta contenido) {
        return validar(contenido).isEmpty();
    }

    // RF-08 (INV-04): contexto no vacío sin contar espacios y como máximo 2000 caracteres.
    private static void validarContexto(String contexto, List<ErrorDeValidacion> errores) {
        if (contexto.isBlank()) {
            errores.add(new ErrorDeValidacion("RF-08", "El contexto es obligatorio."));
        } else if (longitud(contexto) > LONGITUD_MAXIMA_CONTEXTO) {
            errores.add(new ErrorDeValidacion("RF-08",
                    "El contexto no puede superar " + LONGITUD_MAXIMA_CONTEXTO + " caracteres."));
        }
    }

    // RF-09 (INV-03): pregunta directa no vacía y como máximo 500 caracteres.
    private static void validarPreguntaDirecta(String preguntaDirecta, List<ErrorDeValidacion> errores) {
        if (preguntaDirecta.isBlank()) {
            errores.add(new ErrorDeValidacion("RF-09", "La pregunta directa es obligatoria."));
        } else if (longitud(preguntaDirecta) > LONGITUD_MAXIMA_PREGUNTA_DIRECTA) {
            errores.add(new ErrorDeValidacion("RF-09",
                    "La pregunta directa no puede superar " + LONGITUD_MAXIMA_PREGUNTA_DIRECTA + " caracteres."));
        }
    }

    // RF-10 / D-01 (INV-01): exactamente cuatro opciones con letras A–D sin repetir.
    private static void validarCantidadYLetras(List<OpcionDeRespuesta> opciones, List<ErrorDeValidacion> errores) {
        if (opciones.size() != CANTIDAD_OPCIONES) {
            errores.add(new ErrorDeValidacion("RF-10",
                    "Debe haber exactamente cuatro opciones; hay " + opciones.size() + "."));
            return;
        }
        Set<String> letras = new HashSet<>();
        opciones.forEach(opcion -> letras.add(opcion.letra()));
        if (!letras.equals(LETRAS_VALIDAS)) {
            errores.add(new ErrorDeValidacion("RF-10", "Las opciones deben usar las letras A, B, C y D sin repetir."));
        }
    }

    // RF-11 (INV-02): exactamente una opción con esCorrecta = true.
    private static void validarUnicaCorrecta(List<OpcionDeRespuesta> opciones, List<ErrorDeValidacion> errores) {
        long correctas = opciones.stream().filter(OpcionDeRespuesta::esCorrecta).count();
        if (correctas != CANTIDAD_RESPUESTAS_CORRECTAS) {
            errores.add(new ErrorDeValidacion("RF-11",
                    "Debe haber exactamente una respuesta correcta; hay " + correctas + "."));
        }
    }

    // RF-12 (INV-05): ninguna opción usa las frases prohibidas, comparando en minúsculas y sin tildes.
    private static void validarFrasesProhibidas(List<OpcionDeRespuesta> opciones, List<ErrorDeValidacion> errores) {
        for (OpcionDeRespuesta opcion : opciones) {
            String normalizado = normalizar(opcion.texto());
            FRASES_PROHIBIDAS.stream()
                    .filter(normalizado::contains)
                    .findFirst()
                    .ifPresent(frase -> errores.add(new ErrorDeValidacion("RF-12",
                            "La opción " + opcion.letra() + " usa la expresión prohibida \"" + frase + "\".")));
        }
    }

    // RF-13 (INV-06): cada opción tiene de 1 a 300 caracteres y empieza por mayúscula o dígito.
    private static void validarFormaDeOpciones(List<OpcionDeRespuesta> opciones, List<ErrorDeValidacion> errores) {
        for (OpcionDeRespuesta opcion : opciones) {
            int longitud = longitud(opcion.texto());
            if (longitud < LONGITUD_MINIMA_OPCION || longitud > LONGITUD_MAXIMA_OPCION) {
                errores.add(new ErrorDeValidacion("RF-13", "La opción " + opcion.letra() + " debe tener entre "
                        + LONGITUD_MINIMA_OPCION + " y " + LONGITUD_MAXIMA_OPCION + " caracteres."));
            } else if (!empiezaPorMayusculaODigito(opcion.texto())) {
                errores.add(new ErrorDeValidacion("RF-13",
                        "La opción " + opcion.letra() + " debe empezar por mayúscula o dígito."));
            }
        }
    }

    // RF-13 (INV-06): cada distractor mide entre 0,5 y 2 veces la longitud de la respuesta correcta.
    // Solo se mide cuando hay exactamente una respuesta correcta con texto; si no, ya falla RF-11 o RF-13.
    private static void validarProporcionDeDistractores(ContenidoDePregunta contenido, List<ErrorDeValidacion> errores) {
        Optional<OpcionDeRespuesta> correcta = contenido.respuestaCorrecta();
        if (correcta.isEmpty() || longitud(correcta.get().texto()) == 0) {
            return;
        }
        int longitudCorrecta = longitud(correcta.get().texto());
        for (OpcionDeRespuesta opcion : contenido.opciones()) {
            if (opcion.esCorrecta()) {
                continue;
            }
            int longitudDistractor = longitud(opcion.texto());
            boolean muyCorto = longitudDistractor < FACTOR_MINIMO_DISTRACTOR * longitudCorrecta;
            boolean muyLargo = longitudDistractor > FACTOR_MAXIMO_DISTRACTOR * longitudCorrecta;
            if (muyCorto || muyLargo) {
                errores.add(new ErrorDeValidacion("RF-13", "La longitud del distractor " + opcion.letra()
                        + " debe estar entre 0,5 y 2 veces la de la respuesta correcta."));
            }
        }
    }

    // INV-04: justificación no vacía y al menos una entrada en la bibliografía.
    private static void validarJustificacionYBibliografia(ContenidoDePregunta contenido, List<ErrorDeValidacion> errores) {
        if (contenido.justificacion().texto().isBlank()) {
            errores.add(new ErrorDeValidacion("INV-04", "La justificación es obligatoria."));
        }
        if (!contenido.bibliografia().tieneAlgunaReferencia()) {
            errores.add(new ErrorDeValidacion("INV-04", "La bibliografía debe tener al menos una referencia."));
        }
    }

    /**
     * Normaliza un texto para comparar frases: minúsculas, sin tildes y con los espacios colapsados (RF-12).
     *
     * @param texto texto original
     * @return texto normalizado
     */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    // CONTRATOS.md 4 ("Longitud de textos"): puntos de código Unicode, sin los espacios del inicio y del final.
    private static int longitud(String texto) {
        return LongitudDeTexto.medir(texto);
    }

    // RF-13: el primer carácter se evalúa sobre el texto sin los espacios del inicio (CONTRATOS.md 4).
    private static boolean empiezaPorMayusculaODigito(String texto) {
        int primero = texto.strip().codePointAt(0);
        return Character.isUpperCase(primero) || Character.isDigit(primero);
    }
}
