# Horario de clases para el apoderado (HU-51)

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-51 · Consulta de horario de clases de sección para el Apoderado (iteración 2, Must)
- **Estado:** terminado

## Qué hice

El apoderado elige a uno de sus hijos matriculados y ve el horario de su sección: año académico, grado y sección, tutor(a) con celular y correo, y la matriz de lunes a viernes con bloques, recreos, curso, docente y aula. Puede descargarlo con «Descargar Horario».

## Archivos principales

- `backend/.../horarios/web/HorarioApoderadoController.java`: `GET /horarios/apoderado/estudiantes` y `GET /horarios/apoderado/estudiantes/{id}/horario?anio=`.
- `backend/.../horarios/aplicacion/ServicioHorarioApoderado.java`, `DtosHorarioApoderado.java` y el puerto `HijosDelApoderado` (`infraestructura/HijosDelApoderadoSql.java`).
- `frontend/src/features/horarios/components/horario-apoderado.tsx`, `api/horario-apoderado.ts` y `lib/horario-apoderado.ts`; ruta `app/routes/app/apoderado/horarios.tsx`.

## Decisiones y por qué

- **Decisión:** endpoints nuevos para el rol Apoderado en un controlador aparte, que reutilizan `ServicioHorarios.horarioDeSeccion` sin cambiarlo.
  **Por qué:** el módulo `horarios` es de la pareja de horarios (HU-45). Así no se toca nada del Coordinador ni sus reglas, y la matriz se arma igual para los dos.
  **Alternativas descartadas:** abrir los endpoints del Coordinador al Apoderado (verían versión, cruces e ids internos, y cualquier sección).
- **Decisión:** el apoderado solo ve las secciones de sus hijos con matrícula en estado MATRICULADO (el criterio de aceptación dice «hijos matriculados»); para otro estudiante o un año sin matrícula, 404.
  **Por qué:** un horario es de una sección; la matrícula es la que dice en qué sección está el hijo.
- **Decisión:** no hay estado «publicado»: el horario se ve en cuanto el Coordinador lo guarda. Mientras no exista, la pantalla dice «El horario de esta sección aún no está disponible.» y «Descargar Horario» queda deshabilitado.
  **Por qué:** HU-45 no tiene un paso de publicar. Si más adelante se agrega, basta con que `ServicioHorarioApoderado` lo revise.
- **Decisión:** «Descargar Horario» abre una página imprimible en una pestaña nueva y el diálogo de impresión, donde el navegador lo guarda como PDF.
  **Por qué:** el proyecto no tiene una librería de PDF y agregar una dependencia afecta a todos. Si el PO pide un PDF generado por el sistema, se puede cambiar solo esa función (`descargarHorario`).
- **Decisión:** la pantalla junta las dos versiones de Figma: la de «Apoderado (real)» (selector del hijo, «Descargar Horario» y matriz) y la del frame HU-51 («Datos del horario» con año y tutor).
  **Por qué:** el documento de HUs de la pareja pide los dos.
- **Decisión:** la vista del Estudiante (HU-50, Could) no se tocó: sigue con su componente `MiHorario` y datos simulados.
  **Por qué:** HU-50 no se hará por ahora; el apoderado tiene su propio componente y no depende de ella.
- **Decisión:** ícono `FileDown` para `picture_as_pdf` (agregado a la tabla de equivalencias).

## Pendientes / problemas conocidos

- [ ] Subgrupos (HU-47, iteración 3): cuando existan, cada bloque podrá traer más de un curso; la matriz ya sabe dibujarlos.
- [ ] Si el PO pide un estado «publicado» o un PDF generado por el sistema, ver las decisiones de arriba.

## Notas para quien siga

- Datos de prueba: Luis Paredes (U20260003) tiene a Sofía matriculada en 4.° de secundaria A de 2026 (sin horario guardado: sale «aún no está disponible»). Para verla con horario, el Coordinador (U20260005) debe armar el horario de esa sección.
