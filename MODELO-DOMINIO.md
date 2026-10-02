# MODELO-DOMINIO.md · Modelo de dominio del Taller 1 (referencia para el Taller 2)

> Este archivo contiene **el modelo táctico del Taller 1** (decisiones, lenguaje ubicuo, entidades, value objects, agregados, invariantes, servicios, eventos, repositorios y casos de uso) que necesitan los agentes, porque el documento del Taller 1 no está en el repositorio.
>
> **Jerarquía de fuentes:** `CONTRATOS.md` (lo que se comunican los servicios y los estándares) > **Parte A** de este archivo (ajustes acordados para el Taller 2) > **Parte B** (extracto literal del Taller 1). Si la Parte B contradice algo de CONTRATOS.md o de la Parte A, **mandan estos dos**.
>
> Versión 1.1 (2-oct-2026): se aclaró quién puede consultar un intento (A.2). Se cambia con un PR aprobado por al menos otro integrante, igual que CONTRATOS.md. Los agentes **no** lo modifican.

---

## Cómo usar este archivo (instrucciones para Claude Code)

1. Lee la **Parte A completa**: los ajustes y aclaraciones del Taller 2.
2. En la **Parte B** lee solo las secciones que la tabla siguiente indica para tu servicio. Lo demás describe contextos que no implementas.
3. Cita las reglas con su identificador (`INV-29`, `D-14`, `CU-13`) en comentarios, documentación y pruebas.
4. Los nombres del Taller 1 terminados en `Service` y `Repository` se escriben en el código con los sufijos `Servicio` y `Repositorio` (CONTRATOS.md, sección 3.4). Por ejemplo, `CalificadorSimulacroService` → `CalificadorSimulacroServicio`.

### Qué lee cada servicio en la Parte B

| Servicio | Decisiones | Agregados (B.3) | Invariantes (B.4) | Servicios de dominio (B.5) | Eventos (B.6) | Repositorios (B.7) | Casos de uso (B.8) |
|---|---|---|---|---|---|---|---|
| `servicio-editorial` | D-01 a D-07, D-10, D-13, D-14, D-15 | 7.1 Pregunta, 7.2 Proceso de Revisión | **INV-01 a INV-21** | 10.2, 10.3, 10.4 | 11.1, 11.2 | 12.1, 12.2 | CU-04 a CU-12, CU-18 |
| `servicio-catalogo` | D-10, D-11, D-13 | 7.6 Competencia | **INV-22 a INV-24** | — (ver A.3) | 11.6 | 12.3 | — (D-11: administración del catálogo) |
| `servicio-evaluacion` | D-01, D-10, D-13 | 7.3 Simulacro, 7.4 Intento de Simulacro | **INV-25 a INV-32** | 10.1, 10.5 | 11.3, 11.4 (y consume 11.1: `PreguntaPublicada` y `PreguntaArchivada`) | 12.4, 12.5 | CU-13, CU-14, CU-15 |

Las secciones B.1 (decisiones) y B.2 (lenguaje ubicuo) son útiles para todos.

---

# PARTE A · Ajustes y aclaraciones del Taller 2 (prevalecen sobre la Parte B)

## A.1 Erratas del Taller 1 ya resueltas

| # | Lo que dice el Taller 1 | Lo que se implementa | Motivo |
|---|---|---|---|
| E-1 | CU-04: "exactamente **cuatro** Distractores y una única Respuesta correcta" | **Tres** distractores + una correcta = cuatro opciones | D-01 manda; CU-04 tiene un error de redacción |
| E-2 | Tabla 11.1: `PreguntaEnRevision` aparece tres veces y faltan eventos | Se agregan `PreguntaAprobada` (emitido por `Pregunta.aprobar()`) y `PreguntaRechazada` (emitido por `Pregunta.rechazar()`), que el servicio 10.3 ya menciona | Coherencia con 10.3 |
| E-3 | 11.1: `PreguntaSometidaARevision` "dispara la apertura de un nuevo Proceso de Revisión" | No abre nada. El proceso se abre cuando el Administrador asigna revisores (CU-10) | D-14 |
| E-4 | VO Dictamen: "Aprobada, Rechazada, Requiere Modificaciones" | Solo `APROBADA` y `RECHAZADA` | INV-20 |
| E-5 | Nombres de eventos del catálogo distintos entre 11.6 y el diagrama | Se usan los de 11.6: `CompetenciaCreada`, `CompetenciaRenombrada`, `TemaCreado`, `SubtemaCreado` | Texto sobre diagrama |
| E-6 | 10.1: el calificador "aplica ponderaciones por Competencia" | **Sin ponderaciones**: `puntaje = correctas / total × 100`, con desglose por competencia | CONTRATOS 7.6 y 11.3 |

## A.2 Cómo se interpretan algunas invariantes en el código

- **INV-01 a INV-06 (estructura de la Pregunta).** Se aplican como **validación estructural**. Una Pregunta en `BORRADOR` puede incumplirlas (D-02); **nunca sale de `BORRADOR` sin cumplirlas** (INV-11). Los incumplimientos se devuelven en `erroresValidacion`; no son una excepción. Los valores numéricos exactos (longitudes, frases prohibidas) están en CONTRATOS.md 11.1.
- **INV-07 (clasificación existente).** Editorial la verifica llamando a Catálogo por gRPC al crear y al modificar (CONTRATOS.md 6). Solo se guardan identificadores (D-13).
- **INV-16 (el autor no puede ser revisor).** Se verifica comparando ids. Como no existe el servicio de Identidad, no se puede comprobar que los ids tengan el rol Revisor: es una limitación documentada (CONTRATOS.md 11.1).
- **INV-25 (solo preguntas publicadas).** En Evaluación se verifica contra la **copia local** `PreguntaEvaluable` en estado `PUBLICADA`, que se alimenta con los eventos de Editorial.
- **INV-29 (todo intento pertenece a un único simulacro y a un único estudiante).** El `IntentoDeSimulacro` se crea con `simulacroId` y `estudianteId` **obligatorios e inmutables**: no hay ningún método que los cambie. Consecuencias que se implementan y se prueban como parte de INV-29:
  1. no se puede crear un intento sin simulacro o sin estudiante;
  2. solo el estudiante dueño (`estudianteId == X-Usuario-Id`) puede **responder y finalizar** su intento; si no, `ACCESO_DENEGADO`. En la **consulta**, un `ESTUDIANTE` solo ve su propio intento, y un `DOCENTE` puede leer cualquiera, solo lectura (CONTRATOS.md 8.3);
  3. una respuesta solo puede referirse a una pregunta de **ese** simulacro; si no, `PREGUNTA_NO_PERTENECE_AL_SIMULACRO`.
- **INV-31 (respuestas solo en curso y antes de vencer).** Se implementa con el **vencimiento perezoso** de CONTRATOS.md 11.3 → `INTENTO_FINALIZADO`.
- **INV-32 (calificación única e inmutable).** Archivar una pregunta después **no** cambia calificaciones existentes. La copia local de la pregunta nunca se modifica, solo cambia su estado.
- **INV-24 (nombres únicos).** La unicidad de competencia en todo el catálogo cruza agregados, así que la resuelve un servicio de dominio con el repositorio. La de tema y subtema la protege el propio agregado `Competencia`.

## A.3 Elementos del Taller 1 que se adaptan en el Taller 2

| Elemento del Taller 1 | En el Taller 2 |
|---|---|
| `PreguntaRepository` usado (en lectura) por `EnsambladorSimulacroService` y `CalificadorSimulacroService` | Se reemplaza en Evaluación por `PreguntaEvaluableRepositorio` sobre la copia local, porque ningún servicio lee la base de datos de otro. `buscarPublicadasPorCriterios` vive allí. |
| `UsuarioRepository` usado por `AsignadorRevisoresService` | No existe: no hay servicio de Identidad. Ver INV-16 en A.2. |
| CU-15 "en reacción al evento `IntentoDeSimulacroFinalizado`" | Se ejecuta en el mismo caso de uso de finalización, dentro del proceso; no pasa por el broker (CONTRATOS.md 11.3). |
| CU-14 "el Simulacro está disponible para él" (`buscarDisponiblesParaEstudiante`) | Todos los simulacros definidos están disponibles para todos los estudiantes. No hay grupos ni asignaciones. |
| Catálogo: validación de clasificación | Servicios de dominio `ValidadorClasificacionServicio` y `VerificadorNombreCompetenciaServicio` (no estaban en el Taller 1; se agregan para INV-07 e INV-24). |
| Agregados `Usuario`, `ReporteAcademico`, `RegistroDeAuditoria`; servicio `GeneradorReporteAcademicoService`; CU-01 a CU-03, CU-16 y CU-17 | **Fuera de alcance** del Taller 2. Aparecen en la Parte B solo como contexto. No se implementan. |
| Estados del intento (no definidos en el Taller 1) | `EN_CURSO` → `FINALIZADO` → `CALIFICADO` (CONTRATOS.md 11.3). |
| Estado del proceso de revisión (no definido) | `ABIERTO` → `CERRADO` (INV-21). |

---

# PARTE B · Extracto literal del Taller 1

> Texto copiado del documento "Taller 1 · Microservicios · DDD" del equipo, sin cambios de contenido. Se omitieron las imágenes (diagramas) y las secciones 1, 3, 4, 8 y 14 a 16 (contexto general, arquitectura y justificaciones), que no se necesitan para implementar.


## B.1 Decisiones de dominio

### 0. Decisiones de dominio acordadas

Estas decisiones (Ver tabla 1) cierran ambigüedades del enunciado del proyecto. Deben quedar en el informe porque de ellas se derivan las invariantes y las reglas de negocio de los casos de uso.

###### Tabla 1. Decisiones de dominio acordadas

