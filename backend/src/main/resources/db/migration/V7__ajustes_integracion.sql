-- =====================================================================
-- V7 · Ajustes de integración entre módulos (decisiones del 2026-10-19, ver docs/decisiones/)
--   1. El grado del estudiante apunta al catálogo configuracion.grado (antes: nivel + grado en texto).
--      El colegio tiene primaria y secundaria: ya no se inscribe en inicial.
--   2. El estudiante solo guarda si sigue en el colegio (ACTIVO / RETIRADO). Si está inscrito o
--      matriculado en un año lo dice su matrícula (matricula.matricula).
--   3. Cada estructura horaria indica su nivel; la principal del nivel es la que usan sus secciones.
--   4. El descuento aplicado es del estudiante, con un rango de vigencia que puede abarcar varios años;
--      cada cuota rebajada indica qué descuento la rebajó.
--   5. La sección se reserva al crear la matrícula (no puede quedar vacía) y el código es MAT-AAAA-NNNN.
-- El estudiante con cuenta propia se une por la persona (seguridad.usuario.persona_id =
-- estudiantes.estudiante.persona_id); no necesita columnas nuevas.
-- =====================================================================

-- 1. Grado del estudiante → catálogo de grados.
ALTER TABLE estudiantes.estudiante ADD COLUMN grado_codigo VARCHAR(10) REFERENCES configuracion.grado (codigo);

UPDATE estudiantes.estudiante
SET grado_codigo = CASE nivel WHEN 'PRIMARIA' THEN 'PRI-' WHEN 'SECUNDARIA' THEN 'SEC-' END
                   || substring(grado FROM '^([0-9])')
WHERE nivel IN ('PRIMARIA', 'SECUNDARIA');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM estudiantes.estudiante e
               LEFT JOIN configuracion.grado g ON g.codigo = e.grado_codigo
               WHERE g.codigo IS NULL) THEN
        RAISE EXCEPTION 'V7: hay estudiantes de inicial o con un grado no reconocido. Corrige su nivel y grado antes de aplicar esta migración.';
    END IF;
END $$;

ALTER TABLE estudiantes.estudiante ALTER COLUMN grado_codigo SET NOT NULL;
ALTER TABLE estudiantes.estudiante DROP CONSTRAINT ck_estudiante_nivel;
ALTER TABLE estudiantes.estudiante DROP COLUMN nivel;
ALTER TABLE estudiantes.estudiante DROP COLUMN grado;

-- 2. Estado del estudiante: ACTIVO o RETIRADO.
ALTER TABLE estudiantes.estudiante DROP CONSTRAINT ck_estudiante_estado;
UPDATE estudiantes.estudiante SET estado = CASE WHEN estado = 'INACTIVO' THEN 'RETIRADO' ELSE 'ACTIVO' END;
ALTER TABLE estudiantes.estudiante ALTER COLUMN estado SET DEFAULT 'ACTIVO';
ALTER TABLE estudiantes.estudiante ADD CONSTRAINT ck_estudiante_estado CHECK (estado IN ('ACTIVO', 'RETIRADO'));

-- 3. Estructura horaria por nivel.
ALTER TABLE configuracion.estructura_horaria ADD COLUMN nivel VARCHAR(20);
ALTER TABLE configuracion.estructura_horaria ADD COLUMN es_principal BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE configuracion.estructura_horaria SET nivel = CASE WHEN codigo ILIKE '%PRI%' THEN 'PRIMARIA' ELSE 'SECUNDARIA' END;
ALTER TABLE configuracion.estructura_horaria ALTER COLUMN nivel SET NOT NULL;
ALTER TABLE configuracion.estructura_horaria
    ADD CONSTRAINT ck_estructura_horaria_nivel CHECK (nivel IN ('PRIMARIA', 'SECUNDARIA'));
-- Una sola estructura principal por nivel y año (otras, como "talleres de tarde", no son principales).
CREATE UNIQUE INDEX ux_estructura_horaria_principal ON configuracion.estructura_horaria (anio_escolar_id, nivel)
    WHERE es_principal;

-- 4. Descuento aplicado al estudiante, con vigencia de uno o varios años.
ALTER TABLE pagos.descuento_aplicado ADD COLUMN estudiante_id UUID REFERENCES estudiantes.estudiante (id);
UPDATE pagos.descuento_aplicado d
SET estudiante_id = c.estudiante_id
FROM pagos.compromiso_pago c
WHERE c.id = d.compromiso_id;
ALTER TABLE pagos.descuento_aplicado ALTER COLUMN estudiante_id SET NOT NULL;
DROP INDEX pagos.ux_descuento_aplicado_activo;
ALTER TABLE pagos.descuento_aplicado DROP COLUMN compromiso_id;
-- RF-87: un solo descuento activo por estudiante; uno nuevo reemplaza al anterior (REEMPLAZADO).
CREATE UNIQUE INDEX ux_descuento_aplicado_activo ON pagos.descuento_aplicado (estudiante_id) WHERE estado = 'ACTIVO';

ALTER TABLE pagos.cuota ADD COLUMN descuento_aplicado_id UUID REFERENCES pagos.descuento_aplicado (id);
ALTER TABLE pagos.cuota
    ADD CONSTRAINT ck_cuota_descuento CHECK (monto_descuento = 0 OR descuento_aplicado_id IS NOT NULL);

-- 5. Matrícula: sección obligatoria y formato del código.
ALTER TABLE matricula.matricula ALTER COLUMN seccion_id SET NOT NULL;
ALTER TABLE matricula.matricula ADD CONSTRAINT ck_matricula_formato_codigo CHECK (codigo ~ '^MAT-[0-9]{4}-[0-9]{4,}$');
