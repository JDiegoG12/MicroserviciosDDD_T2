# servicio-catalogo

Microservicio del **Catálogo Académico** (dueño: P3 · Python 3.12 · FastAPI · PostgreSQL).

Administra la jerarquía Competencia → Tema → Subtema y publica el servicio gRPC `CatalogoAcademico.ValidarClasificacion`, que los demás servicios usan para comprobar que una clasificación existe y es coherente. Al arrancar con la base vacía carga el catálogo semilla de CONTRATOS.md §4.3, en una sola transacción.

- REST: `http://localhost:8082/api/v1` · Swagger: `/docs` · OpenAPI: `/openapi.json` · Salud: `/salud`
- gRPC: `localhost:50051` (sin TLS, con reflexión). Ejemplos con `grpcurl` en el [README raíz](../README.md#probar-grpc-con-grpcurl).
- Contratos: [CONTRATOS.md](../CONTRATOS.md) secciones 5, 6, 8.2 y 11.2.
- Dudas: [DUDAS.md](DUDAS.md) (hoy, ninguna abierta).

## Estado

| Etapa | Alcance | Estado |
|---|---|---|
| 1 | Dominio + aplicación + pruebas unitarias (solo librería estándar) | Hecha |
| 2 | PostgreSQL (SQLAlchemy asíncrono + Alembic), REST, servidor gRPC, Dockerfile | Hecha |

## Estructura

```
servicio-catalogo/
├── Dockerfile · pyproject.toml · DUDAS.md
├── scripts/generar_grpc.py         genera el código gRPC desde /contratos/proto
├── catalogo/
│   ├── principal.py                arranca REST + gRPC en un solo proceso asyncio
│   ├── dominio/                    agregado Competencia, value objects, eventos, servicios de dominio
│   ├── aplicacion/                 casos de uso, puertos, DTOs, datos semilla
│   ├── infraestructura/            adaptadores de salida
│   │   ├── base_datos/             modelos SQLAlchemy, mapeador, CompetenciaRepositorioSqlAlchemy,
│   │   │                           ejecutor transaccional
│   │   ├── migraciones/            Alembic (0001: tablas, llaves foráneas e índices únicos)
│   │   ├── inicializacion.py       migraciones + siembra con reintentos
│   │   ├── publicador_eventos_log_adaptador.py   los eventos de dominio solo se escriben en el log
│   │   ├── correlacion.py · registro_log.py · configuracion.py
│   └── interfaces/                 adaptadores de entrada
│       ├── rest/                   routers FastAPI, DTOs camelCase, errores problem+json, middleware
│       └── grpc/                   servicer y servidor grpc.aio (generado/ no se versiona)
└── tests/                          dominio/ · aplicacion/ · interfaces/ (API y gRPC) · integracion/
```

**Cómo encajan los casos de uso (síncronos) con SQLAlchemy asíncrono.** Los casos de uso de la etapa 1 son síncronos. `EjecutorTransaccionalSqlAlchemy` abre una `AsyncSession`, ejecuta el caso de uso con `AsyncSession.run_sync` (que le da un repositorio de estilo síncrono sobre el motor asíncrono) y hace `COMMIT` o `ROLLBACK`. Así cada caso de uso es una transacción, la siembra es todo o nada y el bucle de eventos nunca se bloquea.

## Entorno virtual y código gRPC

Requiere Python 3.12 o superior. Desde la carpeta `servicio-catalogo/`:

**Windows (PowerShell)**

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
python scripts/generar_grpc.py
```

**Linux / macOS**

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
python scripts/generar_grpc.py
```

`scripts/generar_grpc.py` compila `contratos/proto/catalogo/v1/catalogo_academico.proto` con `grpcio-tools` y escribe los módulos en `catalogo/interfaces/grpc/generado/`. **Esa carpeta no se versiona** (CONTRATOS.md 6; está en el `.gitignore` del servicio). El script corrige los imports generados (`from catalogo.v1 import ...`) para que funcionen dentro del paquete. Se ejecuta en el Dockerfile y, si falta, `pytest` lo ejecuta solo antes de las pruebas. Hay que volver a ejecutarlo si cambia el `.proto`.

## Pruebas

```bash
pytest                               # todo: unitarias, API, gRPC e integración
pytest -m "not integracion"          # sin Docker
pytest --cov=catalogo --cov-report=term-missing
```

| Carpeta | Qué prueba | Necesita Docker |
|---|---|---|
| `tests/dominio`, `tests/aplicacion` | Invariantes INV-22 a INV-24, casos de uso con repositorio en memoria | No |
| `tests/interfaces` | API REST completa (401, 403, 400, problem+json, mapa 5.3, Swagger/OpenAPI) y servidor gRPC en proceso con un *stub* generado | No |
| `tests/integracion` | Repositorio, migraciones, índices únicos, llaves foráneas, siembra y **arranque con PostgreSQL detenido** contra **PostgreSQL 16 real** con Testcontainers | **Sí** |

Si Docker no está disponible, las pruebas de integración se **omiten** con un mensaje claro (no se simulan con dobles). En Windows el servicio y las pruebas usan `SelectorEventLoop`, porque `psycopg` asíncrono no funciona con el bucle por defecto de Windows (en Linux no hace falta).

## Ejecutar con Docker

El contexto de construcción es la **raíz del repositorio** (CONTRATOS.md 2). Desde la raíz:

```bash
cp .env.example .env
docker compose --profile servicios build servicio-catalogo
docker compose --profile servicios up -d servicio-catalogo   # levanta también bd-catalogo
docker compose ps                                            # espera "healthy"
curl http://localhost:8082/salud
docker compose stop servicio-catalogo
```

## Arranque (CONTRATOS 9.3.6)

Ningún servidor depende de la base de datos para iniciar:

1. FastAPI y el servidor `grpc.aio` arrancan **sin esperar** a PostgreSQL. `/salud`, `/docs` y `/openapi.json` responden 200 desde el primer momento.
2. Una tarea de fondo (`asyncio`) aplica las **migraciones de Alembic** y **después** ejecuta `SembrarCatalogoCasoUso` en **una sola transacción** (todo o nada). Reintenta con espera progresiva (1, 2, 4... hasta un máximo de 30 s entre intentos) y escribe cada intento y su resultado en el log. Al terminar marca `EstadoInicializacion.catalogo_listo`.
3. Mientras `catalogo_listo` sea falso, o si la base de datos se cae después, REST responde **503** `BASE_DE_DATOS_NO_DISPONIBLE` (*problem+json*) y gRPC `UNAVAILABLE` (mensaje en español). Un error inesperado en gRPC es `INTERNAL`, sin detalles internos.
4. Un segundo arranque sobre la misma base no duplica la siembra.

Líneas clave del log: `Inicialización de la base de datos, intento N`, `Intento N fallido: la base de datos aún no está lista`, `Running upgrade -> 0001` y `Catálogo listo tras N intento(s): ... catálogo sembrado`.

Si un servidor no puede arrancar (por ejemplo, un puerto ocupado) el proceso termina con un error claro y código 1. La validación del esquema se comprueba en las pruebas de integración (`tests/integracion`), no al arrancar.

## Errores de protocolo

Siempre en `application/problem+json`, con `idCorrelacion` (CONTRATOS 5.3, v1.10): ruta inexistente → 404 `RECURSO_NO_ENCONTRADO`; método no permitido → 405 `METODO_NO_PERMITIDO`; cuerpo que no es JSON (por ejemplo `Content-Type: text/plain`) → 415 `TIPO_DE_CONTENIDO_NO_SOPORTADO`. Un `X-Id-Correlacion` con formato inválido nunca es error: se genera uno nuevo y se registra un aviso.

## Variables de entorno (CONTRATOS.md 9.2)

| Variable | Valor por defecto local |
|---|---|
| `CATALOGO_PUERTO_HTTP` | `8082` |
| `CATALOGO_PUERTO_GRPC` | `50051` |
| `CATALOGO_BD_URL` | `postgresql+psycopg://catalogo:catalogo@localhost:5434/catalogo` |

Ejecución local desde el IDE (con `bd-catalogo` en Docker): `python -m catalogo.principal`.

## Endpoints (CONTRATOS.md 8.2)

Todos bajo `/api/v1`, con los encabezados `X-Usuario-Id`, `X-Roles` y, opcionalmente, `X-Id-Correlacion`. Errores en `application/problem+json`.

| Método y ruta | Rol | Éxito | Errores propios |
|---|---|---|---|
| `POST /competencias` | `ADMINISTRADOR` | 201 + `Location` | 409 `NOMBRE_DUPLICADO` |
| `GET /competencias` | cualquiera | 200 lista | — |
| `GET /competencias/{competenciaId}` | cualquiera | 200 | 404 `COMPETENCIA_NO_ENCONTRADA` |
| `PUT /competencias/{competenciaId}` | `ADMINISTRADOR` | 200 | 404, 409 |
| `POST /competencias/{competenciaId}/temas` | `ADMINISTRADOR` | 201 | 404, 409 |
| `PUT /competencias/{competenciaId}/temas/{temaId}` | `ADMINISTRADOR` | 200 | 404 `TEMA_NO_ENCONTRADO`, 409 |
| `POST /competencias/{competenciaId}/temas/{temaId}/subtemas` | `ADMINISTRADOR` | 201 | 404, 409 |
| `PUT /competencias/{competenciaId}/temas/{temaId}/subtemas/{subtemaId}` | `ADMINISTRADOR` | 200 | 404 `SUBTEMA_NO_ENCONTRADO`, 409 |

`Location` de los `POST` de temas y subtemas: apunta a la competencia (`/api/v1/competencias/{competenciaId}`), porque temas y subtemas no tienen un `GET` propio (CONTRATOS 8.2).

Además, en todos: 400 `SOLICITUD_INVALIDA`, 401 `NO_AUTENTICADO`, 503 `BASE_DE_DATOS_NO_DISPONIBLE`, 500 `ERROR_INTERNO` y, en las escrituras, 403 `ACCESO_DENEGADO`. No hay `DELETE`.

## Casos de uso

| Caso de uso | Endpoint | Rol |
|---|---|---|
| `CrearCompetenciaCasoUso` | `POST /competencias` | `ADMINISTRADOR` |
| `RenombrarCompetenciaCasoUso` | `PUT /competencias/{competenciaId}` | `ADMINISTRADOR` |
| `AgregarTemaCasoUso` | `POST /competencias/{competenciaId}/temas` | `ADMINISTRADOR` |
| `RenombrarTemaCasoUso` | `PUT .../temas/{temaId}` | `ADMINISTRADOR` |
| `AgregarSubtemaCasoUso` | `POST .../temas/{temaId}/subtemas` | `ADMINISTRADOR` |
| `RenombrarSubtemaCasoUso` | `PUT .../subtemas/{subtemaId}` | `ADMINISTRADOR` |
| `ListarCompetenciasCasoUso` | `GET /competencias` | cualquiera |
| `ObtenerCompetenciaCasoUso` | `GET /competencias/{competenciaId}` | cualquiera |
| `ValidarClasificacionCasoUso` | gRPC `CatalogoAcademico.ValidarClasificacion` | ninguno (llamada interna) |
| `SembrarCatalogoCasoUso` | al arrancar el servicio | ninguno |
