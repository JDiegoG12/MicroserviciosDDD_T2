import { AsyncLocalStorage } from 'node:async_hooks';

interface DatosDeCorrelacion {
  readonly idCorrelacion: string;
}

/**
 * Contexto asincrono que lleva el `idCorrelacion` de la peticion HTTP (o
 * del evento consumido) actual a traves de toda la cadena de llamadas,
 * sin que `aplicacion` ni `dominio` tengan que recibirlo como parametro
 * (CONTRATOS.md 4.1: "Se propaga a metadatos gRPC, al campo idCorrelacion
 * del evento y a todas las lineas de log").
 *
 * Lo usan: el middleware de correlacion (lo escribe), el logger
 * estructurado (lo lee para cada linea) y `PublicadorEventosRabbitMqAdaptador`
 * (lo lee para armar el sobre del evento saliente).
 */
const almacenamiento = new AsyncLocalStorage<DatosDeCorrelacion>();

/**
 * Ejecuta `funcion` con un `idCorrelacion` fijo en el contexto asincrono.
 *
 * @param idCorrelacion Id de correlacion a propagar.
 * @param funcion Funcion (sincrona o asincrona) a ejecutar dentro del contexto.
 * @returns El resultado de `funcion`.
 */
export function ejecutarConCorrelacion<T>(idCorrelacion: string, funcion: () => T): T {
  return almacenamiento.run({ idCorrelacion }, funcion);
}

/**
 * Id de correlacion del contexto asincrono actual.
 *
 * @returns El `idCorrelacion` activo, o `null` si no se establecio ninguno
 * (por ejemplo, en el arranque del proceso).
 */
export function obtenerIdCorrelacionActual(): string | null {
  return almacenamiento.getStore()?.idCorrelacion ?? null;
}
