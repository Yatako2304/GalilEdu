-- =====================================================================
-- V3 · Documento que acredita al apoderado (HU-7, paso 3)
-- Figma > "Inscripción | Paso 3 - Variante sin padres (apoderado)":
-- "Documento que lo acredita" (tipo) + archivo adjunto (PDF o imagen).
-- Tabla aparte para no cargar el archivo cada vez que se lee el vínculo.
-- =====================================================================

CREATE TABLE estudiantes.documento_vinculo (
    vinculo_id     UUID PRIMARY KEY REFERENCES estudiantes.vinculo_familiar (id) ON DELETE CASCADE,
    tipo           VARCHAR(60)  NOT NULL,
    nombre_archivo VARCHAR(200) NOT NULL,
    tipo_mime      VARCHAR(100) NOT NULL,
    tamano_bytes   INTEGER      NOT NULL,
    contenido      BYTEA        NOT NULL,
    subido_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_documento_vinculo_mime CHECK (tipo_mime IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT ck_documento_vinculo_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 5242880)
);
