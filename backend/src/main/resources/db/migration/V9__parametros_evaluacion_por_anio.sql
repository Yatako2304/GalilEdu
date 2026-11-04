-- Los parámetros de calificación pertenecen al año escolar y rigen para todos sus cursos.
-- Las columnas históricas de plan_competencia se conservan para no perder información,
-- pero desde V9 dejan de ser la fuente efectiva y reciben valores técnicos por defecto.
ALTER TABLE configuracion.anio_escolar
    ADD COLUMN escala_evaluacion VARCHAR(12) NOT NULL DEFAULT 'VIGESIMAL',
    ADD COLUMN regla_nota VARCHAR(20) NOT NULL DEFAULT 'PROMEDIO_PONDERADO',
    ADD COLUMN consolidacion_periodos VARCHAR(20) NOT NULL DEFAULT 'PROMEDIO_PERIODOS',
    ADD CONSTRAINT ck_anio_escala_evaluacion
        CHECK (escala_evaluacion IN ('VIGESIMAL', 'CUALITATIVA')),
    ADD CONSTRAINT ck_anio_regla_nota
        CHECK (
            (escala_evaluacion = 'VIGESIMAL'
                AND regla_nota IN ('PROMEDIO_SIMPLE', 'PROMEDIO_PONDERADO'))
            OR (escala_evaluacion = 'CUALITATIVA'
                AND regla_nota IN ('NIVEL_MAS_FRECUENTE', 'ULTIMO_NIVEL'))
        ),
    ADD CONSTRAINT ck_anio_consolidacion_periodos
        CHECK (consolidacion_periodos IN ('PROMEDIO_PERIODOS', 'ULTIMO_PERIODO'));

ALTER TABLE academico.plan_competencia
    ALTER COLUMN escala SET DEFAULT 'VIGESIMAL',
    ALTER COLUMN regla_nota SET DEFAULT 'PROMEDIO_SIMPLE',
    ALTER COLUMN consolidacion SET DEFAULT 'PROMEDIO_PERIODOS';

COMMENT ON COLUMN academico.plan_competencia.escala IS
    'Legado anterior a V9. La escala efectiva está en configuracion.anio_escolar.';
COMMENT ON COLUMN academico.plan_competencia.regla_nota IS
    'Legado anterior a V9. La regla efectiva está en configuracion.anio_escolar.';
COMMENT ON COLUMN academico.plan_competencia.consolidacion IS
    'Legado anterior a V9. La consolidación efectiva está en configuracion.anio_escolar.';
