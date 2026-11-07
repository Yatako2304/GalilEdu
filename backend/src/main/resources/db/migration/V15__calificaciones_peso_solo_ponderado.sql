-- Los ítems de años con promedio simple o reglas cualitativas no tienen peso.
-- Se conservan los porcentajes existentes; la aplicación solo los usa con PROMEDIO_PONDERADO.
ALTER TABLE academico.item_competencia
    ALTER COLUMN peso_porcentaje DROP NOT NULL;

COMMENT ON COLUMN academico.item_competencia.peso_porcentaje IS
    'Porcentaje obligatorio solo con PROMEDIO_PONDERADO. NULL para reglas sin ponderación; si existe, debe ser mayor que 0 y menor o igual que 100.';
