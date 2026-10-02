import { CodigoError } from '../../../dominio/excepciones/codigo-error';

/**
 * Clase base de toda excepcion lanzada por la capa de aplicacion.
 *
 * Igual que `ExcepcionDeDominio`, lleva el `codigo` exacto de
 * CONTRATOS.md 5.3/8.x para que la etapa 2 la traduzca a HTTP sin
 * ambiguedad. Se diferencia de las excepciones de dominio en que
 * representa un error de orquestacion (por ejemplo, "no existe el
 * Simulacro con ese id"), no la violacion de una invariante del dominio.
 */
export abstract class ExcepcionDeAplicacion extends Error {
  protected constructor(
    public readonly codigo: CodigoError,
    mensaje: string,
  ) {
    super(mensaje);
    this.name = new.target.name;
  }
}
