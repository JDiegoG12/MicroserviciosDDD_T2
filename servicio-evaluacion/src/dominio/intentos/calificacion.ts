import { InconsistenciaDeDatosExcepcion } from '../excepciones/inconsistencia-de-datos.excepcion';

/**
 * Calificacion de un IntentoDeSimulacro desagregada por una competencia
 * (CONTRATOS.md 7.6: "mismos campos de calificacion mas competenciaId").
 */
export interface DesglosePorCompetencia {
  readonly competenciaId: string;
  readonly totalPreguntas: number;
  readonly correctas: number;
  readonly puntaje: number;
}

interface PropiedadesCalificacion {
  readonly totalPreguntas: number;
  readonly correctas: number;
  readonly puntaje: number;
  readonly desglosePorCompetencia: readonly DesglosePorCompetencia[];
}

/**
 * Redondea un puntaje a 2 decimales, como exige CONTRATOS.md seccion 4
 * ("Numero decimal de 0 a 100 con 2 decimales").
 *
 * @param correctas Cantidad de respuestas correctas.
 * @param total Cantidad total de preguntas.
 * @returns `correctas / total * 100`, redondeado a 2 decimales.
 */
export function calcularPuntaje(correctas: number, total: number): number {
  return Math.round((correctas / total) * 100 * 100) / 100;
}

/**
 * Resultado numerico y definitivo obtenido por un Estudiante en un Intento
 * de Simulacro (MODELO-DOMINIO.md, Value Object "Calificacion del
 * Simulacro", B.3 §6). Una vez construida, es inmutable (INV-32).
 *
 * Sin ponderaciones por competencia (MODELO-DOMINIO.md A.1, erreta E-6):
 * `puntaje = correctas / totalPreguntas x 100`, con desglose simple por
 * `competenciaId`.
 */
export class Calificacion {
  private constructor(private readonly propiedades: PropiedadesCalificacion) {}

  public get totalPreguntas(): number {
    return this.propiedades.totalPreguntas;
  }

  public get correctas(): number {
    return this.propiedades.correctas;
  }

  public get puntaje(): number {
    return this.propiedades.puntaje;
  }

  public get desglosePorCompetencia(): readonly DesglosePorCompetencia[] {
    return this.propiedades.desglosePorCompetencia;
  }

  /**
   * Construye una Calificacion ya calculada por
   * `CalificadorSimulacroServicio`.
   *
   * Comprueba las reglas que CONTRATOS.md 7.6 garantiza en el codigo del
   * productor: `correctas <= totalPreguntas`, al menos un elemento en el
   * desglose (INV-26: todo simulacro tiene al menos una pregunta) y que la
   * suma del desglose coincida con el total. Si alguna falla, es una
   * inconsistencia del calificador, no un dato de entrada del usuario.
   *
   * @param datos Totales ya calculados y desglose por competencia.
   * @returns La Calificacion construida.
   * @throws InconsistenciaDeDatosExcepcion Si los datos no son coherentes
   * entre si.
   */
  public static crear(datos: {
    totalPreguntas: number;
    correctas: number;
    desglosePorCompetencia: readonly DesglosePorCompetencia[];
  }): Calificacion {
    if (datos.totalPreguntas < 1) {
      throw new InconsistenciaDeDatosExcepcion('totalPreguntas debe ser al menos 1 (INV-26).');
    }
    if (datos.correctas < 0 || datos.correctas > datos.totalPreguntas) {
      throw new InconsistenciaDeDatosExcepcion('correctas debe estar entre 0 y totalPreguntas.');
    }
    if (datos.desglosePorCompetencia.length === 0) {
      throw new InconsistenciaDeDatosExcepcion('El desglose por competencia debe tener al menos un elemento.');
    }
    const totalDesglosado = datos.desglosePorCompetencia.reduce(
      (suma, item) => suma + item.totalPreguntas,
      0,
    );
    if (totalDesglosado !== datos.totalPreguntas) {
      throw new InconsistenciaDeDatosExcepcion(
        'La suma del desglose por competencia debe coincidir con el total de preguntas.',
      );
    }

    return new Calificacion({
      totalPreguntas: datos.totalPreguntas,
      correctas: datos.correctas,
      puntaje: calcularPuntaje(datos.correctas, datos.totalPreguntas),
      desglosePorCompetencia: datos.desglosePorCompetencia,
    });
  }

  /**
   * Reconstruye una Calificacion desde su forma persistida, sin repetir
   * las validaciones.
   *
   * @param props Propiedades leidas de la persistencia.
   * @returns La Calificacion reconstruida.
   */
  public static reconstruir(props: PropiedadesCalificacion): Calificacion {
    return new Calificacion({ ...props });
  }
}
