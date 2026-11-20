-- HU-4: recuperación de contraseña con un código de 6 dígitos enviado al correo de la persona.
-- Solo vale el código más reciente de cada usuario; se guarda su hash (nunca el código).
CREATE TABLE seguridad.codigo_recuperacion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID        NOT NULL REFERENCES seguridad.usuario (id) ON DELETE CASCADE,
    codigo_hash VARCHAR(100) NOT NULL,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_en   TIMESTAMPTZ NOT NULL,
    intentos    SMALLINT    NOT NULL DEFAULT 0,
    usado_en    TIMESTAMPTZ,
    CONSTRAINT ck_codigo_recuperacion_intentos CHECK (intentos >= 0)
);

CREATE INDEX ix_codigo_recuperacion_usuario ON seguridad.codigo_recuperacion (usuario_id, creado_en DESC);
