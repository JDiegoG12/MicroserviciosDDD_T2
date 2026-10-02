package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Enunciado puntual y único que formula lo que se pregunta (lenguaje ubicuo 2.2, INV-03).
 *
 * <p>La Pregunta guarda exactamente un value object de este tipo, así que INV-03 ("una y solo una
 * Pregunta directa") se cumple por construcción. Que no esté vacía (RF-09) lo mide la validación
 * estructural.</p>
 *
 * @param texto enunciado; nunca nulo
 */
public record PreguntaDirecta(String texto) {

    /**
     * Valida que el texto no sea nulo.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public PreguntaDirecta {
        Validaciones.requerirNoNulo(texto, "preguntaDirecta");
    }
}
