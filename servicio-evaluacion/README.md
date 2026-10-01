# servicio-evaluacion

Microservicio de **Evaluación y Simulacros** (dueño: P2 · TypeScript · Node 20 · NestJS 10 · MongoDB).

Mantiene una copia local de las preguntas publicadas (a partir de los eventos `PreguntaPublicada` y `PreguntaArchivada`), permite a los docentes definir simulacros y a los estudiantes presentarlos; al calificar un intento publica `IntentoDeSimulacroCalificado`.

- REST: `http://localhost:8083/api/v1` · Swagger: `/docs` · Salud: `/salud`
- Contratos: [CONTRATOS.md](../CONTRATOS.md) secciones 7, 8.3 y 11.3.

> **Implementación pendiente (fase 1).**
