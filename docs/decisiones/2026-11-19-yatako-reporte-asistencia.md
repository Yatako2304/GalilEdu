# Reporte de inasistencias y tardanzas (HU-68)

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-68 (RF-146)
- **Estado:** terminado

## Qué hice

El botón "Reporte mensual" de la asistencia (AS-01) abre `/app/tutor/asistencia/reporte`. Ahí se ve el consolidado de la sección por mes o por periodo académico y se exporta a PDF. Ficha: [asistencia.md](../modulos/asistencia.md).

## Archivos principales

- `backend/src/main/java/com/galiledu/asistencia/aplicacion/ServicioReporteAsistencia.java` y `RangoReporte.java`
- `backend/src/main/java/com/galiledu/asistencia/infraestructura/ConsultasReporteAsistencia.java` y `ArchivoReporteAsistencia.java`
- `frontend/src/features/asistencia/components/reporte-asistencia.tsx`

## Decisiones y por qué

- **Decisión:** el reporte se pide **por mes o por periodo** (bimestre o trimestre, según el régimen del año).
  **Por qué:** RF-146, la HU-68 y Figma (el botón "Reporte mensual") dicen mensual, pero el plan de pruebas pide "por bimestre". Acordado con el equipo el 19/11.
- **Decisión:** Figma no tiene la pantalla. Se armó con lo acordado: pestañas "Por mes" / "Por bimestre", el selector del mes o periodo, una tabla por apellidos con Faltas, Justificadas, Tardanzas y Justificadas, el total de la sección y "Exportar PDF". Usa los componentes y el estilo de AS-01.
- **Decisión:** "Faltas" y "Tardanzas" incluyen las justificadas, y la columna "Justificadas" dice cuántas lo son.
  **Por qué:** RF-146 pide los totales "según el tipo de registro". Así el total de faltas no cambia al justificar, y las justificadas se ven aparte. El PDF lo aclara al pie.
- **Decisión:** solo cuentan los días con asistencia registrada, y el reporte dice cuántos son. Un día sin registro no cuenta como falta.
- **Decisión:** solo se ofrecen los meses y periodos del año escolar que ya empezaron. Sin parámetros, se muestra el mes actual. Al pasar a "Por bimestre" se abre el periodo más reciente.
- **Decisión:** los meses se nombran con el locale `es-PE` ("Setiembre"), como el "set." que usa Figma.
- **Decisión:** el PDF se genera en el backend con PDFBox (A4 vertical), con el mismo estilo que el reporte de notas y la constancia de pago. Repite el encabezado de la tabla en cada página y pone el total al final.
