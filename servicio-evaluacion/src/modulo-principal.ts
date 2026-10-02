import { MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { MongooseModule } from '@nestjs/mongoose';
import { ConfiguracionServicio, OPCIONES_MONGOOSE } from './infraestructura/configuracion/configuracion.servicio';
import { RelojDelSistemaAdaptador } from './infraestructura/reloj/reloj-del-sistema.adaptador';
import { FuenteAleatoriaDelSistema } from './infraestructura/aleatoriedad/fuente-aleatoria-del-sistema';
import { ConexionRabbitMqServicio } from './infraestructura/mensajeria/conexion-rabbitmq.servicio';
import { PublicadorEventosRabbitMqAdaptador } from './infraestructura/mensajeria/publicador-eventos-rabbitmq.adaptador';
import { esquemaPreguntaEvaluable } from './infraestructura/persistencia/mongo/pregunta-evaluable.schema';
import { PreguntaEvaluableRepositorioMongo } from './infraestructura/persistencia/mongo/pregunta-evaluable.repositorio-mongo';
import { esquemaSimulacro } from './infraestructura/persistencia/mongo/simulacro.schema';
import { SimulacroRepositorioMongo } from './infraestructura/persistencia/mongo/simulacro.repositorio-mongo';
import { esquemaIntentoDeSimulacro } from './infraestructura/persistencia/mongo/intento-de-simulacro.schema';
import { IntentoDeSimulacroRepositorioMongo } from './infraestructura/persistencia/mongo/intento-de-simulacro.repositorio-mongo';
import { esquemaEventoProcesado } from './infraestructura/persistencia/mongo/evento-procesado.schema';
import { ReconexionMongoServicio } from './infraestructura/persistencia/mongo/reconexion-mongo.servicio';
import { RegistroEventosProcesadosRepositorioMongo } from './infraestructura/persistencia/mongo/registro-eventos-procesados.repositorio-mongo';
import { PROVIDERS_DE_CASOS_DE_USO } from './infraestructura/casos-de-uso.providers';
import {
  FUENTE_ALEATORIA,
  INTENTO_DE_SIMULACRO_REPOSITORIO,
  PREGUNTA_EVALUABLE_REPOSITORIO,
  PUBLICADOR_EVENTOS_PUERTO,
  REGISTRO_EVENTOS_PROCESADOS_PUERTO,
  RELOJ_PUERTO,
  SIMULACRO_REPOSITORIO,
} from './infraestructura/tokens-de-inyeccion';
import { IdentidadGuardia } from './interfaces/rest/guardias/identidad.guardia';
import { CorrelacionMiddleware } from './interfaces/rest/middlewares/correlacion.middleware';
import { MetodoNoPermitidoMiddleware } from './interfaces/rest/middlewares/metodo-no-permitido.middleware';
import { TipoDeContenidoMiddleware } from './interfaces/rest/middlewares/tipo-de-contenido.middleware';
import { SaludControlador } from './interfaces/rest/salud.controlador';
import { SimulacrosControlador } from './interfaces/rest/simulacros.controlador';
import { IntentosControlador } from './interfaces/rest/intentos.controlador';
import { PreguntasEvaluablesControlador } from './interfaces/rest/preguntas-evaluables.controlador';
import { PreguntaEditorialConsumidor } from './interfaces/mensajeria/pregunta-editorial.consumidor';

const URL_MONGO = process.env.EVALUACION_MONGO_URL ?? 'mongodb://localhost:27017/evaluacion';

/**
 * Modulo raiz de NestJS del servicio de Evaluacion y Simulacros.
 *
 * Conecta `dominio` y `aplicacion` (framework-free) con sus adaptadores de
 * `infraestructura` (MongoDB, RabbitMQ) y sus controladores de
 * `interfaces/rest`, a traves de los tokens de `tokens-de-inyeccion.ts` y
 * las fabricas de `casos-de-uso.providers.ts`.
 */
// CONTRATOS.md 9.3.6: el servicio arranca aunque Mongo no este listo.
// `lazyConnection: true` hace que NestJS no espere la conexion inicial
// durante el arranque (Nest solo entrega la Connection sin esperar su
// `asPromise()`), para que el proceso no se bloquee ni se caiga.
//
// Importante (documentado por Mongoose: "Mongoose will not automatically
// try to reconnect after initial connection failure... by design"): si
// esa conexion inicial falla (por ejemplo `bd-evaluacion` no esta lista
// todavia), el driver de MongoDB **no** reintenta solo en segundo plano;
// se queda `disconnected` para siempre, a diferencia de una conexion que
// si llego a establecerse y luego se cae (esa si se reconecta sola, ver
// la prueba de resiliencia). Por eso `ReconexionMongoServicio` reintenta
// la conexion a mano mientras siga `disconnected`.
//
// `bufferCommands: false` hace que una operacion lanzada mientras la
// conexion no esta lista falle de inmediato en vez de quedar encolada en
// silencio esperando a que Mongo aparezca: asi `verificarConexionMongo`
// (en cada repositorio Mongo) puede responder 503
// BASE_DE_DATOS_NO_DISPONIBLE de forma predecible (CONTRATOS.md 5.3).
@Module({
  imports: [
    MongooseModule.forRoot(URL_MONGO, {
      lazyConnection: true,
      ...OPCIONES_MONGOOSE,
    }),
    MongooseModule.forFeature([
      { name: 'PreguntaEvaluable', schema: esquemaPreguntaEvaluable },
      { name: 'Simulacro', schema: esquemaSimulacro },
      { name: 'IntentoDeSimulacro', schema: esquemaIntentoDeSimulacro },
      { name: 'EventoProcesado', schema: esquemaEventoProcesado },
    ]),
  ],
  controllers: [SaludControlador, SimulacrosControlador, IntentosControlador, PreguntasEvaluablesControlador],
  providers: [
    ConfiguracionServicio,
    ConexionRabbitMqServicio,
    PreguntaEditorialConsumidor,
    ReconexionMongoServicio,
    { provide: RELOJ_PUERTO, useClass: RelojDelSistemaAdaptador },
    { provide: FUENTE_ALEATORIA, useClass: FuenteAleatoriaDelSistema },
    { provide: PREGUNTA_EVALUABLE_REPOSITORIO, useClass: PreguntaEvaluableRepositorioMongo },
    { provide: SIMULACRO_REPOSITORIO, useClass: SimulacroRepositorioMongo },
    { provide: INTENTO_DE_SIMULACRO_REPOSITORIO, useClass: IntentoDeSimulacroRepositorioMongo },
    { provide: REGISTRO_EVENTOS_PROCESADOS_PUERTO, useClass: RegistroEventosProcesadosRepositorioMongo },
    { provide: PUBLICADOR_EVENTOS_PUERTO, useClass: PublicadorEventosRabbitMqAdaptador },
    { provide: APP_GUARD, useClass: IdentidadGuardia },
    ...PROVIDERS_DE_CASOS_DE_USO,
  ],
})
export class ModuloPrincipal implements NestModule {
  public configure(consumer: MiddlewareConsumer): void {
    // Orden importante: la correlacion se establece primero (para que
    // cualquier error posterior, incluidos el 405 y el 415, quede
    // trazado); el metodo se valida antes que el tipo de contenido,
    // porque no tiene sentido revisar el Content-Type de una ruta que ni
    // siquiera acepta ese metodo.
    consumer.apply(CorrelacionMiddleware, MetodoNoPermitidoMiddleware, TipoDeContenidoMiddleware).forRoutes('*');
  }
}
