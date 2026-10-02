import { AsyncLocalStorage } from 'node:async_hooks';
import { generarUuid, normalizarUuid } from '../../dominio/compartido/uuid';

interface DatosDeCorrelacion {
  readonly idCorrelacion: string;
}

/** Nombre del encabezado HTTP de correlacion (CONTRATOS.md 4.1). */
export const ENCABEZADO_CORRELACION = 'X-Id-Correlacion';

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

/**
 * Resuelve el `idCorrelacion` a partir del encabezado `X-Id-Correlacion`
 * recibido (CONTRATOS.md 4.1): si no llega, o si no es un UUID valido, se
 * genera uno nuevo. Un UUID canonico en mayusculas se acepta y se
 * normaliza a minusculas (CONTRATOS.md seccion 4).
 *
 * La usan tanto `CorrelacionMiddleware` (caso normal) como
 * `FiltroExcepcionesGlobal` (caso de respaldo: una peticion que fallo
 * antes de que el middleware llegara a correr, por ejemplo un cuerpo JSON
 * ilegible, ya que el body-parser de Express corre antes que los
 * middlewares de Nest).
 *
 * @param encabezadoCrudo Valor crudo del encabezado `X-Id-Correlacion`, si llego.
 * @returns El `idCorrelacion` a usar, y si se reemplazo el valor recibido
 * por ser invalido.
 */
export function resolverIdCorrelacion(encabezadoCrudo: string | undefined): {
  readonly idCorrelacion: string;
  readonly eraInvalido: boolean;
} {
  if (!encabezadoCrudo) {
    return { idCorrelacion: generarUuid(), eraInvalido: false };
  }
  const normalizado = normalizarUuid(encabezadoCrudo);
  return normalizado
    ? { idCorrelacion: normalizado, eraInvalido: false }
    : { idCorrelacion: generarUuid(), eraInvalido: true };
}
