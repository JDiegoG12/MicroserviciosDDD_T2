import { Injectable, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';
import { TipoDeContenidoNoSoportadoExcepcion } from '../excepciones/tipo-de-contenido-no-soportado.excepcion';

/** Metodos HTTP en los que CONTRATOS.md 5.1 exige `application/json`. */
const METODOS_CON_CUERPO = new Set(['POST', 'PUT', 'PATCH']);

/**
 * Responde 415 `TIPO_DE_CONTENIDO_NO_SOPORTADO` cuando una peticion
 * `POST`/`PUT`/`PATCH` trae un cuerpo con un `Content-Type` distinto de
 * `application/json` (CONTRATOS.md 5.1 y 5.3).
 *
 * Una peticion sin cuerpo (sin `Content-Length` ni `Transfer-Encoding`,
 * por ejemplo `POST /intentos/{intentoId}/finalizacion`) no se revisa: no
 * hay nada que el `Content-Type` describa.
 */
@Injectable()
export class TipoDeContenidoMiddleware implements NestMiddleware {
  public use(peticion: Request, _respuesta: Response, siguiente: NextFunction): void {
    if (!METODOS_CON_CUERPO.has(peticion.method) || !this.tieneCuerpo(peticion)) {
      siguiente();
      return;
    }
    const tipoContenido = peticion.header('content-type') ?? '';
    if (!tipoContenido.toLowerCase().startsWith('application/json')) {
      throw new TipoDeContenidoNoSoportadoExcepcion(
        `Content-Type "${tipoContenido || '(ausente)'}" no soportado: se espera application/json.`,
      );
    }
    siguiente();
  }

  private tieneCuerpo(peticion: Request): boolean {
    const longitud = peticion.header('content-length');
    if (longitud !== undefined) {
      return Number(longitud) > 0;
    }
    return peticion.header('transfer-encoding') !== undefined;
  }
}
