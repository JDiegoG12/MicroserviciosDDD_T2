import { PreguntasInsuficientesExcepcion } from '../excepciones/preguntas-insuficientes.excepcion';
import { SolicitudInvalidaExcepcion } from '../excepciones/solicitud-invalida.excepcion';
import { PreguntaEvaluable } from '../preguntas-evaluables/pregunta-evaluable';
import { CriterioDeGeneracion } from '../simulacros/criterio-de-generacion';
import { FuenteAleatoria } from './fuente-aleatoria';

/**
 * Servicio de dominio que ensambla el conjunto de preguntas de un
 * Simulacro nuevo (MODELO-DOMINIO.md B.5 §10.5, CU-13).
 *
 * Es un servicio de dominio y no un metodo de `Simulacro` porque coordina
 * muchas instancias de `PreguntaEvaluable` de solo lectura para construir
 * un agregado nuevo: "la regla de balance por Competencia/Tema/Subtema/
 * Nivel de Dificultad es una restriccion global que ninguna Pregunta
 * individual conoce" (B.5 §10.5).
 */
export class EnsambladorSimulacroServicio {
  constructor(private readonly fuenteAleatoria: FuenteAleatoria) {}

  /**
   * Filtra las candidatas PUBLICADA que cumplen el criterio, descarta
   * duplicados por `preguntaId` y elige `cantidadPreguntas` al azar sin
   * repetir (INV-25, INV-27, RF-21, RF-22).
   *
   * @param candidatas Preguntas candidatas, normalmente el resultado de
   * `PreguntaEvaluableRepositorio.buscarPublicadasPorCriterios`.
   * @param criterio Criterio de generacion a cumplir.
   * @param cantidadPreguntas Cantidad de preguntas que debe tener el
   * simulacro; debe ser un entero mayor o igual a 1.
   * @returns Las preguntas elegidas, en un orden aleatorio.
   * @throws SolicitudInvalidaExcepcion Si `cantidadPreguntas` no es un
   * entero mayor o igual a 1.
   * @throws PreguntasInsuficientesExcepcion Si, despues de filtrar y
   * quitar duplicados, no hay suficientes candidatas (INV-26).
   */
  public ensamblar(
    candidatas: readonly PreguntaEvaluable[],
    criterio: CriterioDeGeneracion,
    cantidadPreguntas: number,
  ): PreguntaEvaluable[] {
    if (!Number.isInteger(cantidadPreguntas) || cantidadPreguntas < 1) {
      throw new SolicitudInvalidaExcepcion('La cantidad de preguntas debe ser un entero mayor o igual a 1.');
    }

    const elegibles: PreguntaEvaluable[] = [];
    const idsVistos = new Set<string>();
    for (const candidata of candidatas) {
      if (!candidata.estaPublicada() || !criterio.cumple(candidata)) {
        continue;
      }
      if (idsVistos.has(candidata.preguntaId)) {
        continue;
      }
      idsVistos.add(candidata.preguntaId);
      elegibles.push(candidata);
    }

    if (elegibles.length < cantidadPreguntas) {
      throw new PreguntasInsuficientesExcepcion(
        `Se necesitan ${cantidadPreguntas} preguntas publicadas que cumplan el criterio, pero solo hay ${elegibles.length} (INV-26).`,
      );
    }

    return this.elegirAlAzar(elegibles, cantidadPreguntas);
  }

  /**
   * Elige `cantidad` elementos de `elegibles` al azar, sin repetir, con un
   * Fisher-Yates parcial hacia adelante. Con una `FuenteAleatoria` que
   * siempre devuelve 0, el metodo conserva el orden original de
   * `elegibles`, lo que hace deterministas las pruebas que no necesitan
   * verificar la aleatoriedad en si misma.
   *
   * @param elegibles Candidatas ya filtradas, sin duplicados.
   * @param cantidad Cantidad de elementos a elegir.
   * @returns Los primeros `cantidad` elementos tras la mezcla parcial.
   */
  private elegirAlAzar(elegibles: readonly PreguntaEvaluable[], cantidad: number): PreguntaEvaluable[] {
    const mezcla = [...elegibles];
    const limite = Math.min(cantidad, mezcla.length);
    for (let indice = 0; indice < limite; indice += 1) {
      const indiceAleatorio = indice + Math.floor(this.fuenteAleatoria.siguiente() * (mezcla.length - indice));
      [mezcla[indice], mezcla[indiceAleatorio]] = [mezcla[indiceAleatorio], mezcla[indice]];
    }
    return mezcla.slice(0, limite);
  }
}
