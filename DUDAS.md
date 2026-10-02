# DUDAS.md · Dudas abiertas de `servicio-editorial` (Etapa 1)

Decisiones que CONTRATOS.md (v1.5) y MODELO-DOMINIO.md (v1.1) no fijan. En cada una se tomó la opción más conservadora y se marcó en el código con `DUDA:`. Para cerrar una duda, se fija la regla en CONTRATOS.md por PR y luego se ajusta el código indicado.

Las rutas son relativas a `servicio-editorial/src/main/java/co/edu/unicauca/bancopreguntas/editorial/`.

## Resumen

| # | Duda | Impacto en la etapa 2 | Archivo principal |
|---|---|---|---|
| 1 | Código del 404 de procesos | Alto (contrato REST) | `aplicacion/excepciones/ProcesoRevisionNoEncontradoExcepcion.java:8` |
| 2 | Campos de `PreguntaResumen` | Alto (contrato REST) | `aplicacion/resultados/PreguntaResumen.java:8` |
| 3 | Forma de `evaluaciones` en `ProcesoRevisionRespuesta` | Medio (contrato REST) | `aplicacion/resultados/ProcesoRevisionRespuesta.java:34` |
| 4 | Forma de `historialRevisiones` en `TrazabilidadRespuesta` | Medio (contrato REST) | `aplicacion/resultados/TrazabilidadRespuesta.java:33` |
| 5 | Dos agregados en un mismo caso de uso | Alto (arquitectura) | `aplicacion/casosuso/AsignarRevisoresCasoUso.java:31` y `RegistrarEvaluacionCasoUso.java:36` |
| 6 | Restricción de CU-06 con varios roles | Medio | `aplicacion/casosuso/ConsultarPreguntasCasoUso.java:33` |
| 7 | 403 o 404 al ver una pregunta no permitida | Bajo | `aplicacion/casosuso/ObtenerPreguntaCasoUso.java:24` |
| 8 | Filtros de `GET /procesos-revision` | Medio | `aplicacion/casosuso/ConsultarProcesosRevisionCasoUso.java:26` |
| 9 | Publicar sin dictamen favorable | Bajo | `dominio/servicios/PublicadorPreguntaServicio.java:35` |
| 10 | Código de criterios o valoraciones inválidos | Bajo | `dominio/modelo/revision/FormatoDeEvaluacion.java:51` y `CriterioEvaluado.java:27` |
| 11 | `tamano` mayor a 100 | Bajo | `dominio/modelo/comun/Paginacion.java:29` |
| 12 | Cómo se cuentan los caracteres | Bajo | `dominio/servicios/ValidacionEstructural.java:201` |
| 13 | Anexar al historial fuera de `EN_REVISION` | Bajo | `dominio/modelo/pregunta/Pregunta.java:181` |
| 14 | Responsable en la trazabilidad de aprobar o rechazar | Bajo | `dominio/servicios/ResolutorDictamenServicio.java:50` |

---

## 1. Código del 404 de procesos de revisión
- **Qué falta:** CONTRATOS 8.1 indica 404 para `GET /procesos-revision/{procesoId}` y `POST /procesos-revision/{procesoId}/evaluaciones`, pero no nombra el `codigo`. En 5.3 solo aparecen `PREGUNTA_NO_ENCONTRADA` y `COMPETENCIA_NO_ENCONTRADA`.
- **Decisión tomada:** `PROCESO_REVISION_NO_ENCONTRADO`, siguiendo el patrón de los otros 404.
- **Dónde:**
  - `aplicacion/excepciones/ProcesoRevisionNoEncontradoExcepcion.java:8`
  - se usa en `aplicacion/casosuso/RegistrarEvaluacionCasoUso.java` y `aplicacion/casosuso/ObtenerProcesoRevisionCasoUso.java`.
- **Propuesta:** agregar el código a las tablas 5.3 y 8.1 de CONTRATOS.md.

