import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { ID_COMPETENCIA_RAZONAMIENTO } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';
import { construirAppDePrueba, DoblesDeAppDePrueba } from './apoyo/construir-app-de-prueba';
import { encabezadosDocente, encabezadosEstudiante } from './apoyo/encabezados';

describe('Simulacros (CONTRATOS.md 8.3)', () => {
  let app: INestApplication;
  let dobles: DoblesDeAppDePrueba;

  beforeAll(async () => {
    ({ app, dobles } = await construirAppDePrueba());
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => {
    dobles.preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }));
    dobles.preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }));
  });

  it('POST /simulacros (caso feliz): 201, Location y cuerpo con SimulacroRespuesta', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .send({
        nombre: 'Simulacro de API',
        criterios: {
          competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO],
          temaIds: [],
          subtemaIds: [],
          nivelesDificultad: [],
        },
        cantidadPreguntas: 2,
        duracionMaximaMinutos: 30,
      });

    expect(respuesta.status).toBe(201);
    expect(respuesta.headers.location).toBe(`/api/v1/simulacros/${respuesta.body.simulacroId}`);
    expect(respuesta.body.preguntas).toHaveLength(2);
  });

  it('POST /simulacros con rol ESTUDIANTE: 403 ACCESO_DENEGADO', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosEstudiante())
      .send({
        nombre: 'Simulacro de API',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 1,
        duracionMaximaMinutos: 30,
      });

    expect(respuesta.status).toBe(403);
  });

  it('POST /simulacros con duracion invalida: 422 DURACION_INVALIDA', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .send({
        nombre: 'Simulacro con duracion invalida',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 1,
        duracionMaximaMinutos: 0,
      });

    expect(respuesta.status).toBe(422);
    expect(respuesta.body.codigo).toBe('DURACION_INVALIDA');
  });

  it('POST /simulacros con duracionMaximaMinutos mal formada (no es un numero): 400 SOLICITUD_INVALIDA', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .send({
        nombre: 'Simulacro con duracion mal formada',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 1,
        duracionMaximaMinutos: 'treinta',
      });

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
  });

  it('POST /simulacros sin candidatas suficientes: 422 PREGUNTAS_INSUFICIENTES', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .send({
        nombre: 'Simulacro imposible',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 999,
        duracionMaximaMinutos: 30,
      });

    expect(respuesta.status).toBe(422);
    expect(respuesta.body.codigo).toBe('PREGUNTAS_INSUFICIENTES');
  });

  it('GET /simulacros devuelve una pagina (CONTRATOS.md 5.1), rol ESTUDIANTE incluido', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/simulacros')
      .set(encabezadosEstudiante());

    expect(respuesta.status).toBe(200);
    expect(respuesta.body).toHaveProperty('contenido');
    expect(respuesta.body).toHaveProperty('totalElementos');
    expect(respuesta.body).toHaveProperty('pagina');
    expect(respuesta.body).toHaveProperty('tamano');
  });

  it('GET /simulacros?tamano=101 -> 400 SOLICITUD_INVALIDA', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/simulacros')
      .query({ tamano: 101 })
      .set(encabezadosDocente());

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
  });
});
