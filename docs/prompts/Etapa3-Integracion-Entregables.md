# ETAPA 3 · Integración final y entregables (un solo agente, todo el monorepo)

Eres el agente que cierra el Taller 2. Los tres microservicios ya están implementados, auditados e integrados en `main`. Tu trabajo tiene tres partes:
1. cerrar los últimos pendientes menores de Editorial y Catálogo;
2. producir los **entregables** que faltan: colección de Postman, diagrama de arquitectura y README;
3. demostrar el sistema funcionando de punta a punta.

Tienes acceso a todo el repositorio.

## Contexto que debes leer (y solo esto)
- `CONTRATOS.md` (debe ser la **v1.11**; si no lo es, detente y avísame), en especial las secciones 1, 2, 4, 5, 6, 7, 8, 9 y 10.
- `MODELO-DOMINIO.md`, solo la Parte A.
- `docs/revision/ESTADO-PRE-ETAPA3.md`, si existe. Es la auditoría previa: todo lo que marca como `OK` **no se vuelve a verificar**.
- Del código, lee únicamente lo que necesites para lo que vas a cambiar o documentar. No hagas recorridos completos de los servicios.

## Reglas
1. **Git es tarea del integrante:** solo `git status`, `git diff` y `git log`. No ejecutes `add`, `commit`, `push`, `checkout`, `switch`, `stash`, `reset`, `merge` ni `rebase`. Trabaja sobre `main` actualizado; si la copia local no está al día con `origin/main`, detente y avísame.
2. **No cambies contratos:** `/contratos`, `CONTRATOS.md` y `MODELO-DOMINIO.md` no se tocan. Si encuentras una contradicción, anótala en el informe final.
3. **No cambies comportamiento de los servicios** salvo lo listado en la parte 1. Si el flujo de Postman revela un defecto real, corrígelo con el cambio mínimo, agrega su prueba y repórtalo en el informe.
4. Todo en español, legible y documentado.
5. **Economía de tokens (obligatorio):**
   - no vuelvas a auditar lo que ya está en `OK`;
   - ejecuta solo las pruebas de los archivos que modifiques;
   - construye y levanta los contenedores **una vez** al inicio (parte 0) y **una vez** al final (parte 5);
   - la prueba de punta a punta es la ejecución de la colección con Newman, sin recorridos manuales con `curl` que la dupliquen;
   - respuestas y resumen breves, con tablas.

## Parte 0 · Verificación rápida del estado integrado (una sola vez)
Evaluación se integró desde una rama que estaba desactualizada, así que primero confirma que `main` está sano:
1. `git log --oneline -5` y `git status`, para registrar el estado.
2. `scripts/validar-contratos.sh` (o `.ps1`) → todo OK.
3. `docker compose --profile servicios up -d --build` → los **7** contenedores quedan `healthy`.
4. En los tres servicios, `/salud` → 200.

Si algo falla, diagnostica la causa y corrígela con el cambio mínimo, por ejemplo un conflicto de fusión mal resuelto o un archivo duplicado entre `infraestructura/mensajeria` e `interfaces/mensajeria` en Evaluación. Repórtalo y continúa.

