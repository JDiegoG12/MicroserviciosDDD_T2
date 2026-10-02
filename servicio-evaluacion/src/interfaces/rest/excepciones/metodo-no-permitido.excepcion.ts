/**
 * El metodo HTTP de la peticion no esta soportado en una ruta que si
 * existe (CONTRATOS.md 5.3, codigo `METODO_NO_PERMITIDO`, 405). Por
 * ejemplo, `DELETE /api/v1/simulacros` cuando esa ruta solo acepta `GET` y
 * `POST`.
 *
 * `METODO_NO_PERMITIDO` es un codigo transversal (comun a los tres
 * servicios), no una invariante de este dominio: por eso no vive en
 * `CodigoError`, sino aqui, en la capa que conoce las rutas HTTP.
 */
export class MetodoNoPermitidoExcepcion extends Error {
  public readonly codigo = 'METODO_NO_PERMITIDO' as const;

  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'MetodoNoPermitidoExcepcion';
  }
}