| \#   | Punto abierto en el enunciado               | Decisión del equipo                                                                                                                                                                                                                      |
|------|---------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| D-01 | Número de opciones de respuesta             | Cuatro opciones: tres Distractores y una Respuesta correcta (RF-05, RF-10, RF-11). Conforme al formato ICFES de cuatro opciones.                                                                                                         |
| D-02 | Diferencia entre Borrador y En construcción | *Borrador*: Pregunta creada pero estructuralmente incompleta. *En construcción*: Pregunta que ya supera la Validación estructural pero que el Autor aún no somete. Ambos son estados editables.                                          |
| D-03 | Número mínimo de Revisores                  | Mínimo dos Revisores asignados por Pregunta.                                                                                                                                                                                             |
| D-04 | Regla de aprobación                         | El Dictamen se calcula únicamente cuando **todos** los Revisores asignados han emitido su evaluación. Si el porcentaje de aprobaciones **supera el 70%**, la Pregunta pasa a Aprobada; en caso contrario pasa a Rechazada.               |
| D-05 | Consecuencia práctica de D-04               | Con dos o tres Revisores, la regla del 70% equivale a **unanimidad** (2/3 = 66,7% no alcanza el umbral). Es una política deliberadamente estricta.                                                                                       |
| D-06 | Autor como Revisor de su propia Pregunta    | Prohibido. Un Usuario no puede ser asignado como Revisor de una Pregunta de la cual es Autor.                                                                                                                                            |
| D-07 | Destino de una Pregunta Rechazada           | Retorna al estado En construcción conservando íntegro el Historial de revisiones.                                                                                                                                                        |
| D-08 | Multiplicidad de Roles                      | Un Usuario puede tener varios Roles simultáneamente.                                                                                                                                                                                     |
| D-09 | Rol de Coordinador                          | No es un Rol del sistema (RF-03 define cinco). Es un destinatario de los Reportes académicos, atendido a través del Rol Docente.                                                                                                         |
| D-10 | Nivel de dificultad                         | Enumeración cerrada: Bajo, Medio, Alto.                                                                                                                                                                                                  |
| D-11 | Catálogo de Competencias, Temas y Subtemas  | Administrable por el Administrador, no fijo en código (RNF-13).                                                                                                                                                                          |
| D-12 | Alcance del modelado                        | Se modelan conceptualmente las ocho historias de usuario. La implementación posterior será reducida.                                                                                                                                     |
| D-13 | Referencia al Catálogo Académico            | Competencia, Tema y Subtema se referencian **por identificador**, nunca por copia. El identificador es estable e independiente del nombre, de modo que renombrar una Competencia se refleja en todo el histórico sin romper referencias. |
| D-14 | Momento de apertura del Proceso de Revisión | El Proceso de revisión se abre cuando el Administrador asigna los Revisores (CU-10), no cuando el Autor somete la Pregunta. Entre ambos momentos la Pregunta permanece en Pendiente de revisión sin Proceso asociado.                    |
| D-15 | Ubicación del Historial de revisiones       | El Historial de revisiones vive dentro del agregado **Pregunta**, no dentro del Proceso de revisión. Así sobrevive a los sucesivos Procesos que genera D-07 y queda disponible para CU-18 en una sola lectura.                           |

---

## B.2 Lenguaje ubicuo

### 2. Lenguaje ubicuo

Glosario de términos del dominio, agrupado por área de significado.

#### 2.1 Usuarios y roles

| Término            | Definición                                                                                                                                     | Observaciones / Relaciones                                                                             |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Usuario            | Persona registrada en el sistema, identificada por sus credenciales, a la que se asignan uno o más Roles.                                      | Un Usuario puede tener varios Roles simultáneamente (D-08).                                            |
| Rol                | Perfil que determina las acciones permitidas a un Usuario dentro del sistema, según su función.                                                | El acceso a las funcionalidades se restringe según el Rol asignado (RNF-07).                           |
| Administrador      | Rol con privilegios para gestionar el sistema, sus Usuarios y la configuración general de la plataforma.                                       | Responsable de la asignación de Revisores, la publicación de Preguntas y la Auditoría.                 |
| Autor de preguntas | Rol encargado de crear y modificar Preguntas mientras estas se encuentran en un estado editable.                                               | Da inicio al Ciclo de vida de la Pregunta. No puede ser Revisor de sus propias Preguntas (D-06).       |
| Revisor            | Rol asignado a una Pregunta para evaluarla mediante la Revisión por pares, diligenciando el Formato de evaluación y registrando Observaciones. | Su evaluación es aprobatoria o reprobatoria y alimenta el Dictamen.                                    |
| Docente            | Rol que define Simulacros y hace uso de los Reportes académicos y las Estadísticas generadas a partir del Desempeño de los Estudiantes.        | Puede además actuar como Autor o Revisor. Atiende también las necesidades de los coordinadores (D-09). |
| Estudiante         | Rol que resuelve Simulacros compuestos por Preguntas Publicadas y cuyo Desempeño es registrado y analizado por el sistema.                     | Su historial de Intentos de simulacro alimenta el Seguimiento académico.                               |

#### 2.2 Banco de preguntas y estructura de la Pregunta

| Término                | Definición                                                                                                                                                      | Observaciones / Relaciones                                                                                                                                                                 |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Banco de preguntas     | Ámbito del dominio que agrupa la autoría, validación y ciclo de vida del conjunto de Preguntas gestionadas por el sistema, en cualquiera de sus estados.        | Reúne todas las Preguntas del sistema, en cualquier estado de su Ciclo de vida. Ninguna Pregunta se elimina de él: las que dejan de usarse quedan Archivadas, preservando la Trazabilidad. |
| Pregunta               | Elemento fundamental de evaluación, de selección múltiple con única respuesta, sometido a un Ciclo de vida controlado y susceptible de integrar Simulacros.     | Unidad de consistencia del Banco de preguntas.                                                                                                                                             |
| Contexto               | Texto o situación descrita al inicio de una Pregunta que brinda la información necesaria para responder la Pregunta directa.                                    | Componente obligatorio, validado automáticamente (RF-08).                                                                                                                                  |
| Pregunta directa       | Enunciado puntual y único que formula lo que se solicita al Estudiante dentro de una Pregunta, a partir del Contexto dado.                                      | Debe existir una y solo una por Pregunta (RF-09).                                                                                                                                          |
| Opción de respuesta    | Cada una de las cuatro alternativas que se presentan al Estudiante en una Pregunta: tres Distractores y una Respuesta correcta.                                 | Concepto que generaliza Distractor y Respuesta correcta (D-01).                                                                                                                            |
| Distractor             | Cada una de las tres Opciones de respuesta incorrectas de una Pregunta, que deben ser plausibles pero erróneas.                                                 | Debe cumplir criterios de longitud y estructura gramatical; no puede usar expresiones como “todas/ninguna de las anteriores” (RF-12, RF-13).                                               |
| Respuesta correcta     | Opción de respuesta que representa la solución válida de una Pregunta.                                                                                          | Debe existir una única por Pregunta y se acompaña de una Justificación (RF-11).                                                                                                            |
| Justificación          | Explicación que sustenta por qué la Respuesta correcta es la solución válida de una Pregunta.                                                                   | Componente obligatorio; apoya el proceso de Revisión por pares.                                                                                                                            |
| Bibliografía           | Referencia a las fuentes que sustentan el Contexto, la Respuesta correcta y la Justificación de una Pregunta.                                                   | Componente obligatorio de toda Pregunta (RF-05).                                                                                                                                           |
| Validación estructural | Conjunto de verificaciones automáticas que comprueban que una Pregunta cumple los criterios de forma exigidos antes de poder ser sometida a Revisión por pares. | Cubre RF-08 a RF-13. Condición para transitar de Borrador a En construcción.                                                                                                               |

#### 2.3 Clasificación académica

| Término             | Definición                                                                                                                                            | Observaciones / Relaciones                                                                                              |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| Competencia         | Categoría que agrupa las habilidades y conocimientos evaluados por una Pregunta; se usa como criterio de clasificación y de generación de Simulacros. | Cada Pregunta pertenece a una Competencia; base del cálculo de Fortalezas y Debilidades. Catálogo administrable (D-11). |
| Tema                | Categoría de clasificación de una Pregunta dentro de una Competencia, más general que el Subtema.                                                     | Criterio de filtro y de generación de Simulacros.                                                                       |
| Subtema             | Categoría de clasificación que especifica con mayor detalle el Tema al que pertenece una Pregunta.                                                    | Refina la clasificación del Tema; usado en filtros y generación de Simulacros.                                          |
| Nivel de dificultad | Atributo de una Pregunta que indica su grado de complejidad, utilizado para balancear la generación de Simulacros.                                    | Valores posibles: Bajo, Medio, Alto (D-10).                                                                             |
| Conocimiento        | Conjunto de saberes teóricos o conceptuales que una Pregunta busca evaluar en el Estudiante.                                                          | Asociado a la Competencia y al Tema/Subtema de la Pregunta.                                                             |
| Habilidad           | Capacidad práctica o cognitiva del Estudiante para aplicar el Conocimiento, evaluada indirectamente mediante la Pregunta y su Nivel de dificultad.    | Vinculada a la Competencia; se refleja en el Desempeño.                                                                 |

#### 2.4 Ciclo de vida de la Pregunta

| Término                | Definición                                                                                                                                   | Observaciones / Relaciones                                                                                                           |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Estado (ciclo de vida) | Condición en la que se encuentra una Pregunta dentro de su Ciclo de vida, la cual determina las acciones permitidas sobre ella.              | Estados posibles: Borrador, En construcción, Pendiente de revisión, En revisión, Aprobada, Rechazada, Publicada y Archivada (RF-14). |
| Borrador               | Estado inicial del Ciclo de vida, en el que la Pregunta aún está estructuralmente incompleta.                                                | Editable. No puede someterse a Revisión por pares hasta superar la Validación estructural (D-02).                                    |
| En construcción        | Estado en el que la Pregunta ya supera la Validación estructural pero su Autor aún no la somete a Revisión por pares.                        | Editable. Es también el estado al que retorna una Pregunta Rechazada (D-02, D-07).                                                   |
| Pendiente de revisión  | Estado en el que la Pregunta ha sido sometida por su Autor y espera la asignación de Revisores.                                              | Antecede al estado En revisión. No editable.                                                                                         |
| En revisión            | Estado en el que la Pregunta está siendo evaluada activamente por los Revisores asignados mediante la Revisión por pares.                    | Transiciona a Aprobada o Rechazada según el Dictamen.                                                                                |
| Aprobada               | Estado que indica que la Pregunta superó la Revisión por pares con un porcentaje de aprobaciones superior al 70%.                            | Habilita la transición al estado Publicada.                                                                                          |
| Rechazada              | Estado que indica que la Pregunta no alcanzó el umbral de aprobación de la Revisión por pares.                                               | Retorna a En construcción conservando el Historial de revisiones (D-07).                                                             |
| Publicada              | Estado en el que la Pregunta está disponible para ser utilizada en la generación de Simulacros.                                              | Solo las Preguntas Publicadas pueden integrar un Simulacro (RF-21).                                                                  |
| Archivada              | Estado que indica que la Pregunta fue retirada de uso activo sin eliminarla físicamente del Banco de preguntas.                              | Preserva la Trazabilidad; ninguna Pregunta se elimina físicamente (RNF-16).                                                          |
| Transición de estado   | Cambio controlado de una Pregunta de un Estado del ciclo de vida a otro, permitido únicamente si cumple las reglas definidas por el sistema. | Cada transición queda registrada para la Trazabilidad y la Auditoría (RF-15, RNF-09).                                                |

