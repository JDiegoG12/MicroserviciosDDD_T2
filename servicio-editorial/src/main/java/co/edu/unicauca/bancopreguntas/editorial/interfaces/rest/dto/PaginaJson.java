package co.edu.unicauca.bancopreguntas.editorial.interfaces.rest.dto;

import java.util.List;

/**
 * Página de resultados (CONTRATOS.md 5.1).
 *
 * @param contenido      elementos de la página
 * @param pagina         número de página, desde 0
 * @param tamano         tamaño de página
 * @param totalElementos total de elementos
 * @param totalPaginas   total de páginas
 * @param <T>            tipo de los elementos
 */
public record PaginaJson<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
