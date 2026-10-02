import { Provider } from '@nestjs/common';
import { CierreDeIntento } from '../aplicacion/intentos/cierre-de-intento';
import { FinalizarIntentoCasoUso } from '../aplicacion/intentos/finalizar-intento.caso-uso';
import { IniciarIntentoCasoUso } from '../aplicacion/intentos/iniciar-intento.caso-uso';
import { ObtenerIntentoCasoUso } from '../aplicacion/intentos/obtener-intento.caso-uso';
import { RegistrarRespuestaCasoUso } from '../aplicacion/intentos/registrar-respuesta.caso-uso';
import { ConsultarPreguntasEvaluablesCasoUso } from '../aplicacion/preguntas-evaluables/consultar-preguntas-evaluables.caso-uso';
import { RegistrarPreguntaArchivadaCasoUso } from '../aplicacion/preguntas-evaluables/registrar-pregunta-archivada.caso-uso';
import { RegistrarPreguntaPublicadaCasoUso } from '../aplicacion/preguntas-evaluables/registrar-pregunta-publicada.caso-uso';
import { DefinirSimulacroCasoUso } from '../aplicacion/simulacros/definir-simulacro.caso-uso';
import { ListarSimulacrosCasoUso } from '../aplicacion/simulacros/listar-simulacros.caso-uso';
import { ObtenerSimulacroCasoUso } from '../aplicacion/simulacros/obtener-simulacro.caso-uso';
import { PreguntaEvaluableRepositorio } from '../dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { CalificadorSimulacroServicio } from '../dominio/servicios/calificador-simulacro.servicio';
import { EnsambladorSimulacroServicio } from '../dominio/servicios/ensamblador-simulacro.servicio';
import { FuenteAleatoria } from '../dominio/servicios/fuente-aleatoria';
import { IntentoDeSimulacroRepositorio } from '../dominio/intentos/intento-de-simulacro.repositorio';
import { SimulacroRepositorio } from '../dominio/simulacros/simulacro.repositorio';
import { PublicadorEventosPuerto } from '../aplicacion/puertos/salida/publicador-eventos.puerto';
import { RegistroEventosProcesadosPuerto } from '../aplicacion/puertos/salida/registro-eventos-procesados.puerto';
import { RelojPuerto } from '../aplicacion/puertos/salida/reloj.puerto';
import {
  FUENTE_ALEATORIA,
  INTENTO_DE_SIMULACRO_REPOSITORIO,
  PREGUNTA_EVALUABLE_REPOSITORIO,
  PUBLICADOR_EVENTOS_PUERTO,
  REGISTRO_EVENTOS_PROCESADOS_PUERTO,
  RELOJ_PUERTO,
  SIMULACRO_REPOSITORIO,
} from './tokens-de-inyeccion';

/**
 * Providers de NestJS que construyen los servicios de dominio y los casos
 * de uso de `aplicacion` mediante fabricas explicitas (`useFactory`).
 *
 * Ni `dominio` ni `aplicacion` tienen decoradores de NestJS (CLAUDE.md del
 * servicio): esta es la unica pieza, dentro de `infraestructura`, que sabe
 * como conectarlos con los adaptadores concretos registrados bajo los
 * tokens de `tokens-de-inyeccion.ts`.
 */
