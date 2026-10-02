# DUDAS.md · Dudas de `servicio-editorial`

Las 14 dudas de la etapa 1 se resolvieron en CONTRATOS.md v1.8 y se aplicaron en el paso 0 de la etapa 2.
Las 6 dudas de la etapa 2 se resolvieron en CONTRATOS.md v1.9 y v1.10 y se aplicaron en la etapa 2b.

## Dudas abiertas
Ninguna. No queda ningún comentario `DUDA:` en el código de `servicio-editorial`.

## Dudas resueltas (etapa 2)
Las rutas son relativas a `servicio-editorial/src/main/` y `java/...` = `java/co/edu/unicauca/bancopreguntas/editorial`.

| # | Duda | Decisión | Resuelta en CONTRATOS.md | Dónde quedó aplicada |
|---|---|---|---|---|
| 1 | Arranque sin base de datos con `ddl-auto=validate` | Manda 9.3.6: `ddl-auto=none` en ejecución, Flyway en segundo plano con reintentos (máximo 30 s), 503 `BASE_DE_DATOS_NO_DISPONIBLE` mientras el esquema no esté listo; la validación del esquema pasa a las pruebas de integración. | 9.3.6 (v1.10) | `resources/application.yml`; `java/.../infraestructura/persistencia/MigradorEsquemaEnSegundoPlano.java` y `EstadoDelEsquema.java` (nuevos); `java/.../interfaces/rest/seguridad/InterceptorEsquemaListo.java` (nuevo); perfil de pruebas `src/test/resources/application-pruebas.yml`; prueba `ArranqueSinBaseDeDatosIT` |
| 2 | Código del conflicto de bloqueo optimista | 409 `CONFLICTO_DE_CONCURRENCIA`; el cliente puede reintentar. Documentado en Swagger en los endpoints que modifican una `Pregunta` o un `ProcesoDeRevision`. | 5.3 (v1.10) | `java/.../interfaces/rest/errores/ManejadorGlobalErrores.java` (`conflictoDeConcurrencia`), `MapaCodigosHttp.java`, `interfaces/rest/openapi/ConfiguracionOpenApi.java` |
| 3 | Códigos de los errores de protocolo | 404 `RECURSO_NO_ENCONTRADO`, 405 `METODO_NO_PERMITIDO`, 415 `TIPO_DE_CONTENIDO_NO_SOPORTADO`; cualquier otro conserva el estado de Spring con `SOLICITUD_INVALIDA`. | 5.3 (v1.10, confirmada en v1.11) | `java/.../interfaces/rest/errores/ManejadorGlobalErrores.java` (`deProtocolo`) |
| 4 | `X-Id-Correlacion` mal formado | Se acepta la decisión: se genera uno nuevo (UUID v4) y se registra un aviso; nunca es error. | 4.1 (v1.10) | `java/.../interfaces/rest/seguridad/FiltroCorrelacion.java` |
| 5 | Rol desconocido en `X-Roles` | Se acepta la decisión y se completa: rol desconocido → 400 `SOLICITUD_INVALIDA`; `X-Roles` vacío → 401 `NO_AUTENTICADO`. | 4.1 (v1.9) | `java/.../interfaces/rest/seguridad/ResolutorUsuarioActual.java` |
| 6 | Comentario "Spring Boot 3" en `docker-compose.yml` | Lo corrige el integrante a mano (archivo compartido). | — (no es del agente) | `docker-compose.yml:142` |

### Ajuste adicional de la etapa 2b (sin duda previa)
- **UUID de entrada** (CONTRATOS.md 4, v1.9): solo la forma canónica de 36 caracteres con guiones; las mayúsculas se aceptan y se normalizan a minúsculas; llaves, `urn:uuid:` o sin guiones → 400; no se exige la versión 4.
  - Aplicado en un solo punto: `java/.../dominio/modelo/comun/Validaciones.java` (`aUuid`). Ese método lo usan los parámetros de ruta, `X-Usuario-Id`, `revisoresIds`, `clasificacion` y los filtros.
