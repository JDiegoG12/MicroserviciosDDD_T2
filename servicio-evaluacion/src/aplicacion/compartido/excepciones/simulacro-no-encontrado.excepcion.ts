import { CodigoError } from '../../../dominio/excepciones/codigo-error';
import { ExcepcionDeAplicacion } from './excepcion-de-aplicacion';

/**
 * No existe un Simulacro con el id solicitado (CONTRATOS.md 8.3, 404
 * `SIMULACRO_NO_ENCONTRADO`).
 */
export class SimulacroNoEncontradoExcepcion extends ExcepcionDeAplicacion {
  constructor(simulacroId: string) {
    super(CodigoError.SIMULACRO_NO_ENCONTRADO, `No existe un simulacro con id ${simulacroId}.`);
  }
}
