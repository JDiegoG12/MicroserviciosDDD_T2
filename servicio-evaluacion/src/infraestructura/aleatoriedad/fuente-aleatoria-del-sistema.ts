import { Injectable } from '@nestjs/common';
import { FuenteAleatoria } from '../../dominio/servicios/fuente-aleatoria';

/**
 * Implementacion real de `FuenteAleatoria` con `Math.random()`.
 *
 * Es la unica clase de todo el servicio que puede llamar a `Math.random()`:
 * el dominio la recibe inyectada para que sus pruebas sean deterministas
 * (CLAUDE.md del servicio).
 */
@Injectable()
export class FuenteAleatoriaDelSistema implements FuenteAleatoria {
  public siguiente(): number {
    return Math.random();
  }
}
