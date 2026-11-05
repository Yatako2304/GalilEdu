# Pagos y pensiones

- **HU:** HU-36, 37 (iteración 1) · HU-35, 38–42 (2)
- **Estado:** 🚧 en desarrollo: HU-36, HU-37 y HU-41 hechas; falta integrar la tarjeta de HU-36 al detalle de HU-24 y completar la iteración 2
- **Última actualización:** 2026-11-04

## Alcance

Cada estudiante tiene un **compromiso de pago** por año escolar. Contiene la cuota de matrícula, las pensiones del tarifario y las cuotas especiales o masivas. El Personal administrativo aplica descuentos del catálogo y consulta la morosidad. El apoderado ve el cronograma, paga en línea una o varias cuotas y descarga sus constancias.

El tarifario y la política de morosidad están en [configuracion.md](configuracion.md).

## Flujo

```
Matrícula validada ─► compromiso + cuota de matrícula (HU-36) ─► pago ─► matriculado ─► pensiones del tarifario (HU-37)
Apoderado elige cuotas ─► orden de pago (cuotas EN_PROCESO) ─► pasarela ─► APROBADA: pago + constancia C-2026-…
                                                                        └► RECHAZADA: las cuotas vuelven a PENDIENTE
```

**Se calcula al consultar y no se guarda:**
- **Cuota vencida:** una pensión impaga después de su vencimiento más los días de gracia.
- **Días de atraso.**
- **Recargo por mora:** la tasa por cada 30 días sobre el capital pendiente, sin pasar del tope.
- **Deuda del apoderado:** la suma de todos sus hijos.

Al pagar, el recargo cubierto queda guardado en `pago_detalle.recargo`.

## Endpoints

Ver la tabla de [`backend/README.md`](../../backend/README.md): `/pagos/estado-cuenta`, `/pagos/estudiantes/{id}/compromisos`, `/pagos/compromisos/{id}`, `/pagos/matriculas/{id}/cronograma`, `/pagos/compromisos/{id}/retiro` y `/pagos/matriculas/{id}/cuota-matricula`.

- HU-36: cuando el colegio aprueba los documentos (HU-29/HU-72), Matrícula debe llamar a `ServicioCuotaMatricula.generar(matriculaId, usuarioId)`; para saber si ya se pagó (HU-31) usa `ServicioCuotaMatricula.consultar(matriculaId)`. Mientras tanto, el personal la genera con el `POST`. Como el cronograma, exige el tarifario del año CONFIRMADO (P19, HU-34): en los datos de prueba el de 2027 está EN_CONFIGURACION, así que hay que confirmarlo antes de generar la cuota de Mateo. La tarjeta "Pago de matrícula" de B6/B7 usa el `GET` (404 = todavía no generada) y su enlace "Ver compromiso de pago" lleva a P8 con el `estudianteId` de la respuesta.
- Al confirmarse el pago de la matrícula (HU-31), Matrícula debe llamar a `ServicioCronograma.generarPensiones(matriculaId, usuarioId)`. Ver la [decisión](../decisiones/2026-10-20-leonardo-cronograma-pensiones.md).
- Flujo de la HU-37: menú **Pensiones** → P2 ("Todos los alumnos") → ⋮ del alumno → **Ver cronograma** (P8, `/app/administrativo/pensiones/{estudianteId}`) o **Registrar retiro** (modal, fecha de hoy). Falta el enlace desde B7 ("Ver compromiso de pago") cuando exista ese detalle de matrícula.
- Las pestañas "Compromisos vigentes" (P1, HU-38) y "Cuotas vencidas" (P3, HU-39) se ven desactivadas.

## HU-41 · Pago en línea

Ver la [decisión](../decisiones/2026-11-04-yatako-pasarela-de-pagos.md). Código: `pagos/aplicacion/ServicioPagoEnLinea`, `PasarelaPagos` (puerto) con `PasarelaStripe` y `PasarelaSimulada`; frontend en `features/pagos` (`seleccion-cuotas.ts`, `barra-pago-cuotas.tsx`, `pago-cuotas.tsx`, `resultado-pago.tsx`).

