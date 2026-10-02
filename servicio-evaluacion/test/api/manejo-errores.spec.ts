import { INestApplication, RequestMethod, ValidationPipe } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { BaseDeDatosNoDisponibleExcepcion } from '../../src/infraestructura/persistencia/mongo/base-de-datos-no-disponible.excepcion';
import { PROVIDERS_DE_CASOS_DE_USO } from '../../src/infraestructura/casos-de-uso.providers';
import {
  FUENTE_ALEATORIA,
  INTENTO_DE_SIMULACRO_REPOSITORIO,
  PREGUNTA_EVALUABLE_REPOSITORIO,
  PUBLICADOR_EVENTOS_PUERTO,
  REGISTRO_EVENTOS_PROCESADOS_PUERTO,
  RELOJ_PUERTO,
  SIMULACRO_REPOSITORIO,
} from '../../src/infraestructura/tokens-de-inyeccion';
import { IdentidadGuardia } from '../../src/interfaces/rest/guardias/identidad.guardia';
import { aplanarErroresDeValidacion } from '../../src/interfaces/rest/filtros/aplanar-errores-de-validacion';
import { FiltroExcepcionesGlobal } from '../../src/interfaces/rest/filtros/filtro-excepciones.global';
import { SolicitudInvalidaHttpExcepcion } from '../../src/interfaces/rest/excepciones/solicitud-invalida-http.excepcion';
import { SimulacrosControlador } from '../../src/interfaces/rest/simulacros.controlador';
import { PreguntaEvaluableRepositorioEnMemoria } from '../dobles/pregunta-evaluable-repositorio-en-memoria';
import { IntentoDeSimulacroRepositorioEnMemoria } from '../dobles/intento-de-simulacro-repositorio-en-memoria';
import { RegistroEventosProcesadosEnMemoria } from '../dobles/registro-eventos-procesados-en-memoria';
import { PublicadorEventosEnMemoria } from '../dobles/publicador-eventos-en-memoria';
import { RelojFijo } from '../dobles/reloj-fijo';
import { FuenteAleatoriaFija } from '../dobles/fuente-aleatoria-fija';
import { construirAppDePrueba } from './apoyo/construir-app-de-prueba';
import { encabezadosDocente, encabezadosEstudiante } from './apoyo/encabezados';

/** Repositorio que siempre falla como si Mongo estuviera desconectado. */
class SimulacroRepositorioCaido {
  public async guardar(): Promise<never> {
    throw new BaseDeDatosNoDisponibleExcepcion();
  }
  public async obtenerPorId(): Promise<never> {
    throw new BaseDeDatosNoDisponibleExcepcion();
  }
  public async listar(): Promise<never> {
    throw new BaseDeDatosNoDisponibleExcepcion();
  }
}

/**
 * Arma una app de prueba identica a `construirAppDePrueba`, pero con
 * `SimulacroRepositorio` lanzando `BaseDeDatosNoDisponibleExcepcion` en
 * cada llamada, para probar el 503 sin necesitar MongoDB real.
 */
async function construirAppConBaseDeDatosCaida(): Promise<INestApplication> {
  const modulo = await Test.createTestingModule({
    controllers: [SimulacrosControlador],
    providers: [
      { provide: SIMULACRO_REPOSITORIO, useClass: SimulacroRepositorioCaido },
      { provide: PREGUNTA_EVALUABLE_REPOSITORIO, useValue: new PreguntaEvaluableRepositorioEnMemoria() },
      { provide: INTENTO_DE_SIMULACRO_REPOSITORIO, useValue: new IntentoDeSimulacroRepositorioEnMemoria() },
      { provide: REGISTRO_EVENTOS_PROCESADOS_PUERTO, useValue: new RegistroEventosProcesadosEnMemoria() },
      { provide: PUBLICADOR_EVENTOS_PUERTO, useValue: new PublicadorEventosEnMemoria() },
      { provide: RELOJ_PUERTO, useValue: new RelojFijo() },
      { provide: FUENTE_ALEATORIA, useValue: new FuenteAleatoriaFija() },
      { provide: APP_GUARD, useClass: IdentidadGuardia },
      ...PROVIDERS_DE_CASOS_DE_USO,
    ],
  }).compile();

  const app = modulo.createNestApplication();
  app.setGlobalPrefix('api/v1', { exclude: [{ path: 'salud', method: RequestMethod.GET }] });
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      exceptionFactory: (errores) => new SolicitudInvalidaHttpExcepcion(aplanarErroresDeValidacion(errores)),
    }),
  );
  app.useGlobalFilters(new FiltroExcepcionesGlobal());
  await app.init();
  return app;
}

