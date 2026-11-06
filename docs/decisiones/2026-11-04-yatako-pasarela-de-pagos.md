# Pasarela de pagos: Stripe Checkout con una pasarela simulada

- **Autor:** Yatako2304
- **Fecha:** 2026-11-04
- **HU / tarea:** HU-41 (pago en línea de cuotas pendientes)
- **Estado:** terminado

## Qué hice

El apoderado marca cuotas de sus hijos, el sistema arma la orden (capital + recargo por mora), pone las cuotas EN_PROCESO y lo
redirige a la página de pago de la pasarela. La confirmación llega por notificación (webhook) o por consulta directa; se
registra un pago con constancia por estudiante. Si la pasarela rechaza, el apoderado cancela o pasan 30 minutos, las cuotas
vuelven a su estado anterior.

## Archivos principales

- `backend/src/main/java/com/galiledu/pagos/aplicacion/ServicioPagoEnLinea.java` y `PasarelaPagos.java`
- `backend/src/main/java/com/galiledu/pagos/infraestructura/PasarelaStripe.java`, `PasarelaSimulada.java`, `FirmaStripe.java`
- `backend/src/main/resources/db/migration/V13__pago_en_linea.sql`
- `frontend/src/features/pagos/components/seleccion-cuotas.ts`, `barra-pago-cuotas.tsx`, `resultado-pago.tsx`

## Decisiones y por qué

- **Decisión:** Stripe Checkout (página de pago alojada por Stripe, en soles) en modo de prueba.
  **Por qué:** es la integración más simple y mejor documentada, tiene modo de prueba sin empresa constituida y la tarjeta nunca
  pasa por nuestro servidor (sección 3.8: "el sistema no almacena datos de tarjetas").
  **Alternativas descartadas:** Culqi y Mercado Pago (opciones peruanas; se pueden sumar como otro adaptador), Stripe Elements
  (formulario dentro de la app: más trabajo y más superficie de seguridad). Ojo: Stripe no acepta comercios constituidos en
  Perú; para cobrar de verdad habría que cambiar de proveedor.
- **Decisión:** la pasarela es un puerto (`PasarelaPagos`) con dos adaptadores: `stripe` y `simulada` (por defecto,
  `PASARELA_PAGOS`).
  **Por qué:** se puede desarrollar, probar y demostrar sin cuenta ni llaves; cambiar de proveedor solo toca el adaptador.
- **Decisión:** Stripe se llama con su API REST (`RestClient`), sin agregar la librería `stripe-java`; la firma del webhook se
  verifica a mano (`FirmaStripe`, HMAC-SHA256 con tolerancia de 5 min).
  **Por qué:** solo usamos tres llamadas (crear, consultar y expirar la sesión) y así no se agrega una dependencia.
- **Decisión:** la notificación no se cree: al recibirla se vuelve a consultar la sesión en Stripe y la orden se bloquea
  (`SELECT … FOR UPDATE`) mientras se resuelve.
  **Por qué:** una notificación repetida o falsificada no puede registrar un pago dos veces (RF-103).
- **Decisión:** las pensiones se pagan en orden de antigüedad (confirmado con el equipo; el PO dijo que "usualmente vas pagando
  el más antiguo"). La matrícula y otros conceptos se pueden pagar sin ese orden.
- **Decisión:** el recargo es la tasa por cada 30 días de atraso desde el vencimiento (pasada la gracia), sobre el capital
  pendiente y con tope; no se capitaliza.
- **Decisión:** pueden pagar el padre, la madre o el apoderado vinculados que tengan cuenta (acta del PO, 30/10).
- **Decisión:** una orden genera un pago por estudiante, porque cada pago y su constancia son de un solo estudiante. V13 quita
  el UNIQUE de `pago.orden_pago_id`; que no se duplique lo garantiza el estado de la orden.

## Pendientes / problemas conocidos

- [ ] Crear la cuenta de Stripe (modo de prueba) y poner las llaves en `backend/.env` y en `deploy/.env` del servidor.
- [ ] Webhook en el servidor: Stripe pide HTTPS en modo real; mientras tanto la consulta directa (al volver el apoderado y cada
  minuto para órdenes vencidas) resuelve los pagos aunque la notificación no llegue.
- [ ] La pasarela simulada guarda el resultado en memoria: si el backend se reinicia, las órdenes abiertas terminan expirando.

## Notas para quien siga

- En local, para probar el webhook de Stripe: `stripe listen --forward-to localhost:8080/api/pagos/pasarela/stripe/notificaciones`
  (Stripe CLI) y usar el `whsec_…` que imprime.
- HU-31 escucha el evento `PagoEnLineaAprobado` para matricular cuando la cuota pagada es la de matrícula.
