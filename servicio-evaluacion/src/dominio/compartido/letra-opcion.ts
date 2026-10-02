/**
 * Letra de una opcion de respuesta, en el formato exacto de
 * CONTRATOS.md seccion 4 ("A", "B", "C", "D": mayuscula, un caracter).
 */
export enum LetraOpcion {
  A = 'A',
  B = 'B',
  C = 'C',
  D = 'D',
}

/**
 * Comprueba si un texto es una letra de opcion valida (`A` a `D`).
 *
 * @param valor Texto a validar.
 * @returns `true` si `valor` es una de las cuatro letras validas.
 */
export function esLetraOpcion(valor: string): valor is LetraOpcion {
  return (
    valor === LetraOpcion.A ||
    valor === LetraOpcion.B ||
    valor === LetraOpcion.C ||
    valor === LetraOpcion.D
  );
}
