# CLAUDE.md · servicio-editorial

## Qué es este servicio
- **Contexto (Taller 1):** Gestión Editorial de Preguntas.
- **Dueño:** P1.
- **Tecnología:** Java 21 · Spring Boot 4.1.1 (Spring Framework 7, Jackson 3) · Maven · PostgreSQL 16 (`bd-editorial`).
- **Puerto:** REST `8081` (`http://localhost:8081/api/v1`). Swagger en `/docs`, salud en `/salud`.
- **Produce:** REST de la sección 8.1 y los eventos `PreguntaPublicada` y `PreguntaArchivada` (7.4, 7.5).
- **Consume:** gRPC `CatalogoAcademico.ValidarClasificacion` (sección 6), como cliente.

## Antes de escribir código
**Antes de escribir código lee /CONTRATOS.md: secciones 0 a 10, la 12 y la 11.1 de este servicio.**
Lee también **/MODELO-DOMINIO.md**: la Parte A completa y, de la Parte B, lo que su tabla indica para `servicio-editorial` (INV-01 a INV-21, D-01 a D-07, D-10, D-13 a D-15, CU-04 a CU-12 y CU-18).
CONTRATOS.md es la fuente única de verdad: si algo de este archivo choca con él, manda CONTRATOS.md.

## Reglas
1. **No leas ni modifiques otros servicios** (`servicio-catalogo`, `servicio-evaluacion`). Todo lo que necesitas de ellos está en CONTRATOS.md y en `/contratos`.
2. **No modifiques `/contratos` ni `CONTRATOS.md`.** Si un contrato no alcanza, detente y repórtalo al equipo (se cambia por PR aprobado por al menos otro integrante).
3. **Clean Architecture (§3.3):** las dependencias apuntan hacia `dominio`; `dominio` no importa Spring, JPA, AMQP ni gRPC; entidades de dominio sin anotaciones de persistencia (las `@Entity` viven en `infraestructura` con *mappers*); controladores sin reglas de negocio; un caso de uso = una clase; un agregado por transacción (salvo la excepción documentada de 11.1 para `AsignarRevisoresCasoUso` y `RegistrarEvaluacionCasoUso`); los eventos se traducen a JSON en `infraestructura` y se publican después del commit; nunca se borra físicamente una Pregunta.
4. **Sufijos (§3.4):** `CasoUso`, `Puerto`, `Repositorio`, `RepositorioJpa`, `Adaptador`, `Controlador`, `Consumidor`, `Servicio`, `Excepcion`.
5. **Documentación (§3.2):** **JavaDoc** en toda clase pública y todo método público (propósito, `@param`, `@return`, `@throws` y la regla RF/INV/D/CU que implementa). Cada endpoint documentado en Swagger/OpenAPI.
6. **Pruebas (§3.5):** pruebas unitarias de dominio **obligatorias** con JUnit 5; cada invariante (INV-xx) tiene al menos una prueba que la viola y comprueba el rechazo. Las pruebas de dominio no levantan base de datos, broker ni servidor.
7. **Idioma (§3.1):** todo en español; identificadores sin tildes ni ñ; vocabulario del lenguaje ubicuo del Taller 1.
8. **Docker (§2 y §9.3):** el `Dockerfile` va en esta carpeta, pero el contexto de construcción es la raíz del repositorio (`COPY contratos/proto ./contratos/proto`, `COPY servicio-editorial/ ./`). Con el contexto en la raíz, Docker solo lee el `.dockerignore` de la raíz: no crees uno propio en esta carpeta; si falta excluir algo, pídelo por PR sobre el de la raíz.

## Estructura (sección 11.1)
Paquete raíz `co.edu.unicauca.bancopreguntas.editorial` con los subpaquetes de Clean Architecture:

```
servicio-editorial/
├── Dockerfile                       ← fase 1 (P1)
├── pom.xml                          ← fase 1 (P1); genera el código gRPC desde ../contratos/proto
└── src/
    ├── main/java/co/edu/unicauca/bancopreguntas/editorial/
    │   ├── dominio/                 ← agregados Pregunta y ProcesoDeRevision, value objects,
    │   │                              servicios de dominio, eventos de dominio,
    │   │                              interfaces de repositorio, excepciones de dominio
    │   ├── aplicacion/              ← casos de uso, puertos de entrada y salida, DTOs
    │   ├── infraestructura/         ← repositorios JPA, publicador RabbitMQ, cliente gRPC, configuración
    │   └── interfaces/              ← controladores REST
    └── test/java/co/edu/unicauca/bancopreguntas/editorial/
        └── dominio/                 ← pruebas unitarias de dominio (JUnit 5)
```
