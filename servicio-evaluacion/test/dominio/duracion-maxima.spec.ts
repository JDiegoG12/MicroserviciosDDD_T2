import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { capturarError } from '../apoyo/afirmaciones';

describe('DuracionMaxima (INV-28)', () => {
  it('acepta un entero positivo de minutos', () => {
    const duracion = DuracionMaxima.enMinutos(30);

    expect(duracion.enMinutosNumero()).toBe(30);
  });

  it('rechaza 0 con DURACION_INVALIDA', () => {
    const error = capturarError(() => DuracionMaxima.enMinutos(0));
    expect(error.codigo).toBe(CodigoError.DURACION_INVALIDA);
  });

  it('rechaza un numero negativo con DURACION_INVALIDA', () => {
    const error = capturarError(() => DuracionMaxima.enMinutos(-10));
    expect(error.codigo).toBe(CodigoError.DURACION_INVALIDA);
  });

  it('rechaza un numero decimal con DURACION_INVALIDA', () => {
    const error = capturarError(() => DuracionMaxima.enMinutos(15.5));
    expect(error.codigo).toBe(CodigoError.DURACION_INVALIDA);
  });

  it('sumarA calcula fechaLimite = fechaInicio + duracionMaximaMinutos', () => {
    const duracion = DuracionMaxima.enMinutos(30);
    const inicio = new Date('2026-10-01T15:40:00Z');

    const limite = duracion.sumarA(inicio);

    expect(limite.toISOString()).toBe('2026-10-01T16:10:00.000Z');
  });
});
