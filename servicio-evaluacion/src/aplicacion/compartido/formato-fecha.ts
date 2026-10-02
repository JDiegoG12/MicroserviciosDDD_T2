/**
 * Convierte una fecha a texto ISO-8601 en UTC terminado en `Z`, sin
 * milisegundos (CONTRATOS.md seccion 4: `"2026-10-01T15:30:00Z"`).
 *
 * @param fecha Fecha a convertir.
 * @returns El texto ISO-8601 en UTC.
 */
export function aTextoIso(fecha: Date): string {
  return fecha.toISOString().replace(/\.\d{3}Z$/, 'Z');
}
