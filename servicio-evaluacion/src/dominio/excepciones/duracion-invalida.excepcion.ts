import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * La duracion maxima de un Simulacro no es un numero entero estrictamente
 * positivo de minutos (INV-28, CONTRATOS.md 8.3, codigo `DURACION_INVALIDA`,
 * 422).
 */
export class DuracionInvalidaExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.DURACION_INVALIDA, mensaje);
  }
}
