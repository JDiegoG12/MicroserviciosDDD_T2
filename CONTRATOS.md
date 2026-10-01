# CONTRATOS.md · Banco de Preguntas Saber Pro (Taller 2)

> **Fuente única de verdad** para los tres microservicios. Si algo de este documento choca con el código, **manda este documento**.
> Cambiar cualquier contrato de las secciones 5 a 8 se hace con un PR que modifique este archivo y la carpeta `/contratos` en el mismo commit, avisando al equipo. Basta la aprobación de **otro** integrante.
>
> **Versión 1.1** (2-oct-2026). Cambios respecto a la 1.0 en la sección 13.

---

## 0. Cómo usar este documento (instrucciones para Claude Code)

1. Lee **siempre** las secciones 1 a 10 (reglas comunes y contratos).
2. Lee **solo** la sección de tu servicio en la parte 11 (11.1 Editorial, 11.2 Catálogo, 11.3 Evaluación).
3. **No** leas ni modifiques el código de otro servicio: todo lo que necesitas de él está aquí.
4. **No** modifiques `/contratos` ni este archivo por iniciativa propia; si un contrato no alcanza, detente y repórtalo.
5. Los nombres entre `comillas de código` son **exactos**: respeta mayúsculas, guiones y puntos tal cual.

Cada servicio tendrá un `CLAUDE.md` propio con esta indicación: *"Lee /CONTRATOS.md (secciones 1–10 y la sección 11.x de este servicio) antes de escribir código."*

---

## 1. Visión general

| Servicio | Contexto (Taller 1) | Tecnología | Base de datos | Dueño (GitHub) | REST | gRPC |
|---|---|---|---|---|---|---|
| `servicio-editorial` | Gestión Editorial de Preguntas | Java 21 · Spring Boot 3 | PostgreSQL 16 (`bd-editorial`) | P1 `@juanvec06` | 8081 | cliente |
| `servicio-catalogo` | Catálogo Académico | Python 3.12 · FastAPI | PostgreSQL 16 (`bd-catalogo`) | P3 `@JuanDv1` | 8082 | servidor 50051 |
| `servicio-evaluacion` | Evaluación y Simulacros | TypeScript · Node 20 · NestJS 10 | MongoDB 7 (`bd-evaluacion`) | P2 `@JDiegoG12` | 8083 | — |
| `rabbitmq` | Broker de mensajes | RabbitMQ 3.13 (management) | — | P3 (compose) | 15672 (consola) | — |

```mermaid
flowchart LR
  PM(["Postman"])
  ED["servicio-editorial<br/>Java · Spring Boot<br/>:8081"]
  CA["servicio-catalogo<br/>Python · FastAPI<br/>:8082 REST · :50051 gRPC"]
  EV["servicio-evaluacion<br/>TypeScript · NestJS<br/>:8083"]
  EX1{{"exchange editorial.eventos<br/>(topic)"}}
  Q1[["cola evaluacion.preguntas"]]
  EX2{{"exchange evaluacion.eventos<br/>(topic)"}}
  DB1[("bd-editorial<br/>PostgreSQL")]
  DB2[("bd-catalogo<br/>PostgreSQL")]
  DB3[("bd-evaluacion<br/>MongoDB")]

  PM -- "REST /api/v1" --> ED
  PM -- "REST /api/v1" --> CA
  PM -- "REST /api/v1" --> EV
  ED -- "gRPC CatalogoAcademico.ValidarClasificacion" --> CA
  ED -- "pregunta.publicada<br/>pregunta.archivada" --> EX1
  EX1 --> Q1
  Q1 -- "consume" --> EV
  EV -- "intento.calificado<br/>(sin consumidor por ahora)" --> EX2
  ED --- DB1
  CA --- DB2
  EV --- DB3
```

**Comunicaciones exactas del sistema (no hay otras):**

| # | Tipo | Origen → Destino | Contrato | Sección |
|---|---|---|---|---|
| C1 | REST | Postman → cada servicio | APIs `/api/v1/...` | 8 |
| C2 | gRPC síncrono | `servicio-editorial` → `servicio-catalogo` | `CatalogoAcademico.ValidarClasificacion` | 6 |
| C3 | Evento asíncrono | `servicio-editorial` → RabbitMQ → `servicio-evaluacion` | `PreguntaPublicada`, `PreguntaArchivada` | 7 |
| C4 | Evento asíncrono | `servicio-evaluacion` → RabbitMQ | `IntentoDeSimulacroCalificado` (sin consumidor) | 7 |

Ningún servicio lee la base de datos de otro. Ningún servicio llama por REST a otro servicio.

---

## 2. Estructura del monorepo

```
/
├── CONTRATOS.md                  ← este archivo
├── README.md                     ← cómo ejecutar y probar todo (P3)
├── docker-compose.yml            ← P3
├── .env.example                  ← variables por defecto (sin secretos reales)
├── .github/CODEOWNERS            ← cada carpeta de servicio → su dueño; lo compartido → los tres (basta uno)
├── contratos/
│   ├── proto/catalogo/v1/catalogo_academico.proto      ← dueño P3
│   └── eventos/
│       ├── editorial/pregunta-publicada.v1.schema.json ← dueño P1
│       ├── editorial/pregunta-archivada.v1.schema.json ← dueño P1
│       ├── evaluacion/intento-calificado.v1.schema.json← dueño P2
│       └── ejemplos/*.json                             ← un ejemplo válido por evento
├── docs/arquitectura/            ← diagrama de arquitectura (entregable)
├── postman/
│   ├── BancoPreguntas.postman_collection.json
│   └── local.postman_environment.json
├── servicio-editorial/           ← P1 (incluye su Dockerfile y CLAUDE.md)
├── servicio-catalogo/            ← P3 (incluye su Dockerfile y CLAUDE.md)
└── servicio-evaluacion/          ← P2 (incluye su Dockerfile y CLAUDE.md)
```

**Regla de construcción Docker (fuente típica de conflicto):** el `.proto` vive fuera de las carpetas de servicio, así que **el contexto de construcción de las imágenes es la raíz del repositorio**:

```yaml
servicio-editorial:
  build:
    context: .
    dockerfile: servicio-editorial/Dockerfile
```

Dentro de cada Dockerfile las rutas se escriben relativas a la raíz (`COPY contratos/proto ./contratos/proto`, `COPY servicio-editorial/ ./`). Como el contexto es la raíz, Docker **solo lee el `.dockerignore` de la raíz** (los de las carpetas de servicio se ignoran). Por eso hay **un único `.dockerignore` en la raíz**, que excluye `**/node_modules`, `**/target`, `**/__pycache__`, `**/.venv`, `.git`, `.env`, `postman` y `docs`. No se crean `.dockerignore` por servicio ni `Dockerfile.dockerignore`. Si un dueño necesita excluir algo más, lo agrega al de la raíz con un PR.

---

## 3. Estándares de código comunes

### 3.1 Idioma y legibilidad
- **Todo en español**: clases, métodos, variables, paquetes, rutas, claves JSON, nombres de colas, mensajes de error, comentarios, documentación y commits.
- **Identificadores sin tildes ni ñ** (código, archivos, claves JSON, rutas, variables de entorno, enums): `Revision`, `tamano`, `anio`, `Calificacion`. Las tildes **sí** van en comentarios, documentación y textos para el usuario.
- Los nombres salen del **lenguaje ubicuo del Taller 1**: `Pregunta`, `PreguntaDirecta`, `OpcionDeRespuesta`, `ProcesoDeRevision`, `FormatoDeEvaluacion`, `Dictamen`, `Simulacro`, `IntentoDeSimulacro`, `Competencia`, `Tema`, `Subtema`, `NivelDeDificultad`. No inventar sinónimos (`Question`, `Exam`, `Item`, `Quiz` están prohibidos).
- Código que entienda cualquier integrante: métodos cortos, nombres que expliquen la intención, sin abreviaturas crípticas (`prg`, `tmp2`, `x`), sin "magia" ni metaprogramación innecesaria.
- Las reglas de negocio citan su origen en la documentación: `// INV-20: el dictamen aprueba solo si el porcentaje es estrictamente mayor a 70 %`.

### 3.2 Documentación obligatoria
| Lenguaje | Formato | Qué se documenta como mínimo |
|---|---|---|
| Java | **JavaDoc** | Toda clase pública y todo método público: propósito, `@param`, `@return`, `@throws`, regla del Taller 1 que implementa (RF/INV/D/CU). |
| Python | **Docstrings estilo Google (PEP 257)** | Todo módulo, clase y función pública: descripción, `Args:`, `Returns:`, `Raises:`. |
| TypeScript | **TSDoc** | Toda clase, interfaz, método y función exportados: descripción, `@param`, `@returns`, `@throws`. |

