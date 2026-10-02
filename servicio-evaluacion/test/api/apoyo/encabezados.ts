import { ID_DOCENTE, ID_ESTUDIANTE, ID_OTRO_ESTUDIANTE } from '../../apoyo/datos-de-prueba';

/** Encabezados de identidad de CONTRATOS.md 4.1 para un DOCENTE de prueba. */
export function encabezadosDocente(): Record<string, string> {
  return { 'X-Usuario-Id': ID_DOCENTE, 'X-Roles': 'DOCENTE' };
}

/** Encabezados de identidad para un ESTUDIANTE de prueba. */
export function encabezadosEstudiante(): Record<string, string> {
  return { 'X-Usuario-Id': ID_ESTUDIANTE, 'X-Roles': 'ESTUDIANTE' };
}

/** Encabezados de identidad para un segundo estudiante (no dueno). */
export function encabezadosOtroEstudiante(): Record<string, string> {
  return { 'X-Usuario-Id': ID_OTRO_ESTUDIANTE, 'X-Roles': 'ESTUDIANTE' };
}

/** Encabezados de identidad para un ADMINISTRADOR (rol siempre incorrecto en este servicio). */
export function encabezadosAdministrador(): Record<string, string> {
  return { 'X-Usuario-Id': '11111111-1111-4111-8111-000000000001', 'X-Roles': 'ADMINISTRADOR' };
}
