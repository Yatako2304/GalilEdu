-- El estado del periodo (PLANIFICADO, EN_CURSO, CERRADO) ya no se guarda: se calcula con sus fechas y el estado del año
-- (PeriodoAcademico.estadoEn), porque nada lo actualizaría a tiempo. Al borrar la columna se borra también
-- ck_periodo_academico_estado. Ver docs/decisiones/2026-10-21-royal-22-estado-periodo-calculado.md.
ALTER TABLE configuracion.periodo_academico DROP COLUMN estado;