#### 2.5 Revisión por pares

| Término                   | Definición                                                                                                                                                 | Observaciones / Relaciones                                                                                |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Revisión por pares        | Proceso mediante el cual varios Revisores evalúan la calidad pedagógica, técnica y estructural de una Pregunta antes de su aprobación o rechazo.           | Genera Formatos de evaluación y Observaciones; determina la Transición de estado de la Pregunta.          |
| Proceso de revisión       | Instancia concreta de la Revisión por pares abierta sobre una Pregunta, que agrupa las Asignaciones de revisor, los Formatos de evaluación y el Dictamen.  | Unidad de consistencia de la Revisión por pares.                                                          |
| Asignación de revisor     | Vínculo entre un Revisor y una Pregunta que lo habilita y obliga a diligenciar un Formato de evaluación.                                                   | Mínimo dos por Pregunta. Un Revisor no puede ser asignado a una Pregunta de la que es Autor (D-03, D-06). |
| Formato de evaluación     | Instrumento que diligencia un Revisor para calificar y comentar la calidad de una Pregunta, y en el que consigna su decisión aprobatoria o reprobatoria.   | Da origen a las Observaciones y al Historial de revisiones (RF-17).                                       |
| Observación (del revisor) | Comentario o anotación registrada por un Revisor sobre aspectos a corregir o resaltar de una Pregunta.                                                     | Forma parte del Formato de evaluación y del Historial de revisiones (RF-18).                              |
| Dictamen                  | Resultado consolidado de la Revisión por pares, calculado como el porcentaje de Formatos de evaluación aprobatorios sobre el total de Revisores asignados. | Solo se calcula cuando todos los Revisores asignados han evaluado. Umbral: superior al 70% (D-04).        |
| Historial de revisiones   | Registro cronológico de todas las evaluaciones, Observaciones y Dictámenes emitidos sobre una Pregunta a lo largo de su Ciclo de vida.                     | Se conserva aunque la Pregunta sea Rechazada y vuelva a edición (RF-19, D-07).                            |

#### 2.6 Simulacros

| Término                  | Definición                                                                                                                          | Observaciones / Relaciones                                                                                  |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Simulacro                | Definición de una evaluación: conjunto de Preguntas Publicadas, criterios de generación y Duración máxima.                          | Es una plantilla reutilizable; su presentación por parte de un Estudiante se denomina Intento de simulacro. |
| Intento de simulacro     | Presentación concreta de un Simulacro por parte de un Estudiante, con instante de inicio, Respuestas del estudiante y Calificación. | Un Simulacro origina muchos Intentos. Unidad de consistencia independiente del Simulacro.                   |
| Duración máxima          | Tiempo límite establecido para completar un Intento de simulacro.                                                                   | Al vencerse se dispara la Calificación automática (RF-23).                                                  |
| Respuesta del estudiante | Opción de respuesta seleccionada por un Estudiante para una Pregunta dentro de un Intento de simulacro.                             | Base de la Calificación automática.                                                                         |
| Calificación automática  | Proceso mediante el cual el sistema evalúa y puntúa las Respuestas del estudiante sin intervención manual.                          | Se ejecuta al finalizar el Estudiante o al vencer la Duración máxima (RF-24).                               |
| Calificación             | Resultado numérico obtenido por un Estudiante en un Intento de simulacro, desagregable por Competencia.                             | Producto de la Calificación automática; insumo del Desempeño.                                               |

#### 2.7 Seguimiento académico

| Término                     | Definición                                                                                                                         | Observaciones / Relaciones                                                             |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Desempeño                   | Medida del nivel de logro de un Estudiante en uno o varios Intentos de simulacro, expresada mediante Estadísticas individuales.    | Se calcula a partir de respuestas correctas e incorrectas; base del Reporte académico. |
| Estadística individual      | Conjunto de indicadores numéricos generados a partir del Desempeño de un Estudiante en sus Intentos de simulacro.                  | Base para identificar Fortalezas y Debilidades (RF-26).                                |
| Fortaleza (por competencia) | Competencia en la que un Estudiante demuestra un Desempeño superior según sus Estadísticas individuales.                           | Se calcula por Competencia; complementa a la Debilidad (RF-27).                        |
| Debilidad (por competencia) | Competencia en la que un Estudiante demuestra un Desempeño inferior según sus Estadísticas individuales.                           | Orienta al Docente en el Reporte académico (RF-27).                                    |
| Reporte académico           | Documento generado por el sistema que resume el Desempeño, las Estadísticas, Fortalezas y Debilidades de uno o varios Estudiantes. | Dirigido a Docentes y coordinadores (RF-28, D-09).                                     |

#### 2.8 Términos transversales

| Término      | Definición                                                                                                                                                     | Observaciones / Relaciones                                                                                                            |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| Trazabilidad | Capacidad del sistema de registrar y preservar el historial completo de cambios realizados sobre una Pregunta a lo largo de su Ciclo de vida.                  | Incluye fecha y Usuario responsable de cada modificación; se mantiene incluso cuando la Pregunta es Archivada (RF-29, RF-30, RNF-16). |
| Auditoría    | Registro de eventos relevantes del sistema, como inicios de sesión, Transiciones de estado y acciones de Revisión por pares, con fines de control y seguridad. | Requerida como parte de la Seguridad del sistema (RNF-09).                                                                            |

---

## B.3 Entidades, value objects y agregados

### 5. Entidades

Las Entidades son los elementos centrales del sistema. Se caracterizan por tener una identidad única que los distingue y una historia (o ciclo de vida) en la que cambian de estado sin dejar de ser el mismo objeto. Para este modelo, se identifican las siguientes:

**Pregunta**

Es el núcleo del sistema. Cada pregunta tiene un identificador único que la acompaña durante todo su recorrido. Nace como un Borrador y pasa por varias etapas (En construcción, En revisión, Aprobada) hasta ser Publicada o Archivada. Aunque su texto, justificación o sus opciones cambien, sigue siendo la misma pregunta, lo que permite llevar un historial exacto de su proceso editorial.

**Proceso de Revisión**

Es la instancia que agrupa todo el trabajo necesario para evaluar una Pregunta. Nace cuando un administrador asigna a los revisores y se mantiene activo mientras ellos llenan sus formatos de evaluación. Termina de forma definitiva cuando se calcula y emite el dictamen final sobre la pregunta evaluada.

**Formato de Evaluación**

Instrumento que diligencia un Revisor para calificar y comentar la calidad de una Pregunta, y en el que consigna su decisión aprobatoria o reprobatoria. Requiere identidad propia porque cada Revisor asignado produce exactamente uno y el sistema debe poder identificar individualmente cuál corresponde a cuál Revisor; sin embargo, una vez emitido es inmutable, por lo que dicha identidad no implica una evolución posterior.

**Simulacro**

Es la plantilla reutilizable de una evaluación: un conjunto de Preguntas Publicadas, criterios de generación y una Duración Máxima. Requiere identidad propia porque un mismo Simulacro origina múltiples Intentos de Simulacro que deben poder referenciarlo de forma estable a lo largo del tiempo, incluso si alguna de las Preguntas que contiene fuera archivada posteriormente.

**Intento de Simulacro**

Es el registro de una prueba específica realizada por un Estudiante. Su ciclo de vida es directo: inicia cuando el estudiante arranca la prueba, se actualiza cada vez que responde una pregunta y finaliza cuando entrega o se acaba el tiempo. Al cerrarse, recibe una calificación automática y se convierte en un registro histórico inmodificable.

**Usuario**

Representa a las personas que interactúan con la plataforma, identificadas de forma única por su correo institucional. A lo largo del tiempo, un usuario puede ganar o perder diferentes roles (Autor, Revisor, Docente), pero su identidad en el sistema nunca cambia. Esto facilita el seguimiento claro de quién hace qué.

**Competencia, Tema y Subtema**

Dentro del Catálogo Académico, estos tres conceptos tienen identidad y ciclo de vida propios: el Administrador los crea, edita y gobierna operativamente, por lo que cada uno posee un identificador estable e independiente de su etiqueta textual, renombrar una Competencia no rompe las referencias que ya existan hacia ella. Desde el resto del sistema estos elementos se referencian **siempre por identificador y nunca por copia** (D-13): la Pregunta guarda un CompetenciaId, un TemaId y un SubtemaId, y el Reporte académico agrupa por esos mismos identificadores. La consecuencia deliberada es que renombrar una Competencia se refleja de inmediato en todo el histórico, incluidos los Reportes ya generados.

**Reporte Académico**

Consolidación del Desempeño de uno o varios Estudiantes en un período y un alcance determinados. Requiere identidad propia porque el Docente debe poder recuperarlo, citarlo y compararlo con otros reportes emitidos en fechas distintas. Su ciclo de vida es mínimo: se genera y se sella; nunca se edita, de modo que un alcance distinto exige un reporte nuevo.

**Registro de Auditoría**

Anotación fechada de un evento relevante del sistema (inicio de sesión, Transición de estado o acción de Revisión por pares) con su Usuario responsable. Requiere identidad propia porque cada anotación debe poder referenciarse individualmente en una consulta de control, y su ciclo de vida se agota en el instante de su creación: una vez escrita es inmutable.

### 6. Value Objects

En el modelo del Banco de Preguntas Saber PRO, los siguientes elementos se clasifican como Objetos de Valor porque el sistema no requiere rastrear su historial individual; su existencia se justifica exclusivamente por los datos que contienen, son inmutables y dependen totalmente de una Entidad Raíz.

**Nivel de Dificultad**

Atributo cerrado de una Pregunta: Bajo, Medio o Alto, que indica su grado de complejidad y se usa para balancear la generación de Simulacros. No tiene identidad ni justifica rastreo individual: dos Preguntas con Nivel "Alto" comparten exactamente el mismo valor. Se clasifica bajo el mismo criterio ya aplicado a la Clasificación Académica.

**Duración Máxima**

Valor numérico que expresa el tiempo límite de un Intento de Simulacro. Es inmutable una vez fijado el Simulacro y se autovalida contra su propia invariante ("debe ser un valor positivo", CU-13); no requiere identidad ni tiene sentido fuera del Simulacro que lo define.

**Componentes de la Pregunta (Enunciado, Opciones de Respuesta y Justificación)**

