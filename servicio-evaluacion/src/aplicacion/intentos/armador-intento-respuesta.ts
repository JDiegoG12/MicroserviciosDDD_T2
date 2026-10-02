import { aTextoIso } from '../compartido/formato-fecha';
import { InconsistenciaDeDatosExcepcion } from '../../dominio/excepciones/inconsistencia-de-datos.excepcion';
import { IntentoDeSimulacro } from '../../dominio/intentos/intento-de-simulacro';
import { PreguntaEvaluable } from '../../dominio/preguntas-evaluables/pregunta-evaluable';
import { IntentoRespuesta, PreguntaDelIntentoRespuesta } from './dto/intento-respuesta';

/**
 * Construye el DTO `IntentoRespuesta` a partir de un IntentoDeSimulacro y
 * las copias locales de sus preguntas.
 *
 * Arma el contexto, el enunciado y las opciones de cada pregunta a partir
 * de `PreguntaEvaluable.vistaSinClave()`, que nunca incluye
 * `letraCorrecta` (CONTRATOS.md 8.3 y 11.3).
 *
 * @param intento El intento, ya actualizado con el vencimiento perezoso.
 * @param preguntas Las copias locales de todas las preguntas del simulacro
 * de este intento.
 * @returns El DTO de respuesta.
 * @throws InconsistenciaDeDatosExcepcion Si falta la copia local de alguna
 * pregunta seleccionada del intento.
 */
export function armarIntentoRespuesta(
  intento: IntentoDeSimulacro,
  preguntas: readonly PreguntaEvaluable[],
): IntentoRespuesta {
  const preguntasPorId = new Map(preguntas.map((pregunta) => [pregunta.preguntaId, pregunta]));

  const preguntasRespuesta: PreguntaDelIntentoRespuesta[] = intento.preguntasSeleccionadas().map((seleccionada) => {
    const pregunta = preguntasPorId.get(seleccionada.preguntaId);
    if (!pregunta) {
      throw new InconsistenciaDeDatosExcepcion(
        `No se encontro la copia local de la pregunta ${seleccionada.preguntaId} del intento.`,
      );
    }
    const vista = pregunta.vistaSinClave();
    return {
      preguntaId: seleccionada.preguntaId,
      posicion: seleccionada.posicion,
      contexto: vista.contexto,
      preguntaDirecta: vista.preguntaDirecta,
      opciones: vista.opciones,
    };
  });

  const calificacion = intento.calificacion;

  return {
    intentoId: intento.intentoId.aTexto(),
    simulacroId: intento.simulacroId.aTexto(),
    estudianteId: intento.estudianteId,
    estado: intento.estado,
    fechaInicio: aTextoIso(intento.fechaInicio),
    fechaLimite: aTextoIso(intento.fechaLimite),
    fechaFinalizacion: intento.fechaFinalizacion ? aTextoIso(intento.fechaFinalizacion) : null,
    finalizadoPor: intento.finalizadoPor,
    preguntas: preguntasRespuesta,
    respuestas: intento.todasLasRespuestas().map((respuesta) => ({
      preguntaId: respuesta.preguntaId,
      letraSeleccionada: respuesta.letraSeleccionada,
    })),
    calificacion: calificacion
      ? {
          totalPreguntas: calificacion.totalPreguntas,
          correctas: calificacion.correctas,
          puntaje: calificacion.puntaje,
          desglosePorCompetencia: calificacion.desglosePorCompetencia,
        }
      : null,
  };
}
