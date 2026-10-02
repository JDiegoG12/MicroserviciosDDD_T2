package co.edu.unicauca.bancopreguntas.editorial.aplicacion.puertos.salida;

/**
 * Motivo por el que Catálogo rechaza una clasificación. Refleja el enum {@code MotivoRechazo} del
 * {@code .proto} de CONTRATOS.md 6, sin el prefijo {@code MOTIVO_RECHAZO_}.
 */
public enum MotivoRechazoClasificacion {
    /** La clasificación es válida. */
    NINGUNO,
    /** La Competencia no existe. */
    COMPETENCIA_INEXISTENTE,
    /** El Tema no existe. */
    TEMA_INEXISTENTE,
    /** El Tema no pertenece a la Competencia. */
    TEMA_NO_PERTENECE_A_COMPETENCIA,
    /** El Subtema no existe. */
    SUBTEMA_INEXISTENTE,
    /** El Subtema no pertenece al Tema. */
    SUBTEMA_NO_PERTENECE_A_TEMA
}
