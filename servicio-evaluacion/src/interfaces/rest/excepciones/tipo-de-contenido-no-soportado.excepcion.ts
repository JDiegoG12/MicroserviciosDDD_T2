/**
 * La peticion trae un cuerpo pero su `Content-Type` no es
 * `application/json` (CONTRATOS.md 5.1 y 5.3, codigo
 * `TIPO_DE_CONTENIDO_NO_SOPORTADO`, 415).
 *
 * `TIPO_DE_CONTENIDO_NO_SOPORTADO` es un codigo transversal (comun a los
 * tres servicios), no una invariante de este dominio: por eso no vive en
 * `CodigoError`, sino aqui, en la capa que conoce los encabezados HTTP.
 */
export class TipoDeContenidoNoSoportadoExcepcion extends Error {
  public readonly codigo = 'TIPO_DE_CONTENIDO_NO_SOPORTADO' as const;

  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'TipoDeContenidoNoSoportadoExcepcion';
  }
}
