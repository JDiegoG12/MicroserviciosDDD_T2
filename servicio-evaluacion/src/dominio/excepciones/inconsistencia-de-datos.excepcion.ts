import { CodigoError } from './codigo-error';
import { ExcepcionDeDominio } from './excepcion-de-dominio';

/**
 * Se detecto un estado interno que nunca deberia ocurrir en operacion
 * normal: por ejemplo, falta la copia local de una Pregunta que un
 * Simulacro o un Intento ya referencian. Las copias nunca se borran, asi
 * que esto es una inconsistencia interna (CONTRATOS.md 8.3, "Copia local
 * ausente": 500 `ERROR_INTERNO` y un registro de error en el log; el
 * registro en el log lo hace quien atrape esta excepcion en
 * `interfaces`).
 */
export class InconsistenciaDeDatosExcepcion extends ExcepcionDeDominio {
  constructor(mensaje: string) {
    super(CodigoError.ERROR_INTERNO, mensaje);
  }
}
