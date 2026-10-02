package co.edu.unicauca.bancopreguntas.editorial.aplicacion.excepciones;

/**
 * Raíz de las excepciones de la capa de aplicación: recursos inexistentes y fallos o rechazos de
 * dependencias externas. Llevan el {@code codigo} exacto de CONTRATOS.md 5.3 / 8.1.
 */
public abstract class ExcepcionDeAplicacion extends RuntimeException {

    private final String codigo;

    /**
     * Crea la excepción con su código de contrato.
     *
     * @param codigo  código exacto de CONTRATOS.md
     * @param mensaje explicación para el usuario
     */
    protected ExcepcionDeAplicacion(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    /**
     * Crea la excepción con su código de contrato y la causa técnica.
     *
     * @param codigo  código exacto de CONTRATOS.md
     * @param mensaje explicación para el usuario
     * @param causa   error original
     */
    protected ExcepcionDeAplicacion(String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }

    /**
     * Devuelve el código de contrato de la excepción.
     *
     * @return código exacto de CONTRATOS.md
     */
    public String getCodigo() {
        return codigo;
    }
}
