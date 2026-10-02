import * as fs from 'node:fs';
import * as path from 'node:path';
import * as amqplib from 'amqplib';
import Ajv from 'ajv/dist/2020';
import addFormats from 'ajv-formats';
import { GenericContainer, StartedTestContainer, Wait } from 'testcontainers';
import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { CalificadorSimulacroServicio } from '../../src/dominio/servicios/calificador-simulacro.servicio';
import { Rol, UsuarioActual } from '../../src/aplicacion/compartido/usuario-actual';
import { CierreDeIntento } from '../../src/aplicacion/intentos/cierre-de-intento';
import { FinalizarIntentoCasoUso } from '../../src/aplicacion/intentos/finalizar-intento.caso-uso';
import { RegistrarPreguntaArchivadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-archivada.caso-uso';
import { RegistrarPreguntaPublicadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-publicada.caso-uso';
import { ConfiguracionServicio } from '../../src/infraestructura/configuracion/configuracion.servicio';
import { ConexionRabbitMqServicio } from '../../src/infraestructura/mensajeria/conexion-rabbitmq.servicio';
import { PreguntaEditorialConsumidor } from '../../src/infraestructura/mensajeria/pregunta-editorial.consumidor';
import { PublicadorEventosRabbitMqAdaptador } from '../../src/infraestructura/mensajeria/publicador-eventos-rabbitmq.adaptador';
import {
  asegurarTopologiaRabbitMq,
  COLA_EVALUACION_PREGUNTAS_DLQ,
  EXCHANGE_EDITORIAL_EVENTOS,
  EXCHANGE_EVALUACION_EVENTOS,
  ROUTING_KEY_INTENTO_CALIFICADO,
  ROUTING_KEY_PREGUNTA_ARCHIVADA,
  ROUTING_KEY_PREGUNTA_PUBLICADA,
} from '../../src/infraestructura/mensajeria/topologia-rabbitmq';
import { ID_DOCENTE, ID_ESTUDIANTE } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';
import { ConexionMongoFalsa } from '../dobles/conexion-mongo-falsa';
import { IntentoDeSimulacroRepositorioEnMemoria } from '../dobles/intento-de-simulacro-repositorio-en-memoria';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { RegistroEventosProcesadosEnMemoria } from '../dobles/registro-eventos-procesados-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';

jest.setTimeout(120_000);

const DIR_EJEMPLOS = path.resolve(__dirname, '..', '..', '..', 'contratos', 'eventos', 'ejemplos');
const ESQUEMA_INTENTO_CALIFICADO = JSON.parse(
  fs.readFileSync(
    path.resolve(__dirname, '..', '..', '..', 'contratos', 'eventos', 'evaluacion', 'intento-calificado.v1.schema.json'),
    'utf-8',
  ),
);

function leerEjemplo(nombre: string) {
  return JSON.parse(fs.readFileSync(path.join(DIR_EJEMPLOS, nombre), 'utf-8'));
}

async function esperar(condicion: () => boolean | Promise<boolean>, maximoMs = 10_000): Promise<void> {
  const inicio = Date.now();
  while (Date.now() - inicio < maximoMs) {
    if (await condicion()) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('Tiempo de espera agotado.');
}

describe('Mensajeria RabbitMQ (integracion con Testcontainers, rabbitmq:3.13-management)', () => {
  let contenedor: StartedTestContainer;
  let conexionCruda: amqplib.ChannelModel;
  let canalCrudo: amqplib.ConfirmChannel;
  let configuracion: ConfiguracionServicio;

  beforeAll(async () => {
    contenedor = await new GenericContainer('rabbitmq:3.13-management')
      .withEnvironment({ RABBITMQ_DEFAULT_USER: 'banco', RABBITMQ_DEFAULT_PASS: 'banco123' })
      .withExposedPorts(5672, 15672)
      .withWaitStrategy(Wait.forLogMessage(/Server startup complete/))
      .start();

    process.env.RABBITMQ_HOST = contenedor.getHost();
    process.env.RABBITMQ_PUERTO = String(contenedor.getMappedPort(5672));
    process.env.RABBITMQ_USUARIO = 'banco';
    process.env.RABBITMQ_CLAVE = 'banco123';
    configuracion = new ConfiguracionServicio();

    conexionCruda = await amqplib.connect(configuracion.rabbitMqUrl);
    canalCrudo = await conexionCruda.createConfirmChannel();
    await asegurarTopologiaRabbitMq(canalCrudo);
  });

  afterAll(async () => {
    await canalCrudo?.close();
    await conexionCruda?.close();
    await contenedor?.stop();
  });

  async function publicarEnEditorialEventos(routingKey: string, contenido: unknown): Promise<void> {
    canalCrudo.publish(EXCHANGE_EDITORIAL_EVENTOS, routingKey, Buffer.from(JSON.stringify(contenido)), {
      contentType: 'application/json',
    });
  }

  async function contarEnCola(nombreCola: string): Promise<number> {
    const info = await canalCrudo.checkQueue(nombreCola);
    return info.messageCount;
  }

  describe('Consumidor (PreguntaEditorialConsumidor)', () => {
    let preguntaEvaluableRepositorio: PreguntaEvaluableRepositorioEnMemoria;
    let registroEventosProcesados: RegistroEventosProcesadosEnMemoria;
    let conexion: ConexionRabbitMqServicio;
    let consumidor: PreguntaEditorialConsumidor;

    beforeAll(async () => {
      preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioEnMemoria();
      registroEventosProcesados = new RegistroEventosProcesadosEnMemoria();
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
      conexion = new ConexionRabbitMqServicio(configuracion);
      consumidor = new PreguntaEditorialConsumidor(
        conexion,
        registrarPublicada,
        registrarArchivada,
        new ConexionMongoFalsa(true) as never,
      );
      consumidor.onModuleInit();
      // Da tiempo a que el canal del consumidor se conecte y declare su topologia.
      await new Promise((resolve) => setTimeout(resolve, 1500));
    });

    afterAll(async () => {
      await conexion.onModuleDestroy();
    });

    it('1. procesa PreguntaPublicada de ejemplo: la copia local queda PUBLICADA', async () => {
      const ejemplo = leerEjemplo('pregunta-publicada.v1.ejemplo.json');
      await publicarEnEditorialEventos(ROUTING_KEY_PREGUNTA_PUBLICADA, ejemplo);

      await esperar(async () => (await preguntaEvaluableRepositorio.obtenerPorId(ejemplo.datos.preguntaId)) !== null);

      const pregunta = await preguntaEvaluableRepositorio.obtenerPorId(ejemplo.datos.preguntaId);
      expect(pregunta?.estado).toBe('PUBLICADA');
    });

    it('2. procesa PreguntaPublicada con un campo extra (lector tolerante): no va a la DLQ', async () => {
      const ejemplo = leerEjemplo('pregunta-publicada.v1.ejemplo-campo-extra.json');
      const antesDeLaDlq = await contarEnCola(COLA_EVALUACION_PREGUNTAS_DLQ);

      await publicarEnEditorialEventos(ROUTING_KEY_PREGUNTA_PUBLICADA, ejemplo);
      await esperar(() => registroEventosProcesados.estaReclamado(ejemplo.idEvento));

      const despuesDeLaDlq = await contarEnCola(COLA_EVALUACION_PREGUNTAS_DLQ);
      expect(despuesDeLaDlq).toBe(antesDeLaDlq);
    });

    it('3. el mismo idEvento dos veces no duplica (idempotencia)', async () => {
      const ejemplo = {
        ...leerEjemplo('pregunta-publicada.v1.ejemplo.json'),
        idEvento: '44444444-5555-4666-8777-888888888888',
        datos: { ...leerEjemplo('pregunta-publicada.v1.ejemplo.json').datos, preguntaId: '55555555-6666-4777-8888-999999999999' },
      };

      await publicarEnEditorialEventos(ROUTING_KEY_PREGUNTA_PUBLICADA, ejemplo);
      await esperar(async () => (await preguntaEvaluableRepositorio.obtenerPorId(ejemplo.datos.preguntaId)) !== null);
      const guardadosTrasLaPrimera = preguntaEvaluableRepositorio.cantidadDeGuardados;

      await publicarEnEditorialEventos(ROUTING_KEY_PREGUNTA_PUBLICADA, ejemplo);
      await new Promise((resolve) => setTimeout(resolve, 500));

      expect(preguntaEvaluableRepositorio.cantidadDeGuardados).toBe(guardadosTrasLaPrimera);
    });

    it('4. una PreguntaArchivada antes que su PreguntaPublicada deja la copia ARCHIVADA', async () => {
      const archivada = {
        idEvento: '66666666-7777-4888-8999-aaaaaaaaaaaa',
        tipoEvento: 'PreguntaArchivada',
        versionEvento: 1,
        fechaOcurrencia: '2026-10-05T10:00:00Z',
        origen: 'servicio-editorial',
        idCorrelacion: null,
        datos: {
          preguntaId: 'bbbbbbbb-cccc-4ddd-8eee-ffffffffffff',
          motivo: 'Motivo de prueba',
          fechaArchivado: '2026-10-05T10:00:00Z',
        },
      };

      await publicarEnEditorialEventos(ROUTING_KEY_PREGUNTA_ARCHIVADA, archivada);
      await esperar(async () => (await preguntaEvaluableRepositorio.obtenerPorId(archivada.datos.preguntaId)) !== null);

      const pregunta = await preguntaEvaluableRepositorio.obtenerPorId(archivada.datos.preguntaId);
      expect(pregunta?.estado).toBe('ARCHIVADA');
    });

    it('5. un JSON roto y un versionEvento 2 terminan en la DLQ', async () => {
      const antes = await contarEnCola(COLA_EVALUACION_PREGUNTAS_DLQ);

      canalCrudo.publish(
        EXCHANGE_EDITORIAL_EVENTOS,
        ROUTING_KEY_PREGUNTA_PUBLICADA,
        Buffer.from('{ json invalido'),
        { contentType: 'application/json' },
      );
      const ejemploVersionIncorrecta = { ...leerEjemplo('pregunta-publicada.v1.ejemplo.json'), versionEvento: 2 };
      await publicarEnEditorialEventos(ROUTING_KEY_PREGUNTA_PUBLICADA, ejemploVersionIncorrecta);

      await esperar(async () => (await contarEnCola(COLA_EVALUACION_PREGUNTAS_DLQ)) >= antes + 2);
    });
  });

  describe('Publicador (PublicadorEventosRabbitMqAdaptador)', () => {
    it('6. finalizar un intento publica IntentoDeSimulacroCalificado valido segun el esquema', async () => {
      const preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioEnMemoria();
      const intentoDeSimulacroRepositorio = new IntentoDeSimulacroRepositorioEnMemoria();
      const pregunta = crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A });
      preguntaEvaluableRepositorio.agregar(pregunta);

      const simulacro = Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: 'Simulacro de mensajeria',
        criterio: CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] }),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas: [pregunta],
        ahora: new Date('2026-10-01T10:00:00Z'),
      });
      const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, new Date('2026-10-01T10:00:00Z'));
      intento.registrarRespuesta(pregunta.preguntaId, LetraOpcion.A, new Date('2026-10-01T10:01:00Z'));
      await intentoDeSimulacroRepositorio.guardar(intento);

      const conexionPublicador = new ConexionRabbitMqServicio(configuracion);
      const publicador = new PublicadorEventosRabbitMqAdaptador(conexionPublicador, configuracion);
      const cierreDeIntento = new CierreDeIntento(
        preguntaEvaluableRepositorio,
        intentoDeSimulacroRepositorio,
        new CalificadorSimulacroServicio(),
        publicador,
      );
      const finalizarIntento = new FinalizarIntentoCasoUso(
        intentoDeSimulacroRepositorio,
        preguntaEvaluableRepositorio,
        cierreDeIntento,
        new RelojFijo(new Date('2026-10-01T10:05:00Z')),
      );

      // Cola temporal enlazada a intento.calificado, antes de finalizar.
      await canalCrudo.assertQueue('prueba.intento.calificado', { exclusive: false, autoDelete: true });
      await canalCrudo.bindQueue('prueba.intento.calificado', EXCHANGE_EVALUACION_EVENTOS, ROUTING_KEY_INTENTO_CALIFICADO);

      await finalizarIntento.ejecutar({
        usuario: new UsuarioActual(ID_ESTUDIANTE, [Rol.ESTUDIANTE]),
        intentoId: intento.intentoId.aTexto(),
      });

      let mensajeObtenido: amqplib.GetMessage | false = false;
      await esperar(async () => {
        mensajeObtenido = await canalCrudo.get('prueba.intento.calificado', {});
        return mensajeObtenido !== false;
      });

      expect(mensajeObtenido).not.toBe(false);
      const mensaje = mensajeObtenido as unknown as amqplib.GetMessage;
      const payload = JSON.parse(mensaje.content.toString('utf-8'));

      const ajv = new Ajv({ strict: false });
      addFormats(ajv);
      const validar = ajv.compile(ESQUEMA_INTENTO_CALIFICADO);
      const esValido = validar(payload);
      expect(esValido).toBe(true);
      if (!esValido) {
        // eslint-disable-next-line no-console
        console.error(validar.errors);
      }

      await conexionPublicador.onModuleDestroy();
    });
  });
});
