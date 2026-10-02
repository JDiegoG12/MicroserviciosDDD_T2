import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { Calificacion } from '../../src/dominio/intentos/calificacion';
import { EstadoIntento } from '../../src/dominio/intentos/estado-intento';
import { IntentoDeSimulacroCalificadoEvento } from '../../src/dominio/intentos/eventos/intento-de-simulacro-calificado.evento';
import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { capturarError } from '../apoyo/afirmaciones';
import { ID_DOCENTE, ID_ESTUDIANTE, ID_OTRO_ESTUDIANTE } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

const FECHA_INICIO = new Date('2026-10-01T15:40:00Z');

function construirSimulacro(cantidadPreguntas = 2, duracionMinutos = 30) {
  const preguntas = Array.from({ length: cantidadPreguntas }, () => crearPreguntaPublicada());
  const criterio = CriterioDeGeneracion.crear({
    competenciaIds: [],
    temaIds: [],
    subtemaIds: [],
    nivelesDificultad: [],
  });
  return Simulacro.definir({
    docenteId: ID_DOCENTE,
    nombre: 'Simulacro de prueba',
    criterio,
    duracion: DuracionMaxima.enMinutos(duracionMinutos),
    preguntas,
    ahora: FECHA_INICIO,
  });
}

describe('IntentoDeSimulacro.iniciar', () => {
  it('calcula fechaLimite = fechaInicio + duracionMaximaMinutos y registra IntentoDeSimulacroIniciado', () => {
    const simulacro = construirSimulacro(2, 30);

    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);

    expect(intento.estado).toBe(EstadoIntento.EN_CURSO);
    expect(intento.fechaLimite.toISOString()).toBe('2026-10-01T16:10:00.000Z');
    const eventos = intento.extraerEventos();
    expect(eventos).toHaveLength(1);
    expect(eventos[0].tipoEvento).toBe('IntentoDeSimulacroIniciado');
  });

  it('INV-29 (1): rechaza un estudianteId invalido con SOLICITUD_INVALIDA', () => {
    const simulacro = construirSimulacro();
    const error = capturarError(() => IntentoDeSimulacro.iniciar(simulacro, 'no-es-uuid', FECHA_INICIO));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });
});

describe('IntentoDeSimulacro.verificarPropietario (INV-29, 2)', () => {
  it('rechaza a un usuario distinto del dueno con ACCESO_DENEGADO', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);

    const error = capturarError(() => intento.verificarPropietario(ID_OTRO_ESTUDIANTE));
    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });

  it('acepta al dueno sin lanzar', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);

    expect(() => intento.verificarPropietario(ID_ESTUDIANTE)).not.toThrow();
  });
});

describe('IntentoDeSimulacro.registrarRespuesta', () => {
  it('INV-29 (3): rechaza una pregunta que no pertenece al simulacro', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);

    const error = capturarError(() =>
      intento.registrarRespuesta('00000000-0000-4000-8000-000000000000', LetraOpcion.A, FECHA_INICIO),
    );
    expect(error.codigo).toBe(CodigoError.PREGUNTA_NO_PERTENECE_AL_SIMULACRO);
  });

  it('rechaza una letra fuera de A-D con SOLICITUD_INVALIDA', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    const [primeraPregunta] = intento.preguntaIds();

    const error = capturarError(() => intento.registrarRespuesta(primeraPregunta, 'Z', FECHA_INICIO));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('INV-30: la ultima respuesta reemplaza a la anterior', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    const [primeraPregunta] = intento.preguntaIds();

    intento.registrarRespuesta(primeraPregunta, LetraOpcion.A, FECHA_INICIO);
    intento.registrarRespuesta(primeraPregunta, LetraOpcion.D, FECHA_INICIO);

    expect(intento.respuestaPara(primeraPregunta)?.letraSeleccionada).toBe(LetraOpcion.D);
    expect(intento.todasLasRespuestas()).toHaveLength(1);
  });

  it('INV-31: responder un intento ya finalizado lanza INTENTO_FINALIZADO', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    const [primeraPregunta] = intento.preguntaIds();
    intento.finalizar('ESTUDIANTE', FECHA_INICIO);

    const error = capturarError(() => intento.registrarRespuesta(primeraPregunta, LetraOpcion.A, FECHA_INICIO));
    expect(error.codigo).toBe(CodigoError.INTENTO_FINALIZADO);
  });

  it('vencimiento perezoso: responder en o despues de fechaLimite lanza INTENTO_FINALIZADO y queda TIEMPO_AGOTADO', () => {
    const simulacro = construirSimulacro(2, 30);
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    const [primeraPregunta] = intento.preguntaIds();
    const momentoVencido = intento.fechaLimite;

    const error = capturarError(() => intento.registrarRespuesta(primeraPregunta, LetraOpcion.A, momentoVencido));

    expect(error.codigo).toBe(CodigoError.INTENTO_FINALIZADO);
    expect(intento.estado).toBe(EstadoIntento.FINALIZADO);
    expect(intento.finalizadoPor).toBe('TIEMPO_AGOTADO');
    expect(intento.fechaFinalizacion?.getTime()).toBe(momentoVencido.getTime());
  });
});

