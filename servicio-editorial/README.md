# servicio-editorial

Microservicio de **Gestión Editorial de Preguntas** (dueño: P1 · Java 21 · Spring Boot 3 · PostgreSQL).

Gestiona el ciclo de vida de las preguntas (creación, validación estructural, revisión por pares, dictamen, publicación y archivado), valida la clasificación académica con `servicio-catalogo` por gRPC y publica los eventos `PreguntaPublicada` y `PreguntaArchivada` en RabbitMQ.

- REST: `http://localhost:8081/api/v1` · Swagger: `/docs` · Salud: `/salud`
- Contratos: [CONTRATOS.md](../CONTRATOS.md) secciones 6, 7.4, 7.5, 8.1 y 11.1.

> **Implementación pendiente (fase 1).**
