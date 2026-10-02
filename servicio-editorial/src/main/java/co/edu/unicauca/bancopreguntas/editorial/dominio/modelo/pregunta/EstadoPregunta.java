package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.pregunta;

/**
 * Los ocho estados del ciclo de vida de la Pregunta, con los nombres exactos de CONTRATOS.md 11.1 (RF-14).
 */
public enum EstadoPregunta {
    /** Creada pero estructuralmente incompleta. Editable (D-02). */
    BORRADOR,
    /** Supera la validación estructural y aún no se somete. Editable (D-02). */
    EN_CONSTRUCCION,
    /** Sometida por su Autor; espera la asignación de Revisores (CU-07). */
    PENDIENTE_REVISION,
    /** Evaluada por los Revisores asignados (CU-10, D-14). */
    EN_REVISION,
    /** Dictamen mayor a 70 % (INV-20). */
    APROBADA,
    /** Dictamen menor o igual a 70 %. Estado transitorio: pasa a EN_CONSTRUCCION (D-07). */
    RECHAZADA,
    /** Disponible para Simulacros (CU-08). */
    PUBLICADA,
    /** Retirada del uso activo sin borrarse (CU-09, INV-12). */
    ARCHIVADA;

    /**
     * Indica si en este estado se permite modificar el contenido (INV-10).
     *
     * @return {@code true} solo para {@code BORRADOR} y {@code EN_CONSTRUCCION}
     */
    public boolean esEditable() {
        return this == BORRADOR || this == EN_CONSTRUCCION;
    }
}
