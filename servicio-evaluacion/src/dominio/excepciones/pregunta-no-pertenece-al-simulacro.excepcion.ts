import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * El estudiante intento responder una pregunta que no forma parte del
 * Simulacro de su intento (tercera consecuencia de INV-29,
 * MODELO-DOMINIO.md A.2; CONTRATOS.md 8.3, codigo
 * `PREGUNTA_NO_PERTENECE_AL_SIMULACRO`, 422).
 */
export class PreguntaNoPerteneceAlSimulacroExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.PREGUNTA_NO_PERTENECE_AL_SIMULACRO, mensaje);
  }
}
