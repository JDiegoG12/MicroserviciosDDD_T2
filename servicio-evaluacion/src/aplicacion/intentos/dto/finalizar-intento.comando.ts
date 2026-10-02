import { UsuarioActual } from '../../compartido/usuario-actual';

/**
 * Comando de entrada de `FinalizarIntentoCasoUso` (CU-14 + CU-15), futuro
 * cuerpo de `POST /intentos/{intentoId}/finalizacion` (CONTRATOS.md 8.3).
 */
export interface FinalizarIntentoComando {
  readonly usuario: UsuarioActual;
  readonly intentoId: string;
}
