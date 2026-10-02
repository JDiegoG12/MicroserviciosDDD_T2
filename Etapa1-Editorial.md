# ETAPA 1 · servicio-editorial (P1 · Java 21 · Spring Boot 4.1.1)

Eres el agente responsable del microservicio **servicio-editorial** (contexto Gestión Editorial de Preguntas) del Taller 2. Es el servicio con el dominio más rico del sistema y el que sostiene el criterio "DDD y Clean Architecture" de la rúbrica: prioriza un modelo expresivo y bien protegido por invariantes.

## Reglas obligatorias (todas las etapas)
1. Antes de escribir código lee **/CONTRATOS.md** (secciones 0 a 10, la sección 12 y la sección 11.x de tu servicio) y el **CLAUDE.md** de tu servicio. CONTRATOS.md es la fuente de verdad: nombres, códigos de error, enums, rutas y reglas deben quedar EXACTAMENTE como allí aparecen.
2. Trabaja **solo** dentro de la carpeta de tu servicio. No leas ni modifiques otros servicios, `/contratos`, `CONTRATOS.md`, `docker-compose.yml` ni `/postman`.
3. **Git es tarea del integrante.** No ejecutes `git add`, `git commit`, `git push`, `git reset`, `git checkout`, `git switch`, `git merge`, `git rebase` ni `git stash`. Solo puedes usar `git status`, `git diff` y `git log`. Deja los cambios sin preparar.
4. Todo en español: clases, métodos, variables, paquetes, comentarios, mensajes y documentación. Identificadores sin tildes ni ñ.
5. Código legible para cualquier integrante del equipo: nombres que expliquen la intención, métodos cortos, sin abreviaturas crípticas. Cada regla de negocio cita su origen en la documentación (RF-xx, INV-xx, D-xx, CU-xx).
6. Documentación obligatoria en todo elemento público, según la sección 3.2 de CONTRATOS.md.
7. Si algo de CONTRATOS.md es ambiguo o no alcanza, **no inventes**: toma la opción más conservadora, déjala marcada con un comentario `// DUDA:` (o `# DUDA:`) y repórtala en el resumen final.

## Alcance de la ETAPA 1: dominio + aplicación + pruebas
- **Sí:** el proyecto con su archivo de construcción; la capa `dominio` completa; la capa `aplicacion` completa (casos de uso, puertos de entrada y salida, DTOs de comandos y resultados); y las pruebas unitarias de ambas capas.
- **No (son de la etapa 2):** base de datos, controladores REST, Swagger, gRPC, RabbitMQ, Dockerfile y configuración de Docker. Las carpetas `infraestructura` e `interfaces` se crean vacías, con un archivo que explique qué irá allí.
- Los puertos de salida se prueban con **dobles en memoria** (implementaciones falsas) ubicados en la carpeta de pruebas, nunca en el código de producción.
- El dominio no importa nada de frameworks ni de librerías de infraestructura. Excepción: la librería estándar del lenguaje.
- Las fechas y horas se obtienen de un puerto `RelojPuerto` (inyectado en los casos de uso) y se pasan al dominio como parámetro. El dominio nunca consulta la hora del sistema por su cuenta, para que las pruebas sean deterministas.
- Los agregados acumulan sus **eventos de dominio** en una lista interna. El caso de uso los extrae después de guardar el agregado y se los entrega a `PublicadorEventosPuerto`.
- Las excepciones de dominio y de aplicación llevan el `codigo` exacto de la sección 5.3 / 8.x de CONTRATOS.md (por ejemplo `TRANSICION_NO_PERMITIDA`), para que en la etapa 2 se traduzcan a HTTP sin ambigüedad.
- La identidad del usuario llega a los casos de uso como un objeto `UsuarioActual` (id + roles), que la etapa 2 armará desde los encabezados `X-Usuario-Id` y `X-Roles`. La verificación de **rol** se hace en aplicación; las reglas de **propiedad** (por ejemplo "solo el autor modifica") se hacen en el dominio.

## Forma de trabajo
1. Muestra primero un plan breve: árbol de carpetas, clases por capa y lista de pruebas. Después impleméntalo sin esperar confirmación.
2. Implementa primero el dominio y ejecuta sus pruebas. Después implementa la aplicación y ejecuta todas las pruebas.
3. Al final verifica con una búsqueda en el código que la carpeta `dominio` no importe frameworks ni otras capas.

## Resumen final que debes entregar
- Árbol de archivos creados.
- Tabla "Invariante o regla → clase que la protege → prueba que la verifica" (todas las de tu sección 11.x).
- Lista de casos de uso, cada uno con su CU y su endpoint futuro (sección 8.x).
- Salida resumida de la ejecución de pruebas (cuántas pasan y cuántas fallan) y la verificación de dependencias del dominio.
- Lista de `DUDA:` encontradas.
- Lista de archivos creados o modificados, para que el integrante los revise y haga el commit.

