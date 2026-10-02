import { RelojPuerto } from '../../src/aplicacion/puertos/salida/reloj.puerto';

/**
 * Doble en memoria de `RelojPuerto`, para que las pruebas controlen el
 * tiempo y sean deterministas (CLAUDE.md: "el dominio nunca consulta la
 * hora del sistema por su cuenta").
 */
export class RelojFijo implements RelojPuerto {
  private instanteActual: Date;

  constructor(instanteInicial: Date = new Date('2026-10-01T10:00:00Z')) {
    this.instanteActual = instanteInicial;
  }

  public ahora(): Date {
    return this.instanteActual;
  }

  /**
   * Adelanta el reloj una cantidad de minutos.
   *
   * @param minutos Minutos a avanzar.
   */
  public avanzarMinutos(minutos: number): void {
    this.instanteActual = new Date(this.instanteActual.getTime() + minutos * 60_000);
  }

  /**
   * Fija el reloj en un instante exacto.
   *
   * @param instante Nuevo instante actual.
   */
  public fijarEn(instante: Date): void {
    this.instanteActual = instante;
  }
}
