import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';
import { construirAppDePrueba, DoblesDeAppDePrueba } from './apoyo/construir-app-de-prueba';
import { encabezadosDocente, encabezadosEstudiante } from './apoyo/encabezados';

describe('GET /preguntas-evaluables (CONTRATOS.md 8.3)', () => {
  let app: INestApplication;
  let dobles: DoblesDeAppDePrueba;

  beforeAll(async () => {
    ({ app, dobles } = await construirAppDePrueba());
  });

  afterAll(async () => {
    await app.close();
  });

  it('devuelve una pagina de copias locales sin letraCorrecta, rol DOCENTE', async () => {
    dobles.preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada());
    dobles.preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada());

    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/preguntas-evaluables')
      .set(encabezadosDocente());

    expect(respuesta.status).toBe(200);
    expect(respuesta.body.contenido.length).toBeGreaterThanOrEqual(2);
    expect(JSON.stringify(respuesta.body)).not.toContain('letraCorrecta');
  });

  it('rechaza el rol ESTUDIANTE con 403 ACCESO_DENEGADO', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/preguntas-evaluables')
      .set(encabezadosEstudiante());

    expect(respuesta.status).toBe(403);
  });
});
