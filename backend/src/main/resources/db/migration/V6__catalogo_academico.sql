-- =====================================================================
-- V6 · Catálogo académico (HU-52 a HU-56) y ambientes físicos (HU-43)
-- Figma > Personal Administrativo › Configuraciones:
--   "CI-14 · Plan curricular: áreas, competencias y cursos (HU-52, 53, 54)" y sus modales
--   "CI-17 · Plan de evaluación (HU-56)" y modal "<curso> · <grado>"
--   "CI-23 · Escala de calificación — tabla de conversión vigente (HU-55)" y "Nueva versión"
--   "CI-26 · Ambientes físicos (HU-43)" y "Nuevo ambiente físico"
-- Las áreas, competencias, cursos y ambientes no se borran: se desactivan (RF-126 a RF-128, RF-112).
-- Reglas que cruzan tablas y las valida el servicio: la competencia de un plan es del área del curso,
-- la sección del docente es del mismo grado y año, y los rangos de la tabla de conversión cubren 0–20
-- sin vacíos ni cruces.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS academico;

-- ---------------------------------------------------------------------
-- HU-52 · Áreas curriculares.
-- ---------------------------------------------------------------------
CREATE TABLE academico.area (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre         VARCHAR(80)  NOT NULL UNIQUE,
    descripcion    VARCHAR(500),
    orden          SMALLINT     NOT NULL DEFAULT 1,
    estado         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVA',
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_area_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

-- HU-53 · Competencias de cada área.
CREATE TABLE academico.competencia (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id        UUID         NOT NULL REFERENCES academico.area (id),
    nombre         VARCHAR(150) NOT NULL,
    descripcion    VARCHAR(500),
    orden          SMALLINT     NOT NULL DEFAULT 1,
    estado         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVA',
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_competencia_nombre UNIQUE (area_id, nombre),
    CONSTRAINT ck_competencia_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

-- HU-54 · Cursos de cada área y los grados en los que se dictan (RF-128).
CREATE TABLE academico.curso (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id        UUID        NOT NULL REFERENCES academico.area (id),
    nombre         VARCHAR(80) NOT NULL UNIQUE,
    estado         VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_curso_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE academico.curso_grado (
    curso_id     UUID        NOT NULL REFERENCES academico.curso (id) ON DELETE CASCADE,
    grado_codigo VARCHAR(10) NOT NULL REFERENCES configuracion.grado (codigo),
    PRIMARY KEY (curso_id, grado_codigo)
);

-- ---------------------------------------------------------------------
-- HU-56 · Plan de evaluación de un año: qué cursos se dictan en cada grado, sus horas,
-- qué competencias evalúan (con escala y reglas de nota) y qué docente los dicta en cada sección.
-- ---------------------------------------------------------------------
CREATE TABLE academico.plan_curso (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id  UUID        NOT NULL REFERENCES configuracion.anio_escolar (id) ON DELETE CASCADE,
    grado_codigo     VARCHAR(10) NOT NULL,
    curso_id         UUID        NOT NULL,
    horas_semanales  SMALLINT    NOT NULL,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_plan_curso UNIQUE (anio_escolar_id, grado_codigo, curso_id),
    -- Solo se planifica un curso en un grado donde se dicta.
    CONSTRAINT fk_plan_curso_grado FOREIGN KEY (curso_id, grado_codigo)
        REFERENCES academico.curso_grado (curso_id, grado_codigo),
    CONSTRAINT ck_plan_curso_horas CHECK (horas_semanales BETWEEN 1 AND 20)
);

-- RF-130 a RF-132: escala, regla de nota final y consolidación de periodos de cada competencia.
CREATE TABLE academico.plan_competencia (
    plan_curso_id  UUID        NOT NULL REFERENCES academico.plan_curso (id) ON DELETE CASCADE,
    competencia_id UUID        NOT NULL REFERENCES academico.competencia (id),
    escala         VARCHAR(12) NOT NULL,
    regla_nota     VARCHAR(20) NOT NULL,
    consolidacion  VARCHAR(20) NOT NULL,
    PRIMARY KEY (plan_curso_id, competencia_id),
    CONSTRAINT ck_plan_competencia_escala CHECK (escala IN ('VIGESIMAL', 'CUALITATIVA')),
    CONSTRAINT ck_plan_competencia_regla CHECK (
        (escala = 'VIGESIMAL' AND regla_nota IN ('PROMEDIO_SIMPLE', 'PROMEDIO_PONDERADO'))
        OR (escala = 'CUALITATIVA' AND regla_nota IN ('NIVEL_MAS_FRECUENTE', 'ULTIMO_NIVEL'))),
    CONSTRAINT ck_plan_competencia_consolidacion CHECK (consolidacion IN ('PROMEDIO_PERIODOS', 'ULTIMO_PERIODO'))
);

-- Docente que dicta un curso del plan en cada sección (CI-17: "2 secciones × 16 cursos").
CREATE TABLE academico.docente_seccion (
    plan_curso_id UUID NOT NULL REFERENCES academico.plan_curso (id) ON DELETE CASCADE,
    seccion_id    UUID NOT NULL REFERENCES configuracion.seccion (id) ON DELETE CASCADE,
    docente_id    UUID NOT NULL REFERENCES seguridad.usuario (id),
    PRIMARY KEY (plan_curso_id, seccion_id)
);

CREATE INDEX ix_docente_seccion_docente ON academico.docente_seccion (docente_id);

-- ---------------------------------------------------------------------
-- HU-55 · Tabla de conversión vigesimal → AD/A/B/C, con versiones por fecha de vigencia.
-- ---------------------------------------------------------------------
CREATE TABLE academico.tabla_conversion (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vigente_desde  DATE        NOT NULL UNIQUE,
    creado_por     UUID REFERENCES seguridad.usuario (id),
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE academico.tabla_conversion_nivel (
    tabla_id    UUID        NOT NULL REFERENCES academico.tabla_conversion (id) ON DELETE CASCADE,
    nivel       VARCHAR(2)  NOT NULL,
    significado VARCHAR(40) NOT NULL,
    nota_desde  SMALLINT    NOT NULL,
    nota_hasta  SMALLINT    NOT NULL,
    PRIMARY KEY (tabla_id, nivel),
    CONSTRAINT uq_tabla_conversion_desde UNIQUE (tabla_id, nota_desde),
    CONSTRAINT ck_tabla_conversion_nivel CHECK (nivel IN ('AD', 'A', 'B', 'C')),
    CONSTRAINT ck_tabla_conversion_rango CHECK (nota_desde BETWEEN 0 AND 20 AND nota_hasta BETWEEN 0 AND 20
        AND nota_desde <= nota_hasta)
);

-- ---------------------------------------------------------------------
-- HU-43 · Ambientes físicos (aulas y espacios especiales). Las secciones usan un aula regular.
-- ---------------------------------------------------------------------
CREATE TABLE configuracion.ambiente (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo         VARCHAR(20)   NOT NULL UNIQUE,
    nombre         VARCHAR(80)   NOT NULL,
    tipo           VARCHAR(20)   NOT NULL,
    aforo_maximo   SMALLINT      NOT NULL,
    recursos       VARCHAR(60)[] NOT NULL DEFAULT '{}',
    estado         VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO',
    creado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_ambiente_tipo CHECK (tipo IN ('AULA_REGULAR', 'ESPACIO_ESPECIAL')),
    -- "Un aula regular admite como máximo 45" (Modal - Nuevo ambiente físico).
    CONSTRAINT ck_ambiente_aforo CHECK (aforo_maximo > 0 AND (tipo <> 'AULA_REGULAR' OR aforo_maximo <= 45)),
    CONSTRAINT ck_ambiente_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

ALTER TABLE configuracion.seccion ADD COLUMN ambiente_id UUID REFERENCES configuracion.ambiente (id);
