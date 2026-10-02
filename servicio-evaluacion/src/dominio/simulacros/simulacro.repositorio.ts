import { Simulacro } from './simulacro';
import { SimulacroId } from './simulacro-id';

/**
 * Puerto de salida del dominio para persistir y consultar Simulacro
 * (MODELO-DOMINIO.md B.7 §12.4).
 *
 * En este servicio todos los simulacros definidos estan disponibles para
 * todos los estudiantes (MODELO-DOMINIO.md A.3: "No hay grupos ni
 * asignaciones"), asi que no existe un metodo de busqueda restringida por
 * estudiante como en el Taller 1 original.
 */
export interface SimulacroRepositorio {
  /**
   * Guarda un Simulacro.
   *
   * @param simulacro El agregado a guardar.
   */
  guardar(simulacro: Simulacro): Promise<void>;

  /**
   * Busca un Simulacro por su identificador.
   *
   * @param simulacroId Id del simulacro.
   * @returns El Simulacro encontrado, o `null` si no existe (CONTRATOS.md
   * 8.3, 404 `SIMULACRO_NO_ENCONTRADO`).
   */
  obtenerPorId(simulacroId: SimulacroId): Promise<Simulacro | null>;

  /**
   * Lista todos los Simulacros definidos, para `GET /simulacros`
   * (CONTRATOS.md 8.3).
   *
   * @returns Todos los simulacros, en el orden que decida la implementacion.
   */
  listar(): Promise<Simulacro[]>;
}
