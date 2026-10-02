import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { FinalizarIntentoCasoUso } from '../../src/aplicacion/intentos/finalizar-intento.caso-uso';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { construirEntornoIntentos } from '../apoyo/construir-entorno-intentos';
import { crearEstudiante } from '../apoyo/fabrica-usuarios';

describe('FinalizarIntentoCasoUso (CU-14 + CU-15)', () => {
  it('finaliza y califica el intento en la misma llamada, y publica IntentoDeSimulacroCalificado', async () => {
    const entorno = await construirEntornoIntentos(2, 30);
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new FinalizarIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );

    const respuesta = await casoUso.ejecutar({ usuario: estudiante, intentoId: intento.intentoId.aTexto() });

    expect(respuesta.estado).toBe('CALIFICADO');
    expect(respuesta.finalizadoPor).toBe('ESTUDIANTE');
    expect(respuesta.calificacion).not.toBeNull();
    expect(respuesta.calificacion?.totalPreguntas).toBe(2);

    const evento = entorno.publicadorEventos.buscarPorTipo<{
      tipoEvento: string;
      fechaOcurrencia: Date;
      intentoId: string;
      simulacroId: string;
      estudianteId: string;
      fechaInicio: Date;
      fechaFinalizacion: Date;
      finalizadoPor: string;
      calificacion: { totalPreguntas: number };
    }>('IntentoDeSimulacroCalificado');
    expect(evento).toBeDefined();
    expect(evento?.intentoId).toBe(intento.intentoId.aTexto());
    expect(evento?.simulacroId).toBe(entorno.simulacro.simulacroId.aTexto());
    expect(evento?.estudianteId).toBe(estudiante.id);
    expect(evento?.finalizadoPor).toBe('ESTUDIANTE');
    expect(evento?.calificacion.totalPreguntas).toBe(2);
  });

  it('finalizar un intento ya calificado lanza INTENTO_FINALIZADO', async () => {
    const entorno = await construirEntornoIntentos(2, 30);
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new FinalizarIntentoCasoUso(
      entorno.intentoDeSimulacroRepositorio,
      entorno.preguntaEvaluableRepositorio,
      entorno.cierreDeIntento,
      entorno.reloj,
    );
    await casoUso.ejecutar({ usuario: estudiante, intentoId: intento.intentoId.aTexto() });

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: estudiante, intentoId: intento.intentoId.aTexto() }),
    );

    expect(error.codigo).toBe(CodigoError.INTENTO_FINALIZADO);
  });

  it('si el vencimiento perezoso cierra el intento en la misma llamada, responde normal con TIEMPO_AGOTADO', async () => {
    const entorno = await construirEntornoIntentos(2, 30);
    const estudiante = crearEstudiante();
    const intento = IntentoDeSimulacro.iniciar(entorno.simulacro, estudiante.id, entorno.reloj.ahora());
    await entorno.intentoDeSimulacroRepositorio.guardar(intento);
    const casoUso = new FinalizarIntentoCasoUso(
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
});
