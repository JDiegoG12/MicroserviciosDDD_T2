package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Se intentó una transición del ciclo de vida que la máquina de estados de la Pregunta no declara.
 *
 * <p>Código de contrato: {@code TRANSICION_NO_PERMITIDA}. INV-09, RF-15 (HTTP 409).</p>
 */
public class TransicionNoPermitidaExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "TRANSICION_NO_PERMITIDA";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public TransicionNoPermitidaExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
