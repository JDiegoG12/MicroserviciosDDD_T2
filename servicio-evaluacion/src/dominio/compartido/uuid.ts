import { randomUUID } from 'node:crypto';

/**
 * Patron de un UUID v4 en texto, en minusculas y con guiones
 * (CONTRATOS.md seccion 4).
 */
const PATRON_UUID_V4 =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

/**
 * Comprueba si un texto es un UUID v4 valido en el formato exacto que exige
 * CONTRATOS.md (minusculas, con guiones).
 *
 * @param valor Texto a validar.
 * @returns `true` si `valor` cumple el formato.
 */
export function esUuidValido(valor: string): boolean {
  return PATRON_UUID_V4.test(valor);
}

/**
 * Normaliza un UUID canonico (8-4-4-4-12 con guiones) a minusculas, para
 * aceptar el mismo identificador escrito en mayusculas (CONTRATOS.md
 * seccion 4) en rutas, encabezados y cuerpos.
 *
 * Solo normaliza mayusculas/minusculas: una variante no canonica (con
 * llaves, el prefijo `urn:uuid:` o sin guiones) sigue siendo invalida,
 * aunque sus letras ya esten en minusculas.
 *
 * @param valor Texto a normalizar.
 * @returns El UUID en minusculas si `valor` es valido (sin distinguir
 * mayusculas de minusculas), o `null` si no lo es.
 */
export function normalizarUuid(valor: string): string | null {
  const candidato = valor.toLowerCase();
  return PATRON_UUID_V4.test(candidato) ? candidato : null;
}

/**
 * Genera un identificador nuevo en formato UUID v4.
 *
 * Unica dependencia permitida de la libreria estandar del lenguaje dentro
 * del dominio (CLAUDE.md del servicio: "el dominio no importa nada de
 * frameworks ni de librerias de infraestructura. Excepcion: la libreria
 * estandar del lenguaje").
 *
 * @returns Un UUID v4 en minusculas y con guiones.
 */
export function generarUuid(): string {
  return randomUUID();
}
