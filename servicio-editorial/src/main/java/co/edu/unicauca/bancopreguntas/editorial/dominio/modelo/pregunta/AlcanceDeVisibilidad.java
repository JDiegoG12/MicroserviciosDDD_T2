package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;

/**
 * Conjunto de Preguntas que un usuario puede ver al consultar (CU-06, RNF-07; CONTRATOS.md 8.1).
 *
 * <p>Si {@code sinRestriccion} es verdadero no hay restricción (rol {@code ADMINISTRADOR}). Si no, el alcance es la
 * <strong>unión</strong> de las partes presentes (D-08: un usuario puede tener varios roles):</p>
 * <ul>
 *   <li>{@code autorPropio}: las Preguntas de ese Autor, en cualquier estado (rol {@code AUTOR});</li>
 *   <li>{@code revisorAsignado}: las Preguntas de los Procesos {@code ABIERTO} donde ese Revisor está asignado
 *       (rol {@code REVISOR});</li>
 *   <li>{@code publicadas}: todas las Preguntas {@code PUBLICADA} (rol {@code DOCENTE}).</li>
 * </ul>
 * <p>Los filtros de la consulta se aplican sobre esa unión, y la paginación sobre el resultado combinado.</p>
 *
 * @param sinRestriccion  sin restricción (rol ADMINISTRADOR)
 * @param autorPropio     Autor cuyas Preguntas se incluyen, o {@code null}
 * @param revisorAsignado Revisor cuyas Preguntas asignadas se incluyen, o {@code null}
 * @param publicadas      si se incluyen todas las Preguntas {@code PUBLICADA}
 */
public record AlcanceDeVisibilidad(boolean sinRestriccion, UsuarioId autorPropio, UsuarioId revisorAsignado, boolean publicadas) {

    /**
     * Alcance sin restricción.
     *
     * @return alcance total
     */
    public static AlcanceDeVisibilidad todas() {
        return new AlcanceDeVisibilidad(true, null, null, false);
    }

    /**
     * Alcance vacío, al que se le van uniendo partes.
     *
     * @return alcance que no incluye ninguna Pregunta
     */
    public static AlcanceDeVisibilidad ninguna() {
        return new AlcanceDeVisibilidad(false, null, null, false);
    }

    /**
     * Une las Preguntas propias de un Autor.
     *
     * @param autor Autor
     * @return alcance nuevo
     */
    public AlcanceDeVisibilidad conPropiasDe(UsuarioId autor) {
        return new AlcanceDeVisibilidad(sinRestriccion, autor, revisorAsignado, publicadas);
    }

    /**
     * Une las Preguntas asignadas a un Revisor en Procesos abiertos.
     *
     * @param revisor Revisor
     * @return alcance nuevo
     */
    public AlcanceDeVisibilidad conAsignadasA(UsuarioId revisor) {
        return new AlcanceDeVisibilidad(sinRestriccion, autorPropio, revisor, publicadas);
    }

    /**
     * Une todas las Preguntas publicadas.
     *
     * @return alcance nuevo
     */
    public AlcanceDeVisibilidad conPublicadas() {
        return new AlcanceDeVisibilidad(sinRestriccion, autorPropio, revisorAsignado, true);
    }

    /**
     * Indica si el alcance no incluye ninguna Pregunta.
     *
     * @return {@code true} si no hay ninguna parte
     */
    public boolean estaVacio() {
        return !sinRestriccion && autorPropio == null && revisorAsignado == null && !publicadas;
    }
}
