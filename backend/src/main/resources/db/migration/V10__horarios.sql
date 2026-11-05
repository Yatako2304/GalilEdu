-- =====================================================================
-- V10 · Horarios (HU-45): asignación de cursos a los bloques del horario semanal de una sección
-- Un esquema por módulo: horarios (horario_seccion y horario_bloque).
-- Un "bloque" no es una fila de catálogo: es el par (día, número de bloque) dentro de la jornada
-- de la estructura horaria, que se calcula en código. Una fila de horario_bloque dice:
-- "en este día y bloque, la sección dicta este curso del plan en este ambiente".
-- Las reglas que la BD no puede garantizar (el plan_curso es del año y grado de la sección, el día
-- pertenece a dias_lectivos, el bloque cae dentro de la jornada, horas semanales, cruces de docente
-- y de ambiente) las valida el servicio; no se modelan con triggers.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS horarios;

-- ---------------------------------------------------------------------
-- Cabecera: como máximo un horario semanal por sección. La crea el servicio la primera vez
-- que se guarda el horario (no al crear la sección).
-- ---------------------------------------------------------------------
CREATE TABLE horarios.horario_seccion (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- El horario nace y muere con la sección: al eliminarla se borran su horario y sus bloques.
    seccion_id            UUID   NOT NULL REFERENCES configuracion.seccion (id) ON DELETE CASCADE,
    -- RESTRICT: no se puede eliminar una estructura horaria que ya tiene horarios.
    estructura_horaria_id UUID   NOT NULL REFERENCES configuracion.estructura_horaria (id) ON DELETE RESTRICT,
    -- Bloqueo optimista: el cliente envía la versión que leyó; si no coincide con la actual,
    -- el guardado se rechaza con 409. Se incrementa en cada guardado (lógica del servicio).
    version               BIGINT NOT NULL DEFAULT 0,
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_horario_seccion_seccion UNIQUE (seccion_id)
);

-- ---------------------------------------------------------------------
-- Una fila por bloque asignado.
-- No hay docente_id: el docente es el de academico.docente_seccion (plan_curso_id, seccion_id).
-- No hay grupo: los subgrupos (HU-47, iteración 3) traerán su propia migración, que agregará
-- la tabla grupo y horario_bloque.grupo_id y docente_id (este último rellenado desde docente_seccion).
-- ---------------------------------------------------------------------
CREATE TABLE horarios.horario_bloque (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    horario_seccion_id  UUID        NOT NULL REFERENCES horarios.horario_seccion (id) ON DELETE CASCADE,
    dia                 VARCHAR(3)  NOT NULL,
    numero_bloque       SMALLINT    NOT NULL,
    plan_curso_id       UUID        NOT NULL REFERENCES academico.plan_curso (id) ON DELETE RESTRICT,
    -- Obligatorio: se guarda siempre (por defecto, el aula de la sección).
    ambiente_id         UUID        NOT NULL REFERENCES configuracion.ambiente (id) ON DELETE RESTRICT,
    -- Una sección no puede tener dos cursos en el mismo bloque.
    CONSTRAINT uq_horario_bloque_posicion UNIQUE (horario_seccion_id, dia, numero_bloque),
    -- Mismos códigos que configuracion.estructura_horaria.dias_lectivos.
    CONSTRAINT ck_horario_bloque_dia CHECK (dia IN ('LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB')),
    CONSTRAINT ck_horario_bloque_numero CHECK (numero_bloque >= 1)
);

-- Consultas de cruces entre secciones.
CREATE INDEX ix_horario_bloque_ambiente_dia ON horarios.horario_bloque (ambiente_id, dia);
CREATE INDEX ix_horario_bloque_plan_curso ON horarios.horario_bloque (plan_curso_id);

-- Consulta de qué horarios usan una estructura (HU-44).
CREATE INDEX ix_horario_seccion_estructura ON horarios.horario_seccion (estructura_horaria_id);