No tienen identidad independiente ni sentido fuera del contexto de una Pregunta. Una opción de respuesta (ej. "A. 45" con el valor booleano de esCorrecta = true) no pasa por estados de revisión propios. Si un revisor detecta un error ortográfico en una opción, el dominio no actualiza el estado de esa opción; estructuralmente, la opción defectuosa se destruye y se reemplaza por un nuevo Objeto de Valor con el texto corregido dentro de la Entidad Pregunta.

**Calificación del Simulacro**

Es el resultado numérico y cuantitativo que se genera al finalizar un Intento de Simulacro. Una calificación de "85/100" no tiene historia ni evolución; es una medida estática y definitiva. Constituye un valor inmutable que refleja el estado de conocimiento del estudiante en ese instante exacto. Depende totalmente de la Entidad Raíz Intento de Simulacro.

**Dictamen de Revisión**

Representa el veredicto final (ej. Aprobada, Rechazada, Requiere Modificaciones) dentro del Proceso de Revisión. No es una entidad porque no evoluciona; es un registro descriptivo emitido en un momento específico. Una vez que el evaluador emite el dictamen, este valor se sella y no muta. Depende totalmente de la Entidad Raíz Proceso de Revisión.

**Observación del Revisor**

Comentario o anotación registrada por un Revisor dentro de su Formato de Evaluación. No tiene identidad propia ni evoluciona una vez escrita: si el Revisor desea corregirla antes de emitir su decisión final, el dominio reemplaza el Objeto de Valor completo, no lo edita parcialmente. Depende totalmente de la Entidad local Formato de Evaluación.

**Respuesta del Estudiante**

Durante un Intento de Simulacro, la selección que hace el estudiante (ej. "Opción B") es un dato transaccional. No requiere un identificador único (ID) propio en la lógica de negocio; se define enteramente por la combinación del ID de la Pregunta y el valor de la opción seleccionada. Si el estudiante cambia de opinión antes de entregar, el sistema simplemente descarta el Objeto de Valor anterior e instancia uno nuevo. Depende totalmente de la Entidad Raíz Intento de Simulacro.

**Asignación de Revisor**

Vínculo entre un Revisor y el Proceso de Revisión abierto sobre una Pregunta. Se modela como Objeto de Valor y no como Entidad porque su contenido (el identificador del Revisor y el instante de la asignación) nunca cambia: una asignación no se edita, se crea al abrir el Proceso y desaparece con él. Dentro del Proceso se distingue por el RevisorId, que ya la identifica sin necesidad de un identificador técnico propio. Depende totalmente de la Entidad Raíz Proceso de Revisión.

**Clasificación Académica**

Terna inmutable formada por CompetenciaId, TemaId y SubtemaId que sitúa a una Pregunta dentro del Catálogo Académico. Es un Objeto de Valor porque no tiene identidad propia ni historia: dos Preguntas clasificadas igual comparten exactamente el mismo valor. Contiene únicamente referencias por identificador, nunca copias de los nombres del catálogo (D-13), de modo que el Catálogo sigue siendo el único dueño de sus etiquetas. Depende totalmente de la Entidad Raíz Pregunta.

**Historial de Revisiones**

Colección cronológica y de solo anexado de los Formatos de evaluación y Dictámenes emitidos sobre una Pregunta a lo largo de todos sus Procesos de revisión. Vive dentro del agregado Pregunta (D-15) porque es la historia de la Pregunta y no la de un Proceso concreto: por efecto de D-07 una misma Pregunta puede acumular varios Procesos, y el Historial debe atravesarlos todos. Es un Objeto de Valor porque sus elementos, una vez anexados, son inmutables y no se identifican individualmente fuera de su Pregunta.

**Pregunta Seleccionada**

Referencia por identificador a una Pregunta Publicada que forma parte de un Simulacro, junto con su posición dentro de él. Es un Objeto de Valor porque no tiene identidad ni ciclo de vida: si el Docente redefine el Simulacro, la selección se reemplaza en bloque. Depende totalmente de la Entidad Raíz Simulacro.

### 7. Agregados

Un Agregado es un grupo de Entidades y Objetos de Valor que se tratan como una unidad para efectos de consistencia transaccional: todo lo que está dentro de sus límites se guarda o modifica de forma atómica, y todo lo que está fuera se referencia únicamente por identificador (Id), nunca por referencia directa al objeto completo. El criterio para trazar cada límite no es "qué conceptos están relacionados", sino qué invariante concreta exige que ese grupo cambie junto, en la misma transacción, para no quedar en un estado inconsistente.

Con las Entidades ya identificadas (Punto 5), se definen los siguientes ocho Agregados.

#### 7.1 Agregado Pregunta

**Aggregate Root**: Pregunta

**Bounded Context**:Gestión Editorial de Preguntas

**Value Objects**: Contexto, PreguntaDirecta, OpcionDeRespuesta, Justificacion, Bibliografia, NivelDeDificultad, Estado, RegistroDeTrazabilidad, Autor, ClasificacionAcademica, HistorialDeRevisiones

Es la unidad de consistencia del Banco de preguntas y el agregado central del dominio: reúne la estructura del ítem y su ciclo de vida de ocho estados en una sola frontera. La máquina de estados vive dentro del agregado, como un método que valida las precondiciones de cada transición, no como lógica externa dispersa, por eso publicar y archivar no son un contexto aparte. Sus invariantes obligan a mirar el conjunto completo: exactamente cuatro “OpcionDeRespuesta” con exactamente una marcada como correcta, una y solo una Pregunta directa, y Contexto, Justificación y Bibliografía obligatorios; nada de eso se puede validar objeto a objeto.

#### 7.2 Agregado Proceso de Revisión

**Aggregate Root**: ProcesoDeRevision

**Bounded Context**: Gestión Editorial de Preguntas  
**Entidades Internas**: FormatoDeEvaluacion

**Value Objects**: AsignacionDeRevisor, Dictamen, Revisor, Observacion (dentro del FormatoDeEvaluacion)

Es la instancia concreta de una Revisión por pares sobre una Pregunta, y es un agregado separado de Pregunta por dos razones que se refuerzan. La primera es la invariante que lo define: el Dictamen solo puede calcularse cuando todos los Revisores asignados han emitido su evaluación, así que la frontera de consistencia tiene que abarcar el conjunto completo de Formatos, ese conjunto es justamente lo que este agregado protege, con un umbral estrictamente superior al 70%, mínimo dos Revisores y ninguno que sea el Autor.

#### 7.3 Agregado Simulacro

**Aggregate Root**: Simulacro

**Bounded Context:** Evaluación y Simulacros

**Value Objects:** CriterioDeGeneracion, DuracionMaxima, Docente, PreguntasSeleccionadas

Es la definición reutilizable de una evaluación, no su presentación: los criterios con los que se eligieron las Preguntas, el tiempo límite y el conjunto de “Preguntas” seleccionadas. Su invariante es la que conecta con todo el proceso editorial, solo pueden integrarlo Preguntas Publicadas, es decir, ítems que superaron la Revisión por pares, es el punto donde el control de calidad se vuelve efectivo. Un Simulacro tampoco puede quedar sin “Preguntas”, si no hay suficientes que satisfagan los criterios, la definición no se completa. El Docente que lo definió se guarda como referencia por id, no como objeto.

#### 7.4 Agregado Intento de Simulacro

**Aggregate Root**: IntentoDeSimulacro

**Bounded Context:** Evaluación y Simulacros

**Value Objects:** RespuestaDelEstudiante, Calificacion, Estudiante

Representa la ejecución específica de una prueba por parte de un alumno y se define como un agregado **autónomo** respecto al *Simulacro*, tal como lo establece el glosario al categorizarlo como una unidad de consistencia propia. Esta separación responde a criterios de escalabilidad y concurrencia: dado que un único Simulacro genera múltiples ejecuciones, incluirlas en la misma frontera causaría un crecimiento desmedido del objeto y bloqueos transaccionales innecesarios ante cada interacción del alumno. Sus invariantes garantizan la integridad del proceso: se admite máximo una *RespuestaDelEstudiante* por ítem, las modificaciones se restringen al tiempo de ejecución y, tras el cierre, la *Calificacion* obtenida es **inmodificable**. Además, la estabilidad del resultado se protege contra cambios editoriales externos, asegurando que el archivado posterior de reactivos no afecte los puntajes consolidados.

#### 7.5 Agregado Usuario

**Aggregate Root:** Usuario

**Bounded Context**: Identidad y Acceso

**Value Objects**: Rol, CorreoInstitucional

Constituye la representación de la identidad individual y el repositorio central de credenciales y **Roles**. Como **Aggregate Root**, este componente salvaguarda la **invariante** de que todo sujeto debe poseer al menos un perfil activo, admitiendo la coexistencia de múltiples funciones (como *Autor* y *Docente*) sin comprometer la unicidad vinculada al correo institucional. Esta arquitectura prescinde de subtipos para asegurar la correspondencia unívoca entre la persona y el objeto de dominio. La interacción con otros **Bounded Contexts** se gestiona mediante referencia por identificador, donde cada contexto traduce el ID a su actor específico; de este modo, la desvinculación de un perfil, como el de *Revisor*, no altera la integridad ni la existencia de los registros históricos producidos previamente por el usuario.

#### 7.6 Agregado Competencia

**Aggregate Root:** Competencia

**Bounded Context**: Catálogo Académico

**Entidades internas**:Tema, Subtema

Constituye el catálogo académico que sustenta la clasificación de las Preguntas y la generación de Simulacros. *Tema* y *Subtema* se definen como entidades internas y no como agregados autónomos debido a que carecen de significado fuera de su **Competencia**, cuya jerarquía debe transaccionar de forma atómica: cualquier reorganización de un nivel superior arrastra necesariamente a sus dependientes. Se establece como un agregado administrable por el Administrador, marcando una distinción fundamental con el *NivelDeDificultad*, el cual, al ser una enumeración cerrada de tres valores, se modela como un **Value Object**. Este componente es consumido por tres contextos distintos mediante referencia por identificador, evitando la duplicidad de datos y garantizando la sincronización del catálogo en todo el sistema.

#### 7.7 Agregado Reporte Académico 

**Aggregate Root**: ReporteAcademico

**Bounded Context**: Seguimiento Académico

**Value Objects:** EstadisticaIndividual, Fortaleza, Debilidad, Estudiante, Docente

Consolida el desempeño de uno o varios Estudiantes en Estadísticas, Fortalezas y Debilidades, siempre desagregadas por Competencia, que es la unidad con la que el Docente puede actuar. Es un agregado de lectura: se construye únicamente a partir de Intentos calificados y no modifica ningún dato del dominio, así que su consistencia es la de la foto que toma, no la de un estado que gobierne. Es el único agregado que lleva dos referencias a persona con papeles distintos: el Estudiante es el sujeto del reporte y el Docente su destinatario.

