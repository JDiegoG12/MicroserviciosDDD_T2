import { NivelDificultad } from '../compartido/nivel-dificultad';
import { PreguntaEvaluable } from '../preguntas-evaluables/pregunta-evaluable';

interface PropiedadesCriterioDeGeneracion {
  readonly competenciaIds: readonly string[];
  readonly temaIds: readonly string[];
  readonly subtemaIds: readonly string[];
  readonly nivelesDificultad: readonly NivelDificultad[];
}

/**
 * Criterio con el que un Docente filtra las Preguntas Publicadas para
 * ensamblar un Simulacro (CONTRATOS.md 8.3 y 11.3, B.5 §10.5 del Taller 1).
 *
 * Es un Value Object inmutable: dos Simulacros con los mismos filtros
 * comparten el mismo criterio, no tiene identidad propia.
 */
export class CriterioDeGeneracion {
  private constructor(private readonly propiedades: PropiedadesCriterioDeGeneracion) {}

  public get competenciaIds(): readonly string[] {
    return this.propiedades.competenciaIds;
  }

  public get temaIds(): readonly string[] {
    return this.propiedades.temaIds;
  }

  public get subtemaIds(): readonly string[] {
    return this.propiedades.subtemaIds;
  }

  public get nivelesDificultad(): readonly NivelDificultad[] {
    return this.propiedades.nivelesDificultad;
  }

  /**
   * Construye un criterio de generacion. No valida que los ids existan en
   * el Catalogo: este servicio no hace llamadas sincronas a Catalogo
   * (CONTRATOS.md 11.3, "No hacer"), asi que esa verificacion no aplica
   * aqui.
   *
   * @param datos Listas de filtro, cualquiera puede venir vacia.
   * @returns El criterio construido.
   */
  public static crear(datos: {
    competenciaIds: readonly string[];
    temaIds: readonly string[];
    subtemaIds: readonly string[];
    nivelesDificultad: readonly NivelDificultad[];
  }): CriterioDeGeneracion {
    return new CriterioDeGeneracion({
      competenciaIds: [...datos.competenciaIds],
      temaIds: [...datos.temaIds],
      subtemaIds: [...datos.subtemaIds],
      nivelesDificultad: [...datos.nivelesDificultad],
    });
  }

  /**
   * Comprueba si una PreguntaEvaluable cumple este criterio, con la
   * semantica exacta de CONTRATOS.md 8.3: dentro de una lista es **O**,
   * entre listas es **Y**, y una lista vacia **no filtra**.
   *
   * Una copia sin contenido (una marca de archivo que aun no recibio su
   * publicacion) nunca cumple, porque no tiene clasificacion ni nivel que
   * comparar.
   *
   * @param pregunta La PreguntaEvaluable candidata.
   * @returns `true` si la pregunta satisface las cuatro listas de filtro.
   */
  public cumple(pregunta: PreguntaEvaluable): boolean {
    if (pregunta.contenido === null) {
      return false;
    }
    const { clasificacion, nivelDificultad } = pregunta.contenido;

    const cumpleCompetencia =
      this.propiedades.competenciaIds.length === 0 ||
      this.propiedades.competenciaIds.includes(clasificacion.competenciaId);
    const cumpleTema =
      this.propiedades.temaIds.length === 0 || this.propiedades.temaIds.includes(clasificacion.temaId);
    const cumpleSubtema =
      this.propiedades.subtemaIds.length === 0 ||
      this.propiedades.subtemaIds.includes(clasificacion.subtemaId);
    const cumpleNivel =
      this.propiedades.nivelesDificultad.length === 0 ||
      this.propiedades.nivelesDificultad.includes(nivelDificultad);

    return cumpleCompetencia && cumpleTema && cumpleSubtema && cumpleNivel;
  }
}
