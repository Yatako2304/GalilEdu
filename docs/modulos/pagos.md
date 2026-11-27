# Pagos y pensiones

- **HU:** HU-36, 37 (iteración 1) · HU-35, 38–41 (2) · HU-42 (3)
- **Estado:** 🚧 en desarrollo: HU-35 a HU-42 implementadas en backend y frontend; falta integrar la tarjeta de HU-36 al detalle de HU-24
- **Última actualización:** 2026-11-27

## Alcance

Cada estudiante tiene un **compromiso de pago** por año escolar. Contiene la cuota de matrícula, las pensiones del tarifario y las cuotas especiales o masivas. El Personal administrativo aplica descuentos del catálogo y consulta la morosidad. El apoderado ve el cronograma, paga en línea una o varias cuotas y descarga sus constancias.

El tarifario y la política de morosidad están en [configuracion.md](configuracion.md).

## Flujo

```
Matrícula validada ─► compromiso + cuota de matrícula (HU-36) ─► pago ─► matriculado ─► pensiones del tarifario (HU-37)
Apoderado elige cuotas ─► orden de pago (cuotas EN_PROCESO) ─► pasarela ─► APROBADA: pago + constancia C-2026-…
Personal adm. «Registrar pago» (P8) ─► cuotas + fecha + medio ─────────────────► pago en caja + constancia C-2026-…
                                                                        └► RECHAZADA: las cuotas vuelven a PENDIENTE
```

**Se calcula al consultar y no se guarda:**
- **Cuota vencida:** una pensión impaga después de su vencimiento más los días de gracia.
- **Días de atraso.**
- **Recargo por mora:** la tasa por cada **30 días completos** de atraso, contados desde el vencimiento, sobre el capital pendiente (monto final menos lo pagado), sin capitalizar y sin pasar del tope (porcentaje del capital). Como en Figma P11: S/ 600 con tasa 2 % → 32 días = S/ 12 y 63 días = S/ 24. Solo las pensiones generan mora (RF-91).
- **Deuda del apoderado:** la suma de todos sus hijos.

Al pagar, el recargo cubierto queda guardado en `pago_detalle.recargo`.

## Endpoints

Ver la tabla de [`backend/README.md`](../../backend/README.md): `/pagos/estado-cuenta`, `/pagos/estudiantes/{id}/compromisos`, `/pagos/compromisos/{id}` (y `/morosidad`, `/pagos`, `/descuento`, `/descuentos-aplicables`, `/descuentos`), `/pagos/descuentos`, `/pagos/descuentos-aplicados/{id}/revocacion`, `/pagos/apoderado/...`, `/pagos/matriculas/{id}/cronograma`, `/pagos/compromisos/{id}/retiro` y `/pagos/matriculas/{id}/cuota-matricula`.

- El tarifario se lee con el servicio de Configuración (`ServicioTarifario.vigente` y `calendario`, de Luciana); ya no hay SQL propio sobre `pagos.tarifario` para generar cuotas. Sin tarifario CONFIRMADO responde 409 "El tarifario {año} aún no está confirmado.".
- HU-35: el límite de descuento (RF-77) se consulta y se cambia en **Configuración de pensiones → Catálogo de descuentos → Cambiar límite**. Rige para lo que se registre o edite desde el cambio; los descuentos ya guardados no se revalidan.
- **Cuota de ingreso (corrección 4 de la iteración 1):** al generar la cuota de matrícula (HU-36) de un estudiante **NUEVO**, se agrega también la cuota de ingreso con el monto del tarifario (`monto_cuota_ingreso`) y el mismo vencimiento. Se cobra una sola vez por estudiante (no se genera si ya tiene una no anulada, ni si el tarifario no tiene monto). No condiciona la confirmación de la matrícula, que sigue dependiendo del pago de la cuota de matrícula; el apoderado la paga en línea como cualquier otra cuota y no genera mora.
- HU-38: el descuento es del **estudiante**: al aplicarlo se rebajan las cuotas PENDIENTES sin pagos de todos sus compromisos vigentes cuyos conceptos cubre y que vencen dentro de la vigencia (por defecto, desde hoy). "Hermanos matriculados" cuenta a los estudiantes con matrícula MATRICULADO en el año del compromiso que comparten padre, madre o apoderado, incluido él mismo.
- HU-38 (corrección del PO): el descuento **por hermanos** solo se aplica al hermano matriculado de **menor edad** del año (fecha de nacimiento más reciente; si son gemelos, a ambos). El porcentaje sale del tramo por la cantidad de hermanos matriculados. A otro hermano se le rechaza con 409 y en «descuentos aplicables» llega con `aplicable: false`, el motivo y los `beneficiarios`. Con 3 o más hermanos, también solo al menor (pendiente de confirmar con el PO).
- HU-38 (corrección del PO): un descuento puede regir **hasta que el estudiante termine el colegio** (`hastaFinEscolaridad: true`, sin fecha de fin). El personal elige esta opción en el formulario; como el descuento es del estudiante, sigue rebajando la cuota de matrícula y las pensiones que se generen en los años siguientes, sin volver a registrarlo. Deja de aplicarse si el personal lo revoca o lo reemplaza, y en la práctica al terminar el colegio o retirarse, porque ya no se generan compromisos nuevos (el egreso todavía no existe en el sistema).
- HU-40: lo consulta cualquier padre, madre o apoderado vinculado al estudiante (no solo el responsable de matrícula, corrección 2). El "código de estudiante" es su número de documento porque la ficha no tiene otro código.

