# ETAPA 2 · servicio-editorial (P1 · Java 21 · Spring Boot 4.1.1)

Eres el agente responsable de **servicio-editorial**. En esta etapa conectas el dominio y la aplicación de la etapa 1 con PostgreSQL, REST, el cliente gRPC de Catálogo y el publicador de RabbitMQ, y lo empaquetas en Docker. Es el centro del flujo de punta a punta: sin su evento no hay demostración.

## Reglas obligatorias (todas las etapas)
1. Antes de escribir código lee **/CONTRATOS.md** (secciones 0 a 10, la sección 12 y la sección 11.x de tu servicio), **/MODELO-DOMINIO.md** (Parte A completa y, de la Parte B, lo que su tabla indica para tu servicio) y el **CLAUDE.md** de tu servicio. CONTRATOS.md es la fuente de verdad: rutas, nombres JSON, códigos de error, colas, variables de entorno y puertos deben quedar EXACTAMENTE como allí aparecen.
2. Trabaja **solo** dentro de la carpeta de tu servicio. Puedes **leer** `/contratos` (el `.proto`, los esquemas y los ejemplos de eventos), pero no modificarlo. No modifiques otros servicios, `CONTRATOS.md`, `MODELO-DOMINIO.md` ni `/postman`.
3. **Git es tarea del integrante.** No ejecutes `git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git switch`, `git merge`, `git rebase` ni `git stash`. Solo puedes usar `git status`, `git diff` y `git log`. Deja los cambios sin preparar.
4. Todo en español, con identificadores sin tildes ni ñ. Código legible para cualquier integrante. Documentación obligatoria en todo elemento público (CONTRATOS.md 3.2).
5. Si algo es ambiguo, no inventes: toma la opción más conservadora, márcala con `DUDA:` en un comentario y repórtala.

## Punto de partida
- La **etapa 1 ya está hecha**: las capas `dominio` y `aplicacion` existen y tienen pruebas. **Léelas antes de empezar** y ejecuta las pruebas para confirmar que pasan.
- En esta etapa **no reescribas** el dominio ni la aplicación. Solo construyes a su alrededor. Si de verdad necesitas un cambio (por ejemplo, un método que falta en un puerto), hazlo mínimo, sin romper las pruebas existentes, y explícalo en el resumen con el motivo.

## Paso 0 · Resolver las 14 dudas de la etapa 1 (antes de todo lo demás)
El equipo revisó `DUDAS.md` y decidió cada punto. Las decisiones están en **CONTRATOS.md v1.8**: secciones 3.3.5, 4, 5.3, 8.1, 11.1, y el historial 13 → 1.8. Aplica cada una, actualiza o agrega sus pruebas, **borra el comentario `DUDA:`** y reemplázalo por una referencia a la regla. Al final, actualiza `DUDAS.md` moviendo cada punto a "Dudas resueltas" con la sección de CONTRATOS que lo resuelve.

