# Calificar seleccionando solo el ítem

- **Autor:** josezavaleta
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-59 · simplificar el registro de calificaciones
- **Estado:** terminado

## Qué hice

Retiré el selector de competencia de **Calificar alumnos** por solicitud expresa de josezavaleta. El selector de ítem muestra todos los ítems del curso, sección y periodo, independientemente de las competencias asociadas.

## Archivos principales

- `frontend/src/features/calificaciones/components/calificar-alumnos.tsx`
- `frontend/src/features/calificaciones/components/calificaciones-docente.tsx`
- `frontend/src/features/calificaciones/components/__tests__/calificaciones-docente.test.tsx`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

- **Decisión:** seleccionar únicamente el ítem al registrar las notas.
  **Por qué:** la calificación pertenece al estudiante y al ítem; sus competencias reutilizan esa nota con los pesos configurados. Elegir una competencia antes de calificar agrega un paso innecesario. Este ajuste sustituye el selector de competencia del flujo original por petición del usuario.
  **Alternativas descartadas:** mantener el filtro por competencia o registrar notas diferentes para cada asociación.

## Validación

Las 12 pruebas de la pantalla pasan, incluida la selección entre ítems de distintas competencias sin duplicar los compartidos. La comprobación de tipos de TypeScript también pasó.

## Notas para quien siga

La API ya registra las notas por ítem. Este cambio es de interfaz y documentación; no requiere cambios de esquema ni del archivo de datos de prueba. La configuración de competencias y pesos y la selección de competencias del consolidado conservan sus funciones.