describe('Manejo de errores (CONTRATOS.md 5.2 y 5.3)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    ({ app } = await construirAppDePrueba());
  });

  afterAll(async () => {
    await app.close();
  });

  it('401 NO_AUTENTICADO si faltan los encabezados de identidad', async () => {
    const respuesta = await request(app.getHttpServer()).get('/api/v1/preguntas-evaluables');

    expect(respuesta.status).toBe(401);
    expect(respuesta.type).toBe('application/problem+json');
    expect(respuesta.body.codigo).toBe('NO_AUTENTICADO');
    expect(respuesta.body).toHaveProperty('idCorrelacion');
    expect(respuesta.body).toHaveProperty('instance', '/api/v1/preguntas-evaluables');
  });

  it('400 SOLICITUD_INVALIDA si X-Usuario-Id no es un UUID', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/preguntas-evaluables')
      .set('X-Usuario-Id', 'no-es-uuid')
      .set('X-Roles', 'DOCENTE');

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
  });

  it('400 SOLICITUD_INVALIDA si X-Roles trae un rol que no existe', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/preguntas-evaluables')
      .set('X-Usuario-Id', '11111111-1111-4111-8111-000000000005')
      .set('X-Roles', 'SUPERADMIN');

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
  });

  it('403 ACCESO_DENEGADO con un rol insuficiente', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/preguntas-evaluables')
      .set(encabezadosEstudiante());

    expect(respuesta.status).toBe(403);
    expect(respuesta.body.codigo).toBe('ACCESO_DENEGADO');
  });

  it('400 SOLICITUD_INVALIDA con la lista de errores si el cuerpo es invalido', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros')
      .set(encabezadosDocente())
      .send({ nombre: '' });

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
    expect(Array.isArray(respuesta.body.errores)).toBe(true);
    expect(respuesta.body.errores.length).toBeGreaterThan(0);
  });

  it('devuelve el X-Id-Correlacion recibido, igual en el encabezado y en el cuerpo', async () => {
    const idCorrelacion = '9a1b2c3d-4e5f-4061-8071-8091a0b1c2d3';
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/preguntas-evaluables')
      .set(encabezadosDocente())
      .set('X-Id-Correlacion', idCorrelacion);

    expect(respuesta.status).toBe(200);
    expect(respuesta.headers['x-id-correlacion']).toBe(idCorrelacion);
  });

  it('404 SIMULACRO_NO_ENCONTRADO se traduce al HTTP correcto', async () => {
    const respuesta = await request(app.getHttpServer())
      .get('/api/v1/simulacros/5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f')
      .set(encabezadosDocente());

    expect(respuesta.status).toBe(404);
    expect(respuesta.body.codigo).toBe('SIMULACRO_NO_ENCONTRADO');
  });

  describe('503 BASE_DE_DATOS_NO_DISPONIBLE (CONTRATOS.md 5.3 y 9.3.6)', () => {
    let appConBaseDeDatosCaida: INestApplication;

    beforeAll(async () => {
      appConBaseDeDatosCaida = await construirAppConBaseDeDatosCaida();
    });

    afterAll(async () => {
      await appConBaseDeDatosCaida.close();
    });

    it('un repositorio que no puede conectar a Mongo responde 503, no 500', async () => {
      const respuesta = await request(appConBaseDeDatosCaida.getHttpServer())
        .get('/api/v1/simulacros')
        .set(encabezadosDocente());

      expect(respuesta.status).toBe(503);
      expect(respuesta.type).toBe('application/problem+json');
      expect(respuesta.body.codigo).toBe('BASE_DE_DATOS_NO_DISPONIBLE');
    });
  });
});
