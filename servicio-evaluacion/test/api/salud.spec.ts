import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { construirAppDePrueba } from './apoyo/construir-app-de-prueba';

describe('GET /salud', () => {
  let app: INestApplication;

  beforeAll(async () => {
    ({ app } = await construirAppDePrueba());
  });

  afterAll(async () => {
    await app.close();
  });

  it('responde 200 fuera de /api/v1 y sin encabezados de identidad', async () => {
    const respuesta = await request(app.getHttpServer()).get('/salud');

    expect(respuesta.status).toBe(200);
    expect(respuesta.body).toEqual({ estado: 'OK', servicio: 'servicio-evaluacion' });
  });
});
