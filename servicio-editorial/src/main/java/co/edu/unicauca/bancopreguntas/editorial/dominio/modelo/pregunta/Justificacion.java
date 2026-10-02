package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Explicación de por qué la Respuesta correcta es la solución válida (lenguaje ubicuo 2.2).
 *
 * <p>Es obligatoria para salir de {@code BORRADOR} (INV-04); la comprobación la hace la validación
 * estructural.</p>
 *
 * @param texto contenido de la justificación; nunca nulo
 */
public record Justificacion(String texto) {

    /**
     * Valida que el texto no sea nulo.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public Justificacion {
        Validaciones.requerirNoNulo(texto, "justificacion");
    }
}
