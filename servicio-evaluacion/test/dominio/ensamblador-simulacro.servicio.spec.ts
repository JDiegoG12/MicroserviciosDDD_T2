import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { EnsambladorSimulacroServicio } from '../../src/dominio/servicios/ensamblador-simulacro.servicio';
import { FuenteAleatoriaFija } from '../dobles/fuente-aleatoria-fija';
import { capturarError } from '../apoyo/afirmaciones';
import { ID_COMPETENCIA_DISENO, ID_COMPETENCIA_RAZONAMIENTO } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

function criterioPorCompetencia(competenciaId: string) {
  return CriterioDeGeneracion.crear({
    competenciaIds: [competenciaId],
    temaIds: [],
    subtemaIds: [],
    nivelesDificultad: [],
  });
}

describe('EnsambladorSimulacroServicio.ensamblar', () => {
  it('INV-25: nunca elige una pregunta archivada', () => {
    const publicada = crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO });
    const archivada = crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }).archivar(
      'Motivo de prueba',
      new Date(),
      new Date(),
    );
    const ensamblador = new EnsambladorSimulacroServicio(new FuenteAleatoriaFija());

    const elegidas = ensamblador.ensamblar(
      [publicada, archivada],
      criterioPorCompetencia(ID_COMPETENCIA_RAZONAMIENTO),
      1,
    );

    expect(elegidas).toHaveLength(1);
    expect(elegidas[0].preguntaId).toBe(publicada.preguntaId);
  });

  it('INV-26: lanza PREGUNTAS_INSUFICIENTES si no hay candidatas suficientes', () => {
    const candidatas = [crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_DISENO })];
    const ensamblador = new EnsambladorSimulacroServicio(new FuenteAleatoriaFija());

    const error = capturarError(() =>
      ensamblador.ensamblar(candidatas, criterioPorCompetencia(ID_COMPETENCIA_RAZONAMIENTO), 1),
    );

    expect(error.codigo).toBe(CodigoError.PREGUNTAS_INSUFICIENTES);
  });

  it('INV-27: una candidata duplicada por preguntaId no se cuenta dos veces', () => {
    const pregunta = crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO });
    const ensamblador = new EnsambladorSimulacroServicio(new FuenteAleatoriaFija());

    const error = capturarError(() =>
      ensamblador.ensamblar([pregunta, pregunta], criterioPorCompetencia(ID_COMPETENCIA_RAZONAMIENTO), 2),
    );

    expect(error.codigo).toBe(CodigoError.PREGUNTAS_INSUFICIENTES);
  });

  it('rechaza una cantidadPreguntas no entera o menor a 1 con SOLICITUD_INVALIDA', () => {
    const ensamblador = new EnsambladorSimulacroServicio(new FuenteAleatoriaFija());
    const candidatas = [crearPreguntaPublicada()];

    const errorCero = capturarError(() => ensamblador.ensamblar(candidatas, criterioPorCompetencia('x'), 0));
    const errorDecimal = capturarError(() => ensamblador.ensamblar(candidatas, criterioPorCompetencia('x'), 1.5));

    expect(errorCero.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
    expect(errorDecimal.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('con una fuente aleatoria fija en 0 conserva el orden original y elige las primeras N', () => {
    const preguntas = [
      crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }),
      crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }),
      crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }),
    ];
    const ensamblador = new EnsambladorSimulacroServicio(new FuenteAleatoriaFija());

    const elegidas = ensamblador.ensamblar(preguntas, criterioPorCompetencia(ID_COMPETENCIA_RAZONAMIENTO), 2);

    expect(elegidas.map((p) => p.preguntaId)).toEqual([preguntas[0].preguntaId, preguntas[1].preguntaId]);
  });
});
