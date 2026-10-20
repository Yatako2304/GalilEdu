# Matrícula

- **HU:** HU-24, 27, 30 (iteración 1) · HU-28, 29, 31, 32 (2) · HU-25, 26, 33, 71, 72 (3)
- **Estado:** ⏳ por empezar: las tablas y los datos de prueba ya están; faltan backend y frontend
- **Última actualización:** 2026-10-19

## Alcance

El Personal administrativo reserva una sección para cada estudiante, nuevo o continuador. El apoderado sube los documentos firmados. El colegio los valida, se genera la cuota de matrícula y, al pagarla, el estudiante queda **matriculado** con un código (`MAT-2027-0044`) y su cronograma de pensiones.

La configuración del periodo (fechas, reglas y vacantes) está en [configuracion.md](configuracion.md).

## Flujo (Figma › "Flujo de matrícula · Personal administrativo" y Apoderado HU-28 a HU-32)

```
SECCION_RESERVADA ──(apoderado sube documentos)──► EN_VALIDACION ──(colegio aprueba y no hay deuda)──►
PENDIENTE_PAGO (se crea el compromiso con la cuota de matrícula, HU-36) ──(pago confirmado)──►
MATRICULADO (código MAT-…, cronograma de pensiones, HU-37)          · también: RETIRADO, ANULADA
```

- **Deuda (HU-30):** antes de aprobar se revisa si el apoderado tiene cuotas vencidas de **cualquiera** de sus hijos. Se calcula desde `pagos.cuota`; no se guarda.
- **Vacantes:** capacidad de la sección (`configuracion.seccion.capacidad_maxima`) menos las matrículas activas de esa sección. Se calculan; cambiar de sección (HU-26) mueve la matrícula y deja historial.

## Pantallas (Figma)

| Pantalla | Frame en Figma |
| --- | --- |
| Matrículas del periodo | Personal Administrativo › "A3 · Matrícula 2027 › Matrículas del periodo (HU-24, HU-71)" |
| Detalle / validar | "A5 · Detalle — Sección reservada", "A6 · Modal Completar matrícula", modal "¿Aprobar documentación?" |
| Pendiente de pago / Matriculado | "B6 · Detalle — Pendiente de pago (HU-36, HU-31)", "B7 · Detalle — Matriculado (HU-31, HU-37)" |
| Cambiar sección | modales "Cambiar sección" y "¿Cambiar sección?" |
| Apoderado | Apoderado › "HU-28" a "HU-32" (descarga y carga de documentos, deuda, pago, confirmación) |

## Base de datos

- **Esquema:** `matricula` · **Migraciones:** `V4` (periodo de matrícula) y `V5__matricula_y_pagos.sql`

| Tabla | Para qué |
| --- | --- |
| `matricula.periodo_matricula` | Fechas y reglas del periodo (V4). |
| `matricula.documento_requerido` | Documentos que pide el periodo, con plantilla opcional para descargar (HU-21/22, HU-28). |
| `matricula.matricula` | Una por estudiante y año. Guarda grado, sección, NUEVO/CONTINUADOR, estado, código, si fue extemporánea y su motivo (HU-25), la fecha de confirmación (RF-68) y el retiro. |
| `matricula.documento_matricula` | Archivo firmado de cada documento requerido: PDF, JPG o PNG de hasta 10 MB. Estado EN_REVISION, APROBADO u OBSERVADO; una observación es obligatoria si está OBSERVADO. |
| `matricula.cambio_seccion` | Historial de cambios de sección (HU-26). |
| `matricula.correlativo_matricula` | Último número de código por año. |

Desde una matrícula se llega a: el estudiante (`estudiantes.estudiante`) y su apoderado (`estudiantes.vinculo_familiar` con `es_responsable_matricula`), el año, el grado y la sección (`configuracion.*`), y el compromiso de pago (`pagos.compromiso_pago.matricula_id`).

**Datos de prueba:** Luis Paredes (`U20260003`) tiene dos hijos:
- **Sofía:** MATRICULADO en 4.° A de 2026, con el código `MAT-2026-0001`.
- **Mateo:** SECCION_RESERVADA en 1.° A de 2027.

El periodo 2026 está cerrado y el de 2027 habilitado. Ambos piden los 4 documentos de Figma.

## Pendientes

- [ ] Backend y frontend.
- [x] Código de matrícula: `MAT-AAAA-NNNN`, correlativo por año. La BD lo exige (V7).
- [x] Al matricular se cobra solo la cuota de matrícula del tarifario (S/ 450 en 2027); el S/ 850 de la pantalla del apoderado no se usa. La cuota de ingreso va aparte. Ver la [decisión](../decisiones/2026-10-19-yatako-integracion-bd.md).
- [ ] Al quedar matriculado, crear la cuenta del estudiante y enviar las credenciales al apoderado (V7).
- [ ] La información adicional de HU-32 (dirección, contacto de emergencia, alergias y seguro) ya existe en `estudiantes.*`. Matrícula la muestra y la actualiza; no la duplica.
