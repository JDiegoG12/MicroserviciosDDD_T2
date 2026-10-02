package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

/**
 * Valoración de un criterio dentro del Formato de evaluación (CU-11; CONTRATOS.md 8.1).
 *
 * @param criterio   {@code PEDAGOGICO}, {@code TECNICO} o {@code ESTRUCTURAL}
 * @param valoracion entero de 1 a 5
 */
public record CriterioEvaluado(TipoCriterio criterio, int valoracion) {

    /** Valoración mínima permitida. */
    public static final int VALORACION_MINIMA = 1;

    /** Valoración máxima permitida. */
    public static final int VALORACION_MAXIMA = 5;

    /**
     * Valida el criterio y el rango de la valoración.
     *
     * @throws DatoInvalidoExcepcion si el criterio falta o la valoración está fuera de 1..5
     */
    public CriterioEvaluado {
        Validaciones.requerirNoNulo(criterio, "criterios[].criterio");
        // DUDA: CONTRATOS 8.1 no asigna código a una valoración fuera de rango; se usa SOLICITUD_INVALIDA (400).
        if (valoracion < VALORACION_MINIMA || valoracion > VALORACION_MAXIMA) {
            throw new DatoInvalidoExcepcion("La valoración del criterio " + criterio + " debe estar entre "
                    + VALORACION_MINIMA + " y " + VALORACION_MAXIMA + ".");
        }
    }
}