#### 7.8 Agregados del Registro De Auditoría 

**Aggregate Root**: RegistroDeAuditoria

**Bounded Context**: Auditoría

**Value Objects**: EventoDeSistema

Registra los eventos relevantes del sistema —inicios de sesión, transiciones de estado, acciones de revisión— con fines de control y seguridad. Es deliberadamente mínimo: su única invariante es la inmutabilidad, y es un consumidor pasivo, no pide cambios a nadie y se limita a suscribirse a lo que los demás publican. No confundirlo con la Trazabilidad: esa es la historia de una Pregunta y vive dentro del agregado Pregunta, mientras que esto es el registro de eventos del sistema. Coinciden en anotar las transiciones de estado, pero por razones distintas y con ciclos de vida distintos.

---

## B.4 Invariantes

### 9.Invariantes

Una invariante es una regla que debe cumplirse siempre dentro de la frontera de un agregado, en todo momento y no solo al final de una operación. Por eso se enuncian agrupadas por agregado: cada una justifica, precisamente, por qué ese conjunto de objetos tiene que guardarse y modificarse de forma atómica.

###### Tabla 5. Invariantes por agregado

| \#     | Agregado            | Invariante                                                                                                                                                                    |
|--------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| INV-01 | Pregunta            | Toda Pregunta tiene exactamente cuatro Opciones de respuesta: tres Distractores y una Respuesta correcta (D-01, RF-05, RF-10, RF-11).                                         |
| INV-02 | Pregunta            | Existe una y solo una Opción de respuesta marcada como correcta (RF-11).                                                                                                      |
| INV-03 | Pregunta            | Existe una y solo una Pregunta directa (RF-09).                                                                                                                               |
| INV-04 | Pregunta            | Contexto, Justificación y Bibliografía son obligatorios y no pueden quedar vacíos (RF-05, RF-08).                                                                             |
| INV-05 | Pregunta            | Ningún Distractor puede emplear expresiones como “todas las anteriores” o “ninguna de las anteriores” (RF-12).                                                                |
| INV-06 | Pregunta            | Todo Distractor respeta los criterios de longitud y estructura gramatical definidos (RF-13).                                                                                  |
| INV-07 | Pregunta            | Toda Pregunta referencia exactamente una Competencia, un Tema y un Subtema existentes en el Catálogo académico, siempre por identificador (RF-05, D-13).                      |
| INV-08 | Pregunta            | El Nivel de dificultad pertenece al conjunto cerrado {Bajo, Medio, Alto} (D-10).                                                                                              |
| INV-09 | Pregunta            | El Estado es siempre uno de los ocho definidos y solo cambia mediante una Transición válida declarada en la máquina de estados del agregado (RF-14, RF-15).                   |
| INV-10 | Pregunta            | Solo es modificable mientras esté en Borrador o En construcción (RF-06, D-02).                                                                                                |
| INV-11 | Pregunta            | No transita a En construcción hasta superar íntegramente la Validación estructural (D-02).                                                                                    |
| INV-12 | Pregunta            | Ninguna Pregunta se elimina físicamente; el retiro del uso activo se representa con el estado Archivada (RNF-16).                                                             |
| INV-13 | Pregunta            | Todo cambio de contenido o de estado añade un RegistroDeTrazabilidad con fecha y Usuario responsable; la Trazabilidad es de solo anexado y nunca se reescribe (RF-29, RF-30). |
| INV-14 | Pregunta            | El Historial de revisiones es de solo anexado y se conserva íntegro aunque la Pregunta sea Rechazada o Archivada (RF-19, D-07, D-15).                                         |
| INV-15 | ProcesoDeRevision   | Todo Proceso tiene como mínimo dos Asignaciones de revisor (D-03).                                                                                                            |
| INV-16 | ProcesoDeRevision   | Ningún Revisor asignado puede ser el Autor de la Pregunta evaluada (D-06).                                                                                                    |
| INV-17 | ProcesoDeRevision   | Un mismo Revisor no puede tener más de una Asignación dentro del mismo Proceso.                                                                                               |
| INV-18 | ProcesoDeRevision   | Cada Revisor asignado produce a lo sumo un Formato de evaluación, inmutable una vez emitido (RF-17).                                                                          |
| INV-19 | ProcesoDeRevision   | El Dictamen sólo puede calcularse cuando el 100% de los Revisores asignados ha emitido su evaluación (D-04).                                                                  |
| INV-20 | ProcesoDeRevision   | El Dictamen admite exactamente dos valores: Aprobada si el porcentaje de evaluaciones aprobatorias es estrictamente superior al 70%, Rechazada en cualquier otro caso (D-04). |
| INV-21 | ProcesoDeRevision   | Un Dictamen emitido es inmutable y cierra definitivamente el Proceso.                                                                                                         |
| INV-22 | Competencia         | Toda Competencia posee un identificador estable e independiente de su nombre (D-13).                                                                                          |
| INV-23 | Competencia         | Todo Tema pertenece a una única Competencia y todo Subtema a un único Tema: no existen elementos huérfanos dentro del catálogo.                                               |
| INV-24 | Competencia         | El nombre de una Competencia es único dentro del Catálogo, y el de un Tema es único dentro de su Competencia.                                                                 |
| INV-25 | Simulacro           | Un Simulacro solo puede componerse de Preguntas que estén en estado Publicada en el momento del ensamblado (RF-21).                                                           |
| INV-26 | Simulacro           | Un Simulacro no puede quedar sin Preguntas seleccionadas; si no hay suficientes que satisfagan los criterios, la definición no se completa.                                   |
| INV-27 | Simulacro           | Una misma Pregunta no puede aparecer dos veces dentro del mismo Simulacro.                                                                                                    |
| INV-28 | Simulacro           | La Duración máxima es un valor estrictamente positivo (CU-13).                                                                                                                |
| INV-29 | IntentoDeSimulacro  | Todo Intento pertenece a un único Simulacro y a un único Estudiante.                                                                                                          |
| INV-30 | IntentoDeSimulacro  | Se admite a lo sumo una Respuesta del estudiante por cada Pregunta del Simulacro (CU-14).                                                                                     |
| INV-31 | IntentoDeSimulacro  | Las Respuestas solo pueden registrarse o modificarse mientras el Intento esté en curso y no haya vencido la Duración máxima (RF-23).                                          |
| INV-32 | IntentoDeSimulacro  | Un Intento se califica una sola vez; una vez cerrado, su Calificación es inmutable y no la afectan cambios editoriales posteriores sobre las Preguntas (RF-24).               |
| INV-33 | Usuario             | Todo Usuario conserva en todo momento al menos un Rol asignado (CU-01, CU-03).                                                                                                |
| INV-34 | Usuario             | El Correo institucional identifica de forma única a un Usuario dentro del sistema.                                                                                            |
| INV-35 | Usuario             | La contraseña se almacena exclusivamente como hash, nunca en texto plano (RNF-08).                                                                                            |
| INV-36 | ReporteAcademico    | Se construye únicamente a partir de Intentos de simulacro calificados.                                                                                                        |
| INV-37 | ReporteAcademico    | Es inmutable una vez generado: un alcance distinto exige generar un Reporte nuevo, nunca editar el anterior.                                                                  |
| INV-38 | ReporteAcademico    | Las Fortalezas y Debilidades se expresan siempre desagregadas por Competencia (RF-27).                                                                                        |
| INV-39 | RegistroDeAuditoria | Es inmutable: solo admite creación, nunca modificación ni borrado (RNF-09).                                                                                                   |
| INV-40 | RegistroDeAuditoria | Todo registro consigna el instante, el Usuario responsable y el tipo de evento (RNF-09, RF-30).                                                                               |

---

## B.5 Servicios de dominio

### 10. Servicios de Dominio

En DDD, un Servicio de Dominio representa lógica de negocio pura, sin estado, que no pertenece de forma natural a una Entidad ni a un Objeto de Valor porque coordina múltiples elementos o aplica reglas globales del negocio.

#### 10.1 Servicio de Calificación de Simulacros (CalificadorSimulacroService)

**Agregados que coordina**: Intento de Simulacro (escritura) y Pregunta (solo lectura, para conocer la clave correcta).

**Por qué es un Servicio de Dominio**: contrasta las Respuestas del Estudiante contra las claves correctas de cada Pregunta, aplica ponderaciones por Competencia y calcula el VO Calificación. Esa lógica no pertenece a Intento de Simulacro (que solo guarda respuestas) ni a Pregunta (que solo conoce su clave), por lo que debe vivir fuera de ambos.

**Mutación delegada:** intentoDeSimulacro.cerrarConCalificacion(calificacion).

**Evento emitido como consecuencia:** IntentoDeSimulacroCalificado.

**Repositorios de los que depende:** IntentoDeSimulacroRepository, PreguntaRepository (lectura).

**Disparador**: CU-15, en reacción al evento IntentoDeSimulacroFinalizado.

#### 10.2 Servicio de Asignación de Revisores (AsignadorRevisoresService)

**Agregados que coordina**: Usuario (lectura de Rol) y Proceso de Revisión (escritura). Recibe el AutorId de la Pregunta como parámetro de entrada, no carga el Agregado Pregunta completo.

**Por qué es un Servicio de Dominio**: valida que cada Usuario propuesto tenga Rol Revisor, que ninguno coincida con el AutorId recibido (D-06) y que se cumpla el mínimo de dos Revisores (D-03). Esta validación cruza Usuario y Proceso de Revisión.

**Mutación delegada**: procesoDeRevision.asignarRevisores(revisores) y, una vez abierto el Proceso, pregunta.iniciarRevision(). Es el mismo servicio el que ejecuta ambas porque la apertura del Proceso y la transición de la Pregunta a En revisión son un solo hecho del negocio (D-14).

**Eventos emitidos como consecuencia:** RevisoresAsignados, ProcesoDeRevisionAbierto

**Repositorios de los que depende:** UsuarioRepository (lectura), ProcesoDeRevisionRepository (escritura).

**Disparado**r: CU-10.

#### 10.3 Servicio de Resolución del Dictamen (ResolutorDictamenService)

**Agregados que coordina**: ProcesoDeRevision (lectura del Dictamen y de los Formatos de evaluación) y Pregunta (escritura, delegada).

**Por qué es un Servicio de Dominio:** el Dictamen se calcula dentro de ProcesoDeRevision, pero el cambio de estado pertenece a la máquina de estados que vive dentro de Pregunta. Ningún agregado puede mutar a otro ni ejecutar transiciones sobre un agregado del que no es dueño, de modo que la coordinación entre ambos necesita obligatoriamente un servicio intermedio. Lo mismo ocurre con el Historial de revisiones: como vive en Pregunta (D-15) pero se alimenta de Formatos que produce el Proceso, alguien externo a los dos tiene que trasladarlos.

