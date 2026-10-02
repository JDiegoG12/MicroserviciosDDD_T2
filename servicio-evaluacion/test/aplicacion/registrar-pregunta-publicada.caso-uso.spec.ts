import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { EstadoPreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { PreguntaEvaluableRepositorio } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { RegistrarPreguntaPublicadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-publicada.caso-uso';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { RegistroEventosProcesadosEnMemoria } from '../dobles/registro-eventos-procesados-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import {
  ID_COMPETENCIA_RAZONAMIENTO,
  ID_SUBTEMA_MEDIDAS_CENTRALES,
  ID_TEMA_ESTADISTICA,
} from '../apoyo/datos-de-prueba';

describe('RegistrarPreguntaPublicadaCasoUso', () => {
  it('crea la copia local en estado PUBLICADA la primera vez que llega el evento', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const casoUso = new RegistrarPreguntaPublicadaCasoUso(
      repositorio,
      new RegistroEventosProcesadosEnMemoria(),
      new RelojFijo(),
    );

    const resultado = await casoUso.ejecutar({
      idEvento: '6d1f3a0c-3b2e-4f7a-9c1d-2e3f4a5b6c7d',
      preguntaId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f',
      contexto: 'Contexto de prueba.',
      preguntaDirecta: 'Pregunta directa de prueba?',
      opciones: [
        { letra: LetraOpcion.A, texto: 'A' },
        { letra: LetraOpcion.B, texto: 'B' },
        { letra: LetraOpcion.C, texto: 'C' },
        { letra: LetraOpcion.D, texto: 'D' },
      ],
      letraCorrecta: LetraOpcion.A,
      clasificacion: {
        competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
        temaId: ID_TEMA_ESTADISTICA,
        subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
      },
      nivelDificultad: NivelDificultad.BAJO,
      fechaPublicacion: new Date('2026-10-01T15:30:00Z'),
    });

    expect(resultado).toBe('REGISTRADO');
    const pregunta = await repositorio.obtenerPorId('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    expect(pregunta?.estado).toBe(EstadoPreguntaEvaluable.PUBLICADA);
  });

  it('propaga SOLICITUD_INVALIDA si el contenido del evento no cumple el contrato', async () => {
    const casoUso = new RegistrarPreguntaPublicadaCasoUso(
      new PreguntaEvaluableRepositorioEnMemoria(),
      new RegistroEventosProcesadosEnMemoria(),
      new RelojFijo(),
    );

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        idEvento: '6d1f3a0c-3b2e-4f7a-9c1d-2e3f4a5b6c7d',
        preguntaId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f',
        contexto: 'Contexto',
        preguntaDirecta: 'Pregunta',
        opciones: [
          { letra: LetraOpcion.A, texto: 'A' },
          { letra: LetraOpcion.A, texto: 'A repetida' },
          { letra: LetraOpcion.C, texto: 'C' },
          { letra: LetraOpcion.D, texto: 'D' },
        ],
        letraCorrecta: LetraOpcion.A,
        clasificacion: {
          competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
          temaId: ID_TEMA_ESTADISTICA,
          subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
        },
        nivelDificultad: NivelDificultad.BAJO,
        fechaPublicacion: new Date('2026-10-01T15:30:00Z'),
      }),
    );

    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('CONTRATOS 7.7.2: si el guardado falla, libera el reclamo y un reenvio del mismo idEvento si se procesa', async () => {
    const repositorioBase = new PreguntaEvaluableRepositorioEnMemoria();
    let primeraLlamada = true;
    const repositorioQueFallaUnaVez: PreguntaEvaluableRepositorio = {
      guardar: async (pregunta) => {
        if (primeraLlamada) {
          primeraLlamada = false;
          throw new Error('Fallo transitorio simulado (por ejemplo, Mongo cayendo a mitad del guardado).');
        }
        return repositorioBase.guardar(pregunta);
      },
      obtenerPorId: (id) => repositorioBase.obtenerPorId(id),
      obtenerPorIds: (ids) => repositorioBase.obtenerPorIds(ids),
      buscarPublicadasPorCriterios: (criterio) => repositorioBase.buscarPublicadasPorCriterios(criterio),
      buscarPaginado: (filtros, pagina, tamano) => repositorioBase.buscarPaginado(filtros, pagina, tamano),
    };
    const registroEventosProcesados = new RegistroEventosProcesadosEnMemoria();
    const casoUso = new RegistrarPreguntaPublicadaCasoUso(
      repositorioQueFallaUnaVez,
      registroEventosProcesados,
      new RelojFijo(),
    );
    const comando = {
      idEvento: '6d1f3a0c-3b2e-4f7a-9c1d-2e3f4a5b6c7d',
      preguntaId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f',
      contexto: 'Contexto de prueba.',
      preguntaDirecta: 'Pregunta directa de prueba?',
      opciones: [
        { letra: LetraOpcion.A, texto: 'A' },
        { letra: LetraOpcion.B, texto: 'B' },
        { letra: LetraOpcion.C, texto: 'C' },
        { letra: LetraOpcion.D, texto: 'D' },
      ],
      letraCorrecta: LetraOpcion.A,
      clasificacion: {
        competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
        temaId: ID_TEMA_ESTADISTICA,
        subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
      },
      nivelDificultad: NivelDificultad.BAJO,
      fechaPublicacion: new Date('2026-10-01T15:30:00Z'),
    };

    // Primer intento: el guardado falla. El caso de uso debe relanzar el
    // error (para que el consumidor decida el nack) y liberar el reclamo.
    const primerError = await capturarErrorAsincrono(() => casoUso.ejecutar(comando));
    expect(primerError.message).toContain('Fallo transitorio simulado');
    expect(registroEventosProcesados.estaReclamado(comando.idEvento)).toBe(false);

    // Reenvio del mismo idEvento (por ejemplo, desde la DLQ): como el
    // reclamo se libero, se procesa de verdad y la pregunta queda guardada.
    const resultado = await casoUso.ejecutar(comando);
    expect(resultado).toBe('REGISTRADO');
    const pregunta = await repositorioBase.obtenerPorId(comando.preguntaId);
    expect(pregunta?.estado).toBe(EstadoPreguntaEvaluable.PUBLICADA);
  });
});
