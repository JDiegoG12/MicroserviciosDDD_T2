package co.edu.unicauca.bancopreguntas.editorial.infraestructura.persistencia;

/**
 * Indica si las migraciones de la base de datos ya terminaron (CONTRATOS.md 9.3.6). Mientras no estén listas, los
 * endpoints que usan persistencia responden 503 {@code BASE_DE_DATOS_NO_DISPONIBLE}.
 */
public interface EstadoDelEsquema {

    /**
     * Consulta el estado de las migraciones.
     *
     * @return {@code true} cuando Flyway terminó de migrar el esquema
     */
    boolean esquemaListo();
}