| # | Duda | Decisión |
|---|---|---|
| 1 | Código del 404 de procesos | **Se acepta** `PROCESO_REVISION_NO_ENCONTRADO` (ya está en 5.3). |
| 2 | Campos de `PreguntaResumen` | **Se aceptan** tal como los definiste (8.1). |
| 3 | Forma de `evaluaciones` | **Se acepta** (8.1). |
| 4 | Forma de `historialRevisiones` | **Se acepta** (8.1). |
| 5 | Dos agregados en un caso de uso | **Se acepta como excepción documentada**: una sola transacción local, con bloqueo optimista en ambos agregados (3.3.5 y 11.1, "Excepción a 3.3.5"). En la etapa 2 los dos guardados van dentro del mismo decorador transaccional. Agrega una prueba de integración: si falla el guardado de la pregunta, el proceso tampoco queda guardado. Deja en el JavaDoc de ambos casos de uso la justificación (D-14, D-15). |
| 6 | CU-06 con varios roles | **Se cambia: unión de resultados.** `ADMINISTRADOR` ve todas; si no, se ve la unión de lo que permite cada rol (por ejemplo `AUTOR,DOCENTE` = sus preguntas + todas las `PUBLICADA`). Los filtros y la paginación se aplican sobre esa unión **en la base de datos** (por ejemplo con `Specification` de JPA, combinando condiciones con O). Solo `ESTUDIANTE` → 403 (se acepta). Pruebas con combinaciones de roles. |
| 7 | 403 o 404 en una pregunta no visible | **Se acepta 403** `ACCESO_DENEGADO` (8.1). |
| 8 | Filtros de `GET /procesos-revision` | **Se ajusta:** `estado` es `ABIERTO` (por defecto) o `CERRADO`; otro valor → 400. El `REVISOR` solo ve los suyos (si omite `revisorId` se usa el suyo, y si pide el de otro → 403). El `ADMINISTRADOR` **puede omitir** `revisorId` y ve todos los de ese estado. Paginado según 5.1. Extiende el repositorio con un método de búsqueda por estado y revisor opcional, con paginación. |
| 9 | Publicar sin dictamen favorable | **Se acepta** `TRANSICION_NO_PERMITIDA` (8.1). |
| 10 | Criterios o valoraciones inválidos | **Se acepta 400** `SOLICITUD_INVALIDA`. Es la regla general nueva de 5.3: el formato o rango de un campo sin INV detrás es 400. |
| 11 | `tamano` > 100 | **Se acepta el rechazo con 400** (5.1, desde la v1.6). |
| 12 | Unidad de los caracteres | **Se acepta: puntos de código Unicode**, tras quitar los espacios del inicio y del final. Ahora es regla general (sección 4). |
| 13 | Historial fuera de `EN_REVISION` | **Se acepta** `TRANSICION_NO_PERMITIDA` (11.1). |
| 14 | Responsable de aprobar o rechazar | **Se acepta** el revisor que disparó el dictamen, más el `detalle` exacto de 11.1: `"Dictamen automático (CU-12) disparado por la evaluación del revisor <revisorId>"`. |
| — | "Spring Boot 3" en la documentación | Actualiza `servicio-editorial/CLAUDE.md` y `servicio-editorial/README.md` a **Spring Boot 4.1.1**. |

Al terminar el paso 0, todas las pruebas de la etapa 1 siguen en verde y ya no queda ninguna `DUDA:` de la etapa 1 en el código (verifícalo con una búsqueda).

## Alcance de la ETAPA 2: adaptadores + Dockerfile
Implementa todo lo de las capas `infraestructura` e `interfaces` (CONTRATOS.md 3.3), sin atajos:

**REST (sección 5 y 8.x de tu servicio)**
- Todos los endpoints de tu sección 8.x, con sus métodos, rutas, roles, códigos de éxito y errores **exactos**.
- DTOs JSON en camelCase, propios de `interfaces`: no expongas entidades del dominio ni modelos de base de datos.
- Validación de forma → **400** `SOLICITUD_INVALIDA` con la lista `errores` (`campo`, `mensaje`).
- **Encabezados de identidad (4.1):** cada petición a `/api/v1` arma `UsuarioActual` desde `X-Usuario-Id` y `X-Roles`. Si faltan → 401 `NO_AUTENTICADO`; si `X-Usuario-Id` no es un UUID → 400.
- **Correlación:** si llega `X-Id-Correlacion` se usa; si no, se genera. Se devuelve en la respuesta, se escribe en todas las líneas de log y se propaga a gRPC y a los eventos.
- **Errores:** un manejador global produce `application/problem+json` con `type`, `title`, `status`, `detail`, `instance`, `codigo`, `idCorrelacion` y `errores` (5.2), y traduce **cada** código de dominio o aplicación al HTTP de la tabla 5.3. Nunca expone trazas; un error inesperado → 500 `ERROR_INTERNO`.
- `POST` que crea → 201 con encabezado `Location`. Paginación con `pagina` y `tamano` y la respuesta de 5.1.
- `GET /salud` fuera de `/api/v1`, sin encabezados → `{"estado":"OK","servicio":"<nombre>"}`.
- Swagger UI en `GET /docs` y OpenAPI JSON en `GET /openapi.json`. Cada endpoint documenta:
  - descripción y CU;
  - los encabezados `X-Usuario-Id`, `X-Roles` y `X-Id-Correlacion`;
  - un ejemplo de cuerpo y de respuesta;
  - **todas** sus respuestas de error.

**Persistencia**
- Modelos de base de datos y *mappers* en `infraestructura`. El repositorio de dominio se implementa con el sufijo de la tecnología (`...Jpa`, `...SqlAlchemy`, `...Mongo`).
- Los ids se guardan como UUID (texto en Mongo, `uuid` en PostgreSQL) y las fechas en UTC.
- El esquema se crea con **migraciones o índices versionados**, nunca con generación automática en caliente. El detalle está en la sección de tu servicio.
- Nunca se borra físicamente nada (no hay `DELETE`).

