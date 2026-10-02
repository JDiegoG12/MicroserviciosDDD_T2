/**
 * Contrato comun de todo evento de dominio emitido por un agregado de este
 * servicio.
 *
 * Un evento de dominio es un objeto de solo lectura que describe un hecho ya
 * ocurrido. La traduccion al mensaje de integracion (el sobre de
 * CONTRATOS.md 7.3, con `idEvento`, `origen`, `versionEvento`, etc.) ocurre
 * en `infraestructura` (etapa 2), nunca aqui.
 */
export interface EventoDeDominio {
  /** Nombre del evento, por ejemplo `"SimulacroDefinido"`. */
  readonly tipoEvento: string;

  /** Instante del dominio en el que ocurrio el hecho (no cuando se publica). */
  readonly fechaOcurrencia: Date;
}
