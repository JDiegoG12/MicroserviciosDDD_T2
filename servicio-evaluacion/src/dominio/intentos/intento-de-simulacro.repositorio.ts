import { IntentoDeSimulacro } from './intento-de-simulacro';
import { IntentoId } from './intento-id';

/**
 * Puerto de salida del dominio para persistir y consultar
 * IntentoDeSimulacro (MODELO-DOMINIO.md B.7 §12.5, version reducida para
 * este servicio: CONTRATOS.md 8.3 solo necesita `guardar` y
 * `obtenerPorId`).
 */
export interface IntentoDeSimulacroRepositorio {
  /**
   * Guarda un IntentoDeSimulacro. Se llama tanto al iniciarlo como en cada
   * cambio de estado posterior (responder, finalizar, calificar).
   *
   * @param intento El agregado a guardar.
   */
  guardar(intento: IntentoDeSimulacro): Promise<void>;

  /**
   * Busca un IntentoDeSimulacro por su identificador.
   *
   * @param intentoId Id del intento.
   * @returns El intento encontrado, o `null` si no existe (CONTRATOS.md
   * 8.3, 404 `INTENTO_NO_ENCONTRADO`).
   */
  obtenerPorId(intentoId: IntentoId): Promise<IntentoDeSimulacro | null>;
}