Además: cada endpoint REST documentado en Swagger/OpenAPI con descripción, ejemplos y códigos de error; el `.proto` comentado mensaje por mensaje.

### 3.3 Clean Architecture (igual en los tres servicios)

```mermaid
flowchart LR
  I["interfaces<br/>(adaptadores de entrada)<br/>controladores REST · servidor gRPC · consumidores RabbitMQ"]
  A["aplicacion<br/>casos de uso · puertos de entrada · puertos de salida · DTOs"]
  D["dominio<br/>agregados · entidades · value objects · servicios de dominio · eventos de dominio · interfaces de repositorio · excepciones de dominio"]
  F["infraestructura<br/>(adaptadores de salida)<br/>repositorios JPA/SQLAlchemy/Mongo · publicador RabbitMQ · cliente gRPC · configuración"]
  I --> A --> D
  F -. implementa puertos .-> A
  F -. implementa repositorios .-> D
```

Reglas:
1. **Las dependencias apuntan hacia el dominio.** `dominio` no importa nada de `aplicacion`, `infraestructura`, `interfaces` ni de frameworks (Spring, JPA, FastAPI, SQLAlchemy, NestJS, Mongoose, AMQP, gRPC).
2. **Entidades de dominio sin anotaciones de persistencia.** Los modelos de base de datos (`@Entity`, modelos SQLAlchemy, esquemas Mongoose) viven en `infraestructura` y se traducen con *mappers*.
3. **Controladores sin reglas de negocio**: validan forma, llaman a un caso de uso, traducen la respuesta.
4. **Un caso de uso = una clase** (nada de `PreguntaService` gigante).
5. **Un caso de uso modifica un solo agregado por transacción.**
6. **Los eventos de dominio** son objetos del dominio; la **traducción a mensaje de integración** (JSON de la sección 7) ocurre en `infraestructura`.
7. **Repositorios con métodos del negocio** (`buscarPublicadasPorCriterios`), no CRUD genérico expuesto.
8. **Nunca se borra físicamente** una Pregunta (RNF-16); no existen endpoints `DELETE` en el sistema.

### 3.4 Sufijos de nombres (iguales en los tres lenguajes)
| Elemento | Sufijo | Ejemplo |
|---|---|---|
| Caso de uso | `CasoUso` | `CrearPreguntaCasoUso` |
| Puerto de salida (interfaz) | `Puerto` | `CatalogoAcademicoPuerto`, `PublicadorEventosPuerto` |
| Repositorio (interfaz de dominio) | `Repositorio` | `PreguntaRepositorio` |
| Implementación de repositorio | tecnología al final | `PreguntaRepositorioJpa`, `CompetenciaRepositorioSqlAlchemy`, `SimulacroRepositorioMongo` |
| Adaptador de salida | `Adaptador` | `CatalogoAcademicoGrpcAdaptador`, `PublicadorEventosRabbitMqAdaptador` |
| Controlador REST | `Controlador` | `PreguntaControlador` |
| Consumidor de mensajes | `Consumidor` | `PreguntaPublicadaConsumidor` |
| Servicio de dominio | `Servicio` | `ResolutorDictamenServicio`, `CalificadorSimulacroServicio` |
| Excepción de dominio | `Excepcion` | `TransicionNoPermitidaExcepcion` |

Convenciones propias de cada lenguaje se respetan: Java y TS en `PascalCase`/`camelCase`; Python en `snake_case` para funciones, variables y módulos (`crear_pregunta_caso_uso.py` define `CrearPreguntaCasoUso`); archivos TS en `kebab-case` (`crear-pregunta.caso-uso.ts`).

### 3.5 Pruebas
- Pruebas unitarias **obligatorias** del dominio: cada invariante (INV-xx) tiene al menos una prueba que la viola y comprueba el rechazo. JUnit 5 · pytest · Jest.
- Las pruebas de dominio no levantan base de datos, broker ni servidor.

### 3.6 Git
- Ramas: `main` (siempre funcional) y `feature/<servicio>-<tema>` (ej. `feature/editorial-revision`).
- Commits convencionales en español: `feat(editorial): agrega caso de uso de publicación`, `fix(catalogo): ...`, `docs(contratos): ...`.
- Cada integrante solo modifica su carpeta. `/contratos`, `CONTRATOS.md`, `docker-compose.yml` y `/postman` se cambian por PR aprobado por **al menos otro integrante** (no se exige la aprobación de los tres).
- `CODEOWNERS`: `servicio-editorial/` → `@juanvec06`; `servicio-evaluacion/` → `@JDiegoG12`; `servicio-catalogo/` → `@JuanDv1`; lo compartido y la regla `*` → los tres, y basta la aprobación de cualquiera. No se configura protección de rama que exija tres aprobaciones.

---

## 4. Convenciones transversales de datos (el origen típico de los conflictos)

| Tema | Regla exacta |
|---|---|
| Identificadores | **UUID v4 en texto, minúsculas**, con guiones: `"3f2b6c1e-8a4d-4c2e-9b1a-1d2e3f4a5b6c"`. Los genera el servicio dueño del agregado. En Mongo el `_id` **es** el UUID en texto (nunca `ObjectId`). En PostgreSQL columnas tipo `uuid`. |
| Claves JSON | **camelCase** en REST y eventos (`preguntaId`, `competenciaId`, `fechaOcurrencia`). Python usa alias de Pydantic (`alias_generator=to_camel`, `populate_by_name=True`, respuestas `by_alias=True`). |
| Campos del `.proto` | `snake_case` (guía de estilo de Protobuf). El código generado los expone como `getCompetenciaId()` en Java y `competencia_id` en Python. |
| Enums | **MAYÚSCULAS_CON_GUION_BAJO, sin tildes**, serializados por nombre: `EN_CONSTRUCCION`, `ALTO`, `APROBATORIA`. |
| Fechas | **ISO-8601 en UTC con `Z`**: `"2026-10-01T15:30:00Z"`. Nunca epoch numérico, nunca hora local. Contenedores con `TZ=UTC`. Jackson con `WRITE_DATES_AS_TIMESTAMPS=false`. |
| Duraciones | Enteros en minutos con el sufijo en el nombre: `duracionMaximaMinutos`. |
| Puntajes | Número decimal de 0 a 100 con 2 decimales (`puntaje: 66.67`). |
| Letras de opción | `"A"`, `"B"`, `"C"`, `"D"` (mayúscula, un carácter). |
| Codificación | UTF-8 en todo (HTTP, base de datos, mensajes). |
| Campos desconocidos | Al **leer** (eventos o respuestas) se ignoran campos desconocidos (lector tolerante). Al **escribir** se envían exactamente los campos del contrato. |
| Nulos | Un campo opcional ausente se envía como `null`, no se omite, en eventos. En REST se permite omitir. |

### 4.1 Roles y encabezados de identidad
No hay servicio de identidad. Quien llama declara su identidad en encabezados HTTP (en producción lo haría un API Gateway).

| Encabezado | Obligatorio | Formato | Ejemplo |
|---|---|---|---|
| `X-Usuario-Id` | Sí (excepto `/salud` y `/docs`) | UUID | `11111111-1111-4111-8111-000000000002` |
| `X-Roles` | Sí | lista separada por comas, sin espacios significativos | `AUTOR,DOCENTE` |
| `X-Id-Correlacion` | No | UUID; si no llega, el servicio lo genera | — |

Roles válidos (exactos): `ADMINISTRADOR`, `AUTOR`, `REVISOR`, `DOCENTE`, `ESTUDIANTE`.
Falta `X-Usuario-Id` o `X-Roles` → **401** `NO_AUTENTICADO`. Rol insuficiente → **403** `ACCESO_DENEGADO`.
`X-Id-Correlacion` se propaga a: metadatos gRPC `x-id-correlacion`, campo `idCorrelacion` del evento, y todas las líneas de log.

### 4.2 Usuarios de prueba (identificadores fijos)
| Usuario | `X-Usuario-Id` | `X-Roles` |
|---|---|---|
| Administrador | `11111111-1111-4111-8111-000000000001` | `ADMINISTRADOR` |
| Autor | `11111111-1111-4111-8111-000000000002` | `AUTOR` |
| Revisor 1 | `11111111-1111-4111-8111-000000000003` | `REVISOR` |
| Revisor 2 | `11111111-1111-4111-8111-000000000004` | `REVISOR` |
| Revisor 3 | `11111111-1111-4111-8111-000000000007` | `REVISOR` |
| Docente | `11111111-1111-4111-8111-000000000005` | `DOCENTE` |
| Estudiante | `11111111-1111-4111-8111-000000000006` | `ESTUDIANTE` |