## Parte 1 · Pendientes menores de la auditoría
| # | Servicio | Qué hacer |
|---|---|---|
| 9 | Editorial | `CatalogoAcademicoGrpcAdaptadorTest`, el caso `ternaInvalida[1]`, falla en frío por el plazo de prueba de 300 ms. Estabilízalo: calienta el canal antes de los casos parametrizados o sube el plazo **de la prueba**, no el de producción, que sigue en 2 s. Ejecuta esa clase 3 veces seguidas. |
| 11 | Editorial | `GET /docs` responde 302 hacia `/swagger-ui/index.html`. Haz que `/docs` sirva la UI con un 200, por ejemplo configurando la ruta de springdoc o con un *forward* interno en lugar de una redirección. Si springdoc para Spring Boot 4 no lo permite, deja la redirección y documéntala en el README del servicio como decisión aceptada. |
| 12 | Editorial | Un JSON ilegible responde 400 con `errores` vacío. Agrega `{ "campo": "cuerpo", "mensaje": "El cuerpo de la solicitud no es un JSON válido" }`. Ajusta su prueba. |
| 13 | Editorial | En la raíz del repositorio están `DUDAS.md` (de Editorial) y `Etapa2b-Editorial-Ajustes.md`. Mueve `DUDAS.md` a `servicio-editorial/DUDAS.md` y los prompts que haya en la raíz (`Etapa*.md`) a `docs/prompts/`. |
| — | Editorial | Agrega `jacoco-maven-plugin` para reportar la cobertura en `mvn verify`, igual que los otros dos servicios. No exijas un umbral mínimo. |
| — | Catálogo | Completa el docstring de `CompetenciaRepositorioSqlAlchemy.guardar` (sección `Args:`). |

## Parte 2 · Colección de Postman (entregable, CONTRATOS 10)
Crea `postman/BancoPreguntas.postman_collection.json` (formato v2.1) que use el entorno existente `postman/local.postman_environment.json`. Si ese entorno no tiene alguna variable necesaria, agrégala: es la única modificación permitida a ese archivo.

**Reglas de la colección**
- Cada petición lleva sus encabezados `X-Usuario-Id`, `X-Roles` y `X-Id-Correlacion` (`{{$guid}}`), usando los usuarios de prueba de CONTRATOS 4.2 a través de variables.
- Cada petición tiene **pruebas** (`pm.test`) que verifican:
  - el código HTTP;
  - en los errores, el `codigo` del *problem+json*;
  - los campos clave de la respuesta.
- Los scripts guardan los ids que se generan (`preguntaId`, `procesoId`, `simulacroId`, `intentoId` y los que hagan falta) en variables de la colección o del entorno.
- **Debe poder ejecutarse varias veces seguidas** sobre la misma base de datos sin fallar: usa nombres únicos (por ejemplo con `{{$timestamp}}`) para lo que tenga nombre único y los ids semilla de 4.3 para la clasificación.
- **Espera del evento asíncrono:** después de publicar, la petición a `GET /preguntas-evaluables` reintenta (por ejemplo con `setTimeout` y `pm.execution.setNextRequest`, o un *pre-request* con reintentos) hasta encontrar la pregunta, con un máximo de unos 10 s.
- Descripciones en español en la colección, en cada carpeta y en cada petición: qué demuestra y qué CU o regla de CONTRATOS cubre.

**Carpetas** (los nombres de las cuatro primeras son exactos, según CONTRATOS 10)
1. **`01 Catálogo`**
   - listar competencias y comprobar la siembra de 4.3;
   - crear una competencia con un tema y un subtema;
   - renombrar;
   - errores: nombre duplicado 409, sin encabezados 401, rol `ESTUDIANTE` creando 403, ruta inexistente 404 `RECURSO_NO_ENCONTRADO`.
2. **`02 Editorial`**
   - crear una pregunta incompleta (`BORRADOR` con `erroresValidacion`);
   - crear una completa (`EN_CONSTRUCCION`, con validación gRPC real);
   - clasificación inválida 422 `CLASIFICACION_INVALIDA`;
   - modificar;
   - consultar con filtros;
   - errores: modificar una pregunta no editable 409, transición inválida 409, revisores insuficientes 422, autor como revisor 422.
3. **`03 Evaluación`**
   - `GET /preguntas-evaluables`;
   - definir un simulacro;
   - error 422 `PREGUNTAS_INSUFICIENTES`;
   - error 422 `DURACION_INVALIDA`;
   - consultar un intento como `DOCENTE`.
