package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.errores;

/**
 * Faltan los encabezados de identidad {@code X-Usuario-Id} o {@code X-Roles}. Código {@code NO_AUTENTICADO}
 * (CONTRATOS.md 4.1 y 5.3, HTTP 401).
 */
public class NoAutenticadoExcepcion extends RuntimeException {

    /**
     * Crea la excepción.
     *
     * @param mensaje explicación
     */
    public NoAutenticadoExcepcion(String mensaje) {
        super(mensaje);
    }
}
