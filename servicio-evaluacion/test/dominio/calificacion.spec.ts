import { Calificacion, calcularPuntaje } from '../../src/dominio/intentos/calificacion';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { capturarError } from '../apoyo/afirmaciones';
import { ID_COMPETENCIA_RAZONAMIENTO } from '../apoyo/datos-de-prueba';

describe('calcularPuntaje', () => {
  it('redondea a 2 decimales (2 de 3 da 66.67)', () => {
    expect(calcularPuntaje(2, 3)).toBe(66.67);
  });

  it('todas correctas da 100', () => {
    expect(calcularPuntaje(3, 3)).toBe(100);
  });

  it('ninguna correcta da 0', () => {
    expect(calcularPuntaje(0, 3)).toBe(0);
  });
});

describe('Calificacion.crear', () => {
  it('construye una calificacion con el desglose por competencia', () => {
    const calificacion = Calificacion.crear({
      totalPreguntas: 3,
      correctas: 2,
      desglosePorCompetencia: [
        { competenciaId: ID_COMPETENCIA_RAZONAMIENTO, totalPreguntas: 3, correctas: 2, puntaje: 66.67 },
      ],
    });

    expect(calificacion.puntaje).toBe(66.67);
    expect(calificacion.correctas).toBe(2);
  });

  it('rechaza un desglose vacio', () => {
    const error = capturarError(() =>
      Calificacion.crear({ totalPreguntas: 1, correctas: 1, desglosePorCompetencia: [] }),
    );
    expect(error.codigo).toBe(CodigoError.ERROR_INTERNO);
  });

  it('rechaza un desglose cuya suma no coincide con el total', () => {
    const error = capturarError(() =>
      Calificacion.crear({
        totalPreguntas: 3,
        correctas: 2,
        desglosePorCompetencia: [
          { competenciaId: ID_COMPETENCIA_RAZONAMIENTO, totalPreguntas: 2, correctas: 2, puntaje: 100 },
        ],
      }),
    );
    expect(error.codigo).toBe(CodigoError.ERROR_INTERNO);
  });

  it('desglose con dos competencias que suma el total', () => {
    const calificacion = Calificacion.crear({
      totalPreguntas: 10,
      correctas: 7,
      desglosePorCompetencia: [
        { competenciaId: 'a', totalPreguntas: 6, correctas: 5, puntaje: 83.33 },
        { competenciaId: 'b', totalPreguntas: 4, correctas: 2, puntaje: 50 },
      ],
    });

    const sumaTotal = calificacion.desglosePorCompetencia.reduce((s, d) => s + d.totalPreguntas, 0);
    expect(sumaTotal).toBe(calificacion.totalPreguntas);
  });
});
