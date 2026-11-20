-- =====================================================================
-- V22 · Historial de modificaciones de calificaciones (HU-60).
-- Cada cambio de una nota ya registrada deja una fila: valor anterior, valor nuevo, motivo, quién y
-- cuándo. Se guarda por ítem y matrícula (no por calificacion_item.id) para que el historial siga
-- existiendo aunque la nota se borre. Ambos valores nuevos en NULL = la nota se quitó.
-- =====================================================================

CREATE TABLE academico.modificacion_calificacion (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_evaluacion_id  UUID          NOT NULL REFERENCES academico.item_evaluacion (id) ON DELETE CASCADE,
    matricula_id        UUID          NOT NULL REFERENCES matricula.matricula (id),
    nota_anterior       NUMERIC(4, 2),
    nivel_anterior      VARCHAR(2),
    nota_nueva          NUMERIC(4, 2),
    nivel_nuevo         VARCHAR(2),
    motivo              VARCHAR(500)  NOT NULL,
    modificado_por      UUID          NOT NULL REFERENCES seguridad.usuario (id),
    modificado_en       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_modificacion_calificacion_anterior CHECK (
        (nota_anterior IS NOT NULL AND nivel_anterior IS NULL)
        OR (nota_anterior IS NULL AND nivel_anterior IS NOT NULL)),
    CONSTRAINT ck_modificacion_calificacion_nueva CHECK (nota_nueva IS NULL OR nivel_nuevo IS NULL),
    CONSTRAINT ck_modificacion_calificacion_nota CHECK (
        (nota_anterior IS NULL OR nota_anterior BETWEEN 0 AND 20)
        AND (nota_nueva IS NULL OR nota_nueva BETWEEN 0 AND 20)),
    CONSTRAINT ck_modificacion_calificacion_nivel CHECK (
        (nivel_anterior IS NULL OR nivel_anterior IN ('AD', 'A', 'B', 'C'))
        AND (nivel_nuevo IS NULL OR nivel_nuevo IN ('AD', 'A', 'B', 'C'))),
    CONSTRAINT ck_modificacion_calificacion_motivo CHECK (length(btrim(motivo)) > 0)
);

CREATE INDEX ix_modificacion_calificacion_item_matricula
    ON academico.modificacion_calificacion (item_evaluacion_id, matricula_id, modificado_en DESC);

COMMENT ON TABLE academico.modificacion_calificacion IS
    'Historial de cambios de calificaciones ya registradas (HU-60). Solo se agregan filas; no se editan.';
