import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { IntentoId } from '../../src/dominio/intentos/intento-id';
import { SimulacroId } from '../../src/dominio/simulacros/simulacro-id';
import { capturarError } from '../apoyo/afirmaciones';

describe('SimulacroId', () => {
  it('desde construye un identificador a partir de un UUID valido', () => {
    const id = SimulacroId.desde('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    expect(id.aTexto()).toBe('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
  });

  it('desde rechaza un texto que no es UUID con SOLICITUD_INVALIDA', () => {
    const error = capturarError(() => SimulacroId.desde('no-es-uuid'));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('igualA compara dos identificadores por su valor', () => {
    const primero = SimulacroId.desde('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    const igual = SimulacroId.desde('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    const otro = SimulacroId.generar();

    expect(primero.igualA(igual)).toBe(true);
    expect(primero.igualA(otro)).toBe(false);
  });
});

describe('IntentoId', () => {
  it('desde construye un identificador a partir de un UUID valido', () => {
    const id = IntentoId.desde('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    expect(id.aTexto()).toBe('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
  });

  it('desde rechaza un texto que no es UUID con SOLICITUD_INVALIDA', () => {
    const error = capturarError(() => IntentoId.desde('no-es-uuid'));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('igualA compara dos identificadores por su valor', () => {
    const primero = IntentoId.desde('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    const igual = IntentoId.desde('5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f');
    const otro = IntentoId.generar();

    expect(primero.igualA(igual)).toBe(true);
    expect(primero.igualA(otro)).toBe(false);
  });
});
