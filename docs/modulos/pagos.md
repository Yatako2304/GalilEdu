# Pagos y pensiones

- **HU:** HU-36, 37 (iteración 1) · HU-35, 38–42 (2)
- **Estado:** 🚧 en desarrollo: HU-37 hecha (backend y pantallas P2, P8 y modal de retiro); falta HU-36 y la iteración 2
- **Última actualización:** 2026-10-20

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

Ver la tabla de [`backend/README.md`](../../backend/README.md): `/pagos/estado-cuenta`, `/pagos/estudiantes/{id}/compromisos`, `/pagos/compromisos/{id}`, `/pagos/matriculas/{id}/cronograma` y `/pagos/compromisos/{id}/retiro`.

- Al confirmarse el pago de la matrícula (HU-31), Matrícula debe llamar a `ServicioCronograma.generarPensiones(matriculaId, usuarioId)`. Ver la [decisión](../decisiones/2026-10-20-leonardo-cronograma-pensiones.md).
- Flujo de la HU-37: menú **Pensiones** → P2 ("Todos los alumnos") → ⋮ del alumno → **Ver cronograma** (P8, `/app/administrativo/pensiones/{estudianteId}`) o **Registrar retiro** (modal, fecha de hoy). Falta el enlace desde B7 ("Ver compromiso de pago") cuando exista ese detalle de matrícula.
- Las pestañas "Compromisos vigentes" (P1, HU-38) y "Cuotas vencidas" (P3, HU-39) se ven desactivadas.

## Pantallas (Figma)

| Pantalla | Frame en Figma |
| --- | --- |
| Estado de cuenta | Personal Administrativo › "P1 · Compromisos vigentes", "P2 · Todos los alumnos", "P3 · Cuotas vencidas" |
| Compromiso | "P8 · Cronograma de cuotas", "P9 · Pagos (Pago, PagoDetalle)", "P10 · Descuento aplicado", "P11 · Morosidad" |
| Descuentos | "P21 · Catálogo de descuentos", modales "Nuevo descuento", "Añadir descuento" y "¿Revocar …?" |
| Cuotas extra | "P2b · Cuota adicional masiva", modales "Agregar cuota especial" y "Registrar retiro del alumno" |
| Apoderado | Apoderado › "HU-41 Pago en línea y HU-40", "HU-42 Constancia de pago" |

## Base de datos

- **Esquema:** `pagos` · **Migraciones:** `V4` (tarifario) y `V5__matricula_y_pagos.sql`

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
- [ ] HU-36: generar la cuota de matrícula.
- [x] La pensión vence el último día de cada mes (diciembre, el 15), como en el tarifario; el "día 05" del cronograma del apoderado no se usa. Ver la [decisión](../decisiones/2026-10-19-yatako-integracion-bd.md).
- [ ] Pasarela de pagos real: hoy la orden solo guarda la referencia y el resultado; falta elegir el proveedor.
- [ ] Generar el PDF de la constancia y del estado de cuenta (RF-98 y RF-108).
