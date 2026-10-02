/**
 * Faltan los encabezados de identidad `X-Usuario-Id` o `X-Roles`
 * (CONTRATOS.md 4.1, codigo `NO_AUTENTICADO`, 401).
 *
 * `NO_AUTENTICADO` es un codigo transversal de CONTRATOS.md 5.3 (comun a
 * los tres servicios), no una invariante de este dominio: por eso no vive
 * en `CodigoError` (que solo lista los codigos propios de Evaluacion), sino
 * aqui, en la capa que conoce los encabezados HTTP.
 */
export class NoAutenticadoExcepcion extends Error {
  public readonly codigo = 'NO_AUTENTICADO' as const;

  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'NoAutenticadoExcepcion';
  }
}
