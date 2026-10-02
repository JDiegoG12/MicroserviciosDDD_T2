import { CodigoError } from '../../../dominio/excepciones/codigo-error';

/** Un campo invalido de la solicitud, con su mensaje (CONTRATOS.md 5.2). */
export interface ErrorDeCampo {
  readonly campo: string;
  readonly mensaje: string;
}

/**
 * La solicitud HTTP no tiene la forma esperada (cuerpo invalido segun los
 * decoradores de `class-validator`). Se lanza desde el `exceptionFactory`
 * del `ValidationPipe` global, para que el filtro de excepciones produzca
 * el `application/problem+json` con la lista `errores` de CONTRATOS.md 5.2.
 */
export class SolicitudInvalidaHttpExcepcion extends Error {
  public readonly codigo = CodigoError.SOLICITUD_INVALIDA;

  constructor(public readonly errores: readonly ErrorDeCampo[]) {
    super('La solicitud tiene uno o mas campos invalidos.');
    this.name = 'SolicitudInvalidaHttpExcepcion';
  }
}
