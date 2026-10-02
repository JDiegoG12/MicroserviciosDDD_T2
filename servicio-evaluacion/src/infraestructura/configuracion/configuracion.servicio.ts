import { Injectable } from '@nestjs/common';

/**
 * Lee las variables de entorno exactas de CONTRATOS.md 9.2, con los valores
 * por defecto locales de esa misma tabla. Nunca hay credenciales escritas
 * en el codigo: todo viene de `process.env`, que en Docker llena
 * `docker-compose.yml` y localmente llena `.env` (a partir de
 * `.env.example`).
 */
@Injectable()
export class ConfiguracionServicio {
  /** Puerto HTTP del servicio (`EVALUACION_PUERTO_HTTP`, por defecto 8083). */
  public readonly puertoHttp: number = Number(process.env.EVALUACION_PUERTO_HTTP ?? 8083);

  /**
   * URL de conexion a MongoDB (`EVALUACION_MONGO_URL`). En Docker apunta a
   * `bd-evaluacion`; localmente a `localhost:27017` (CONTRATOS.md 9.1 y 9.2).
   */
  public readonly mongoUrl: string =
    process.env.EVALUACION_MONGO_URL ?? 'mongodb://localhost:27017/evaluacion';

  /** Host del broker (`RABBITMQ_HOST`, por defecto `localhost`). */
  public readonly rabbitMqHost: string = process.env.RABBITMQ_HOST ?? 'localhost';

  /** Puerto del broker (`RABBITMQ_PUERTO`, por defecto 5672). */
  public readonly rabbitMqPuerto: number = Number(process.env.RABBITMQ_PUERTO ?? 5672);

  /** Usuario del broker (`RABBITMQ_USUARIO`, por defecto `banco`). */
  public readonly rabbitMqUsuario: string = process.env.RABBITMQ_USUARIO ?? 'banco';

  /** Clave del broker (`RABBITMQ_CLAVE`, por defecto `banco123`). */
  public readonly rabbitMqClave: string = process.env.RABBITMQ_CLAVE ?? 'banco123';

  /**
   * URL AMQP completa, construida a partir de host, puerto, usuario y
   * clave (CONTRATOS.md 7.1: vhost `/`).
   */
  public get rabbitMqUrl(): string {
    return `amqp://${this.rabbitMqUsuario}:${this.rabbitMqClave}@${this.rabbitMqHost}:${this.rabbitMqPuerto}`;
  }

  /** Nombre de este servicio, para `GET /salud` y el campo `origen` de los eventos. */
  public readonly nombreServicio = 'servicio-evaluacion';
}

/**
 * Opciones de conexion de Mongoose (CONTRATOS.md 9.3.6), compartidas entre
 * `ModuloPrincipal` (la conexion inicial, con `lazyConnection: true`) y
 * `ReconexionMongoServicio` (los reintentos manuales tras un arranque en
 * frio): deben ser las mismas opciones en los dos lugares.
 *
 * `bufferCommands: false` hace que una operacion lanzada sin conexion
 * falle de inmediato (503 `BASE_DE_DATOS_NO_DISPONIBLE`) en vez de quedar
 * encolada en silencio. `serverSelectionTimeoutMS: 5_000` acota cuanto
 * espera cada intento antes de darse por vencido.
 */
export const OPCIONES_MONGOOSE = {
  bufferCommands: false,
  serverSelectionTimeoutMS: 5_000,
} as const;