**Mutación delegada:** pregunta.aprobar() o pregunta.rechazar() según el Dictamen recibido, y pregunta.registrarEnHistorial(formato) al consumir cada EvaluacionRegistrada.

**Eventos emitidos como consecuencia**: PreguntaAprobada o PreguntaRechazada, emitidos por el agregado Pregunta, que es su verdadero dueño.

**Repositorios de los que depende**: ProcesoDeRevisionRepository (lectura), PreguntaRepository (escritura).

**Disparador**: CU-12, en reacción al evento DictamenEmitido; y CU-11, en reacción al evento EvaluacionRegistrada.

#### 10.4 Servicio de Publicación de Preguntas (PublicadorPreguntaService)

**Agregados que coordina**: Proceso de Revisión (solo lectura, para verificar el Dictamen) y Pregunta (escritura, delegada).

**Por qué es un Servicio de Dominio:** verificar que exista un Dictamen favorable exige leer un Agregado distinto al que se va a mutar.

**Mutación delegada:** pregunta.publicar().

**Evento emitido como consecuencia**: PreguntaPublicada. La notificación a otros contextos ya no es responsabilidad del servicio, es este mismo evento, consumido de forma asíncrona por "Evaluación y Simulacros", el que informa la disponibilidad del reactivo.

**Repositorios de los que depende**: ProcesoDeRevisionRepository (lectura), PreguntaRepository (escritura).

Disparador: CU-08.

#### 10.5 Servicio de Ensamblado de Simulacros (EnsambladorSimulacroService)

**Agregados que coordina:** múltiples instancias de Pregunta (solo lectura) para construir un nuevo Agregado Simulacro.

**Por qué es un Servicio de Dominio:** la regla de balance por Competencia/Tema/Subtema/Nivel de Dificultad es una restricción global que ninguna Pregunta individual conoce.

**Mutación delegada:** ninguna sobre agregados existentes, este servicio construye un nuevo Agregado Simulacro a partir del resultado de la consulta.

**Evento emitido como consecuencia:** SimulacroDefinido.

**Repositorios de los que depende:** PreguntaRepository, mediante buscarPublicadasPorCriterios(...). Este servicio es, además, el consumidor natural del evento PreguntaPublicada: solo puede seleccionar Preguntas cuyo estado ya refleje esa transición.

**Disparador:** CU-13.

#### 10.6 Servicio de Generación de Reportes Académicos (GeneradorReporteAcademicoService)

**Agregados que coordina:** IntentoDeSimulacro (lectura, muchas instancias de uno o varios Estudiantes) y Competencia (lectura, para resolver el nombre de cada CompetenciaId al presentar el reporte; lo que se almacena en cada EstadisticaIndividual es el identificador, no una copia de la etiqueta, D-13).

**Por qué es un Servicio de Dominio:** consolidar el desempeño de varios Estudiantes en Estadísticas, Fortalezas y Debilidades por Competencia exige leer múltiples Agregados IntentoDeSimulacro y agregarlos, ninguna instancia individual de IntentoDeSimulacro conoce esa regla de consolidación global.

**Construcción**: A diferencia de los otros servicios que mutan un agregado existente, este construye uno nuevo de ReporteAcademico. Es como EnsambladorSimulacroService, que también construye un Simulacro leyendo múltiples Preguntas.

**Evento emitido como consecuencia:** ReporteAcademicoGenerado.

**Repositorios de los que depende:** IntentoDeSimulacroRepository (lectura), CompetenciaRepository (lectura), ReporteAcademicoRepository (escritura, nuevo).

**Disparador:** CU-17.

---

## B.6 Eventos de dominio

### 11. Eventos de Dominio

Un Evento de Dominio representa un hecho relevante, ya ocurrido e inmutable, que una Raíz de Agregado emite después de aplicar una transición válida sobre sí misma. Su función es comunicar ese hecho a otros Agregados o a otros Bounded Contexts **sin acoplarlos directamente**: quien consume el evento reacciona de forma asíncrona, sin que el Agregado emisor conozca ni dependa de quién lo escucha. Por eso este punto solo puede construirse correctamente una vez cerrados los Puntos 7 y 8: cada evento listado aquí corresponde a un método concreto de una Raíz ya formalizada.

Se agrupan por Agregado emisor.

##### 11.1 Eventos del Agregado Pregunta

| **Evento**                    | **Método que lo emite**                                             | **Consumido por**                                                                                               |
|-------------------------------|---------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| PreguntaCreada                | Pregunta.crear(...)                                                 | Auditoría                                                                                                       |
| ValidacionEstructuralSuperada | Pregunta.validarEstructura()                                        | Auditoría, notifica al Autor que puede someter la Pregunta                                                      |
| PreguntaModificada            | Pregunta.modificar(...)                                             | Auditoría, Trazabilidad                                                                                         |
| PreguntaSometidaARevision     | Pregunta.someterARevision()                                         | Contexto de Gestión Editorial (dispara la apertura de un nuevo Proceso de Revisión)                             |
| PreguntaEnRevision            | Pregunta.iniciarRevision() (delegada por AsignadorRevisoresService) | Auditoría, Trazabilidad                                                                                         |
| PreguntaEnRevision            | Pregunta.iniciarRevision() (delegada por AsignadorRevisoresService) | Auditoría, Trazabilidad                                                                                         |
| PreguntaEnRevision            | Pregunta.iniciarRevision() (delegada por AsignadorRevisoresService) | Auditoría, Trazabilidad                                                                                         |
| PreguntaPublicada             | Pregunta.publicar()                                                 | **Bounded Context "Evaluación y Simulacros"**. Habilita la Pregunta para EnsambladorSimulacroService            |
| PreguntaArchivada             | Pregunta.archivar()                                                 | Contexto de Evaluación y Simulacros (excluye la Pregunta de nuevos ensamblados; no afecta Intentos ya cerrados) |

##### 11.2 Eventos del Agregado Proceso de Revisión

| **Evento**               | **Método que lo emite**                                   | **Consumido por**                                          |
|--------------------------|-----------------------------------------------------------|------------------------------------------------------------|
| ProcesoDeRevisionAbierto | ProcesoDeRevision.abrir(preguntaId, autorId)              | Auditoría                                                  |
| RevisoresAsignados       | ProcesoDeRevision.asignarRevisores(revisores)             | Notifica a los Revisores asignados                         |
| EvaluacionRegistrada     | ProcesoDeRevision.registrarEvaluacion(revisorId, formato) | Auditoría, Historial de revisiones (acumulado en Pregunta) |
| DictamenEmitido          | ProcesoDeRevision.calcularDictamen()                      | PublicadorPreguntaService (si el Dictamen es favorable)    |

##### 11.3 Eventos del Agregado Simulacro

| **Evento**        | **Método que lo emite**                                        | **Consumido por**                                                                        |
|-------------------|----------------------------------------------------------------|------------------------------------------------------------------------------------------|
| SimulacroDefinido | Simulacro.definir(...) (dentro de EnsambladorSimulacroService) | Contexto de Evaluación y Simulacros, habilita a los Estudiantes a presentar el Simulacro |

##### 11.4 Eventos del Agregado Intento de Simulacro

| **Evento**                   | **Método que lo emite**                                | **Consumido por**                                                      |
|------------------------------|--------------------------------------------------------|------------------------------------------------------------------------|
| IntentoDeSimulacroIniciado   | IntentoDeSimulacro.iniciar(simulacroId, estudianteId)  | Auditoría                                                              |
| IntentoDeSimulacroFinalizado | IntentoDeSimulacro.finalizar()                         | CalificadorSimulacroService (dispara la calificación automática)       |
| IntentoDeSimulacroCalificado | IntentoDeSimulacro.cerrarConCalificacion(calificacion) | Contexto de Seguimiento Académico (alimenta Estadísticas individuales) |

##### 11.5 Eventos del Agregado Usuario

| **Evento**                | **Método que lo emite**      | **Consumido por** |
|---------------------------|------------------------------|-------------------|
| UsuarioRegistrado         | Usuario.registrar(...)       | Auditoría         |
| SesionIniciada            | Usuario.autenticar(...)      | Auditoría         |
| RolAsignado / RolRetirado | Usuario.actualizarRoles(...) | Auditoría         |

##### 11.6 Eventos de los Agregados del Catálogo Académico

| **Evento**                                | **Método que lo emite**                             | **Consumido por**                                                                                                                                                                                                             |
|-------------------------------------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CompetenciaCreada / CompetenciaRenombrada | Competencia.crear(...) / Competencia.renombrar(...) | Auditoría. Las Preguntas ya clasificadas no necesitan suscribirse: como la referencia es por identificador y el identificador no cambia al renombrar (D-13), el nuevo nombre se refleja solo al resolverlo contra el Catálogo |
| TemaCreado                                | Tema.crear(competenciaId, ...)                      | Auditoría                                                                                                                                                                                                                     |
| SubtemaCreado                             | Subtema.crear(temaId, ...)                          | Auditoría                                                                                                                                                                                                                     |

##### 11.7 Eventos del Agregado ReporteAcademico

| **Evento**               | **Método que lo emite**       | **Consumido por**                             |
|--------------------------|-------------------------------|-----------------------------------------------|
| ReporteAcademicoGenerado | ReporteAcademico.generar(...) | Notifica al Docente que su reporte está listo |

---

## B.7 Repositorios

### 12. Repositorios

**Solo las Raíces de Agregado tienen Repositorio**. Nunca se crea un repositorio para una Entidad interna ni para un Value Object.

Por eso, aunque el modelo tiene muchas Entidades y Value Objects, solo hay **un Repositorio por cada una de las 8 Raíces de Agregado**:

| **Aggregate Root**                                 | **¿Tiene Repositorio propio?**   |
|----------------------------------------------------|----------------------------------|
| Pregunta                                           | Sí                               |
| ProcesoDeRevision                                  | Sí                               |
| Competencia                                        | Sí                               |
| Simulacro                                          | Sí                               |
| IntentoDeSimulacro                                 | Sí                               |
| ReporteAcademico                                   | Sí                               |
| Usuario                                            | Sí                               |
| RegistroDeAuditoria                                | Sí                               |
| FormatoDeEvaluacion (interno de ProcesoDeRevision) | No, se guarda junto con su Raíz  |
| Tema, Subtema (internos de Competencia)            | No, se guardan junto con su Raíz |

