package co.edu.unicauca.bancopreguntas.editorial.dominio.modelo.comun;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados con la forma de CONTRATOS.md 5.1
 * ({@code contenido}, {@code pagina}, {@code tamano}, {@code totalElementos}, {@code totalPaginas}).
 *
 * @param contenido      elementos de esta página
 * @param pagina         número de página, desde 0
 * @param tamano         tamaño de página solicitado
 * @param totalElementos total de elementos que cumplen el filtro
 * @param totalPaginas   total de páginas
 * @param <T>            tipo de los elementos
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    /**
     * Copia el contenido para que la página sea inmutable.
     */
    public Pagina {
        contenido = List.copyOf(contenido);
    }

    /**
     * Arma una página a partir de una lista completa ya filtrada y ordenada.
     *
     * @param todos      todos los elementos que cumplen el filtro
     * @param paginacion página solicitada
     * @param <T>        tipo de los elementos
     * @return la página correspondiente
     */
    public static <T> Pagina<T> desdeLista(List<T> todos, Paginacion paginacion) {
        int desde = (int) Math.min(paginacion.desplazamiento(), todos.size());
        int hasta = Math.min(desde + paginacion.tamano(), todos.size());
        return new Pagina<>(todos.subList(desde, hasta), paginacion.pagina(), paginacion.tamano(),
                todos.size(), calcularTotalPaginas(todos.size(), paginacion.tamano()));
    }

    /**
     * Calcula cuántas páginas hacen falta para mostrar todos los elementos.
     *
     * @param totalElementos total de elementos
     * @param tamano         tamaño de página (mayor que cero)
     * @return número de páginas
     */
    public static int calcularTotalPaginas(long totalElementos, int tamano) {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }

    /**
     * Transforma el contenido conservando los datos de paginación.
     *
     * @param transformacion función que convierte cada elemento
     * @param <R>            tipo resultante
     * @return una página nueva con el contenido transformado
     */
    public <R> Pagina<R> mapear(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(), pagina, tamano, totalElementos, totalPaginas);
    }
}
