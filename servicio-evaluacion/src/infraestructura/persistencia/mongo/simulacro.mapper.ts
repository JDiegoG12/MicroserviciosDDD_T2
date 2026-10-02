import { NivelDificultad } from '../../../dominio/compartido/nivel-dificultad';
import { CriterioDeGeneracion } from '../../../dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../../dominio/simulacros/duracion-maxima';
import { PreguntasSeleccionadas } from '../../../dominio/simulacros/preguntas-seleccionadas';
import { Simulacro } from '../../../dominio/simulacros/simulacro';
import { SimulacroId } from '../../../dominio/simulacros/simulacro-id';
import { SimulacroDocumento } from './simulacro.schema';

/**
 * Traduce un documento de Mongo al agregado de dominio `Simulacro`.
 *
 * @param documento Documento leido de la coleccion `simulacros`.
 * @returns El agregado reconstruido.
 */
export function aSimulacro(documento: SimulacroDocumento): Simulacro {
  return Simulacro.reconstruir({
    simulacroId: SimulacroId.desde(documento._id),
    docenteId: documento.docenteId,
    nombre: documento.nombre,
    criterio: CriterioDeGeneracion.crear({
      competenciaIds: documento.criterio.competenciaIds,
      temaIds: documento.criterio.temaIds,
      subtemaIds: documento.criterio.subtemaIds,
      nivelesDificultad: documento.criterio.nivelesDificultad as NivelDificultad[],
    }),
    duracion: DuracionMaxima.enMinutos(documento.duracionMaximaMinutos),
    preguntas: PreguntasSeleccionadas.reconstruir(documento.preguntas),
    fechaCreacion: documento.fechaCreacion,
  });
}

/**
 * Traduce el agregado de dominio `Simulacro` al documento de Mongo que se
 * guarda en la coleccion `simulacros`.
 *
 * @param simulacro Agregado a persistir.
 * @returns El documento equivalente.
 */
export function aDocumentoSimulacro(simulacro: Simulacro): SimulacroDocumento {
  return {
    _id: simulacro.simulacroId.aTexto(),
    docenteId: simulacro.docenteId,
    nombre: simulacro.nombre,
    criterio: {
      competenciaIds: simulacro.criterio.competenciaIds,
      temaIds: simulacro.criterio.temaIds,
      subtemaIds: simulacro.criterio.subtemaIds,
      nivelesDificultad: simulacro.criterio.nivelesDificultad,
    },
    duracionMaximaMinutos: simulacro.duracion.enMinutosNumero(),
    preguntas: simulacro.preguntas.comoLista(),
    fechaCreacion: simulacro.fechaCreacion,
  };
}
