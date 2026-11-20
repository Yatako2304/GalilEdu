# El reporte institucional (HU-64) es del Personal administrativo, no del Administrador

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-20
- **HU / tarea:** HU-64
- **Estado:** terminado. Corrige el actor de HU-64 en `2026-11-19-alex-sanchezz-modificacion-reportes-apoderado.md`.

## Qué hice

El reporte institucional de notas pasó del **Administrador** al **Personal administrativo**, que es el actor que dice la HU-64. El Coordinador lo sigue teniendo como pestaña «Institucional» de sus Calificaciones.

- El Administrador ya no tiene **Calificaciones**: sale de su menú, se quita su ruta (`/app/admin/calificaciones`) y el backend le responde 403.
- El Personal administrativo tiene **Académico y operaciones › Calificaciones** (`/app/administrativo/calificaciones`), solo con el reporte institucional y su PDF.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/web/ReporteNotasController.java`: `hasAnyRole('COORDINADOR', 'PERSONAL_ADMINISTRATIVO')` para `/anios` y `/institucional`. Lo demás sigue siendo solo del Coordinador.
- `frontend/src/config/paths.ts`, `navigation.ts`, `app/router.tsx` y `app/routes/app/administrativo/calificaciones.tsx` (antes `admin/calificaciones.tsx`).

## Decisiones y por qué

- **Decisión:** HU-64 es del Personal administrativo.
  **Por qué:** así lo dice la HU. La entrada del 19/11 se lo había dado al Administrador por error. El Administrador gestiona usuarios y accesos, no la parte académica.
- **Decisión:** el Coordinador conserva la pestaña «Institucional».
  **Por qué:** no se pidió quitársela, y la usa junto al reporte por curso y grado (HU-63).
- **Decisión:** el Personal administrativo ve solo el reporte institucional, no el reporte por curso y grado ni el detalle por estudiante.
  **Por qué:** esos son de HU-63 y siguen siendo solo del Coordinador.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO si el Coordinador también debe tener HU-64; hoy lo tiene.
