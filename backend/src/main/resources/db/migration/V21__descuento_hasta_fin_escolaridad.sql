-- HU-38 (corrección del PO): un descuento aplicado puede regir "hasta que el estudiante termine el colegio", sin fecha
-- de fin. Sigue rebajando las cuotas de los años siguientes mientras esté ACTIVO.
ALTER TABLE pagos.descuento_aplicado ADD COLUMN hasta_fin_escolaridad BOOLEAN NOT NULL DEFAULT FALSE;

-- Los ya aplicados sin fecha de fin ya regían sin límite: quedan marcados como tales.
UPDATE pagos.descuento_aplicado SET hasta_fin_escolaridad = TRUE WHERE vigencia_hasta IS NULL;

ALTER TABLE pagos.descuento_aplicado ADD CONSTRAINT ck_descuento_aplicado_fin_escolaridad
    CHECK (NOT hasta_fin_escolaridad OR vigencia_hasta IS NULL);
