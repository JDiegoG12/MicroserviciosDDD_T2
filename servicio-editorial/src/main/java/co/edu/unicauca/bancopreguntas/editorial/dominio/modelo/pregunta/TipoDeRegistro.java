package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

/**
 * Tipo de hecho que anota un {@link RegistroDeTrazabilidad} (CONTRATOS.md 8.1, {@code TrazabilidadRespuesta}).
 */
public enum TipoDeRegistro {
    /** Nacimiento de la Pregunta (CU-04). */
    CREACION,
    /** Cambio de contenido (CU-05). */
    MODIFICACION,
    /** Cambio de estado del ciclo de vida (RF-15). */
    TRANSICION
}
