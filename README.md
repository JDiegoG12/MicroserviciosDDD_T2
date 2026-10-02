# Banco de Preguntas Saber Pro · Taller 2

Sistema de microservicios para construir, revisar y publicar preguntas tipo Saber Pro y armar simulacros con ellas. Implementa en código los contextos delimitados diseñados en el Taller 1. Las reglas y los contratos están en [CONTRATOS.md](CONTRATOS.md).

## Microservicios

| Servicio | Qué hace | Tecnología | Puertos |
|---|---|---|---|
| `servicio-editorial` | Gestión Editorial de Preguntas: crea preguntas, las valida, coordina la revisión por pares, las publica y archiva. Valida la clasificación con el catálogo por gRPC y emite los eventos `PreguntaPublicada` y `PreguntaArchivada`. | Java 21 · Spring Boot 4.1.1 · PostgreSQL | 8081 |
| `servicio-catalogo` | Catálogo Académico: competencias, temas y subtemas. Publica el servicio gRPC `CatalogoAcademico.ValidarClasificacion`. | Python 3.12 · FastAPI · PostgreSQL | 8082 (REST) · 50051 (gRPC) |
| `servicio-evaluacion` | Evaluación y Simulacros: guarda copias de las preguntas publicadas (por eventos), arma simulacros, recibe intentos y los califica; emite `IntentoDeSimulacroCalificado`. | TypeScript · Node 24 · NestJS 11 · MongoDB | 8083 |

La comunicación entre servicios es por gRPC (Editorial → Catálogo) y por eventos en RabbitMQ (Editorial → Evaluación). El diagrama está en [docs/arquitectura](docs/arquitectura/README.md).

## Requisitos

