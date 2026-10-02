import { OpcionDePregunta } from '../../../dominio/preguntas-evaluables/contenido-de-pregunta';

/**
 * Una pregunta del intento, con el contexto, el enunciado y las opciones de
 * la copia local, pero **nunca** `letraCorrecta` (CONTRATOS.md 8.3 y 11.3).
 */
export interface PreguntaDelIntentoRespuesta {
  readonly preguntaId: string;
  readonly posicion: number;
  readonly contexto: string;
  readonly preguntaDirecta: string;
  readonly opciones: readonly OpcionDePregunta[];
}

/**
 * Respuesta ya registrada por el estudiante para una pregunta del intento.
 */
export interface RespuestaDelEstudianteRespuesta {
  readonly preguntaId: string;
  readonly letraSeleccionada: string;
}

/**
 * Calificacion de un intento, igual a la del evento `IntentoDeSimulacroCalificado`
 * (CONTRATOS.md 7.6), con su desglose.
 */
export interface CalificacionRespuesta {
  readonly totalPreguntas: number;
  readonly correctas: number;
  readonly puntaje: number;
  readonly desglosePorCompetencia: readonly {
    readonly competenciaId: string;
    readonly totalPreguntas: number;
    readonly correctas: number;
    readonly puntaje: number;
  }[];
}

/**
 * DTO de resultado para los casos de uso de IntentoDeSimulacro, futuro
 * cuerpo de respuesta de `POST /simulacros/{simulacroId}/intentos`,
 * `PUT /intentos/{intentoId}/respuestas/{preguntaId}`,
 * `POST /intentos/{intentoId}/finalizacion` y `GET /intentos/{intentoId}`
 * (CONTRATOS.md 8.3).
 */
export interface IntentoRespuesta {
  readonly intentoId: string;
  readonly simulacroId: string;
  readonly estudianteId: string;
  readonly estado: 'EN_CURSO' | 'FINALIZADO' | 'CALIFICADO';
  readonly fechaInicio: string;
  readonly fechaLimite: string;
  readonly fechaFinalizacion: string | null;
  readonly finalizadoPor: 'ESTUDIANTE' | 'TIEMPO_AGOTADO' | null;
  readonly preguntas: readonly PreguntaDelIntentoRespuesta[];
  readonly respuestas: readonly RespuestaDelEstudianteRespuesta[];
  readonly calificacion: CalificacionRespuesta | null;
}
