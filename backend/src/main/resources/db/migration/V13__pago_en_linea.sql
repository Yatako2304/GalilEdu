-- =====================================================================
-- HU-41 · Pago en línea de cuotas pendientes (pasarela de pagos).
-- Completa pagos.orden_pago (V5) con lo que necesita el flujo de la pasarela.
-- =====================================================================

-- Código legible de la orden: OP-2026-000001.
CREATE SEQUENCE pagos.orden_pago_codigo_seq;

-- La orden expira si la pasarela no la confirma a tiempo (30 min) y sus cuotas vuelven a su estado anterior.
-- url_pago: página de la pasarela a la que se redirige al apoderado.
ALTER TABLE pagos.orden_pago
    ADD COLUMN expira_en TIMESTAMPTZ,
    ADD COLUMN url_pago  VARCHAR(500);

UPDATE pagos.orden_pago SET expira_en = creado_en + INTERVAL '30 minutes' WHERE expira_en IS NULL;
ALTER TABLE pagos.orden_pago ALTER COLUMN expira_en SET NOT NULL;

CREATE INDEX ix_orden_pago_pendiente ON pagos.orden_pago (expira_en) WHERE estado = 'EN_PROCESO';

-- Estado de la cuota antes de entrar a la orden (PENDIENTE o PAGADA_PARCIAL): se restaura si la orden se rechaza o
-- expira (sección 3.8: "las cuotas involucradas retornan a su estado anterior").
ALTER TABLE pagos.orden_pago_cuota ADD COLUMN estado_anterior VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE pagos.orden_pago_cuota ALTER COLUMN estado_anterior DROP DEFAULT;
ALTER TABLE pagos.orden_pago_cuota
    ADD CONSTRAINT ck_orden_pago_cuota_estado_anterior CHECK (estado_anterior IN ('PENDIENTE', 'PAGADA_PARCIAL'));

-- Una orden puede pagar cuotas de varios hijos, y cada pago (con su constancia) es de un solo estudiante: la orden
-- genera un pago por estudiante. Que no se registre dos veces lo garantiza el estado de la orden (RF-103).
ALTER TABLE pagos.pago DROP CONSTRAINT pago_orden_pago_id_key;
CREATE INDEX ix_pago_orden ON pagos.pago (orden_pago_id);
