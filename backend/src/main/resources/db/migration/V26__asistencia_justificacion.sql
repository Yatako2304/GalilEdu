-- Justificación de una falta o tardanza (HU-67, RF-145). Figma › Tutor › modales "Justificar inasistencia" y
-- "Justificar Tardanza": motivo obligatorio, detalle y un sustento adjunto opcional. El estado original (FALTA o
-- TARDANZA) se conserva para que el reporte (HU-68) cuente por tipo; esta fila la marca como justificada.
CREATE TABLE asistencia.justificacion (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asistencia_estudiante_id UUID         NOT NULL UNIQUE
        REFERENCES asistencia.asistencia_estudiante (id) ON DELETE CASCADE,
    motivo                   VARCHAR(20)  NOT NULL,
    detalle                  VARCHAR(500),
    archivo_nombre           VARCHAR(200),
    archivo_tipo             VARCHAR(100),
    archivo_tamano           INTEGER,
    archivo_contenido        BYTEA,
    registrado_por           UUID         NOT NULL REFERENCES seguridad.usuario (id),
    registrado_en            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_justificacion_motivo CHECK (motivo IN ('CITA_MEDICA', 'ENFERMEDAD', 'TRAMITE_OFICIAL', 'OTRO')),
    CONSTRAINT ck_justificacion_otro_detalle CHECK (motivo <> 'OTRO' OR length(trim(detalle)) > 0),
    CONSTRAINT ck_justificacion_archivo CHECK (
        (archivo_nombre IS NULL) = (archivo_contenido IS NULL)
        AND (archivo_nombre IS NULL) = (archivo_tipo IS NULL)
        AND (archivo_nombre IS NULL) = (archivo_tamano IS NULL)),
    CONSTRAINT ck_justificacion_archivo_tipo CHECK (archivo_tipo IS NULL
        OR archivo_tipo IN ('application/pdf', 'image/jpeg', 'image/png')),
    CONSTRAINT ck_justificacion_archivo_tamano CHECK (archivo_tamano IS NULL
        OR archivo_tamano BETWEEN 1 AND 5242880)
);