### 4.3 Catálogo semilla (lo carga `servicio-catalogo` al arrancar si la base está vacía)
| Tipo | Id | Nombre | Pertenece a |
|---|---|---|---|
| Competencia | `22222222-2222-4222-8222-000000000101` | Razonamiento cuantitativo | — |
| Competencia | `22222222-2222-4222-8222-000000000102` | Diseño de software | — |
| Tema | `22222222-2222-4222-8222-000000000201` | Estadística | …101 |
| Tema | `22222222-2222-4222-8222-000000000202` | Álgebra | …101 |
| Tema | `22222222-2222-4222-8222-000000000203` | Patrones de diseño | …102 |
| Tema | `22222222-2222-4222-8222-000000000204` | Arquitectura de software | …102 |
| Subtema | `22222222-2222-4222-8222-000000000301` | Medidas de tendencia central | …201 |
| Subtema | `22222222-2222-4222-8222-000000000302` | Probabilidad | …201 |
| Subtema | `22222222-2222-4222-8222-000000000303` | Ecuaciones lineales | …202 |
| Subtema | `22222222-2222-4222-8222-000000000304` | Patrones creacionales | …203 |
| Subtema | `22222222-2222-4222-8222-000000000305` | Microservicios | …204 |

Las pruebas de Editorial y Evaluación usan **estos** identificadores. Nadie más inventa identificadores de catálogo.

---

## 5. Contrato REST común

### 5.1 Reglas generales
- Prefijo: `/api/v1`. Recursos en **plural** y en `kebab-case`: `/preguntas`, `/procesos-revision`, `/competencias`.
- Sin verbos tipo `/getPreguntas` o `/publicarPregunta`. Las **transiciones del ciclo de vida** se exponen como sub-recursos sustantivados con `POST` (`/preguntas/{preguntaId}/publicacion`).
- `POST` que crea → **201** + encabezado `Location` + cuerpo con el recurso. Transición → **200** + recurso actualizado.
- `Content-Type: application/json; charset=utf-8`.
- Paginación: parámetros `pagina` (desde 0) y `tamano` (por defecto 20, máximo 100). Respuesta:
  ```json
  { "contenido": [], "pagina": 0, "tamano": 20, "totalElementos": 0, "totalPaginas": 0 }
  ```
- Swagger UI en `GET /docs` y OpenAPI JSON en `GET /openapi.json` (en los tres servicios).
- Salud: `GET /salud` (fuera de `/api/v1`, sin encabezados) → `200 {"estado":"OK","servicio":"servicio-editorial"}`.

### 5.2 Formato de error único (RFC 7807, `application/problem+json`)
```json
{
  "type": "https://banco-preguntas/errores/TRANSICION_NO_PERMITIDA",
  "title": "Transición de estado no permitida",
  "status": 409,
  "detail": "La pregunta está en PUBLICADA y no puede pasar a EN_REVISION.",
  "instance": "/api/v1/preguntas/5c0e.../publicacion",
  "codigo": "TRANSICION_NO_PERMITIDA",
  "idCorrelacion": "9a1b...",
  "errores": [ { "campo": "opciones", "mensaje": "Debe haber exactamente cuatro opciones" } ]
}
```

### 5.3 Mapa de códigos HTTP (igual en los tres servicios)
| HTTP | Cuándo | Ejemplos de `codigo` |
|---|---|---|
| 400 | La solicitud está mal formada (JSON inválido, tipo incorrecto, UUID mal escrito, campo obligatorio ausente) | `SOLICITUD_INVALIDA` |
| 401 | Faltan encabezados de identidad | `NO_AUTENTICADO` |
| 403 | Rol insuficiente o el usuario no es el dueño | `ACCESO_DENEGADO`, `REVISOR_NO_ASIGNADO` |
| 404 | El recurso no existe | `PREGUNTA_NO_ENCONTRADA`, `COMPETENCIA_NO_ENCONTRADA` |
| 409 | El **estado actual** no permite la operación o hay duplicado | `TRANSICION_NO_PERMITIDA`, `PREGUNTA_NO_EDITABLE`, `NOMBRE_DUPLICADO`, `INTENTO_FINALIZADO` |
| 422 | Datos bien formados que **violan una regla de negocio o invariante** | `CLASIFICACION_INVALIDA`, `REVISORES_INSUFICIENTES`, `PREGUNTAS_INSUFICIENTES` |
| 503 | Una dependencia externa no responde | `CATALOGO_NO_DISPONIBLE` |
| 500 | Error inesperado (nunca exponer trazas) | `ERROR_INTERNO` |

---

## 6. Contrato gRPC (C2): `CatalogoAcademico`

- Dueño: **P3** (`servicio-catalogo` es el servidor, *Open Host Service*). Consumidor: `servicio-editorial`.
- Archivo: `/contratos/proto/catalogo/v1/catalogo_academico.proto` (exactamente este contenido).
- Dirección en Docker: `servicio-catalogo:50051` · en el equipo local: `localhost:50051`. Sin TLS (red interna).
- El servidor activa **reflexión gRPC** (para probar con Postman/grpcurl).

```proto
syntax = "proto3";

package bancopreguntas.catalogo.v1;

option java_multiple_files = true;
option java_package = "co.edu.unicauca.bancopreguntas.contratos.catalogo.v1";
option java_outer_classname = "CatalogoAcademicoProto";

// Servicio publicado (Open Host Service) del Catálogo Académico.
// Su lenguaje publicado (Published Language) es este archivo.
service CatalogoAcademico {
  // Verifica que la terna Competencia/Tema/Subtema exista y sea coherente
  // (el Tema pertenece a la Competencia y el Subtema al Tema). INV-07, D-13.
  rpc ValidarClasificacion (ValidarClasificacionSolicitud) returns (ValidarClasificacionRespuesta);
}

// Terna de identificadores (UUID en texto) que clasifica una Pregunta.
message ValidarClasificacionSolicitud {
  string competencia_id = 1;
  string tema_id = 2;
  string subtema_id = 3;
}

// Resultado de la validación. Una clasificación inválida NO es un error gRPC:
// se responde OK con valida = false y el motivo.
message ValidarClasificacionRespuesta {
  bool valida = 1;
  MotivoRechazo motivo = 2;   // MOTIVO_RECHAZO_NINGUNO cuando valida = true
  string detalle = 3;         // Texto legible en español para mostrar al usuario
}

// Primer motivo encontrado, en el orden en que se evalúa.
enum MotivoRechazo {
  MOTIVO_RECHAZO_NINGUNO = 0;
  MOTIVO_RECHAZO_COMPETENCIA_INEXISTENTE = 1;
  MOTIVO_RECHAZO_TEMA_INEXISTENTE = 2;
  MOTIVO_RECHAZO_TEMA_NO_PERTENECE_A_COMPETENCIA = 3;
  MOTIVO_RECHAZO_SUBTEMA_INEXISTENTE = 4;
  MOTIVO_RECHAZO_SUBTEMA_NO_PERTENECE_A_TEMA = 5;
}
```

**Comportamiento acordado:**
| Situación | Servidor (Catálogo) | Cliente (Editorial) |
|---|---|---|
| Terna válida | `OK`, `valida=true`, `motivo=MOTIVO_RECHAZO_NINGUNO` | continúa el caso de uso |
| Terna inválida | `OK`, `valida=false`, motivo según el orden del enum | HTTP **422** `CLASIFICACION_INVALIDA` con `detail = detalle` |
| Algún id no es UUID | estado gRPC `INVALID_ARGUMENT` | HTTP **400** `SOLICITUD_INVALIDA` |
| Catálogo caído o tarda más de **2 s** (deadline del cliente) | — | HTTP **503** `CATALOGO_NO_DISPONIBLE`; la pregunta **no** se guarda |

- La respuesta **no devuelve nombres** de competencias: los consumidores guardan solo identificadores (D-13).
- Metadato opcional `x-id-correlacion` en cada llamada.
- Generación de código: Java con `protobuf-maven-plugin` apuntando a `../contratos/proto` (o `/app/contratos/proto` dentro de Docker); Python con `grpcio-tools` durante la construcción, salida en `infraestructura`/`interfaces` (nunca se versiona código generado).

---

## 7. Contrato de eventos (C3 y C4): RabbitMQ

### 7.1 Topología exacta

```mermaid
flowchart LR
  ED["servicio-editorial<br/>(productor)"] -- "routing key pregunta.publicada" --> EX1{{"editorial.eventos<br/>topic · durable"}}
  ED -- "routing key pregunta.archivada" --> EX1
  EX1 -- "binding pregunta.publicada<br/>binding pregunta.archivada" --> Q1[["evaluacion.preguntas<br/>durable · DLX evaluacion.dlx"]]
  Q1 -- "consume (ack manual)" --> EV["servicio-evaluacion<br/>(consumidor)"]
  Q1 -. "mensaje rechazado" .-> DLX{{"evaluacion.dlx<br/>fanout · durable"}} --> DLQ[["evaluacion.preguntas.dlq<br/>durable"]]
  EV -- "routing key intento.calificado" --> EX2{{"evaluacion.eventos<br/>topic · durable"}}
```

