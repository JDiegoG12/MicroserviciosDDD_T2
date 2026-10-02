package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Se intentó asignar dos veces al mismo Revisor dentro de un Proceso.
 *
 * <p>Código de contrato: {@code REVISOR_DUPLICADO}. INV-17 (HTTP 422).</p>
 */
public class RevisorDuplicadoExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "REVISOR_DUPLICADO";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public RevisorDuplicadoExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
