package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.List;

/**
 * Referencias a las fuentes que sustentan la Pregunta (lenguaje ubicuo 2.2, RF-05).
 *
 * <p>Para salir de {@code BORRADOR} debe tener al menos una entrada no vacía (INV-04); la
 * comprobación la hace la validación estructural.</p>
 *
 * @param referencias lista inmutable de referencias; nunca nula
 */
public record Bibliografia(List<String> referencias) {

    /**
     * Valida y copia la lista de referencias.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si la lista o una referencia es nula
     */
    public Bibliografia {
        Validaciones.requerirNoNulo(referencias, "bibliografia");
        referencias.forEach(referencia -> Validaciones.requerirNoNulo(referencia, "bibliografia[]"));
        referencias = List.copyOf(referencias);
    }

    /**
     * Indica si hay al menos una referencia con contenido.
     *
     * @return {@code true} si alguna referencia no está en blanco
     */
    public boolean tieneAlgunaReferencia() {
        return referencias.stream().anyMatch(referencia -> !referencia.isBlank());
    }
}
