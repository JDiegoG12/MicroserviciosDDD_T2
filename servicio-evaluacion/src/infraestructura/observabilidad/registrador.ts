import { obtenerIdCorrelacionActual } from '../correlacion/contexto-correlacion';

/**
 * Niveles de severidad de una linea de log.
 */
type NivelDeLog = 'info' | 'warn' | 'error' | 'debug';

/** Valor de `idCorrelacion` cuando la linea no ocurre dentro de una peticion o un mensaje. */
const SIN_CORRELACION = '-';

/**
 * Registra una linea de log estructurada (JSON) que siempre incluye el
 * `idCorrelacion` del contexto asincrono actual (CONTRATOS.md 4.1: "se
 * propaga ... a todas las lineas de log"), o `"-"` cuando no hay una
 * peticion ni un mensaje en curso (por ejemplo, durante el arranque).
 *
 * Se usa en lugar de `console.log` directo en toda la capa de
 * `infraestructura` e `interfaces`, para que ninguna linea de log del
 * servicio quede sin `idCorrelacion`. `main.ts` tambien enruta por aqui
 * los logs propios de NestJS (`app.useLogger`), para que todas las lineas
 * compartan el mismo formato.
 *
 * @param nivel Severidad de la linea.
 * @param mensaje Mensaje legible.
 * @param detalle Datos adicionales a incluir en la linea (opcional).
 */
export function registrar(nivel: NivelDeLog, mensaje: string, detalle?: Record<string, unknown>): void {
  const linea = {
    nivel,
    mensaje,
    idCorrelacion: obtenerIdCorrelacionActual() ?? SIN_CORRELACION,
    fecha: new Date().toISOString(),
    ...detalle,
  };
  const texto = JSON.stringify(linea);
  if (nivel === 'error') {
    // eslint-disable-next-line no-console
    console.error(texto);
  } else if (nivel === 'warn') {
    // eslint-disable-next-line no-console
    console.warn(texto);
  } else {
    // eslint-disable-next-line no-console
    console.log(texto);
  }
}
