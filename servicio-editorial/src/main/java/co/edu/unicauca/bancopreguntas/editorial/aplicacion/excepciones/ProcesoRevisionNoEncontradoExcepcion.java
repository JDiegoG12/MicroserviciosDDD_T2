package co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones;

/**
 * El Proceso de revisión solicitado no existe. Código {@code PROCESO_REVISION_NO_ENCONTRADO} (CONTRATOS.md 5.3, HTTP 404).
 */
public class ProcesoRevisionNoEncontradoExcepcion extends ExcepcionDeAplicacion {

    /** Código exacto de CONTRATOS.md 5.3 (fila 404). */
    public static final String CODIGO = "PROCESO_REVISION_NO_ENCONTRADO";

    /**
     * Crea la excepción para el identificador buscado.
     *
     * @param procesoId identificador que no existe
     */
    public ProcesoRevisionNoEncontradoExcepcion(String procesoId) {
        super(CODIGO, "No existe el proceso de revisión " + procesoId + ".");
    }
}