| Recurso | Nombre exacto | Tipo y argumentos | Lo declara |
|---|---|---|---|
| Exchange | `editorial.eventos` | `topic`, `durable=true`, `autoDelete=false` | Editorial **y** Evaluación (declaración idempotente, **mismos argumentos**) |
| Exchange | `evaluacion.dlx` | `fanout`, `durable=true` | Evaluación |
| Cola | `evaluacion.preguntas` | `durable=true`, `exclusive=false`, `autoDelete=false`, argumento `x-dead-letter-exchange=evaluacion.dlx` | Evaluación |
| Cola | `evaluacion.preguntas.dlq` | `durable=true` | Evaluación |
| Binding | `editorial.eventos` → `evaluacion.preguntas` | claves `pregunta.publicada` y `pregunta.archivada` | Evaluación |
| Binding | `evaluacion.dlx` → `evaluacion.preguntas.dlq` | — | Evaluación |
| Exchange | `evaluacion.eventos` | `topic`, `durable=true`, `autoDelete=false` | Evaluación |

> Si dos servicios declaran el mismo recurso con argumentos distintos, RabbitMQ responde `PRECONDITION_FAILED` y el servicio no arranca. **Copiar los argumentos de esta tabla tal cual.**

Conexión: host `rabbitmq` (Docker) o `localhost` (local), puerto `5672`, vhost `/`, usuario y clave desde variables de entorno (sección 9).

### 7.2 Propiedades de cada mensaje
| Propiedad AMQP | Valor |
|---|---|
| `content_type` | `application/json` |
| `content_encoding` | `utf-8` |
| `delivery_mode` | `2` (persistente) |
| `message_id` | igual a `idEvento` |
| `type` | igual a `tipoEvento` |
| `timestamp` | instante de publicación |

### 7.3 Sobre común (todos los eventos)
```json
{
  "idEvento": "uuid v4",
  "tipoEvento": "PreguntaPublicada",
  "versionEvento": 1,
  "fechaOcurrencia": "2026-10-01T15:30:00Z",
  "origen": "servicio-editorial",
  "idCorrelacion": "uuid o null",
  "datos": { }
}
```

| Campo | Tipo | Regla |
|---|---|---|
| `idEvento` | UUID | Único por mensaje; base de la idempotencia del consumidor. |
| `tipoEvento` | texto | Uno de: `PreguntaPublicada`, `PreguntaArchivada`, `IntentoDeSimulacroCalificado`. |
| `versionEvento` | entero | Hoy siempre `1`. Un cambio incompatible crea la versión `2` y se acuerda entre los tres. |
| `fechaOcurrencia` | fecha ISO UTC | Cuándo ocurrió el hecho en el dominio (no cuándo se envió). |
| `origen` | texto | `servicio-editorial` o `servicio-evaluacion`. |
| `idCorrelacion` | UUID o `null` | El de la petición HTTP que originó el hecho. |
| `datos` | objeto | Específico de cada evento (abajo). |

### 7.4 `PreguntaPublicada` · routing key `pregunta.publicada` · productor Editorial

Lleva **todo** lo que Evaluación necesita para guardar su copia local de la pregunta como “ítem cerrado” (transferencia de estado por evento). **No** lleva autor, justificación, bibliografía, revisores ni trazabilidad: esos conceptos no existen en Evaluación.

```json
{
  "idEvento": "6d1f3a0c-3b2e-4f7a-9c1d-2e3f4a5b6c7d",
  "tipoEvento": "PreguntaPublicada",
  "versionEvento": 1,
  "fechaOcurrencia": "2026-10-01T15:30:00Z",
  "origen": "servicio-editorial",
  "idCorrelacion": "0b6f2c4e-1d3a-4e5f-8a7b-9c0d1e2f3a4b",
  "datos": {
    "preguntaId": "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f",
    "contexto": "Un grupo de 5 estudiantes obtuvo las notas 3,0; 3,5; 4,0; 4,0 y 4,5.",
    "preguntaDirecta": "¿Cuál es la moda del conjunto de notas?",
    "opciones": [
      { "letra": "A", "texto": "3,0" },
      { "letra": "B", "texto": "3,8" },
      { "letra": "C", "texto": "4,0" },
      { "letra": "D", "texto": "4,5" }
    ],
    "letraCorrecta": "C",
    "clasificacion": {
      "competenciaId": "22222222-2222-4222-8222-000000000101",
      "temaId": "22222222-2222-4222-8222-000000000201",
      "subtemaId": "22222222-2222-4222-8222-000000000301"
    },
    "nivelDificultad": "BAJO",
    "fechaPublicacion": "2026-10-01T15:30:00Z"
  }
}
```

| Campo de `datos` | Tipo | Regla |
|---|---|---|
| `preguntaId` | UUID | Id de la Pregunta en Editorial; Evaluación lo usa como `_id` de su copia. |
| `contexto`, `preguntaDirecta` | texto | No vacíos. |
| `opciones` | arreglo de 4 | Letras `A`–`D` en ese orden, sin repetir. Orden definido por Editorial. |
| `letraCorrecta` | `"A"`–`"D"` | Exactamente una (D-01: 3 distractores + 1 correcta). |
| `clasificacion` | objeto | Tres UUID del catálogo; solo identificadores (D-13). |
| `nivelDificultad` | enum | `BAJO`, `MEDIO`, `ALTO` (D-10). |
| `fechaPublicacion` | fecha ISO UTC | — |

### 7.5 `PreguntaArchivada` · routing key `pregunta.archivada` · productor Editorial
```json
{
  "idEvento": "…", "tipoEvento": "PreguntaArchivada", "versionEvento": 1,
  "fechaOcurrencia": "2026-10-05T10:00:00Z", "origen": "servicio-editorial", "idCorrelacion": null,
  "datos": {
    "preguntaId": "5c0e8d2a-7b1f-4c3d-9e2a-6f4b3c2d1e0f",
    "motivo": "Contenido desactualizado",
    "fechaArchivado": "2026-10-05T10:00:00Z"
  }
}
```

| Campo de `datos` | Tipo | Regla |
|---|---|---|
| `preguntaId` | UUID | Obligatorio. |
| `motivo` | texto | **Obligatorio**, de 1 a 500 caracteres. Viene del cuerpo de `POST /preguntas/{preguntaId}/archivado` (CU-09). |
| `fechaArchivado` | fecha ISO UTC | Obligatorio. |

### 7.6 `IntentoDeSimulacroCalificado` · exchange `evaluacion.eventos` · routing key `intento.calificado` · productor Evaluación
Hoy no tiene consumidor; queda listo para el futuro `servicio-seguimiento` (Customer/Supplier del Taller 1).
```json
{
  "idEvento": "…", "tipoEvento": "IntentoDeSimulacroCalificado", "versionEvento": 1,
  "fechaOcurrencia": "2026-10-01T16:10:00Z", "origen": "servicio-evaluacion", "idCorrelacion": "…",
  "datos": {
    "intentoId": "…", "simulacroId": "…",
    "estudianteId": "11111111-1111-4111-8111-000000000006",
    "fechaInicio": "2026-10-01T15:40:00Z",
    "fechaFinalizacion": "2026-10-01T16:10:00Z",
    "finalizadoPor": "ESTUDIANTE",
    "calificacion": { "totalPreguntas": 10, "correctas": 7, "puntaje": 70.00 },
    "desglosePorCompetencia": [
      { "competenciaId": "22222222-2222-4222-8222-000000000101", "totalPreguntas": 6, "correctas": 5, "puntaje": 83.33 }
    ]
  }
}
```
| Campo de `datos` | Tipo | Regla |
|---|---|---|
| `intentoId`, `simulacroId`, `estudianteId` | UUID | Obligatorios. |
| `fechaInicio`, `fechaFinalizacion` | fecha ISO UTC | Obligatorias. |
| `finalizadoPor` | enum | `ESTUDIANTE` \| `TIEMPO_AGOTADO`. |
| `calificacion` | objeto | `totalPreguntas` entero ≥ 1, `correctas` entero ≥ 0, `puntaje` número entre 0 y 100. |
| `desglosePorCompetencia` | arreglo | **Al menos un elemento** (todo simulacro tiene al menos una pregunta, INV-26); cada elemento con los mismos campos de `calificacion` más `competenciaId`. |

Reglas que el esquema JSON no puede expresar y que **el productor garantiza en su código**: `correctas ≤ totalPreguntas`; `puntaje = correctas / totalPreguntas × 100` redondeado a 2 decimales (en JSON `70.00` se serializa como `70`, y es válido); la suma del desglose coincide con el total.

