package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Cada una de las cuatro alternativas de la Pregunta: tres Distractores y una Respuesta correcta
 * (D-01, lenguaje ubicuo 2.2).
 *
 * <p>Si hay que corregirla, se reemplaza por un value object nuevo (Taller 1, sección 6). La letra y
 * la longitud del texto las revisan RF-10 y RF-13 en la validación estructural.</p>
 *
 * @param letra      letra de la opción ({@code "A"} a {@code "D"}); nunca nula
 * @param texto      texto de la opción; nunca nulo
 * @param esCorrecta {@code true} si es la Respuesta correcta
 */
public record OpcionDeRespuesta(String letra, String texto, boolean esCorrecta) {

    /**
     * Valida que la letra y el texto no sean nulos.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si alguno es nulo
     */
    public OpcionDeRespuesta {
        Validaciones.requerirNoNulo(letra, "opciones[].letra");
        Validaciones.requerirNoNulo(texto, "opciones[].texto");
    }
}
