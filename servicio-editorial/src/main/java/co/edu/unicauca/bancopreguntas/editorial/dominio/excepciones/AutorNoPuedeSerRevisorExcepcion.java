package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Se intentó asignar como Revisor al Autor de la Pregunta.
 *
 * <p>Código de contrato: {@code AUTOR_NO_PUEDE_SER_REVISOR}. INV-16, D-06 (HTTP 422).</p>
 */
public class AutorNoPuedeSerRevisorExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "AUTOR_NO_PUEDE_SER_REVISOR";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public AutorNoPuedeSerRevisorExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
