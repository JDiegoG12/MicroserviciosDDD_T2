/**
 * Nivel de dificultad de una Pregunta, enumeracion cerrada acordada en D-10
 * del Taller 1 (MODELO-DOMINIO.md, Decision D-10) y CONTRATOS.md 7.4.
 */
export enum NivelDificultad {
  BAJO = 'BAJO',
  MEDIO = 'MEDIO',
  ALTO = 'ALTO',
}

/**
 * Comprueba si un texto es uno de los tres niveles de dificultad validos.
 *
 * @param valor Texto a validar.
 * @returns `true` si `valor` es `BAJO`, `MEDIO` o `ALTO`.
 */
export function esNivelDificultad(valor: string): valor is NivelDificultad {
  return (
    valor === NivelDificultad.BAJO ||
    valor === NivelDificultad.MEDIO ||
    valor === NivelDificultad.ALTO
  );
}
