import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { ID_COMPETENCIA_RAZONAMIENTO } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';
import { construirAppDePrueba, DoblesDeAppDePrueba } from './apoyo/construir-app-de-prueba';
import { encabezadosDocente } from './apoyo/encabezados';

/**
 * Una prueba de API por cada fila de la tabla de codigos comunes
 * (CONTRATOS.md secciones 4, 4.1, 5.2 y 5.3): UUID en mayusculas, 405, 415,
 * JSON ilegible, X-Id-Correlacion invalido y mensajes de validacion en
 * espanol.
 */
describe('Codigos comunes (CONTRATOS.md 4, 4.1, 5.2 y 5.3)', () => {
  let app: INestApplication;
  let dobles: DoblesDeAppDePrueba;

  beforeAll(async () => {
    ({ app, dobles } = await construirAppDePrueba());
  });

  afterAll(async () => {
    await app.close();
  });

  describe('UUID: un canonico en mayusculas se acepta y se normaliza a minusculas', () => {
    it('en la ruta: GET /simulacros/{id en MAYUSCULAS} encuentra el simulacro', async () => {
      dobles.preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }));
      const creado = await request(app.getHttpServer())
        .post('/api/v1/simulacros')
        .set(encabezadosDocente())
        .send({
          nombre: 'Simulacro para normalizacion de UUID',
          criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
          cantidadPreguntas: 1,
          duracionMaximaMinutos: 30,
        });
      expect(creado.status).toBe(201);

      const respuesta = await request(app.getHttpServer())
        .get(`/api/v1/simulacros/${creado.body.simulacroId.toUpperCase()}`)
        .set(encabezadosDocente());

      expect(respuesta.status).toBe(200);
      expect(respuesta.body.simulacroId).toBe(creado.body.simulacroId);
    });

    it('en el encabezado: X-Usuario-Id en MAYUSCULAS se acepta igual', async () => {
      const respuesta = await request(app.getHttpServer())
        .get('/api/v1/simulacros')
        .set('X-Usuario-Id', '11111111-1111-4111-8111-000000000005'.toUpperCase())
        .set('X-Roles', 'DOCENTE');

      expect(respuesta.status).toBe(200);
    });

    it('en el cuerpo: competenciaIds en MAYUSCULAS no se rechaza por formato', async () => {
      dobles.preguntaEvaluableRepositorio.agregar(crearPreguntaPublicada({ competenciaId: ID_COMPETENCIA_RAZONAMIENTO }));
      const respuesta = await request(app.getHttpServer())
        .post('/api/v1/simulacros')
        .set(encabezadosDocente())
        .send({
          nombre: 'Simulacro con competenciaIds en mayusculas',
          criterios: {
            competenciaIds: [ID_COMPETENCIA_RAZONAMIENTO.toUpperCase()],
            temaIds: [],
            subtemaIds: [],
            nivelesDificultad: [],
          },
          cantidadPreguntas: 1,
          duracionMaximaMinutos: 30,
        });

      expect(respuesta.status).toBe(201);
    });

    it.each([
      ['con llaves', `{${'11111111-1111-4111-8111-000000000005'}}`],
      ['con el prefijo urn:uuid:', `urn:uuid:11111111-1111-4111-8111-000000000005`],
      ['sin guiones', '11111111111141118111000000000005'],
    ])('rechaza una variante no canonica (%s) con 400 SOLICITUD_INVALIDA', async (_descripcion, variante) => {
      const respuesta = await request(app.getHttpServer())
        .get(`/api/v1/simulacros/${encodeURIComponent(variante)}`)
        .set(encabezadosDocente());

      expect(respuesta.status).toBe(400);
      expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
    });
  });

  it('405 METODO_NO_PERMITIDO: DELETE /simulacros (la ruta existe, no ese metodo)', async () => {
    const respuesta = await request(app.getHttpServer()).delete('/api/v1/simulacros');

    expect(respuesta.status).toBe(405);
    expect(respuesta.body.codigo).toBe('METODO_NO_PERMITIDO');
  });

  it('415 TIPO_DE_CONTENIDO_NO_SOPORTADO: POST con Content-Type text/plain', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .set('Content-Type', 'text/plain')
      .send('esto no es json');

    expect(respuesta.status).toBe(415);
    expect(respuesta.body.codigo).toBe('TIPO_DE_CONTENIDO_NO_SOPORTADO');
  });

  it('400 SOLICITUD_INVALIDA con idCorrelacion: cuerpo JSON ilegible', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .set('Content-Type', 'application/json')
      .send('{ "nombre": "sin cerrar"');

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
    expect(Array.isArray(respuesta.body.errores)).toBe(true);
    expect(respuesta.body.errores[0]).toEqual(expect.objectContaining({ campo: 'cuerpo' }));
    expect(respuesta.body).toHaveProperty('idCorrelacion');
    expect(typeof respuesta.body.idCorrelacion).toBe('string');
  });

  it('X-Id-Correlacion invalido: se genera uno nuevo y se devuelve en la respuesta', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/simulacros')
      .set(encabezadosDocente())
      .set('X-Id-Correlacion', 'no-es-un-uuid');

    expect(respuesta.status).toBe(200);
    expect(respuesta.headers['x-id-correlacion']).toBeDefined();
    expect(respuesta.headers['x-id-correlacion']).not.toBe('no-es-un-uuid');
  });

  it('mensajes de class-validator en espanol', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .send({
        nombre: '',
        criterios: { competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] },
        cantidadPreguntas: 1,
        duracionMaximaMinutos: 30,
      });

    expect(respuesta.status).toBe(400);
    const errorDeNombre = respuesta.body.errores.find((error: { campo: string }) => error.campo === 'nombre');
    expect(errorDeNombre.mensaje).toBe('nombre no debe estar vacio.');
  });
});
