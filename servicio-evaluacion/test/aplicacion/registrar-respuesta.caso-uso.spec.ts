import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { EstadoIntento } from '../../src/dominio/intentos/estado-intento';
import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { RegistrarRespuestaCasoUso } from '../../src/aplicacion/intentos/registrar-respuesta.caso-uso';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { construirEntornoIntentos } from '../apoyo/construir-entorno-intentos';
import { crearEstudiante, crearOtroEstudiante } from '../apoyo/fabrica-usuarios';

describe('RegistrarRespuestaCasoUso (CU-14, respuesta)', () => {
  it('registra la respuesta del estudiante dueno del intento', async () => {
    const entorno = await construirEntornoIntentos();
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new RegistrarRespuestaCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.publicadorEventos,
      entorno.reloj,
    );
    const [preguntaId] = intento.preguntaIds();

    const respuesta = await casoUso.ejecutar({
      usuario: estudiante,
      intentoId: intento.intentoId.aTexto(),
      preguntaId,
      letraSeleccionada: LetraOpcion.A,
    });

    expect(respuesta.respuestas).toEqual([{ preguntaId, letraSeleccionada: LetraOpcion.A }]);
  });

  it('rechaza a un estudiante que no es el dueno con ACCESO_DENEGADO', async () => {
    const entorno = await construirEntornoIntentos();
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new RegistrarRespuestaCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.publicadorEventos,
      entorno.reloj,
    );
    const [preguntaId] = intento.preguntaIds();

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        usuario: crearOtroEstudiante(),
        intentoId: intento.intentoId.aTexto(),
        preguntaId,
        letraSeleccionada: LetraOpcion.A,
      }),
    );

    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });

  it('lanza INTENTO_NO_ENCONTRADO si el intento no existe', async () => {
    const entorno = await construirEntornoIntentos();
    const casoUso = new RegistrarRespuestaCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.publicadorEventos,
      entorno.reloj,
    );

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        usuario: crearEstudiante(),
        intentoId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f',
        preguntaId: '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f',
        letraSeleccionada: LetraOpcion.A,
      }),
    );

    expect(error.codigo).toBe(CodigoError.INTENTO_NO_ENCONTRADO);
  });

  it('vencimiento perezoso: responder despues de fechaLimite lanza INTENTO_FINALIZADO y califica con TIEMPO_AGOTADO', async () => {
    const entorno = await construirEntornoIntentos(2, 30);
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new RegistrarRespuestaCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.publicadorEventos,
      entorno.reloj,
    );
    const [preguntaId] = intento.preguntaIds();
    entorno.reloj.avanzarMinutos(31);

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({
        usuario: estudiante,
        intentoId: intento.intentoId.aTexto(),
        preguntaId,
        letraSeleccionada: LetraOpcion.A,
      }),
    );

    expect(error.codigo).toBe(CodigoError.INTENTO_FINALIZADO);
    const intentoGuardado = await entorno.intentoDeSimulacroRepositorio.obtenerPorId(intento.intentoId);
    expect(intentoGuardado?.estado).toBe(EstadoIntento.CALIFICADO);
    expect(intentoGuardado?.finalizadoPor).toBe('TIEMPO_AGOTADO');
    expect(entorno.publicadorEventos.buscarPorTipo('IntentoDeSimulacroCalificado')).toBeDefined();
  });
});
