import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { EstadoPreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { RegistrarPreguntaArchivadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-archivada.caso-uso';
import { RegistrarPreguntaPublicadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-publicada.caso-uso';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { RegistroEventosProcesadosEnMemoria } from '../dobles/registro-eventos-procesados-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';
import {
  ID_COMPETENCIA_RAZONAMIENTO,
  ID_SUBTEMA_MEDIDAS_CENTRALES,
  ID_TEMA_ESTADISTICA,
} from '../apoyo/datos-de-prueba';

const PREGUNTA_ID = '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f';
const ID_EVENTO = '6d1f3a0c-3b2e-4f7a-9c1d-2e3f4a5b6c7d';

function comandoPublicada() {
  return {
    idEvento: ID_EVENTO,
    preguntaId: PREGUNTA_ID,
    contexto: 'Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.',
    preguntaDirecta: 'Cual es la moda del conjunto de notas?',
    opciones: [
      { letra: LetraOpcion.A, texto: '3,0' },
      { letra: LetraOpcion.B, texto: '3,8' },
      { letra: LetraOpcion.C, texto: '4,0' },
      { letra: LetraOpcion.D, texto: '4,5' },
    ],
    letraCorrecta: LetraOpcion.C,
    clasificacion: {
      competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
      temaId: ID_TEMA_ESTADISTICA,
      subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
    },
    nivelDificultad: NivelDificultad.BAJO,
    fechaPublicacion: new Date('2026-10-01T15:30:00Z'),
  };
}

function criterioSinFiltro() {
  return CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] });
}

describe('Consumo de eventos PreguntaPublicada y PreguntaArchivada (CONTRATOS.md 7.7)', () => {
  it('el mismo idEvento dos veces no duplica la copia local', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const registroEventos = new RegistroEventosProcesadosEnMemoria();
    const casoUso = new RegistrarPreguntaPublicadaCasoUso(repositorio, registroEventos, new RelojFijo());

    const primerResultado = await casoUso.ejecutar(comandoPublicada());
    const segundoResultado = await casoUso.ejecutar(comandoPublicada());

    expect(primerResultado).toBe('REGISTRADO');
    expect(segundoResultado).toBe('DUPLICADO');
    expect(repositorio.cantidadDeGuardados).toBe(1);
  });

  it('una pregunta archivada antes que su publicacion queda ARCHIVADA (orden no garantizado)', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const registroEventos = new RegistroEventosProcesadosEnMemoria();
    const archivarCasoUso = new RegistrarPreguntaArchivadaCasoUso(repositorio, registroEventos, new RelojFijo());
    const publicarCasoUso = new RegistrarPreguntaPublicadaCasoUso(repositorio, registroEventos, new RelojFijo());

    await archivarCasoUso.ejecutar({
      idEvento: 'a1a1a1a1-1111-4111-8111-000000000001',
      preguntaId: PREGUNTA_ID,
      motivo: 'Contenido desactualizado',
      fechaArchivado: new Date('2026-10-05T10:00:00Z'),
    });
    await publicarCasoUso.ejecutar({ ...comandoPublicada(), idEvento: 'b2b2b2b2-2222-4222-8222-000000000002' });

    const pregunta = await repositorio.obtenerPorId(PREGUNTA_ID);
    expect(pregunta?.estado).toBe(EstadoPreguntaEvaluable.ARCHIVADA);
    expect(pregunta?.contenido).not.toBeNull();
  });

  it('una publicada y luego archivada queda ARCHIVADA y no se elige en simulacros nuevos, pero conserva su contenido', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const registroEventos = new RegistroEventosProcesadosEnMemoria();
    const publicarCasoUso = new RegistrarPreguntaPublicadaCasoUso(repositorio, registroEventos, new RelojFijo());
    const archivarCasoUso = new RegistrarPreguntaArchivadaCasoUso(repositorio, registroEventos, new RelojFijo());

    await publicarCasoUso.ejecutar(comandoPublicada());
    await archivarCasoUso.ejecutar({
      idEvento: 'c3c3c3c3-3333-4333-8333-000000000003',
      preguntaId: PREGUNTA_ID,
      motivo: 'Contenido desactualizado',
      fechaArchivado: new Date('2026-10-05T10:00:00Z'),
    });

    const pregunta = await repositorio.obtenerPorId(PREGUNTA_ID);
    expect(pregunta?.estado).toBe(EstadoPreguntaEvaluable.ARCHIVADA);
    expect(pregunta?.contenido).not.toBeNull();

    const candidatasParaSimulacroNuevo = await repositorio.buscarPublicadasPorCriterios(criterioSinFiltro());
    expect(candidatasParaSimulacroNuevo).toHaveLength(0);

    // Un intento que ya referenciaba esta pregunta se sigue calificando
    // igual porque `esCorrecta` sigue funcionando sobre el contenido
    // conservado (CONTRATOS.md 11.3).
    expect(pregunta?.esCorrecta(LetraOpcion.C)).toBe(true);
  });

  it('procesa sin ir a la DLQ un mensaje con un campo adicional desconocido (lector tolerante, 7.7.4)', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const registroEventos = new RegistroEventosProcesadosEnMemoria();
    const casoUso = new RegistrarPreguntaPublicadaCasoUso(repositorio, registroEventos, new RelojFijo());

    const comandoConCampoExtra = { ...comandoPublicada(), campoDesconocido: 'se ignora' } as ReturnType<
      typeof comandoPublicada
    > & { campoDesconocido: string };

    const resultado = await casoUso.ejecutar(comandoConCampoExtra);

    expect(resultado).toBe('REGISTRADO');
  });
});
