# Matrícula

- **HU:** HU-24, 27, 30 (iteración 1) · HU-28, 29, 31, 32 (2) · HU-25, 26, 33, 71, 72 (3)
- **Estado:** 🚧 en desarrollo: HU-24, HU-27, HU-30 y HU-31 completas (HU-24: listado A3, Nueva matrícula, detalle A5/B6/B7 y Cambiar sección, todo conectado a la BD)
- **Última actualización:** 2026-11-05

## Alcance

El Personal administrativo reserva una sección para cada estudiante, nuevo o continuador. El apoderado sube los documentos firmados. El colegio los valida, se genera la cuota de matrícula y, al pagarla, el estudiante queda **matriculado** con un código (`MAT-2027-0044`) y su cronograma de pensiones.

La configuración del periodo (fechas, reglas y vacantes) está en [configuracion.md](configuracion.md).

## Flujo (Figma › "Flujo de matrícula · Personal administrativo" y Apoderado HU-28 a HU-32)

```
SECCION_RESERVADA ──(apoderado sube documentos)──► EN_VALIDACION ──(colegio aprueba y no hay deuda)──►
PENDIENTE_PAGO (se crea el compromiso con la cuota de matrícula, HU-36) ──(pago confirmado)──►
MATRICULADO (código MAT-…, cronograma de pensiones, HU-37)          · también: RETIRADO, ANULADA
```

- **Deuda (HU-30):** antes de aprobar se revisa si **el estudiante** tiene cuotas vencidas; la deuda de un hermano no bloquea su matrícula (corrección de la iteración 2). Se calcula desde `pagos.cuota`; no se guarda.
- **Vacantes:** capacidad de la sección (`configuracion.seccion.capacidad_maxima`) menos las matrículas activas de esa sección. Se calculan; cambiar de sección (HU-26) mueve la matrícula y deja historial.

## Endpoints (apoderado)

Ver la tabla de [`backend/README.md`](../../backend/README.md): hijos y estado (`GET /matricula/apoderado/estudiantes` y `/{id}/estado`) y documentos (HU-28 y HU-29: `GET .../documentos`, descargar plantilla, ver archivo, `PUT .../documentos/{id}` para subir o reemplazar y `POST .../enviar-validacion`). El apoderado solo actúa sobre sus hijos, con la sección reservada y el periodo abierto. Si el colegio completa todos los documentos (al crear la matrícula o al subir el último), la matrícula pasa sola a EN_VALIDACION.

## Endpoints (personal administrativo · HU-24)

`GET /matricula/periodos/{periodoId}` y `GET /matricula/periodos/{periodoId}/matriculas` alimentan la pantalla A3 «Matrículas del periodo» (`/app/administrativo/matricula/periodos/:periodoId/matriculas`). Ya no usa datos de ejemplo: el aviso (abierto o cerrado, con la fecha de cierre real), los cuatro indicadores, el buscador, el filtro de grado y estado, el orden y la paginación vienen del servidor.