export const PROVIDERS_DE_CASOS_DE_USO: Provider[] = [
  {
    provide: EnsambladorSimulacroServicio,
    useFactory: (fuenteAleatoria: FuenteAleatoria) => new EnsambladorSimulacroServicio(fuenteAleatoria),
    inject: [FUENTE_ALEATORIA],
  },
  {
    provide: CalificadorSimulacroServicio,
    useFactory: () => new CalificadorSimulacroServicio(),
  },
  {
    provide: CierreDeIntento,
    useFactory: (
      preguntaRepo: PreguntaEvaluableRepositorio,
      intentoRepo: IntentoDeSimulacroRepositorio,
      calificador: CalificadorSimulacroServicio,
      publicador: PublicadorEventosPuerto,
    ) => new CierreDeIntento(preguntaRepo, intentoRepo, calificador, publicador),
    inject: [
      PREGUNTA_EVALUABLE_REPOSITORIO,
      INTENTO_DE_SIMULACRO_REPOSITORIO,
      CalificadorSimulacroServicio,
      PUBLICADOR_EVENTOS_PUERTO,
    ],
  },
  {
    provide: DefinirSimulacroCasoUso,
    useFactory: (
      preguntaRepo: PreguntaEvaluableRepositorio,
      simulacroRepo: SimulacroRepositorio,
      ensamblador: EnsambladorSimulacroServicio,
      publicador: PublicadorEventosPuerto,
      reloj: RelojPuerto,
    ) => new DefinirSimulacroCasoUso(preguntaRepo, simulacroRepo, ensamblador, publicador, reloj),
    inject: [
      PREGUNTA_EVALUABLE_REPOSITORIO,
      SIMULACRO_REPOSITORIO,
      EnsambladorSimulacroServicio,
      PUBLICADOR_EVENTOS_PUERTO,
      RELOJ_PUERTO,
    ],
  },
  {
    provide: ListarSimulacrosCasoUso,
    useFactory: (simulacroRepo: SimulacroRepositorio) => new ListarSimulacrosCasoUso(simulacroRepo),
    inject: [SIMULACRO_REPOSITORIO],
  },
  {
    provide: ObtenerSimulacroCasoUso,
    useFactory: (simulacroRepo: SimulacroRepositorio) => new ObtenerSimulacroCasoUso(simulacroRepo),
    inject: [SIMULACRO_REPOSITORIO],
  },
  {
    provide: IniciarIntentoCasoUso,
    useFactory: (
      simulacroRepo: SimulacroRepositorio,
      intentoRepo: IntentoDeSimulacroRepositorio,
      preguntaRepo: PreguntaEvaluableRepositorio,
      publicador: PublicadorEventosPuerto,
      reloj: RelojPuerto,
    ) => new IniciarIntentoCasoUso(simulacroRepo, intentoRepo, preguntaRepo, publicador, reloj),
    inject: [
      SIMULACRO_REPOSITORIO,
      INTENTO_DE_SIMULACRO_REPOSITORIO,
      PREGUNTA_EVALUABLE_REPOSITORIO,
      PUBLICADOR_EVENTOS_PUERTO,
      RELOJ_PUERTO,
    ],
  },
  {
    provide: RegistrarRespuestaCasoUso,
    useFactory: (
      intentoRepo: IntentoDeSimulacroRepositorio,
      preguntaRepo: PreguntaEvaluableRepositorio,
      cierreDeIntento: CierreDeIntento,
      publicador: PublicadorEventosPuerto,
      reloj: RelojPuerto,
    ) => new RegistrarRespuestaCasoUso(intentoRepo, preguntaRepo, cierreDeIntento, publicador, reloj),
    inject: [
      INTENTO_DE_SIMULACRO_REPOSITORIO,
      PREGUNTA_EVALUABLE_REPOSITORIO,
      CierreDeIntento,
      PUBLICADOR_EVENTOS_PUERTO,
      RELOJ_PUERTO,
    ],
  },
  {
    provide: FinalizarIntentoCasoUso,
    useFactory: (
      intentoRepo: IntentoDeSimulacroRepositorio,
      preguntaRepo: PreguntaEvaluableRepositorio,
      cierreDeIntento: CierreDeIntento,
      reloj: RelojPuerto,
    ) => new FinalizarIntentoCasoUso(intentoRepo, preguntaRepo, cierreDeIntento, reloj),
    inject: [INTENTO_DE_SIMULACRO_REPOSITORIO, PREGUNTA_EVALUABLE_REPOSITORIO, CierreDeIntento, RELOJ_PUERTO],
  },
  {
    provide: ObtenerIntentoCasoUso,
    useFactory: (
      intentoRepo: IntentoDeSimulacroRepositorio,
      preguntaRepo: PreguntaEvaluableRepositorio,
      cierreDeIntento: CierreDeIntento,
      reloj: RelojPuerto,
    ) => new ObtenerIntentoCasoUso(intentoRepo, preguntaRepo, cierreDeIntento, reloj),
    inject: [INTENTO_DE_SIMULACRO_REPOSITORIO, PREGUNTA_EVALUABLE_REPOSITORIO, CierreDeIntento, RELOJ_PUERTO],
  },
  {
    provide: ConsultarPreguntasEvaluablesCasoUso,
    useFactory: (preguntaRepo: PreguntaEvaluableRepositorio) =>
      new ConsultarPreguntasEvaluablesCasoUso(preguntaRepo),
    inject: [PREGUNTA_EVALUABLE_REPOSITORIO],
  },
  {
    provide: RegistrarPreguntaPublicadaCasoUso,
    useFactory: (
      preguntaRepo: PreguntaEvaluableRepositorio,
      registroEventos: RegistroEventosProcesadosPuerto,
      reloj: RelojPuerto,
    ) => new RegistrarPreguntaPublicadaCasoUso(preguntaRepo, registroEventos, reloj),
    inject: [PREGUNTA_EVALUABLE_REPOSITORIO, REGISTRO_EVENTOS_PROCESADOS_PUERTO, RELOJ_PUERTO],
  },
  {
    provide: RegistrarPreguntaArchivadaCasoUso,
    useFactory: (
      preguntaRepo: PreguntaEvaluableRepositorio,
      registroEventos: RegistroEventosProcesadosPuerto,
      reloj: RelojPuerto,
    ) => new RegistrarPreguntaArchivadaCasoUso(preguntaRepo, registroEventos, reloj),
    inject: [PREGUNTA_EVALUABLE_REPOSITORIO, REGISTRO_EVENTOS_PROCESADOS_PUERTO, RELOJ_PUERTO],
  },
];
