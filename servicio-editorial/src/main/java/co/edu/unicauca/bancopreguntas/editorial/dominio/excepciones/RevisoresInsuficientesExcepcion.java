package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Se intentó abrir un Proceso de revisión con menos de dos Revisores.
 *
 * <p>Código de contrato: {@code REVISORES_INSUFICIENTES}. INV-15, D-03 (HTTP 422).</p>
 */
public class RevisoresInsuficientesExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "REVISORES_INSUFICIENTES";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public RevisoresInsuficientesExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