4. **`04 Flujo completo`**, en orden y ejecutable de corrido. Es la demostración principal:
   1. Verificar la siembra del catálogo.
   2. Crear **2** preguntas completas. Para cada una: enviar a revisión, asignar 2 revisores, registrar 2 evaluaciones aprobatorias (dictamen `APROBADA`) y publicar.
   3. Esperar a que ambas aparezcan en `GET /preguntas-evaluables` (evento `PreguntaPublicada` por RabbitMQ).
   4. Definir un simulacro de 2 preguntas.
   5. Iniciar un intento como `ESTUDIANTE`, responder una bien y otra mal, y finalizar → `CALIFICADO` con puntaje 50.
   6. Archivar una pregunta en Editorial → esperar a que Evaluación la muestre `ARCHIVADA` → un simulacro nuevo de 2 preguntas con los mismos criterios → 422 `PREGUNTAS_INSUFICIENTES`.
   7. Consultar la trazabilidad de la pregunta archivada (registros en orden, hasta `ARCHIVADA`).

   Usa criterios de simulacro que solo incluyan las preguntas creadas en esta ejecución. Por ejemplo, crea en el paso 1 un subtema nuevo con nombre único y clasifica ahí las preguntas, para que las ejecuciones anteriores no interfieran.
5. **`05 Flujo de rechazo`**
   - una pregunta con 1 evaluación aprobatoria y 1 reprobatoria → dictamen `RECHAZADA` → la pregunta vuelve a `EN_CONSTRUCCION` (D-07);
   - se puede modificar de nuevo;
   - el historial conserva el proceso anterior.
6. **`06 Casos comunes`** (una petición por caso, contra un solo servicio por caso):
   - `X-Roles` vacío 401, rol desconocido 400;
   - UUID en mayúsculas aceptado, UUID con llaves 400;
   - JSON ilegible 400, 405 `METODO_NO_PERMITIDO`, 415 `TIPO_DE_CONTENIDO_NO_SOPORTADO`;
   - `tamano=101` 400;
   - `X-Id-Correlacion` inválido → la respuesta trae uno nuevo.

**No incluyas en la colección** el 409 por concurrencia ni el vencimiento por tiempo: ya están cubiertos por pruebas automáticas de los servicios. Menciónalo en la descripción de la colección, indicando qué pruebas los cubren (búscalas por nombre).

**gRPC:** la colección v2.1 no exporta peticiones gRPC. En la descripción de la colección, remite a la sección de grpcurl del README. La parte 4 verifica que esa sección esté completa.

**Ejecución con Newman** (sin instalar nada, con Docker y en la red del compose):
```
docker run --rm --network red-banco-preguntas -v "<ruta>/postman:/etc/newman" postman/newman:6-alpine run BancoPreguntas.postman_collection.json -e local.postman_environment.json --env-var urlEditorial=http://servicio-editorial:8081/api/v1 --env-var urlCatalogo=http://servicio-catalogo:8082/api/v1 --env-var urlEvaluacion=http://servicio-evaluacion:8083/api/v1 --reporters cli,json --reporter-json-export resultados/newman-resultado.json
```
Deja ese comando, con la versión para PowerShell, en `postman/README.md` y en el README raíz. El reporte se guarda en `postman/resultados/`: es la evidencia del entregable "Pruebas del funcionamiento en Postman".

## Parte 3 · Diagrama de arquitectura (entregable)
En `docs/arquitectura/`:
1. `arquitectura.mmd`: un diagrama Mermaid de **contenedores**, al estilo C4, que muestre:
   - el cliente (Postman);
   - los 3 microservicios, con su tecnología y puertos;
   - sus 3 bases de datos;
   - las flechas **REST** (cliente → servicios);
   - **gRPC** (`servicio-editorial` → `servicio-catalogo`, `CatalogoAcademico.ValidarClasificacion`);
   - **RabbitMQ**, con los exchanges `editorial.eventos` y `evaluacion.eventos`, la cola `evaluacion.preguntas`, la DLQ y los eventos `PreguntaPublicada`, `PreguntaArchivada` e `IntentoDeSimulacroCalificado`;
   - la capa interna (Clean Architecture) de cada servicio, de forma resumida.
