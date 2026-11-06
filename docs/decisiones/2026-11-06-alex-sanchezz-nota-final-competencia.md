# Nota final de una competencia entre los cursos del grado

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-62, HU-56
- **Estado:** terminado; faltan por confirmar con el PO las reglas de la sección «Pendientes»

## Qué hice

Agregué el cálculo de la nota de una competencia en el año, entre todos los cursos del grado que la evalúan. Usa el [peso de cada curso](2026-11-05-alex-sanchezz-peso-curso-competencia.md) y los parámetros del año escolar. El docente la ve en **Consolidado de notas**, en la tarjeta **Nota final de la competencia**: una columna por curso, la nota final y su estado.

El «Promedio final» del consolidado no cambia: sigue siendo la nota de un solo curso en un periodo, con los ítems seleccionados.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/dominio/CalculoNotas.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioNotaCompetencia.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/RepositorioCalificaciones.java`
- `frontend/src/features/calificaciones/components/nota-final-competencia.tsx`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

Las pidió alex-sanchezz sin fijar estas reglas, así que son valores por defecto y están pendientes de confirmar.

- **Decisión:** calcular en tres pasos: ítems → periodo (regla de nota del año), periodos → curso (consolidación del año), cursos → competencia (peso del plan).
  **Por qué:** cada columna de la tabla es la nota de un curso, así que la nota final se puede comprobar a mano con los pesos del encabezado. Es la primera vez que se aplica `consolidacion_periodos`.
  **Alternativas descartadas:** combinar los cursos dentro de cada periodo y luego consolidar. Con todas las notas da lo mismo en promedio, pero la tabla necesitaría una columna por curso y periodo.
- **Decisión:** un curso sin notas no cuenta: se normaliza por los pesos de los cursos que sí tienen, y la nota se marca como **Parcial**. También queda parcial si falta un periodo que influye en la nota o un ítem por calificar.
  **Por qué:** es el criterio que ya usa el promedio de un curso con los ítems sin calificar. La marca evita leer como definitiva una nota a medio año.
  **Alternativas descartadas:** contar el curso sin notas como 0, porque desaprobaría al estudiante por una nota que aún no existe; esperar a tener todas las notas, porque no habría ninguna nota hasta fin de año.
- **Decisión:** con `PROMEDIO_PONDERADO` y pesos incompletos no se calcula la nota final: se muestra un aviso, «—» y el estado Pendiente. Las notas por curso sí se muestran.
  **Por qué:** normalizar pesos que el colegio todavía no terminó de definir daría una nota inventada.
  **Alternativas descartadas:** normalizar por los pesos que haya, porque un curso sin peso quedaría fuera sin que nadie lo decida.
- **Decisión:** el peso del curso solo se usa con `PROMEDIO_PONDERADO`. Con `PROMEDIO_SIMPLE` los cursos pesan igual, y las reglas cualitativas se aplican entre los cursos (`NIVEL_MAS_FRECUENTE` desempata por el nivel mayor; `ULTIMO_NIVEL` toma el registrado más reciente).
  **Por qué:** es coherente con cómo esas reglas tratan los pesos de los ítems.
  **Alternativas descartadas:** ponderar siempre, porque contradice la regla elegida para el año.
- **Decisión:** en la escala cualitativa, `PROMEDIO_PERIODOS` aplica la regla de nota del año sobre los niveles de cada periodo. `ULTIMO_PERIODO` toma el último periodo con notas de cada curso.
  **Por qué:** los niveles no se pueden promediar, y la configuración del año admite esa combinación.
  **Alternativas descartadas:** convertir los niveles a números con la tabla de conversión, porque cada nivel es un rango y la conversión inversa no es exacta.
- **Decisión:** los pasos intermedios no se redondean; solo se redondea lo que se muestra, a 2 decimales con `HALF_UP`. El estado se decide con la nota ya redondeada.
  **Por qué:** así un redondeo no se acumula entre periodos y cursos, y el estado coincide con la nota que se ve. Por eso, en casos raros, la cuenta a mano con las notas mostradas puede diferir en 0,01.
  **Alternativas descartadas:** redondear en cada paso, porque el error se acumula.
- **Decisión:** la nota aprobatoria es el rango A de la tabla de conversión vigente al inicio del último periodo del año; si no hay tabla, 14.
  **Por qué:** sigue la regla del consolidado por periodo, aplicada al cierre del año.
  **Alternativas descartadas:** usar el primer periodo, porque una versión nueva de la tabla no regiría para la nota final.
- **Decisión:** mostrarla solo al Docente, en el consolidado de un curso y sección que tiene asignados. Ve los resultados por curso de sus colegas en esa sección, no sus ítems.
  **Por qué:** es la pantalla donde hoy está el promedio final y la única de calificaciones implementada: la del Coordinador sigue siendo un marcador de módulo.
  **Alternativas descartadas:** crear ahora pantallas para Coordinador, Tutor y Apoderado, porque no hay diseño ni HU en este alcance.
- **Decisión:** agregar la tarjeta aunque no aparece en lo que la ficha documenta de los frames del consolidado (`648:17853` y `631:16187`). No se abrió Figma para esta tarea.
  **Por qué:** sin ella no hay dónde ver la nota entre cursos. Si Figma no la tiene, es una excepción a «manda Figma» que falta confirmar con el equipo.
  **Alternativas descartadas:** agregar una columna al consolidado por curso, porque mezclaría la nota de un periodo con la del año.
- **Decisión:** calcular la nota al consultarla, sin guardarla.
  **Por qué:** cambia con cada nota o peso nuevo, y guardarla exigiría recalcular y mantener copias sincronizadas.
  **Alternativas descartadas:** una tabla de notas finales, que hará falta cuando exista un cierre oficial del año, fuera de este alcance.

## Cambios en la base de datos

Ninguno: no hay migración nueva. Se leen `academico.item_evaluacion`, `item_competencia`, `calificacion_item`, `plan_competencia` (V13) y los parámetros de `configuracion.anio_escolar`.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO las reglas de esta entrada, sobre todo cursos sin notas, pesos incompletos y uso del peso solo con `PROMEDIO_PONDERADO`.
- [ ] Revisar en Figma si existe un diseño para esta nota y, si no, confirmar la excepción con el equipo. Confirmar también si el Docente debe ver los resultados de los otros cursos.
- [ ] Mostrar la nota a Coordinador, Tutor, Apoderado y Estudiante cuando se implementen sus pantallas.
- [ ] Incluir la nota final en la exportación CSV; hoy solo se exporta el consolidado por curso.

## Notas para quien siga

`CalculoNotas` no toca la BD y el consolidado por curso también la usa: si cambia una regla, cambia en los dos sitios. `parcial` solo es verdadero cuando hay nota final. Con `ULTIMO_PERIODO`, una falta en un periodo anterior no vuelve parcial la nota, porque no la cambia.
