-- =====================================================================
-- V2 · Estudiantes e inscripción (HU-7)
-- Figma > "Admin / Estudiantes | Inscripción" (pasos 1 a 5).
-- El estudiante NO tiene usuario: lo gestiona su responsable de matrícula,
-- que sí tiene cuenta (rol APODERADO, un usuario por familia).
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS estudiantes;

-- Paso 1 · Datos del estudiante (los datos personales básicos están en personas.persona).
CREATE TABLE estudiantes.estudiante (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id                UUID        NOT NULL UNIQUE REFERENCES personas.persona (id),
    pais_nacimiento           VARCHAR(60),
    departamento_nacimiento   VARCHAR(60),
    provincia_nacimiento      VARCHAR(60),
    distrito_nacimiento       VARCHAR(60),
    referencia_domicilio      VARCHAR(200),
    tiene_internet            BOOLEAN,
    lengua_materna            VARCHAR(40),
    segunda_lengua            VARCHAR(40),
    autoidentificacion_etnica VARCHAR(60),
    tiene_discapacidad        BOOLEAN     NOT NULL DEFAULT FALSE,
    discapacidad_detalle      VARCHAR(200),
    colegio_procedencia       VARCHAR(150),
    gestion_procedencia       VARCHAR(20),
    anio_academico            SMALLINT    NOT NULL,
    nivel                     VARCHAR(20) NOT NULL,
    grado                     VARCHAR(30) NOT NULL,
    forma_ingreso             VARCHAR(40),
    modo_salida               VARCHAR(20),
    estado                    VARCHAR(15) NOT NULL DEFAULT 'INSCRITO',
    eliminado                 BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_en                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_estudiante_estado CHECK (estado IN ('INSCRITO', 'MATRICULADO', 'INACTIVO')),
    CONSTRAINT ck_estudiante_nivel CHECK (nivel IN ('INICIAL', 'PRIMARIA', 'SECUNDARIA')),
    CONSTRAINT ck_estudiante_salida CHECK (modo_salida IS NULL OR modo_salida IN ('PERSONA_AUTORIZADA', 'SOLO', 'MOVILIDAD'))
);

-- Paso 2 · Ficha médica (1 a 1 con el estudiante).
CREATE TABLE estudiantes.ficha_medica (
    estudiante_id           UUID PRIMARY KEY REFERENCES estudiantes.estudiante (id) ON DELETE CASCADE,
    tipo_sangre             VARCHAR(5),
    peso_kg                 NUMERIC(5, 2),
    estatura_m              NUMERIC(3, 2),
    tipo_seguro             VARCHAR(40),
    tiene_alergias          BOOLEAN NOT NULL DEFAULT FALSE,
    alergias_alimentarias   VARCHAR(200),
    alergias_medicamentosas VARCHAR(200),
    fiebre_maxima           VARCHAR(20),
    ha_convulsionado        BOOLEAN NOT NULL DEFAULT FALSE,
    tiene_enfermedad        BOOLEAN NOT NULL DEFAULT FALSE,
    enfermedad_detalle      VARCHAR(200),
    toma_medicamento        BOOLEAN NOT NULL DEFAULT FALSE,
    medicamento_detalle     VARCHAR(200),
    observaciones           VARCHAR(500)
);

-- Paso 3 · Padres y apoderado. Una persona puede estar vinculada a varios hermanos.
-- Si el padre/madre "no registra / fallecido", persona_id es NULL y se guarda el motivo.
CREATE TABLE estudiantes.vinculo_familiar (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id            UUID        NOT NULL REFERENCES estudiantes.estudiante (id) ON DELETE CASCADE,
    persona_id               UUID        REFERENCES personas.persona (id),
    parentesco               VARCHAR(10) NOT NULL,
    relacion                 VARCHAR(60),
    vive_con_estudiante      BOOLEAN     NOT NULL DEFAULT FALSE,
    tiene_patria_potestad    BOOLEAN     NOT NULL DEFAULT FALSE,
    es_responsable_matricula BOOLEAN     NOT NULL DEFAULT FALSE,
    motivo_no_registra       VARCHAR(60),
    observacion              VARCHAR(300),
    CONSTRAINT ck_vinculo_parentesco CHECK (parentesco IN ('PADRE', 'MADRE', 'APODERADO')),
    CONSTRAINT ck_vinculo_persona CHECK (persona_id IS NOT NULL OR motivo_no_registra IS NOT NULL)
);

CREATE UNIQUE INDEX ux_vinculo_padre_madre ON estudiantes.vinculo_familiar (estudiante_id, parentesco)
    WHERE parentesco IN ('PADRE', 'MADRE');
CREATE UNIQUE INDEX ux_vinculo_responsable ON estudiantes.vinculo_familiar (estudiante_id)
    WHERE es_responsable_matricula;
CREATE INDEX ix_vinculo_persona ON estudiantes.vinculo_familiar (persona_id);

-- Paso 4 · Contactos de emergencia (mínimo 2).
CREATE TABLE estudiantes.contacto_emergencia (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id   UUID         NOT NULL REFERENCES estudiantes.estudiante (id) ON DELETE CASCADE,
    nombre_completo VARCHAR(150) NOT NULL,
    parentesco      VARCHAR(60)  NOT NULL,
    telefono_fijo   VARCHAR(15),
    celular         VARCHAR(9),
    orden           SMALLINT     NOT NULL
);

-- Paso 4 · Personas autorizadas a recoger al estudiante.
CREATE TABLE estudiantes.autorizado_recojo (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id   UUID         NOT NULL REFERENCES estudiantes.estudiante (id) ON DELETE CASCADE,
    nombre_completo VARCHAR(150) NOT NULL,
    relacion        VARCHAR(60)  NOT NULL
);
