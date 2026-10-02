# Postman · Banco de Preguntas Saber Pro (Taller 2)

| Archivo | Contenido |
|---|---|
| [BancoPreguntas.postman_collection.json](BancoPreguntas.postman_collection.json) | Colección (formato v2.1) con 6 carpetas y pruebas `pm.test` en cada petición. |
| [local.postman_environment.json](local.postman_environment.json) | Entorno: URLs locales, usuarios de prueba (CONTRATOS.md 4.2) y clasificación semilla (4.3). |
| `resultados/` | Reportes JSON de Newman (`newman-resultado.json`). |

## Carpetas

| Carpeta | Qué demuestra |
|---|---|
| `01 Catálogo` | Siembra 4.3, crear competencia → tema → subtema, renombrar, 409 `NOMBRE_DUPLICADO`, 401, 403, 404. |
| `02 Editorial` | BORRADOR con `erroresValidacion`, EN_CONSTRUCCION con validación gRPC real, 422 `CLASIFICACION_INVALIDA`, modificar, filtros, 409 `TRANSICION_NO_PERMITIDA` y `PREGUNTA_NO_EDITABLE`, 422 `REVISORES_INSUFICIENTES` y `AUTOR_NO_PUEDE_SER_REVISOR`. |
| `03 Evaluación` | Copia local por evento, simulacro, 422 `PREGUNTAS_INSUFICIENTES` y `DURACION_INVALIDA`, intento consultado por un DOCENTE. |
| `04 Flujo completo` | Demostración de punta a punta (CONTRATOS.md 10): 2 preguntas publicadas → simulacro → intento CALIFICADO con puntaje 50 → archivado propagado → 422 → trazabilidad. |
| `05 Flujo de rechazo` | Dictamen RECHAZADA (1 de 2) → vuelve a EN_CONSTRUCCION (D-07) → se modifica → el historial conserva el proceso. |
| `06 Casos comunes` | X-Roles vacío 401, rol desconocido 400, UUID en mayúsculas, UUID con llaves 400, JSON ilegible 400, 405, 415, `tamano=101` 400, X-Id-Correlacion inválido. |

Ejecute la colección **completa y en orden**: la carpeta 03 usa la competencia creada en 01 y la 06 usa la pregunta creada en 02. Se puede repetir sobre la misma base de datos (los nombres llevan marca de tiempo y el flujo 04 crea su propia clasificación). Los eventos son asíncronos: las peticiones «Esperar…» reintentan cada 500 ms hasta ~10 s.

Quedan fuera de la colección, cubiertos por pruebas automáticas: el 409 `CONFLICTO_DE_CONCURRENCIA` (`servicio-editorial`: `PersistenciaIT.modificacionesConcurrentes`, `ApiErroresTest.conflictoDeConcurrencia`) y el vencimiento por tiempo de un intento (`servicio-evaluacion`: `test/dominio/intento-de-simulacro.spec.ts` y los `*.caso-uso.spec.ts` de finalizar, obtener y registrar respuesta). La llamada gRPC directa se prueba con `grpcurl` (README raíz, sección «Cómo probar»).

## Postman (interfaz gráfica)

1. Levante el sistema: `docker compose --profile servicios up -d --build` y espere 7 contenedores `healthy`.
2. Importe los dos archivos de esta carpeta y seleccione el entorno **local**.
3. Ejecute la colección con el *Collection Runner* (o carpeta por carpeta, en orden).

## Newman (línea de comandos, sin instalar nada)

Newman corre en un contenedor dentro de la red `red-banco-preguntas`, por eso las URLs apuntan a los nombres de los servicios. Desde la raíz del repositorio:

```bash
# Linux, macOS o Git Bash (en Git Bash agrega antes: export MSYS_NO_PATHCONV=1)
docker run --rm --network red-banco-preguntas -v "$PWD/postman:/etc/newman" postman/newman:6-alpine \
  run BancoPreguntas.postman_collection.json -e local.postman_environment.json \
  --env-var urlEditorial=http://servicio-editorial:8081/api/v1 \
  --env-var urlCatalogo=http://servicio-catalogo:8082/api/v1 \
  --env-var urlEvaluacion=http://servicio-evaluacion:8083/api/v1 \
  --reporters cli,json --reporter-json-export resultados/newman-resultado.json
```

```powershell
# Windows (PowerShell)
docker run --rm --network red-banco-preguntas -v "${PWD}/postman:/etc/newman" postman/newman:6-alpine `
  run BancoPreguntas.postman_collection.json -e local.postman_environment.json `
  --env-var urlEditorial=http://servicio-editorial:8081/api/v1 `
  --env-var urlCatalogo=http://servicio-catalogo:8082/api/v1 `
  --env-var urlEvaluacion=http://servicio-evaluacion:8083/api/v1 `
  --reporters cli,json --reporter-json-export resultados/newman-resultado.json
```

El resumen final de Newman debe mostrar `failed 0` en *requests*, *test-scripts* y *assertions*. El reporte queda en `postman/resultados/newman-resultado.json`.
