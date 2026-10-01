# Banco de Preguntas Saber Pro · Taller 2

Sistema de microservicios para construir, revisar y publicar preguntas tipo Saber Pro y armar simulacros con ellas. Implementa en código los contextos delimitados diseñados en el Taller 1. Las reglas y los contratos están en [CONTRATOS.md](CONTRATOS.md).

## Microservicios

| Servicio | Qué hace | Tecnología | Puertos |
|---|---|---|---|
| `servicio-editorial` | Gestión Editorial de Preguntas: crea preguntas, las valida, coordina la revisión por pares, las publica y archiva. Valida la clasificación con el catálogo por gRPC y emite los eventos `PreguntaPublicada` y `PreguntaArchivada`. | Java 21 · Spring Boot 3 · PostgreSQL | 8081 |
| `servicio-catalogo` | Catálogo Académico: competencias, temas y subtemas. Publica el servicio gRPC `CatalogoAcademico.ValidarClasificacion`. | Python 3.12 · FastAPI · PostgreSQL | 8082 (REST) · 50051 (gRPC) |
| `servicio-evaluacion` | Evaluación y Simulacros: guarda copias de las preguntas publicadas (por eventos), arma simulacros, recibe intentos y los califica; emite `IntentoDeSimulacroCalificado`. | TypeScript · Node 20 · NestJS 10 · MongoDB | 8083 |

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

> TODO: documentar las llamadas con `grpcurl` a `CatalogoAcademico.ValidarClasificacion` (por ejemplo `list`, `describe` y una terna válida y otra inválida del catálogo semilla).

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
