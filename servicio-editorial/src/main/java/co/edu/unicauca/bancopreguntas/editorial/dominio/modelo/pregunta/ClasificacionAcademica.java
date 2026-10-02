package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

import co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun.Validaciones;

import java.util.UUID;

/**
 * Terna Competencia/Tema/Subtema que sitúa a la Pregunta en el Catálogo Académico (Taller 1, sección 6).
 *
 * <p>Solo guarda identificadores, nunca nombres (D-13). Que la terna exista y sea coherente (INV-07)
 * lo verifica el caso de uso llamando a Catálogo por gRPC antes de guardar (MODELO-DOMINIO A.2).</p>
 *
 * @param competenciaId identificador de la Competencia
 * @param temaId        identificador del Tema
 * @param subtemaId     identificador del Subtema
 */
public record ClasificacionAcademica(UUID competenciaId, UUID temaId, UUID subtemaId) {

    /**
     * Exige los tres identificadores (INV-07: exactamente una Competencia, un Tema y un Subtema).
     *
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si falta alguno
     */
    public ClasificacionAcademica {
        Validaciones.requerirNoNulo(competenciaId, "clasificacion.competenciaId");
        Validaciones.requerirNoNulo(temaId, "clasificacion.temaId");
        Validaciones.requerirNoNulo(subtemaId, "clasificacion.subtemaId");
    }

    /**
     * Crea la clasificación a partir de los tres UUID en texto.
     *
     * @param competenciaId UUID de la Competencia
     * @param temaId        UUID del Tema
     * @param subtemaId     UUID del Subtema
     * @return la clasificación
     * @throws co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion si alguno falta o no es UUID
     */
    public static ClasificacionAcademica de(String competenciaId, String temaId, String subtemaId) {
        return new ClasificacionAcademica(
                Validaciones.aUuid(competenciaId, "clasificacion.competenciaId"),
                Validaciones.aUuid(temaId, "clasificacion.temaId"),
                Validaciones.aUuid(subtemaId, "clasificacion.subtemaId"));
    }
}
