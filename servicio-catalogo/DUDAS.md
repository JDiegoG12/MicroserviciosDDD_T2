# DUDAS.md · servicio-catalogo

Puntos en que CONTRATOS.md o MODELO-DOMINIO.md no alcanzaban. Cada duda se resolvió en una versión posterior de CONTRATOS.md y el código ya la aplica: los comentarios `# DUDA:` se reemplazaron por un comentario que cita la regla.

## Dudas abiertas

Ninguna.

## Dudas resueltas (etapa 2)

| # | Duda | Resuelta en | Decisión | Dónde se aplica |
|---|---|---|---|---|
| 1 | Código de error para rutas inexistentes (404) y métodos no permitidos (405) | CONTRATOS 5.3 (v1.10 y v1.11) | `RECURSO_NO_ENCONTRADO` (404), `METODO_NO_PERMITIDO` (405) y `TIPO_DE_CONTENIDO_NO_SOPORTADO` (415) en *problem+json*, iguales en los tres servicios. Reemplazan al nombre local `RUTA_NO_ENCONTRADA` | `ESTADO_Y_TITULO_POR_CODIGO` en `interfaces/rest/errores.py`; el 415 en `interfaces/rest/middleware_correlacion.py` |
| 2 | `X-Id-Correlacion` que no es UUID | CONTRATOS 4.1 (v1.10) | Se genera uno nuevo (UUID v4) y se registra un **aviso** en el log; nunca es error | `normalizar_id_correlacion` en `infraestructura/correlacion.py` |
| 3 | `Location` de `POST …/temas` y `POST …/subtemas` | CONTRATOS 8.2 (v1.11) | Apunta a `/api/v1/competencias/{competenciaId}`, porque temas y subtemas no tienen un `GET` propio. Está documentado en Swagger | `interfaces/rest/routers/competencias.py` |
| 4 | Estado gRPC cuando la base de datos no responde | CONTRATOS 6 (v1.11) | Base de datos caída o esquema aún no listo → `UNAVAILABLE` con mensaje en español; error inesperado → `INTERNAL` sin detalles internos. Editorial traduce ambos a 503 `CATALOGO_NO_DISPONIBLE` | `interfaces/grpc/catalogo_academico_servicer.py` |
| 5 | Comentarios desactualizados (Spring Boot 3, Node 20, NestJS 10) | CONTRATOS 1, 1.3 y 1.4 | Corregidos a Spring Boot 4.1.1 y Node 24 · NestJS 11 en `docker-compose.yml`, `README.md` y `CLAUDE.md` de la raíz | Documentación compartida |

Además, el arranque sin base de datos quedó alineado con CONTRATOS 9.3.6 (v1.10): las migraciones y la siembra corren en segundo plano con reintentos (ver el README del servicio).

## Dudas resueltas (etapa 1, CONTRATOS v1.9)

| # | Duda de la etapa 1 | Decisión de v1.9 | Dónde se aplica |
|---|---|---|---|
| 1 | La `ñ` perdía su tilde al normalizar | La `ñ` se conserva como letra propia (`Año` ≠ `Ano`) | `NombreCatalogo.normalizado` |
| 2 | `PUT` sin `descripcion` | Ausente: se conserva; `null` o `""`: se borra; con texto: se reemplaza | `Competencia.renombrar`, `SinCambio`, router (`model_fields_set`) |
| 3 | Renombrar con una variante que se normaliza igual | Se actualiza el texto guardado (y se emite el evento); si el texto es idéntico no cambia nada | `Competencia.renombrar`, `renombrar_tema`, `Tema._renombrar_subtema` |
| 4 | La siembra no era atómica | Una sola transacción (todo o nada) | `EjecutorTransaccionalSqlAlchemy`, `SembrarCatalogoCasoUso` |
| 5 | Lecturas sin ningún rol | `X-Roles` vacío es 401 `NO_AUTENTICADO`; rol desconocido es 400 | `obtener_usuario_actual` |
| 6 | Formas de UUID aceptadas | Solo la forma canónica; la mayúscula se normaliza | `_IdentificadorUuid.desde_texto`, `obtener_usuario_actual` |
