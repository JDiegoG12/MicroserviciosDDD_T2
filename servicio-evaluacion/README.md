# servicio-evaluacion

Microservicio de **Evaluación y Simulacros** (dueño: P2 · TypeScript · Node 24 (v24.20.0) · NestJS 11 · MongoDB).

Mantiene una copia local de las preguntas publicadas (a partir de los eventos `PreguntaPublicada` y `PreguntaArchivada`), permite a los docentes definir simulacros y a los estudiantes presentarlos; al calificar un intento publica `IntentoDeSimulacroCalificado`.

- REST: `http://localhost:8083/api/v1` · Swagger: `/docs` (JSON en `/openapi.json`) · Salud: `/salud`
- Contratos: [CONTRATOS.md](../CONTRATOS.md) secciones 4, 5, 7, 8.3, 9 y 11.3, y [MODELO-DOMINIO.md](../MODELO-DOMINIO.md).

## Estado actual

**Etapas 1 y 2 implementadas**: dominio, aplicación, persistencia MongoDB, consumidor y publicador de RabbitMQ, API REST con Swagger, y Dockerfile. Queda para la etapa 3 lo que el equipo defina a continuación (por ejemplo, Seguimiento Académico).

## Notas tecnicas

- **Mongoose 8.24.4, no la serie 9.x**: aunque Mongoose 9 ya soporta MongoDB 7 y Node 24, `@nestjs/mongoose` (de donde viene `MongooseModule`, usado en todo el servicio) todavia tiene abierto el seguimiento de su soporte a Mongoose 9 (ver `nestjs/mongoose#2665` en GitHub), aunque su `peerDependencies` ya declara `^9.0.0`. Se fija entonces la ultima version estable de la serie 8.x, la combinacion probada con `mongo:7` (CONTRATOS.md 9.2) y con `@nestjs/mongoose ^12`.
- **Arranque sin base de datos (CONTRATOS.md 9.3.6)**: `MongooseModule.forRoot(...)` usa `lazyConnection: true` para que Nest no espere la conexion inicial al arrancar (el servicio queda `healthy` aunque Mongo aun no responda) y `bufferCommands: false` para que una operacion lanzada sin conexion falle de inmediato con 503 `BASE_DE_DATOS_NO_DISPONIBLE`, en vez de quedar encolada en silencio. El consumidor de RabbitMQ (`PreguntaEditorialConsumidor`) registra ademas un listener de `error` sobre la `Connection` de Mongoose: sin el, un fallo de la conexion inicial (`MongoServerSelectionError`) termina en una promesa rechazada sin capturar que tumba el proceso (ver `mongoose/lib/connection.js`, issue `Automattic/mongoose#14377`).

## Ejecutar localmente (sin Docker)

```bash
npm install
cp ../.env.example ../.env   # si no existe ya, en la raiz del repo
npm run start:dev            # necesita Mongo en localhost:27017 y RabbitMQ en localhost:5672
```

## Ejecutar con Docker Compose

```bash
# desde la raiz del repositorio
docker compose --profile servicios build servicio-evaluacion
docker compose --profile servicios up -d servicio-evaluacion   # levanta tambien bd-evaluacion y rabbitmq
docker compose stop servicio-evaluacion                        # detiene solo este servicio
```

## Pruebas

```bash
npm install
npm run typecheck     # solo compilacion (tsc --noEmit)
npm run build         # compila a dist/ (nest build)

npm run test:unitarias   # dominio + aplicacion, con dobles en memoria (test/dobles)
npm run test:api         # API completa (NestJS + supertest) con dobles, sin Mongo ni RabbitMQ reales
npm run test:integracion # repositorios Mongo y mensajeria RabbitMQ con Testcontainers (requiere Docker)
npm test                 # las tres suites anteriores juntas
npm run test:cov         # con reporte de cobertura
```

`test/dominio` y `test/aplicacion` no levantan nada externo. `test/api` monta la app de NestJS completa (guardias, filtros, pipes) con los puertos de salida atados a dobles en memoria. `test/integracion` sí necesita Docker: levanta un `mongo:7` y un `rabbitmq:3.13-management` reales con Testcontainers.

## Publicar eventos de ejemplo en RabbitMQ

Con el servicio y RabbitMQ corriendo (Docker o local), para tener preguntas `PUBLICADA` con las que probar `POST /simulacros`:

```bash
# bash / Git Bash
./scripts/publicar-ejemplos.sh

# PowerShell
./scripts/publicar-ejemplos.ps1
```

Publica los ejemplos de `/contratos/eventos/ejemplos/` (incluido el de campo extra, que demuestra el lector tolerante) más cuatro variaciones de `PreguntaPublicada` con `idEvento` y `preguntaId` nuevos. Variables de entorno opcionales: `RABBITMQ_HOST`, `RABBITMQ_MGMT_PUERTO`, `RABBITMQ_USUARIO`, `RABBITMQ_CLAVE` (valores por defecto de CONTRATOS.md 9.2).
