import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * La solicitud esta mal formada: un identificador no es un UUID valido, una
 * letra de opcion no esta entre A y D, o un dato obligatorio falta o tiene
 * un tipo incorrecto (CONTRATOS.md 5.3, codigo `SOLICITUD_INVALIDA`, 400).
 */
export class SolicitudInvalidaExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.SOLICITUD_INVALIDA, mensaje);
  }
}
