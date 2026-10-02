import { LoggerService } from '@nestjs/common';
import { registrar } from './registrador';

/**
 * Puente entre el logger interno de NestJS y `registrar()`.
 *
 * Sin esto, los mensajes de arranque de Nest (`NestFactory`,
 * `InstanceLoader`, `RoutesResolver`, etc.) salen con el formato de texto
 * coloreado por defecto de Nest, distinto del JSON estructurado del resto
 * del servicio. Con `app.useLogger(new RegistradorNestjsServicio())` en
 * `main.ts`, esas lineas tambien pasan por `registrar()` y llevan
 * `idCorrelacion: "-"` (no hay peticion ni mensaje en curso durante el
 * arranque), con el mismo formato que las demas.
 */
export class RegistradorNestjsServicio implements LoggerService {
  public log(mensaje: unknown, ...parametros: unknown[]): void {
    registrar('info', this.aTexto(mensaje), this.contexto(parametros));
  }

  public warn(mensaje: unknown, ...parametros: unknown[]): void {
    registrar('warn', this.aTexto(mensaje), this.contexto(parametros));
  }

  public error(mensaje: unknown, ...parametros: unknown[]): void {
    registrar('error', this.aTexto(mensaje), this.contexto(parametros));
  }

  public debug(mensaje: unknown, ...parametros: unknown[]): void {
    registrar('debug', this.aTexto(mensaje), this.contexto(parametros));
  }

  public verbose(mensaje: unknown, ...parametros: unknown[]): void {
    registrar('debug', this.aTexto(mensaje), this.contexto(parametros));
  }

  private aTexto(mensaje: unknown): string {
    return typeof mensaje === 'string' ? mensaje : JSON.stringify(mensaje);
  }

  private contexto(parametros: unknown[]): Record<string, unknown> | undefined {
    // Nest suele mandar el "contexto" (por ejemplo, "InstanceLoader") como
    // ultimo parametro de texto; el resto (si lo hay) suele ser una traza.
    if (parametros.length === 0) {
      return undefined;
    }
    return { contextoNest: parametros.map((p) => (typeof p === 'string' ? p : JSON.stringify(p))) };
  }
}
