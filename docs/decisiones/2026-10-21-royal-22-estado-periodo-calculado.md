# El estado del periodo académico se calcula con las fechas (HU-74)

- **Autor:** royal-22
- **Fecha:** 2026-10-21
- **HU / tarea:** HU-74 (E-40, RF-134)
- **Estado:** en progreso

## Qué hice

Eliminé la columna `estado` de `configuracion.periodo_academico` (migración `V8__configuracion_periodo_sin_estado.sql`) y la quité del seed. El estado del periodo (PLANIFICADO, EN_CURSO, CERRADO) ahora lo da `PeriodoAcademico.estadoEn(hoy, estadoAnio)`, y si los docentes aún pueden registrar notas lo da `registroNotasAbierto(hoy, estadoAnio)`. El enum `EstadoPeriodo` se conserva para ese valor calculado.

## Archivos principales

- `backend/src/main/resources/db/migration/V8__configuracion_periodo_sin_estado.sql`
- `backend/src/main/java/com/galiledu/configuracion/dominio/PeriodoAcademico.java`
- `backend/src/main/java/com/galiledu/compartido/RelojConfig.java` (el "hoy" en `America/Lima`)

## Decisiones y por qué

- **Decisión:** el estado del periodo no se guarda; se calcula con el estado del año y las fechas. En un año PLANIFICADO todos sus periodos son PLANIFICADO; en un año CERRADO, todos son CERRADO; solo en un año ACTIVO se decide por fechas (PLANIFICADO antes del inicio, EN_CURSO entre inicio y fin, CERRADO después del fin). `periodoEnCurso` es vacío si el año no está ACTIVO.
  **Por qué:** nada actualizaría la columna a tiempo (no hay un proceso diario), y un estado guardado se desfasaría de las fechas apenas pase la medianoche. Un año planificado no debe mostrar periodos en curso aunque sus fechas incluyan hoy.
  **Alternativas descartadas:** botones o endpoints para abrir y cerrar cada periodo; un proceso programado que actualice la columna; decidir solo por fechas sin mirar el estado del año.
- **Decisión:** el registro de notas depende de la fecha límite del periodo, no de su estado. Está abierto solo si el año está ACTIVO y `fechaInicio <= hoy <= fechaLimiteNotas`; la fecha límite puede caer dentro del periodo siguiente. No se abre para periodos que no han empezado ni para años planificados o cerrados. Para ampliar el plazo se edita el periodo, incluso uno ya terminado.
  **Por qué:** RF-134 pide registrar una fecha límite de notas por periodo, y así el cierre del plazo no depende de que alguien cierre el periodo a mano.
  **Alternativas descartadas:** cerrar el registro al terminar el periodo; permitir notas desde antes de que empiece el periodo.
- **Decisión:** "hoy" sale de un bean `java.time.Clock` en `America/Lima` (`RelojConfig`); el dominio recibe `hoy` como parámetro. Las pruebas de integración fijan el reloj con `RelojFijoConfig`.
  **Por qué:** así las reglas no dependen de la fecha real y se prueban con fechas fijas.

## Pendientes / problemas conocidos

- [x] Activar y cerrar año ya tienen endpoint; el botón Activar aún no está en Figma (CI-06) y "Cerrar año" solo aparece en CI-12.

## Notas para quien siga

- Si otro módulo (Calificaciones) necesita saber si un periodo admite notas, debe preguntar al servicio de configuración; no leer `periodo_academico` por SQL ni suponer un estado guardado.
- `R__datos_prueba.sql` ya no inserta `estado` en `periodo_academico`. En una BD local ya migrada, Flyway aplica V8 primero y luego vuelve a ejecutar el seed.