- **Solo lista a quienes ya tienen matrícula.** Un estudiante inscrito sin matrícula no aparece ni cuenta en los indicadores; se matricula con «Nueva matrícula» (pendiente).
- **Indicadores:** *en proceso* = no matriculados; *requieren acción* = sección reservada o en validación (lo que espera el colegio).
- **Nueva matrícula (A6):** el botón abre el modal «Nueva matrícula» (paso 1): se busca al alumno por nombre o DNI, se ven sus datos, su sección y los 4 documentos pendientes, y «Guardar» crea la matrícula en `SECCION_RESERVADA`. La sección se asigna sola: la primera del grado con cupo en orden alfabético (si A está llena pasa a B; sin cupo en ninguna, no se puede guardar). Solo con el periodo ABIERTO; con el periodo cerrado el botón queda desactivado hasta que exista la matrícula extemporánea (HU-25). Los documentos se pueden subir ahora o después. El tipo es CONTINUADOR si el estudiante estuvo matriculado en un año anterior; si no, NUEVO. Solo se ofrecen estudiantes cuyo año académico es el del periodo.
- **Cambiar sección (A5 y A6):** el modal lista las secciones del grado con tutor, vacantes libres y ocupación `n/capacidad`; la actual va marcada, las llenas no se pueden elegir y se muestra el «Efecto en las vacantes» (se libera una en el origen y se ocupa una en el destino). Al confirmar «¿Cambiar sección?» la matrícula se mueve y queda un registro en `matricula.cambio_seccion` **sin motivo** (decisión del equipo; el historial es HU-26). En Nueva matrícula la elección se envía con «Guardar» (`seccionId`); si entre tanto la sección se llenó, se reasigna a la primera con cupo y se avisa. Con el periodo cerrado solo se puede cambiar de sección a quienes ya están `MATRICULADO`.
- **Detalle (A5, B6, B7):** `GET /matricula/matriculas/{id}` alimenta la pantalla real para todos los estados (stepper con `pasoActual`, alerta según el estado, datos del estudiante y apoderado, documentos con su estado EN_REVISION/APROBADO/OBSERVADO). Los documentos se pueden **ver** y **subir o reemplazar** (el reemplazo vuelve a `EN_REVISION`); retirada o anulada no admite cambios. En EN_VALIDACION cada documento en revisión tiene **Observar** (motivo obligatorio: la matrícula vuelve a SECCION_RESERVADA y el apoderado reemplaza solo ese documento; los aprobados quedan bloqueados) y **Validar** abre «¿Aprobar documentación?» (aprueba y pasa a PENDIENTE_PAGO; deshabilitado si hay observados). Pendiente: la cuota de matrícula y el bloqueo por deuda (HU-30/36). En PENDIENTE_PAGO y MATRICULADO se integra la tarjeta de pago de HU-36/37.
- **EN_VALIDACION** se conserva como cuarto estado (el apoderado lo alcanza al subir todo); cuenta como «requiere acción» igual que SECCION_RESERVADA.

- La deuda (HU-30) se pide a Pagos con `ServicioEstadoCuenta.cuotasVencidas` (IEstadoCuenta): pensiones impagas después del vencimiento más los días de gracia, solo del estudiante consultado. Solo bloquea si el periodo tiene `bloquear_por_morosidad`. El estado visible del periodo (PROGRAMADO, ABIERTO o CERRADO) lo calcula Configuración con `ServicioPeriodoMatricula.estadoEn` y el `Clock` de la app.
- `pasoActual` del stepper: 1 SECCION_RESERVADA, 2 EN_VALIDACION, 3 PENDIENTE_PAGO, 4 MATRICULADO (0 sin matrícula).
- Si `periodo.abierto` es false, el frontend no muestra la opción de matricular (RF-59).
- `pagoMatricula` (HU-31): la cuota de matrícula con `puedePagar` y, si no se puede, el `motivo`. Es null mientras la cuota no exista.

## HU-31 · Derivación a pagos

El apoderado paga la cuota de matrícula con el pago en línea de HU-41 (misma orden, misma pasarela). Cuando Pagos confirma el pago publica `PagoEnLineaAprobado` con `matriculasPagadas`; `EscuchaPagoMatricula` lo recibe **después de guardar el pago** y, para cada matrícula, `ServicioConfirmacionPago`:

1. `matricular`: si sigue en PENDIENTE_PAGO, la pasa a MATRICULADO con el código `MAT-AAAA-NNNN` (correlativo por año, con bloqueo de fila) y `confirmado_en`. Si ya estaba matriculada no hace nada.
2. `generarPensiones`: crea el cronograma de HU-37.

Cada paso va en su propia transacción: si algo falla se registra en el log, pero el pago ya cobrado nunca se deshace.

