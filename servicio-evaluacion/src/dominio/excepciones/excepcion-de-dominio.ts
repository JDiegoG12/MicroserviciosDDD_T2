import { CodigoError } from './codigo-error';

/**
 * Clase base de toda excepcion lanzada por el dominio.
 *
 * Toda excepcion de dominio lleva el `codigo` exacto de CONTRATOS.md 5.3/8.x
 * para que la etapa 2 la traduzca a HTTP sin ambiguedad.
 */
export abstract class ExcepcionDeDominio extends Error {
  protected constructor(
    public readonly codigo: CodigoError,
    mensaje: string,
  ) {
    super(mensaje);
    this.name = new.target.name;
  }
}
