package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.UsuarioId;
import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.time.Instant;

/**
 * Vínculo entre un Revisor y el Proceso de revisión; dentro del Proceso se distingue por el
 * {@code revisorId} (Taller 1, sección 6).
 *
 * @param revisorId       identificador del Revisor
 * @param fechaAsignacion instante UTC de la asignación
 */
public record AsignacionDeRevisor(UsuarioId revisorId, Instant fechaAsignacion) {

    /**
     * Exige los dos datos de la asignación.
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta alguno
     */
    public AsignacionDeRevisor {
        Validaciones.requerirNoNulo(revisorId, "revisorId");
        Validaciones.requerirNoNulo(fechaAsignacion, "fechaAsignacion");
    }
}
