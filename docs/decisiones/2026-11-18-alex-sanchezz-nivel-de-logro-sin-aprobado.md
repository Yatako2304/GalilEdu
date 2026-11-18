# Nivel de logro en lugar de «Aprobado / Desaprobado»

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-18
- **HU / tarea:** HU-62 (consolidado de notas) y la nota oficial por competencia
- **Estado:** terminado; reemplaza el estado «Aprobado / Desaprobado» del consolidado

## Qué hice

Quité «Aprobado», «Desaprobado» y la nota aprobatoria de las calificaciones. Ahora cada resultado muestra su **nivel de logro** con su significado:

| Nivel | Significado | Color |
| --- | --- | --- |
| AD | Logro destacado | Verde |
| A | Logro esperado | Verde |
| B | En proceso | Ámbar |
| C | En inicio | Rojo |
| — | Pendiente (sin notas) | Gris |

El cambio aplica en todos los lugares donde aparecía el estado:

- **«Promedio final» del consolidado por curso:** en vigesimal se muestra, por ejemplo, «A · Logro esperado».
- **Nota de la competencia:** cada periodo y la nota final muestran su nivel y su significado. El campo de la API pasa de `estado` a `logro`.
- **PDF del consolidado:** usa los mismos textos y colores.

Esta entrada reemplaza el estado «Aprobado / Desaprobado / Pendiente» que se introdujo en el consolidado (`fdfa4e2`, 05/11) y que [la nota oficial por competencia](2026-11-16-alex-sanchezz-nota-oficial-competencia.md) había extendido.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/dominio/CalculoNotas.java` (`logro`)
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java` (`logroResultado`)
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioNotasCompetencia.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/ArchivoReporteCalificaciones.java` (colores del PDF)
- `frontend/src/features/calificaciones/components/logro.ts` (colores en pantalla)

## Decisiones y por qué

- **Decisión:** informar cada competencia con su nivel de logro y su significado, sin «aprobado» ni «desaprobado».
  **Por qué:** así se evalúa por competencias en el Perú (RVM 094-2020-MINEDU). Cada competencia se califica con AD, A, B o C; aprobar o no es la **situación final del año** del estudiante (promovido, requiere recuperación o permanece en el grado), y se decide con todas sus competencias, no con una. Lo señaló alex-sanchezz. Ninguna entrada anterior justificaba el estado.
  **Alternativas descartadas:** mantener «Aprobado» con A o AD, porque presenta B («En proceso») como un fracaso cuando significa que el estudiante está cerca de lo esperado.
- **Decisión:** tomar el significado del enum `NivelLogro`, que coincide con `academico.tabla_conversion_nivel.significado`.
  **Por qué:** es un texto fijo de la norma, igual en todas las versiones de la tabla.
- **Decisión:** se conserva el redondeo al entero (0,5 sube) para elegir la letra; ya no hay una regla aparte para el estado.

## Cambios en la base de datos

Ninguno. Se eliminó la consulta `RepositorioCalificaciones.notaAprobatoria`; los rangos se leen con `rangosConversion`.

## Pendientes / problemas conocidos

- [ ] La situación final del año (promovido, requiere recuperación, permanece) pertenece a un cierre de año que aún no existe.
- [ ] Avisar a quien hizo la HU-62: el texto bajo el «Promedio final» cambió.

## Notas para quien siga

Los colores se deciden por el significado («Logro destacado», «En proceso»…), en `colorLogro` del frontend y en `ArchivoReporteCalificaciones.colorLogro`. Si cambian los textos, cambian los colores.
