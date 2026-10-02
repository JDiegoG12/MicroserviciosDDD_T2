import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { NivelDificultad } from '../../src/dominio/compartido/nivel-dificultad';
import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { crearContenidoDePregunta } from '../../src/dominio/preguntas-evaluables/contenido-de-pregunta';
import { EstadoPreguntaEvaluable, PreguntaEvaluable } from '../../src/dominio/preguntas-evaluables/pregunta-evaluable';
import { capturarError } from '../apoyo/afirmaciones';
import { ID_COMPETENCIA_RAZONAMIENTO, ID_SUBTEMA_MEDIDAS_CENTRALES, ID_TEMA_ESTADISTICA } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

const AHORA = new Date('2026-10-01T15:30:00Z');
const PREGUNTA_ID = '5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f';

function construirContenidoValido() {
  return crearContenidoDePregunta({
    contexto: 'Contexto de prueba.',
    preguntaDirecta: 'Pregunta directa de prueba?',
    opciones: [
      { letra: LetraOpcion.A, texto: 'Opcion A' },
      { letra: LetraOpcion.B, texto: 'Opcion B' },
      { letra: LetraOpcion.C, texto: 'Opcion C' },
      { letra: LetraOpcion.D, texto: 'Opcion D' },
    ],
    letraCorrecta: LetraOpcion.B,
    clasificacion: {
      competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
      temaId: ID_TEMA_ESTADISTICA,
      subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
    },
    nivelDificultad: NivelDificultad.MEDIO,
    fechaPublicacion: AHORA,
  });
}

