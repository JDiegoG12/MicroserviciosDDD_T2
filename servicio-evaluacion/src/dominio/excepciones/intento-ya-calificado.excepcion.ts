import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * Se intento calificar un IntentoDeSimulacro que ya estaba CALIFICADO: la
 * calificacion es inmutable (INV-32, CONTRATOS.md 8.3, codigo
 * `INTENTO_YA_CALIFICADO`, 409).
 */
export class IntentoYaCalificadoExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.INTENTO_YA_CALIFICADO, mensaje);
  }
}
