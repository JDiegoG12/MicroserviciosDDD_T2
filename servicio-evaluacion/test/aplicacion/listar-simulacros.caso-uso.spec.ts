import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { ListarSimulacrosCasoUso } from '../../src/aplicacion/simulacros/listar-simulacros.caso-uso';
import { SimulacroRepositorioEnMemoria } from '../dobles/simulacro-repositorio-en-memoria';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { ID_DOCENTE } from '../apoyo/datos-de-prueba';
import { crearAdministrador, crearDocente, crearEstudiante } from '../apoyo/fabrica-usuarios';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

function criterioSinFiltro() {
  return CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] });
}

describe('ListarSimulacrosCasoUso', () => {
  it('lista todos los simulacros definidos, para DOCENTE y ESTUDIANTE', async () => {
    const simulacroRepositorio = new SimulacroRepositorioEnMemoria();
    const simulacro = Simulacro.definir({
      docenteId: ID_DOCENTE,
      nombre: 'Simulacro 1',
      criterio: criterioSinFiltro(),
      duracion: DuracionMaxima.enMinutos(30),
      preguntas: [crearPreguntaPublicada()],
      ahora: new Date(),
    });
    await simulacroRepositorio.guardar(simulacro);
    const casoUso = new ListarSimulacrosCasoUso(simulacroRepositorio);

    const respuestaDocente = await casoUso.ejecutar({ usuario: crearDocente() });
    const respuestaEstudiante = await casoUso.ejecutar({ usuario: crearEstudiante() });

    expect(respuestaDocente.contenido).toHaveLength(1);
    expect(respuestaDocente.totalElementos).toBe(1);
    expect(respuestaDocente.contenido[0]).not.toHaveProperty('preguntas');
    expect(respuestaEstudiante.contenido).toHaveLength(1);
  });

  it('pagina el listado segun CONTRATOS.md 5.1', async () => {
    const simulacroRepositorio = new SimulacroRepositorioEnMemoria();
    for (let i = 0; i < 3; i += 1) {
      await simulacroRepositorio.guardar(
        Simulacro.definir({
          docenteId: ID_DOCENTE,
          nombre: `Simulacro ${i}`,
          criterio: criterioSinFiltro(),
          duracion: DuracionMaxima.enMinutos(30),
          preguntas: [crearPreguntaPublicada()],
          ahora: new Date(),
        }),
      );
    }
    const casoUso = new ListarSimulacrosCasoUso(simulacroRepositorio);

    const pagina = await casoUso.ejecutar({ usuario: crearDocente(), pagina: 0, tamano: 2 });

    expect(pagina.contenido).toHaveLength(2);
    expect(pagina.totalElementos).toBe(3);
    expect(pagina.totalPaginas).toBe(2);
  });

  it('rechaza un tamano mayor a 100 con SOLICITUD_INVALIDA', async () => {
    const casoUso = new ListarSimulacrosCasoUso(new SimulacroRepositorioEnMemoria());

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearDocente(), tamano: 101 }),
    );

    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('rechaza a un usuario sin rol DOCENTE ni ESTUDIANTE', async () => {
    const casoUso = new ListarSimulacrosCasoUso(new SimulacroRepositorioEnMemoria());

    const error = await capturarErrorAsincrono(() => casoUso.ejecutar({ usuario: crearAdministrador() }));

    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });
});
