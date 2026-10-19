-- =====================================================================
-- V1 · Personas y seguridad (HU-1, HU-6, HU-9, HU-12)
-- Campos tomados de Figma > Administrador > "Nuevo usuario" / "Editar usuario".
-- Convención: un esquema por módulo, nombres en español y snake_case.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS personas;
CREATE SCHEMA IF NOT EXISTS seguridad;

-- ---------------------------------------------------------------------
-- personas.persona: cualquier persona del colegio (usuario, padre, estudiante…).
-- Una persona puede no tener cuenta (ej. estudiante o madre sin usuario).
-- ---------------------------------------------------------------------
CREATE TABLE personas.persona (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_documento    VARCHAR(20)  NOT NULL,
    numero_documento  VARCHAR(12)  NOT NULL,
    apellido_paterno  VARCHAR(80)  NOT NULL,
    apellido_materno  VARCHAR(80),
    nombres           VARCHAR(120) NOT NULL,
    fecha_nacimiento  DATE,
    nacionalidad      VARCHAR(60),
    lugar_nacimiento  VARCHAR(120),
    sexo              VARCHAR(10),
    estado_civil      VARCHAR(20),
    grado_instruccion VARCHAR(40),
    celular           VARCHAR(9),
    correo            VARCHAR(254),
    telefono_casa     VARCHAR(15),
    profesion         VARCHAR(120),
    centro_trabajo    VARCHAR(120),
    cargo             VARCHAR(120),
    distrito          VARCHAR(80),
    direccion         VARCHAR(200),
    es_exalumno       BOOLEAN      NOT NULL DEFAULT FALSE,
    creado_en         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_persona_tipo_documento CHECK (tipo_documento IN ('DNI', 'CARNET_EXTRANJERIA')),
    CONSTRAINT ck_persona_dni CHECK (tipo_documento <> 'DNI' OR numero_documento ~ '^[0-9]{8}$'),
    CONSTRAINT ck_persona_ce CHECK (tipo_documento <> 'CARNET_EXTRANJERIA' OR numero_documento ~ '^[A-Za-z0-9]{1,12}$'),
    CONSTRAINT ck_persona_sexo CHECK (sexo IS NULL OR sexo IN ('MASCULINO', 'FEMENINO')),
    CONSTRAINT ck_persona_celular CHECK (celular IS NULL OR celular ~ '^[0-9]{9}$')
);

-- RF-14 / RF-23: no se repite el documento ni el correo.
CREATE UNIQUE INDEX ux_persona_documento ON personas.persona (numero_documento);
CREATE UNIQUE INDEX ux_persona_correo ON personas.persona (lower(correo)) WHERE correo IS NOT NULL;

-- ---------------------------------------------------------------------
-- seguridad.rol: catálogo fijo de roles (RF-13).
-- ---------------------------------------------------------------------
CREATE TABLE seguridad.rol (
    codigo  VARCHAR(30) PRIMARY KEY,
    nombre  VARCHAR(60) NOT NULL,
    orden   SMALLINT    NOT NULL
);

INSERT INTO seguridad.rol (codigo, nombre, orden) VALUES
    ('ADMINISTRADOR',           'Administrador',           1),
    ('PERSONAL_ADMINISTRATIVO', 'Personal administrativo', 2),
    ('COORDINADOR',             'Coordinador',             3),
    ('APODERADO',               'Apoderado',               4),
    ('DOCENTE',                 'Docente',                 5),
    ('TUTOR',                   'Tutor',                   6),
    ('ESTUDIANTE',              'Estudiante',              7);

-- ---------------------------------------------------------------------
-- seguridad.usuario: cuenta de acceso de una persona.
-- nombre_usuario = U{año}{correlativo de 4 dígitos} (RF-15).
-- eliminado = borrado lógico ("Eliminar" en Figma); no se muestra en la lista.
-- ---------------------------------------------------------------------
CREATE TABLE seguridad.usuario (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    persona_id        UUID        NOT NULL UNIQUE REFERENCES personas.persona (id),
    nombre_usuario    VARCHAR(9)  NOT NULL UNIQUE,
    contrasena_hash   VARCHAR(100) NOT NULL,
    estado            VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    eliminado         BOOLEAN     NOT NULL DEFAULT FALSE,
    intentos_fallidos SMALLINT    NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMPTZ,
    ultimo_acceso     TIMESTAMPTZ,
    creado_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_usuario_nombre CHECK (nombre_usuario ~ '^U[0-9]{8}$')
);

-- Un usuario puede tener varios roles (ej. Docente + Tutor).
CREATE TABLE seguridad.usuario_rol (
    usuario_id UUID        NOT NULL REFERENCES seguridad.usuario (id) ON DELETE CASCADE,
    rol_codigo VARCHAR(30) NOT NULL REFERENCES seguridad.rol (codigo),
    PRIMARY KEY (usuario_id, rol_codigo)
);

-- Correlativo anual para generar nombres de usuario sin duplicados.
CREATE TABLE seguridad.correlativo_usuario (
    anio   SMALLINT PRIMARY KEY,
    ultimo INTEGER  NOT NULL
);