**Configuración**
- Solo con las variables de entorno de la sección 9.2 (nombres exactos), con los valores por defecto locales de esa tabla. Nada de credenciales en el código.
- El servicio arranca aunque la base de datos o el broker aún no estén listos, y reintenta (9.3.6).

**Dockerfile** en `servicio-<nombre>/Dockerfile`, según 9.3:
- multietapa, con contexto en la **raíz del repositorio**;
- usuario sin privilegios, `EXPOSE`, `curl` instalado y `HEALTHCHECK` contra `/salud`, `TZ=UTC`;
- la imagen final no contiene código de pruebas ni herramientas de construcción.

No modifiques `docker-compose.yml`: la entrada de tu servicio ya existe. Si algo de ella no funciona, repórtalo como `DUDA:` con la corrección exacta.

## Pruebas de esta etapa
- Las pruebas de la etapa 1 siguen pasando.
- **Pruebas de la API** (sin base de datos real, con dobles de los casos de uso o de los repositorios):
  - cada endpoint en su caso feliz;
  - 401 sin encabezados y 403 con rol incorrecto;
  - 400 con cuerpo inválido;
  - el formato *problem+json* con `codigo`;
  - el mapeo de cada código de error al HTTP de la tabla 5.3.
- **Pruebas de integración de los adaptadores** con contenedores reales mediante **Testcontainers**, en la versión para tu lenguaje: repositorios contra su base de datos real y mensajería contra RabbitMQ real.
- Si Docker no está disponible, márcalo como `DUDA:` y no simules estas pruebas con dobles.

## Verificación final (obligatoria)
1. Ejecuta todas las pruebas: unitarias, de API e integración.
2. Construye la imagen: `docker compose --profile servicios build servicio-<nombre>`.
3. Levanta tu servicio con sus dependencias: `docker compose --profile servicios up -d servicio-<nombre>`. Espera a que quede `healthy`.
4. Prueba de humo con `curl` contra el contenedor, mostrando las respuestas:
   - `/salud`, `/docs` y `/openapi.json`;
   - un caso feliz por cada grupo de endpoints;
   - un 401, un 403, un 400 y un error de negocio (409 o 422).
   Más las pruebas específicas de tu servicio.
5. Revisa los logs del contenedor: sin errores y con `idCorrelacion` en cada línea.
6. Detén solo tu servicio (`docker compose stop servicio-<nombre>`), sin borrar volúmenes.

## Resumen final que debes entregar
- Tabla de endpoints implementados (método, ruta, rol, código de éxito, errores) y su prueba de humo (código HTTP obtenido).
- Resultado de las pruebas: unitarias, API e integración (cuántas pasan y cuántas fallan).
- Tamaño de la imagen y tiempo hasta quedar `healthy`.
- Cambios que tuviste que hacer al dominio o a la aplicación, con su justificación (idealmente ninguno).
- Lista de `DUDA:` y lista de archivos creados o modificados, para que el integrante los revise y haga el commit.

## Detalle de servicio-editorial

**Dependencias (Spring Boot 4.1.1, sin fijar versiones que gestione Boot)**
- Usa los *starters* vigentes en Spring Boot 4 para MVC, validación, JPA, AMQP y Flyway. Consulta la documentación oficial de Boot 4, porque varios starters cambiaron de nombre o se modularizaron respecto a Boot 3.
- Driver de PostgreSQL y el módulo de Flyway para PostgreSQL.
- **springdoc-openapi** en la versión compatible con Spring Boot 4.
- **gRPC:** `grpc-netty-shaded`, `grpc-protobuf` y `grpc-stub`, más un plugin de Maven para protobuf que genere el código desde `${project.basedir}/../contratos/proto` en `target/`. El código generado no se versiona.
- Pruebas: Testcontainers (PostgreSQL, RabbitMQ), un validador de JSON Schema (por ejemplo `networknt/json-schema-validator`) y grpc-testing o un servidor gRPC en proceso.
- Si una versión de terceros no es compatible con Boot 4, márcala como `DUDA:` con la alternativa que usaste.

**Capas y Spring**
- `dominio` y `aplicacion` siguen **sin anotaciones de Spring**. Los casos de uso se declaran como beans en `infraestructura.configuracion`.
- La transacción de cada caso de uso se maneja con un decorador o `TransactionTemplate` en infraestructura, no con `@Transactional` dentro de la aplicación.

