import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { IntentoDeSimulacro } from '../../src/dominio/intentos/intento-de-simulacro';
import { CalificadorSimulacroServicio } from '../../src/dominio/servicios/calificador-simulacro.servicio';
import { PreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { capturarError } from '../apoyo/afirmaciones';
import {
  ID_COMPETENCIA_DISENO,
  ID_COMPETENCIA_RAZONAMIENTO,
  ID_DOCENTE,
  ID_ESTUDIANTE,
} from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

const FECHA_INICIO = new Date('2026-10-01T15:40:00Z');
const FECHA_FIN = new Date('2026-10-01T16:00:00Z');

function construirIntento(preguntas: PreguntaEvaluable[]): IntentoDeSimulacro {
  const criterio = CriterioDeGeneracion.crear({
    competenciaIds: [],
    temaIds: [],
    subtemaIds: [],
    nivelesDificultad: [],
  });
  const simulacro = Simulacro.definir({
    docenteId: ID_DOCENTE,
    nombre: 'Simulacro para calificar',
    criterio,
    duracion: DuracionMaxima.enMinutos(30),
    preguntas,
    ahora: FECHA_INICIO,
  });
  return IntentoDeSimulacro.iniciar(simulacro, ID_ESTUDIANTE, FECHA_INICIO);
}

describe('CalificadorSimulacroServicio.calificar', () => {
  const calificador = new CalificadorSimulacroServicio();

  it('todas las respuestas correctas da puntaje 100', () => {
    const preguntas = [
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A }),
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.B }),
    ];
    const intento = construirIntento(preguntas);
    intento.registrarRespuesta(preguntas[0].preguntaId, LetraOpcion.A, FECHA_INICIO);
    intento.registrarRespuesta(preguntas[1].preguntaId, LetraOpcion.B, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const calificacion = calificador.calificar(intento, preguntas);

    expect(calificacion.correctas).toBe(2);
    expect(calificacion.puntaje).toBe(100);
  });

  it('ninguna respuesta correcta da puntaje 0', () => {
    const preguntas = [
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A }),
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.B }),
    ];
    const intento = construirIntento(preguntas);
    intento.registrarRespuesta(preguntas[0].preguntaId, LetraOpcion.D, FECHA_INICIO);
    intento.registrarRespuesta(preguntas[1].preguntaId, LetraOpcion.D, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const calificacion = calificador.calificar(intento, preguntas);

    expect(calificacion.correctas).toBe(0);
    expect(calificacion.puntaje).toBe(0);
  });

  it('una pregunta sin responder cuenta como incorrecta', () => {
    const preguntas = [
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A }),
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.B }),
    ];
    const intento = construirIntento(preguntas);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const calificacion = calificador.calificar(intento, preguntas);

    expect(calificacion.correctas).toBe(0);
    expect(calificacion.puntaje).toBe(0);
  });

  it('2 de 3 correctas da puntaje 66.67', () => {
    const preguntas = [
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A }),
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.B }),
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.C }),
    ];
    const intento = construirIntento(preguntas);
    intento.registrarRespuesta(preguntas[0].preguntaId, LetraOpcion.A, FECHA_INICIO);
    intento.registrarRespuesta(preguntas[1].preguntaId, LetraOpcion.B, FECHA_INICIO);
    intento.registrarRespuesta(preguntas[2].preguntaId, LetraOpcion.D, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const calificacion = calificador.calificar(intento, preguntas);

    expect(calificacion.puntaje).toBe(66.67);
  });

  it('arma el desglose con dos competencias que suma el total', () => {
    const preguntas = [
      crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO, letraCorrecta: LetraOpcion.A }),
      crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO, letraCorrecta: LetraOpcion.A }),
      crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_DISENO, letraCorrecta: LetraOpcion.B }),
    ];
    const intento = construirIntento(preguntas);
    intento.registrarRespuesta(preguntas[0].preguntaId, LetraOpcion.A, FECHA_INICIO);
    intento.registrarRespuesta(preguntas[1].preguntaId, LetraOpcion.D, FECHA_INICIO);
    intento.registrarRespuesta(preguntas[2].preguntaId, LetraOpcion.B, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const calificacion = calificador.calificar(intento, preguntas);

    expect(calificacion.desglosePorCompetencia).toHaveLength(2);
    const sumaDesglose = calificacion.desglosePorCompetencia.reduce((s, d) => s + d.totalPreguntas, 0);
    expect(sumaDesglose).toBe(calificacion.totalPreguntas);
    const desgloseRazonamiento = calificacion.desglosePorCompetencia.find(
      (d) => d.competenciaId === ID_COMPETENCIA_RAZONAMIENTO,
    );
    expect(desgloseRazonamiento).toEqual({
      competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
      totalPreguntas: 2,
      correctas: 1,
      puntaje: 50,
    });
  });

  it('una pregunta archivada despues de iniciar el intento se califica igual, porque conserva su contenido', () => {
    const preguntas = [crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A })];
    const intento = construirIntento(preguntas);
    intento.registrarRespuesta(preguntas[0].preguntaId, LetraOpcion.A, FECHA_INICIO);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const preguntaArchivada = preguntas[0].archivar('Motivo de prueba', FECHA_FIN, FECHA_FIN);
    const calificacion = calificador.calificar(intento, [preguntaArchivada]);

    expect(calificacion.correctas).toBe(1);
    expect(calificacion.puntaje).toBe(100);
  });

  it('lanza ERROR_INTERNO si falta la copia local de una pregunta del intento', () => {
    const preguntas = [crearPreguntaPublicada()];
    const intento = construirIntento(preguntas);
    intento.finalizar('ESTUDIANTE', FECHA_FIN);

    const error = capturarError(() => calificador.calificar(intento, []));
    expect(error.codigo).toBe(CodigoError.ERROR_INTERNO);
  });
});
