import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { IntentoDeSimulacroRepositorio } from '../../src/dominio/intentos/intento-de-simulacro.repositorio';
import { IntentoId } from '../../src/dominio/intentos/intento-id';

/**
 * Doble en memoria de `IntentoDeSimulacroRepositorio`, para las pruebas de
 * aplicacion. No es codigo de produccion.
 */
export class IntentoDeSimulacroRepositorioEnMemoria implements IntentoDeSimulacroRepositorio {
  private readonly intentosPorId = new Map<string, IntentoDeSimulacro>();
  public cantidadDeGuardados = 0;

  public async guardar(intento: IntentoDeSimulacro): Promise<void> {
    this.intentosPorId.set(intento.intentoId.aTexto(), intento);
    this.cantidadDeGuardados += 1;
  }

  public async obtenerPorId(intentoId: IntentoId): Promise<IntentoDeSimulacro | null> {
    return this.intentosPorId.get(intentoId.aTexto()) ?? null;
  }
}
