package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Se intentó modificar una Pregunta que no está en un estado editable.
 *
 * <p>Código de contrato: {@code PREGUNTA_NO_EDITABLE}. INV-10, RF-06, D-02 (HTTP 409).</p>
 */
public class PreguntaNoEditableExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "PREGUNTA_NO_EDITABLE";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public PreguntaNoEditableExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
