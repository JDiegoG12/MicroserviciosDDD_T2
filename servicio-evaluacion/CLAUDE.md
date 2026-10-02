# CLAUDE.md · servicio-evaluacion

## Qué es este servicio
- **Contexto (Taller 1):** Evaluación y Simulacros.
- **Dueño:** P2.
- **Tecnología:** TypeScript · Node 24 (v24.20.0) · NestJS 11 · MongoDB 7 (`bd-evaluacion`).
- **Puerto:** REST `8083` (`http://localhost:8083/api/v1`). Swagger en `/docs`, salud en `/salud`.
- **Produce:** REST de la sección 8.3 y el evento `IntentoDeSimulacroCalificado` (7.6).
- **Consume:** eventos `PreguntaPublicada` y `PreguntaArchivada` de la cola `evaluacion.preguntas` (sección 7).

## Antes de escribir código
**Antes de escribir código lee /CONTRATOS.md: secciones 1 a 10 y la sección 11.3 de este servicio.**
Ese documento es la fuente única de verdad: si algo de este archivo choca con él, manda CONTRATOS.md.

## Reglas
1. **No leas ni modifiques otros servicios** (`servicio-editorial`, `servicio-catalogo`). Todo lo que necesitas de ellos está en CONTRATOS.md y en `/contratos`. Este servicio no hace llamadas síncronas a otros.
2. **No modifiques `/contratos` ni `CONTRATOS.md`.** Si un contrato no alcanza, detente y repórtalo al equipo (se cambia por PR aprobado por al menos otro integrante).
3. **Clean Architecture (§3.3):** las dependencias apuntan hacia `dominio`; `dominio` no importa NestJS, Mongoose ni AMQP; los esquemas Mongoose viven en `infraestructura` y se traducen con *mappers*; controladores y consumidores sin reglas de negocio; un caso de uso = una clase; un agregado por transacción; el `_id` en Mongo es el UUID en texto (nunca `ObjectId`).
4. **Sufijos (§3.4):** `CasoUso`, `Puerto`, `Repositorio`, `RepositorioMongo`, `Adaptador`, `Controlador`, `Consumidor`, `Servicio`, `Excepcion`. Archivos en `kebab-case` (`crear-simulacro.caso-uso.ts`).
5. **Documentación (§3.2):** **TSDoc** en toda clase, interfaz, método y función exportados (descripción, `@param`, `@returns`, `@throws`). Cada endpoint documentado en Swagger/OpenAPI.
6. **Pruebas (§3.5):** pruebas unitarias de dominio **obligatorias** con Jest; cada invariante (INV-xx) tiene al menos una prueba que la viola y comprueba el rechazo. Las pruebas de dominio no levantan base de datos, broker ni servidor. Las pruebas del consumidor validan contra `/contratos/eventos/ejemplos`.
7. **Idioma (§3.1):** todo en español; identificadores sin tildes ni ñ; vocabulario del lenguaje ubicuo del Taller 1.
8. **Docker (§2 y §9.3):** el `Dockerfile` va en esta carpeta, pero el contexto de construcción es la raíz del repositorio (`COPY servicio-evaluacion/ ./`). Con el contexto en la raíz, Docker solo lee el `.dockerignore` de la raíz: no crees uno propio en esta carpeta; si falta excluir algo, pídelo por PR sobre el de la raíz.

## Estructura (sección 11.3)

```
servicio-evaluacion/
├── Dockerfile                ← etapa 2 (P2)
├── package.json              ← etapa 1 (P2), dependencias de etapa 2 agregadas
└── src/
    ├── dominio/              ← agregados Simulacro e IntentoDeSimulacro, PreguntaEvaluable,
    │                           servicios de dominio, interfaces de repositorio, excepciones
    ├── aplicacion/           ← casos de uso, puertos, DTOs
    ├── infraestructura/      ← repositorios Mongo, publicador RabbitMQ, configuración
    └── interfaces/
        ├── rest/             ← controladores
        └── mensajeria/       ← consumidores de RabbitMQ
```
Colecciones de Mongo: `preguntas_evaluables`, `simulacros`, `intentos`.
