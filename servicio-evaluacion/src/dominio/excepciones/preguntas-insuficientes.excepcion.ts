import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * No hay suficientes Preguntas Publicadas que cumplan el criterio de
 * generacion para ensamblar un Simulacro: la lista de seleccionadas
 * quedaria vacia (INV-26, CONTRATOS.md 8.3, codigo `PREGUNTAS_INSUFICIENTES`,
 * 422). INV-25 (no publicada) e INV-27 (duplicada) tienen sus propios
 * codigos: ver `PreguntaNoPublicadaExcepcion` y
 * `PreguntaDuplicadaEnSimulacroExcepcion` (CONTRATOS.md 8.3, "Codigos de
 * error internos").
 */
export class PreguntasInsuficientesExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.PREGUNTAS_INSUFICIENTES, mensaje);
  }
}
