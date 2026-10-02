import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { generarUuid } from '../../src/dominio/compartido/uuid';
import { crearContenidoDePregunta } from '../../src/dominio/preguntas-evaluables/contenido-de-pregunta';
import { PreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { ID_COMPETENCIA_RAZONAMIENTO, ID_SUBTEMA_MEDIDAS_CENTRALES, ID_TEMA_ESTADISTICA } from './datos-de-prueba';

/**
 * Crea una PreguntaEvaluable en estado PUBLICADA con valores por defecto
 * razonables, para no repetir el armado completo en cada prueba.
 *
 * @param datos Campos a sobrescribir.
 * @returns La PreguntaEvaluable creada.
 */
export function crearPreguntaPublicada(datos?: {
  preguntaId?: string;
  competenciaId?: string;
  temaId?: string;
  subtemaId?: string;
  nivelDificultad?: NivelDificultad;
  letraCorrecta?: LetraOpcion;
  fechaPublicacion?: Date;
  ahora?: Date;
}): PreguntaEvaluable {
  const contenido = crearContenidoDePregunta({
    contexto: 'Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.',
    preguntaDirecta: 'Cual es la moda del conjunto de notas?',
    opciones: [
      { letra: LetraOpcion.A, texto: '3,0' },
      { letra: LetraOpcion.B, texto: '3,8' },
      { letra: LetraOpcion.C, texto: '4,0' },
      { letra: LetraOpcion.D, texto: '4,5' },
    ],
    letraCorrecta: datos?.letraCorrecta ?? LetraOpcion.C,
    clasificacion: {
      competenciaId: datos?.competenciaId ?? ID_COMPETENCIA_RAZONAMIENTO,
      temaId: datos?.temaId ?? ID_TEMA_ESTADISTICA,
      subtemaId: datos?.subtemaId ?? ID_SUBTEMA_MEDIDAS_CENTRALES,
    },
    nivelDificultad: datos?.nivelDificultad ?? NivelDificultad.BAJO,
    fechaPublicacion: datos?.fechaPublicacion ?? new Date('2026-10-01T15:30:00Z'),
  });

  return PreguntaEvaluable.desdePublicacion(
    datos?.preguntaId ?? generarUuid(),
    contenido,
    datos?.ahora ?? new Date('2026-10-01T15:30:00Z'),
  );
}
