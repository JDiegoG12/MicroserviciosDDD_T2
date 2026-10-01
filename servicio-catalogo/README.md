# servicio-catalogo

Microservicio del **Catálogo Académico** (dueño: P3 · Python 3.12 · FastAPI · PostgreSQL).

Administra la jerarquía Competencia → Tema → Subtema y publica el servicio gRPC `CatalogoAcademico.ValidarClasificacion`, que los demás servicios usan para comprobar que una clasificación existe y es coherente. Al arrancar con la base vacía carga el catálogo semilla de CONTRATOS.md §4.3.

- REST: `http://localhost:8082/api/v1` · Swagger: `/docs` · Salud: `/salud`
- gRPC: `localhost:50051` (con reflexión)
- Contratos: [CONTRATOS.md](../CONTRATOS.md) secciones 6, 8.2 y 11.2.

> **Implementación pendiente (fase 1).**
