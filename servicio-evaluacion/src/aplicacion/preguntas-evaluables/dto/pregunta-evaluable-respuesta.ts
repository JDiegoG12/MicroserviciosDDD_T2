import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';
import {
  ClasificacionDePregunta,
  OpcionDePregunta,
} from '../../../dominio/preguntas-evaluables/contenido-de-pregunta';

/**
 * DTO de resultado para `ConsultarPreguntasEvaluablesCasoUso`, cuerpo de
 * `GET /preguntas-evaluables` (CONTRATOS.md 8.3): una pagina de copias
 * locales, **sin** `letraCorrecta`. `motivoArchivo` y `fechaArchivado` son
 * `null` si la pregunta nunca se archivo.
 */
export interface PreguntaEvaluableRespuesta {
  readonly preguntaId: string;
  readonly contexto: string;
  readonly preguntaDirecta: string;
  readonly opciones: readonly OpcionDePregunta[];
  readonly clasificacion: ClasificacionDePregunta;
  readonly nivelDificultad: NivelDificultad;
  readonly estado: 'PUBLICADA' | 'ARCHIVADA';
  readonly fechaPublicacion: string;
  readonly motivoArchivo: string | null;
  readonly fechaArchivado: string | null;
  readonly fechaActualizacion: string;
}