describe('IntentoDeSimulacro.finalizar', () => {
  it('registra IntentoDeSimulacroFinalizado y cambia el estado a FINALIZADO', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    intento.extraerEventos();

    intento.finalizar('ESTUDIANTE', FECHA_INICIO);

    expect(intento.estado).toBe(EstadoIntento.FINALIZADO);
    const eventos = intento.extraerEventos();
    expect(eventos).toHaveLength(1);
    expect(eventos[0].tipoEvento).toBe('IntentoDeSimulacroFinalizado');
  });

  it('finalizar por segunda vez lanza INTENTO_FINALIZADO', () => {
    const simulacro = construirSimulacro();
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_INICIO);

    const error = capturarError(() => intento.finalizar('ESTUDIANTE', FECHA_INICIO));
    expect(error.codigo).toBe(CodigoError.INTENTO_FINALIZADO);
  });
});

describe('IntentoDeSimulacro.cerrarConCalificacion (INV-32)', () => {
  function calificacionDePrueba(totalPreguntas: number) {
    return Calificacion.crear({
      totalPreguntas,
      correctas: totalPreguntas,
      desglosePorCompetencia: [
        { competenciaId: 'cualquiera', totalPreguntas, correctas: totalPreguntas, puntaje: 100 },
      ],
    });
  }

  it('rechaza calificar un intento que aun esta EN_CURSO', () => {
    const simulacro = construirSimulacro(2);
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);

    const error = capturarError(() => intento.cerrarConCalificacion(calificacionDePrueba(2)));
    expect(error.codigo).toBe(CodigoError.TRANSICION_NO_PERMITIDA);
  });

  it('cierra un intento FINALIZADO y registra IntentoDeSimulacroCalificado con los datos de 7.6', () => {
    const simulacro = construirSimulacro(2);
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', new Date('2026-10-01T16:00:00Z'));
    intento.extraerEventos();

    intento.cerrarConCalificacion(calificacionDePrueba(2));

    expect(intento.estado).toBe(EstadoIntento.CALIFICADO);
    const eventos = intento.extraerEventos();
    expect(eventos).toHaveLength(1);
    const evento = eventos[0] as IntentoDeSimulacroCalificadoEvento;
    expect(evento.tipoEvento).toBe('IntentoDeSimulacroCalificado');
    expect(evento.intentoId).toBe(intento.intentoId.aTexto());
    expect(evento.simulacroId).toBe(intento.simulacroId.aTexto());
    expect(evento.estudianteId).toBe(ID_ESTUDIANTE);
    expect(evento.fechaInicio).toEqual(FECHA_INICIO);
    expect(evento.fechaFinalizacion).toEqual(new Date('2026-10-01T16:00:00Z'));
    expect(evento.finalizadoPor).toBe('ESTUDIANTE');
    expect(evento.calificacion.puntaje).toBe(100);
  });

  it('rechaza calificar dos veces el mismo intento con INTENTO_YA_CALIFICADO', () => {
    const simulacro = construirSimulacro(2);
    const intento = IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_INICIO);
    intento.cerrarConCalificacion(calificacionDePrueba(2));

    const error = capturarError(() => intento.cerrarConCalificacion(calificacionDePrueba(2)));
    expect(error.codigo).toBe(CodigoError.INTENTO_YA_CALIFICADO);
  });
});
