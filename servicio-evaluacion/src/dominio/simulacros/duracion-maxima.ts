import { DuracionInvalidaExcepcion } from '../excepciones/duracion-invalida.excepcion';

/**
 * Tiempo limite para completar un Intento de Simulacro, en minutos
 * (CONTRATOS.md 4 y 8.3, campo `duracionMaximaMinutos`).
 *
 * Value Object inmutable; se autovalida contra su propia invariante INV-28
 * (MODELO-DOMINIO.md: "debe ser un valor positivo").
 */
export class DuracionMaxima {
  private constructor(private readonly minutos: number) {}

  /**
   * Construye una DuracionMaxima a partir de un numero de minutos.
   *
   * @param minutos Cantidad de minutos; debe ser un entero estrictamente positivo.
   * @returns La duracion construida.
   * @throws DuracionInvalidaExcepcion Si `minutos` no es un entero mayor que cero (INV-28).
   */
  public static enMinutos(minutos: number): DuracionMaxima {
    if (!Number.isInteger(minutos) || minutos <= 0) {
      throw new DuracionInvalidaExcepcion(
        'La duracion maxima debe ser un numero entero de minutos mayor que cero.',
      );
    }
    return new DuracionMaxima(minutos);
  }

  /**
   * Cantidad de minutos de esta duracion.
   *
   * @returns El valor en minutos.
   */
  public enMinutosNumero(): number {
    return this.minutos;
  }

  /**
   * Calcula la fecha limite a partir de una fecha de inicio
   * (`fechaLimite = fechaInicio + duracionMaximaMinutos`, CONTRATOS.md 11.3).
   *
   * @param fecha Fecha de inicio.
   * @returns La fecha de inicio mas esta duracion.
   */
  public sumarA(fecha: Date): Date {
    return new Date(fecha.getTime() + this.minutos * 60_000);
  }
}