### 7.7 Reglas de entrega (productor y consumidor)
**Productor (Editorial y Evaluación):**
1. Publica **después** de confirmar la transacción en su base de datos (en Spring: `@TransactionalEventListener(phase = AFTER_COMMIT)`). Nunca dentro del dominio.
2. Activa *publisher confirms*; si RabbitMQ no confirma, registra el error en el log con `idEvento`.

**Consumidor (Evaluación):**
1. **Ack manual** solo después de guardar en MongoDB.
2. **Idempotente**: procesar dos veces el mismo `idEvento` o la misma `preguntaId` no duplica nada (guardar con *upsert* por `preguntaId` y registrar `idEvento` procesados).
3. **Orden no garantizado**: si llega `PreguntaArchivada` de una pregunta desconocida, se guarda una marca `ARCHIVADA`; si después llega su `PreguntaPublicada`, la pregunta **queda archivada**.
4. Mensaje inválido → `nack` **sin** reencolar → termina en `evaluacion.preguntas.dlq`. Se registra en el log. Es inválido **solo** si: el JSON no se puede leer, `versionEvento` ≠ 1, `tipoEvento` no es uno de los esperados, o falta (o tiene tipo incorrecto) un campo **obligatorio**. **Un campo adicional desconocido NO lo vuelve inválido**: se ignora (lector tolerante, sección 4). Por eso el consumidor **no** valida con los esquemas estrictos de `/contratos`; valida solo los campos que usa.
5. Error transitorio (Mongo caído) → `nack` sin reencolar también (sin bucles infinitos); se revisa la DLQ en la consola de RabbitMQ.
6. Reintenta la conexión al broker al arrancar (espera progresiva, máximo 30 s entre intentos): el servicio **no** debe caerse si RabbitMQ aún no está listo.

### 7.8 Esquemas JSON
En `/contratos/eventos/...` hay un JSON Schema (draft 2020-12) por evento y un ejemplo válido en `/contratos/eventos/ejemplos/`. Se comprueban con `scripts/validar-contratos.sh` (o `.ps1` en Windows).

- **Los esquemas son estrictos** (`additionalProperties: false`) y describen lo que el **productor** debe enviar. El productor tiene una prueba que valida un mensaje real generado por su código contra el esquema.
- **El consumidor es tolerante** (regla 7.7.4). En sus pruebas usa los ejemplos de `/contratos/eventos/ejemplos/` y además un ejemplo con un campo extra, que debe procesarse sin ir a la DLQ.
- Límites de texto en los esquemas: `contexto` 1–2000, `preguntaDirecta` 1–500, `opciones[].texto` 1–300, `motivo` 1–500 caracteres (los mismos de la validación estructural de 11.1).
- `desglosePorCompetencia` con `minItems: 1`; `puntaje` con `minimum: 0` y `maximum: 100`, sin `multipleOf`.

---

## 8. Contratos REST por servicio

Todos los cuerpos usan los formatos de la sección 4. Los roles indicados se verifican con `X-Roles`.

### 8.1 `servicio-editorial` · `http://localhost:8081/api/v1`

| Método y ruta | Rol | CU | Éxito | Errores propios |
|---|---|---|---|---|
| `POST /preguntas` | `AUTOR` | CU-04 | 201 `PreguntaRespuesta` | 422 `CLASIFICACION_INVALIDA`, 503 `CATALOGO_NO_DISPONIBLE` |
| `PUT /preguntas/{preguntaId}` | `AUTOR` (dueño) | CU-05 | 200 `PreguntaRespuesta` | 404, 403, 409 `PREGUNTA_NO_EDITABLE`, 422, 503 |
| `GET /preguntas` | cualquiera | CU-06 | 200 página de `PreguntaResumen` | — |
| `GET /preguntas/{preguntaId}` | cualquiera (según rol) | CU-06 | 200 `PreguntaRespuesta` | 404 |
| `POST /preguntas/{preguntaId}/envio-revision` | `AUTOR` (dueño) | CU-07 | 200 `PreguntaRespuesta` | 404, 403, 409 `TRANSICION_NO_PERMITIDA` |
| `POST /preguntas/{preguntaId}/procesos-revision` | `ADMINISTRADOR` | CU-10 | 201 `ProcesoRevisionRespuesta` | 404, 409, 422 `REVISORES_INSUFICIENTES`, `AUTOR_NO_PUEDE_SER_REVISOR`, `REVISOR_DUPLICADO` |
| `GET /procesos-revision/{procesoId}` | `ADMINISTRADOR`, `REVISOR` asignado | — | 200 `ProcesoRevisionRespuesta` | 404, 403 |
| `GET /procesos-revision?revisorId={uuid}&estado=ABIERTO` | `REVISOR`, `ADMINISTRADOR` | CU-06 | 200 página | — |
| `POST /procesos-revision/{procesoId}/evaluaciones` | `REVISOR` asignado | CU-11 (+CU-12 automático) | 201 `ProcesoRevisionRespuesta` | 404, 403 `REVISOR_NO_ASIGNADO`, 409 `EVALUACION_YA_REGISTRADA`, 409 `PROCESO_CERRADO` |
| `POST /preguntas/{preguntaId}/publicacion` | `ADMINISTRADOR` | CU-08 | 200 `PreguntaRespuesta` (+ evento) | 404, 409 `TRANSICION_NO_PERMITIDA` |
| `POST /preguntas/{preguntaId}/archivado` | `ADMINISTRADOR` | CU-09 | 200 `PreguntaRespuesta` (+ evento) | 400 si falta `motivo`, 404, 409 `TRANSICION_NO_PERMITIDA` |
| `GET /preguntas/{preguntaId}/trazabilidad` | `ADMINISTRADOR` | CU-18 | 200 `TrazabilidadRespuesta` | 404 |

Filtros de `GET /preguntas`: `competenciaId`, `temaId`, `subtemaId`, `nivelDificultad`, `estado`, `autorId`, `pagina`, `tamano`. Restricción por rol (CU-06): `AUTOR` ve solo las suyas, `REVISOR` las asignadas, `DOCENTE` las `PUBLICADA`, `ADMINISTRADOR` todas.

**`PreguntaSolicitud`** (POST y PUT):
```json
{
  "contexto": "texto",
  "preguntaDirecta": "texto",
  "opciones": [
    { "letra": "A", "texto": "…", "esCorrecta": false },
    { "letra": "B", "texto": "…", "esCorrecta": false },
    { "letra": "C", "texto": "…", "esCorrecta": true },
    { "letra": "D", "texto": "…", "esCorrecta": false }
  ],
  "justificacion": "texto",
  "bibliografia": ["referencia 1"],
  "clasificacion": { "competenciaId": "uuid", "temaId": "uuid", "subtemaId": "uuid" },
  "nivelDificultad": "BAJO"
}
```
`clasificacion` y `nivelDificultad` son obligatorios (400 si faltan). El resto puede venir incompleto: la pregunta queda en `BORRADOR` (D-02).

**Estado al crear (aclaración):** toda pregunta **nace** en `BORRADOR` y, **en la misma petición**, se ejecuta la validación estructural. Si la supera, pasa a `EN_CONSTRUCCION` antes de responder. Por eso una pregunta completa responde `201` con `estado = EN_CONSTRUCCION`, y una incompleta con `estado = BORRADOR` y la lista `erroresValidacion`. La trazabilidad registra ambos hechos (`CREACION` y la `TRANSICION` `BORRADOR → EN_CONSTRUCCION`). `PUT` se comporta igual: revalida y ajusta el estado.

**Archivar** (`POST /preguntas/{preguntaId}/archivado`): cuerpo obligatorio `{ "motivo": "texto de 1 a 500 caracteres" }` (CU-09). Ese `motivo` es el que viaja en el evento `PreguntaArchivada`.

**`PreguntaRespuesta`**: todos los campos anteriores más `preguntaId`, `autorId`, `estado`, `erroresValidacion` (`[{ "regla": "RF-10", "mensaje": "…" }]`, vacío si supera la validación), `fechaCreacion`, `fechaActualizacion`.

**Asignar revisores** (`POST /preguntas/{preguntaId}/procesos-revision`):
```json
{ "revisoresIds": ["11111111-1111-4111-8111-000000000003", "11111111-1111-4111-8111-000000000004"] }
```

**Registrar evaluación** (`POST /procesos-revision/{procesoId}/evaluaciones`):
```json
{
  "criterios": [
    { "criterio": "PEDAGOGICO", "valoracion": 4 },
    { "criterio": "TECNICO", "valoracion": 5 },
    { "criterio": "ESTRUCTURAL", "valoracion": 4 }
  ],
  "observaciones": ["El distractor B es poco plausible"],
  "decision": "APROBATORIA"
}
```
`criterio` ∈ {`PEDAGOGICO`, `TECNICO`, `ESTRUCTURAL`} (los tres obligatorios); `valoracion` entero 1–5; `decision` ∈ {`APROBATORIA`, `REPROBATORIA`}.

