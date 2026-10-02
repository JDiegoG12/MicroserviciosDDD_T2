import { INestApplication, RequestMethod, ValidationPipe } from '@nestjs/common';
import { APP_GUARD, HttpAdapterHost } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import { PROVIDERS_DE_CASOS_DE_USO } from '../../../src/infraestructura/casos-de-uso.providers';
import {
  FUENTE_ALEATORIA,
  INTENTO_DE_SIMULACRO_REPOSITORIO,
  PREGUNTA_EVALUABLE_REPOSITORIO,
  PUBLICADOR_EVENTOS_PUERTO,
  REGISTRO_EVENTOS_PROCESADOS_PUERTO,
  RELOJ_PUERTO,
  SIMULACRO_REPOSITORIO,
} from '../../../src/infraestructura/tokens-de-inyeccion';
import { IdentidadGuardia } from '../../../src/interfaces/rest/guardias/identidad.guardia';
import { aplanarErroresDeValidacion } from '../../../src/interfaces/rest/filtros/aplanar-errores-de-validacion';
import { FiltroExcepcionesGlobal } from '../../../src/interfaces/rest/filtros/filtro-excepciones.global';
import { SolicitudInvalidaHttpExcepcion } from '../../../src/interfaces/rest/excepciones/solicitud-invalida-http.excepcion';
import { CorrelacionMiddleware } from '../../../src/interfaces/rest/middlewares/correlacion.middleware';
import { MetodoNoPermitidoMiddleware } from '../../../src/interfaces/rest/middlewares/metodo-no-permitido.middleware';
import { TipoDeContenidoMiddleware } from '../../../src/interfaces/rest/middlewares/tipo-de-contenido.middleware';
import { SaludControlador } from '../../../src/interfaces/rest/salud.controlador';
import { SimulacrosControlador } from '../../../src/interfaces/rest/simulacros.controlador';
import { IntentosControlador } from '../../../src/interfaces/rest/intentos.controlador';
import { PreguntasEvaluablesControlador } from '../../../src/interfaces/rest/preguntas-evaluables.controlador';
import { ConfiguracionServicio } from '../../../src/infraestructura/configuracion/configuracion.servicio';
import { PreguntaEvaluableRepositorioEnMemoria } from '../../dobles/pregunta-evaluable-repositorio-en-memoria';
import { SimulacroRepositorioEnMemoria } from '../../dobles/simulacro-repositorio-en-memoria';
import { IntentoDeSimulacroRepositorioEnMemoria } from '../../dobles/intento-de-simulacro-repositorio-en-memoria';
import { RegistroEventosProcesadosEnMemoria } from '../../dobles/registro-eventos-procesados-en-memoria';
import { PublicadorEventosEnMemoria } from '../../dobles/publicador-eventos-en-memoria';
import { RelojFijo } from '../../dobles/reloj-fijo';
import { FuenteAleatoriaFija } from '../../dobles/fuente-aleatoria-fija';

/** Dobles compartidos de una app de prueba, para preparar datos y hacer afirmaciones. */
export interface DoblesDeAppDePrueba {
  readonly preguntaEvaluableRepositorio: PreguntaEvaluableRepositorioEnMemoria;
  readonly simulacroRepositorio: SimulacroRepositorioEnMemoria;
  readonly intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorioEnMemoria;
  readonly registroEventosProcesados: RegistroEventosProcesadosEnMemoria;
  readonly publicadorEventos: PublicadorEventosEnMemoria;
  readonly reloj: RelojFijo;
}

/**
 * Construye una `INestApplication` de prueba con los mismos controladores,
 * guardias, pipes y filtros que `main.ts`, pero con los puertos de salida
 * atados a dobles en memoria (sin Mongo ni RabbitMQ reales): permite probar
 * la API completa (encabezados, validacion, codigos HTTP, problem+json)
 * sin infraestructura externa.
 *
 * @returns La app ya inicializada y los dobles para preparar datos.
 */
export async function construirAppDePrueba(): Promise<{ app: INestApplication; dobles: DoblesDeAppDePrueba }> {
  const preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioEnMemoria();
  const simulacroRepositorio = new SimulacroRepositorioEnMemoria();
  const intentoDeSimulacroRepositorio = new IntentoDeSimulacroRepositorioEnMemoria();
  const registroEventosProcesados = new RegistroEventosProcesadosEnMemoria();
  const publicadorEventos = new PublicadorEventosEnMemoria();
  const reloj = new RelojFijo();
  const fuenteAleatoria = new FuenteAleatoriaFija();

  const moduloDePrueba = await Test.createTestingModule({
    controllers: [SaludControlador, SimulacrosControlador, IntentosControlador, PreguntasEvaluablesControlador],
    providers: [
      ConfiguracionServicio,
      { provide: PREGUNTA_EVALUABLE_REPOSITORIO, useValue: preguntaEvaluableRepositorio },
      { provide: SIMULACRO_REPOSITORIO, useValue: simulacroRepositorio },
      { provide: INTENTO_DE_SIMULACRO_REPOSITORIO, useValue: intentoDeSimulacroRepositorio },
      { provide: REGISTRO_EVENTOS_PROCESADOS_PUERTO, useValue: registroEventosProcesados },
      { provide: PUBLICADOR_EVENTOS_PUERTO, useValue: publicadorEventos },
      { provide: RELOJ_PUERTO, useValue: reloj },
      { provide: FUENTE_ALEATORIA, useValue: fuenteAleatoria },
      { provide: APP_GUARD, useClass: IdentidadGuardia },
      ...PROVIDERS_DE_CASOS_DE_USO,
    ],
  }).compile();

  const app = moduloDePrueba.createNestApplication();
  const middlewareDeCorrelacion = new CorrelacionMiddleware();
  const middlewareDeMetodo = new MetodoNoPermitidoMiddleware({ httpAdapter: app.getHttpAdapter() } as HttpAdapterHost);
  const middlewareDeContenido = new TipoDeContenidoMiddleware();
  app.use((req: unknown, res: unknown, next: () => void) =>
    middlewareDeCorrelacion.use(req as never, res as never, next),
  );
  app.use((req: unknown, res: unknown, next: () => void) =>
    middlewareDeMetodo.use(req as never, res as never, next),
  );
  app.use((req: unknown, res: unknown, next: () => void) =>
    middlewareDeContenido.use(req as never, res as never, next),
  );
  app.setGlobalPrefix('api/v1', {
    exclude: [
      { path: 'salud', method: RequestMethod.GET },
      { path: 'docs', method: RequestMethod.GET },
      { path: 'openapi.json', method: RequestMethod.GET },
    ],
  });
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      exceptionFactory: (errores) => new SolicitudInvalidaHttpExcepcion(aplanarErroresDeValidacion(errores)),
    }),
  );
  app.useGlobalFilters(new FiltroExcepcionesGlobal());
  await app.init();

  return {
    app,
    dobles: {
      preguntaEvaluableRepositorio,
      simulacroRepositorio,
      intentoDeSimulacroRepositorio,
      registroEventosProcesados,
      publicadorEventos,
      reloj,
    },
  };
}
