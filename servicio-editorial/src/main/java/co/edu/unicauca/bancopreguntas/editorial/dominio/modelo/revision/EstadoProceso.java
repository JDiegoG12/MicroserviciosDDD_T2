package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

/**
 * Estado del Proceso de revisión (MODELO-DOMINIO A.3; INV-21).
 */
public enum EstadoProceso {
    /** Acepta evaluaciones de los Revisores asignados. */
    ABIERTO,
    /** Tiene Dictamen; no admite más cambios (INV-21). */
    CERRADO
}
