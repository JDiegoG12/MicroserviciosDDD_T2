package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Grado de complejidad de la Pregunta: enumeración cerrada (D-10, INV-08).
 */
public enum NivelDeDificultad {
    /** Dificultad baja. */
    BAJO,
    /** Dificultad media. */
    MEDIO,
    /** Dificultad alta. */
    ALTO;

    /**
     * Convierte el nombre exacto del contrato en el valor del enum.
     *
     * @param nombre {@code BAJO}, {@code MEDIO} o {@code ALTO}
     * @return el nivel correspondiente
     * @throws DatoInvalidoExcepcion si el nombre es nulo o no pertenece al conjunto cerrado (INV-08)
     */
    public static NivelDeDificultad desdeNombre(String nombre) {
        Validaciones.requerirNoNulo(nombre, "nivelDificultad");
        try {
            return valueOf(nombre);
        } catch (IllegalArgumentException error) {
            throw new DatoInvalidoExcepcion("INV-08: el nivel de dificultad debe ser BAJO, MEDIO o ALTO, no " + nombre + ".");
        }
    }
}