Un Repositorio en DDD es una **interfaz** (un contrato), no una implementación concreta. El Dominio solo declara qué operaciones necesita, nunca menciona SQL, tablas ni ningún detalle de la base de datos que se use por debajo. Eso es justamente lo que permite que el Dominio no dependa de la tecnología de persistencia.

#### 12.1. PreguntaRepository

Guarda y recupera el agregado completo: la Pregunta junto con sus Value Objects y su Trazabilidad interna.

guardar(pregunta: Pregunta): void

obtenerPorId(preguntaId: String): Pregunta

buscarPorAutor(autorId: String): List\<Pregunta\>

buscarPorEstado(estado: EstadoPregunta): List\<Pregunta\>

buscarPublicadasPorCriterios(competenciaId, temaId, subtemaId, nivelDificultad): List\<Pregunta\>

buscarPorIds(preguntaIds: List\<String\>): List\<Pregunta\>

**Por qué existen estos métodos:**

- buscarPorAutor y buscarPorEstado responden directamente a CU-06 ("Consultar preguntas mediante filtros"), donde el resultado se restringe según el Rol del actor.

- buscarPublicadasPorCriterios sirve para que EnsambladorSimulacroService pueda construir un Simulacro. Consulta por la ClasificacionAcademica y el NivelDeDificultad que el agregado Pregunta declara.

- buscarPorIds resuelve el caso de CU-06 cuando el actor es un Revisor: primero se obtienen sus Procesos activos con ProcesoDeRevisionRepository y después se recuperan aquí las Preguntas correspondientes. Son dos agregados distintos y por tanto dos repositorios distintos; ningún repositorio puede devolver un agregado que no le pertenece.

#### 12.2. ProcesoDeRevisionRepository

Guarda el Proceso de Revisión completo, incluyendo sus Asignaciones de Revisor y sus Formatos de Evaluación.

guardar(proceso: ProcesoDeRevision): void

obtenerPorId(procesoId: String): ProcesoDeRevision

obtenerVigentePorPregunta(preguntaId: String): ProcesoDeRevision

buscarHistoricosPorPregunta(preguntaId: String): List\<ProcesoDeRevision\>

buscarActivosPorRevisor(revisorId: String): List\<ProcesoDeRevision\>

**Por qué existen estos métodos:**

- obtenerVigentePorPregunta y buscarHistoricosPorPregunta están separados porque una Pregunta puede acumular varios Procesos de revisión a lo largo de su vida: por D-07, una Pregunta Rechazada vuelve a En construcción y puede volver a someterse, abriendo un Proceso nuevo. El primero devuelve el Proceso abierto o el último cerrado, que es lo que necesitan PublicadorPreguntaService y ResolutorDictamenService; el segundo devuelve la serie completa, que es lo que necesita CU-18.

- buscarActivosPorRevisor devuelve Procesos, no Preguntas. Soporta la parte de CU-06 en la que “un Revisor consulta las Preguntas que le fueron asignadas” combinándose con PreguntaRepository.buscarPorIds sobre los preguntaId obtenidos.

#### 12.3. CompetenciaRepository

Guarda el agregado Competencia, que internamente contiene su jerarquía completa de Tema y Subtema.

guardar(competencia: Competencia): void

obtenerPorId(competenciaId: String): Competencia

listarTodas(): List\<Competencia\>

**Por qué es tan simple:** este agregado es administrado directamente por el Administrador (D-11) con operaciones CRUD estándar, sin las consultas complejas que tienen Pregunta o Simulacro

#### 12.4. SimulacroRepository

Guarda la plantilla reutilizable con sus criterios de generación y la lista de Preguntas seleccionadas.

guardar(simulacro: Simulacro): void

obtenerPorId(simulacroId: String): Simulacro

buscarDisponiblesParaEstudiante(estudianteId: String): List\<Simulacro\>

**Por qué existe buscarDisponiblesParaEstudiante:** CU-14 requiere que "El Simulacro existe y está disponible para él" como precondición, el sistema necesita poder consultar qué Simulacros puede presentar un Estudiante determinado.

#### 12.5. IntentoDeSimulacroRepository

Guarda cada intento individual: sus Respuestas del Estudiante y, al cerrarse, su Calificación.

guardar(intento: IntentoDeSimulacro): void

obtenerPorId(intentoId: String): IntentoDeSimulacro

buscarPorEstudiante(estudianteId: String, desde?: Date, hasta?: Date): List\<IntentoDeSimulacro\>

buscarCalificadosPorEstudiante(estudianteId: String):

List\<IntentoDeSimulacro\>

**Por qué existen estos métodos:**

- buscarPorEstudiante con rango de fechas opcional responde exactamente a la Entrada de CU-16: "Identificador del Estudiante y, opcionalmente, rango de fechas o Competencia".

- buscarCalificadosPorEstudiante es el método que usará directamente el nuevo GeneradorReporteAcademicoService para construir un ReporteAcademico.

##### Tabla 6. Resumen de los 8 Repositorios

| **Repositorio**               | **Agregado que persiste**     | **Método más importante que lo justifica** | **Caso de uso principal**                                 |
|-------------------------------|-------------------------------|--------------------------------------------|-----------------------------------------------------------|
| PreguntaRepository            | Pregunta                      | buscarPublicadasPorCriterios               | CU-06, alimenta a EnsambladorSimulacroService             |
| ProcesoDeRevisionRepository   | ProcesoDeRevision             | obtenerPorPregunta                         | CU-10, CU-11, CU-12                                       |
| CompetenciaRepository         | Competencia (+ Tema, Subtema) | listarTodas                                | Administración del Catálogo (D-11)                        |
| SimulacroRepository           | Simulacro                     | buscarDisponiblesParaEstudiante            | CU-13, CU-14                                              |
| IntentoDeSimulacroRepository  | IntentoDeSimulacro            | buscarCalificadosPorEstudiante             | CU-15, CU-16, alimenta a GeneradorReporteAcademicoService |
| ReporteAcademicoRepository    | ReporteAcademico              | guardar (sin actualizar, es inmutable)     | CU-17                                                     |
| UsuarioRepository             | Usuario                       | obtenerPorCorreo                           | CU-01, CU-02                                              |
| RegistroDeAuditoriaRepository | RegistroDeAuditoria           | buscarPorRangoDeFecha                      |                                                           |

---

## B.8 Casos de uso implementados (CU-04 a CU-15 y CU-18)

#### CU-04. Crear pregunta

- **Actor:** Autor de preguntas
- **Entrada:** Contexto, Pregunta directa, tres Distractores, Respuesta correcta, Justificación, Bibliografía, Competencia, Tema, Subtema y Nivel de dificultad.
- **Precondiciones:** El Autor se encuentra autenticado con el Rol Autor de preguntas. Existen Competencias, Temas y Subtemas en el Catálogo académico.
- **Proceso:** 1. El Autor diligencia los componentes de la Pregunta.
  2. El sistema crea la Pregunta en estado Borrador y la asocia a su Autor.
  3. El sistema ejecuta la Validación estructural.
  4. Si la Pregunta supera todas las verificaciones, transita a En construcción; en caso contrario permanece en Borrador con el detalle de lo que falta.
- **Reglas de negocio:** Una Pregunta debe tener exactamente cuatro Distractores y una única Respuesta correcta (D-01, RF-10, RF-11). Debe existir una y solo una Pregunta directa (RF-09). El Contexto es obligatorio (RF-08). Los Distractores no pueden emplear expresiones como todas las anteriores o ninguna de las anteriores (RF-12) y deben respetar criterios de longitud y estructura gramatical (RF-13). Toda Pregunta nace en estado Borrador (D-02).
- **Eventos generados:** PreguntaCreada, ValidacionEstructuralSuperada
- **Resultado:** Pregunta registrada en el Banco de preguntas, en estado Borrador o En construcción según el resultado de la validación.

#### CU-05. Modificar pregunta

- **Actor:** Autor de preguntas
- **Entrada:** Identificador de la Pregunta y componentes a modificar.
- **Precondiciones:** El actor es el Autor de la Pregunta. La Pregunta se encuentra en estado Borrador o En construcción.
- **Proceso:** 1. El Autor selecciona una Pregunta suya en estado editable.
  2. Modifica los componentes requeridos.
  3. El sistema vuelve a ejecutar la Validación estructural.
  4. El sistema actualiza el estado según el resultado y registra el cambio en la Trazabilidad con fecha y Usuario responsable.
- **Reglas de negocio:** Solo se puede modificar una Pregunta en estados editables: Borrador y En construcción (RF-06, D-02). Una Pregunta Rechazada retorna a En construcción y vuelve a ser modificable (D-07). Toda modificación queda registrada en la Trazabilidad (RF-29, RF-30). Las validaciones estructurales se reevalúan en cada modificación.
- **Eventos generados:** PreguntaModificada
- **Resultado:** Pregunta actualizada, con su Trazabilidad ampliada y su estado recalculado.

#### CU-06. Consultar preguntas mediante filtros

- **Actor:** Autor de preguntas, Revisor, Docente, Administrador
- **Entrada:** Criterios de filtro: Competencia, Tema, Subtema, Nivel de dificultad, Estado del ciclo de vida o Autor.
- **Precondiciones:** El actor se encuentra autenticado.
- **Proceso:** 1. El actor indica los criterios de filtro.
  2. El sistema consulta el Banco de preguntas aplicando además las restricciones derivadas del Rol del actor.
  3. El sistema devuelve el listado de Preguntas coincidentes.
- **Reglas de negocio:** El resultado se restringe según el Rol: un Autor consulta sus propias Preguntas, un Revisor las que le fueron asignadas, un Docente las Publicadas y un Administrador la totalidad (RNF-07). Las Preguntas Archivadas siguen siendo consultables (RNF-16). La consulta debe responder en menos de tres segundos (RNF-04).
- **Eventos generados:** Ninguno
- **Resultado:** Listado de Preguntas que satisfacen los criterios, visible según el Rol del actor.

#### CU-07. Someter pregunta a revisión

- **Actor:** Autor de preguntas
- **Entrada:** Identificador de la Pregunta a someter.
- **Precondiciones:** El actor es el Autor de la Pregunta. La Pregunta se encuentra en estado En construcción, es decir, ya superó la Validación estructural.
- **Proceso:** 1. El Autor selecciona la Pregunta.
  2. El sistema verifica que la Pregunta supere la Validación estructural.
  3. El sistema transiciona la Pregunta a Pendiente de revisión.
  4. La Pregunta deja de ser editable y queda a la espera de asignación de Revisores.
