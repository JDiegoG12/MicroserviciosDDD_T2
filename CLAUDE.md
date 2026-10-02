# CLAUDE.md · Monorepo Banco de Preguntas Saber Pro (Taller 2)

## Qué es este repositorio
Monorepo del Taller 2 de Arquitectura de Microservicios. Contiene tres microservicios que implementan los contextos delimitados del Taller 1, sus contratos compartidos y la infraestructura común:

| Carpeta | Servicio | Dueño | Tecnología | Puertos |
|---|---|---|---|---|
| `servicio-editorial/` | Gestión Editorial de Preguntas | P1 | Java 21 · Spring Boot 4.1.1 · PostgreSQL | 8081 |
| `servicio-catalogo/` | Catálogo Académico | P3 | Python 3.12 · FastAPI · PostgreSQL | 8082 (REST) · 50051 (gRPC) |
| `servicio-evaluacion/` | Evaluación y Simulacros | P2 | TypeScript · Node 24 · NestJS 11 · MongoDB | 8083 |

Otras carpetas: `contratos/` (proto y esquemas de eventos), `docs/arquitectura/` (diagrama entregable), `postman/` (colección y entorno), `scripts/` (validación de contratos).

## Antes de hacer cualquier cosa
1. Lee **/CONTRATOS.md**: es la fuente única de verdad (nombres, rutas, puertos, variables, colas, eventos y `.proto`). Si algo choca con él, manda CONTRATOS.md.
2. Si vas a trabajar en un servicio, lee además el `CLAUDE.md` de esa carpeta:
   - [servicio-editorial/CLAUDE.md](servicio-editorial/CLAUDE.md)
   - [servicio-catalogo/CLAUDE.md](servicio-catalogo/CLAUDE.md)
   - [servicio-evaluacion/CLAUDE.md](servicio-evaluacion/CLAUDE.md)

## Reglas del repositorio
- **`/contratos`, `CONTRATOS.md`, `docker-compose.yml`, `/postman` y `/scripts` solo se cambian por PR aprobado por al menos otro integrante** (ver `.github/CODEOWNERS`; no se exige la aprobación de los tres). No los modifiques por iniciativa propia: si un contrato no alcanza, detente y repórtalo.
- Cada integrante solo modifica la carpeta de su servicio. No leas ni modifiques el código de otro servicio.
- Todo en español (código, comentarios, documentación, commits). Identificadores sin tildes ni ñ.
- Commits convencionales en español: `feat(editorial): ...`, `fix(catalogo): ...`, `docs(contratos): ...`. Ramas `main` y `feature/<servicio>-<tema>`.
- El contexto de construcción Docker de los tres servicios es la **raíz** del repositorio.
- Después de tocar `/contratos`, ejecuta `scripts/validar-contratos.sh` (o `.ps1` en Windows).
