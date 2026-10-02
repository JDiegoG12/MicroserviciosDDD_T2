package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * El usuario no tiene el rol requerido o no es el dueño del recurso.
 *
 * <p>Código de contrato: {@code ACCESO_DENEGADO}. CONTRATOS.md 4.1 (HTTP 403).</p>
 */
public class AccesoDenegadoExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "ACCESO_DENEGADO";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public AccesoDenegadoExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
