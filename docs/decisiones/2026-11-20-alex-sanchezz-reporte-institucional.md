# Reporte institucional de notas como reporte de logros

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-20
- **HU / tarea:** HU-64
- **Estado:** terminado (pendiente de revisar con el PO, porque HU-64 no tiene frame en Figma)

## Qué hice

El reporte institucional (pestaña «Institucional» de Calificaciones y su PDF) ahora se lee como un reporte de logros de aprendizaje. Muestra el **porcentaje** de calificaciones en cada nivel (AD, A, B, C) con la cantidad entre paréntesis, el porcentaje con «A o AD» y una barra de distribución por fila. Se quitaron las columnas «Pendientes» y «Sin notas», que se confundían entre sí y mezclaban casos distintos.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioReportesNotas.java` y `DtosReportesNotas.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/ArchivoReporteCalificaciones.java`: cada tabla del PDF puede tener su propia primera columna (la de áreas decía «Grado y sección»).
- `frontend/src/features/calificaciones/components/reportes-notas.tsx`
- `frontend/src/components/ui/barra-distribucion/` (componente nuevo)

## Decisiones y por qué

- **Decisión:** porcentajes por nivel sobre las calificaciones completas, con la cantidad al lado.
  **Por qué:** así se presentan los resultados por nivel de logro en los reportes escolares. Con solo cantidades no se podían comparar secciones de distinto tamaño, y se mezclaban «notas» con «estudiantes» (6 estudiantes, 22 notas).
  **Alternativas descartadas:** contar estudiantes en vez de calificaciones. Un estudiante tiene una nota por competencia, así que habría que elegir una nota «resumen» por estudiante, y eso no existe en la evaluación por competencias.
- **Decisión:** lo que no se cuenta se explica aparte, cada caso con su texto:
  - **Calificaciones por completar** (falta registrar algún ítem): «+2 por completar» en la fila y un aviso con las secciones donde faltan.
  - **Competencias que no se pueden calcular** (los pesos de sus cursos no suman 100 %): un aviso con el grado y la competencia. Antes se escondían dentro de «Sin notas».
  - **Áreas aún sin calificaciones**: no salen en la tabla y se nombran debajo.
  **Por qué:** «Pendientes» y «Sin notas» parecían decir lo mismo. «Sin notas» mezclaba lo normal (todavía no se evalúa) con un error de configuración (pesos incompletos), y agregaba ceros que no ayudaban a leer el reporte.
- **Decisión:** en la tabla por grado, el total del grado va arriba y sus secciones debajo (con sangría); «Todo el colegio» al final.
  **Por qué:** se lee como un reporte agrupado. En el PDF el orden no cambió.
- **Decisión:** la barra usa los colores de la escala de Figma CI-23 (AD turquesa, A verde, B ámbar, C rojo) y es un componente de `components/ui`, porque Figma no lo tiene.

## Pendientes / problemas conocidos

- [ ] Revisar el diseño con el PO; HU-64 no tiene frame en Figma.
- [ ] Los porcentajes se redondean por separado, así que una fila puede sumar 99 % o 101 %.

## Notas para quien siga

- El contrato de `GET /calificaciones/reportes/institucional` cambió: `Distribucion` ya no trae `pendientes` ni `sinNotas`. Ahora trae `calificaciones` y `porCompletar`, y el reporte trae `sinCalcular`. Solo lo usa la pestaña Institucional.
