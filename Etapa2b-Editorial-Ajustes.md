# ETAPA 2b · servicio-editorial · Ajustes tras la revisión de la etapa 2

Eres el agente responsable de **servicio-editorial** (Java 21 · Spring Boot 4.1.1). La etapa 2 quedó construida y verificada. El equipo revisó las 6 dudas de `servicio-editorial/DUDAS.md` (etapa 2) y decidió los ajustes de abajo. Las decisiones están en **CONTRATOS.md v1.10**: secciones 4.1, 5.3, 9.3.6 y el historial 13 → 1.9 y 1.10. Léelas antes de empezar.

## Reglas (las mismas de siempre)
- Trabaja solo en `servicio-editorial/`. Puedes leer `/contratos`, pero no modificarlo. No toques `CONTRATOS.md`, `MODELO-DOMINIO.md`, `docker-compose.yml` ni `/postman`.
- **Git es tarea del integrante:** solo `git status`, `git diff` y `git log`. Deja los cambios sin preparar.
- Todo en español, con JavaDoc, y legible. Cambios mínimos y precisos: no reescribas lo que ya funciona.
- Cada `DUDA:` resuelta se **borra** y se reemplaza por un comentario que cite la regla (por ejemplo `// CONTRATOS 5.3: CONFLICTO_DE_CONCURRENCIA`). Al final, mueve las 6 dudas de `DUDAS.md` a una sección "Dudas resueltas (etapa 2)" con la sección de CONTRATOS que resuelve cada una.

## Ajuste 1 · El servicio arranca sin base de datos (CONTRATOS 9.3.6 v1.10)
El equipo decidió que **manda 9.3.6**: el servicio no puede depender de PostgreSQL para arrancar, igual que Evaluación. Se descarta `restart: unless-stopped` como solución.
1. **Hibernate:** en ejecución, `spring.jpa.hibernate.ddl-auto=none`. Fija el dialecto de PostgreSQL de forma explícita y evita que Hibernate abra una conexión al arrancar solo para detectar metadatos. Hibernate 7 tiene una propiedad para esto (busca en la documentación de Hibernate y de Spring Boot 4 el equivalente de `hibernate.boot.allow_jdbc_metadata_access=false`); usa la que corresponda a la versión que trae Boot 4.1.1.
2. **Pool de conexiones:** configura HikariCP para que no falle si la base de datos no responde al iniciar (por ejemplo, `initialization-fail-timeout` negativo).
3. **Flyway en segundo plano:**
   - desactiva la migración automática de Spring al arrancar;
   - crea un componente de infraestructura (por ejemplo `MigradorEsquemaEnSegundoPlano`) que, tras el arranque, ejecute `Flyway.migrate()` con reintentos y espera progresiva (máximo 30 s entre intentos);
   - expón su estado (`esquemaListo`) para que el resto del servicio lo consulte;
   - escribe cada intento y el resultado final en el log.
4. **Endpoints sin base de datos o sin esquema:**
   - mientras la base de datos no responda **o** las migraciones no hayan terminado, los endpoints que usan persistencia responden **503** `BASE_DE_DATOS_NO_DISPONIBLE` en *problem+json*;
   - `/salud`, `/docs` y `/openapi.json` siguen en 200;
   - reutiliza el manejo que ya tienes en `ManejadorGlobalErrores.baseDeDatosCaida` y agrega la verificación de `esquemaListo`, por ejemplo con un filtro o interceptor para `/api/v1/**`.
5. **Validación del esquema:** queda en la prueba de integración `PersistenciaIT`, que ya existe. Asegúrate de que esa prueba ejecute las migraciones y valide las entidades contra el esquema, por ejemplo levantando el contexto con `ddl-auto=validate` **solo en el perfil de pruebas**.
6. **Pruebas nuevas:**
   - de API: con `esquemaListo=false` → 503 `BASE_DE_DATOS_NO_DISPONIBLE`;
   - de integración (Testcontainers): el contexto arranca con el contenedor de PostgreSQL **detenido**; al iniciarlo, el migrador aplica Flyway y los endpoints pasan de 503 a responder normalmente.
7. Actualiza `resources/application.yml` y el README del servicio explicando el arranque.

