package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores;

/**
 * Las migraciones de la base de datos aún no terminan. Código {@code BASE_DE_DATOS_NO_DISPONIBLE}
 * (CONTRATOS.md 5.3 y 9.3.6, HTTP 503).
 */
public class BaseDeDatosNoDisponibleExcepcion extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param mensaje explicación
     */
    public BaseDeDatosNoDisponibleExcepcion(String mensaje) {
        super(mensaje);
    }
}
