/**
 * Shim de pruebas para `@nestjs/mongoose`.
 *
 * `@nestjs/mongoose@12` se publica como ESM puro (`"type": "module"`), que
 * Jest no puede cargar con su transformador de CommonJS (ts-jest). En
 * produccion esto no es un problema: Node 24 resuelve `require()` de un
 * paquete ESM de forma nativa (por eso el servicio corre bien en Docker).
 *
 * Los repositorios Mongo y el consumidor de RabbitMQ solo usan
 * `InjectModel`/`InjectConnection` como decoradores de parametro, y solo
 * importan en las pruebas que instancian la clase directamente con
 * `new Repositorio(modelo)` (sin el contenedor de NestJS, que es quien de
 * verdad necesita esa metadata). Por eso un decorador que no hace nada es
 * un reemplazo seguro aqui: Jest lo usa en vez del paquete real gracias a
 * `moduleNameMapper` en la configuracion de `jest` de `package.json`.
 */
export function InjectModel(_nombreModelo: string): ParameterDecorator {
  return () => {
    // No-op: fuera del contenedor de NestJS no hay nada que inyectar.
  };
}

/** Shim de `InjectConnection` (ver documentacion del modulo). */
export function InjectConnection(_nombreConexion?: string): ParameterDecorator {
  return () => {
    // No-op: fuera del contenedor de NestJS no hay nada que inyectar.
  };
}
