package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Un Revisor intentó registrar una segunda evaluación en el mismo Proceso.
 *
 * <p>Código de contrato: {@code EVALUACION_YA_REGISTRADA}. INV-18, RF-17 (HTTP 409).</p>
 */
public class EvaluacionYaRegistradaExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "EVALUACION_YA_REGISTRADA";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public EvaluacionYaRegistradaExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
