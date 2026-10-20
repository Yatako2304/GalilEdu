-- =====================================================================
-- V4 · Configuración del año escolar (HU-74, HU-75, HU-44, HU-23, HU-34)
-- Figma > Personal Administrativo:
--   "CI-01 / CI-03 / CI-06 · Año escolar" y "Modal - Nuevo año escolar"     (HU-74)
--   "CI-09 · Secciones por grado y tutores" y "Modal - Nueva sección"       (HU-75)
--   "CI-12 · Estructura horaria" y "Modal - Nueva estructura horaria"       (HU-44)
--   "13 · Matrícula › Configuración de matrícula"                          (HU-23)
--   "P19 · Configuración › Tarifario y calendario"                         (HU-34)
-- Un esquema por módulo: configuracion (año, periodos, grados, secciones, horario),
-- matricula (periodo de matrícula) y pagos (tarifario). Las reglas que dependen de
-- otros datos (p. ej. "la jornada termina a las 18:00") las valida el servicio.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS configuracion;
CREATE SCHEMA IF NOT EXISTS matricula;
CREATE SCHEMA IF NOT EXISTS pagos;

-- ---------------------------------------------------------------------
-- HU-74 · Año escolar. Se crea PLANIFICADO; solo uno puede estar ACTIVO.
-- ---------------------------------------------------------------------
CREATE TABLE configuracion.anio_escolar (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio           SMALLINT    NOT NULL UNIQUE,
    fecha_inicio   DATE        NOT NULL,
    fecha_fin      DATE        NOT NULL,
    regimen        VARCHAR(10) NOT NULL,
    estado         VARCHAR(12) NOT NULL DEFAULT 'PLANIFICADO',
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_anio_escolar_anio CHECK (anio BETWEEN 2000 AND 2100),
    CONSTRAINT ck_anio_escolar_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_anio_escolar_regimen CHECK (regimen IN ('BIMESTRAL', 'TRIMESTRAL')),
    CONSTRAINT ck_anio_escolar_estado CHECK (estado IN ('PLANIFICADO', 'ACTIVO', 'CERRADO'))
);

-- "Solo un año puede estar activo a la vez" (CI-01).
CREATE UNIQUE INDEX ux_anio_escolar_un_activo ON configuracion.anio_escolar (estado) WHERE estado = 'ACTIVO';

-- Periodos académicos: 4 bimestres o 3 trimestres, generados según el régimen.
CREATE TABLE configuracion.periodo_academico (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id    UUID        NOT NULL REFERENCES configuracion.anio_escolar (id) ON DELETE CASCADE,
    numero             SMALLINT    NOT NULL,
    nombre             VARCHAR(30) NOT NULL,
    fecha_inicio       DATE        NOT NULL,
    fecha_fin          DATE        NOT NULL,
    fecha_limite_notas DATE        NOT NULL,
    estado             VARCHAR(12) NOT NULL DEFAULT 'PLANIFICADO',
    CONSTRAINT uq_periodo_academico_numero UNIQUE (anio_escolar_id, numero),
    CONSTRAINT ck_periodo_academico_numero CHECK (numero BETWEEN 1 AND 4),
    CONSTRAINT ck_periodo_academico_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_periodo_academico_limite CHECK (fecha_limite_notas >= fecha_fin),
    CONSTRAINT ck_periodo_academico_estado CHECK (estado IN ('PLANIFICADO', 'EN_CURSO', 'CERRADO'))
);

-- ---------------------------------------------------------------------
-- Catálogo fijo de grados (11 grados según CI-06: 1.° a 6.° de primaria y 1.° a 5.° de secundaria).
-- ---------------------------------------------------------------------
CREATE TABLE configuracion.grado (
    codigo VARCHAR(10) PRIMARY KEY,
    nivel  VARCHAR(20) NOT NULL,
    numero SMALLINT    NOT NULL,
    nombre VARCHAR(40) NOT NULL,
    orden  SMALLINT    NOT NULL,
    CONSTRAINT uq_grado_nivel_numero UNIQUE (nivel, numero),
    CONSTRAINT ck_grado_nivel CHECK (nivel IN ('PRIMARIA', 'SECUNDARIA'))
);

