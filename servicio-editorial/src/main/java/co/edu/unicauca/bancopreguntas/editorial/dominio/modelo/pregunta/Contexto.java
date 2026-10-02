package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Texto o situación al inicio de la Pregunta que da la información para responder (lenguaje ubicuo 2.2).
 *
 * <p>Puede estar vacío mientras la Pregunta sea {@code BORRADOR} (D-02); la regla RF-08 la mide
 * {@link co.edu.unicauca.bancopreguntas.editorial.dominio.servicios.ValidacionEstructural}.</p>
 *
 * @param texto contenido del contexto; nunca nulo
 */
public record Contexto(String texto) {

    /**
     * Valida que el texto no sea nulo.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public Contexto {
        Validaciones.requerirNoNulo(texto, "contexto");
    }
}
