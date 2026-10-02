-- =============================================================================
-- servicio-editorial · V1 · Esquema inicial (Flyway). Hibernate solo valida (ddl-auto=validate).
--
-- Decisiones:
-- * Identificadores en columnas uuid y fechas en timestamptz, siempre en UTC (CONTRATOS.md 4).
-- * Bloqueo optimista: columna `version` en las dos raíces de agregado (CONTRATOS.md 11.1, excepción a 3.3.5).
-- * Las colecciones de value objects (opciones, bibliografía, trazabilidad, historial, asignaciones, formatos,
--   criterios y observaciones) van en TABLAS HIJAS y no en columnas jsonb, porque:
--     1) los filtros de CU-06 consultan las asignaciones por SQL (EXISTS sobre proceso_asignacion);
--     2) evita depender del soporte de Hibernate 7 para Jackson 3 en columnas JSON;
--     3) cada fila queda tipada y con restricciones CHECK, igual que el dominio.
-- * Las tablas hijas usan como clave (dueño, posición): la posición conserva el orden del dominio.
-- * Nunca se borra una Pregunta ni un Proceso (RNF-16, INV-12). La trazabilidad y el historial son de solo
--   anexado (INV-13, INV-14): el mapper solo inserta las entradas nuevas.
-- =============================================================================

-- Agregado Pregunta ------------------------------------------------------------

CREATE TABLE pregunta (
    id                  uuid         PRIMARY KEY,
    autor_id            uuid         NOT NULL,
    contexto            text         NOT NULL,
    pregunta_directa    text         NOT NULL,
    justificacion       text         NOT NULL,
    competencia_id      uuid         NOT NULL,
    tema_id             uuid         NOT NULL,
    subtema_id          uuid         NOT NULL,
    nivel_dificultad    varchar(10)  NOT NULL CHECK (nivel_dificultad IN ('BAJO', 'MEDIO', 'ALTO')),
    estado              varchar(20)  NOT NULL CHECK (estado IN ('BORRADOR', 'EN_CONSTRUCCION', 'PENDIENTE_REVISION',
                                                                'EN_REVISION', 'APROBADA', 'RECHAZADA', 'PUBLICADA',
                                                                'ARCHIVADA')),
    fecha_creacion      timestamptz  NOT NULL,
    fecha_actualizacion timestamptz  NOT NULL,
    version             bigint       NOT NULL
);

-- Filtros de GET /preguntas (CONTRATOS.md 8.1) y orden de la paginación.
CREATE INDEX ix_pregunta_autor ON pregunta (autor_id);
CREATE INDEX ix_pregunta_estado ON pregunta (estado);
CREATE INDEX ix_pregunta_clasificacion ON pregunta (competencia_id, tema_id, subtema_id);
CREATE INDEX ix_pregunta_nivel ON pregunta (nivel_dificultad);
CREATE INDEX ix_pregunta_orden ON pregunta (fecha_creacion, id);

CREATE TABLE pregunta_opcion (
    pregunta_id uuid    NOT NULL REFERENCES pregunta (id),
    posicion    integer NOT NULL,
    letra       text    NOT NULL,
    texto       text    NOT NULL,
    es_correcta boolean NOT NULL,
    PRIMARY KEY (pregunta_id, posicion)
);

CREATE TABLE pregunta_bibliografia (
    pregunta_id uuid    NOT NULL REFERENCES pregunta (id),
    posicion    integer NOT NULL,
    referencia  text    NOT NULL,
    PRIMARY KEY (pregunta_id, posicion)
);

-- RegistroDeTrazabilidad: solo anexado (INV-13, RF-29, RF-30).
CREATE TABLE pregunta_trazabilidad (
    pregunta_id     uuid        NOT NULL REFERENCES pregunta (id),
    secuencia       integer     NOT NULL,
    fecha           timestamptz NOT NULL,
    usuario_id      uuid        NOT NULL,
    tipo            varchar(20) NOT NULL CHECK (tipo IN ('CREACION', 'MODIFICACION', 'TRANSICION')),
    estado_anterior varchar(20),
    estado_nuevo    varchar(20) NOT NULL,
    detalle         text        NOT NULL,
    PRIMARY KEY (pregunta_id, secuencia)
);