## Proyecto
- Maven en `servicio-editorial/`: `groupId` `co.edu.unicauca.bancopreguntas`, `artifactId` `servicio-editorial`, **Java 21** (`<java.version>21</java.version>`), padre `spring-boot-starter-parent` **versión 4.1.1** (fija, no uses otra). Spring Boot 4 usa Spring Framework 7, Jakarta EE 11 y Jackson 3. Si dudas de una dependencia o configuración, consulta la documentación oficial de Spring Boot 4 en lugar de copiar ejemplos de Spring Boot 3.
- Dependencias de esta etapa: `spring-boot-starter` y `spring-boot-starter-test` (JUnit Jupiter, AssertJ y Mockito, en las versiones que gestiona Spring Boot 4.1.1; no fijes versiones a mano). Las demás (web, JPA, AMQP, gRPC, springdoc, PostgreSQL) se agregan en la etapa 2.
- Paquete raíz `co.edu.unicauca.bancopreguntas.editorial`, con la clase `ServicioEditorialAplicacion` y los subpaquetes `dominio`, `aplicacion`, `infraestructura` e `interfaces`.
- El dominio es Java puro: sin anotaciones de Spring, JPA ni Jackson.

## Dominio (sección 11.1 de CONTRATOS.md + Taller 1)
Paquetes sugeridos: `dominio.modelo.pregunta`, `dominio.modelo.revision`, `dominio.modelo.comun`, `dominio.servicios`, `dominio.eventos`, `dominio.repositorios`, `dominio.excepciones`.

**Agregado `Pregunta` (raíz)**
- Value objects (inmutables, se validan al construirse; usa `record` cuando aplique):
  - `PreguntaId`, `UsuarioId`, `Contexto`, `PreguntaDirecta`, `OpcionDeRespuesta` (`letra`, `texto`, `esCorrecta`), `Justificacion`, `Bibliografia`;
  - `ClasificacionAcademica` (`competenciaId`, `temaId`, `subtemaId`; solo identificadores, D-13);
  - `NivelDeDificultad` (`BAJO`, `MEDIO`, `ALTO`), `EstadoPregunta` (los 8 valores exactos), `RegistroDeTrazabilidad` y `HistorialDeRevisiones` (solo anexado).
- La **máquina de estados vive dentro de la Pregunta**: el diagrama de 11.1 exactamente, con una tabla de transiciones permitidas. Toda transición no declarada lanza `TransicionNoPermitidaExcepcion` (`TRANSICION_NO_PERMITIDA`).
- Métodos con nombres del lenguaje ubicuo:
  - `crear(...)`: nace en `BORRADOR`, valida y pasa a `EN_CONSTRUCCION` en el mismo paso si cumple (aclaración de 8.1).
  - `modificar(...)`: solo el autor (si no, `ACCESO_DENEGADO`); solo en estados editables (si no, `PREGUNTA_NO_EDITABLE`); revalida y ajusta el estado.
  - `enviarARevision(...)`, `iniciarRevision(...)`, `registrarEnHistorial(...)`.
  - `aprobar(...)`; `rechazar(...)`, que pasa por `RECHAZADA` y queda en `EN_CONSTRUCCION`, D-07.
  - `publicar(...)`; `archivar(motivo, ...)`, con motivo obligatorio de 1 a 500 caracteres.
- **Validación estructural** RF-08 a RF-13 e INV-04 con los valores exactos de la tabla de 11.1, como constantes con nombre, en una clase de dominio `ValidacionEstructural` que devuelve la lista de `ErrorDeValidacion` (`regla`, `mensaje`). Una pregunta en `BORRADOR` puede incumplir reglas; nunca sale de `BORRADOR` sin cumplirlas (INV-11). La comparación de frases prohibidas (RF-12) es en minúsculas y sin tildes.
- Cada cambio de contenido o de estado anexa un `RegistroDeTrazabilidad` (fecha, usuarioId, tipo `CREACION`/`MODIFICACION`/`TRANSICION`, estado anterior y nuevo, detalle) (RF-29, RF-30, INV-13). Nunca se borra (INV-12).
- Eventos de dominio que emite: `PreguntaCreada`, `ValidacionEstructuralSuperada`, `PreguntaModificada`, `PreguntaSometidaARevision`, `PreguntaEnRevision`, `PreguntaAprobada`, `PreguntaRechazada`, `PreguntaPublicada` y `PreguntaArchivada`. `PreguntaPublicada` lleva **todos** los datos que necesita el evento de integración de la sección 7.4. `PreguntaArchivada` lleva los de 7.5.

**Agregado `ProcesoDeRevision` (raíz)**
- Entidad interna `FormatoDeEvaluacion`. Value objects:
  - `AsignacionDeRevisor`, `Observacion`;
  - `CriterioEvaluado`: `criterio` en `PEDAGOGICO`/`TECNICO`/`ESTRUCTURAL` y `valoracion` de 1 a 5; los tres criterios son obligatorios;
  - `DecisionRevision`: `APROBATORIA`/`REPROBATORIA`;
  - `Dictamen`: `resultado` `APROBADA`/`RECHAZADA`, `porcentajeAprobacion` con 2 decimales, `fechaEmision`.