| Criterio | Qué se comprueba |
|---|---|
| CAHU31-1 | Con la matrícula en PENDIENTE_PAGO y la cuota generada, el apoderado ve "Pago de matrícula" y el botón **Pagar matrícula**. |
| CAHU31-2 | Solo bloquean las pensiones vencidas **del propio hijo**; la deuda de un hermano no impide pagar la matrícula (reunión con el PO, 30/10). Se valida al mostrar el botón y al crear la orden. |
| CAHU31-3 | El pago usa el flujo de HU-41 con una orden que contiene solo la cuota de matrícula. |
| CAHU31-4 | Al confirmarse: MATRICULADO, código `MAT-AAAA-NNNN`, pensiones generadas y paso 4 "Matriculado". |
| CAHU31-5 | Si el pago se rechaza o expira, la cuota vuelve a pendiente y la matrícula sigue en PENDIENTE_PAGO. |
| CAHU31-6 | Ya matriculado no hay botón, ni otro código, ni otro cronograma. |

Pruebas: `DerivacionPagosIT` y `EscuchaPagoMatriculaTests` (backend); `pago-matricula-apoderado.test.tsx` y `consulta-matricula.test.tsx` (frontend).


## Pantallas (Figma)

| Pantalla                        | Frame en Figma                                                                                                                                            |
| ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Matrículas del periodo          | Personal Administrativo › "A3 · Matrícula 2027 › Matrículas del periodo (HU-24, HU-71)"                                                                   |
| Detalle / validar               | "A5 · Detalle — Sección reservada", "A6 · Modal Completar matrícula", modal "¿Aprobar documentación?"                                                     |
| Pendiente de pago / Matriculado | "B6 · Detalle — Pendiente de pago (HU-36, HU-31)", "B7 · Detalle — Matriculado (HU-31, HU-37)"                                                            |
| Cambiar sección                 | modales "Cambiar sección" y "¿Cambiar sección?"                                                                                                           |
| Apoderado                       | Apoderado › "Matrícula 2027" (consulta de periodo, grado, sección, estado y progreso, HU-27) y "HU-28" a "HU-32" (documentos, deuda, pago y confirmación) |

## Base de datos

- **Esquema:** `matricula` · **Migraciones:** `V4` (periodo de matrícula) y `V5__matricula_y_pagos.sql`

| Tabla                             | Para qué                                                                                                                                                                   |
| --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `matricula.periodo_matricula`     | Fechas y reglas del periodo (V4).                                                                                                                                          |
| `matricula.documento_requerido`   | Documentos que pide el periodo, con plantilla opcional para descargar (HU-21/22, HU-28).                                                                                   |
| `matricula.matricula`             | Una por estudiante y año. Guarda grado, sección, NUEVO/CONTINUADOR, estado, código, si fue extemporánea y su motivo (HU-25), la fecha de confirmación (RF-68) y el retiro. |
| `matricula.documento_matricula`   | Archivo firmado de cada documento requerido: PDF, JPG o PNG de hasta 10 MB. Estado EN_REVISION, APROBADO u OBSERVADO; una observación es obligatoria si está OBSERVADO.    |
| `matricula.cambio_seccion`        | Historial de cambios de sección (HU-26).                                                                                                                                   |
| `matricula.correlativo_matricula` | Último número de código por año.                                                                                                                                           |

Desde una matrícula se llega a: el estudiante (`estudiantes.estudiante`) y su apoderado (`estudiantes.vinculo_familiar` con `es_responsable_matricula`), el año, el grado y la sección (`configuracion.*`), y el compromiso de pago (`pagos.compromiso_pago.matricula_id`).

**Datos de prueba:** Luis Paredes (`U20260003`) tiene dos hijos:

- **Sofía:** MATRICULADO en 4.° A de 2026, con el código `MAT-2026-0001`.
- **Mateo:** SECCION_RESERVADA en 1.° A de 2027.

El periodo 2026 está cerrado y el de 2027 habilitado. Ambos piden los 4 documentos de Figma.

## Observaciones encontradas

