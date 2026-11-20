-- HU-2 y HU-3: el administrador activa o desactiva el inicio de sesión con Google para toda la institución.
-- Una sola fila (id = 1). Usuario y contraseña siempre siguen funcionando.
-- V21 y V22 quedan libres para la rama de asistencia (feat/asistencia-registro), que debe renumerar sus V19 y V20.
CREATE TABLE seguridad.configuracion_acceso (
    id                SMALLINT    PRIMARY KEY DEFAULT 1,
    google_habilitado BOOLEAN     NOT NULL DEFAULT false,
    actualizado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_por   UUID        REFERENCES seguridad.usuario (id) ON DELETE SET NULL,
    CONSTRAINT ck_configuracion_acceso_unica CHECK (id = 1)
);

INSERT INTO seguridad.configuracion_acceso (id) VALUES (1);
