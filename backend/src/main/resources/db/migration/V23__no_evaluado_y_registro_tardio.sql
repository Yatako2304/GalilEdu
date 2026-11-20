-- =====================================================================
-- V23 · «No evaluado» y registro fuera de plazo (HU-60, nota oficial pendiente hasta completar).
-- Una celda (ítem × matrícula) queda resuelta con una nota o marcada como «No evaluado» con motivo
-- (se matriculó después, licencia médica, exoneración…). «No evaluado» no es un 0: ese ítem no
-- cuenta para ese estudiante. Una celda sin fila es un registro faltante y deja la nota oficial
-- pendiente. El historial ahora admite un valor anterior vacío (nota registrada fuera de plazo) y
-- los cambios desde y hacia «No evaluado».
-- =====================================================================

ALTER TABLE academico.calificacion_item
    ADD COLUMN no_evaluado        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN motivo_no_evaluado VARCHAR(500),
    DROP CONSTRAINT ck_calificacion_item_un_valor,
    ADD CONSTRAINT ck_calificacion_item_un_valor CHECK (
        (NOT no_evaluado AND motivo_no_evaluado IS NULL
            AND ((nota_numerica IS NOT NULL AND nivel_logro IS NULL)
                 OR (nota_numerica IS NULL AND nivel_logro IS NOT NULL)))
        OR (no_evaluado AND nota_numerica IS NULL AND nivel_logro IS NULL
            AND length(btrim(coalesce(motivo_no_evaluado, ''))) > 0));

COMMENT ON COLUMN academico.calificacion_item.no_evaluado IS
    'La celda está resuelta sin nota: el ítem no cuenta para este estudiante. Requiere motivo.';

ALTER TABLE academico.modificacion_calificacion
    ADD COLUMN no_evaluado_anterior BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN no_evaluado_nuevo    BOOLEAN NOT NULL DEFAULT FALSE,
    DROP CONSTRAINT ck_modificacion_calificacion_anterior,
    ADD CONSTRAINT ck_modificacion_calificacion_anterior CHECK (nota_anterior IS NULL OR nivel_anterior IS NULL),
    ADD CONSTRAINT ck_modificacion_calificacion_no_evaluado CHECK (
        (NOT no_evaluado_anterior OR (nota_anterior IS NULL AND nivel_anterior IS NULL))
        AND (NOT no_evaluado_nuevo OR (nota_nueva IS NULL AND nivel_nuevo IS NULL)));

COMMENT ON TABLE academico.modificacion_calificacion IS
    'Historial de cambios de calificaciones (HU-60). Anterior vacío y sin «No evaluado» = registrada fuera de plazo. '
    'Nuevo vacío y sin «No evaluado» = la nota se quitó. Solo se agregan filas.';