2. `flujo-principal.mmd`: diagrama de secuencia del flujo de la carpeta `04 Flujo completo`.
3. Exporta ambos a **PNG y SVG** con mermaid-cli en Docker (por ejemplo `minlag/mermaid-cli`), sin instalar nada. Revisa que las imágenes se vean bien: textos legibles y flechas sin cruces confusos.
4. Actualiza `docs/arquitectura/README.md`: incrusta las imágenes, explica cada elemento en pocas líneas (qué hace cada servicio, por qué gRPC hacia Catálogo y por qué un evento hacia Evaluación, citando el context map del Taller 1) y elimina el `TODO`.

## Parte 4 · README raíz (entregable)
Reescribe `README.md` sin ningún `TODO`, en español, con estas secciones:
1. **Descripción:** el sistema, los 3 microservicios, sus contextos del Taller 1 y su tecnología (Spring Boot 4.1.1 · Java 21; FastAPI · Python 3.12; NestJS 11 · Node 24).
2. **Arquitectura:** la imagen del diagrama y un enlace a `docs/arquitectura/`.
3. **Requisitos:** Docker y Docker Compose.
4. **Cómo ejecutar:** `cp .env.example .env` y `docker compose --profile servicios up -d --build`, cómo comprobar que todo está `healthy` y cómo detener sin borrar datos.
5. **URLs y puertos:** tabla con REST, Swagger (`/docs`), salud, gRPC (50051) y la consola de RabbitMQ (15672, `banco`/`banco123`).
6. **Cómo probar:**
   - Postman (importar la colección y el entorno, orden de ejecución);
   - Newman (los comandos de la parte 2);
   - gRPC con grpcurl (comprueba que los comandos existentes funcionan y corrígelos si no);
   - publicar eventos de ejemplo con el script de Evaluación;
   - el validador de contratos.
7. **Comunicación entre microservicios:** tabla de los contratos C1 a C4 de CONTRATOS 1, con enlaces a `/contratos`.
8. **Estructura del repositorio:** un árbol resumido.
9. **Documentación del proyecto:** enlaces a `CONTRATOS.md`, `MODELO-DOMINIO.md`, el README de cada servicio y los `DUDAS.md`.
10. **Equipo:** la tabla de dueños de CONTRATOS 1 (`@juanvec06`, `@JDiegoG12`, `@JuanDv1`).
11. **Problemas comunes:** puertos ocupados, contenedores que no quedan `healthy`, cómo ver los logs y cómo revisar la DLQ.

## Parte 5 · Verificación final (una sola vez)
1. Las pruebas de **solo** los archivos modificados en la parte 1, y la clase gRPC de Editorial 3 veces.
2. `docker compose --profile servicios up -d --build` → 7/7 `healthy`.
3. Newman con la colección completa **dos veces seguidas** (para demostrar que se puede repetir) → 0 fallas en ambas. Si algo falla, corrige la colección o, si es un defecto real del servicio, el código, según la regla 3.
4. `docker compose --profile servicios down`, **sin** `-v`.

## Resumen final (breve)
- Tabla de la parte 1: ítem → hecho / no hecho → evidencia en una línea.
- Resultado de la parte 0 (estado integrado) y de cualquier corrección que necesitó.
- Newman: peticiones, pruebas aprobadas y fallidas en cada una de las dos ejecuciones, y la ruta del reporte JSON.
- Archivos de los entregables: colección, diagramas (`.mmd`, `.png` y `.svg`) y README.
- Defectos reales encontrados y corregidos, si hubo.
- Contradicciones con CONTRATOS, si hubo.
- Lista de archivos creados o modificados para el commit.