**`ProcesoRevisionRespuesta`**: `procesoId`, `preguntaId`, `estado` (`ABIERTO` | `CERRADO`), `asignaciones` (`[{revisorId, fechaAsignacion, evaluacionRegistrada}]`), `evaluaciones`, `dictamen` (`null` | `{ "resultado": "APROBADA" | "RECHAZADA", "porcentajeAprobacion": 100.00, "fechaEmision": "…" }`).

**`TrazabilidadRespuesta`**: `preguntaId`, `registros` (`[{fecha, usuarioId, tipo: "CREACION"|"MODIFICACION"|"TRANSICION", estadoAnterior, estadoNuevo, detalle}]` en orden cronológico) e `historialRevisiones` (evaluaciones y dictámenes de todos los procesos).

### 8.2 `servicio-catalogo` · `http://localhost:8082/api/v1`

| Método y ruta | Rol | Éxito | Errores propios |
|---|---|---|---|
| `POST /competencias` | `ADMINISTRADOR` | 201 `CompetenciaRespuesta` | 409 `NOMBRE_DUPLICADO` |
| `GET /competencias` | cualquiera | 200 lista de `CompetenciaRespuesta` (con temas y subtemas anidados) | — |
| `GET /competencias/{competenciaId}` | cualquiera | 200 `CompetenciaRespuesta` | 404 `COMPETENCIA_NO_ENCONTRADA` |
| `PUT /competencias/{competenciaId}` | `ADMINISTRADOR` | 200 (renombrar) | 404, 409 `NOMBRE_DUPLICADO` |
| `POST /competencias/{competenciaId}/temas` | `ADMINISTRADOR` | 201 `CompetenciaRespuesta` | 404, 409 `NOMBRE_DUPLICADO` |
| `PUT /competencias/{competenciaId}/temas/{temaId}` | `ADMINISTRADOR` | 200 | 404 `TEMA_NO_ENCONTRADO`, 409 |
| `POST /competencias/{competenciaId}/temas/{temaId}/subtemas` | `ADMINISTRADOR` | 201 `CompetenciaRespuesta` | 404, 409 |
| `PUT /competencias/{competenciaId}/temas/{temaId}/subtemas/{subtemaId}` | `ADMINISTRADOR` | 200 | 404 `SUBTEMA_NO_ENCONTRADO`, 409 |

Cuerpo de creación o renombrado: `{ "nombre": "texto", "descripcion": "texto opcional" }`.
**`CompetenciaRespuesta`**: `{ "competenciaId", "nombre", "descripcion", "temas": [ { "temaId", "nombre", "subtemas": [ { "subtemaId", "nombre" } ] } ] }`.
Sin `DELETE`: borrar un elemento del catálogo dejaría preguntas con referencias rotas (D-13).

### 8.3 `servicio-evaluacion` · `http://localhost:8083/api/v1`

| Método y ruta | Rol | CU | Éxito | Errores propios |
|---|---|---|---|---|
| `POST /simulacros` | `DOCENTE` | CU-13 | 201 `SimulacroRespuesta` | 422 `PREGUNTAS_INSUFICIENTES`, 422 `DURACION_INVALIDA` |
| `GET /simulacros` | `DOCENTE`, `ESTUDIANTE` | — | 200 lista | — |
| `GET /simulacros/{simulacroId}` | `DOCENTE`, `ESTUDIANTE` | — | 200 `SimulacroRespuesta` (sin claves) | 404 `SIMULACRO_NO_ENCONTRADO` |
| `POST /simulacros/{simulacroId}/intentos` | `ESTUDIANTE` | CU-14 | 201 `IntentoRespuesta` | 404 |
| `PUT /intentos/{intentoId}/respuestas/{preguntaId}` | `ESTUDIANTE` (dueño) | CU-14 | 200 `IntentoRespuesta` | 404 `INTENTO_NO_ENCONTRADO`, 403, 409 `INTENTO_FINALIZADO`, 422 `PREGUNTA_NO_PERTENECE_AL_SIMULACRO` |
| `POST /intentos/{intentoId}/finalizacion` | `ESTUDIANTE` (dueño) | CU-14 + CU-15 | 200 `IntentoRespuesta` con calificación (+ evento) | 404, 403, 409 `INTENTO_FINALIZADO` |
| `GET /intentos/{intentoId}` | `ESTUDIANTE` (dueño), `DOCENTE` | — | 200 `IntentoRespuesta` | 404, 403 |
| `GET /preguntas-evaluables` | `DOCENTE` | — | 200 página de copias locales (sin `letraCorrecta`) | — |

`GET /preguntas-evaluables` existe para **demostrar en Postman que el evento llegó** (filtros `competenciaId`, `nivelDificultad`, `estado`).

**Definir simulacro** (`POST /simulacros`):
```json
{
  "nombre": "Simulacro cuantitativo 1",
  "criterios": {
    "competenciaIds": ["22222222-2222-4222-8222-000000000101"],
    "temaIds": [],
    "subtemaIds": [],
    "nivelesDificultad": ["BAJO", "MEDIO"]
  },
  "cantidadPreguntas": 5,
  "duracionMaximaMinutos": 30
}
```
Semántica de los criterios: dentro de una lista es **O**, entre listas es **Y**, y una lista vacía **no filtra**. Solo se eligen copias en estado `PUBLICADA`, al azar y sin repetir (INV-25, INV-27). Si hay menos de `cantidadPreguntas` candidatas → 422 `PREGUNTAS_INSUFICIENTES` (INV-26). `duracionMaximaMinutos` > 0 (INV-28).

**`IntentoRespuesta`**: `intentoId`, `simulacroId`, `estudianteId`, `estado` (`EN_CURSO` | `FINALIZADO` | `CALIFICADO`), `fechaInicio`, `fechaLimite`, `fechaFinalizacion`, `finalizadoPor`, `preguntas` (`[{preguntaId, posicion, contexto, preguntaDirecta, opciones:[{letra, texto}]}]`, **nunca** `letraCorrecta`), `respuestas` (`[{preguntaId, letraSeleccionada}]`), `calificacion` (`null` hasta calificar, luego igual a la del evento 7.6, con su desglose).

Cuerpo de respuesta del estudiante: `{ "letraSeleccionada": "B" }`.

---

## 9. Docker, puertos y variables de entorno

### 9.1 Nombres y puertos (los de la columna "Nombre en Docker" son los hosts de red)
| Nombre en Docker | Imagen | Puerto interno | Puerto publicado en el equipo |
|---|---|---|---|
| `servicio-editorial` | propia | 8081 | 8081 |
| `servicio-catalogo` | propia | 8082 (REST), 50051 (gRPC) | 8082, 50051 |
| `servicio-evaluacion` | propia | 8083 | 8083 |
| `rabbitmq` | `rabbitmq:3.13-management` | 5672, 15672 | 5672, 15672 |
| `bd-editorial` | `postgres:16` | 5432 | 5433 (solo depuración) |
| `bd-catalogo` | `postgres:16` | 5432 | 5434 (solo depuración) |
| `bd-evaluacion` | `mongo:7` | 27017 | 27017 (solo depuración) |

> **Error típico:** usar `localhost` dentro de un contenedor. Entre contenedores se usa el nombre de Docker (`rabbitmq`, `bd-editorial`, `servicio-catalogo`). Los valores por defecto del código apuntan a `localhost` con los puertos publicados, para poder ejecutar un servicio desde el IDE con el resto en Docker.

### 9.2 Variables de entorno (nombres exactos)
| Servicio | Variable | Valor en Docker | Valor por defecto local |
|---|---|---|---|
| editorial | `EDITORIAL_PUERTO_HTTP` | `8081` | `8081` |
| editorial | `EDITORIAL_BD_URL` | `jdbc:postgresql://bd-editorial:5432/editorial` | `jdbc:postgresql://localhost:5433/editorial` |
| editorial | `EDITORIAL_BD_USUARIO` / `EDITORIAL_BD_CLAVE` | `editorial` / `editorial` | igual |
| editorial | `CATALOGO_GRPC_HOST` / `CATALOGO_GRPC_PUERTO` | `servicio-catalogo` / `50051` | `localhost` / `50051` |
| catalogo | `CATALOGO_PUERTO_HTTP` / `CATALOGO_PUERTO_GRPC` | `8082` / `50051` | igual |
| catalogo | `CATALOGO_BD_URL` | `postgresql+psycopg://catalogo:catalogo@bd-catalogo:5432/catalogo` | `postgresql+psycopg://catalogo:catalogo@localhost:5434/catalogo` |
| contenedor `bd-catalogo` | `BD_CATALOGO_USUARIO` / `BD_CATALOGO_CLAVE` | `catalogo` / `catalogo` | — (solo las usa el contenedor de la base; deben coincidir con las de `CATALOGO_BD_URL`) |
| evaluacion | `EVALUACION_PUERTO_HTTP` | `8083` | `8083` |
| evaluacion | `EVALUACION_MONGO_URL` | `mongodb://bd-evaluacion:27017/evaluacion` | `mongodb://localhost:27017/evaluacion` |
| editorial y evaluacion | `RABBITMQ_HOST` / `RABBITMQ_PUERTO` | `rabbitmq` / `5672` | `localhost` / `5672` |
| editorial y evaluacion | `RABBITMQ_USUARIO` / `RABBITMQ_CLAVE` | `banco` / `banco123` | igual |
| todos | `TZ` | `UTC` | — |

