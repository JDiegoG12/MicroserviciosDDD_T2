import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { ObtenerSimulacroCasoUso } from '../../src/aplicacion/simulacros/obtener-simulacro.caso-uso';
import { SimulacroRepositorioEnMemoria } from '../dobles/simulacro-repositorio-en-memoria';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { ID_DOCENTE } from '../apoyo/datos-de-prueba';
import { crearDocente } from '../apoyo/fabrica-usuarios';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

function criterioSinFiltro() {
  return CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] });
}

describe('ObtenerSimulacroCasoUso', () => {
  it('devuelve el SimulacroRespuesta sin letraCorrecta', async () => {
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
    const casoUso = new ObtenerSimulacroCasoUso(simulacroRepositorio);

    const respuesta = await casoUso.ejecutar({
      usuario: crearDocente(),
      simulacroId: simulacro.simulacroId.aTexto(),
    });

    expect(respuesta.simulacroId).toBe(simulacro.simulacroId.aTexto());
    expect(JSON.stringify(respuesta)).not.toContain('letraCorrecta');
  });

  it('lanza SIMULACRO_NO_ENCONTRADO si no existe', async () => {
    const casoUso = new ObtenerSimulacroCasoUso(new SimulacroRepositorioEnMemoria());

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearDocente(), simulacroId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f' }),
    );

    expect(error.codigo).toBe(CodigoError.SIMULACRO_NO_ENCONTRADO);
  });
});
