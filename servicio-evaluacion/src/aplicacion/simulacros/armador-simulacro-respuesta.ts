import { aTextoIso } from '../compartido/formato-fecha';
import { Simulacro } from '../../dominio/simulacros/simulacro';
import { SimulacroRespuesta, SimulacroResumen } from './dto/simulacro-respuesta';

/**
 * Construye el DTO `SimulacroRespuesta` a partir de un agregado Simulacro.
 * Compartido por `DefinirSimulacroCasoUso` y `ObtenerSimulacroCasoUso` para
 * no repetir el mapeo.
 *
 * @param simulacro El agregado ya guardado.
 * @returns El DTO de respuesta.
 */
export function armarSimulacroRespuesta(simulacro: Simulacro): SimulacroRespuesta {
  return {
    simulacroId: simulacro.simulacroId.aTexto(),
    nombre: simulacro.nombre,
    docenteId: simulacro.docenteId,
    criterios: {
      competenciaIds: simulacro.criterio.competenciaIds,
      temaIds: simulacro.criterio.temaIds,
      subtemaIds: simulacro.criterio.subtemaIds,
      nivelesDificultad: simulacro.criterio.nivelesDificultad,
    },
    cantidadPreguntas: simulacro.preguntas.ids().length,
    duracionMaximaMinutos: simulacro.duracion.enMinutosNumero(),
    preguntas: simulacro.preguntas.comoLista(),
    fechaCreacion: aTextoIso(simulacro.fechaCreacion),
  };
}

/**
 * Construye el DTO `SimulacroResumen` (CONTRATOS.md 8.3: "el mismo objeto
 * sin preguntas"), usado por `ListarSimulacrosCasoUso`.
 *
 * @param simulacro El agregado ya guardado.
 * @returns El DTO de resumen, sin `preguntas`.
 */
export function armarSimulacroResumen(simulacro: Simulacro): SimulacroResumen {
  return {
    simulacroId: simulacro.simulacroId.aTexto(),
    nombre: simulacro.nombre,
    docenteId: simulacro.docenteId,
    criterios: {
      competenciaIds: simulacro.criterio.competenciaIds,
      temaIds: simulacro.criterio.temaIds,
      subtemaIds: simulacro.criterio.subtemaIds,
      nivelesDificultad: simulacro.criterio.nivelesDificultad,
    },
    cantidadPreguntas: simulacro.preguntas.ids().length,
    duracionMaximaMinutos: simulacro.duracion.enMinutosNumero(),
    fechaCreacion: aTextoIso(simulacro.fechaCreacion),
  };
}
