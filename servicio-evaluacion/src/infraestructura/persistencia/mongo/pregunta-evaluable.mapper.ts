import { LetraOpcion } from '../../../dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';
import { ContenidoDePregunta } from '../../../dominio/preguntas-evaluables/contenido-de-pregunta';
import { EstadoPreguntaEvaluable, PreguntaEvaluable } from '../../../dominio/preguntas-evaluables/pregunta-evaluable';
import { PreguntaEvaluableDocumento } from './pregunta-evaluable.schema';

function aContenido(contenido: PreguntaEvaluableDocumento['contenido']): ContenidoDePregunta | null {
  if (!contenido) {
    return null;
  }
  return {
    contexto: contenido.contexto,
    preguntaDirecta: contenido.preguntaDirecta,
    opciones: contenido.opciones.map((opcion) => ({
      letra: opcion.letra as LetraOpcion,
      texto: opcion.texto,
    })),
    letraCorrecta: contenido.letraCorrecta as LetraOpcion,
    clasificacion: contenido.clasificacion,
    nivelDificultad: contenido.nivelDificultad as NivelDificultad,
    fechaPublicacion: contenido.fechaPublicacion,
  };
}

/**
 * Traduce un documento de Mongo a la entidad de dominio `PreguntaEvaluable`.
 *
 * @param documento Documento leido de la coleccion `preguntas_evaluables`.
 * @returns La entidad de dominio reconstruida.
 */
export function aPreguntaEvaluable(documento: PreguntaEvaluableDocumento): PreguntaEvaluable {
  return PreguntaEvaluable.reconstruir({
    preguntaId: documento._id,
    contenido: aContenido(documento.contenido),
    estado: documento.estado as EstadoPreguntaEvaluable,
    motivoArchivo: documento.motivoArchivo,
    fechaArchivado: documento.fechaArchivado,
    fechaActualizacion: documento.fechaActualizacion,
  });
}

/**
 * Traduce una entidad de dominio `PreguntaEvaluable` al documento de Mongo
 * que se guarda en la coleccion `preguntas_evaluables`.
 *
 * @param pregunta Entidad de dominio a persistir.
 * @returns El documento equivalente.
 */
export function aDocumentoPreguntaEvaluable(pregunta: PreguntaEvaluable): PreguntaEvaluableDocumento {
  return {
    _id: pregunta.preguntaId,
    contenido: pregunta.contenido,
    estado: pregunta.estado,
    motivoArchivo: pregunta.motivoArchivo,
    fechaArchivado: pregunta.fechaArchivado,
    fechaActualizacion: pregunta.fechaActualizacion,
  };
}