- **El año de la pantalla viene del periodo de matrícula:** el título muestra "Matrícula 2027" porque ese es el año escolar del periodo consultado, aunque sus fechas de apertura y cierre estén en 2026. En los datos de prueba, el periodo 2027 está habilitado del **15 de septiembre al 31 de octubre de 2026**. El frontend no calcula el año desde la fecha actual.
- **Sofía y Mateo tienen situaciones diferentes en 2027:** Mateo tiene una sección reservada en 1.° A; Sofía está matriculada en 4.° A de 2026, pero no tiene una matrícula creada para 2027, por lo que en el periodo consultado aparece pendiente de asignación.
- **La deuda se valida por estudiante (HU-30, corregida en la iteración 2):** la pensión vencida de S/ 600 pertenece a Sofía y solo bloquea la matrícula de Sofía. Mateo no tiene deuda propia, así que la deuda de su hermana ya no lo bloquea.
- **Tarjeta de pago (HU-36/37):** ya está integrada en el detalle real de HU-24 (estados PENDIENTE_PAGO y MATRICULADO).
- **Pantallas administrativas:** A3, A5, B6 y B7 son reales (HU-24). Siguen sin implementar HU-25, HU-26 (historial), HU-28, HU-29, HU-31 y la parte de HU-32 que genera la cuota y revisa la deuda.
- **Plantillas para el personal administrativo:** Nueva matrícula y el detalle muestran «Plantilla» en cada documento que la tiene (`GET /matricula/periodos/{id}/documentos/{id}/plantilla`).
- **Pantalla del apoderado (HU-28 y HU-29):** «Matrícula 2027» muestra la tarjeta «Documentos de matrícula»: descarga de plantilla, subir o reemplazar el firmado, ver el archivo y «Enviar a validación» (habilitado con todos los documentos). Los avisos cambian según la etapa (en validación, documentación validada, matriculado). Siguen pendientes la tarjeta de pago del apoderado (HU-31), la constancia y los datos del estudiante y apoderado dentro de esta pantalla.
- **HU-37 solo corresponde a estudiantes matriculados:** Sofía tiene el cronograma 2026 de los datos de prueba. Mateo todavía está en `SECCION_RESERVADA`, así que no debe tener pensiones generadas hasta completar la matrícula.
- **Figma es la fuente visual; la API es la fuente de los datos:** textos, jerarquía y componentes siguen los frames indicados arriba. Año, fechas, grado, sección, deuda, montos y estados se muestran desde el backend.

## Cómo probar las HU en pantalla

### Preparación

1. Levanta PostgreSQL y el backend en `http://localhost:8080/api`:
   ```powershell
   cd C:\Users\valer\Documents\GalilEdu\backend
   .\mvnw.cmd spring-boot:run
   ```
2. En otra terminal, levanta el frontend usando la API real:
   ```powershell
   cd C:\Users\valer\Documents\GalilEdu\frontend
   $env:VITE_APP_ENABLE_API_MOCKING='false'
   npm.cmd run dev
   ```
3. Abre `http://localhost:3000`. Todos los usuarios de prueba usan la contraseña `Galiledu2026`.

### HU-27 · Consulta del periodo y estado de matrícula

1. Inicia sesión como apoderado con `U20260003`.
2. En el menú lateral, abre **Matrícula** (`/app/apoderado/matricula`).
3. Con **Mateo Paredes** seleccionado, comprueba:
   - título **Matrícula 2027** y estado del periodo;
   - grado **1.° de primaria**, sección **A** y estado **Sección reservada**;
   - cierre **31 oct 2026** y primer paso del progreso.
4. Abre el selector de estudiante y elige **Sofía Paredes**.
5. Comprueba que la consulta cambia de hijo y que, para el periodo 2027, Sofía aparece **Pendiente de asignación** y sin sección confirmada.

**Resultado esperado:** el periodo, grado, sección, estado y progreso cambian según el hijo seleccionado y siempre provienen de la API.

### HU-30 · Validación de deuda del estudiante

1. Continúa en **Matrícula** con la sesión de `U20260003`.
2. Selecciona **Mateo Paredes** y comprueba que no aparece la tarjeta **Deuda pendiente** ni el impedimento por deuda.
3. Cambia a **Sofía Paredes**.
4. En la tarjeta **Deuda pendiente**, comprueba la pensión de septiembre, vencida el 30 de septiembre de 2026, por **S/ 600.00**, y el mensaje que atribuye la deuda a Sofía.

**Resultado esperado:** la deuda solo bloquea el proceso del hijo que la tiene (corrección de la iteración 2).

### HU-36 · Generación de la cuota de matrícula

