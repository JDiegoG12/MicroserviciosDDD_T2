import { NivelDificultad } from '../compartido/nivel-dificultad';
import { ClasificacionDePregunta, OpcionDePregunta } from './contenido-de-pregunta';

/**
 * Vista de una PreguntaEvaluable para el estudiante o el docente, **sin**
 * `letraCorrecta` (CONTRATOS.md 7.4, 8.3 y 11.3: "Expone un metodo que
 * devuelve la vista para el estudiante, sin letraCorrecta").
 *
 * Forma exacta del elemento de `GET /preguntas-evaluables` (CONTRATOS.md
 * 8.3): incluye `motivoArchivo` y `fechaArchivado` (`null` si la pregunta
 * nunca se archivo).
 */
export interface VistaPreguntaEvaluable {
  readonly preguntaId: string;
  readonly contexto: string;
  readonly preguntaDirecta: string;
  readonly opciones: readonly OpcionDePregunta[];
  readonly clasificacion: ClasificacionDePregunta;
  readonly nivelDificultad: NivelDificultad;
  readonly estado: 'PUBLICADA' | 'ARCHIVADA';
  readonly fechaPublicacion: Date;
  readonly motivoArchivo: string | null;
  readonly fechaArchivado: Date | null;
  readonly fechaActualizacion: Date;
}
