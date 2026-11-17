# Nota oficial por competencia y peso de cada curso

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-16
- **HU / tarea:** HU-56 (plan de evaluación) y HU-62 (consolidado de notas)
- **Estado:** terminado; las reglas de la sección «Pendientes» falta confirmarlas con el PO

## Qué hice

La nota oficial pasa a ser de la **competencia**: una por periodo y una final, aunque la evalúen varios cursos del grado. Por ejemplo, Aritmética y Geometría evalúan «Resuelve problemas de cantidad». El curso no tiene nota final propia: aporta a la nota de cada competencia que evalúa.

- **Plan de evaluación:** una vista nueva, «Competencias del grado», muestra qué cursos evalúan cada competencia y cuánto aporta cada uno. El modal «Ajustar pesos» exige que sumen 100 %.
- **Consolidado del docente:** una tarjeta nueva, «Nota de la competencia», muestra la nota por periodo y la final, con el aporte de cada curso.

Esta entrada reemplaza el enfoque de la rama `feat/catalogo-peso-curso-competencia` (no integrada). Esa rama pedía el peso en el modal de cada curso y calculaba solo la nota final, sin la nota por periodo.

## Archivos principales

- `backend/src/main/resources/db/migration/V16__peso_curso_competencia.sql`
- `backend/src/main/java/com/galiledu/academico/dominio/CalculoNotas.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioNotasCompetencia.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioPlanEvaluacion.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/RepositorioPesosPlan.java`
- `frontend/src/features/catalogo-academico/components/competencias-grado.tsx`
- `frontend/src/features/calificaciones/components/notas-competencia.tsx`

## Decisiones y por qué

- **Decisión:** cada nivel configura una sola cosa.
  - El **año escolar** fija la escala, la regla de nota y la consolidación (sin cambios).
  - El **plan de evaluación** fija, por competencia, cuánto aporta cada curso.
  - El **docente** define los ítems y sus pesos dentro de su curso (sin cambios).

  **Por qué:** respeta la [decisión de parámetros por año](2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md). Lo único que faltaba era cómo juntar varios cursos en una competencia.
  **Alternativas descartadas:** una regla de cálculo propia por curso, porque contradice esa decisión y crea notas finales que compiten entre sí.
- **Decisión:** el cálculo va en este orden.
  1. Ítems → aporte del curso en el periodo, con la regla del año.
  2. Aportes → nota de la competencia en el periodo, con el peso del curso.
  3. Notas de periodo → nota final, con la consolidación del año.

  **Por qué:** la libreta necesita la nota de la competencia en cada periodo. Consolidar primero cada curso y combinar al final (lo que hacía la rama anterior) no la produce. Además, con notas faltantes, «último periodo» o la escala cualitativa da resultados distintos.
  **Alternativas descartadas:** consolidar los periodos por curso y combinar los cursos al final.
- **Decisión:** los pesos de una competencia se editan juntos, en un modal por competencia, y deben sumar **exactamente 100 %**.
  **Por qué:** el 100 % se reparte entre cursos. Editarlos curso por curso, como en la rama anterior, no deja ver cuánto aportan los demás ni exigir el total exacto.
  **Alternativas descartadas:** el campo «Peso en %» en el modal de cada curso.
- **Decisión:** si una competencia la evalúa un solo curso, ese curso aporta el 100 % automáticamente.
  - Un curso nuevo en una competencia compartida entra «sin peso», y la competencia queda con **pesos incompletos** hasta que se ajusten.
  - Al quitar un curso, si queda uno solo, recibe el 100 %.
  - Copiar un plan copia los pesos.

  **Por qué:** no hay que configurar nada en el caso común, y no se reparten pesos a escondidas.
  **Alternativas descartadas:** volver a repartir en partes iguales al cambiar los cursos, porque pisaría pesos que el colegio ya eligió.
- **Decisión:** el peso del curso solo se usa con `PROMEDIO_PONDERADO`.
  - Con `PROMEDIO_SIMPLE` los cursos aportan por igual.
  - En la escala cualitativa se aplica la regla del año a los niveles de los cursos (`NIVEL_MAS_FRECUENTE` desempata por el nivel mayor; `ULTIMO_NIVEL` toma el más reciente).

  **Por qué:** es lo mismo que ya se hace con el peso de los ítems ([decisión de pesos solo en ponderado](2026-11-06-josezavaleta-pesos-solo-promedio-ponderado.md)).
- **Decisión:** con promedio ponderado y pesos incompletos no se calcula la nota de la competencia («—» y un aviso), pero sí se ven los aportes.
  **Por qué:** normalizar pesos que el colegio no terminó de definir inventaría la nota.
