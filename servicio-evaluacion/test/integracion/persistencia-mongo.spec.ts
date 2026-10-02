import { GenericContainer, StartedTestContainer, Wait } from 'testcontainers';
import mongoose, { Connection } from 'mongoose';
import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { PreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { esquemaPreguntaEvaluable } from '../../src/infraestructura/persistencia/mongo/pregunta-evaluable.schema';
import { PreguntaEvaluableRepositorioMongo } from '../../src/infraestructura/persistencia/mongo/pregunta-evaluable.repositorio-mongo';
import { esquemaSimulacro } from '../../src/infraestructura/persistencia/mongo/simulacro.schema';
import { SimulacroRepositorioMongo } from '../../src/infraestructura/persistencia/mongo/simulacro.repositorio-mongo';
import { esquemaIntentoDeSimulacro } from '../../src/infraestructura/persistencia/mongo/intento-de-simulacro.schema';
import { IntentoDeSimulacroRepositorioMongo } from '../../src/infraestructura/persistencia/mongo/intento-de-simulacro.repositorio-mongo';
import { esquemaEventoProcesado } from '../../src/infraestructura/persistencia/mongo/evento-procesado.schema';
import { RegistroEventosProcesadosRepositorioMongo } from '../../src/infraestructura/persistencia/mongo/registro-eventos-procesados.repositorio-mongo';
import { ID_COMPETENCIA_DISENO, ID_COMPETENCIA_RAZONAMIENTO, ID_DOCENTE, ID_ESTUDIANTE } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

jest.setTimeout(120_000);

describe('Repositorios Mongo (integracion con Testcontainers, mongo:7)', () => {
  let contenedor: StartedTestContainer;
  let conexion: Connection;
  let preguntaEvaluableRepositorio: PreguntaEvaluableRepositorioMongo;
  let simulacroRepositorio: SimulacroRepositorioMongo;
  let intentoDeSimulacroRepositorio: IntentoDeSimulacroRepositorioMongo;
  let registroEventosProcesados: RegistroEventosProcesadosRepositorioMongo;

  beforeAll(async () => {
    contenedor = await new GenericContainer('mongo:7')
      .withExposedPorts(27017)
      .withWaitStrategy(Wait.forLogMessage(/Waiting for connections/))
      .start();

    const uri = `mongodb://${contenedor.getHost()}:${contenedor.getMappedPort(27017)}/evaluacion-prueba`;
    conexion = (await mongoose.createConnection(uri).asPromise()) as Connection;

    const modeloPregunta = conexion.model('PreguntaEvaluable', esquemaPreguntaEvaluable);
    const modeloSimulacro = conexion.model('Simulacro', esquemaSimulacro);
    const modeloIntento = conexion.model('IntentoDeSimulacro', esquemaIntentoDeSimulacro);
    const modeloEventoProcesado = conexion.model('EventoProcesado', esquemaEventoProcesado);

    preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioMongo(modeloPregunta);
    simulacroRepositorio = new SimulacroRepositorioMongo(modeloSimulacro);
    intentoDeSimulacroRepositorio = new IntentoDeSimulacroRepositorioMongo(modeloIntento);
    registroEventosProcesados = new RegistroEventosProcesadosRepositorioMongo(modeloEventoProcesado);
  });

  afterAll(async () => {
    await conexion?.close();
    await contenedor?.stop();
  });

  describe('PreguntaEvaluableRepositorioMongo', () => {
    it('guarda y recupera una pregunta por id, con _id = preguntaId en texto', async () => {
      const pregunta = crearPreguntaPublicada();
      await preguntaEvaluableRepositorio.guardar(pregunta);

      const encontrada = await preguntaEvaluableRepositorio.obtenerPorId(pregunta.preguntaId);

      expect(encontrada).not.toBeNull();
      expect(encontrada?.preguntaId).toBe(pregunta.preguntaId);
      expect(encontrada?.contenido?.contexto).toBe(pregunta.contenido?.contexto);
    });

    it('obtenerPorIds devuelve solo las que existen', async () => {
      const pregunta = crearPreguntaPublicada();
      await preguntaEvaluableRepositorio.guardar(pregunta);

      const encontradas = await preguntaEvaluableRepositorio.obtenerPorIds([
        pregunta.preguntaId,
        '00000000-0000-4000-8000-000000000000',
      ]);

      expect(encontradas).toHaveLength(1);
    });

    it('buscarPublicadasPorCriterios aplica $in dentro de cada lista y Y entre listas', async () => {
      const coincide = crearPreguntaPublicada({
        competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
        nivelDificultad: NivelDificultad.ALTO,
      });
      const noCoincide = crearPreguntaPublicada({
        competenciaId: ID_COMPETENCIA_DISENO,
        nivelDificultad: NivelDificultad.ALTO,
      });
      await preguntaEvaluableRepositorio.guardar(coincide);
      await preguntaEvaluableRepositorio.guardar(noCoincide);

      const criterio = CriterioDeGeneracion.crear({
        competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO],
        temaIds: [],
        subtemaIds: [],
        nivelesDificultad: [NivelDificultad.ALTO],
      });
      const resultado = await preguntaEvaluableRepositorio.buscarPublicadasPorCriterios(criterio);

      expect(resultado.map((p) => p.preguntaId)).toContain(coincide.preguntaId);
      expect(resultado.map((p) => p.preguntaId)).not.toContain(noCoincide.preguntaId);
    });

    it('buscarPaginado filtra por estado y pagina', async () => {
      const archivada = crearPreguntaPublicada().archivar('Motivo', new Date(), new Date());
      await preguntaEvaluableRepositorio.guardar(archivada);

      const pagina = await preguntaEvaluableRepositorio.buscarPaginado({ estado: archivada.estado }, 0, 100);

      expect(pagina.elementos.some((p) => p.preguntaId === archivada.preguntaId)).toBe(true);
      expect(pagina.elementos.every((p) => p.estado === 'ARCHIVADA')).toBe(true);
    });

    it('incorporarPublicacion sobre una copia leida de Mongo conserva la regla 7.7.3', async () => {
      const marca = PreguntaEvaluable.marcaDeArchivo(
        '11111111-2222-4333-8444-555555555555',
        'Motivo',
        new Date(),
        new Date(),
      );
      await preguntaEvaluableRepositorio.guardar(marca);

      const leida = await preguntaEvaluableRepositorio.obtenerPorId(marca.preguntaId);
      const contenido = crearPreguntaPublicada().contenido!;
      const actualizada = leida!.incorporarPublicacion(contenido, new Date());
      await preguntaEvaluableRepositorio.guardar(actualizada);

      const final = await preguntaEvaluableRepositorio.obtenerPorId(marca.preguntaId);
      expect(final?.estado).toBe('ARCHIVADA');
      expect(final?.contenido).not.toBeNull();
    });
  });

  describe('SimulacroRepositorioMongo', () => {
    it('guarda, recupera por id y lista', async () => {
      const preguntas = [crearPreguntaPublicada(), crearPreguntaPublicada()];
      const simulacro = Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: 'Simulacro de integracion',
        criterio: CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] }),
        duracion: DuracionMaxima.enMinutos(45),
        preguntas,
        ahora: new Date('2026-10-01T10:00:00Z'),
      });

      await simulacroRepositorio.guardar(simulacro);
      const recuperado = await simulacroRepositorio.obtenerPorId(simulacro.simulacroId);
      const listado = await simulacroRepositorio.listar();

      expect(recuperado?.nombre).toBe('Simulacro de integracion');
      expect(recuperado?.preguntas.ids()).toEqual(preguntas.map((p) => p.preguntaId));
      expect(listado.some((s) => s.simulacroId.igualA(simulacro.simulacroId))).toBe(true);
    });
  });

  describe('IntentoDeSimulacroRepositorioMongo', () => {
    it('guarda y recupera un intento con respuestas y calificacion', async () => {
      const preguntas = [crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A })];
      const simulacro = Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: 'Simulacro para intento',
        criterio: CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] }),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas,
        ahora: new Date('2026-10-01T10:00:00Z'),
      });
      const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, new Date('2026-10-01T10:05:00Z'));
      intento.registrarRespuesta(preguntas[0].preguntaId, LetraOpcion.A, new Date('2026-10-01T10:06:00Z'));
      intento.finalizar('ESTUDIANTE', new Date('2026-10-01T10:10:00Z'));

      await intentoDeSimulacroRepositorio.guardar(intento);
      const recuperado = await intentoDeSimulacroRepositorio.obtenerPorId(intento.intentoId);

      expect(recuperado?.estado).toBe('FINALIZADO');
      expect(recuperado?.respuestaPara(preguntas[0].preguntaId)?.letraSeleccionada).toBe('A');
      expect(recuperado?.preguntasSeleccionadas()).toEqual(intento.preguntasSeleccionadas());
    });
  });

  describe('RegistroEventosProcesadosRepositorioMongo (reclamar y liberar si falla, CONTRATOS 7.7.2)', () => {
    it('reclamar es atomico: solo una de dos entregas concurrentes ve true', async () => {
      const idEvento = '22222222-3333-4444-8555-666666666666';

      const [primero, segundo] = await Promise.all([
        registroEventosProcesados.reclamar(idEvento),
        registroEventosProcesados.reclamar(idEvento),
      ]);

      // Una de las dos debe reclamar (true) y la otra debe ver que ya estaba reclamado (false).
      expect([primero, segundo].sort()).toEqual([false, true]);

      const terceraConsulta = await registroEventosProcesados.reclamar(idEvento);
      expect(terceraConsulta).toBe(false);
    });

    it('liberar permite reclamar de nuevo el mismo idEvento', async () => {
      const idEvento = '33333333-4444-4555-8666-777777777777';
      await registroEventosProcesados.reclamar(idEvento);

      await registroEventosProcesados.liberar(idEvento);

      const reclamadoDeNuevo = await registroEventosProcesados.reclamar(idEvento);
      expect(reclamadoDeNuevo).toBe(true);
    });

    it('liberar nunca lanza, incluso si el idEvento no estaba reclamado', async () => {
      await expect(registroEventosProcesados.liberar('44444444-5555-4666-8777-000000000000')).resolves.not.toThrow();
    });
  });
});
