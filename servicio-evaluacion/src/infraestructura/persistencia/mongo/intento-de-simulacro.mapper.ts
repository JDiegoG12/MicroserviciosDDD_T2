import { LetraOpcion } from '../../../dominio/compartido/letra-opcion';
import { Calificacion } from '../../../dominio/intentos/calificacion';
import { EstadoIntento } from '../../../dominio/intentos/estado-intento';
import { FinalizadorIntento } from '../../../dominio/intentos/eventos/intento-de-simulacro-finalizado.evento';
import { IntentoDeSimulacro } from '../../../dominio/intentos/intento-de-simulacro';
import { IntentoId } from '../../../dominio/intentos/intento-id';
import { RespuestaDelEstudiante } from '../../../dominio/intentos/respuesta-del-estudiante';
import { PreguntasSeleccionadas } from '../../../dominio/simulacros/preguntas-seleccionadas';
import { SimulacroId } from '../../../dominio/simulacros/simulacro-id';
import { IntentoDeSimulacroDocumento } from './intento-de-simulacro.schema';

/**
 * Traduce un documento de Mongo al agregado de dominio `IntentoDeSimulacro`.
 *
 * @param documento Documento leido de la coleccion `intentos`.
 * @returns El agregado reconstruido.
 */
export function aIntentoDeSimulacro(documento: IntentoDeSimulacroDocumento): IntentoDeSimulacro {
  const respuestas = new Map<string, RespuestaDelEstudiante>(
    documento.respuestas.map((respuesta) => [
      respuesta.preguntaId,
      { preguntaId: respuesta.preguntaId, letraSeleccionada: respuesta.letraSeleccionada as LetraOpcion },
    ]),
  );

  return IntentoDeSimulacro.reconstruir({
    intentoId: IntentoId.desde(documento._id),
    simulacroId: SimulacroId.desde(documento.simulacroId),
    estudianteId: documento.estudianteId,
    preguntasDelSimulacro: PreguntasSeleccionadas.reconstruir(documento.preguntasDelSimulacro),
    fechaInicio: documento.fechaInicio,
    fechaLimite: documento.fechaLimite,
    estado: documento.estado as EstadoIntento,
    fechaFinalizacion: documento.fechaFinalizacion,
    finalizadoPor: documento.finalizadoPor as FinalizadorIntento | null,
    respuestas,
    calificacion: documento.calificacion ? Calificacion.reconstruir(documento.calificacion) : null,
  });
}

/**
 * Traduce el agregado de dominio `IntentoDeSimulacro` al documento de Mongo
 * que se guarda en la coleccion `intentos`.
 *
 * @param intento Agregado a persistir.
 * @returns El documento equivalente.
 */
export function aDocumentoIntentoDeSimulacro(intento: IntentoDeSimulacro): IntentoDeSimulacroDocumento {
  const calificacion = intento.calificacion;
  return {
    _id: intento.intentoId.aTexto(),
    simulacroId: intento.simulacroId.aTexto(),
    estudianteId: intento.estudianteId,
    preguntasDelSimulacro: intento.preguntasSeleccionadas(),
    fechaInicio: intento.fechaInicio,
    fechaLimite: intento.fechaLimite,
    estado: intento.estado,
    fechaFinalizacion: intento.fechaFinalizacion,
    finalizadoPor: intento.finalizadoPor,
    respuestas: intento.todasLasRespuestas(),
    calificacion: calificacion
      ? {
          totalPreguntas: calificacion.totalPreguntas,
          correctas: calificacion.correctas,
          puntaje: calificacion.puntaje,
          desglosePorCompetencia: [...calificacion.desglosePorCompetencia],
        }
      : null,
  };
}
