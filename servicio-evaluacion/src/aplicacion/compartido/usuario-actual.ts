import { AccesoDenegadoExcepcion } from '../../dominio/excepciones/acceso-denegado.excepcion';

/**
 * Roles validos del sistema, en el formato exacto de CONTRATOS.md 4.1.
 */
export enum Rol {
  ADMINISTRADOR = 'ADMINISTRADOR',
  AUTOR = 'AUTOR',
  REVISOR = 'REVISOR',
  DOCENTE = 'DOCENTE',
  ESTUDIANTE = 'ESTUDIANTE',
}

/**
 * Identidad de quien invoca un caso de uso, construida por la etapa 2 a
 * partir de los encabezados `X-Usuario-Id` y `X-Roles` (CONTRATOS.md 4.1).
 *
 * La verificacion de **rol** se hace en aplicacion (con `exigirAlgunRol`);
 * las reglas de **propiedad** (por ejemplo "solo el estudiante dueno
 * modifica su intento") se hacen en el dominio (CLAUDE.md del servicio).
 */
export class UsuarioActual {
  constructor(
    public readonly id: string,
    public readonly roles: readonly Rol[],
  ) {}

  /**
   * Comprueba si el usuario tiene alguno de los roles dados.
   *
   * @param roles Roles aceptados.
   * @returns `true` si el usuario tiene al menos uno de esos roles.
   */
  public tieneRol(...roles: readonly Rol[]): boolean {
    return roles.some((rol) => this.roles.includes(rol));
  }

  /**
   * Exige que el usuario tenga alguno de los roles dados; si no, lanza la
   * excepcion de dominio de acceso denegado (CONTRATOS.md 5.3, 403
   * `ACCESO_DENEGADO`).
   *
   * @param roles Roles aceptados para la operacion.
   * @throws AccesoDenegadoExcepcion Si el usuario no tiene ninguno de esos roles.
   */
  public exigirAlgunRol(...roles: readonly Rol[]): void {
    if (!this.tieneRol(...roles)) {
      throw new AccesoDenegadoExcepcion(
        `La operacion requiere alguno de estos roles: ${roles.join(', ')}.`,
      );
    }
  }
}
