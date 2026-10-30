# Matrícula

- **HU:** HU-24, 27, 30 (iteración 1) · HU-28, 29, 31, 32 (2) · HU-25, 26, 33, 71, 72 (3)
- **Estado:** 🚧 en desarrollo: HU-27 y HU-30 completas; falta HU-24
- **Última actualización:** 2026-10-30

## Alcance

El apoderado inicia la matrícula de cada estudiante, nuevo o continuador. Mientras el proceso no haya terminado, la sección se muestra como **Por asignar**. Al finalizar la matrícula, el colegio asigna la sección y el estudiante queda **matriculado** con un código (`MAT-2027-0044`) y su cronograma de pensiones.

La configuración del periodo (fechas, reglas y vacantes) está en [configuracion.md](configuracion.md).

## Flujo (Figma › "Flujo de matrícula · Personal administrativo" y Apoderado HU-28 a HU-32)

```
SECCION_RESERVADA ──(apoderado sube documentos)──► EN_VALIDACION ──(colegio aprueba y no hay deuda)──►
PENDIENTE_PAGO (se crea el compromiso con la cuota de matrícula, HU-36) ──(pago confirmado)──►
MATRICULADO (código MAT-…, cronograma de pensiones, HU-37)          · también: RETIRADO, ANULADA
```

- **Deuda (HU-30):** antes de aprobar se revisa si el apoderado tiene cuotas vencidas de **cualquiera** de sus hijos. Se calcula desde `pagos.cuota`; no se guarda.
- **Sección:** es un resultado de la matrícula. El frontend muestra **Por asignar** durante `SECCION_RESERVADA`, `EN_VALIDACION` y `PENDIENTE_PAGO`; muestra la sección registrada al llegar a `MATRICULADO` y la conserva como dato histórico si después pasa a `RETIRADO`.
- **Vacantes:** capacidad de la sección (`configuracion.seccion.capacidad_maxima`) menos las matrículas activas de esa sección. Se calculan; cambiar de sección (HU-26) mueve la matrícula y deja historial.

## Endpoints (apoderado)

Ver la tabla de [`backend/README.md`](../../backend/README.md): `GET /matricula/apoderado/estudiantes` y `GET /matricula/apoderado/estudiantes/{id}/estado`.

- La deuda (HU-30) se pide a Pagos con `ServicioEstadoCuenta.cuotasVencidas` (IEstadoCuenta): pensiones impagas después del vencimiento más los días de gracia, de **todos** los hijos del apoderado. Solo bloquea si el periodo tiene `bloquear_por_morosidad`.
- `pasoActual` del stepper: 1 SECCION_RESERVADA, 2 EN_VALIDACION, 3 PENDIENTE_PAGO, 4 MATRICULADO (0 sin matrícula).
- Si `periodo.abierto` es false, el frontend no muestra la opción de matricular (RF-59).

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

**Datos de prueba locales:** Luis Paredes (`U20260003`) tiene dos hijos:

- **Sofía:** MATRICULADO en 4.° A de 2026, con el código `MAT-2026-0001`.
- **Mateo:** `SECCION_RESERVADA` para 2027. El dato legado conserva 1.° A, pero el frontend lo oculta y muestra **Por asignar** hasta finalizar la matrícula.

El periodo 2026 está cerrado y el de 2027 habilitado. Ambos piden los 4 documentos de Figma.

## Observaciones encontradas

- **El año de la pantalla viene del periodo de matrícula:** el título muestra "Matrícula 2027" porque ese es el año escolar del periodo consultado, aunque sus fechas de apertura y cierre estén en 2026. En los datos de prueba, el periodo 2027 está habilitado del **15 de septiembre al 31 de octubre de 2026**. El frontend no calcula el año desde la fecha actual.
- **Sofía y Mateo tienen situaciones diferentes en 2027:** Mateo tiene una matrícula en proceso y su sección se muestra como **Por asignar**; Sofía está matriculada en 4.° A de 2026, pero no tiene una matrícula creada para 2027, por lo que también aparece pendiente de asignación para ese periodo.
- **La deuda se valida por familia (HU-30):** la pensión vencida de S/ 600 pertenece a Sofía. Al consultar a Mateo, la matrícula también queda bloqueada porque el periodo tiene `bloquear_por_morosidad`; la interfaz aclara que Mateo no tiene deuda propia y que el impedimento proviene de Sofía. Al consultar a Sofía, la misma deuda se muestra como propia.
- **HU-36 tiene un acceso temporal:** como el listado y detalle administrativo de HU-24 aún no existen, la tarjeta "Pago de matrícula" se prueba pasando `matriculaId` en la URL. Cuando HU-24 esté disponible, la misma tarjeta debe integrarse en su detalle.
- **Flujo visual administrativo disponible:** se añadieron las pantallas de apoyo A3, A5, B6 y B7 para poder navegar desde el periodo hasta cada estado de matrícula. El listado, los documentos y los datos personales son estáticos; no implementan HU-24, HU-25, HU-26, HU-28, HU-29, HU-31 ni HU-32. Sus acciones permanecen desactivadas. Solo las tarjetas ya entregadas de HU-36 y HU-37 consultan la API.
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
   - grado **1.° de primaria**, sección **Por asignar** y estado **En proceso**;
   - cierre **31 oct 2026** y primer paso del progreso.
4. Abre el selector de estudiante y elige **Sofía Paredes**.
5. Comprueba que la consulta cambia de hijo y que, para el periodo 2027, Sofía aparece **Pendiente de asignación** y sin sección confirmada.

**Resultado esperado:** el periodo, grado, sección, estado y progreso cambian según el hijo seleccionado y siempre provienen de la API.

### HU-30 · Validación de deuda familiar

1. Continúa en **Matrícula** con la sesión de `U20260003`.
2. Selecciona **Mateo Paredes**.
3. Comprueba la etiqueta **Deuda familiar** y el mensaje: Mateo no tiene deuda propia, pero su matrícula está bloqueada por la deuda de Sofía.
4. En la tarjeta **Deuda pendiente**, comprueba la pensión de septiembre de Sofía, vencida el 30 de septiembre de 2026, por **S/ 600.00**, y el total familiar.
5. Cambia a **Sofía Paredes**.
6. Comprueba la etiqueta **Deuda propia** y el mensaje que atribuye los S/ 600.00 a Sofía.

**Resultado esperado:** la misma deuda familiar bloquea el proceso de ambos hijos, pero la pantalla distingue claramente quién tiene la deuda.

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

- [x] Backend de HU-27 y HU-30 (estado de matrícula y deuda del apoderado).
- [x] Pantalla de HU-27 y tarjeta del Home: consulta periodo, grado, sección, estado y progreso por cada hijo.
- [x] Pantalla de HU-30: alerta contextual que distingue deuda propia y deuda de otro hijo, detalle de cuotas vencidas por estudiante y total de deuda familiar.
- [ ] HU-24: reservar la sección.
- [x] Código de matrícula: `MAT-AAAA-NNNN`, correlativo por año. La BD lo exige (V7).
- [x] Al matricular se cobra solo la cuota de matrícula del tarifario (S/ 450 en 2027); el S/ 850 de la pantalla del apoderado no se usa. La cuota de ingreso va aparte. Ver la [decisión](../decisiones/2026-10-19-yatako-integracion-bd.md).
- [ ] Al quedar matriculado, crear la cuenta del estudiante y enviar las credenciales al apoderado (V7).
- [ ] La información adicional de HU-32 (dirección, contacto de emergencia, alergias y seguro) ya existe en `estudiantes.*`. Matrícula la muestra y la actualiza; no la duplica.