## 2. Campos de `PreguntaResumen`
- **Qué falta:** 8.1 dice que `GET /preguntas` devuelve una "página de `PreguntaResumen`", pero no define sus campos.
- **Decisión tomada:** `preguntaId`, `autorId`, `preguntaDirecta`, `estado`, `clasificacion`, `nivelDificultad` y `fechaActualizacion`.
- **Dónde:**
  - `aplicacion/resultados/PreguntaResumen.java:8`
  - se arma en `aplicacion/resultados/MapeadorDeResultados.java` (`aPreguntaResumen`).

## 3. Forma de `evaluaciones` en `ProcesoRevisionRespuesta`
- **Qué falta:** 8.1 lista el campo `evaluaciones` sin detallar sus campos.
- **Decisión tomada:** `{ revisorId, criterios: [{criterio, valoracion}], observaciones: [texto], decision, fechaEmision }`.
- **Dónde:** `aplicacion/resultados/ProcesoRevisionRespuesta.java:34`.

## 4. Forma de `historialRevisiones` en `TrazabilidadRespuesta`
- **Qué falta:** 8.1 dice "evaluaciones y dictámenes de todos los procesos", sin dar la forma.
- **Decisión tomada:** cada entrada es `{ tipo: "EVALUACION" | "DICTAMEN", procesoId, fecha, evaluacion | null, dictamen | null }`, en orden cronológico.
- **Dónde:**
  - `aplicacion/resultados/TrazabilidadRespuesta.java:33`
  - se arma en `aplicacion/resultados/MapeadorDeResultados.java` (`aEntradaHistorial`).

## 5. Dos agregados en un mismo caso de uso
- **Qué choca:** CONTRATOS 3.3.5 dice "un caso de uso modifica un solo agregado por transacción". Pero:
  - D-14 y el Taller 1 (10.2) definen la apertura del Proceso y el paso de la Pregunta a `EN_REVISION` como un solo hecho;
  - D-15 y el Taller 1 (10.3) piden trasladar cada evaluación y el dictamen a la Pregunta.
- **Decisión tomada:** ambos casos de uso guardan el Proceso y la Pregunta. En la etapa 2 los dos guardados deben ir en la misma transacción.
  - Alternativa: consistencia eventual por evento interno (`ProcesoDeRevisionAbierto` y `DictamenEmitido`).
- **Dónde:**
  - `aplicacion/casosuso/AsignarRevisoresCasoUso.java:31`
  - `aplicacion/casosuso/RegistrarEvaluacionCasoUso.java:36`
- **Propuesta:** el equipo decide si se acepta la excepción y la anota en CONTRATOS 3.3.5 u 11.1.

## 6. Restricción de CU-06 cuando el usuario tiene varios roles
- **Qué falta:** CU-06 dice que el `AUTOR` ve las suyas, el `REVISOR` las asignadas, el `DOCENTE` las `PUBLICADA` y el `ADMINISTRADOR` todas. No dice qué pasa si un usuario tiene varios roles (D-08), ni qué ve un `ESTUDIANTE`.
- **Decisión tomada:**
  - se aplica el rol más amplio, en el orden ADMINISTRADOR > AUTOR > REVISOR > DOCENTE, sin unir los resultados;
  - un usuario solo `ESTUDIANTE` recibe `ACCESO_DENEGADO`.
- **Dónde:** `aplicacion/casosuso/ConsultarPreguntasCasoUso.java:33`.

## 7. 403 o 404 al obtener una pregunta que el rol no puede ver
- **Qué falta:** 8.1 solo lista 404 para `GET /preguntas/{preguntaId}`.
- **Decisión tomada:** si la pregunta existe pero ningún rol del usuario permite verla, se responde 403 `ACCESO_DENEGADO`, no 404.
- **Dónde:** `aplicacion/casosuso/ObtenerPreguntaCasoUso.java:24`.

