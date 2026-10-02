import { Calificacion, DesglosePorCompetencia } from '../intentos/calificacion';
import { IntentoDeSimulacro } from '../intentos/intento-de-simulacro';
import { InconsistenciaDeDatosExcepcion } from '../excepciones/inconsistencia-de-datos.excepcion';
import { PreguntaEvaluable } from '../preguntas-evaluables/pregunta-evaluable';

/**
 * Servicio de dominio que compara las respuestas de un
 * IntentoDeSimulacro contra las copias locales de las preguntas y calcula
 * su Calificacion (MODELO-DOMINIO.md B.5 §10.1, CU-15).
 *
 * Es un servicio de dominio porque coordina dos agregados de solo lectura
 * (IntentoDeSimulacro e, indirectamente, PreguntaEvaluable) para producir
 * un Value Object nuevo: esa logica no pertenece a ninguno de los dos por
 * si solo.
 *
 * Sin ponderaciones por competencia (MODELO-DOMINIO.md A.1, erreta E-6).
 */
export class CalificadorSimulacroServicio {
  /**
   * Califica un intento ya finalizado.
   *
   * Una pregunta sin respuesta cuenta como incorrecta. El desglose se
   * arma por `competenciaId`, en el orden en que cada competencia aparece
   * por primera vez entre las preguntas del simulacro.
   *
   * Si una pregunta se archivo despues de que el estudiante inicio el
   * intento, se califica igual porque `PreguntaEvaluable.archivar`
   * conserva su contenido (CONTRATOS.md 11.3: "Archivar una pregunta no
   * altera simulacros ni intentos existentes").
   *
   * @param intento El IntentoDeSimulacro ya finalizado, con sus respuestas.
   * @param preguntas Las copias locales de todas las preguntas del
   * simulacro de este intento.
   * @returns La Calificacion calculada, lista para
   * `intento.cerrarConCalificacion(...)`.
   * @throws InconsistenciaDeDatosExcepcion Si falta la copia local de
   * alguna pregunta del simulacro.
   */
  public calificar(intento: IntentoDeSimulacro, preguntas: readonly PreguntaEvaluable[]): Calificacion {
    const preguntasPorId = new Map(preguntas.map((pregunta) => [pregunta.preguntaId, pregunta]));

    let correctas = 0;
    const ordenCompetencias: string[] = [];
    const acumuladoPorCompetencia = new Map<string, { total: number; correctas: number }>();

    for (const preguntaId of intento.preguntaIds()) {
      const pregunta = preguntasPorId.get(preguntaId);
      if (!pregunta || pregunta.contenido === null) {
        throw new InconsistenciaDeDatosExcepcion(
          `No se encontro la copia local de la pregunta ${preguntaId} para calificar el intento.`,
        );
      }
      const respuesta = intento.respuestaPara(preguntaId);
      const esCorrecta = respuesta !== undefined && pregunta.esCorrecta(respuesta.letraSeleccionada);
      if (esCorrecta) {
        correctas += 1;
      }

      const competenciaId = pregunta.contenido.clasificacion.competenciaId;
      if (!acumuladoPorCompetencia.has(competenciaId)) {
        acumuladoPorCompetencia.set(competenciaId, { total: 0, correctas: 0 });
        ordenCompetencias.push(competenciaId);
      }
      const acumulado = acumuladoPorCompetencia.get(competenciaId)!;
      acumulado.total += 1;
      if (esCorrecta) {
        acumulado.correctas += 1;
      }
    }

    const desglosePorCompetencia: DesglosePorCompetencia[] = ordenCompetencias.map((competenciaId) => {
      const acumulado = acumuladoPorCompetencia.get(competenciaId)!;
      return {
        competenciaId,
        totalPreguntas: acumulado.total,
        correctas: acumulado.correctas,
        puntaje: Math.round((acumulado.correctas / acumulado.total) * 100 * 100) / 100,
      };
    });

    return Calificacion.crear({
      totalPreguntas: intento.preguntaIds().length,
      correctas,
      desglosePorCompetencia,
    });
  }
}
