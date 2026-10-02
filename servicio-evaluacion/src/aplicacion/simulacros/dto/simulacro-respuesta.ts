import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';

/**
 * Criterios de generacion de un Simulacro, en la forma del cuerpo de
 * `POST /simulacros` (CONTRATOS.md 8.3).
 */
export interface CriteriosRespuesta {
  readonly competenciaIds: readonly string[];
  readonly temaIds: readonly string[];
  readonly subtemaIds: readonly string[];
  readonly nivelesDificultad: readonly NivelDificultad[];
}

/**
 * Una pregunta seleccionada dentro de la respuesta de un Simulacro.
 */
export interface PreguntaSeleccionadaRespuesta {
  readonly preguntaId: string;
  readonly posicion: number;
}

/**
 * DTO de resultado para `DefinirSimulacroCasoUso` y
 * `ObtenerSimulacroCasoUso`, cuerpo de respuesta exacto de
 * `POST /simulacros` y `GET /simulacros/{simulacroId}` (CONTRATOS.md 8.3).
 */
export interface SimulacroRespuesta {
  readonly simulacroId: string;
  readonly nombre: string;
  readonly docenteId: string;
  readonly criterios: CriteriosRespuesta;
  readonly cantidadPreguntas: number;
  readonly duracionMaximaMinutos: number;
  readonly preguntas: readonly PreguntaSeleccionadaRespuesta[];
  readonly fechaCreacion: string;
}

/**
 * DTO de resultado para `ListarSimulacrosCasoUso`, elemento de la pagina de
 * `GET /simulacros` (CONTRATOS.md 8.3): el mismo `SimulacroRespuesta` sin
 * `preguntas`.
 */
export type SimulacroResumen = Omit<SimulacroRespuesta, 'preguntas'>;