describe('PreguntaEvaluable', () => {
  it('desdePublicacion crea la copia en estado PUBLICADA con el contenido recibido', () => {
    const contenido = construirContenidoValido();
    const pregunta = PreguntaEvaluable.desdePublicacion(PREGUNTA_ID, contenido, AHORA);

    expect(pregunta.estado).toBe(EstadoPreguntaEvaluable.PUBLICADA);
    expect(pregunta.estaPublicada()).toBe(true);
    expect(pregunta.contenido).toBe(contenido);
  });

  it('crearContenidoDePregunta rechaza letras repetidas con SOLICITUD_INVALIDA', () => {
    const error = capturarError(() =>
      crearContenidoDePregunta({
        contexto: 'Contexto',
        preguntaDirecta: 'Pregunta',
        opciones: [
          { letra: LetraOpcion.A, texto: 'A' },
          { letra: LetraOpcion.A, texto: 'A repetida' },
          { letra: LetraOpcion.C, texto: 'C' },
          { letra: LetraOpcion.D, texto: 'D' },
        ],
        letraCorrecta: LetraOpcion.A,
        clasificacion: {
          competenciaId: ID_COMPETENCIA_RAZONAMIENTO,
          temaId: ID_TEMA_ESTADISTICA,
          subtemaId: ID_SUBTEMA_MEDIDAS_CENTRALES,
        },
        nivelDificultad: NivelDificultad.BAJO,
        fechaPublicacion: AHORA,
      }),
    );

    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('marcaDeArchivo crea una copia ARCHIVADA sin contenido, con motivo y fecha', () => {
    const fechaArchivado = new Date('2026-10-05T10:00:00Z');
    const marca = PreguntaEvaluable.marcaDeArchivo(PREGUNTA_ID, 'Contenido desactualizado', fechaArchivado, AHORA);

    expect(marca.estado).toBe(EstadoPreguntaEvaluable.ARCHIVADA);
    expect(marca.contenido).toBeNull();
    expect(marca.estaPublicada()).toBe(false);
    expect(marca.motivoArchivo).toBe('Contenido desactualizado');
    expect(marca.fechaArchivado).toBe(fechaArchivado);
  });

  it('archivar conserva el contenido existente, cambia el estado a ARCHIVADA y guarda motivo y fecha', () => {
    const contenido = construirContenidoValido();
    const publicada = PreguntaEvaluable.desdePublicacion(PREGUNTA_ID, contenido, AHORA);
    const fechaArchivado = new Date('2026-10-05T10:00:00Z');

    const archivada = publicada.archivar('Contenido desactualizado', fechaArchivado, fechaArchivado);

    expect(archivada.estado).toBe(EstadoPreguntaEvaluable.ARCHIVADA);
    expect(archivada.contenido).toBe(contenido);
    expect(archivada.motivoArchivo).toBe('Contenido desactualizado');
    expect(archivada.fechaArchivado).toBe(fechaArchivado);
  });

  it('archivar rechaza un motivo vacio', () => {
    const publicada = crearPreguntaPublicada();
    const error = capturarError(() => publicada.archivar('   ', AHORA, AHORA));
    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('regla de orden 7.7.3: una pregunta archivada sigue archivada aunque llegue su publicacion', () => {
    const marca = PreguntaEvaluable.marcaDeArchivo(PREGUNTA_ID, 'Motivo', new Date('2026-10-05T10:00:00Z'), AHORA);
    const contenido = construirContenidoValido();

    const actualizada = marca.incorporarPublicacion(contenido, new Date('2026-10-02T00:00:00Z'));

    expect(actualizada.estado).toBe(EstadoPreguntaEvaluable.ARCHIVADA);
    expect(actualizada.contenido).toBe(contenido);
  });

  it('INV-32/A.2: incorporarPublicacion no modifica una copia que ya tenia contenido', () => {
    const contenidoOriginal = construirContenidoValido();
    const publicada = PreguntaEvaluable.desdePublicacion(PREGUNTA_ID, contenidoOriginal, AHORA);
    const otroContenido = construirContenidoValido();

    const resultado = publicada.incorporarPublicacion(otroContenido, new Date('2026-10-02T00:00:00Z'));

    expect(resultado).toBe(publicada);
    expect(resultado.contenido).toBe(contenidoOriginal);
  });

  it('vistaSinClave nunca incluye letraCorrecta', () => {
    const contenido = construirContenidoValido();
    const pregunta = PreguntaEvaluable.desdePublicacion(PREGUNTA_ID, contenido, AHORA);

    const vista = pregunta.vistaSinClave();

    expect(vista).not.toHaveProperty('letraCorrecta');
    expect(JSON.stringify(vista)).not.toContain('letraCorrecta');
  });

  it('esCorrecta compara la letra contra la letraCorrecta de la copia', () => {
    const pregunta = crearPreguntaPublicada({ letraCorrecta: LetraOpcion.C });

    expect(pregunta.esCorrecta(LetraOpcion.C)).toBe(true);
    expect(pregunta.esCorrecta(LetraOpcion.A)).toBe(false);
  });

  it('desdePublicacion y marcaDeArchivo rechazan un preguntaId que no es UUID', () => {
    const contenido = construirContenidoValido();

    expect(capturarError(() => PreguntaEvaluable.desdePublicacion('no-es-uuid', contenido, AHORA)).codigo).toBe(
      CodigoError.SOLICITUD_INVALIDA,
    );
    expect(
      capturarError(() => PreguntaEvaluable.marcaDeArchivo('no-es-uuid', 'Motivo', AHORA, AHORA)).codigo,
    ).toBe(CodigoError.SOLICITUD_INVALIDA);
  });

  it('reconstruir recupera una copia identica a la persistida', () => {
    const contenido = construirContenidoValido();
    const original = PreguntaEvaluable.desdePublicacion(PREGUNTA_ID, contenido, AHORA);

    const reconstruida = PreguntaEvaluable.reconstruir({
      preguntaId: original.preguntaId,
      contenido: original.contenido,
      estado: original.estado,
      motivoArchivo: original.motivoArchivo,
      fechaArchivado: original.fechaArchivado,
      fechaActualizacion: original.fechaActualizacion,
    });

    expect(reconstruida.preguntaId).toBe(original.preguntaId);
    expect(reconstruida.fechaActualizacion).toBe(AHORA);
  });

  it('esCorrecta y vistaSinClave lanzan SOLICITUD_INVALIDA sobre una marca sin contenido', () => {
    const marca = PreguntaEvaluable.marcaDeArchivo(PREGUNTA_ID, 'Motivo', AHORA, AHORA);

    expect(capturarError(() => marca.esCorrecta(LetraOpcion.A)).codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
    expect(capturarError(() => marca.vistaSinClave()).codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });
});
