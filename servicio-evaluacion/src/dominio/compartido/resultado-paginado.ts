/**
 * Resultado de una busqueda paginada sobre un repositorio del dominio.
 *
 * Es deliberadamente mas simple que la `Pagina<T>` de aplicacion (que ya
 * trae `pagina`, `tamano` y `totalPaginas` de CONTRATOS.md 5.1): el dominio
 * solo necesita saber cuantos elementos hay en total para que la aplicacion
 * calcule el resto.
 *
 * @typeParam T Tipo de los elementos de la pagina.
 */
export interface ResultadoPaginado<T> {
  /** Elementos de la pagina solicitada. */
  readonly elementos: T[];

  /** Cantidad total de elementos que cumplen el filtro, sin paginar. */
  readonly totalElementos: number;
}
