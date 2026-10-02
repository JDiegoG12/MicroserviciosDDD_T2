# servicio-editorial

Microservicio de **Gestión Editorial de Preguntas** (dueño: P1 · Java 21 · Spring Boot 4.1.1 · PostgreSQL).

Gestiona el ciclo de vida de las preguntas (creación, validación estructural, revisión por pares, dictamen, publicación y archivado), valida la clasificación académica con `servicio-catalogo` por gRPC y publica los eventos `PreguntaPublicada` y `PreguntaArchivada` en RabbitMQ.

- REST: `http://localhost:8081/api/v1` · Swagger UI: `/docs` (200, sin redirección; la página de springdoc queda también en `/swagger-ui.html`) · OpenAPI: `/openapi.json` · Salud: `/salud`
- Contratos: [CONTRATOS.md](../CONTRATOS.md) secciones 6, 7.4, 7.5, 8.1 y 11.1. Modelo de dominio: [MODELO-DOMINIO.md](../MODELO-DOMINIO.md).
- Dudas resueltas y abiertas del servicio: [DUDAS.md](DUDAS.md).

## Capas (Clean Architecture, CONTRATOS.md 3.3)
| Paquete | Contenido |
|---|---|
| `dominio` | Agregados `Pregunta` y `ProcesoDeRevision`, value objects, servicios de dominio, eventos, repositorios (interfaces). Java puro. |
| `aplicacion` | Doce casos de uso, puertos de entrada y salida, comandos y resultados. Sin Spring. |
| `infraestructura` | JPA + Flyway, cliente gRPC de Catálogo, publicador RabbitMQ, decorador transaccional y cableado de Spring. |
| `interfaces` | Controladores REST, identidad por encabezados, errores `application/problem+json`, OpenAPI. |

## Arranque sin base de datos (CONTRATOS.md 9.3.6)
El servicio **no depende de PostgreSQL para arrancar**:
- Hibernate no toca el esquema (`ddl-auto=none`) ni abre conexiones al iniciar: el dialecto va fijo y `hibernate.boot.allow_jdbc_metadata_access=false`. Hikari tiene `initialization-fail-timeout=-1`.
- Flyway **no** corre al arrancar. `MigradorEsquemaEnSegundoPlano` ejecuta `Flyway.migrate()` tras el arranque, con reintentos y espera progresiva (1 s, 2 s, 4 s… hasta 30 s), y registra cada intento en el log.
- Mientras las migraciones no terminan, todo `/api/v1/**` responde **503** `BASE_DE_DATOS_NO_DISPONIBLE` en *problem+json*. `/salud`, `/docs` y `/openapi.json` siguen en 200. Si la base de datos cae después, las peticiones también responden 503.
- La validación del esquema frente a las entidades se hace en las pruebas de integración (perfil `pruebas`: Flyway al iniciar y `ddl-auto=validate`).

## Pruebas
```bash
cd servicio-editorial
mvn test     # unitarias (dominio, aplicación, gRPC en proceso, contrato de eventos) y de API (@WebMvcTest)
mvn verify   # además, las de integración *IT con Testcontainers (PostgreSQL 16 y RabbitMQ 3.13); requiere Docker
```
`mvn verify` deja el reporte de cobertura de JaCoCo (unitarias + integración, sin umbral) en `target/site/jacoco-combinado/index.html`.

## Ejecución con Docker (contexto = raíz del repositorio)
```bash
cp .env.example .env
docker compose --profile servicios build servicio-editorial
docker compose --profile servicios up -d servicio-editorial   # levanta también bd-editorial y rabbitmq
curl http://localhost:8081/salud
```

## Ejecución local desde el IDE
Con `bd-editorial` y `rabbitmq` en Docker (`docker compose up -d bd-editorial rabbitmq`), los valores por defecto de `application.yml` apuntan a `localhost` con los puertos publicados (CONTRATOS.md 9.2): `mvn spring-boot:run`.

## Variables de entorno (CONTRATOS.md 9.2)
`EDITORIAL_PUERTO_HTTP`, `EDITORIAL_BD_URL`, `EDITORIAL_BD_USUARIO`, `EDITORIAL_BD_CLAVE`, `CATALOGO_GRPC_HOST`, `CATALOGO_GRPC_PUERTO`, `RABBITMQ_HOST`, `RABBITMQ_PUERTO`, `RABBITMQ_USUARIO`, `RABBITMQ_CLAVE`.
