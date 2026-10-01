# CLAUDE.md · servicio-catalogo

## Qué es este servicio
- **Contexto (Taller 1):** Catálogo Académico.
- **Dueño:** P3.
- **Tecnología:** Python 3.12 · FastAPI · SQLAlchemy · gRPC (`grpc.aio`) · PostgreSQL 16 (`bd-catalogo`).
- **Puertos:** REST `8082` (`http://localhost:8082/api/v1`) y gRPC `50051`. Swagger en `/docs`, salud en `/salud`.
- **Produce:** REST de la sección 8.2 y el servidor gRPC `CatalogoAcademico` (sección 6, con reflexión activada).
- **Consume:** nada.

## Antes de escribir código
**Antes de escribir código lee /CONTRATOS.md: secciones 1 a 10 y la sección 11.2 de este servicio.**
Ese documento es la fuente única de verdad: si algo de este archivo choca con él, manda CONTRATOS.md.

## Reglas
1. **No leas ni modifiques otros servicios** (`servicio-editorial`, `servicio-evaluacion`). Todo lo que necesitas de ellos está en CONTRATOS.md y en `/contratos`.
2. **No modifiques `/contratos` ni `CONTRATOS.md`.** Si un contrato no alcanza, detente y repórtalo al equipo (se cambia por PR aprobado por al menos otro integrante).
3. **Clean Architecture (§3.3):** las dependencias apuntan hacia `dominio`; `dominio` no importa FastAPI, SQLAlchemy ni gRPC; los modelos SQLAlchemy viven en `infraestructura` y se traducen con *mappers*; routers sin reglas de negocio; un caso de uso = una clase; un agregado (`Competencia`) por transacción; no existe `DELETE`.
4. **Sufijos (§3.4):** `CasoUso`, `Puerto`, `Repositorio`, `RepositorioSqlAlchemy`, `Adaptador`, `Controlador`, `Servicio`, `Excepcion`. Módulos en `snake_case` (`validar_clasificacion_caso_uso.py` define `ValidarClasificacionCasoUso`).
5. **Documentación (§3.2):** **docstrings estilo Google (PEP 257)** en todo módulo, clase y función pública (descripción, `Args:`, `Returns:`, `Raises:`). Cada endpoint documentado en Swagger/OpenAPI.
6. **Pruebas (§3.5):** pruebas unitarias de dominio **obligatorias** con pytest; cada invariante (INV-22, INV-23, INV-24) tiene al menos una prueba que la viola y comprueba el rechazo. Las pruebas de dominio no levantan base de datos ni servidor.
7. **Idioma (§3.1):** todo en español; identificadores sin tildes ni ñ; claves JSON en camelCase con alias de Pydantic.
8. **gRPC (§6):** el código se genera con `grpcio-tools` durante la construcción desde `/contratos/proto` y **nunca se versiona**.
9. **Docker (§2 y §9.3):** el `Dockerfile` va en esta carpeta, pero el contexto de construcción es la raíz del repositorio (`COPY contratos/proto ./contratos/proto`, `COPY servicio-catalogo/ ./`). Con el contexto en la raíz, Docker solo lee el `.dockerignore` de la raíz: no crees uno propio en esta carpeta; si falta excluir algo, pídelo por PR sobre el de la raíz.

## Estructura (sección 11.2)
Paquete raíz `catalogo`. FastAPI (uvicorn) y el servidor `grpc.aio` corren en el **mismo proceso** con asyncio.

```
servicio-catalogo/
├── Dockerfile                ← fase 1 (P3)
├── pyproject.toml            ← fase 1 (P3)
└── catalogo/
    ├── dominio/              ← agregado Competencia (entidades internas Tema y Subtema),
    │                           interfaces de repositorio, excepciones de dominio
    ├── aplicacion/           ← casos de uso (p. ej. ValidarClasificacionCasoUso), puertos, DTOs
    ├── infraestructura/      ← SQLAlchemy (modelos, repositorios, mappers) y siembra de datos (§4.3)
    └── interfaces/
        ├── rest/             ← routers FastAPI
        └── grpc/             ← servicer de CatalogoAcademico
```
La ubicación de las pruebas (`tests/` u otra) la decide el dueño en la fase 1.
