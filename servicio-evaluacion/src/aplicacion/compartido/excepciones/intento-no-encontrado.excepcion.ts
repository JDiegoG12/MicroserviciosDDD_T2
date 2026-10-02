import { CodigoError } from '../../../dominio/excepciones/codigo-error';
import { ExcepcionDeAplicacion } from './excepcion-de-aplicacion';

/**
 * No existe un IntentoDeSimulacro con el id solicitado (CONTRATOS.md 8.3,
 * 404 `INTENTO_NO_ENCONTRADO`).
 */
export class IntentoNoEncontradoExcepcion extends ExcepcionDeAplicacion {
  constructor(intentoId: string) {
    super(CodigoError.INTENTO_NO_ENCONTRADO, `No existe un intento con id ${intentoId}.`);
  }
}
