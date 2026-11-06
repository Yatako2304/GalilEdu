-- =====================================================================
-- V13 · Peso de cada curso en la nota de una competencia (HU-56).
-- Una competencia puede evaluarse en varios cursos del mismo grado y año;
-- peso_curso dice cuánto aporta cada uno. NULL significa que falta definirlo.
-- La suma de hasta 100 % entre cursos cruza filas y la valida el servicio.
-- =====================================================================

ALTER TABLE academico.plan_competencia
    ADD COLUMN peso_curso NUMERIC(5, 2),
    ADD CONSTRAINT ck_plan_competencia_peso_curso
        CHECK (peso_curso IS NULL OR (peso_curso > 0 AND peso_curso <= 100));

-- Los planes existentes reparten el 100 % en partes iguales. El primer curso recibe
-- el resto del redondeo para que la suma sea exacta (33,34 + 33,33 + 33,33).
WITH reparto AS (
    SELECT pco.plan_curso_id,
           pco.competencia_id,
           row_number() OVER grupo AS fila,
           count(*) OVER (PARTITION BY pc.anio_escolar_id, pc.grado_codigo, pco.competencia_id) AS cursos
    FROM academico.plan_competencia pco
    JOIN academico.plan_curso pc ON pc.id = pco.plan_curso_id
    WINDOW grupo AS (PARTITION BY pc.anio_escolar_id, pc.grado_codigo, pco.competencia_id ORDER BY pc.curso_id)
)
UPDATE academico.plan_competencia pco
SET peso_curso = CASE
        WHEN r.fila = 1 THEN 100 - trunc(100.0 / r.cursos, 2) * (r.cursos - 1)
        ELSE trunc(100.0 / r.cursos, 2)
    END
FROM reparto r
WHERE pco.plan_curso_id = r.plan_curso_id
  AND pco.competencia_id = r.competencia_id;
