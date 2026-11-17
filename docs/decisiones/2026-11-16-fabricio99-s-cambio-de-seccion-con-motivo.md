# HU-26: cambio de sección con motivo e historial visible

- **Autor:** fabricio99-s
- **Fecha:** 2026-11-16
- **HU / tarea:** HU-26 (RF-56, RF-57)
- **Estado:** terminado
- **Reemplaza en parte a:** la decisión de HU-24 de registrar el cambio de sección "sin motivo".

## Qué hice

El cambio de sección ya existía desde HU-24 (modal en el detalle de la matrícula, mismo grado, solo con vacante, cupos calculados). Para cerrar HU-26 agregué: el **motivo obligatorio** del cambio (RF-56: reorganización por cambio de año escolar o solicitud del apoderado, con observación opcional), el **historial visible** en el detalle de la matrícula y el endpoint que lo alimenta. Los cupos (RF-57) siguen calculándose contando matrículas por sección, así que el origen y el destino se ajustan solos e inmediatamente.

## Archivos principales

- `backend/.../matricula/aplicacion/MotivoCambioSeccion.java`, `ServicioMatriculaAdministrativa.java`, `DtosMatricula.java`
- `backend/.../matricula/web/MatriculaDetalleController.java` (`PUT .../seccion` con motivo y `GET .../cambios-seccion`)
- `backend/.../matricula/infraestructura/ConsultasMatricula.java`, `EscriturasMatricula.java`
- `frontend/src/features/matricula/components/dialogo-cambiar-seccion.tsx`, `historial-cambios-seccion.tsx`, `detalle-matricula.tsx`
- Pruebas: `MatriculaDetalleIT` y `detalle-matricula.test.tsx`

## Decisiones y por qué

- **Decisión:** el motivo se pide solo al cambiar la sección desde el detalle de una matrícula, no en "Nueva matrícula".
  **Por qué:** al crear la matrícula todavía no hay sección de origen ni cambio que registrar; solo se elige la sección.
- **Decisión:** el motivo se guarda como texto legible en `cambio_seccion.motivo` ("Solicitud del apoderado" o "Solicitud del apoderado: observación"), sin migración nueva.
  **Por qué:** la columna ya existía (500 caracteres) y los dos motivos son fijos; el servicio separa motivo y observación al leerlos.
  **Alternativas descartadas:** una columna `motivo_tipo` (más estricta, pero exige migración para dos valores).
- **Decisión:** se mantiene que se pueda cambiar de sección con el periodo abierto aunque la matrícula aún no esté `MATRICULADO`; con el periodo cerrado, solo a quienes ya lo están.
  **Por qué:** el flujo de A5/A6 (reserva y validación) lo necesita. RF-56 dice "ya matriculado", pero el producto aceptó conservar la regla actual.
- **Decisión:** los cambios anteriores a esta entrega quedan con `motivo = null`; el historial los muestra como "Sin motivo registrado".

## Pendientes / problemas conocidos

- [ ] El apoderado no ve el historial (la sección no se le muestra hasta que está matriculado).
- [ ] No hay notificación al apoderado cuando se cambia la sección de su hijo.

## Notas para quien siga

- El horario y el tutor del estudiante no se guardan en la matrícula: salen de la sección, así que cambian solos al mover la matrícula.
