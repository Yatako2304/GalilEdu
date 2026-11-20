# El reporte del docente: un periodo o el año, no los dos mezclados

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-62 (consolidado de notas) y la nota oficial por competencia
- **Estado:** terminado. Reemplaza, de la [nota oficial por competencia](2026-11-16-alex-sanchezz-nota-oficial-competencia.md), solo la parte de «la nota final de la competencia va al final del PDF del consolidado»

## Qué hice

- **Consolidado de un periodo:** su PDF termina con la nota oficial de cada competencia **en ese periodo**. Antes terminaba con todos los periodos y la nota final del año.
- **«Promedio final» → «Promedio del periodo»:** así se llama ahora la columna del consolidado y su interruptor. La palabra «final» queda solo para el año.
- **«Todo el año»:** es una opción nueva del selector «Periodo lectivo». Genera un **resumen del año**, en pantalla y en PDF. Por competencia muestra:
  - el aporte de este curso en cada periodo y su **aporte final**;
  - la nota oficial de la competencia en cada periodo y la final.
- **Fecha y hora:** los PDF del docente muestran cuándo se generaron.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioNotasCompetencia.java` (`reporteNotasPeriodo`, `resumenAnual`, `pdfResumenAnual`)
- `backend/src/main/java/com/galiledu/academico/web/CalificacionController.java` (`/resumen-anual` y `/resumen-anual/pdf`)
- `frontend/src/features/calificaciones/components/reporte-calificaciones.tsx`

## Decisiones y por qué

- **Decisión:** cada reporte tiene un solo alcance: un periodo o todo el año.
  **Por qué:** el PDF de un periodo traía la nota del año. Esa nota incluye periodos que todavía no terminan y sale «Parcial», así que el docente recibía datos que no pidió. Lo señaló alex-sanchezz.
  **Alternativas descartadas:**
  - Quitar del todo la nota de la competencia del PDF del docente: en el periodo es la que va a la libreta, y sirve que el docente la vea.
  - Dejar la nota del año al final de cada periodo, por lo dicho arriba.
- **Decisión:** el resumen del año muestra el **aporte final** del curso con ese nombre, no «nota final del curso». Se marca como referencial.
  **Por qué:** el curso no tiene nota final propia (decisión del 16/11). El aporte final es la misma cifra que da el reporte HU-63 al elegir el curso y «Nota final», así que el docente y la coordinación ven el mismo número.
- **Decisión:** el resumen del año aclara, en pantalla y en el PDF, que la nota de la competencia **no** es el promedio de los aportes finales.
  **Por qué:** la nota de la competencia combina los cursos en cada periodo y después junta los periodos. Si a un curso le falta un periodo, con «último periodo» o en la escala de letras, el resultado no coincide con hacer la cuenta usando los aportes finales. Sin la aclaración, el docente intentaría verificarla a mano.
- **Decisión:** el aporte final es «Parcial» si al curso le falta algún periodo; con `ULTIMO_PERIODO`, solo si le falta el último.
  **Por qué:** es el mismo criterio de la nota final de la competencia.
- **Decisión:** los PDF del docente llevan la fecha y hora de generación.
  **Por qué:** la nota de la competencia cambia mientras otros cursos registren notas, y un PDF descargado se queda con el valor de ese momento.

## Cambios en la base de datos

Ninguno.

## Pendientes / problemas conocidos

- [ ] No hay frame en Figma para «Todo el año»: usa la misma tabla agrupada del consolidado. Confirmar con diseño.

## Notas para quien siga

- En el resumen del año, una competencia tiene **dos** grupos de columnas con el mismo `competenciaId`. El frontend agrupa por `competenciaId` y `competencia` (título del grupo); el PDF, por el título.
- El encabezado «Promedio del periodo» se parte en dos líneas en el PDF. Las pruebas buscan «Promedio del».