- **Pasarela:** `PASARELA_PAGOS=simulada` (por defecto, no cobra; la "página de pago" es `/app/apoderado/pensiones/pasarela-simulada`) o `stripe` (Stripe Checkout en modo de prueba, con `STRIPE_SECRET_KEY` y `STRIPE_WEBHOOK_SECRET`).
- **Recargo por mora** (sección 3.10): `Morosidad.recargo` = capital pendiente × tasa × períodos de 30 días desde el vencimiento, solo pasada la gracia y sin pasar del tope. Solo pensiones.
- **Una orden puede pagar cuotas de varios hijos;** al aprobarse se registra **un pago con su constancia por estudiante** (V11 quitó el UNIQUE de `pago.orden_pago_id`).
- **Evento `PagoEnLineaAprobado`:** se publica al aprobarse una orden, con las cuotas que quedaron pagadas. Matrícula lo escucha para HU-31.

**Cómo se acopla con HU-40 (cronograma del apoderado):** en Figma ("HU-41 Pago en línea de cuotas pendientes y HU-40") la tabla del cronograma tiene una columna de casillas y encima la barra de selección. La tabla de HU-40 usa `useSeleccionCuotas(useCuotasPorPagar().data)` para sus casillas (`estaSeleccionada`, `alternar`) y pone `<BarraPagoCuotas>` encima; al pagar llama a `useCrearOrdenPago().mutate(ids)`. Mientras HU-40 no exista, `PagoCuotas` arma una tabla mínima en Pensiones.

| Código | Criterio de aceptación |
| --- | --- |
| CAHU41-1 | El apoderado (padre, madre o apoderado vinculado) ve las cuotas pendientes y vencidas de cada hijo, con capital y recargo por separado. |
| CAHU41-2 | Al marcar cuotas (de uno o varios hijos), el total (capital + recargo) se actualiza. |
| CAHU41-3 | Las pensiones se pagan en orden: al marcar una se marcan las anteriores impagas de ese hijo; el backend rechaza saltarse una. |
| CAHU41-4 | Una cuota pagada, anulada o en otro pago en proceso no se puede marcar. |
| CAHU41-5 | "Pagar seleccionadas" crea la orden, pone las cuotas en proceso y lleva a la pasarela; el sistema no pide ni guarda datos de tarjeta. |
| CAHU41-6 | Pago aprobado: pago con constancia `C-AAAA-NNNNN` por estudiante, cuotas pagadas con el recargo cubierto y pantalla "Pago realizado". |
| CAHU41-7 | Pago rechazado: la orden queda rechazada con su motivo y las cuotas vuelven a su estado anterior. |
| CAHU41-8 | Sin confirmación en 30 minutos la orden expira y las cuotas vuelven a pendiente. |
| CAHU41-9 | Si la notificación no llega, al volver el apoderado (o al expirar) se consulta el estado a la pasarela. |
| CAHU41-10 | Una notificación repetida no registra dos pagos. |
| CAHU41-11 | Si la pasarela confirma otro monto, el pago queda OBSERVADO con la diferencia. |
| CAHU41-12 | Una cuota en proceso no cuenta como vencida ni bloquea la matrícula mientras se resuelve. |
| CAHU41-13 | Nadie puede pagar ni ver cuotas u órdenes de una familia que no es la suya. |

Pruebas: `PagoEnLineaIT` (un caso por criterio), `MorosidadTests`, `FirmaStripeTests`; `pago-cuotas.test.tsx`.

## Pantallas (Figma)

| Pantalla | Frame en Figma |
| --- | --- |
| Estado de cuenta | Personal Administrativo › "P1 · Compromisos vigentes", "P2 · Todos los alumnos", "P3 · Cuotas vencidas" |
| Compromiso | "P8 · Cronograma de cuotas", "P9 · Pagos (Pago, PagoDetalle)", "P10 · Descuento aplicado", "P11 · Morosidad" |
| Descuentos | "P21 · Catálogo de descuentos", modales "Nuevo descuento", "Añadir descuento" y "¿Revocar …?" |
| Cuotas extra | "P2b · Cuota adicional masiva", modales "Agregar cuota especial" y "Registrar retiro del alumno" |
| Apoderado | Apoderado › "HU-41 Pago en línea y HU-40", "HU-42 Constancia de pago" |

