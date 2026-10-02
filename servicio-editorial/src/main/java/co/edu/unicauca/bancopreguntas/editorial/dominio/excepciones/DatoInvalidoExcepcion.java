package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Un value object recibió un dato que no puede representar (nulo, fuera de rango o mal formado).
 *
 * <p>Código de contrato: {@code SOLICITUD_INVALIDA}. CONTRATOS.md 5.3 (HTTP 400).</p>
 */
public class DatoInvalidoExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "SOLICITUD_INVALIDA";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public DatoInvalidoExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
