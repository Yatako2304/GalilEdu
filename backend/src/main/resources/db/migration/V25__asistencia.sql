-- Asistencia diaria de estudiantes (HU-65, RF-143). Figma › Tutor › "HU65 · Marcar asistencia de la sección tutorada"
-- (AS-01 a AS-06). El tutor o cotutor de la sección registra una vez al día el estado de cada estudiante matriculado y
-- solo puede corregirlo ese mismo día; cada corrección queda en el historial (quién, cuándo, antes y después).
CREATE SCHEMA IF NOT EXISTS asistencia;

-- Una toma de asistencia por sección y día. "Registrada hoy a las 08:05 · Selene Curo" sale de registrado_*.
CREATE TABLE asistencia.registro_diario (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seccion_id      UUID        NOT NULL REFERENCES configuracion.seccion (id),
    fecha           DATE        NOT NULL,
    registrado_por  UUID        NOT NULL REFERENCES seguridad.usuario (id),
    registrado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    corregido_por   UUID REFERENCES seguridad.usuario (id),
    corregido_en    TIMESTAMPTZ,
    version         INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT uq_registro_diario_seccion_fecha UNIQUE (seccion_id, fecha),
    CONSTRAINT ck_registro_diario_correccion CHECK ((corregido_por IS NULL) = (corregido_en IS NULL))
);

-- Estado de cada estudiante en ese día. La hora de llegada es obligatoria en la tardanza y no va en los demás estados.
CREATE TABLE asistencia.asistencia_estudiante (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registro_id   UUID        NOT NULL REFERENCES asistencia.registro_diario (id) ON DELETE CASCADE,
    matricula_id  UUID        NOT NULL REFERENCES matricula.matricula (id),
    estado        VARCHAR(10) NOT NULL,
    hora_llegada  TIME,
    CONSTRAINT uq_asistencia_estudiante_registro_matricula UNIQUE (registro_id, matricula_id),
    CONSTRAINT ck_asistencia_estudiante_estado CHECK (estado IN ('PRESENTE', 'TARDANZA', 'FALTA')),
    CONSTRAINT ck_asistencia_estudiante_hora CHECK ((estado = 'TARDANZA') = (hora_llegada IS NOT NULL))
);

CREATE INDEX ix_asistencia_estudiante_matricula ON asistencia.asistencia_estudiante (matricula_id);

-- Historial de correcciones del mismo día (AS-06: "Los cambios quedarán en el historial con la hora").
CREATE TABLE asistencia.correccion (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asistencia_estudiante_id UUID        NOT NULL REFERENCES asistencia.asistencia_estudiante (id) ON DELETE CASCADE,
    estado_anterior          VARCHAR(10) NOT NULL,
    hora_anterior            TIME,
    estado_nuevo             VARCHAR(10) NOT NULL,
    hora_nueva               TIME,
    corregido_por            UUID        NOT NULL REFERENCES seguridad.usuario (id),
    corregido_en             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_correccion_estados CHECK (estado_anterior IN ('PRESENTE', 'TARDANZA', 'FALTA')
        AND estado_nuevo IN ('PRESENTE', 'TARDANZA', 'FALTA')),
    CONSTRAINT ck_correccion_cambio CHECK (estado_anterior <> estado_nuevo
        OR hora_anterior IS DISTINCT FROM hora_nueva)
);

CREATE INDEX ix_correccion_asistencia ON asistencia.correccion (asistencia_estudiante_id);
