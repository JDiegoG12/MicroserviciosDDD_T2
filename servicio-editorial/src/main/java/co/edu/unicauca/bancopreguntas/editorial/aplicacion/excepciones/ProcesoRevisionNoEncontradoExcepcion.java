package co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones;

/**
 * El Proceso de revisión solicitado no existe (HTTP 404, CONTRATOS.md 8.1).
 */
public class ProcesoRevisionNoEncontradoExcepcion extends ExcepcionDeAplicacion {

    // DUDA: CONTRATOS 8.1 indica 404 para los procesos pero no nombra el código; se sigue el patrón
    // de PREGUNTA_NO_ENCONTRADA con PROCESO_REVISION_NO_ENCONTRADO.
    /** Código propuesto para el 404 de procesos. */
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