- **Decisión:** un curso sin notas en el periodo no cuenta y la nota se marca **Parcial**. También es parcial si un ítem está sin calificar.
  - La nota final es parcial mientras falte algún periodo.
  - Con `ULTIMO_PERIODO`, es parcial mientras el último periodo no tenga su nota completa.

  **Por qué:** es el mismo criterio del promedio de un curso con ítems pendientes. Contar como 0 desaprobaría por una nota que aún no existe.
- **Decisión:** el «Promedio general» del consolidado de un curso (que mezcla varias competencias) se mantiene, marcado como **referencial** en pantalla y en el PDF.
  **Por qué:** las competencias no se promedian entre sí para la nota oficial, pero el docente lo usaba como resumen.
- **Decisión:** la nota final de la competencia se muestra **siempre en nivel de logro** (AD, A, B, C), en pantalla y al final del PDF del consolidado.
  - La nota vigesimal, tal como se muestra (2 decimales), se redondea al entero con 0,5 hacia arriba y se ubica en la tabla de conversión vigente (16,5 → 17 → A; 13,5 → 14 → A; 17,5 → 18 → AD).
  - El estado usa la misma regla, también en el consolidado por curso.

  **Por qué:** lo pidió alex-sanchezz: la nota final de las competencias tiene que estar en letras. La tabla de conversión solo tiene rangos enteros, así que una nota con decimales necesita una regla. Redondear con 0,5 hacia arriba es la práctica habitual de la escala vigesimal, y aplicar la misma regla al estado evita un «A» desaprobado.
  **Alternativas descartadas:** truncar (13,5 → 13 → B), que perjudica al estudiante en el borde; comparar el estado con decimales y la letra con enteros, que se contradirían.
- **Decisión:** la nota de la competencia se calcula al consultarla y no se guarda.
  **Por qué:** cambia con cada nota o peso. Guardarla hará falta cuando exista un cierre oficial del año, fuera de este alcance.
- **Decisión:** los pasos intermedios no se redondean. Solo se redondea lo que se muestra (2 decimales, `HALF_UP`), y el estado se decide con la nota redondeada.
  **Por qué:** evita acumular redondeos entre cursos y periodos. Por eso, en casos raros, la cuenta a mano con los aportes mostrados puede diferir en 0,01.

## Cambios en los esquemas de base de datos

`V16__peso_curso_competencia.sql` agrega una columna a `academico.plan_competencia`:

| Columna | Tipo | Regla |
| --- | --- | --- |
| `peso_curso` | `NUMERIC(5, 2)`, anulable | `ck_plan_competencia_peso_curso`: `NULL` o mayor que 0 y hasta 100. La suma de 100 entre cursos la valida el servicio. |

Los planes existentes reparten el 100 % en partes iguales entre los cursos de cada competencia. Los datos de prueba (`R__datos_prueba.sql`) hacen que Aritmética (60 %) y Geometría (40 %) compartan «Resuelve problemas de cantidad» en 1.° de secundaria 2026.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO estas reglas: pesos solo en ponderado, «parcial» con notas faltantes, sin nota con pesos incompletos, promedio general referencial y el redondeo al entero (0,5 sube) para la letra y el estado.
- [ ] Revisar en Figma si hay diseño para «Competencias del grado» y para la nota de la competencia; si no, confirmar la excepción con el equipo.
- [ ] Mostrar la nota de la competencia a Tutor, Coordinador, Apoderado y Estudiante cuando se implementen sus pantallas (hoy son marcadores). El cálculo ya está en `ServicioNotasCompetencia`.
- [x] La nota final de la competencia, en nivel de logro, va al final del PDF del consolidado.
- [ ] Decidir si además hace falta un reporte propio (libreta) por estudiante.
- [ ] Decidir si los pesos se bloquean al iniciar el año, como los parámetros del año. Hoy se pueden cambiar mientras el año no esté cerrado.

## Notas para quien siga

- `CalculoNotas` no toca la base de datos y se prueba sola (`CalculoNotasTests`).
- El «Promedio final» del consolidado se sigue calculando en `ServicioCalificaciones` con su propio redondeo. Si cambia una regla, cambia en los dos lugares.
- `RepositorioPesosPlan.completarUnicos` se llama después de agregar, configurar, quitar o copiar cursos.
- Los cambios de pesos de un grado se serializan con un bloqueo consultivo de PostgreSQL.
- Si tu base local tenía la V13 de la rama anterior, Flyway no arranca: crea de nuevo la base `galiledu` (y `galiledu_test`) vacía.