## Ajuste 2 · Conflicto de bloqueo optimista → `CONFLICTO_DE_CONCURRENCIA`
- Cambia `ManejadorGlobalErrores.conflictoDeConcurrencia` para que responda **409** con `codigo = CONFLICTO_DE_CONCURRENCIA`, en lugar de `TRANSICION_NO_PERMITIDA`. El `detail` en español debe indicar que otra operación modificó el recurso y que puede reintentar.
- Prueba: dos actualizaciones concurrentes de la misma pregunta (o la misma versión cargada dos veces) → una responde OK y la otra 409 `CONFLICTO_DE_CONCURRENCIA`.
- Documenta el 409 en Swagger en los endpoints que modifican `Pregunta` o `ProcesoDeRevision`.

## Ajuste 3 · Errores de protocolo con código propio
En `ManejadorGlobalErrores.deProtocolo` (CONTRATOS 5.3 v1.10):
- ruta inexistente → 404 `RECURSO_NO_ENCONTRADO`;
- método no permitido → 405 `METODO_NO_PERMITIDO`;
- tipo de contenido no soportado → 415 `TIPO_DE_CONTENIDO_NO_SOPORTADO`.

Todos van en *problem+json*, con `idCorrelacion`. Cualquier otro error de protocolo no previsto sigue como `SOLICITUD_INVALIDA` con el estado que propone Spring. Agrega una prueba de API por cada caso.

## Ajuste 4 · `X-Id-Correlacion` mal formado: se acepta tu decisión
Ya está en CONTRATOS 4.1 v1.10: se genera uno nuevo y se registra un aviso. Solo reemplaza el comentario `DUDA:` por la referencia a la regla. Verifica que exista una prueba de este caso; si no existe, agrégala.

## Ajuste 5 · Rol desconocido: se acepta tu decisión, y se completa
Está en CONTRATOS 4.1 desde la v1.9: un rol desconocido → 400 `SOLICITUD_INVALIDA`, y **`X-Roles` vacío → 401** `NO_AUTENTICADO`. Reemplaza el `DUDA:` por la referencia. Verifica ambos casos con pruebas.

## Ajuste 6 · Alineación con CONTRATOS v1.9 (UUID)
CONTRATOS 4 v1.9 fija las formas de UUID aceptadas en la entrada:
- solo la forma canónica de 36 caracteres con guiones;
- si llega en mayúsculas, se acepta y se normaliza a minúsculas;
- cualquier otra forma (llaves, sin guiones, `urn:uuid:`) → 400;
- no se exige la versión 4.

Verifica esto en los parámetros de ruta, en los encabezados (`X-Usuario-Id`) y en los cuerpos (`revisoresIds`, `clasificacion`). Ajusta y prueba si hace falta. La duda 6 (el comentario en `docker-compose.yml`) **no** es tuya: la corrige el integrante a mano.

## Verificación final
1. Todas las pruebas: unitarias, de API e integración. Una búsqueda confirma que no queda ninguna `DUDA:` en el código.
2. `docker compose --profile servicios build servicio-editorial`.
3. **Prueba de humo sin base de datos:**
   - `docker compose stop bd-editorial`;
   - reinicia `servicio-editorial`: debe quedar `healthy` **sin reiniciarse en bucle**;
   - `GET /api/v1/preguntas` (con encabezados válidos) → 503 `BASE_DE_DATOS_NO_DISPONIBLE`;
   - `docker compose start bd-editorial`: en pocos segundos el log muestra la migración aplicada y `GET /api/v1/preguntas` responde 200.
4. Prueba de humo de los códigos nuevos con `curl`: 404 `RECURSO_NO_ENCONTRADO`, 405 `METODO_NO_PERMITIDO` y 415 `TIPO_DE_CONTENIDO_NO_SOPORTADO`.
5. Repite el flujo feliz de la etapa 2 (crear, enviar a revisión, asignar revisores, evaluar, publicar) y comprueba que sale `PreguntaPublicada`. Si `servicio-catalogo` aún no está disponible, valida la creación con el servidor gRPC falso de las pruebas y deja el flujo real para la etapa 3.
6. Deja todo detenido, sin borrar volúmenes.

## Resumen final
- Qué cambió en cada ajuste: archivos, configuración y componentes nuevos.
- Pruebas: cuántas pasan y cuántas fallan, más las nuevas.
- Salida de la prueba de humo sin base de datos: códigos HTTP y líneas clave del log.
- `DUDAS.md` actualizado.
- Lista de archivos creados o modificados para el commit.