**Persistencia (PostgreSQL + JPA + Flyway)**
- Migración `V1__esquema_inicial.sql`, con `spring.jpa.hibernate.ddl-auto=validate`. Las entidades JPA viven en `infraestructura.persistencia` y se traducen con *mappers*.
- Se persiste **todo** el agregado `Pregunta`:
  - opciones;
  - bibliografía;
  - `RegistroDeTrazabilidad`, de solo anexado;
  - `HistorialDeRevisiones`, de solo anexado;
  - clasificación como tres columnas `uuid`.
- Se persiste todo `ProcesoDeRevision`: asignaciones, formatos, criterios, observaciones y dictamen.
- Puedes usar tablas hijas o columnas `jsonb` para colecciones de value objects; justifica la elección en un comentario de la migración.
- Bloqueo optimista (columna `version`) en ambos agregados.
- `PreguntaRepositorioJpa.buscarPorCriterios` aplica los filtros y la paginación de 8.1 en la base de datos, no en memoria.

**Cliente gRPC (`CatalogoAcademicoGrpcAdaptador` implementa `CatalogoAcademicoPuerto`)**
- Canal hacia `CATALOGO_GRPC_HOST:CATALOGO_GRPC_PUERTO`, en texto plano y **perezoso**: el servicio arranca aunque Catálogo esté caído.
- *Deadline* de **2 s** por llamada y metadato `x-id-correlacion`.
- Traducción exacta de la tabla de la sección 6:
  - `valida=false` → `CLASIFICACION_INVALIDA` con el `detalle`;
  - `INVALID_ARGUMENT` → `SOLICITUD_INVALIDA`;
  - `UNAVAILABLE`, `DEADLINE_EXCEEDED` y cualquier otro error → `CATALOGO_NO_DISPONIBLE`.
- Prueba con un **servidor gRPC falso en proceso** que use el mismo `.proto`: válida, inválida con cada motivo, id mal formado y servidor que no responde (deadline).

**Publicador RabbitMQ (`PublicadorEventosRabbitMqAdaptador` implementa `PublicadorEventosPuerto`)**
- Declara el exchange `editorial.eventos` (`topic`, `durable=true`, `autoDelete=false`), exactamente como en 7.1.
- Traduce `PreguntaPublicada` y `PreguntaArchivada` del dominio al **sobre de 7.3** y a los `datos` de 7.4 y 7.5:
  - `idEvento` nuevo;
  - `fechaOcurrencia` del dominio;
  - `origen` = `servicio-editorial`;
  - `idCorrelacion` de la petición.
- Publica con las routing keys `pregunta.publicada` y `pregunta.archivada` y las propiedades AMQP de 7.2.
- Los demás eventos de dominio no van al broker: solo se registran en el log.
- **Publica después del commit** (7.7: `@TransactionalEventListener(phase = AFTER_COMMIT)` o una sincronización de transacción equivalente). Si la transacción falla, no se publica nada.
- *Publisher confirms* activos. Si no hay confirmación, el log registra el `idEvento`.
- **Prueba del productor contra el contrato:** el JSON que genera tu código para cada evento se valida contra `/contratos/eventos/editorial/*.schema.json` y debe pasar. Prueba también que las fechas salen en texto ISO UTC con `Z` (Jackson 3).
- Prueba de integración con RabbitMQ real (Testcontainers):
  - publicar una pregunta deja en una cola de prueba, enlazada a `pregunta.publicada`, un mensaje válido;
  - si el caso de uso falla después de guardar, no sale ningún mensaje.

**Pruebas de humo propias (paso 4 de la verificación)**
Con la infraestructura arriba y **sin** Catálogo:
- `POST /api/v1/preguntas` responde 503 `CATALOGO_NO_DISPONIBLE` y el servicio sigue sano.

Si en tu máquina ya está disponible `servicio-catalogo`, haz además el flujo completo:
1. Crear la pregunta con los ids de la tabla 4.3.
2. Enviarla a revisión.
3. Asignar 2 revisores.
4. Registrar 2 evaluaciones aprobatorias.
5. Publicar.
6. Verificar en la consola de RabbitMQ (o con una cola temporal) que salió `PreguntaPublicada`.
7. Consultar la trazabilidad.

Si Catálogo no está disponible, deja ese flujo documentado como pendiente para la etapa 3.
