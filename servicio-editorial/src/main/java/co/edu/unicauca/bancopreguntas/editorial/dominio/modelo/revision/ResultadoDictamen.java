package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.revision;

/**
 * Los dos únicos valores que admite el Dictamen (INV-20; MODELO-DOMINIO A.1, errata E-4).
 */
public enum ResultadoDictamen {
    /** Porcentaje de aprobatorias estrictamente mayor a 70 %. */
    APROBADA,
    /** Cualquier otro caso. */
    RECHAZADA
}
