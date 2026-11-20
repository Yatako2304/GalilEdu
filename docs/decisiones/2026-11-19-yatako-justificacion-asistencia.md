# Justificación de faltas y tardanzas (HU-67)

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-67 (RF-145)
- **Estado:** terminado

## Qué hice

El tutor o cotutor justifica una falta o tardanza ya registrada desde la columna "Justificación" de AS-05, en el modal de Figma "Justificar inasistencia / tardanza". El botón de la fecha abre un calendario para ir a días pasados. Ficha: [asistencia.md](../modulos/asistencia.md).

## Archivos principales

- `backend/src/main/resources/db/migration/V26__asistencia_justificacion.sql`
- `backend/src/main/java/com/galiledu/asistencia/dominio/Justificacion.java` y `ServicioAsistencia` (`justificar`, `sustento`)
- `frontend/src/features/asistencia/components/dialogo-justificar.tsx`, `tabla-asistencia.tsx` y `asistencia-seccion.tsx` (`SelectorFecha`)

## Decisiones y por qué

Acordadas con el equipo el 19/11 (las tres marcadas como propuesta se aprobaron ese mismo día):

- **Decisión:** los motivos son Cita médica, Enfermedad, Trámite oficial y Otro. Figma solo muestra "Cita Médica"; se quitó "Duelo familiar" de la propuesta inicial.
- **Decisión:** el sustento es opcional: PDF, JPG o PNG de hasta 5 MB, guardado en la BD (BYTEA), como las evidencias de Calificaciones y los documentos de Matrícula.
  **Por qué:** está en Figma pero no en el RF-145. El ejemplo de Figma es un `.mp4`; se descartó el video.
- **Decisión:** se pueden justificar días pasados, eligiéndolos con el botón de la fecha. El modal de Figma muestra una fecha anterior a "hoy", pero Figma no tenía cómo llegar a ella.
- **Decisión:** se conserva el tipo original (FALTA o TARDANZA) y la justificación queda en su propia tabla.
  **Por qué:** RF-145 habla de pasar el estado a "Justificada", pero el reporte (HU-68, RF-146) cuenta "según el tipo de registro". Guardar el tipo y la marca de justificada permite contar las dos cosas.
- **Decisión (propuesta aprobada):** con el motivo "Otro" el detalle es obligatorio. Con los demás motivos es opcional.
  **Por qué:** "Otro" sin explicación no es un "motivo formal" (RF-145). Si el equipo no lo quiere, basta con quitar la regla de `Justificacion.de`, la restricción `ck_justificacion_otro_detalle` (con una migración nueva) y la validación del modal.
- **Decisión (propuesta aprobada):** una falta o tardanza justificada ya no se corrige (409 "No puedes corregir una falta o tardanza que ya está justificada.").
  **Por qué:** si se corrigiera a "Presente", la justificación quedaría colgando, y borrarla sin avisar perdería el sustento. Se prefirió rechazarlo de forma explícita.
- **Decisión (propuesta aprobada):** una justificación no se edita ni se anula, porque no lo piden la HU ni el RF. Queda anotado como pendiente.
- **Decisión:** Figma no dibuja una fila ya justificada. Se muestra la etiqueta "Justificada", con el motivo y el detalle en un tooltip, y un botón para descargar el sustento si lo hay.

## Pendientes / problemas conocidos

- [ ] Editar o anular una justificación, si el equipo lo pide.

## Notas para quien siga

- En las pruebas del frontend, MSW no lee el multipart bajo jsdom: se espía `api.post`, como en `plantillas.test.ts` y `detalle-matricula.test.tsx`.
