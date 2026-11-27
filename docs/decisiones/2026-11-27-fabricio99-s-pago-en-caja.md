# Pago en caja sin migración

- **Autor:** fabricio99-s
- **Fecha:** 2026-11-27
- **HU / tarea:** fila 29 del plan de pruebas («Registrar pago de cuota»)
- **Estado:** terminado

## Qué hice

El Personal administrativo registra lo que el apoderado pagó en el colegio con el botón «Registrar pago» del cronograma del compromiso (P8). Elige las cuotas, la fecha, el medio de pago y el número de operación, y se emite la misma constancia que en el pago en línea. Las reglas de cobro que ya tenía HU-41 pasaron a `CobroCuotas` para que las usen los dos pagos.

## Archivos principales

- `backend/src/main/java/com/galiledu/pagos/aplicacion/ServicioPagoEnCaja.java` y `CobroCuotas.java`
- `backend/src/main/java/com/galiledu/pagos/web/PagoEnCajaController.java`
- `backend/src/main/java/com/galiledu/pagos/aplicacion/PagoRegistrado.java` (antes `PagoEnLineaAprobado`)
- `frontend/src/features/pagos/components/dialogo-registrar-pago.tsx`
- `frontend/src/app/routes/app/administrativo/compromiso-pago.tsx`

## Decisiones y por qué

- **Decisión:** no se crea una migración; el pago en caja usa `pagos.pago` tal como está. Se reconoce por no tener orden y sí `registrado_por`; el medio y el número de operación van como texto en `referencia` ("Transferencia · 00123456").
  **Por qué:** el equipo prefirió no tocar la BD y la tabla ya tenía lo necesario (`orden_pago_id` opcional y `registrado_por`).
  **Alternativas descartadas:** columnas `medio_pago` y `numero_operacion` con su CHECK. Es lo más limpio si después se piden reportes por medio de pago; habría que migrar y separar el texto de `referencia`.
- **Decisión:** mismas reglas que el pago en línea: cuotas completas, pensiones en orden, la matrícula sin pensiones vencidas fuera del pago y una cuota con pago en línea en proceso no se registra.
  **Por qué:** que una cuota se pague igual por cualquier canal y no duplicar reglas.
- **Decisión:** la fecha de pago puede ser anterior (nunca futura) y el recargo por mora se calcula a esa fecha.
  **Por qué:** el personal a veces registra días después lo que se pagó antes; el apoderado no debe pagar mora por la demora del registro.
- **Decisión:** el evento `PagoEnLineaAprobado` pasa a llamarse `PagoRegistrado` y lo publican los dos pagos.
  **Por qué:** Matrícula (HU-31) debe matricular igual si la cuota de matrícula se paga en caja.
- **Decisión:** el botón va a la derecha de las pestañas del cronograma; el modal copia el estilo de «Añadir descuento».
  **Por qué:** no hay frame en Figma; el lugar lo pidió el equipo.

## Pendientes / problemas conocidos

- [ ] Anular un pago en caja mal registrado.
- [ ] Pagos parciales de una cuota (el estado PAGADA_PARCIAL existe, pero no se usa desde caja).
- [ ] El personal no descarga la constancia en PDF desde P9; el apoderado sí desde su historial.

## Notas para quien siga

- Para saber si un pago fue en caja: `orden_pago_id IS NULL AND registrado_por IS NOT NULL`. Los pagos de los datos de prueba no tienen orden ni `registrado_por` y se muestran como "En línea".
- La observación del personal se guarda en `observacion` y sale en la constancia como «Observación», aunque el pago queda CONFIRMADO.