- HU-36: cuando el colegio aprueba los documentos (HU-29/HU-72), Matrícula debe llamar a `ServicioCuotaMatricula.generar(matriculaId, usuarioId)`; para saber si ya se pagó (HU-31) usa `ServicioCuotaMatricula.buscar(matriculaId)` (no lanza error si aún no existe). Mientras tanto, el personal la genera con el `POST`. Como el cronograma, exige el tarifario del año CONFIRMADO (P19, HU-34): en los datos de prueba el de 2027 está EN_CONFIGURACION, así que hay que confirmarlo antes de generar la cuota de Mateo. La tarjeta "Pago de matrícula" de B6/B7 usa el `GET` (404 = todavía no generada) y su enlace "Ver compromiso de pago" lleva a P8 con el `estudianteId` de la respuesta.
- Al confirmarse el pago de la matrícula (HU-31), Matrícula llama a `ServicioCronograma.generarPensiones(matriculaId, usuarioId)` desde `EscuchaPagoMatricula`. Ver la [decisión](../decisiones/2026-10-20-leonardo-cronograma-pensiones.md).
- Flujo de la HU-37: menú **Pensiones** → P2 ("Todos los alumnos") → ⋮ del alumno → **Ver cronograma** (P8, `/app/administrativo/pensiones/{estudianteId}`) o **Registrar retiro** (el retiro completo de Matrícula, con fecha y motivo; ver [matricula.md](matricula.md#retiro-del-alumno-del-colegio-fila-36-del-plan)). Falta el enlace desde B7 ("Ver compromiso de pago") cuando exista ese detalle de matrícula.
- En P8 ya están operativas las pestañas **Pagos** (P9), **Descuento** (P10) y **Morosidad** (P11). El catálogo global de descuentos se abre desde **Configuración de pensiones → Catálogo de descuentos** (P21).
- P8 muestra el **recargo por mora** en la misma fila de cada cuota vencida. En los textos de la interfaz se usa **monto pagado**, **monto de cuotas** y **monto pendiente**; `capital` se conserva únicamente como nombre técnico del contrato con el backend.
- En P21 los tramos por hermanos pueden quitarse mientras quede una fila editable. Antes de guardar se exige un mínimo de dos tramos y se rechazan rangos superpuestos, sin importar el orden en que se ingresen. Los tramos ocultos no se validan cuando el tipo es **Porcentaje único** (DEF-01).
- RF-77 se muestra en P21 mediante la tarjeta **Límite institucional de descuento** y el diálogo **Cambiar límite** (1 % a 100 %), construidos con los mismos componentes del sistema de diseño; el frame P21 original solo mostraba el catálogo y el límite no tenía pantalla propia.
- La vista **Apoderado → Pensiones** permite cambiar de hijo y compromiso, consultar el cronograma con mora y revisar pagos. Las casillas y el botón de pago permanecen deshabilitados porque corresponden a HU-41.
- Las pestañas "Compromisos vigentes" (P1, HU-38) y "Cuotas vencidas" (P3, HU-39) se ven desactivadas.

## HU-41 · Pago en línea

Ver la [decisión](../decisiones/2026-11-04-yatako-pasarela-de-pagos.md). Código: `pagos/aplicacion/ServicioPagoEnLinea`, `PasarelaPagos` (puerto) con `PasarelaStripe` y `PasarelaSimulada`; frontend en `features/pagos` (`seleccion-cuotas.ts`, `barra-pago-cuotas.tsx`, `pensiones-apoderado.tsx`, `resultado-pago.tsx`).

- **Pasarela:** `PASARELA_PAGOS=simulada` (por defecto, no cobra; la "página de pago" es `/app/apoderado/pensiones/pasarela-simulada`) o `stripe` (Stripe Checkout en modo de prueba, con `STRIPE_SECRET_KEY` y `STRIPE_WEBHOOK_SECRET`).
- **Recargo por mora** (sección 3.10): `Cuota.recargo`, la misma regla de HU-39 (Figma P11): capital pendiente × tasa × períodos **completos** de 30 días desde el vencimiento, solo pasada la gracia y sin pasar del tope. Solo pensiones. Así la orden cobra lo mismo que muestra la pestaña Morosidad.
- **Una orden puede pagar cuotas de varios hijos;** al aprobarse se registra **un pago con su constancia por estudiante** (V13 quitó el UNIQUE de `pago.orden_pago_id`).
- **Evento `PagoRegistrado`** (antes `PagoEnLineaAprobado`): se publica al aprobarse una orden o al registrarse un pago en caja, con las cuotas que quedaron pagadas. Incluye `matriculasPagadas` (las matrículas cuya cuota quedó pagada); Matrícula lo escucha para HU-31. La cuota de matrícula no se puede pagar si el mismo estudiante tiene pensiones vencidas fuera de la orden (CAHU31-2).

**Cómo se acopla con HU-40 (cronograma del apoderado):** en Figma ("HU-41 Pago en línea de cuotas pendientes y HU-40") la tabla del cronograma tiene una columna de casillas y encima la barra de selección. La tabla de HU-40 usa `useSeleccionCuotas(useCuotasPorPagar().data)` para sus casillas (`estaSeleccionada`, `alternar`) y pone `<BarraPagoCuotas>` encima; al pagar llama a `useCrearOrdenPago().mutate(ids)`. Ya está hecho en `pensiones-apoderado.tsx`: solo se pueden marcar las cuotas que el backend devuelve como pagables.

| Código | Criterio de aceptación |
| --- | --- |
| CAHU41-1 | El apoderado (padre, madre o apoderado vinculado) ve las cuotas pendientes y vencidas de cada hijo, con capital y recargo por separado. |
| CAHU41-2 | Al marcar cuotas (de uno o varios hijos), el total (capital + recargo) se actualiza. |
| CAHU41-3 | Las pensiones se pagan en orden: al marcar una se marcan las anteriores impagas de ese hijo; el backend rechaza saltarse una. |
| CAHU41-4 | Una cuota pagada, anulada o en otro pago en proceso no se puede marcar. |
| CAHU41-5 | "Pagar seleccionadas" crea la orden, pone las cuotas en proceso y lleva a la pasarela; el sistema no pide ni guarda datos de tarjeta. |
| CAHU41-6 | Pago aprobado: pago con constancia `C-AAAA-NNNNN` por estudiante, cuotas pagadas con el recargo cubierto y pantalla "Pago realizado". |
| CAHU41-7 | Pago rechazado: la orden queda rechazada con su motivo y las cuotas vuelven a su estado anterior. |
| CAHU41-8 | Sin confirmación en 30 minutos la orden expira y las cuotas vuelven a pendiente. Si el apoderado cerró la pasarela, Pensiones le muestra el aviso "Tienes un pago en proceso" con **Continuar pago** (vuelve a la pasarela) y **Cancelar pago** (libera las cuotas al momento); `GET /cuotas` trae la orden abierta en `ordenEnProceso`. |
| CAHU41-9 | Si la notificación no llega, al volver el apoderado (o al expirar) se consulta el estado a la pasarela. |
| CAHU41-10 | Una notificación repetida no registra dos pagos. |
| CAHU41-11 | Si la pasarela confirma otro monto, el pago queda OBSERVADO con la diferencia. |
| CAHU41-12 | Una cuota en proceso no cuenta como vencida ni bloquea la matrícula mientras se resuelve. |
| CAHU41-13 | Nadie puede pagar ni ver cuotas u órdenes de una familia que no es la suya. |

Pruebas: `PagoEnLineaIT` (un caso por criterio), `CuotaTests` (recargo), `FirmaStripeTests`; `seleccion-cuotas.test.ts` y `pensiones-apoderado.test.tsx`.

## Estado de cuenta (P2)

Lista a **todos los estudiantes no eliminados**, también los RETIRADOS (con la etiqueta «Retirado»), porque pueden haber dejado deuda antes de retirarse. **Orden:** por año del compromiso (2026, 2027…) y, dentro de cada año, alfabético; al final los que no tienen compromiso. La lista se pagina de 10 en 10 en el navegador. Cada uno aparece con su compromiso más reciente, el VIGENTE primero, aunque sea de otro año: antes solo salían los compromisos del año ACTIVO y, con la matrícula del año siguiente abierta, la lista quedaba casi vacía. La columna «Compromiso» (no está en Figma) muestra el año de ese compromiso, para distinguir quién paga 2026, 2027, etc.; quien no tiene sale con «Sin compromiso» y montos en cero. Las tarjetas suman los compromisos VIGENTES de la lista; con `?anio=` se filtra un año (y las tarjetas dicen el año).

## Pago en caja (fila 29 del plan de pruebas)

El apoderado paga en el colegio y el Personal administrativo lo registra con el botón **«Registrar pago»**, a la derecha de las pestañas del cronograma (P8). No hay frame en Figma: el modal sigue el de «Añadir descuento». Ver la [decisión](../decisiones/2026-11-27-fabricio99-s-pago-en-caja.md).

- **Reglas (las mismas de HU-41, en `CobroCuotas`):** cuotas completas; las pensiones en orden de antigüedad (al marcar una se marcan las anteriores); la matrícula no se paga con pensiones vencidas fuera del pago; una cuota EN_PROCESO (pago en línea abierto) no se puede registrar.
- **Fecha de pago:** hoy o anterior, nunca futura. El recargo por mora se calcula a esa fecha.
- **Medio de pago:** Efectivo, Transferencia, Depósito, Yape/Plin o Tarjeta (POS). Todos menos el efectivo piden número de operación, y la misma operación no se registra dos veces.
- **Sin migración:** el pago usa `pagos.pago` tal como está. Se reconoce porque no tiene orden y sí `registrado_por`; el medio y la operación van como texto en `referencia` ("Transferencia · 00123456") y la observación del personal en `observacion` (el pago queda CONFIRMADO).
- Emite la misma constancia correlativa y publica `PagoRegistrado`, así que pagar la matrícula en caja también matricula (HU-31). La pestaña «Pagos» muestra el medio ("En línea" o el de caja).

## HU-42 · Constancia de pago

Ver la [decisión](../decisiones/2026-11-19-yatako-constancia-de-pago-pdf.md). Código: `ArchivoConstanciaPago` (PDF con PDFBox, mismo estilo que el reporte de notas), `ConsultasPagos.constancia` y `detalleConstancia`, `ServicioDeudaApoderado.constancia` y `constanciaPdf`, y `GET /pagos/apoderado/pagos/{id}/constancia`. Frontend: `descargarConstancia` (`api/pensiones-apoderado.ts`) y la columna de descarga de `TablaPagos`.

- **Dónde:** Apoderado › Pensiones › pestaña **Pagos**: cada fila tiene el botón ⬇ "Descargar constancia C-AAAA-NNNNN". La tabla sigue siendo **una fila por pago** (una constancia), no una por concepto como en Figma (ver la decisión). P9 del personal usa la misma tabla **sin** el botón.
- **Contenido (RF-107):** número de constancia, estudiante y documento, año escolar con grado y sección, fecha y hora del pago (Lima), referencia de la pasarela, estado (Confirmado u Observado con su observación), una fila por cuota cancelada (concepto, cuota, vencimiento, monto, recargo y subtotal) y los totales: monto de cuotas, recargo por mora y total pagado. Si hay muchas cuotas, la tabla continúa en otra página y los totales van al final.
- **Historial (RF-108):** es la misma pestaña Pagos, ya ordenada del pago más reciente al más antiguo (RF-97).
- **Acceso:** cualquier padre, madre o apoderado vinculado al estudiante (como HU-40). Un pago de otra familia o inexistente responde 404.

| Código | Criterio de aceptación |
| --- | --- |
| CAHU42-1 | El apoderado ve en Pagos cada pago de su hijo con su número de constancia, del más reciente al más antiguo. |
| CAHU42-2 | Al pulsar ⬇ descarga `constancia-C-AAAA-NNNNN.pdf` con los datos del RF-107. |
| CAHU42-3 | No puede descargar la constancia de un pago de otra familia (404). |
| CAHU42-4 | El personal administrativo no usa la vista del apoderado (403) y en P9 no aparece el botón. |

Pruebas: `ArchivoConstanciaPagoTests` (contenido, pago observado y paginación), `DeudaApoderadoIT` (descarga, otra familia, pago inexistente y personal); `tablas-pagos.test.tsx` y `pensiones-apoderado.test.tsx` (botón y descarga).

## Pantallas (Figma)

| Pantalla | Frame en Figma |
| --- | --- |
| Estado de cuenta | Personal Administrativo › "P1 · Compromisos vigentes", "P2 · Todos los alumnos", "P3 · Cuotas vencidas" |
| Compromiso | "P8 · Cronograma de cuotas", "P9 · Pagos (Pago, PagoDetalle)", "P10 · Descuento aplicado", "P11 · Morosidad" |
| Descuentos | "P21 · Catálogo de descuentos", modales "Nuevo descuento", "Añadir descuento" y "¿Revocar …?" |
| Cuotas extra | "P2b · Cuota adicional masiva", modales "Agregar cuota especial" y "Registrar retiro del alumno" |
| Apoderado | Apoderado › "HU-41 Pago en línea y HU-40", "HU-42 Constancia de pago" |

## Base de datos

- **Esquema:** `pagos` · **Migraciones:** `V4` (tarifario), `V5__matricula_y_pagos.sql`, `V13__pago_en_linea.sql` (vencimiento de la orden, estado anterior de cada cuota y varios pagos por orden) y `V14__pago_en_linea_stripe.sql` (referencias y URL de Stripe más largas)

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
- [x] HU-39 (backend): morosidad del compromiso (`GET /pagos/compromisos/{id}/morosidad`), como Figma P11.
- [x] HU-39 (front): pestaña "Morosidad (n)" en P8, con monto pendiente y recargo separados.
- [x] HU-36 (backend): generar y consultar la cuota de matrícula.
- [x] HU-36 (front): tarjeta reutilizable "Pago de matrícula" de B6/B7; se abre con `?matriculaId=` hasta que HU-24 aporte el detalle administrativo.
- [x] La pensión vence el último día de cada mes (diciembre, el 15), como en el tarifario; el "día 05" del cronograma del apoderado no se usa. Ver la [decisión](../decisiones/2026-10-19-yatako-integracion-bd.md).
- [x] HU-35 (backend): catálogo de descuentos, tramos por hermanos, activación y límite de la institución.
- [x] HU-38 (backend): descuentos aplicables, aplicar con reemplazo confirmado, recálculo de cuotas pendientes, consulta y revocación.
- [x] HU-40 (backend): hijos con saldo y cuotas vencidas, cronograma con recargo e historial de pagos del apoderado.
- [x] HU-35 (front): catálogo P21, alta/edición, tramos por hermanos y activación/desactivación.
- [x] HU-38 (front): descuentos aplicables, reemplazo, ficha P10 y revocación con motivo.
- [x] HU-40 (front): selector de hijos y compromisos, cronograma con recargo e historial de pagos.
- [x] P9: historial de pagos del compromiso para personal administrativo.
- [x] Pasarela de pagos (HU-41): Stripe Checkout en modo de prueba, con una pasarela simulada mientras no haya llaves. Probado de punta a punta con Stripe en local (06/11): pago aprobado con la tarjeta 4242 y confirmado por consulta directa. Se quitó `payment_method_types` (Stripe ya no lo acepta; los métodos de pago se eligen en el dashboard).
- [x] HU-40 + HU-41 (front): las casillas del cronograma de HU-40 arman el pago en línea con `useSeleccionCuotas` y `BarraPagoCuotas`; se quitó la tabla mínima `PagoCuotas`.
- [ ] Crear la cuenta de Stripe en modo de prueba y configurar el webhook en el servidor (necesita HTTPS para el modo real).
- [x] HU-42: PDF de la constancia y su descarga desde el historial del apoderado (RF-107 y RF-108).
- [ ] Generar el PDF del estado de cuenta (RF-98).
- [x] Pago en caja (fila 29 del plan de pruebas): «Registrar pago» en P8, sin migración.
- [ ] Pago en caja: anular un pago mal registrado y pagos parciales (no se hicieron).
