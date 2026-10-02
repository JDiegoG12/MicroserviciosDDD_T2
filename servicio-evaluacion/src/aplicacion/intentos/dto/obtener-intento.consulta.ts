import { UsuarioActual } from '../../compartido/usuario-actual';

/**
 * Consulta de entrada de `ObtenerIntentoCasoUso`, futura
 * `GET /intentos/{intentoId}` (CONTRATOS.md 8.3).
 */
export interface ObtenerIntentoConsulta {
  readonly usuario: UsuarioActual;
  readonly intentoId: string;
}
