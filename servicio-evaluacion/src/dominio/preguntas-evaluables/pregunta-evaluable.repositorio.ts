import { ResultadoPaginado } from '../compartido/resultado-paginado';
import { NivelDificultad } from '../compartido/nivel-dificultad';
import { CriterioDeGeneracion } from '../simulacros/criterio-de-generacion';
import { EstadoPreguntaEvaluable, PreguntaEvaluable } from './pregunta-evaluable';

/**
 * Filtros de `GET /preguntas-evaluables` (CONTRATOS.md 8.3).
 */
export interface FiltrosPreguntaEvaluable {
  readonly competenciaId?: string;
  readonly nivelDificultad?: NivelDificultad;
  readonly estado?: EstadoPreguntaEvaluable;
}

/**
 * Puerto de salida del dominio para persistir y consultar PreguntaEvaluable.
 *
 * Reemplaza en este servicio al `PreguntaRepository` del Taller 1
 * (MODELO-DOMINIO.md A.3): ningun servicio lee la base de datos de otro, asi
 * que aqui se consulta la copia local, no la Pregunta original de Editorial.
 */
export interface PreguntaEvaluableRepositorio {
  /**
   * Guarda una PreguntaEvaluable. Debe comportarse como *upsert* por
   * `preguntaId` (CONTRATOS.md 7.7.2), porque el mismo `preguntaId` puede
   * guardarse varias veces a lo largo de su vida (publicacion, archivado).
   *
   * @param pregunta La copia a guardar.
   */
  guardar(pregunta: PreguntaEvaluable): Promise<void>;

  /**
   * Busca una PreguntaEvaluable por su identificador.
   *
   * @param preguntaId Id de la Pregunta en Editorial.
   * @returns La copia encontrada, o `null` si este servicio aun no la conoce.
   */
  obtenerPorId(preguntaId: string): Promise<PreguntaEvaluable | null>;

  /**
   * Busca varias PreguntaEvaluable por su identificador, por ejemplo para
   * calificar un intento o para armar la vista de un Simulacro ya definido.
   *
   * @param preguntaIds Ids a buscar.
   * @returns Las copias encontradas (puede ser menos que `preguntaIds.length`
   * si alguna no existe).
   */
  obtenerPorIds(preguntaIds: readonly string[]): Promise<PreguntaEvaluable[]>;

  /**
   * Busca las copias PUBLICADA que cumplen un criterio de generacion, para
   * que `EnsambladorSimulacroServicio` pueda construir un Simulacro
   * (B.5 §10.5 del Taller 1, CONTRATOS.md 8.3).
   *
   * @param criterio Criterio de filtro.
   * @returns Las copias candidatas (puede incluir mas de las necesarias; el
   * ensamblador decide cuantas y cuales usar).
   */
  buscarPublicadasPorCriterios(criterio: CriterioDeGeneracion): Promise<PreguntaEvaluable[]>;

  /**
   * Busca copias locales paginadas, para `GET /preguntas-evaluables`
   * (CONTRATOS.md 8.3), que existe para demostrar que el evento llego.
   *
   * @param filtros Filtros opcionales de competencia, nivel y estado.
   * @param pagina Pagina solicitada, desde 0.
   * @param tamano Tamano de pagina.
   * @returns La pagina de resultados y el total de elementos que cumplen el filtro.
   */
  buscarPaginado(
    filtros: FiltrosPreguntaEvaluable,
    pagina: number,
    tamano: number,
  ): Promise<ResultadoPaginado<PreguntaEvaluable>>;
}
