-- HU-2 y HU-3: el administrador activa o desactiva el inicio de sesión con Google para toda la institución.
-- Una sola fila (id = 1). Usuario y contraseña siempre siguen funcionando.
-- Es V24 porque main ya ocupa hasta V23 (V21 a V23 son de pagos y calificaciones).
CREATE TABLE seguridad.configuracion_acceso (
    id                SMALLINT    PRIMARY KEY DEFAULT 1,
    google_habilitado BOOLEAN     NOT NULL DEFAULT false,
    actualizado_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_por   UUID        REFERENCES seguridad.usuario (id) ON DELETE SET NULL,
    CONSTRAINT ck_configuracion_acceso_unica CHECK (id = 1)
);

INSERT INTO seguridad.configuracion_acceso (id) VALUES (1);