Las credenciales viven en `.env` (no versionado) con los valores de `.env.example`. Nunca dentro del código ni de la imagen. El contenedor `bd-editorial` usa `EDITORIAL_BD_USUARIO` / `EDITORIAL_BD_CLAVE`.

**MongoDB sin autenticación (decisión intencional):** `bd-evaluacion` corre sin usuario ni clave porque solo es accesible dentro de la red Docker y en el puerto de depuración local. Es aceptable para el taller. En producción se activaría la autenticación y la URL llevaría credenciales.

### 9.3 Reglas para cada Dockerfile (cada dueño hace el suyo)
1. **Multietapa**: construcción (Maven / pip / npm) y ejecución separadas; la imagen final no lleva compiladores ni código fuente de pruebas.
2. Contexto = raíz del repositorio (sección 2).
3. Usuario sin privilegios (no `root`).
4. `EXPOSE` con los puertos internos de 9.1.
5. Debe incluir `curl` (lo usa el healthcheck): `HEALTHCHECK` / compose ejecuta `curl -f http://localhost:<puerto>/salud`.
6. El servicio arranca aunque sus dependencias no estén listas y reintenta conectarse (BD, RabbitMQ). El cliente gRPC de Editorial se conecta de forma perezosa (en la primera llamada).

`docker-compose.yml` (P3) usa `depends_on` con `condition: service_healthy` para las bases de datos y RabbitMQ (`rabbitmq-diagnostics -q ping`).

---

## 10. Flujo de punta a punta (lo que se demuestra en Postman)

```mermaid
sequenceDiagram
  autonumber
  actor A as Autor / Admin / Revisores
  participant ED as servicio-editorial
  participant CA as servicio-catalogo
  participant MQ as RabbitMQ
  participant EV as servicio-evaluacion
  actor D as Docente / Estudiante
  A->>ED: POST /preguntas
  ED->>CA: gRPC ValidarClasificacion
  CA-->>ED: valida = true
  ED-->>A: 201 (EN_CONSTRUCCION si viene completa)
  A->>ED: POST /preguntas/{id}/envio-revision
  A->>ED: POST /preguntas/{id}/procesos-revision (2 revisores)
  A->>ED: POST /procesos-revision/{id}/evaluaciones (x2)
  Note over ED: Dictamen automático mayor a 70 % → APROBADA
  A->>ED: POST /preguntas/{id}/publicacion
  ED-)MQ: PreguntaPublicada (pregunta.publicada)
  MQ-)EV: entrega en evaluacion.preguntas
  Note over EV: guarda copia local PUBLICADA
  D->>EV: GET /preguntas-evaluables
  D->>EV: POST /simulacros
  D->>EV: POST /simulacros/{id}/intentos
  D->>EV: PUT /intentos/{id}/respuestas/{preguntaId}
  D->>EV: POST /intentos/{id}/finalizacion
  EV-)MQ: IntentoDeSimulacroCalificado
  EV-->>D: 200 con calificación
```

**Postman (nombres exactos):**
- Colección `postman/BancoPreguntas.postman_collection.json` con carpetas `01 Catálogo`, `02 Editorial`, `03 Evaluación`, `04 Flujo completo`.
- Entorno `postman/local.postman_environment.json` con las variables `urlEditorial` (`http://localhost:8081/api/v1`), `urlCatalogo` (`http://localhost:8082/api/v1`), `urlEvaluacion` (`http://localhost:8083/api/v1`), `idAdministrador`, `idAutor`, `idRevisor1`, `idRevisor2`, `idRevisor3`, `idDocente`, `idEstudiante`, `competenciaId`, `temaId`, `subtemaId`, `direccionGrpcCatalogo` (`localhost:50051`), y las que llenan los scripts de prueba: `preguntaId`, `procesoId`, `simulacroId`, `intentoId`.
- La llamada gRPC se prueba con una petición gRPC de Postman (usando la reflexión del servidor) y también se documenta con `grpcurl` en el README.
- Para ver el evento: consola de RabbitMQ en `http://localhost:15672`, cola `evaluacion.preguntas`.

---

## 11. Secciones por servicio

### 11.1 `servicio-editorial` (P1 · Java 21 · Spring Boot 3 · PostgreSQL)

**Produce:** REST 8.1 · eventos `PreguntaPublicada` y `PreguntaArchivada` (7.4, 7.5).
**Consume:** gRPC `CatalogoAcademico.ValidarClasificacion` (6).
**Paquete raíz:** `co.edu.unicauca.bancopreguntas.editorial` con subpaquetes `dominio`, `aplicacion`, `infraestructura`, `interfaces`.

**Agregados (Taller 1):**
- `Pregunta` (raíz) con los value objects `Contexto`, `PreguntaDirecta`, `OpcionDeRespuesta`, `Justificacion`, `Bibliografia`, `ClasificacionAcademica`, `NivelDeDificultad`, `EstadoPregunta`, `RegistroDeTrazabilidad`, `HistorialDeRevisiones`. La máquina de estados vive **dentro** del agregado.
- `ProcesoDeRevision` (raíz) con la entidad interna `FormatoDeEvaluacion` y los value objects `AsignacionDeRevisor`, `Observacion`, `Dictamen`.

**Servicios de dominio:** `AsignadorRevisoresServicio`, `ResolutorDictamenServicio`, `PublicadorPreguntaServicio`.
**Puertos de salida:** `CatalogoAcademicoPuerto` (adaptador gRPC), `PublicadorEventosPuerto` (adaptador RabbitMQ), `PreguntaRepositorio`, `ProcesoDeRevisionRepositorio` (adaptadores JPA).

**Ciclo de vida (enum `EstadoPregunta`, valores exactos):**
```mermaid
stateDiagram-v2
  [*] --> BORRADOR: crear
  BORRADOR --> EN_CONSTRUCCION: supera validación estructural
  EN_CONSTRUCCION --> BORRADOR: una modificación la deja incompleta
  EN_CONSTRUCCION --> PENDIENTE_REVISION: envío a revisión (CU-07)
  PENDIENTE_REVISION --> EN_REVISION: asignar revisores (CU-10, D-14)
  EN_REVISION --> APROBADA: dictamen mayor a 70 %
  EN_REVISION --> RECHAZADA: dictamen menor o igual a 70 %
  RECHAZADA --> EN_CONSTRUCCION: automático (D-07)
  APROBADA --> PUBLICADA: publicación (emite PreguntaPublicada)
  PUBLICADA --> ARCHIVADA: archivado (emite PreguntaArchivada)
```
Solo `BORRADOR` y `EN_CONSTRUCCION` son editables (INV-10). `RECHAZADA` es transitorio: se registra en la trazabilidad y en el mismo caso de uso pasa a `EN_CONSTRUCCION`. Cualquier otra transición → 409 `TRANSICION_NO_PERMITIDA`.

**Validación estructural (RF-08 a RF-13). Valores exactos, como constantes con nombre:**
| Regla | Verificación |
|---|---|
| RF-08 | `contexto` no vacío (sin contar espacios), máximo 2000 caracteres. |
| RF-09 | `preguntaDirecta` no vacía, máximo 500 caracteres. |
| RF-10 / D-01 | Exactamente 4 opciones con letras `A`–`D` sin repetir. |
| RF-11 | Exactamente una opción con `esCorrecta = true`. |
| RF-12 | Ninguna opción contiene “todas las anteriores”, “ninguna de las anteriores”, “todas las opciones anteriores” ni “ninguna de las opciones anteriores”, comparando en minúsculas y sin tildes. |
| RF-13 | Cada opción tiene entre 1 y 300 caracteres y empieza por mayúscula o dígito. La longitud de cada distractor está entre 0,5 y 2 veces la de la respuesta correcta. |
| INV-04 | `justificacion` no vacía y al menos una entrada en `bibliografia`. |

