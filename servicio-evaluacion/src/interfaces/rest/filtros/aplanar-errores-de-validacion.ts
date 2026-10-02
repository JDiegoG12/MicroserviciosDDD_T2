import { ValidationError } from '@nestjs/common';
import { ErrorDeCampo } from '../excepciones/solicitud-invalida-http.excepcion';

/**
 * Formateador comun (CONTRATOS.md 3.1: "todo en espanol") que traduce la
 * restriccion de `class-validator` que fallo (la llave de
 * `error.constraints`, por ejemplo `isNotEmpty` o `isUuid`) a un mensaje en
 * espanol. Cubre las restricciones que usan los DTOs de este servicio; una
 * restriccion sin traduccion deja pasar el mensaje original de
 * `class-validator` (en ingles) en vez de fallar.
 */
const TRADUCCIONES_POR_RESTRICCION: Record<string, (campo: string) => string> = {
  isNotEmpty: (campo) => `${campo} no debe estar vacio.`,
  isString: (campo) => `${campo} debe ser una cadena de texto.`,
  isInt: (campo) => `${campo} debe ser un numero entero.`,
  isArray: (campo) => `${campo} debe ser una lista.`,
  isUuid: (campo) => `${campo} debe ser un UUID valido.`,
  isEnum: (campo) => `${campo} tiene un valor no permitido.`,
  isIn: (campo) => `${campo} tiene un valor no permitido.`,
  isPositive: (campo) => `${campo} debe ser un numero positivo.`,
  min: (campo) => `${campo} debe ser mayor o igual al minimo permitido.`,
  max: (campo) => `${campo} debe ser menor o igual al maximo permitido.`,
};

function traducirMensaje(campo: string, restriccion: string, mensajeOriginal: string): string {
  const traductor = TRADUCCIONES_POR_RESTRICCION[restriccion];
  return traductor ? traductor(campo) : mensajeOriginal;
}

/**
 * Aplana el arbol de `ValidationError` que produce `class-validator` a la
 * lista plana `{ campo, mensaje }` de CONTRATOS.md 5.2, con los mensajes
 * traducidos al espanol (CONTRATOS.md 3.1).
 *
 * @param errores Errores de validacion, posiblemente anidados (DTOs dentro
 * de DTOs).
 * @param prefijo Prefijo de propiedad acumulado (uso interno, para la
 * recursion).
 * @returns La lista plana de errores de campo.
 */
export function aplanarErroresDeValidacion(
  errores: readonly ValidationError[],
  prefijo = '',
): ErrorDeCampo[] {
  const resultado: ErrorDeCampo[] = [];
  for (const error of errores) {
    const campo = prefijo ? `${prefijo}.${error.property}` : error.property;
    if (error.constraints) {
      for (const [restriccion, mensaje] of Object.entries(error.constraints)) {
        resultado.push({ campo, mensaje: traducirMensaje(campo, restriccion, mensaje) });
      }
    }
    if (error.children && error.children.length > 0) {
      resultado.push(...aplanarErroresDeValidacion(error.children, campo));
    }
  }
  return resultado;
}
