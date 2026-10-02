package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Comentario que un Revisor deja en su Formato de evaluación (lenguaje ubicuo 2.5, RF-18).
 *
 * @param texto contenido de la observación; nunca nulo
 */
public record Observacion(String texto) {

    /**
     * Valida que el texto no sea nulo.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si es nulo
     */
    public Observacion {
        Validaciones.requerirNoNulo(texto, "observaciones[]");
    }
}
