import { RegistroEventosProcesadosPuerto } from '../../src/aplicacion/puertos/salida/registro-eventos-procesados.puerto';

/**
 * Doble en memoria de `RegistroEventosProcesadosPuerto`, con la estrategia
 * "reclamar y liberar si falla" (CONTRATOS.md 7.7.2). No es codigo de
 * produccion.
 */
export class RegistroEventosProcesadosEnMemoria implements RegistroEventosProcesadosPuerto {
  private readonly idsReclamados = new Set<string>();

  public async reclamar(idEvento: string): Promise<boolean> {
    if (this.idsReclamados.has(idEvento)) {
      return false;
    }
    this.idsReclamados.add(idEvento);
    return true;
  }

  public async liberar(idEvento: string): Promise<void> {
    this.idsReclamados.delete(idEvento);
  }

  /** Helper de pruebas: indica si un idEvento esta reclamado en este momento. */
  public estaReclamado(idEvento: string): boolean {
    return this.idsReclamados.has(idEvento);
  }
}
