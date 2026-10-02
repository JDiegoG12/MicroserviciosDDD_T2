import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { crearContenidoDePregunta } from '../../src/dominio/preguntas-evaluables/contenido-de-pregunta';
import { capturarError } from '../apoyo/afirmaciones';
import { ID_COMPETENCIA_RAZONAMIENTO, ID_SUBTEMA_MEDIDAS_CENTRALES, ID_TEMA_ESTADISTICA } from '../apoyo/datos-de-prueba';

const AHORA = new Date('2026-10-01T15:30:00Z');

function datosValidos() {
  return {
    contexto: 'Contexto de prueba.',
    preguntaDirecta: 'Pregunta directa de prueba?',
    opciones: [
      { letra: LetraOpcion.A, texto: 'Opcion A' },
      { letra: LetraOpcion.B, texto: 'Opcion B' },
      { letra: LetraOpcion.C, texto: 'Opcion C' },
      { letra: LetraOpcion.D, texto: 'Opcion D' },
    ],
    letraCorrecta: LetraOpcion.B as string,
    clasificacion: {
      competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
      temaId: ID_TEMA_ESTADISTICA,
      subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
    },
    nivelDificultad: NivelDificultad.MEDIO as string,
    fechaPublicacion: AHORA,
  };
}

describe('crearContenidoDePregunta', () => {
  it('rechaza un contexto vacio', () => {
    const error = capturarError(() => crearContenidoDePregunta({ ...datosValidos(), contexto: '   ' }));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('rechaza una pregunta directa vacia', () => {
    const error = capturarError(() => crearContenidoDePregunta({ ...datosValidos(), preguntaDirecta: '' }));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('rechaza una cantidad de opciones distinta de cuatro', () => {
    const datos = datosValidos();
    const error = capturarError(() =>
      crearContenidoDePregunta({ ...datos, opciones: datos.opciones.slice(0, 3) }),
    );
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('rechaza una letraCorrecta fuera de A-D', () => {
    const error = capturarError(() => crearContenidoDePregunta({ ...datosValidos(), letraCorrecta: 'Z' }));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('rechaza un nivel de dificultad invalido', () => {
    const error = capturarError(() =>
      crearContenidoDePregunta({ ...datosValidos(), nivelDificultad: 'EXTREMO' }),
    );
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('construye el contenido cuando todos los datos son validos', () => {
    const contenido = crearContenidoDePregunta(datosValidos());
    expect(contenido.letraCorrecta).toBe(LetraOpcion.B);
    expect(contenido.nivelDificultad).toBe(NivelDificultad.MEDIO);
  });
});
