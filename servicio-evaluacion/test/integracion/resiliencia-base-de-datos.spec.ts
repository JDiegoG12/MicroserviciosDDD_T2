import { execSync } from 'node:child_process';
import * as amqplib from 'amqplib';
import mongoose, { Connection } from 'mongoose';
import { GenericContainer, StartedTestContainer, Wait } from 'testcontainers';
import { RegistrarPreguntaArchivadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-archivada.caso-uso';
import { RegistrarPreguntaPublicadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-publicada.caso-uso';
import { ConfiguracionServicio } from '../../src/infraestructura/configuracion/configuracion.servicio';
import { ConexionRabbitMqServicio } from '../../src/infraestructura/mensajeria/conexion-rabbitmq.servicio';
import { PreguntaEditorialConsumidor } from '../../src/infraestructura/mensajeria/pregunta-editorial.consumidor';
import {
  asegurarTopologiaRabbitMq,
  COLA_EVALUACION_PREGUNTAS,
  COLA_EVALUACION_PREGUNTAS_DLQ,
  EXCHANGE_EDITORIAL_EVENTOS,
  ROUTING_KEY_PREGUNTA_PUBLICADA,
} from '../../src/infraestructura/mensajeria/topologia-rabbitmq';
import { esquemaEventoProcesado } from '../../src/infraestructura/persistencia/mongo/evento-procesado.schema';
import { PreguntaEvaluableRepositorioMongo } from '../../src/infraestructura/persistencia/mongo/pregunta-evaluable.repositorio-mongo';
import { esquemaPreguntaEvaluable } from '../../src/infraestructura/persistencia/mongo/pregunta-evaluable.schema';
import { RegistroEventosProcesadosRepositorioMongo } from '../../src/infraestructura/persistencia/mongo/registro-eventos-procesados.repositorio-mongo';
import { RelojFijo } from '../dobles/reloj-fijo';

jest.setTimeout(300_000);

async function esperar(condicion: () => boolean | Promise<boolean>, maximoMs: number, descripcion: string): Promise<void> {
  const inicio = Date.now();
  while (Date.now() - inicio < maximoMs) {
    if (await condicion()) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`Tiempo de espera agotado: ${descripcion}`);
}

describe('Resiliencia sin MongoDB (CONTRATOS.md 9.3.6 y 7.7.5, integracion con Testcontainers)', () => {
  let contenedorMongo: StartedTestContainer;
  let idContenedorMongo: string;
  let puertoMongo: number;
  let hostMongo: string;
  let contenedorRabbit: StartedTestContainer;
  let configuracion: ConfiguracionServicio;
  let conexionCruda: amqplib.ChannelModel;
  let canalCrudo: amqplib.ConfirmChannel;
  let conexionMongo: Connection;
  let conexionRabbitServicio: ConexionRabbitMqServicio;

  beforeAll(async () => {
    contenedorMongo = await new GenericContainer('mongo:7')
      .withExposedPorts(27017)
      .withWaitStrategy(Wait.forLogMessage(/Waiting for connections/))
      .start();
    idContenedorMongo = contenedorMongo.getId();
    hostMongo = contenedorMongo.getHost();
    puertoMongo = contenedorMongo.getMappedPort(27017);

    contenedorRabbit = await new GenericContainer('rabbitmq:3.13-management')
      .withEnvironment({ RABBITMQ_DEFAULT_USER: 'banco', RABBITMQ_DEFAULT_PASS: 'banco123' })
      .withExposedPorts(5672, 15672)
      .withWaitStrategy(Wait.forLogMessage(/Server startup complete/))
      .start();

    process.env.RABBITMQ_HOST = contenedorRabbit.getHost();
    process.env.RABBITMQ_PUERTO = String(contenedorRabbit.getMappedPort(5672));
    process.env.RABBITMQ_USUARIO = 'banco';
    process.env.RABBITMQ_CLAVE = 'banco123';
    configuracion = new ConfiguracionServicio();

    conexionCruda = await amqplib.connect(configuracion.rabbitMqUrl);
    canalCrudo = await conexionCruda.createConfirmChannel();
    await asegurarTopologiaRabbitMq(canalCrudo);
  }, 180_000);

  afterAll(async () => {
    await canalCrudo?.close();
    await conexionCruda?.close();
    await conexionRabbitServicio?.onModuleDestroy();
    await conexionMongo?.close();
    await contenedorRabbit?.stop();
    try {
      execSync(`docker start ${idContenedorMongo}`);
    } catch {
      // Ya puede estar corriendo si la prueba no llego a detenerlo.
    }
    await contenedorMongo?.stop();
  });

  async function contarEnCola(nombreCola: string): Promise<{ mensajes: number; consumidores: number }> {
    const info = await canalCrudo.checkQueue(nombreCola);
    return { mensajes: info.messageCount, consumidores: info.consumerCount };
  }

  it(
    'pausa el consumo al caer MongoDB y lo reanuda al volver, sin perder el mensaje en la DLQ',
    async () => {
      // --- Arranque con Mongo disponible (igual que modulo-principal.ts) ---
      // `mongoose.createConnection(...)` sin `await .asPromise()` es
      // exactamente lo que hace `lazyConnection: true` de @nestjs/mongoose
      // por debajo (ver MongooseCoreModule.createMongooseConnection):
      // conecta en segundo plano, sin bloquear.
      const uriMongo = `mongodb://${hostMongo}:${puertoMongo}/evaluacion-resiliencia`;
      conexionMongo = mongoose.createConnection(uriMongo, { bufferCommands: false });
      await esperar(() => conexionMongo.readyState === 1, 30_000, 'Mongo conecta por primera vez');

      const modeloPregunta = conexionMongo.model('PreguntaEvaluable', esquemaPreguntaEvaluable);
      const modeloEventoProcesado = conexionMongo.model('EventoProcesado', esquemaEventoProcesado);
      const preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioMongo(modeloPregunta);
      const registroEventosProcesados = new RegistroEventosProcesadosRepositorioMongo(modeloEventoProcesado);
      const reloj = new RelojFijo();
      const registrarPublicada = new RegistrarPreguntaPublicadaCasoUso(
        preguntaEvaluableRepositorio,
        registroEventosProcesados,
        reloj,
      );
      const registrarArchivada = new RegistrarPreguntaArchivadaCasoUso(
        preguntaEvaluableRepositorio,
        registroEventosProcesados,
        reloj,
      );

      conexionRabbitServicio = new ConexionRabbitMqServicio(configuracion);
      const consumidor = new PreguntaEditorialConsumidor(
        conexionRabbitServicio,
        registrarPublicada,
        registrarArchivada,
        conexionMongo,
      );
      consumidor.onModuleInit();

      await esperar(
        async () => (await contarEnCola(COLA_EVALUACION_PREGUNTAS)).consumidores === 1,
        30_000,
        'el consumidor empieza a consumir con Mongo arriba',
      );

      // --- Se cae MongoDB: el consumidor debe pausarse ---
      // `remove: false` es imprescindible: por defecto Testcontainers
      // ELIMINA el contenedor al detenerlo (pensado para limpieza al final
      // de la prueba), y aqui se necesita volver a arrancar el mismo
      // contenedor (mismo puerto publicado) para simular una caida
      // temporal, no una sustitucion.
      await contenedorMongo.stop({ timeout: 5, remove: false });

      await esperar(
        () => conexionMongo.readyState !== 1,
        60_000,
        'la Connection de Mongoose detecta la caida',
      );
      await esperar(
        async () => (await contarEnCola(COLA_EVALUACION_PREGUNTAS)).consumidores === 0,
        30_000,
        'el consumidor se pausa (cancela su consumo)',
      );

      // --- Se publica un evento mientras Mongo esta caido ---
      const idEvento = 'aaaaaaaa-1111-4222-8333-444444444444';
      const preguntaId = 'bbbbbbbb-2222-4333-8444-555555555555';
      const evento = {
        idEvento,
        tipoEvento: 'PreguntaPublicada',
        versionEvento: 1,
        fechaOcurrencia: '2026-10-01T15:30:00Z',
        origen: 'servicio-editorial',
        idCorrelacion: null,
        datos: {
          preguntaId,
          contexto: 'Pregunta publicada mientras Mongo estaba caido.',
          preguntaDirecta: 'Se procesa cuando Mongo vuelve?',
          opciones: [
            { letra: 'A', texto: 'A' },
            { letra: 'B', texto: 'B' },
            { letra: 'C', texto: 'C' },
            { letra: 'D', texto: 'D' },
          ],
          letraCorrecta: 'A',
          clasificacion: {
            competenciaId: '22222222-2222-4222-8222-000000000101',
            temaId: '22222222-2222-4222-8222-000000000201',
            subtemaId: '22222222-2222-4222-8222-000000000301',
          },
          nivelDificultad: 'BAJO',
          fechaPublicacion: '2026-10-01T15:30:00Z',
        },
      };
      canalCrudo.publish(EXCHANGE_EDITORIAL_EVENTOS, ROUTING_KEY_PREGUNTA_PUBLICADA, Buffer.from(JSON.stringify(evento)));

      // Espera un momento prudente: el mensaje debe quedar esperando en la
      // cola principal (nadie lo consume) y la DLQ debe seguir vacia.
      await new Promise((resolve) => setTimeout(resolve, 3_000));
      const estadoMientrasCaido = await contarEnCola(COLA_EVALUACION_PREGUNTAS);
      const dlqMientrasCaido = await contarEnCola(COLA_EVALUACION_PREGUNTAS_DLQ);
      expect(estadoMientrasCaido.mensajes).toBeGreaterThanOrEqual(1);
      expect(estadoMientrasCaido.consumidores).toBe(0);
      expect(dlqMientrasCaido.mensajes).toBe(0);

      // --- Vuelve MongoDB: el consumidor debe reanudarse y procesar el mensaje ---
      execSync(`docker start ${idContenedorMongo}`);

      await esperar(() => conexionMongo.readyState === 1, 90_000, 'Mongo reconecta');
      await esperar(
        async () => (await preguntaEvaluableRepositorio.obtenerPorId(preguntaId)) !== null,
        60_000,
        'el mensaje pendiente se procesa tras reconectar',
      );

      const pregunta = await preguntaEvaluableRepositorio.obtenerPorId(preguntaId);
      expect(pregunta?.estado).toBe('PUBLICADA');

      const dlqDespues = await contarEnCola(COLA_EVALUACION_PREGUNTAS_DLQ);
      expect(dlqDespues.mensajes).toBe(0);
    },
  );
});
