package co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones;

/**
 * Catálogo no respondió o tardó más que el deadline de 2 s. Código {@code CATALOGO_NO_DISPONIBLE}
 * (CONTRATOS.md 6, HTTP 503). La Pregunta no se guarda.
 */
public class CatalogoNoDisponibleExcepcion extends ExcepcionDeAplicacion {

    /** Código exacto de CONTRATOS.md. */
    public static final String CODIGO = "CATALOGO_NO_DISPONIBLE";

    /**
     * Crea la excepción con la causa técnica.
     *
     * @param mensaje explicación
     * @param causa   error original del cliente gRPC, o {@code null}
     */
    public CatalogoNoDisponibleExcepcion(String mensaje, Throwable causa) {
        super(CODIGO, mensaje, causa);
    }
}
