-- =====================================================================
-- HU-41 · Pago en línea con Stripe.
-- El identificador de la sesión de Stripe Checkout (cs_test_…) pasa de 60 caracteres y su URL de pago puede pasar de
-- 500. Stripe documenta hasta 255 caracteres para los identificadores; la URL se guarda completa. El pago guarda la
-- misma referencia que la orden.
-- =====================================================================

ALTER TABLE pagos.orden_pago
    ALTER COLUMN referencia_pasarela TYPE VARCHAR(255),
    ALTER COLUMN url_pago TYPE VARCHAR(2048);

ALTER TABLE pagos.pago ALTER COLUMN referencia TYPE VARCHAR(255);
