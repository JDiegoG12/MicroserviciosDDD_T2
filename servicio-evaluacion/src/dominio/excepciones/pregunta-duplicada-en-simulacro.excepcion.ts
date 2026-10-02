import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * Una misma pregunta aparece repetida dentro del mismo Simulacro (INV-27,
 * CONTRATOS.md 8.3, codigo `PREGUNTA_DUPLICADA_EN_SIMULACRO`, 422).
 *
 * Protege una invariante que la API no puede provocar en la practica:
 * `EnsambladorSimulacroServicio` ya descarta duplicados por `preguntaId`
 * antes de llegar aqui. Existe y se prueba en el dominio como defensa
 * adicional (CONTRATOS.md 8.3, "Codigos de error internos").
 */
export class PreguntaDuplicadaEnSimulacroExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.PREGUNTA_DUPLICADA_EN_SIMULACRO, mensaje);
  }
}
