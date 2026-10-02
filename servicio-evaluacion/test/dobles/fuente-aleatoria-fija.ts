import { FuenteAleatoria } from '../../src/dominio/servicios/fuente-aleatoria';

/**
 * Doble en memoria de `FuenteAleatoria`. Por defecto devuelve siempre 0,
 * lo que hace que `EnsambladorSimulacroServicio.elegirAlAzar` conserve el
 * orden original de las candidatas (deterministico para las pruebas que no
 * verifican la aleatoriedad en si misma).
 */
export class FuenteAleatoriaFija implements FuenteAleatoria {
  private indice = 0;

  constructor(private readonly secuencia: readonly number[] = [0]) {}

  public siguiente(): number {
    const valor = this.secuencia[this.indice % this.secuencia.length];
    this.indice += 1;
    return valor;
  }
}
