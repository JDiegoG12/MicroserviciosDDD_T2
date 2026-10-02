import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { LetraOpcion } from '../../src/dominio/compartido/letra-opcion';
import { CriterioDeGeneracion } from '../../src/dominio/simulacros/criterio-de-generacion';
import { DuracionMaxima } from '../../src/dominio/simulacros/duracion-maxima';
import { Simulacro } from '../../src/dominio/simulacros/simulacro';
import { ID_DOCENTE } from '../apoyo/datos-de-prueba';
import { crearPreguntaPublicada } from '../apoyo/fabrica-preguntas';
import { construirAppDePrueba, DoblesDeAppDePrueba } from './apoyo/construir-app-de-prueba';
import { encabezadosDocente, encabezadosEstudiante, encabezadosOtroEstudiante } from './apoyo/encabezados';

function criterioSinFiltro() {
  return CriterioDeGeneracion.crear({ competenciaIds: [], temaIds: [], subtemaIds: [], nivelesDificultad: [] });
}

describe('Intentos (CONTRATOS.md 8.3)', () => {
  let app: INestApplication;
  let dobles: DoblesDeAppDePrueba;

  beforeAll(async () => {
    ({ app, dobles } = await construirAppDePrueba());
  });

  afterAll(async () => {
    await app.close();
  });

  async function crearSimulacroDePrueba(cantidadPreguntas = 2) {
    const preguntas = Array.from({ length: cantidadPreguntas }, () =>
      crearPreguntaPublicada({ letraCorrecta: LetraOpcion.A }),
    );
    for (const pregunta of preguntas) {
      dobles.preguntaEvaluableRepositorio.agregar(pregunta);
    }
    const simulacro = Simulacro.definir({
      docenteId: ID_DOCENTE,
      nombre: 'Simulacro de prueba API',
      criterio: criterioSinFiltro(),
      duracion: DuracionMaxima.enMinutos(30),
      preguntas,
      ahora: dobles.reloj.ahora(),
    });
    await dobles.simulacroRepositorio.guardar(simulacro);
    return simulacro;
  }

  it('flujo feliz completo: iniciar, responder y finalizar un intento', async () => {
    const simulacro = await crearSimulacroDePrueba(2);

    const inicio = await request(app.getHttpServer())
      .post(`/api/v1/simulacros/${simulacro.simulacroId.aTexto()}/intentos`)
      .set(encabezadosEstudiante());
    expect(inicio.status).toBe(201);
    expect(inicio.headers.location).toBe(`/api/v1/intentos/${inicio.body.intentoId}`);
    expect(inicio.body.estado).toBe('EN_CURSO');
    expect(JSON.stringify(inicio.body)).not.toContain('letraCorrecta');

    const intentoId = inicio.body.intentoId;
    const [primeraPregunta, segundaPregunta] = inicio.body.preguntas;

    const respuesta1 = await request(app.getHttpServer())
      .put(`/api/v1/intentos/${intentoId}/respuestas/${primeraPregunta.preguntaId}`)
      .set(encabezadosEstudiante())
      .send({ letraSeleccionada: 'A' });
    expect(respuesta1.status).toBe(200);

    const respuesta2 = await request(app.getHttpServer())
      .put(`/api/v1/intentos/${intentoId}/respuestas/${segundaPregunta.preguntaId}`)
      .set(encabezadosEstudiante())
      .send({ letraSeleccionada: 'A' });
    expect(respuesta2.status).toBe(200);

    const final = await request(app.getHttpServer())
      .post(`/api/v1/intentos/${intentoId}/finalizacion`)
      .set(encabezadosEstudiante());

    expect(final.status).toBe(200);
    expect(final.body.estado).toBe('CALIFICADO');
    expect(final.body.calificacion.puntaje).toBe(100);
    expect(dobles.publicadorEventos.buscarPorTipo('IntentoDeSimulacroCalificado')).toBeDefined();
  });

  it('PUT respuesta con un letraSeleccionada invalida: 400 SOLICITUD_INVALIDA', async () => {
    const simulacro = await crearSimulacroDePrueba(1);
    const inicio = await request(app.getHttpServer())
      .post(`/api/v1/simulacros/${simulacro.simulacroId.aTexto()}/intentos`)
      .set(encabezadosEstudiante());
    const preguntaId = inicio.body.preguntas[0].preguntaId;

    const respuesta = await request(app.getHttpServer())
      .put(`/api/v1/intentos/${inicio.body.intentoId}/respuestas/${preguntaId}`)
      .set(encabezadosEstudiante())
      .send({ letraSeleccionada: 'Z' });

    expect(respuesta.status).toBe(400);
    expect(respuesta.body.codigo).toBe('SOLICITUD_INVALIDA');
  });

  it('un estudiante que no es el dueno recibe 403 ACCESO_DENEGADO', async () => {
    const simulacro = await crearSimulacroDePrueba(1);
    const inicio = await request(app.getHttpServer())
      .post(`/api/v1/simulacros/${simulacro.simulacroId.aTexto()}/intentos`)
      .set(encabezadosEstudiante());

    const respuesta = await request(app.getHttpServer())
      .get(`/api/v1/intentos/${inicio.body.intentoId}`)
      .set(encabezadosOtroEstudiante());

    expect(respuesta.status).toBe(403);
    expect(respuesta.body.codigo).toBe('ACCESO_DENEGADO');
  });

  it('un DOCENTE puede consultar cualquier intento, en solo lectura', async () => {
    const simulacro = await crearSimulacroDePrueba(1);
    const inicio = await request(app.getHttpServer())
      .post(`/api/v1/simulacros/${simulacro.simulacroId.aTexto()}/intentos`)
      .set(encabezadosEstudiante());

    const respuesta = await request(app.getHttpServer())
      .get(`/api/v1/intentos/${inicio.body.intentoId}`)
      .set(encabezadosDocente());

    expect(respuesta.status).toBe(200);
  });

  it('finalizar un intento ya finalizado: 409 INTENTO_FINALIZADO', async () => {
    const simulacro = await crearSimulacroDePrueba(1);
    const inicio = await request(app.getHttpServer())
      .post(`/api/v1/simulacros/${simulacro.simulacroId.aTexto()}/intentos`)
      .set(encabezadosEstudiante());
    await request(app.getHttpServer())
      .post(`/api/v1/intentos/${inicio.body.intentoId}/finalizacion`)
      .set(encabezadosEstudiante());

    const respuesta = await request(app.getHttpServer())
      .post(`/api/v1/intentos/${inicio.body.intentoId}/finalizacion`)
      .set(encabezadosEstudiante());

    expect(respuesta.status).toBe(409);
    expect(respuesta.body.codigo).toBe('INTENTO_FINALIZADO');
  });

  it('POST intentos sobre un simulacro inexistente: 404 SIMULACRO_NO_ENCONTRADO', async () => {
    const respuesta = await request(app.getHttpServer())
      .post('/api/v1/simulacros/5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f/intentos')
      .set(encabezadosEstudiante());

    expect(respuesta.status).toBe(404);
    expect(respuesta.body.codigo).toBe('SIMULACRO_NO_ENCONTRADO');
  });
});