INSERT INTO configuracion.grado (codigo, nivel, numero, nombre, orden) VALUES
    ('PRI-1', 'PRIMARIA',   1, '1.° de primaria',    1),
    ('PRI-2', 'PRIMARIA',   2, '2.° de primaria',    2),
    ('PRI-3', 'PRIMARIA',   3, '3.° de primaria',    3),
    ('PRI-4', 'PRIMARIA',   4, '4.° de primaria',    4),
    ('PRI-5', 'PRIMARIA',   5, '5.° de primaria',    5),
    ('PRI-6', 'PRIMARIA',   6, '6.° de primaria',    6),
    ('SEC-1', 'SECUNDARIA', 1, '1.° de secundaria',  7),
    ('SEC-2', 'SECUNDARIA', 2, '2.° de secundaria',  8),
    ('SEC-3', 'SECUNDARIA', 3, '3.° de secundaria',  9),
    ('SEC-4', 'SECUNDARIA', 4, '4.° de secundaria', 10),
    ('SEC-5', 'SECUNDARIA', 5, '5.° de secundaria', 11);

-- ---------------------------------------------------------------------
-- HU-75 · Secciones de cada grado en un año escolar. La capacidad máxima son las
-- "vacantes por sección" de la configuración de matrícula. Tutor y cotutor son usuarios
-- (rol TUTOR/DOCENTE); se asignan después de crear la sección.
-- ---------------------------------------------------------------------
CREATE TABLE configuracion.seccion (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id  UUID        NOT NULL REFERENCES configuracion.anio_escolar (id) ON DELETE CASCADE,
    grado_codigo     VARCHAR(10) NOT NULL REFERENCES configuracion.grado (codigo),
    nombre           VARCHAR(5)  NOT NULL,
    capacidad_maxima SMALLINT    NOT NULL,
    tutor_id         UUID REFERENCES seguridad.usuario (id),
    cotutor_id       UUID REFERENCES seguridad.usuario (id),
    estado           VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_seccion_nombre UNIQUE (anio_escolar_id, grado_codigo, nombre),
    CONSTRAINT ck_seccion_capacidad CHECK (capacidad_maxima BETWEEN 1 AND 45),
    CONSTRAINT ck_seccion_tutores CHECK (cotutor_id IS NULL OR cotutor_id <> tutor_id),
    CONSTRAINT ck_seccion_estado CHECK (estado IN ('ACTIVA', 'INACTIVA'))
);

-- ---------------------------------------------------------------------
-- HU-44 · Estructura horaria del año (p. ej. EH-PRI-26, EH-SEC-26). Los horarios de cada
-- bloque y receso se calculan desde la hora de entrada, la duración y los recesos.
-- ---------------------------------------------------------------------
CREATE TABLE configuracion.estructura_horaria (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id       UUID         NOT NULL REFERENCES configuracion.anio_escolar (id) ON DELETE CASCADE,
    codigo                VARCHAR(20)  NOT NULL,
    descripcion           VARCHAR(100),
    dias_lectivos         VARCHAR(3)[] NOT NULL,
    hora_entrada          TIME         NOT NULL,
    bloques_por_dia       SMALLINT     NOT NULL,
    duracion_bloque_min   SMALLINT     NOT NULL,
    creado_en             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_estructura_horaria_codigo UNIQUE (anio_escolar_id, codigo),
    CONSTRAINT ck_estructura_horaria_dias CHECK (
        cardinality(dias_lectivos) > 0
        AND dias_lectivos <@ ARRAY['LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB']::VARCHAR(3)[]),
    CONSTRAINT ck_estructura_horaria_bloques CHECK (bloques_por_dia BETWEEN 1 AND 12),
    CONSTRAINT ck_estructura_horaria_duracion CHECK (duracion_bloque_min BETWEEN 20 AND 120)
);

CREATE TABLE configuracion.receso (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estructura_horaria_id UUID     NOT NULL REFERENCES configuracion.estructura_horaria (id) ON DELETE CASCADE,
    despues_de_bloque     SMALLINT NOT NULL,
    duracion_min          SMALLINT NOT NULL,
    CONSTRAINT uq_receso_bloque UNIQUE (estructura_horaria_id, despues_de_bloque),
    CONSTRAINT ck_receso_bloque CHECK (despues_de_bloque >= 1),
    CONSTRAINT ck_receso_duracion CHECK (duracion_min BETWEEN 5 AND 60)
);