1. Cierra la sesión del apoderado e inicia como personal administrativo con `U20260002`.
2. En **Configuración → Periodos de matrícula**, abre el periodo 2027 y pulsa **Ver matrículas**.
3. En la fila de **Mateo Paredes**, abre el menú `⋮` y selecciona **Ver detalle**. También puedes abrir directamente:
   `http://localhost:3000/app/administrativo/matricula/detalle/50000000-0000-0000-0000-000000000002`
4. Si la cuota todavía no existe, comprueba que aparece el botón **Generar cuota de matrícula** y presiónalo.
5. Comprueba que la tarjeta muestra el concepto, **S/ 450.00**, estado pendiente, vencimiento y responsable de la generación.
6. Recarga la página: debe mostrarse la misma cuota y no debe ofrecer crear una duplicada.
7. Para comprobar una matrícula histórica pagada, abre el detalle de **Sofía Paredes** desde el mismo listado o usa:
   `http://localhost:3000/app/administrativo/matricula/detalle/50000000-0000-0000-0000-000000000001`

**Resultado esperado:** la primera URL permite generar o consultar la cuota de Mateo; la segunda muestra la cuota pagada de Sofía con sus datos de pago. Si Mateo ya muestra una cuota al entrar, fue generada en una prueba anterior; para repetir el estado inicial se necesita restaurar una base de datos limpia.

### HU-37 · Consulta del cronograma de pensiones

1. Mantén la sesión de personal administrativo `U20260002`.
2. En el menú lateral, abre **Pensiones** (`/app/administrativo/pensiones`).
3. Localiza a **Sofía Paredes** en el estado de cuenta.
4. Abre el menú de acciones `⋮` y selecciona **Ver cronograma**.
5. Comprueba que el detalle contiene la matrícula y las diez pensiones de 2026, con sus montos, vencimientos y estados. La pensión de septiembre debe aparecer vencida.
6. Regresa al listado y verifica los totales del estado de cuenta.

**Resultado esperado:** Sofía tiene 11 cuotas en total —una matrícula y diez pensiones—. Mateo no aparece con cronograma porque todavía no está matriculado.

## Pendientes

- [x] HU-31: pago de la matrícula en línea, código `MAT-AAAA-NNNN` y pensiones al confirmarse el pago.
- [x] Backend de HU-27 y HU-30 (estado de matrícula y deuda del apoderado).
- [x] Pantalla de HU-27 y tarjeta del Home: consulta periodo, grado, sección, estado y progreso por cada hijo.
- [x] Pantalla de HU-30: alerta contextual que distingue deuda propia y deuda de otro hijo, detalle de cuotas vencidas por estudiante y total de deuda familiar.
- [ ] HU-24: reservar la sección.
- [x] Código de matrícula: `MAT-AAAA-NNNN`, correlativo por año. La BD lo exige (V7).
- [x] Al matricular se cobra solo la cuota de matrícula del tarifario (S/ 450 en 2027); el S/ 850 de la pantalla del apoderado no se usa. La cuota de ingreso va aparte. Ver la [decisión](../decisiones/2026-10-19-yatako-integracion-bd.md).
- [ ] Al quedar matriculado, crear la cuenta del estudiante y enviar las credenciales al apoderado (V7).
- [x] Decisión de producto (2026-11-06): los documentos los suben el apoderado **y** el personal administrativo (el colegio matricula cuando los padres no pueden). Las plantillas que descarga el apoderado son PDF, DOCX, JPG o PNG (antes XLSX/XLS/CSV, que no se firman).
- [ ] La información adicional de HU-32 (dirección, contacto de emergencia, alergias y seguro) ya existe en `estudiantes.*`. Matrícula la muestra y la actualiza; no la duplica.

- **Decisión de producto (2026-11-06): observar documentos.** El colegio observa un documento individual con un motivo; la matrícula regresa a SECCION_RESERVADA, el apoderado reemplaza solo los observados (los aprobados quedan bloqueados) y vuelve a enviar a validación. Si el colegio sube el documento observado por el apoderado, la matrícula vuelve sola a EN_VALIDACION. No se envía correo: el aviso es en pantalla.
