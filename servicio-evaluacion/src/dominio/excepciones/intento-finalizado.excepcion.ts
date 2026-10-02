import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * Se intento registrar una respuesta sobre un IntentoDeSimulacro que ya no
 * esta EN_CURSO, sea porque el estudiante lo finalizo o porque el
 * vencimiento perezoso lo cerro por TIEMPO_AGOTADO (INV-31, CONTRATOS.md
 * 8.3 y 11.3, codigo `INTENTO_FINALIZADO`, 409).
 */
export class IntentoFinalizadoExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.INTENTO_FINALIZADO, mensaje);
  }
}
