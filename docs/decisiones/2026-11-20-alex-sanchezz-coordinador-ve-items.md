# El Coordinador ve todo el año y los ítems de cada estudiante

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-20
- **HU / tarea:** HU-63 (detalle «Ver estudiantes»), reutiliza HU-69
- **Estado:** terminado (pendiente de confirmar con el PO: ninguna HU le da esto al Coordinador)

## Qué hice

En el detalle de una sección del reporte por curso y grado («Ver estudiantes»), el Coordinador ahora tiene:

1. **Todo el año:** por competencia, el nivel de cada periodo y la nota final de cada estudiante (por ejemplo, A · B · C · A → A), como un registro consolidado. Antes veía un solo periodo a la vez, y la vista completa solo estaba en la libreta en PDF.
2. **Ítems:** las notas del estudiante con los ítems que calificó cada docente, por periodo, y la nota oficial de cada competencia. Es solo lectura.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/web/ReporteNotasController.java`: `GET /calificaciones/reportes/matriculas/{matriculaId}/notas`, solo para el Coordinador.
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioNotasApoderado.java`: `notasMatricula`.
- `frontend/src/features/calificaciones/components/detalle-seccion.tsx` y `notas-estudiante.tsx`.

## Decisiones y por qué

- **Decisión:** el Coordinador puede ver (no modificar) los ítems que califica el docente.
  **Por qué:** supervisar la evaluación es parte de su rol. Sin ver los ítems no puede saber por qué una competencia queda en C o pendiente, ni qué le falta registrar al docente.
  **Alternativas descartadas:** darle la planilla del docente (ítems × estudiantes) por curso y sección. Sirve para revisar cómo califica un docente, pero obliga a separar la lógica de la planilla del permiso del docente. Queda como siguiente paso.
- **Decisión:** la vista por estudiante reutiliza la consulta del apoderado (HU-69), con un endpoint propio del Coordinador que no exige vínculo familiar.
  **Por qué:** es la misma información (cursos, ítems y nota oficial), ya probada, y el apoderado y el Coordinador la leen igual. Se exportaron sus bloques (`CursosDelPeriodo`, `NotasPorCompetencia`, `Leyenda`) dentro del mismo feature.
- **Decisión:** en «Todo el año» cada celda muestra solo la letra (con el número y el detalle en el título) y «Pend.» si falta registrar algo.
  **Por qué:** con 4 periodos y la final por competencia hay muchas columnas, y así caben.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO que el Coordinador puede ver los ítems.
- [ ] Planilla del docente en solo lectura por curso y sección (siguiente paso, si se quiere).

## Notas para quien siga

- `NotasPorCompetencia` acepta `paraCoordinador`: el aviso de pesos incompletos le pide ajustarlos en el plan de evaluación, en vez del texto para el apoderado.
