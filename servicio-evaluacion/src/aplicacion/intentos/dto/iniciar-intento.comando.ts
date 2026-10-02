import { UsuarioActual } from '../../compartido/usuario-actual';

/**
 * Comando de entrada de `IniciarIntentoCasoUso` (CU-14), futuro cuerpo de
 * `POST /simulacros/{simulacroId}/intentos` (CONTRATOS.md 8.3).
 */
export interface IniciarIntentoComando {
  readonly usuario: UsuarioActual;
  readonly simulacroId: string;
}
