import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { SimulacroId } from '../../src/dominio/simulacros/simulacro-id';
import { SimulacroRepositorio } from '../../src/dominio/simulacros/simulacro.repositorio';

/**
 * Doble en memoria de `SimulacroRepositorio`, para las pruebas de
 * aplicacion. No es codigo de produccion.
 */
export class SimulacroRepositorioEnMemoria implements SimulacroRepositorio {
  private readonly simulacrosPorId = new Map<string, Simulacro>();
  public cantidadDeGuardados = 0;

  public async guardar(simulacro: Simulacro): Promise<void> {
    this.simulacrosPorId.set(simulacro.simulacroId.aTexto(), simulacro);
    this.cantidadDeGuardados += 1;
  }

  public async obtenerPorId(simulacroId: SimulacroId): Promise<Simulacro | null> {
    return this.simulacrosPorId.get(simulacroId.aTexto()) ?? null;
  }

  public async listar(): Promise<Simulacro[]> {
    return [...this.simulacrosPorId.values()];
  }
}
