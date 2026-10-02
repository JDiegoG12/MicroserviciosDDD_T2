import { Injectable } from '@nestjs/common';
import { RelojPuerto } from '../../aplicacion/puertos/salida/reloj.puerto';

/**
 * Implementacion real de `RelojPuerto` con la hora del sistema.
 *
 * Es la unica clase de todo el servicio que puede llamar a `new Date()`
 * sin argumentos: todo lo demas (dominio y aplicacion) recibe la hora como
 * parametro (CLAUDE.md del servicio).
 */
@Injectable()
export class RelojDelSistemaAdaptador implements RelojPuerto {
  /**
   * Hora actual del sistema.
   *
   * @returns La fecha y hora actuales.
   */
  public ahora(): Date {
    return new Date();
  }
}
