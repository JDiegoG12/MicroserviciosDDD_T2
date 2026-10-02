import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { IniciarIntentoCasoUso } from '../../src/aplicacion/intentos/iniciar-intento.caso-uso';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { construirEntornoIntentos } from '../apoyo/construir-entorno-intentos';
import { crearDocente, crearEstudiante } from '../apoyo/fabrica-usuarios';

describe('IniciarIntentoCasoUso (CU-14, apertura)', () => {
  it('abre un intento EN_CURSO sobre un simulacro existente, sin letraCorrecta', async () => {
    const entorno = await construirEntornoIntentos();
    const casoUso = new IniciarIntentoCasoUso(
      entorno.simulacroRepositorio,
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.publicadorEventos,
      entorno.reloj,
    );

    const respuesta = await casoUso.ejecutar({
      usuario: crearEstudiante(),
      simulacroId: entorno.simulacro.simulacroId.aTexto(),
    });

    expect(respuesta.estado).toBe('EN_CURSO');
    expect(respuesta.preguntas).toHaveLength(2);
    expect(JSON.stringify(respuesta)).not.toContain('letraCorrecta');
    expect(entorno.intentoDeSimulacroRepositorio.cantidadDeGuardados).toBe(1);
  });

  it('lanza SIMULACRO_NO_ENCONTRADO si el simulacro no existe', async () => {
    const entorno = await construirEntornoIntentos();
    const casoUso = new IniciarIntentoCasoUso(
      entorno.simulacroRepositorio,
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.publicadorEventos,
      entorno.reloj,
    );

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearEstudiante(), simulacroId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f' }),
    );

    expect(error.codigo).toBe(CodigoError.SIMULACRO_NO_ENCONTRADO);
  });

  it('rechaza a un usuario sin rol ESTUDIANTE con ACCESO_DENEGADO', async () => {
    const entorno = await construirEntornoIntentos();
    const casoUso = new IniciarIntentoCasoUso(
      entorno.simulacroRepositorio,
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.publicadorEventos,
      entorno.reloj,
    );

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearDocente(), simulacroId: entorno.simulacro.simulacroId.aTexto() }),
    );

    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });
});