## Base de datos

- **Esquema:** `pagos` · **Migraciones:** `V4` (tarifario), `V5__matricula_y_pagos.sql` y `V11__pago_en_linea.sql` (vencimiento de la orden, estado anterior de cada cuota y varios pagos por orden)

| Tabla | Para qué |
| --- | --- |
| `pagos.concepto` | Catálogo fijo: CUOTA_INGRESO, MATRICULA, PENSION, SERVICIO y EXTRAORDINARIA. Solo PENSION genera mora (RF-91). |
| `pagos.configuracion` | Una sola fila con el porcentaje máximo de descuento (RF-77, 50 % por defecto). |
| `pagos.descuento`, `descuento_tramo`, `descuento_concepto` | Catálogo de descuentos (HU-35). Hay dos tipos: de porcentaje único y por hermanos, este último con tramos ("2: 10 %, 3+: 15 %"). Cada descuento indica los conceptos a los que aplica. |
| `pagos.compromiso_pago` | Uno por estudiante y año (RF-83). Se enlaza con la matrícula y el tarifario; puede anularse con un motivo. |
| `pagos.cuota` | Cada cuota guarda `monto_base`, `monto_descuento`, `monto_final` (calculado por la BD) y `monto_pagado`. Estado PENDIENTE, EN_PROCESO, PAGADA_PARCIAL, PAGADA o ANULADA. Solo puede haber una cuota de matrícula por compromiso (RF-79) y la pensión N no se duplica. |
| `pagos.lote_cuota` | Cuota adicional masiva: los datos comunes de la cuota extraordinaria generada para varios estudiantes. |
| `pagos.descuento_aplicado` | Descuento aplicado (HU-38). Guarda una copia del nombre y del porcentaje, la vigencia, el motivo, quién lo aplicó y la revocación. Es del **estudiante**, con una vigencia que puede durar varios años (V7). Solo puede haber uno ACTIVO por estudiante (RF-87), y cada cuota rebajada guarda su `descuento_aplicado_id`. |
| `pagos.orden_pago`, `orden_pago_cuota` | Orden de pago en línea (HU-41) con sus cuotas, capital y recargo. La `referencia_pasarela` es única, así un pago no se registra dos veces (RF-103). |
| `pagos.pago`, `pago_detalle` | Pago confirmado con su constancia correlativa (HU-42). Queda OBSERVADO si el monto no coincide con la orden (RF-105). |
| `pagos.correlativo_constancia` | Último número de constancia por año. |

**Datos de prueba:**
- Compromiso 2026 de Sofía Paredes (hija de `U20260003`): la matrícula y las pensiones de marzo a agosto están pagadas, con 7 constancias. La pensión de septiembre está **vencida** (S/ 600), así que su papá tiene deuda y sirve para probar HU-30.
- Catálogo de descuentos de Figma: Hermanos, Beca 50 %, Acuerdo 20 % y Deportista 25 % (inactivo).
- Tarifario 2026 confirmado.

## Pendientes

- [x] HU-37: generar, consultar y anular por retiro el cronograma de pensiones.
- [x] HU-36 (backend): generar y consultar la cuota de matrícula.
- [x] HU-36 (front): tarjeta reutilizable "Pago de matrícula" de B6/B7; se abre con `?matriculaId=` hasta que HU-24 aporte el detalle administrativo.
- [x] La pensión vence el último día de cada mes (diciembre, el 15), como en el tarifario; el "día 05" del cronograma del apoderado no se usa. Ver la [decisión](../decisiones/2026-10-19-yatako-integracion-bd.md).
- [x] Pasarela de pagos (HU-41): Stripe Checkout en modo de prueba, con una pasarela simulada mientras no haya llaves.
- [ ] Crear la cuenta de Stripe en modo de prueba y configurar el webhook en el servidor (necesita HTTPS para el modo real).
- [ ] HU-40: reemplazar la tabla mínima de `PagoCuotas` por el cronograma completo usando `useSeleccionCuotas` y `BarraPagoCuotas`.
- [ ] Generar el PDF de la constancia y del estado de cuenta (RF-98 y RF-108).
