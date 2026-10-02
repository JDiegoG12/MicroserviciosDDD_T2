import { CodigoError } from '../../src/dominio/excepciones/codigo-error';

/**
 * Ejecuta una funcion que se espera que lance una excepcion sincrona y
 * devuelve el error capturado, para que la prueba afirme su `codigo`.
 *
 * @param funcion Funcion a ejecutar.
 * @returns El error capturado.
 * @throws Error Si `funcion` no lanza ninguna excepcion.
 */
export function capturarError(funcion: () => void): Error & { codigo?: CodigoError } {
  try {
    funcion();
  } catch (error) {
    return error as Error & { codigo?: CodigoError };
  }
  throw new Error('Se esperaba que la funcion lanzara una excepcion, pero no lanzo ninguna.');
}

/**
 * Version asincrona de `capturarError`, para casos de uso (cuyo `ejecutar`
 * siempre devuelve una Promise).
 *
 * @param funcion Funcion asincrona a ejecutar.
 * @returns El error capturado.
 * @throws Error Si `funcion` no lanza ninguna excepcion.
 */
export async function capturarErrorAsincrono(
  funcion: () => Promise<unknown>,
): Promise<Error & { codigo?: CodigoError }> {
  try {
    await funcion();
  } catch (error) {
    return error as Error & { codigo?: CodigoError };
  }
  throw new Error('Se esperaba que la funcion lanzara una excepcion, pero no lanzo ninguna.');
}