Las reglas que fallan se devuelven en `erroresValidacion` (no son error HTTP). Una pregunta en `BORRADOR` puede incumplirlas; nunca sale de `BORRADOR` sin cumplirlas (INV-11).

**Revisión por pares:**
- Al menos 2 revisores (D-03), sin repetir (INV-17), ninguno igual al autor (D-06). Como no existe Identidad, no se puede comprobar que los ids tengan el rol `REVISOR`: es una **limitación documentada**. Al registrar la evaluación sí se exige que `X-Usuario-Id` esté asignado y que `X-Roles` contenga `REVISOR`.
- Dictamen: se calcula solo cuando el 100 % de los asignados evaluó (INV-19). Es `APROBADA` si aprobatorias / asignados × 100 **> 70** (estricto, INV-20). Con 2 o 3 revisores equivale a unanimidad (D-05).
- Cada evaluación se copia al `HistorialDeRevisiones` de la Pregunta (D-15).
- Toda modificación o transición agrega un `RegistroDeTrazabilidad` con fecha y `X-Usuario-Id` (RF-29, RF-30).

**No hacer:** guardar nombres de competencias (solo ids, D-13); publicar el evento desde el dominio; exponer entidades JPA en los controladores; borrar preguntas.

### 11.2 `servicio-catalogo` (P3 · Python 3.12 · FastAPI · PostgreSQL)

**Produce:** REST 8.2 · servidor gRPC `CatalogoAcademico` (6).
**Consume:** nada.
**Paquete raíz:** `catalogo` con `dominio`, `aplicacion`, `infraestructura` (SQLAlchemy, siembra de datos), `interfaces` (`rest` con routers FastAPI y `grpc` con el *servicer*). FastAPI (uvicorn) y el servidor `grpc.aio` corren en el **mismo proceso** con asyncio.

**Agregado:** `Competencia` (raíz) con las entidades internas `Tema` y `Subtema`. Toda modificación de un tema o subtema se hace a través de la `Competencia` y se guarda como una unidad.
**Invariantes:** INV-22 (identificador estable, independiente del nombre), INV-23 (sin huérfanos: todo `Tema` pertenece a una `Competencia` y todo `Subtema` a un `Tema`), INV-24 (nombre de competencia único en el catálogo; nombre de tema único dentro de su competencia; nombre de subtema único dentro de su tema). La unicidad compara sin distinguir mayúsculas y quitando espacios sobrantes.
**Caso de uso de la consulta gRPC:** `ValidarClasificacionCasoUso`, que aplica el orden de verificación del enum `MotivoRechazo`.
**Siembra:** al arrancar, si no hay competencias, carga la tabla 4.3 con esos ids exactos (de forma idempotente).
**Tareas transversales (P3):** esqueleto del repo, `docker-compose.yml`, `.env.example`, `CODEOWNERS`, diagrama de arquitectura, README raíz (incluye `grpcurl`).

**No hacer:** devolver nombres en la respuesta gRPC; ofrecer `DELETE`; mezclar los modelos SQLAlchemy con las entidades de dominio.

### 11.3 `servicio-evaluacion` (P2 · Node 20 · NestJS 10 · MongoDB)

**Produce:** REST 8.3 · evento `IntentoDeSimulacroCalificado` (7.6).
**Consume:** eventos `PreguntaPublicada` y `PreguntaArchivada` de la cola `evaluacion.preguntas` (7).
**Carpetas:** `src/dominio`, `src/aplicacion`, `src/infraestructura` (repositorios Mongo, publicador RabbitMQ), `src/interfaces` (`rest` con controladores, `mensajeria` con consumidores).

**Modelo local de la pregunta:** `PreguntaEvaluable`, el “ítem cerrado” del Taller 1, que es inmutable. Colección `preguntas_evaluables` con `_id = preguntaId` y los campos de 7.4 más `estado` (`PUBLICADA` | `ARCHIVADA`) y `fechaActualizacion`. **No** es un agregado editable: solo lo crean o archivan los consumidores.
**Agregados:**
- `Simulacro` (raíz, colección `simulacros`): `CriterioDeGeneracion`, `DuracionMaxima`, `docenteId`, `PreguntasSeleccionadas` (`[{preguntaId, posicion}]`).
- `IntentoDeSimulacro` (raíz, colección `intentos`): `estudianteId`, `RespuestaDelEstudiante` (como máximo una por pregunta, INV-30) y `Calificacion`.

**Servicios de dominio:** `EnsambladorSimulacroServicio` (selección aleatoria, sección 8.3) y `CalificadorSimulacroServicio` (compara las respuestas con `letraCorrecta` de las copias locales; una pregunta sin respuesta cuenta como incorrecta; puntaje con 2 decimales; desglose por `competenciaId`).

**Estados del intento:**
```mermaid
stateDiagram-v2
  [*] --> EN_CURSO: iniciar (CU-14)
  EN_CURSO --> FINALIZADO: el estudiante finaliza o vence la duración
  FINALIZADO --> CALIFICADO: CalificadorSimulacroServicio (CU-15)
  CALIFICADO --> [*]
```
- `fechaLimite = fechaInicio + duracionMaximaMinutos`.
- **Vencimiento perezoso:** cualquier operación sobre un intento `EN_CURSO` con la hora actual ≥ `fechaLimite` lo finaliza primero (`finalizadoPor = TIEMPO_AGOTADO`), lo califica y luego responde 409 `INTENTO_FINALIZADO` si la operación era registrar una respuesta.
- Finalizar y calificar ocurren en el mismo caso de uso. La calificación es inmutable (INV-32). Al quedar `CALIFICADO` se publica `IntentoDeSimulacroCalificado`.
- Archivar una pregunta **no** altera simulacros ni intentos existentes; solo la excluye de simulacros nuevos.

**No hacer:** exponer `letraCorrecta` antes de calificar; usar `ObjectId`; consultar a Editorial o a Catálogo (este servicio no hace llamadas síncronas); reencolar mensajes en bucle.

---

## 12. Lista de verificación anti-conflictos (antes de integrar)

- [ ] Los ids son UUID en texto en minúsculas en REST, eventos, gRPC y bases de datos.
- [ ] Las claves JSON están en camelCase en los tres servicios (incluido Python con alias).
- [ ] Los enums van en MAYÚSCULAS, sin tildes y con los valores exactos de este documento.
- [ ] Las fechas son ISO-8601 UTC con `Z`, y los contenedores corren con `TZ=UTC`.
- [ ] El `.proto` se usa desde `/contratos` sin copias editadas, y el contexto de Docker es la raíz.
- [ ] Exchange, colas, bindings y argumentos de RabbitMQ coinciden letra por letra con 7.1.
- [ ] El evento se publica después del commit y el consumidor es idempotente con ack manual.
- [ ] Los hosts dentro de Docker usan nombres de servicio, nunca `localhost`.
- [ ] Los errores usan `application/problem+json` con el campo `codigo`.
- [ ] Los encabezados `X-Usuario-Id`, `X-Roles` y `X-Id-Correlacion` tienen exactamente esos nombres.
- [ ] Las pruebas y Postman usan los ids de prueba de 4.2 y 4.3.
- [ ] Swagger está en `/docs` y la salud en `/salud` en los tres servicios.

---

## 13. Historial de cambios

### 1.1 (2-oct-2026), tras la fase 0
1. Ya no se exige la aprobación de los tres: los cambios compartidos necesitan un PR aprobado por al menos otro integrante. Se agregó el `CODEOWNERS` con los usuarios reales (P1 `@juanvec06`, P2 `@JDiegoG12`, P3 `@JuanDv1`) (secciones 1 y 3.6).
2. Hay un solo `.dockerignore`, en la raíz. Los de cada servicio no sirven con el contexto en la raíz (sección 2).
3. Variables `BD_CATALOGO_USUARIO` / `BD_CATALOGO_CLAVE` para el contenedor `bd-catalogo`, y URL local completa de `CATALOGO_BD_URL` (9.2).
4. MongoDB sin autenticación, como decisión intencional (9.2).
5. `PreguntaArchivada.motivo` es obligatorio y llega en el cuerpo `{ "motivo" }` de `POST /preguntas/{preguntaId}/archivado` (7.5 y 8.1).
6. Variables de Postman nuevas: `idRevisor3` y `direccionGrpcCatalogo` (10).
7. Al crear, la pregunta nace en `BORRADOR` y pasa a `EN_CONSTRUCCION` en la misma petición si supera la validación (8.1).
8. Los esquemas son estrictos para el productor y el consumidor es tolerante: un campo extra no envía el mensaje a la DLQ (7.7 y 7.8).
9. Límites de texto en los esquemas, `desglosePorCompetencia` con al menos un elemento, `puntaje` entre 0 y 100, y reglas que garantiza el código del productor (7.6 y 7.8).
10. `evaluacion.eventos` con `autoDelete=false` (7.1).