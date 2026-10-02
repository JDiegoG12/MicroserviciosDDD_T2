package co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones;

/**
 * La Pregunta solicitada no existe. Código {@code PREGUNTA_NO_ENCONTRADA} (CONTRATOS.md 5.3, HTTP 404).
 */
public class PreguntaNoEncontradaExcepcion extends ExcepcionDeAplicacion {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "PREGUNTA_NO_ENCONTRADA";

    /**
     * Crea la excepción para el identificador buscado.
     *
     * @param preguntaId identificador que no existe
     */
    public PreguntaNoEncontradaExcepcion(String preguntaId) {
        super(CODIGO, "No existe la pregunta " + preguntaId + ".");
    }
}
