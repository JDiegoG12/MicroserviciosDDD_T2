package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Se intentó operar sobre un Proceso de revisión que ya tiene Dictamen.
 *
 * <p>Código de contrato: {@code PROCESO_CERRADO}. INV-21 (HTTP 409).</p>
 */
public class ProcesoCerradoExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "PROCESO_CERRADO";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public ProcesoCerradoExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
