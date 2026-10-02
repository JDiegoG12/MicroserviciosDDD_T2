import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';
import { UsuarioActual } from '../../compartido/usuario-actual';

/**
 * Comando de entrada de `DefinirSimulacroCasoUso` (CU-13), futuro cuerpo de
 * `POST /simulacros` (CONTRATOS.md 8.3).
 */
export interface DefinirSimulacroComando {
  readonly usuario: UsuarioActual;
  readonly nombre: string;
  readonly criterios: {
    readonly competenciaIds: readonly string[];
    readonly temaIds: readonly string[];
    readonly subtemaIds: readonly string[];
    readonly nivelesDificultad: readonly NivelDificultad[];
  };
  readonly cantidadPreguntas: number;
  readonly duracionMaximaMinutos: number;
}