## 8. Filtros de `GET /procesos-revision?revisorId=…&estado=…`
- **Qué falta:** el repositorio del Taller 1 (12.2) solo ofrece `buscarActivosPorRevisor`. CONTRATOS no dice qué pasa con `estado=CERRADO`, sin `revisorId`, ni si un revisor puede consultar los procesos de otro.
- **Decisión tomada:**
  - solo se admite `estado=ABIERTO` (o ausente); otro valor da `SOLICITUD_INVALIDA`;
  - un `REVISOR` que no es `ADMINISTRADOR` solo ve los suyos (con otro `revisorId` da `ACCESO_DENEGADO`);
  - para el `ADMINISTRADOR`, `revisorId` es obligatorio.
- **Dónde:** `aplicacion/casosuso/ConsultarProcesosRevisionCasoUso.java:26`.

## 9. Publicar sin un dictamen favorable
- **Qué falta:** 8.1 solo lista 409 `TRANSICION_NO_PERMITIDA` para la publicación.
- **Decisión tomada:** si no hay un Proceso vigente cerrado con dictamen `APROBADA`, también se responde `TRANSICION_NO_PERMITIDA`.
- **Dónde:** `dominio/servicios/PublicadorPreguntaServicio.java:35`.

## 10. Código para criterios o valoraciones inválidos en la evaluación
- **Qué falta:** 8.1 no asigna código a criterios faltantes o repetidos, ni a una valoración fuera de 1–5.
- **Decisión tomada:** 400 `SOLICITUD_INVALIDA`. Otra opción sería un 422 de regla de negocio.
- **Dónde:**
  - `dominio/modelo/revision/FormatoDeEvaluacion.java:51` (criterios)
  - `dominio/modelo/revision/CriterioEvaluado.java:27` (valoración)

## 11. Paginación con `tamano` mayor a 100
- **Qué falta:** 5.1 fija el máximo en 100, pero no dice si un valor mayor se recorta o se rechaza.
- **Decisión tomada:** se rechaza con `SOLICITUD_INVALIDA`.
- **Dónde:** `dominio/modelo/comun/Paginacion.java:29`.

## 12. Cómo se cuentan los caracteres en RF-08, RF-09 y RF-13
- **Qué falta:** 11.1 da los límites (2000, 500 y 300) sin decir la unidad.
- **Decisión tomada:** se cuentan puntos de código Unicode, no unidades UTF-16.
- **Dónde:** `dominio/servicios/ValidacionEstructural.java:201`.

## 13. Anexar al historial una pregunta que no está `EN_REVISION`
- **Qué falta:** ningún código cubre este caso. En la práctica no ocurre por los flujos normales.
- **Decisión tomada:** `TRANSICION_NO_PERMITIDA` (409), porque es el estado actual el que impide la operación.
- **Dónde:** `dominio/modelo/pregunta/Pregunta.java:181`.

## 14. Responsable en la trazabilidad de aprobar o rechazar
- **Qué falta:** RF-30 exige registrar el usuario de cada transición, pero el dictamen lo emite el sistema (CU-12).
- **Decisión tomada:** se registra el `X-Usuario-Id` del revisor cuya evaluación disparó el dictamen.
- **Dónde:**
  - `dominio/servicios/ResolutorDictamenServicio.java:50` y `:53`
  - el usuario lo pasa `aplicacion/casosuso/RegistrarEvaluacionCasoUso.java:91`.
- **Nota:** esta duda no tiene comentario `DUDA:` en el código.

---

## Dudas resueltas
| Duda | Resuelta en |
|---|---|
| Spring Boot 4.1.1 frente a "Spring Boot 3" | CONTRATOS.md v1.4 (secciones 1 y 11.1). Pendiente: actualizar `servicio-editorial/CLAUDE.md` y `servicio-editorial/README.md`, que aún dicen "Spring Boot 3". |
| Definición de INV-01 a INV-21, D-13 a D-15 | MODELO-DOMINIO.md (Parte B.1 y B.4), obligatorio desde CONTRATOS.md v1.5. |
