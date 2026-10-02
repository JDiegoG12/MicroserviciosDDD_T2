import { Injectable, NestMiddleware } from '@nestjs/common';
import { HttpAdapterHost } from '@nestjs/core';
import { NextFunction, Request, Response } from 'express';
import { MetodoNoPermitidoExcepcion } from '../excepciones/metodo-no-permitido.excepcion';

/** Una capa de ruta del router interno de Express. */
interface CapaDeRutaExpress {
  readonly route?: { readonly methods: Record<string, boolean> };
  match(ruta: string): boolean;
}

/**
 * Responde 405 `METODO_NO_PERMITIDO` (en vez del 404 por defecto de
 * Express) cuando la ruta pedida existe para otro metodo HTTP
 * (CONTRATOS.md 5.3). Por ejemplo, `DELETE /api/v1/simulacros` cuando esa
 * ruta solo registro `GET` y `POST`.
 *
 * Inspecciona la tabla de rutas que Express ya registro
 * (`router.stack`): si alguna capa coincide con la ruta pedida pero
 * ninguna con el metodo, es un 405; si ninguna capa coincide con la ruta,
 * se deja pasar (sera un 404 normal, no hay nada que esta ruta exista).
 * La tabla se calcula una sola vez (no cambia en tiempo de ejecucion) y se
 * guarda en cache para no recorrerla en cada peticion.
 */
@Injectable()
export class MetodoNoPermitidoMiddleware implements NestMiddleware {
  private capasDeRuta: readonly CapaDeRutaExpress[] | null = null;

  constructor(private readonly adapterHost: HttpAdapterHost) {}

  public use(peticion: Request, _respuesta: Response, siguiente: NextFunction): void {
    // `peticion.path` no sirve aqui: este middleware se registra con
    // `consumer.apply(...).forRoutes('*')`, y Nest lo anida bajo el
    // router que maneja el prefijo global (`/api/v1`); dentro de ese
    // router, Express ya recorto el prefijo de `req.path`/`req.url` (por
    // eso seria siempre "/"). `originalUrl` conserva la ruta completa tal
    // como llego, que es la que coincide con las rutas ya registradas
    // (`Mapped {/api/v1/simulacros, GET}`, con el prefijo incluido).
    const ruta = peticion.originalUrl.split('?')[0];
    const metodosDisponibles = this.metodosDisponiblesPara(ruta);
    if (!metodosDisponibles || metodosDisponibles.has(peticion.method)) {
      siguiente();
      return;
    }
    throw new MetodoNoPermitidoExcepcion(
      `El metodo ${peticion.method} no esta permitido en ${ruta}. Metodos disponibles: ${[...metodosDisponibles].join(', ')}.`,
    );
  }

  private metodosDisponiblesPara(ruta: string): Set<string> | null {
    let encontrados: Set<string> | null = null;
    for (const capa of this.obtenerCapasDeRuta()) {
      if (!capa.route || !capa.match(ruta)) {
        continue;
      }
      encontrados ??= new Set<string>();
      for (const metodo of Object.keys(capa.route.methods)) {
        encontrados.add(metodo.toUpperCase());
      }
    }
    return encontrados;
  }

  private obtenerCapasDeRuta(): readonly CapaDeRutaExpress[] {
    if (!this.capasDeRuta) {
      const instancia = this.adapterHost.httpAdapter.getInstance() as {
        router?: { stack: CapaDeRutaExpress[] };
      };
      this.capasDeRuta = instancia.router?.stack ?? [];
    }
    return this.capasDeRuta;
  }
}