-- HistorialDeRevisiones: solo anexado (INV-14, D-15). Una fila por evaluación o dictamen.
CREATE TABLE pregunta_historial (
    id                    uuid         PRIMARY KEY,
    pregunta_id           uuid         NOT NULL REFERENCES pregunta (id),
    secuencia             integer      NOT NULL,
    tipo                  varchar(10)  NOT NULL CHECK (tipo IN ('EVALUACION', 'DICTAMEN')),
    proceso_id            uuid         NOT NULL,
    fecha                 timestamptz  NOT NULL,
    revisor_id            uuid,
    decision              varchar(15)  CHECK (decision IN ('APROBATORIA', 'REPROBATORIA')),
    resultado             varchar(10)  CHECK (resultado IN ('APROBADA', 'RECHAZADA')),
    porcentaje_aprobacion numeric(5, 2),
    UNIQUE (pregunta_id, secuencia)
);

CREATE TABLE pregunta_historial_criterio (
    entrada_id uuid        NOT NULL REFERENCES pregunta_historial (id),
    posicion   integer     NOT NULL,
    criterio   varchar(15) NOT NULL CHECK (criterio IN ('PEDAGOGICO', 'TECNICO', 'ESTRUCTURAL')),
    valoracion integer     NOT NULL CHECK (valoracion BETWEEN 1 AND 5),
    PRIMARY KEY (entrada_id, posicion)
);

CREATE TABLE pregunta_historial_observacion (
    entrada_id uuid    NOT NULL REFERENCES pregunta_historial (id),
    posicion   integer NOT NULL,
    texto      text    NOT NULL,
    PRIMARY KEY (entrada_id, posicion)
);

-- Agregado ProcesoDeRevision ---------------------------------------------------

CREATE TABLE proceso_revision (
    id                   uuid         PRIMARY KEY,
    pregunta_id          uuid         NOT NULL REFERENCES pregunta (id),
    autor_id             uuid         NOT NULL,
    fecha_apertura       timestamptz  NOT NULL,
    estado               varchar(10)  NOT NULL CHECK (estado IN ('ABIERTO', 'CERRADO')),
    dictamen_resultado   varchar(10)  CHECK (dictamen_resultado IN ('APROBADA', 'RECHAZADA')),
    dictamen_porcentaje  numeric(5, 2),
    dictamen_fecha       timestamptz,
    version              bigint       NOT NULL
);

CREATE INDEX ix_proceso_pregunta ON proceso_revision (pregunta_id, fecha_apertura);
CREATE INDEX ix_proceso_estado ON proceso_revision (estado, fecha_apertura, id);

-- AsignacionDeRevisor: un revisor una sola vez por proceso (INV-17).
CREATE TABLE proceso_asignacion (
    proceso_id       uuid        NOT NULL REFERENCES proceso_revision (id),
    posicion         integer     NOT NULL,
    revisor_id       uuid        NOT NULL,
    fecha_asignacion timestamptz NOT NULL,
    PRIMARY KEY (proceso_id, posicion),
    UNIQUE (proceso_id, revisor_id)
);

-- CU-06: procesos de un revisor y preguntas asignadas.
CREATE INDEX ix_asignacion_revisor ON proceso_asignacion (revisor_id);

-- FormatoDeEvaluacion: a lo sumo uno por revisor y proceso (INV-18).
CREATE TABLE formato_evaluacion (
    id            uuid        PRIMARY KEY,
    proceso_id    uuid        NOT NULL REFERENCES proceso_revision (id),
    posicion      integer     NOT NULL,
    revisor_id    uuid        NOT NULL,
    decision      varchar(15) NOT NULL CHECK (decision IN ('APROBATORIA', 'REPROBATORIA')),
    fecha_emision timestamptz NOT NULL,
    UNIQUE (proceso_id, posicion),
    UNIQUE (proceso_id, revisor_id)
);

CREATE TABLE formato_criterio (
    formato_id uuid        NOT NULL REFERENCES formato_evaluacion (id),
    posicion   integer     NOT NULL,
    criterio   varchar(15) NOT NULL CHECK (criterio IN ('PEDAGOGICO', 'TECNICO', 'ESTRUCTURAL')),
    valoracion integer     NOT NULL CHECK (valoracion BETWEEN 1 AND 5),
    PRIMARY KEY (formato_id, posicion)
);

CREATE TABLE formato_observacion (
    formato_id uuid    NOT NULL REFERENCES formato_evaluacion (id),
    posicion   integer NOT NULL,
    texto      text    NOT NULL,
    PRIMARY KEY (formato_id, posicion)
);
