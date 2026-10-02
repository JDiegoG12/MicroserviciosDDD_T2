import { Injectable, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';
import { generarUuid } from '../../../dominio/compartido/uuid';
import { ejecutarConCorrelacion } from '../../../infraestructura/correlacion/contexto-correlacion';

const ENCABEZADO_CORRELACION = 'X-Id-Correlacion';

/**
 * Arma el `idCorrelacion` de cada peticion (CONTRATOS.md 4.1): si llega
 * `X-Id-Correlacion` se usa, si no se genera uno nuevo. Se devuelve en la
 * respuesta con el mismo encabezado, y queda disponible durante toda la
 * peticion a traves del contexto asincrono (`contexto-correlacion.ts`),
 * para que todas las lineas de log y el evento publicado lo incluyan.
 */
@Injectable()
export class CorrelacionMiddleware implements NestMiddleware {
  public use(peticion: Request, respuesta: Response, siguiente: NextFunction): void {
    const idCorrelacion = peticion.header(ENCABEZADO_CORRELACION) ?? generarUuid();
    respuesta.setHeader(ENCABEZADO_CORRELACION, idCorrelacion);
    ejecutarConCorrelacion(idCorrelacion, siguiente);
  }
}
