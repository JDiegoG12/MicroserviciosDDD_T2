import { ValidationError } from '@nestjs/common';
import { ErrorDeCampo } from '../excepciones/solicitud-invalida-http.excepcion';

/**
 * Aplana el arbol de `ValidationError` que produce `class-validator` a la
 * lista plana `{ campo, mensaje }` de CONTRATOS.md 5.2.
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
      for (const mensaje of Object.values(error.constraints)) {
        resultado.push({ campo, mensaje });
      }
    }
    if (error.children && error.children.length > 0) {
      resultado.push(...aplanarErroresDeValidacion(error.children, campo));
    }
  }
  return resultado;
}