- **Reglas de negocio:** Solo una Pregunta En construcción puede someterse a revisión (D-02). Una Pregunta en Borrador no puede someterse porque no ha superado la Validación estructural. A partir de Pendiente de revisión la Pregunta no es editable. La transición queda registrada en la Trazabilidad y la Auditoría (RF-15, RNF-09).
- **Eventos generados:** PreguntaSometidaARevision
- **Resultado:** Pregunta en estado Pendiente de revisión, disponible para asignación de Revisores.

#### CU-08. Publicar pregunta

- **Actor:** Administrador
- **Entrada:** Identificador de la Pregunta a publicar.
- **Precondiciones:** La Pregunta se encuentra en estado Aprobada.
- **Proceso:** 1. El Administrador selecciona una Pregunta Aprobada.
  2. El sistema verifica que el estado permita la transición.
  3. El sistema transiciona la Pregunta a Publicada.
  4. El sistema registra la transición en la Trazabilidad y la Auditoría.
- **Reglas de negocio:** Solo una Pregunta Aprobada puede transitar a Publicada (RF-15). Una Pregunta Rechazada no puede publicarse directamente. A partir de este momento la Pregunta queda disponible para integrar Simulacros (RF-21).
- **Eventos generados:** PreguntaPublicada
- **Resultado:** Pregunta disponible en el conjunto de Preguntas Publicadas del Banco de preguntas.

#### CU-09. Archivar pregunta

- **Actor:** Administrador
- **Entrada:** Identificador de la Pregunta y motivo del archivado.
- **Precondiciones:** La Pregunta se encuentra en estado Publicada.
- **Proceso:** 1. El Administrador selecciona la Pregunta a retirar.
  2. Registra el motivo.
  3. El sistema transiciona la Pregunta a Archivada.
  4. El sistema deja de considerarla en la generación de nuevos Simulacros.
- **Reglas de negocio:** Ninguna Pregunta se elimina físicamente del Banco de preguntas (RNF-16). Una Pregunta Archivada conserva íntegras su Trazabilidad y su Historial de revisiones. Los Intentos de simulacro ya realizados que la incluyeron no se ven afectados.
- **Eventos generados:** PreguntaArchivada
- **Resultado:** Pregunta retirada del uso activo, conservada con toda su historia.

#### CU-10. Asignar revisores a una pregunta

- **Actor:** Administrador
- **Entrada:** Identificador de la Pregunta y conjunto de Usuarios con Rol Revisor.
- **Precondiciones:** La Pregunta se encuentra en estado Pendiente de revisión. Existen al menos dos Usuarios con Rol Revisor distintos del Autor.
- **Proceso:** 1. El Administrador selecciona la Pregunta.
  2. Selecciona los Revisores.
  3. El sistema verifica que sean al menos dos y que ninguno sea el Autor.
  4. El sistema abre el Proceso de revisión con las Asignaciones de revisor.
  5. La Pregunta transiciona a En revisión.
- **Reglas de negocio:** Deben asignarse mínimo dos Revisores (D-03). Ningún Revisor puede ser el Autor de la Pregunta (D-06). Un mismo Revisor no puede ser asignado dos veces a la misma Pregunta. Una vez abierto el Proceso de revisión, la Pregunta no es editable.
- **Eventos generados:** RevisoresAsignados, ProcesoDeRevisionAbierto
- **Resultado:** Pregunta en estado En revisión, con su Proceso de revisión abierto y sus Revisores notificados.

#### CU-11. Diligenciar formato de evaluación

- **Actor:** Revisor
- **Entrada:** Identificador de la Pregunta, calificación de los criterios del Formato de evaluación, Observaciones y decisión aprobatoria o reprobatoria.
- **Precondiciones:** El Revisor tiene una Asignación de revisor vigente sobre la Pregunta. La Pregunta se encuentra en estado En revisión. El Revisor no ha emitido aún su evaluación.
- **Proceso:** 1. El Revisor consulta la Pregunta asignada.
  2. Diligencia el Formato de evaluación calificando los criterios pedagógicos, técnicos y estructurales.
  3. Registra sus Observaciones.
  4. Emite su decisión aprobatoria o reprobatoria.
  5. El sistema incorpora la evaluación al Proceso de revisión y al Historial de revisiones.
- **Reglas de negocio:** Cada Revisor asignado diligencia un único Formato de evaluación por Pregunta (RF-17). Una evaluación emitida no puede modificarse, para preservar la integridad del Historial de revisiones. Las Observaciones quedan asociadas al Revisor que las emitió (RF-18). El Revisor no puede evaluar una Pregunta de la que es Autor (D-06).
- **Eventos generados:** EvaluacionRegistrada
- **Resultado:** Formato de evaluación incorporado al Proceso de revisión y al Historial de revisiones de la Pregunta.

#### CU-12. Emitir dictamen de la revisión

- **Actor:** Sistema (disparado automáticamente por el registro de la última evaluación pendiente)
- **Entrada:** Conjunto completo de Formatos de evaluación del Proceso de revisión.
- **Precondiciones:** Todos los Revisores asignados a la Pregunta han emitido su evaluación. La Pregunta se encuentra en estado En revisión.
- **Proceso:** 1. El sistema verifica que no queden evaluaciones pendientes.
  2. Calcula el porcentaje de Formatos de evaluación aprobatorios sobre el total de Revisores asignados.
  3. Si el porcentaje supera el 70%, transiciona la Pregunta a Aprobada; en caso contrario la transiciona a Rechazada y luego a En construcción.
  4. Registra el Dictamen en el Historial de revisiones y la transición en la Auditoría.
- **Reglas de negocio:** El Dictamen se calcula únicamente cuando el 100% de los Revisores asignados ha evaluado (D-04). El umbral de aprobación es estrictamente superior al 70% (D-04). Con dos o tres Revisores esto equivale a unanimidad (D-05). Una Pregunta Rechazada retorna a En construcción conservando su Historial de revisiones (D-07). El Dictamen es inmutable una vez emitido.
- **Eventos generados:** DictamenEmitido y, según el resultado, PreguntaAprobada o PreguntaRechazada
- **Resultado:** Pregunta Aprobada y lista para publicación, o Pregunta devuelta al Autor en estado En construcción con las Observaciones de los Revisores.

#### CU-13. Definir simulacro

- **Actor:** Docente
- **Entrada:** Nombre del Simulacro, criterios de generación (Competencias, Temas, Subtemas y Niveles de dificultad), número de Preguntas y Duración máxima.
- **Precondiciones:** El Docente se encuentra autenticado. Existe un número suficiente de Preguntas Publicadas que satisfacen los criterios indicados.
- **Proceso:** 1. El Docente establece los criterios de generación y la Duración máxima.
  2. El sistema consulta las Preguntas Publicadas que los satisfacen.
  3. El sistema verifica que la cantidad disponible sea suficiente.
  4. El sistema selecciona aleatoriamente las Preguntas respetando la distribución solicitada.
  5. El sistema registra el Simulacro.
- **Reglas de negocio:** Un Simulacro solo puede componerse de Preguntas Publicadas (RF-21). La selección es aleatoria dentro de los criterios de Competencia, Tema y Nivel de dificultad (RF-22). La Duración máxima debe ser un valor positivo. Un Simulacro no puede quedar sin Preguntas. Si no hay Preguntas Publicadas suficientes, la definición no se completa.
- **Eventos generados:** SimulacroDefinido
- **Resultado:** Simulacro registrado y disponible para ser presentado por los Estudiantes.

#### CU-14. Presentar simulacro

- **Actor:** Estudiante
- **Entrada:** Identificador del Simulacro y, progresivamente, las Respuestas del estudiante.
- **Precondiciones:** El Estudiante se encuentra autenticado. El Simulacro existe y está disponible para él.
- **Proceso:** 1. El Estudiante inicia el Simulacro.
  2. El sistema abre un Intento de simulacro registrando el instante de inicio.
  3. El sistema presenta las Preguntas ocultando cuál es la Respuesta correcta y su Justificación.
  4. El Estudiante registra una Respuesta del estudiante por Pregunta.
  5. El Intento finaliza cuando el Estudiante lo cierra o cuando se vence la Duración máxima.
- **Reglas de negocio:** Un Estudiante registra a lo sumo una Respuesta del estudiante por Pregunta dentro de un Intento. Las respuestas solo pueden modificarse mientras el Intento esté en curso. Al vencerse la Duración máxima el Intento se cierra automáticamente con las respuestas registradas hasta ese momento (RF-23). La Respuesta correcta y la Justificación no se revelan durante el Intento.
- **Eventos generados:** IntentoDeSimulacroIniciado, IntentoDeSimulacroFinalizado
- **Resultado:** Intento de simulacro cerrado, listo para ser calificado.

#### CU-15. Calificar intento de simulacro

- **Actor:** Sistema (disparado por el cierre del Intento de simulacro)
- **Entrada:** Intento de simulacro finalizado, con sus Respuestas del estudiante.
- **Precondiciones:** El Intento de simulacro se encuentra finalizado y aún no ha sido calificado.
- **Proceso:** 1. El sistema compara cada Respuesta del estudiante con la Respuesta correcta de la Pregunta.
  2. Calcula la Calificación global.
  3. Desagrega el resultado por Competencia.
  4. Registra la Calificación en el Intento de simulacro y lo marca como calificado.
- **Reglas de negocio:** La calificación es automática y sin intervención manual (RF-24). Un Intento se califica una sola vez y su Calificación es inmutable. Las Preguntas sin respuesta se computan como incorrectas. El archivado posterior de una Pregunta no altera Calificaciones ya emitidas.
- **Eventos generados:** IntentoDeSimulacroCalificado
- **Resultado:** Intento de simulacro calificado, con resultado global y desagregado por Competencia, incorporado al historial del Estudiante.

#### CU-18. Consultar trazabilidad de una pregunta

- **Actor:** Administrador
- **Entrada:** Identificador de la Pregunta.
- **Precondiciones:** El Administrador se encuentra autenticado. La Pregunta existe en el Banco de preguntas, en cualquier estado.
- **Proceso:** 1. El Administrador selecciona la Pregunta.
  2. El sistema recupera el registro completo de modificaciones y Transiciones de estado.
  3. El sistema recupera el Historial de revisiones asociado.
  4. Presenta la historia en orden cronológico.
- **Reglas de negocio:** Toda modificación registra fecha y Usuario responsable (RF-30). La Trazabilidad se conserva aunque la Pregunta esté Archivada (RNF-16). Los registros de Trazabilidad y el Historial de revisiones son inmutables. La consulta no altera el estado de la Pregunta.
- **Eventos generados:** Ninguno
- **Resultado:** Historia completa de la Pregunta: modificaciones, Transiciones de estado, evaluaciones y Dictámenes, en orden cronológico.
