# La nota oficial existe solo cuando está completa

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-60, HU-62, HU-63, HU-64, HU-69 y la nota oficial por competencia
- **Estado:** terminado. Reemplaza la regla de «Parcial» de la [nota oficial por competencia](2026-11-16-alex-sanchezz-nota-oficial-competencia.md) («un curso sin notas o un ítem sin calificar: se calcula con lo que hay y se marca Parcial»)

## Qué hice

- **«Pendiente» en lugar de «Parcial».** Si a un estudiante le falta registrar un ítem que cuenta, su nota oficial queda **«Pendiente (3 de 4 ítems)»**, sin número ni letra. Ya no se muestra el promedio de lo que haya con la marca «Parcial».
- **«No evaluado».** Cada celda (ítem × estudiante) se resuelve con una nota o con «No evaluado», que lleva motivo.
- **Registro fuera de plazo (HU-60).** Pasada la fecha límite, el docente puede registrar con motivo una nota que falta. Antes solo se podían modificar notas que ya existían.
- **Reportes.** HU-63 y HU-64 cuentan las notas pendientes por registros faltantes aparte de las que todavía no tienen ítems.

## Archivos principales

- `backend/src/main/resources/db/migration/V23__no_evaluado_y_registro_tardio.sql`
- `backend/src/main/java/com/galiledu/academico/dominio/CalculoNotas.java` (`Avance`, `Aporte`, `AporteFinal`)
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java` (`modificarCalificacion`)
- `backend/src/main/java/com/galiledu/academico/infraestructura/RepositorioCalificaciones.java` (qué ítems cuentan)
- `frontend/src/features/calificaciones/components/dialogo-modificar-calificacion.tsx` y `calificar-alumnos.tsx`

## Decisiones y por qué

- **Decisión:** una nota oficial existe solo cuando no falta ningún registro. Mientras tanto se ve «Pendiente (3 de 4 ítems)», antes y después de la fecha límite. Aplica a:
  - la nota de la competencia en el periodo y la final;
  - el aporte final del curso;
  - la libreta;
  - HU-63, HU-64 y la vista del apoderado.

  **Por qué:** promediar lo poco que hay daba notas engañosas. Por ejemplo, un 20 en una sola práctica salía «AD · Parcial», y quien ve «AD» se queda con la letra. Lo señaló alex-sanchezz.
  **Alternativas descartadas:**
  - Contar lo que falta como 0 en la nota oficial: castiga al estudiante por un olvido de registro. Solo se usa en la vista de trabajo del docente, que no es una nota.
  - Al vencer el plazo, contar solo lo registrado con la marca «Incompleta»: repartía de nuevo los pesos (una práctica de 10 % pasaba a valer 100 %) y podía premiar al que no entregó frente al que tiene un 05.
  - Un umbral mínimo de peso calificado: una regla más que explicar.
- **Decisión:** la vista de trabajo del docente sí promedia lo registrado, pero sin parecer una nota. Esa vista es el «Promedio del periodo» del consolidado y los aportes de cada curso en la tarjeta de la competencia. Si a un estudiante le falta un registro:
  - en la escala vigesimal, **lo que falta cuenta como 0** en ese promedio. En la escala de letras no hay 0 y se usa solo lo registrado;
  - su promedio va **en gris, sin letra ni color de logro** (también en el PDF);
  - debajo dice cuánto está calificado: en promedio ponderado, «Faltan registros (cuentan como 0) · 70 % del peso calificado»; en las demás reglas, «… · 2 de 3 ítems»;
  - la celda que falta dice «Por registrar». Un ítem que nadie tiene registrado sigue mostrando «—».

  **Por qué:** el docente necesita ver cómo van sus alumnos mientras califica. Pero la primera versión mostraba «A · Logro esperado» en verde con un «Avance: 2 de 3 ítems» en letra chica: un 15 y un 13 sin el ítem de 30 % daban 13,57 (A), cuando con un 8 en ese ítem habría sido 11,9 (B). Lo señaló alex-sanchezz. En ponderado importa el peso que falta, no cuántos ítems.
  Después alex-sanchezz pidió contar como 0 lo que falta en esta vista. Sin eso, el promedio de lo registrado podía **bajar** al completar las notas: 13,57 sin «FSSAF» y 11,9 con un 8. Con el 0 es un piso que solo puede subir (9,5 en ese caso), y el docente ve cuánto se juega con lo que falta.
- **Decisión:** «No evaluado» resuelve una celda sin nota y con motivo, y ese ítem no cuenta para ese estudiante.
  **Por qué:** sin esto, un estudiante que se matriculó a mitad del periodo, que tuvo una licencia o que está exonerado nunca tendría nota. Una celda vacía significa «falta registrar»; «No evaluado» es una exclusión justificada que queda en el historial. No es un 0.
- **Decisión:** un ítem que nadie de la sección tiene registrado (ni nota ni «No evaluado») se ignora.
  **Por qué:** el docente lo creó pero no lo usó, así que no dice nada de ningún estudiante. Así, un ítem recién creado tampoco deja todas las notas pendientes.
- **Decisión:** HU-60 permite, mientras el año esté activo, resolver una celda fuera del lote: modificar una nota, registrarla fuera de plazo o marcar «No evaluado». Pide motivo, salvo al registrar una nota nueva dentro del plazo. El registro en lote sigue cerrado fuera del plazo, y no se pueden crear ítems después de la fecha límite.
  **Por qué:** con la regla anterior, un olvido de registro dejaba la nota pendiente para siempre.
- **Decisión:** un curso sin ítems que cuenten en el periodo no participa, y un periodo sin ítems no cuenta para la nota final.
  **Por qué:** es el mismo criterio que el de los ítems sin uso. Los periodos que no han empezado no frenan la nota del año.

## Cambios en la base de datos

`V23__no_evaluado_y_registro_tardio.sql`:

| Tabla | Cambio |
| --- | --- |
| `academico.calificacion_item` | `no_evaluado` (booleano) y `motivo_no_evaluado`. `ck_calificacion_item_un_valor` ahora admite una fila sin nota si es «No evaluado» con motivo. |
| `academico.modificacion_calificacion` | `no_evaluado_anterior` y `no_evaluado_nuevo`. El valor anterior puede estar vacío: es una nota registrada fuera de plazo. |

Los datos existentes no cambian.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO. Sigue pendiente lo de la decisión del 16/11, ahora con «Pendiente» en lugar de «Parcial».
- [ ] Registrar notas faltantes en lote fuera de plazo, con un solo motivo. Hoy se hace fila por fila.
- [ ] El cierre de año (aún no existe) debería exigir que no quede ninguna nota pendiente.

## Notas para quien siga

- Qué ítems cuentan se decide en SQL. `RepositorioCalificaciones.calificacionesCompetencias` excluye los «No evaluado» del estudiante y los ítems sin uso. `itemsDelEstudiante` excluye solo los ítems sin uso, porque el apoderado ve el «No evaluado». Para `CalculoNotas`, una calificación sin valor es un registro faltante.
- En `CalculoNotas`, `Aporte.avance()` es el promedio de trabajo y `Aporte.nota()` es el oficial (vacío si está pendiente).

- Las migraciones se escribieron como V19 y V20 y se renumeraron a **V22** (historial) y **V23** («No evaluado») al integrar `main`, que ya traía V19 a V21. Si aplicaste las versiones viejas en tu base local, Flyway no arranca: hay que corregir el historial de Flyway o crear la base de nuevo.