-- ---------------------------------------------------------------------
-- HU-23 · Periodo de matrícula regular de un año escolar (uno por año).
-- Una vez HABILITADO no se edita; solo se puede deshabilitar (Modal - Configuración · Matrícula).
-- ---------------------------------------------------------------------
CREATE TABLE matricula.periodo_matricula (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id           UUID        NOT NULL UNIQUE REFERENCES configuracion.anio_escolar (id),
    inicio                    TIMESTAMPTZ NOT NULL,
    cierre                    TIMESTAMPTZ NOT NULL,
    bloquear_por_morosidad    BOOLEAN     NOT NULL DEFAULT TRUE,
    permitir_extemporanea     BOOLEAN     NOT NULL DEFAULT FALSE,
    estado                    VARCHAR(16) NOT NULL DEFAULT 'EN_CONFIGURACION',
    creado_en                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_periodo_matricula_fechas CHECK (cierre > inicio),
    CONSTRAINT ck_periodo_matricula_estado CHECK (estado IN ('EN_CONFIGURACION', 'HABILITADO', 'CERRADO'))
);

-- ---------------------------------------------------------------------
-- HU-34 · Tarifario del año escolar (uno por año). La pensión es única para toda la
-- institución (RF-70). Política de morosidad: días de gracia, recargo y tope obligatorio (RF-72).
-- ---------------------------------------------------------------------
CREATE TABLE pagos.tarifario (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anio_escolar_id         UUID          NOT NULL UNIQUE REFERENCES configuracion.anio_escolar (id),
    monto_cuota_ingreso     NUMERIC(10, 2),
    monto_matricula         NUMERIC(10, 2) NOT NULL,
    vencimiento_matricula   DATE           NOT NULL,
    monto_pension           NUMERIC(10, 2) NOT NULL,
    cantidad_cuotas         SMALLINT       NOT NULL,
    dias_gracia             SMALLINT       NOT NULL,
    recargo_porcentaje      NUMERIC(5, 2)  NOT NULL,
    tope_recargo_porcentaje NUMERIC(5, 2)  NOT NULL,
    estado                  VARCHAR(16)    NOT NULL DEFAULT 'EN_CONFIGURACION',
    creado_en               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    actualizado_en          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_tarifario_montos CHECK (
        monto_matricula >= 0 AND monto_pension > 0 AND (monto_cuota_ingreso IS NULL OR monto_cuota_ingreso >= 0)),
    CONSTRAINT ck_tarifario_cuotas CHECK (cantidad_cuotas BETWEEN 1 AND 12),
    CONSTRAINT ck_tarifario_gracia CHECK (dias_gracia BETWEEN 0 AND 60),
    CONSTRAINT ck_tarifario_recargo CHECK (recargo_porcentaje BETWEEN 0 AND 100),
    CONSTRAINT ck_tarifario_tope CHECK (tope_recargo_porcentaje BETWEEN 0 AND 100),
    CONSTRAINT ck_tarifario_estado CHECK (estado IN ('EN_CONFIGURACION', 'CONFIRMADO'))
);

-- Calendario de cuotas de pensión. Todas usan tarifario.monto_pension.
-- RF-71: no puede haber dos cuotas con la misma fecha de vencimiento.
CREATE TABLE pagos.cuota_tarifario (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarifario_id      UUID        NOT NULL REFERENCES pagos.tarifario (id) ON DELETE CASCADE,
    numero            SMALLINT    NOT NULL,
    nombre            VARCHAR(40) NOT NULL,
    fecha_vencimiento DATE        NOT NULL,
    CONSTRAINT uq_cuota_tarifario_numero UNIQUE (tarifario_id, numero),
    CONSTRAINT uq_cuota_tarifario_vencimiento UNIQUE (tarifario_id, fecha_vencimiento),
    CONSTRAINT ck_cuota_tarifario_numero CHECK (numero BETWEEN 1 AND 12)
);
