import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { ObtenerIntentoCasoUso } from '../../src/aplicacion/intentos/obtener-intento.caso-uso';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { construirEntornoIntentos } from '../apoyo/construir-entorno-intentos';
import { crearDocente, crearEstudiante, crearOtroEstudiante } from '../apoyo/fabrica-usuarios';

describe('ObtenerIntentoCasoUso', () => {
  it('permite al estudiante dueno consultar su propio intento', async () => {
    const entorno = await construirEntornoIntentos();
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new ObtenerIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );

    const respuesta = await casoUso.ejecutar({ usuario: estudiante, intentoId: intento.intentoId.aTexto() });

    expect(respuesta.intentoId).toBe(intento.intentoId.aTexto());
  });

  it('permite a un DOCENTE consultar un intento que no es suyo (CONTRATOS.md 8.3)', async () => {
    const entorno = await construirEntornoIntentos();
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new ObtenerIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );

    const respuesta = await casoUso.ejecutar({ usuario: crearDocente(), intentoId: intento.intentoId.aTexto() });

    expect(respuesta.intentoId).toBe(intento.intentoId.aTexto());
  });

  it('rechaza a un estudiante que no es el dueno con ACCESO_DENEGADO', async () => {
    const entorno = await construirEntornoIntentos();
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new ObtenerIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearOtroEstudiante(), intentoId: intento.intentoId.aTexto() }),
    );

    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });

  it('aplica el vencimiento perezoso tambien al consultar: queda CALIFICADO con TIEMPO_AGOTADO', async () => {
    const entorno = await construirEntornoIntentos(2, 30);
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new ObtenerIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );
    entorno.reloj.avanzarMinutos(31);

    const respuesta = await casoUso.ejecutar({ usuario: estudiante, intentoId: intento.intentoId.aTexto() });

    expect(respuesta.estado).toBe('CALIFICADO');
    expect(respuesta.finalizadoPor).toBe('TIEMPO_AGOTADO');
  });

  it('lanza INTENTO_NO_ENCONTRADO si el intento no existe', async () => {
    const entorno = await construirEntornoIntentos();
    const casoUso = new ObtenerIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearEstudiante(), intentoId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f' }),
    );

    expect(error.codigo).toBe(CodigoError.INTENTO_NO_ENCONTRADO);
  });
});
