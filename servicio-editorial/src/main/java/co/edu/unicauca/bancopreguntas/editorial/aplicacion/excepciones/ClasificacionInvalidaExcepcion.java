package co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones;

/**
 * Catálogo respondió que la terna Competencia/Tema/Subtema no existe o no es coherente (INV-07, D-13).
 * Código {@code CLASIFICACION_INVALIDA} (CONTRATOS.md 6, HTTP 422 con {@code detail = detalle}).
 */
public class ClasificacionInvalidaExcepcion extends ExcepcionDeAplicacion {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "CLASIFICACION_INVALIDA";

    private final String motivo;

    /**
     * Crea la excepción con el motivo y el detalle que devolvió Catálogo.
     *
     * @param motivo  motivo del rechazo (nombre del enum {@code MotivoRechazo})
     * @param detalle texto legible en español para el usuario
     */
    public ClasificacionInvalidaExcepcion(String motivo, String detalle) {
        super(CODIGO, detalle);
        this.motivo = motivo;
    }

    /**
     * Devuelve el motivo del rechazo.
     *
     * @return nombre del motivo
     */
    public String getMotivo() {
        return motivo;
    }
}
