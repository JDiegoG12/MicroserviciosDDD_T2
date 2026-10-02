import { Injectable, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';
import {
  ejecutarConCorrelacion,
  ENCABEZADO_CORRELACION,
  resolverIdCorrelacion,
} from '../../../infraestructura/correlacion/contexto-correlacion';
import { registrar } from '../../../infraestructura/observabilidad/registrador';

/**
 * Arma el `idCorrelacion` de cada peticion (CONTRATOS.md 4.1): si llega
 * `X-Id-Correlacion` y es un UUID valido se usa (normalizado a
 * minusculas); si no llega, o si no es valido, se genera uno nuevo (y se
 * registra un aviso en el log con el valor recibido). Se devuelve en la
 * respuesta con el mismo encabezado, y queda disponible durante toda la
 * peticion a traves del contexto asincrono (`contexto-correlacion.ts`),
 * para que todas las lineas de log y el evento publicado lo incluyan.
 */
@Injectable()
export class CorrelacionMiddleware implements NestMiddleware {
  public use(peticion: Request, respuesta: Response, siguiente: NextFunction): void {
    const encabezadoCrudo = peticion.header(ENCABEZADO_CORRELACION);
    const { idCorrelacion, eraInvalido } = resolverIdCorrelacion(encabezadoCrudo);
    respuesta.setHeader(ENCABEZADO_CORRELACION, idCorrelacion);
    ejecutarConCorrelacion(idCorrelacion, () => {
      if (eraInvalido) {
        registrar('warn', 'X-Id-Correlacion invalido: se genero uno nuevo.', { valorRecibido: encabezadoCrudo });
      }
      siguiente();
    });
  }
}
