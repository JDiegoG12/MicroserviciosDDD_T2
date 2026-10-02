package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import co.edu.unicauca.bancopreguntas.editorial.dominio.excepciones.DatoInvalidoExcepcion;

/**
 * Solicitud de una página de resultados (CONTRATOS.md 5.1: {@code pagina} desde 0, {@code tamano}
 * por defecto 20 y máximo 100).
 *
 * @param pagina número de página, desde 0
 * @param tamano cantidad de elementos por página, de 1 a 100
 */
public record Paginacion(int pagina, int tamano) {

    /** Tamaño de página cuando el cliente no lo indica. */
    public static final int TAMANO_POR_DEFECTO = 20;

    /** Tamaño máximo de página permitido. */
    public static final int TAMANO_MAXIMO = 100;

    /**
     * Valida los límites de la paginación.
     *
     * @throws DatoInvalidoExcepcion si la página es negativa o el tamaño está fuera de 1..100
     */
    public Paginacion {
        if (pagina < 0) {
            throw new DatoInvalidoExcepcion("La página debe ser mayor o igual a 0.");
        }
        // DUDA: CONTRATOS 5.1 fija el máximo en 100 pero no dice si un tamaño mayor se recorta o se
        // rechaza; se toma la opción conservadora de rechazarlo con SOLICITUD_INVALIDA.
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new DatoInvalidoExcepcion("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
    }

    /**
     * Construye la paginación aplicando los valores por defecto cuando faltan.
     *
     * @param pagina número de página o {@code null}
     * @param tamano tamaño de página o {@code null}
     * @return la paginación con los valores por defecto aplicados
     * @throws DatoInvalidoExcepcion si los valores están fuera de rango
     */
    public static Paginacion de(Integer pagina, Integer tamano) {
        return new Paginacion(pagina == null ? 0 : pagina, tamano == null ? TAMANO_POR_DEFECTO : tamano);
    }

    /**
     * Posición del primer elemento de la página dentro del resultado completo.
     *
     * @return índice inicial
     */
    public long desplazamiento() {
        return (long) pagina * tamano;
    }
}
