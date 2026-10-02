import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * Se intento incluir en un Simulacro una pregunta que no esta PUBLICADA
 * (INV-25, CONTRATOS.md 8.3, codigo `PREGUNTA_NO_PUBLICADA`, 422).
 *
 * Protege una invariante que la API no puede provocar en la practica:
 * `EnsambladorSimulacroServicio` ya filtra las candidatas por
 * `estaPublicada()` antes de llegar aqui. Existe y se prueba en el dominio
 * como defensa adicional (CONTRATOS.md 8.3, "Codigos de error internos").
 */
export class PreguntaNoPublicadaExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.PREGUNTA_NO_PUBLICADA, mensaje);
  }
}
