import { CodigoError } from '../../src/dominio/excepciones/codigo-error';
import { ConsultarPreguntasEvaluablesCasoUso } from '../../src/aplicacion/preguntas-evaluables/consultar-preguntas-evaluables.caso-uso';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { capturarErrorAsincrono } from '../apoyo/afirmaciones';
import { crearDocente, crearEstudiante } from '../apoyo/fabrica-usuarios';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';

describe('ConsultarPreguntasEvaluablesCasoUso', () => {
  it('devuelve una pagina de preguntas evaluables sin letraCorrecta', async () => {
    const repositorio = new PreguntaEvaluableRepositorioEnMemoria();
    repositorio.agregar(crearPreguntaPublicada());
    repositorio.agregar(crearPreguntaPublicada());
    const casoUso = new ConsultarPreguntasEvaluablesCasoUso(repositorio);

    const pagina = await casoUso.ejecutar({ usuario: crearDocente() });

    expect(pagina.contenido).toHaveLength(2);
    expect(pagina.totalElementos).toBe(2);
    expect(JSON.stringify(pagina)).not.toContain('letraCorrecta');
  });

  it('rechaza a un usuario sin rol DOCENTE con ACCESO_DENEGADO', async () => {
    const casoUso = new ConsultarPreguntasEvaluablesCasoUso(new PreguntaEvaluableRepositorioEnMemoria());

    const error = await capturarErrorAsincrono(() => casoUso.ejecutar({ usuario: crearEstudiante() }));

    expect(error.codigo).toBe(CodigoError.ACCESO_DENEGADO);
  });

  it('rechaza un tamano de pagina mayor a 100 con SOLICITUD_INVALIDA', async () => {
    const casoUso = new ConsultarPreguntasEvaluablesCasoUso(new PreguntaEvaluableRepositorioEnMemoria());

    const error = await capturarErrorAsincrono(() =>
      casoUso.ejecutar({ usuario: crearDocente(), tamano: 101 }),
    );

    expect(error.codigo).toBe(CodigoError.SOLICITUD_INVALIDA);
  });
});
