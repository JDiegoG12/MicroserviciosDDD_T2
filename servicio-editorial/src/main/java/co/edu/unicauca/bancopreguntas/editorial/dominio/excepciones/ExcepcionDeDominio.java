package co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones;

/**
 * Raíz de todas las excepciones del dominio editorial.
 *
 * <p>Cada subclase lleva el {@code codigo} exacto de la sección 5.3 / 8.1 de CONTRATOS.md, para que en
 * la etapa 2 el manejador de errores lo traduzca a HTTP ({@code application/problem+json}) sin
 * ambigüedad.</p>
 */
public abstract class ExcepcionDeDominio extends RuntimeException {

    private final String codigo;

    /**
     * Crea la excepción con su código de contrato y un mensaje legible en español.
     *
     * @param codigo  código exacto de CONTRATOS.md (por ejemplo {@code TRANSICION_NO_PERMITIDA})
     * @param mensaje explicación para el usuario
     */
    protected ExcepcionDeDominio(String codigo, String mensaje) {
        super(mensaje);
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
