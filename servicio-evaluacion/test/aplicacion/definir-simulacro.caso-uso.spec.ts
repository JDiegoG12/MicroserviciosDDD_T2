import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { EnsambladorSimulacroServicio } from '../../src/dominio/servicios/ensamblador-simulacro.servicio';
import { DefinirSimulacroCasoUso } from '../../src/aplicacion/simulacros/definir-simulacro.caso-uso';
import { FuenteAleatoriaFija } from '../dobles/fuente-aleatoria-fija';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { PublicadorEventosEnMemoria } from '../dobles/publicador-eventos-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';
import { SimulacroRepositorioEnMemoria } from '../dobles/simulacro-repositorio-en-memoria';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { ID_COMPETENCIA_RAZONAMIENTO } from '../apoyo/datos-de-prueba';
import { crearDocente, crearEstudiante } from '../apoyo/fabrica-usuarios';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

function construirCasoUso() {
  const preguntaEvaluableRepositorio = new PreguntaEvaluableRepositorioEnMemoria();
  const simulacroRepositorio = new SimulacroRepositorioEnMemoria();
  const ensamblador = new EnsambladorSimulacroServicio(new FuenteAleatoriaFija());
  const publicadorEventos = new PublicadorEventosEnMemoria();
  const reloj = new RelojFijo();

  const casoUso = new DefinirSimulacroCasoUso(
    preguntaEvaluableRepositorio,
    simulacroRepositorio,
    ensamblador,
    publicadorEventos,
    reloj,
  );

  return { casoUso, preguntaEvaluableRepositorio, simulacroRepositorio, publicadorEventos, reloj };
}

describe('DefinirSimulacroCasoUso (CU-13)', () => {
  it('define un simulacro, lo guarda y publica SimulacroDefinido', async () => {
    const { casoUso, preguntaEvaluableRepositorio, simulacroRepositorio, publicadorEventos } = construirCasoUso();
    preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }));
    preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }));

    const respuesta = await casoUso.ejecutar({
      usuario: crearDocente(),
      nombre: 'Simulacro cuantitativo 1',
      criterios: {
        competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO],
        temaIds: [],
        subtemaIds: [],
        nivelesDificultad: [],
      },
      cantidadPreguntas: 2,
      duracionMaximaMinutos: 30,
    });

    expect(respuesta.preguntas).toHaveLength(2);
    expect(simulacroRepositorio.cantidadDeGuardados).toBe(1);
    expect(publicadorEventos.buscarPorTipo('SimulacroDefinido')).toBeDefined();
  });

  it('rechaza a un usuario sin rol DOCENTE con ACCESO_DENEGADO', async () => {
    const { casoUso } = construirCasoUso();

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        usuario: crearEstudiante(),
        nombre: 'Simulacro',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 1,
        duracionMaximaMinutos: 30,
      }),
    );

    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });

  it('rechaza una duracion invalida con DURACION_INVALIDA antes de buscar preguntas', async () => {
    const { casoUso, preguntaEvaluableRepositorio } = construirCasoUso();

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        usuario: crearDocente(),
        nombre: 'Simulacro',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 1,
        duracionMaximaMinutos: 0,
      }),
    );

    expect(error.codigo).toBe(CodigoError.DURACION_INVALIDA);
    expect(preguntaEvaluableRepositorio.cantidadDeGuardados).toBe(0);
  });

  it('propaga PREGUNTAS_INSUFICIENTES cuando no hay suficientes candidatas', async () => {
    const { casoUso } = construirCasoUso();

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        usuario: crearDocente(),
        nombre: 'Simulacro',
        criterios: {
          competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO],
          temaIds: [],
          subtemaIds: [],
          nivelesDificultad: [NivelDificultad.ALTO],
        },
        cantidadPreguntas: 3,
        duracionMaximaMinutos: 30,
      }),
    );

    expect(error.codigo).toBe(CodigoError.PREGUNTAS_INSUFICIENTES);
  });
});
