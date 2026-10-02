import { Rol, UsuarioActual } from '../../src/aplicacion/compartido/usuario-actual';
import { ID_DOCENTE, ID_ESTUDIANTE, ID_OTRO_ESTUDIANTE } from './datos-de-prueba';

/** Usuario de prueba con rol DOCENTE (CONTRATOS.md 4.2). */
export function crearDocente(): UsuarioActual {
  return new UsuarioActual(ID_DOCENTE, [Rol.DOCENTE]);
}

/** Usuario de prueba con rol ESTUDIANTE (CONTRATOS.md 4.2). */
export function crearEstudiante(): UsuarioActual {
  return new UsuarioActual(ID_ESTUDIANTE, [Rol.ESTUDIANTE]);
}

/** Un segundo estudiante, distinto del dueno habitual de un intento. */
export function crearOtroEstudiante(): UsuarioActual {
  return new UsuarioActual(ID_OTRO_ESTUDIANTE, [Rol.ESTUDIANTE]);
}

/** Usuario de prueba con rol ADMINISTRADOR, util para probar rechazos de rol. */
export function crearAdministrador(): UsuarioActual {
  return new UsuarioActual('11111111-1111-4111-8111-000000000001', [Rol.ADMINISTRADOR]);
}
