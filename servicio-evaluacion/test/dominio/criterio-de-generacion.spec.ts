import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { PreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import {
  ID_COMPETENCIA_DISENO,
  ID_COMPETENCIA_RAZONAMIENTO,
  ID_SUBTEMA_ECUACIONES_LINEALES,
  ID_SUBTEMA_MEDIDAS_CENTRALES,
  ID_SUBTEMA_PROBABILIDAD,
  ID_TEMA_ALGEBRA,
  ID_TEMA_ESTADISTICA,
} from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

function criterioVacio() {
  return CriterioDeGeneracion.crear({
    competenciaIds: [],
    temaIds: [],
    subtemaIds: [],
    nivelesDificultad: [],
  });
}

describe('CriterioDeGeneracion.cumple', () => {
  it('una lista vacia no filtra: cualquier pregunta cumple', () => {
    const pregunta = crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_DISENO });

    expect(criterioVacio().cumple(pregunta)).toBe(true);
  });

  it('dentro de una lista la semantica es O: coincide con cualquiera de los ids', () => {
    const criterio = CriterioDeGeneracion.crear({
      competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO, ID_COMPETENCIA_DISENO],
      temaIds: [],
      subtemaIds: [],
      nivelesDificultad: [],
    });
    const preguntaRazonamiento = crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO });
    const preguntaDiseno = crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_DISENO });

    expect(criterio.cumple(preguntaRazonamiento)).toBe(true);
    expect(criterio.cumple(preguntaDiseno)).toBe(true);
  });

  it('entre listas la semantica es Y: debe cumplir competencia y tema a la vez', () => {
    const criterio = CriterioDeGeneracion.crear({
      competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO],
      temaIds: [ID_TEMA_ALGEBRA],
      subtemaIds: [],
      nivelesDificultad: [],
    });
    const cumpleAmbas = crearPreguntaPublicada({
      competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
      temaId: ID_TEMA_ALGEBRA,
      subtemaId: ID_SUBTEMA_ECUACIONES_LINEALES,
    });
    const soloCumpleCompetencia = crearPreguntaPublicada({
      competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
      temaId: ID_TEMA_ESTADISTICA,
      subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
    });

    expect(criterio.cumple(cumpleAmbas)).toBe(true);
    expect(criterio.cumple(soloCumpleCompetencia)).toBe(false);
  });

  it('semantica O/Y tambien aplica a subtemaIds', () => {
    const criterio = CriterioDeGeneracion.crear({
      competenciaIds: [],
      temaIds: [],
      subtemaIds: [ID_SUBTEMA_MEDIDAS_CENTRALES, ID_SUBTEMA_PROBABILIDAD],
      nivelesDificultad: [],
    });
    const cumple = crearPreguntaPublicada({ subtemaId: ID_SUBTEMA_PROBABILIDAD });
    const noCumple = crearPreguntaPublicada({ subtemaId: ID_SUBTEMA_ECUACIONES_LINEALES });

    expect(criterio.cumple(cumple)).toBe(true);
    expect(criterio.cumple(noCumple)).toBe(false);
  });

  it('filtra tambien por nivelesDificultad con semantica O', () => {
    const criterio = CriterioDeGeneracion.crear({
      competenciaIds: [],
      temaIds: [],
      subtemaIds: [],
      nivelesDificultad: [NivelDificultad.BAJO, NivelDificultad.MEDIO],
    });
    const bajo = crearPreguntaPublicada({ nivelDificultad: NivelDificultad.BAJO });
    const alto = crearPreguntaPublicada({ nivelDificultad: NivelDificultad.ALTO });

    expect(criterio.cumple(bajo)).toBe(true);
    expect(criterio.cumple(alto)).toBe(false);
  });

  it('una copia sin contenido (marca de archivo) nunca cumple', () => {
    const marca = PreguntaEvaluable.marcaDeArchivo(
      '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f',
      'Motivo de prueba',
      new Date(),
      new Date(),
    );

    expect(criterioVacio().cumple(marca)).toBe(false);
  });
});