- `EstadoProceso`: `ABIERTO` | `CERRADO`.
- Invariantes INV-15 a INV-21 y decisiones D-03, D-04, D-05 y D-06. Códigos: `REVISORES_INSUFICIENTES`, `AUTOR_NO_PUEDE_SER_REVISOR`, `REVISOR_DUPLICADO`, `REVISOR_NO_ASIGNADO`, `EVALUACION_YA_REGISTRADA`, `PROCESO_CERRADO`.
- El dictamen se calcula automáticamente al registrarse la última evaluación pendiente. El resultado es `APROBADA` solo si el porcentaje es **estrictamente** mayor a 70 %.
- Eventos: `ProcesoDeRevisionAbierto`, `RevisoresAsignados`, `EvaluacionRegistrada` y `DictamenEmitido`.

**Servicios de dominio** (Taller 1, sección 10)
- `AsignadorRevisoresServicio`: abre el proceso con las validaciones de revisores y llama a `pregunta.iniciarRevision(...)` (D-14).
- `ResolutorDictamenServicio`: traslada cada evaluación al historial de la pregunta (D-15) y, cuando hay dictamen, ejecuta `aprobar` o `rechazar`.
- `PublicadorPreguntaServicio`: verifica que el proceso vigente tenga dictamen `APROBADA` y ejecuta `pregunta.publicar(...)`.

**Repositorios (interfaces de dominio)**
- `PreguntaRepositorio`: `guardar`, `obtenerPorId`, `buscarPorCriterios` (filtros de 8.1 con paginación) y `buscarPorIds`.
- `ProcesoDeRevisionRepositorio`: `guardar`, `obtenerPorId`, `obtenerVigentePorPregunta`, `buscarHistoricosPorPregunta` y `buscarActivosPorRevisor`.

## Aplicación
- Casos de uso, cada uno en su propia clase, con el rol requerido en 8.1:
  - `CrearPreguntaCasoUso`, `ModificarPreguntaCasoUso`, `ConsultarPreguntasCasoUso` (con la restricción por rol de CU-06), `ObtenerPreguntaCasoUso`, `EnviarPreguntaARevisionCasoUso`;
  - `AsignarRevisoresCasoUso`, `RegistrarEvaluacionCasoUso` (incluye CU-12 automático), `ObtenerProcesoRevisionCasoUso`, `ConsultarProcesosRevisionCasoUso`;
  - `PublicarPreguntaCasoUso`, `ArchivarPreguntaCasoUso`, `ConsultarTrazabilidadCasoUso` (CU-18).
- Puertos de salida:
  - `CatalogoAcademicoPuerto`, con `validarClasificacion(ClasificacionAcademica)` que devuelve `ResultadoValidacionClasificacion` (`valida`, `motivo`, `detalle`). Si el catálogo no responde, lanza `CatalogoNoDisponibleExcepcion` (`CATALOGO_NO_DISPONIBLE`).
  - `PublicadorEventosPuerto` y `RelojPuerto`.
- Crear y modificar llaman al catálogo **antes** de guardar. Si la clasificación es inválida, lanzan `CLASIFICACION_INVALIDA` y no se guarda nada.
- Los resultados de los casos de uso son DTOs de aplicación con la forma de `PreguntaRespuesta`, `ProcesoRevisionRespuesta` y `TrazabilidadRespuesta` de 8.1. Todavía sin anotaciones JSON.

## Pruebas mínimas
- Una prueba por cada invariante INV-01 a INV-21 que la viole y compruebe el rechazo con su código.
- La tabla de transiciones: cada transición válida funciona y una muestra representativa de inválidas lanza `TRANSICION_NO_PERMITIDA`.
- Cada regla de validación estructural (RF-08 a RF-13, INV-04), incluida la comparación de frases prohibidas sin tildes ni mayúsculas.
- Dictamen:
  - 2 de 2 aprobatorias → `APROBADA`; 1 de 2 → `RECHAZADA`;
  - 2 de 3 → `RECHAZADA` (66,67 %, D-05); 3 de 3 → `APROBADA`; 3 de 4 → `APROBADA` (75 %);
  - no se calcula con evaluaciones pendientes.
- Rechazo → la pregunta queda `EN_CONSTRUCCION` con el historial intacto y es editable otra vez.
- Casos de uso con dobles:
  - catálogo válido, inválido y caído;
  - rol incorrecto → `ACCESO_DENEGADO`;
  - publicar emite `PreguntaPublicada` con los datos de 7.4;
  - archivar emite `PreguntaArchivada` con su motivo;
  - trazabilidad en orden cronológico.