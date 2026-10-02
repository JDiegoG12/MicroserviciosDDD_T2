package co.edu.unicauca.bancopreguntas.editorial.aplicacion.seguridad;

/**
 * Roles válidos del sistema, con los nombres exactos de CONTRATOS.md 4.1 (RF-03, D-08, D-09).
 */
public enum Rol {
    /** Gestiona el sistema: asigna Revisores, publica, archiva y consulta la trazabilidad. */
    ADMINISTRADOR,
    /** Crea y modifica Preguntas. */
    AUTOR,
    /** Evalúa Preguntas en la Revisión por pares. */
    REVISOR,
    /** Define Simulacros; en Editorial consulta las Preguntas publicadas. */
    DOCENTE,
    /** Presenta Simulacros; no opera en Editorial. */
    ESTUDIANTE
}
