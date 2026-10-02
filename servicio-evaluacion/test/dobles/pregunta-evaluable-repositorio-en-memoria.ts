import { ResultadoPaginado } from '../../src/dominio/compartido/resultado-paginado';
import {
  FiltrosPreguntaEvaluable,
  PreguntaEvaluableRepositorio,
} from '../../src/dominio/preguntas-evaluables/pregunta-evaluable.repositorio';
import { PreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';

/**
 * Doble en memoria de `PreguntaEvaluableRepositorio`, para las pruebas de
 * aplicacion. No es codigo de produccion.
 */
export class PreguntaEvaluableRepositorioEnMemoria implements PreguntaEvaluableRepositorio {
  private readonly preguntasPorId = new Map<string, PreguntaEvaluable>();
  public cantidadDeGuardados = 0;

  public async guardar(pregunta: PreguntaEvaluable): Promise<void> {
    this.preguntasPorId.set(pregunta.preguntaId, pregunta);
    this.cantidadDeGuardados += 1;
  }

  public async obtenerPorId(preguntaId: string): Promise<PreguntaEvaluable | null> {
    return this.preguntasPorId.get(preguntaId) ?? null;
  }

  public async obtenerPorIds(preguntaIds: readonly string[]): Promise<PreguntaEvaluable[]> {
    return preguntaIds
      .map((id) => this.preguntasPorId.get(id))
      .filter((pregunta): pregunta is PreguntaEvaluable => pregunta !== undefined);
  }

  public async buscarPublicadasPorCriterios(criterio: CriterioDeGeneracion): Promise<PreguntaEvaluable[]> {
    return [...this.preguntasPorId.values()].filter(
      (pregunta) => pregunta.estaPublicada() && criterio.cumple(pregunta),
    );
  }

  public async buscarPaginado(
    filtros: FiltrosPreguntaEvaluable,
    pagina: number,
    tamano: number,
  ): Promise<ResultadoPaginado<PreguntaEvaluable>> {
    const todas = [...this.preguntasPorId.values()].filter((pregunta) => {
      if (filtros.estado && pregunta.estado !== filtros.estado) {
        return false;
      }
      if (filtros.nivelDificultad && pregunta.contenido?.nivelDificultad !== filtros.nivelDificultad) {
        return false;
      }
      if (filtros.competenciaId && pregunta.contenido?.clasificacion.competenciaId !== filtros.competenciaId) {
        return false;
      }
      return true;
    });
    const inicio = pagina * tamano;
    return {
      elementos: todas.slice(inicio, inicio + tamano),
      totalElementos: todas.length,
    };
  }

  /** Helper de pruebas: agrega una pregunta directamente al mapa. */
  public agregar(pregunta: PreguntaEvaluable): void {
    this.preguntasPorId.set(pregunta.preguntaId, pregunta);
  }
}