- [Docker](https://docs.docker.com/get-docker/) con **Docker Compose v2** (`docker compose version`).
- Puertos libres en el equipo: 5672, 15672, 5433, 5434, 27017 y, cuando existan los servicios, 8081, 8082, 8083 y 50051.

## Levantar la infraestructura

```bash
cp .env.example .env
docker compose up -d
```

Esto levanta solo la infraestructura: `rabbitmq`, `bd-editorial`, `bd-catalogo` y `bd-evaluacion`. Para ver su estado (deben quedar `healthy`):

```bash
docker compose ps
```

Para detenerla conservando los datos: `docker compose down`. Para borrar también los datos: `docker compose down -v`.

## Levantar todo (cuando existan los servicios)

Los microservicios están en el perfil `servicios` y se construyen desde la raíz del repositorio:

```bash
docker compose --profile servicios up -d --build
```

## URLs útiles

| Qué | URL |
|---|---|
| Consola de RabbitMQ (usuario y clave en `.env`: `banco` / `banco123`) | http://localhost:15672 |
| Swagger de `servicio-editorial` | http://localhost:8081/docs |
| Swagger de `servicio-catalogo` | http://localhost:8082/docs |
| Swagger de `servicio-evaluacion` | http://localhost:8083/docs |
| Salud de `servicio-editorial` | http://localhost:8081/salud |
| Salud de `servicio-catalogo` | http://localhost:8082/salud |
| Salud de `servicio-evaluacion` | http://localhost:8083/salud |
| gRPC de `servicio-catalogo` (con reflexión) | `localhost:50051` |

Los eventos se pueden observar en la consola de RabbitMQ, cola `evaluacion.preguntas`.

## Validar los contratos

Los contratos compartidos están en [contratos/](contratos/README.md). Para comprobarlos solo se necesita Docker:

```bash
# Linux, macOS o Git Bash
./scripts/validar-contratos.sh

# Windows (PowerShell)
powershell -ExecutionPolicy Bypass -File .\scripts\validar-contratos.ps1
```

El script compila `catalogo_academico.proto` con `grpcio-tools` y valida cada ejemplo de `contratos/eventos/ejemplos/` contra su JSON Schema, todo dentro de un contenedor `python:3.12-slim`. Imprime OK o ERROR por archivo y termina con código distinto de 0 si algo falla.

## Pruebas con Postman

> TODO: describir cómo importar `postman/BancoPreguntas.postman_collection.json` y `postman/local.postman_environment.json` y el orden de ejecución de las carpetas `01 Catálogo`, `02 Editorial`, `03 Evaluación` y `04 Flujo completo`.

## Probar gRPC con grpcurl

El servidor gRPC de `servicio-catalogo` (puerto `50051`, sin TLS) tiene la **reflexión activada**, así que `grpcurl` no necesita el archivo `.proto`. Primero levanta el servicio:

```bash
docker compose --profile servicios up -d servicio-catalogo
```

Hay dos formas de ejecutar `grpcurl`. Las dos usan los identificadores de la tabla 4.3 de [CONTRATOS.md](CONTRATOS.md).

**A. Desde el equipo** (con [grpcurl](https://github.com/fullstorydev/grpcurl) instalado, contra el puerto publicado `localhost:50051`):

```bash
# Servicios y método disponibles
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext localhost:50051 list bancopreguntas.catalogo.v1.CatalogoAcademico
grpcurl -plaintext localhost:50051 describe bancopreguntas.catalogo.v1.CatalogoAcademico

# Terna VÁLIDA (competencia …101, tema …201, subtema …301) -> "valida": true
grpcurl -plaintext -emit-defaults \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"22222222-2222-4222-8222-000000000201","subtema_id":"22222222-2222-4222-8222-000000000301"}' \
  localhost:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion

# Terna INVÁLIDA: el subtema …303 es del tema …202, no del …201
# -> "motivo": "MOTIVO_RECHAZO_SUBTEMA_NO_PERTENECE_A_TEMA" (respuesta OK, no es un error gRPC)
grpcurl -plaintext \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"22222222-2222-4222-8222-000000000201","subtema_id":"22222222-2222-4222-8222-000000000303"}' \
  localhost:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion

# Id mal formado -> estado gRPC InvalidArgument
grpcurl -plaintext \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"no-es-uuid","subtema_id":"22222222-2222-4222-8222-000000000301"}' \
  localhost:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion

# Con correlación: aparece en el log del servicio (idCorrelacion=...)
grpcurl -plaintext -H 'x-id-correlacion: 0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b' \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"22222222-2222-4222-8222-000000000201","subtema_id":"22222222-2222-4222-8222-000000000302"}' \
  localhost:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion
```

**B. Desde la red de Docker** (sin instalar nada: se usa la imagen `fullstorydev/grpcurl` y el host `servicio-catalogo`). En Git Bash de Windows agrega antes `export MSYS_NO_PATHCONV=1`.

```bash
docker run --rm --network red-banco-preguntas fullstorydev/grpcurl -plaintext servicio-catalogo:50051 list

docker run --rm --network red-banco-preguntas fullstorydev/grpcurl -plaintext servicio-catalogo:50051 \
  describe bancopreguntas.catalogo.v1.CatalogoAcademico

# Terna válida
docker run --rm --network red-banco-preguntas fullstorydev/grpcurl -plaintext -emit-defaults \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"22222222-2222-4222-8222-000000000201","subtema_id":"22222222-2222-4222-8222-000000000301"}' \
  servicio-catalogo:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion

# Subtema de otro tema -> MOTIVO_RECHAZO_SUBTEMA_NO_PERTENECE_A_TEMA
docker run --rm --network red-banco-preguntas fullstorydev/grpcurl -plaintext \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"22222222-2222-4222-8222-000000000201","subtema_id":"22222222-2222-4222-8222-000000000303"}' \
  servicio-catalogo:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion

# Id mal formado -> Code: InvalidArgument
docker run --rm --network red-banco-preguntas fullstorydev/grpcurl -plaintext \
  -d '{"competencia_id":"22222222-2222-4222-8222-000000000101","tema_id":"no-es-uuid","subtema_id":"22222222-2222-4222-8222-000000000301"}' \
  servicio-catalogo:50051 bancopreguntas.catalogo.v1.CatalogoAcademico/ValidarClasificacion
```

Los otros motivos de rechazo se obtienen cambiando un identificador (usa uno que no exista, por ejemplo `99999999-9999-4999-8999-999999999999`):

| Terna enviada | `motivo` |
|---|---|
| competencia inexistente | `MOTIVO_RECHAZO_COMPETENCIA_INEXISTENTE` |
| competencia `…101`, tema inexistente | `MOTIVO_RECHAZO_TEMA_INEXISTENTE` |
| competencia `…101`, tema `…203` (de la `…102`), subtema `…304` | `MOTIVO_RECHAZO_TEMA_NO_PERTENECE_A_COMPETENCIA` |
| competencia `…101`, tema `…201`, subtema inexistente | `MOTIVO_RECHAZO_SUBTEMA_INEXISTENTE` |
| competencia `…101`, tema `…201`, subtema `…303` (del tema `…202`) | `MOTIVO_RECHAZO_SUBTEMA_NO_PERTENECE_A_TEMA` |

> `grpcurl` no imprime los campos con valor por defecto: una terna válida muestra `"valida": true` pero omite `"motivo": "MOTIVO_RECHAZO_NINGUNO"`. Agrega `-emit-defaults` para verlo.
>
> En Postman: crea una petición gRPC a `localhost:50051` y usa «Using server reflection» para cargar `CatalogoAcademico/ValidarClasificacion`.

## Estructura del repositorio

```
├── CONTRATOS.md            fuente única de verdad
├── docker-compose.yml      infraestructura y servicios
├── .env.example            variables por defecto
├── contratos/              proto gRPC y esquemas de eventos
├── docs/arquitectura/      diagrama de arquitectura (entregable)
├── postman/                colección y entorno de Postman
├── scripts/                validación de contratos
├── servicio-editorial/     P1
├── servicio-catalogo/      P3
└── servicio-evaluacion/    P2
```

`/contratos`, `CONTRATOS.md`, `docker-compose.yml`, `/postman` y `/scripts` solo se cambian por PR aprobado por al menos otro integrante (no se exige la aprobación de los tres).
