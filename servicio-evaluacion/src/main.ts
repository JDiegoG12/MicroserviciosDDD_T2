import 'reflect-metadata';
import { RequestMethod, ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { ModuloPrincipal } from './modulo-principal';
import { SolicitudInvalidaHttpExcepcion } from './interfaces/rest/excepciones/solicitud-invalida-http.excepcion';
import { FiltroExcepcionesGlobal } from './interfaces/rest/filtros/filtro-excepciones.global';
import { aplanarErroresDeValidacion } from './interfaces/rest/filtros/aplanar-errores-de-validacion';
import { RegistradorNestjsServicio } from './infraestructura/observabilidad/registrador-nestjs.servicio';

/**
 * Punto de entrada del servicio de Evaluacion y Simulacros.
 *
 * Prefijo `/api/v1` (CONTRATOS.md 5.1), con `/salud` fuera de el. El
 * `ValidationPipe` global traduce cualquier cuerpo invalido a
 * `SolicitudInvalidaHttpExcepcion`, que el filtro global convierte en el
 * `application/problem+json` de CONTRATOS.md 5.2. Swagger UI en `/docs` y
 * el JSON en `/openapi.json`.
 *
 * `logger: new RegistradorNestjsServicio()` hace que los propios logs de
 * arranque de Nest (`NestFactory`, `InstanceLoader`, `RoutesResolver`...)
 * pasen por el mismo `registrar()` del resto del servicio, con
 * `idCorrelacion: "-"` y el mismo formato JSON en todas las lineas.
 */
async function iniciar(): Promise<void> {
  const aplicacion = await NestFactory.create(ModuloPrincipal, { logger: new RegistradorNestjsServicio() });

  aplicacion.setGlobalPrefix('api/v1', {
    exclude: [
      { path: 'salud', method: RequestMethod.GET },
      { path: 'docs', method: RequestMethod.GET },
      { path: 'openapi.json', method: RequestMethod.GET },
    ],
  });

  aplicacion.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: false,
      exceptionFactory: (errores) => new SolicitudInvalidaHttpExcepcion(aplanarErroresDeValidacion(errores)),
    }),
  );
  aplicacion.useGlobalFilters(new FiltroExcepcionesGlobal());

  const documentoOpenApi = SwaggerModule.createDocument(
    aplicacion,
    new DocumentBuilder()
      .setTitle('servicio-evaluacion')
      .setDescription('Evaluacion y Simulacros (Banco de Preguntas Saber Pro, Taller 2). CONTRATOS.md secciones 7, 8.3 y 11.3.')
      .setVersion('1.0')
      .build(),
  );
  SwaggerModule.setup('docs', aplicacion, documentoOpenApi, { jsonDocumentUrl: 'openapi.json' });

  const puerto = Number(process.env.EVALUACION_PUERTO_HTTP ?? 8083);
  await aplicacion.listen(puerto);
}

void iniciar();
