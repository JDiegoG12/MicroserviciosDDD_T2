import { PreguntaEvaluable } from '../preguntas-evaluables/pregunta-evaluable';
import { PreguntaDuplicadaEnSimulacroExcepcion } from '../excepciones/pregunta-duplicada-en-simulacro.excepcion';
import { PreguntasInsuficientesExcepcion } from '../excepciones/preguntas-insuficientes.excepcion';
import { PreguntaSeleccionada } from './pregunta-seleccionada';

/**
 * Coleccion de PreguntaSeleccionada que integra un Simulacro
 * (MODELO-DOMINIO.md B.3 §7.3: "Value Objects: ... PreguntasSeleccionadas").
 *
 * Es el Value Object de coleccion que protege, dentro del agregado
 * Simulacro, dos invariantes que solo tienen sentido sobre el conjunto
 * completo: INV-26 (al menos una pregunta) e INV-27 (sin repetidas).
 * INV-25 (solo PUBLICADA) se verifica antes de construir esta coleccion,
 * porque depende del estado de cada PreguntaEvaluable, no de la coleccion.
 */
export class PreguntasSeleccionadas {
  private constructor(private readonly preguntas: readonly PreguntaSeleccionada[]) {}

  /**
   * Construye la coleccion a partir de las PreguntaEvaluable ya elegidas
   * por `EnsambladorSimulacroServicio`, asignando la posicion de cada una
   * segun el orden recibido (desde 1).
   *
   * @param preguntasElegidas Preguntas ya seleccionadas, en el orden final.
   * @returns La coleccion construida.
   * @throws PreguntasInsuficientesExcepcion Si la lista esta vacia (INV-26).
   * @throws PreguntaDuplicadaEnSimulacroExcepcion Si hay un `preguntaId`
   * repetido (INV-27, CONTRATOS.md 8.3).
   */
  public static desdePreguntas(preguntasElegidas: readonly PreguntaEvaluable[]): PreguntasSeleccionadas {
    if (preguntasElegidas.length === 0) {
      throw new PreguntasInsuficientesExcepcion(
        'Un simulacro no puede quedar sin preguntas seleccionadas (INV-26).',
      );
    }
    const idsVistos = new Set<string>();
    for (const pregunta of preguntasElegidas) {
      if (idsVistos.has(pregunta.preguntaId)) {
        throw new PreguntaDuplicadaEnSimulacroExcepcion(
          'Una misma pregunta no puede aparecer dos veces dentro del mismo simulacro (INV-27).',
        );
      }
      idsVistos.add(pregunta.preguntaId);
    }
    // CONTRATOS.md 8.3: la posicion empieza en 1.
    const seleccionadas = preguntasElegidas.map(
      (pregunta, indice): PreguntaSeleccionada => ({
        preguntaId: pregunta.preguntaId,
        posicion: indice + 1,
      }),
    );
    return new PreguntasSeleccionadas(seleccionadas);
  }

  /**
   * Reconstruye la coleccion desde su forma persistida, sin repetir las
   * validaciones (ya se cumplieron al guardar).
   *
   * @param seleccionadas Preguntas seleccionadas ya persistidas.
   * @returns La coleccion reconstruida.
   */
  public static reconstruir(seleccionadas: readonly PreguntaSeleccionada[]): PreguntasSeleccionadas {
    return new PreguntasSeleccionadas([...seleccionadas]);
  }

  /**
   * Indica si una pregunta pertenece a este Simulacro (INV-29, tercera
   * consecuencia: una respuesta solo puede referirse a una pregunta de ese
   * simulacro).
   *
   * @param preguntaId Id de la pregunta a comprobar.
   * @returns `true` si la pregunta esta entre las seleccionadas.
   */
  public contiene(preguntaId: string): boolean {
    return this.preguntas.some((pregunta) => pregunta.preguntaId === preguntaId);
  }

  /**
   * Ids de todas las preguntas seleccionadas, en su orden de posicion.
   *
   * @returns La lista de preguntaId.
   */
  public ids(): string[] {
    return this.preguntas.map((pregunta) => pregunta.preguntaId);
  }

  /**
   * Representacion de la coleccion como lista simple, para persistencia o
   * DTOs de respuesta.
   *
   * @returns Una copia de la lista de PreguntaSeleccionada.
   */
  public comoLista(): PreguntaSeleccionada[] {
    return [...this.preguntas];
  }
}
