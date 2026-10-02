package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Un usuario que no está asignado al Proceso intentó registrar una evaluación.
 *
 * <p>Código de contrato: {@code REVISOR_NO_ASIGNADO}. CU-11 (HTTP 403).</p>
 */
public class RevisorNoAsignadoExcepcion extends ExcepcionDeDominio {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "REVISOR_NO_ASIGNADO";

    /**
     * Crea la excepción con un mensaje legible.
     *
     * @param mensaje explicación para el usuario
     */
    public RevisorNoAsignadoExcepcion(String mensaje) {
        super(CODIGO, mensaje);
    }
}
