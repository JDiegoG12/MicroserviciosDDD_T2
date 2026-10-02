import { EstadoPreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { RegistrarPreguntaArchivadaCasoUso } from '../../src/aplicacion/preguntas-evaluables/registrar-pregunta-archivada.caso-uso';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { RegistroEventosProcesadosEnMemoria } from '../dobles/registro-eventos-procesados-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';

const PREGUNTA_ID = '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f';

describe('RegistrarPreguntaArchivadaCasoUso', () => {
  it('crea una marca de archivo cuando la pregunta aun no era conocida', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const casoUso = new RegistrarPreguntaArchivadaCasoUso(
      repositorio,
      new RegistroEventosProcesadosEnMemoria(),
      new RelojFijo(),
    );

    const resultado = await casoUso.ejecutar({
      idEvento: 'a1a1a1a1-1111-4111-8111-000000000001',
      preguntaId: PREGUNTA_ID,
      motivo: 'Contenido desactualizado',
      fechaArchivado: new Date('2026-10-05T10:00:00Z'),
    });

    expect(resultado).toBe('REGISTRADO');
    const pregunta = await repositorio.obtenerPorId(PREGUNTA_ID);
    expect(pregunta?.estado).toBe(EstadoPreguntaEvaluable.ARCHIVADA);
    expect(pregunta?.contenido).toBeNull();
  });

  it('el mismo idEvento dos veces no duplica (idempotencia, CONTRATOS.md 7.7.2)', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    const casoUso = new RegistrarPreguntaArchivadaCasoUso(
      repositorio,
      new RegistroEventosProcesadosEnMemoria(),
      new RelojFijo(),
    );
    const comando = {
      idEvento: 'a1a1a1a1-1111-4111-8111-000000000001',
      preguntaId: PREGUNTA_ID,
      motivo: 'Contenido desactualizado',
      fechaArchivado: new Date('2026-10-05T10:00:00Z'),
    };

    const primerResultado = await casoUso.ejecutar(comando);
    const segundoResultado = await casoUso.ejecutar(comando);

    expect(primerResultado).toBe('REGISTRADO');
    expect(segundoResultado).toBe('DUPLICADO');
    expect(repositorio.cantidadDeGuardados).toBe(1);
  });
});
