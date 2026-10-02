import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { capturarError } from '../apoyo/afirmaciones';
import { ID_DOCENTE } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

const AHORA = new Date('2026-10-01T15:00:00Z');

function criterioSinFiltro() {
  return CriterioDeGeneracion.crear({
    competenciaIds: [],
    temaIds: [],
    subtemaIds: [],
    nivelesDificultad: [],
  });
}

describe('Simulacro.definir', () => {
  it('define un simulacro valido y registra el evento SimulacroDefinido', () => {
    const preguntas = [crearPreguntaPublicada(), crearPreguntaPublicada()];

    const simulacro = Simulacro.definir({
      docenteId: ID_DOCENTE,
      nombre: 'Simulacro cuantitativo 1',
      criterio: criterioSinFiltro(),
      duracion: DuracionMaxima.enMinutos(30),
      preguntas,
      ahora: AHORA,
    });

    expect(simulacro.preguntas.ids()).toHaveLength(2);
    const eventos = simulacro.extraerEventos();
    expect(eventos).toHaveLength(1);
    expect(eventos[0].tipoEvento).toBe('SimulacroDefinido');
  });

  it('INV-25: rechaza una pregunta que no esta PUBLICADA con PREGUNTA_NO_PUBLICADA', () => {
    const publicada = crearPreguntaPublicada();
    const archivada = publicada.archivar('Motivo de prueba', AHORA, AHORA);

    const error = capturarError(() =>
      Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: 'Simulacro con archivada',
        criterio: criterioSinFiltro(),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas: [publicada, archivada],
        ahora: AHORA,
      }),
    );

    expect(error.codigo).toBe(CodigoError.PREGUNTA_NO_PUBLICADA);
  });

  it('INV-26: rechaza una lista vacia de preguntas con PREGUNTAS_INSUFICIENTES', () => {
    const error = capturarError(() =>
      Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: 'Simulacro vacio',
        criterio: criterioSinFiltro(),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas: [],
        ahora: AHORA,
      }),
    );

    expect(error.codigo).toBe(CodigoError.PREGUNTAS_INSUFICIENTES);
  });

  it('INV-27: rechaza una pregunta repetida con PREGUNTA_DUPLICADA_EN_SIMULACRO', () => {
    const pregunta = crearPreguntaPublicada();

    const error = capturarError(() =>
      Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: 'Simulacro con repetida',
        criterio: criterioSinFiltro(),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas: [pregunta, pregunta],
        ahora: AHORA,
      }),
    );

    expect(error.codigo).toBe(CodigoError.PREGUNTA_DUPLICADA_EN_SIMULACRO);
  });

  it('asigna la posicion de cada pregunta empezando en 1', () => {
    const preguntas = [crearPreguntaPublicada(), crearPreguntaPublicada(), crearPreguntaPublicada()];

    const simulacro = Simulacro.definir({
      docenteId: ID_DOCENTE,
      nombre: 'Simulacro con posiciones',
      criterio: criterioSinFiltro(),
      duracion: DuracionMaxima.enMinutos(30),
      preguntas,
      ahora: AHORA,
    });

    expect(simulacro.preguntas.comoLista().map((p) => p.posicion)).toEqual([1, 2, 3]);
  });

  it('rechaza un docenteId que no sea un UUID valido con SOLICITUD_INVALIDA', () => {
    const error = capturarError(() =>
      Simulacro.definir({
        docenteId: 'no-es-un-uuid',
        nombre: 'Simulacro',
        criterio: criterioSinFiltro(),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas: [crearPreguntaPublicada()],
        ahora: AHORA,
      }),
    );

    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('rechaza un nombre vacio con SOLICITUD_INVALIDA', () => {
    const error = capturarError(() =>
      Simulacro.definir({
        docenteId: ID_DOCENTE,
        nombre: '   ',
        criterio: criterioSinFiltro(),
        duracion: DuracionMaxima.enMinutos(30),
        preguntas: [crearPreguntaPublicada()],
        ahora: AHORA,
      }),
    );

    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });
});
