import { CierreDeIntento } from '../../src/aplicacion/intentos/cierre-de-intento';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { CalificadorSimulacroServicio } from '../../src/dominio/servicios/calificador-simulacro.servicio';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { IntentoDeSimulacroRepositorioEnMemoria } from '../dobles/intento-de-simulacro-repositorio-en-memoria';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { PublicadorEventosEnMemoria } from '../dobles/publicador-eventos-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';
import { SimulacroRepositorioEnMemoria } from '../dobles/simulacro-repositorio-en-memoria';
import { ID_DOCENTE } from './datos-de-prueba';
import { crearPreguntaPublicada } from './fabrica-preguntas';

/** Instante de inicio por defecto de los entornos de prueba de intentos. */
export const FECHA_INICIO_POR_DEFECTO = new Date('2026-10-01T15:40:00Z');

/**
 * Construye un entorno completo (repositorios en memoria, servicios de
 * dominio y un Simulacro ya guardado con sus preguntas) para probar los
 * casos de uso de IntentoDeSimulacro sin repetir el montaje en cada
 * prueba.
 *
 * @param cantidadPreguntas Cantidad de preguntas del simulacro de prueba.
 * @param duracionMinutos Duracion maxima del simulacro de prueba.
 * @returns Los repositorios, servicios y el Simulacro ya guardado.
 */
export async function construirEntornoIntentos(cantidadPreguntas = 2, duracionMinutos = 30) {
  const preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioEnMemoria();
  const simulacroRepositorio = new SimulacroRepositorioEnMemoria();
  const intentoDeSimulacroRepositorio = new IntentoDeSimulacroRepositorioEnMemoria();
  const publicadorEventos = new PublicadorEventosEnMemoria();
  const reloj = new RelojFijo(FECHA_INICIO_POR_DEFECTO);
  const calificador = new CalificadorSimulacroServicio();
  const cierreDeIntento = new CierreDeIntento(
    preguntaEvaluableRepositorio,
    intentoDeSimulacroRepositorio,
    calificador,
    publicadorEventos,
  );

  const preguntas = Array.from({ length: cantidadPreguntas }, () => crearPreguntaPublicada());
  for (const pregunta of preguntas) {
    preguntaEvaluableRepositorio.agregar(pregunta);
  }

  const criterio = CriterioDeGeneracion.crear({
    competenciaIds: [],
    temaIds: [],
    subtemaIds: [],
    nivelesDificultad: [],
  });
  const simulacro = Simulacro.definir({
    docenteId: ID_DOCENTE,
    nombre: 'Simulacro de prueba',
    criterio,
    duracion: DuracionMaxima.enMinutos(duracionMinutos),
    preguntas,
    ahora: reloj.ahora(),
  });
  await simulacroRepositorio.guardar(simulacro);

  return {
    preguntaEvaluableRepositorio,
    simulacroRepositorio,
    intentoDeSimulacroRepositorio,
    publicadorEventos,
    reloj,
    cierreDeIntento,
    preguntas,
    simulacro,
  };
}
