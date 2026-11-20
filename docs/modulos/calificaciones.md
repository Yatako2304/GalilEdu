# Calificaciones

- **HU:** HU-57, HU-59, HU-60 y HU-62 (Docente) · HU-63 y HU-64 (reportes del Coordinador y del Administrador) · HU-69 (Apoderado)
- **Estado:** ✅ HU-57, HU-59 y HU-62 terminadas y verificadas · 🚧 HU-60, HU-63, HU-64 y HU-69 con backend, frontend y pruebas; falta revisión del equipo y confirmar criterios con el PO
- **Última actualización:** 2026-11-20

## Alcance

El Docente trabaja únicamente con los cursos y secciones que tiene asignados en el plan de evaluación. Desde una sola pantalla puede:

- registrar ítems de evaluación con un criterio, tipo y competencias; el peso se solicita solo en promedio ponderado;
- calificar a cada estudiante matriculado y adjuntar una evidencia pedagógica;
- consultar resultados por ítem, competencia o periodo y descargar el consolidado como una tabla en PDF;
- ver la **nota oficial de cada competencia**: una por periodo y una final, combinando todos los cursos del grado que la evalúan.

**La nota oficial es de la competencia, no del curso.** El «Promedio del periodo» del consolidado es lo que aporta este curso a la competencia en un periodo (hasta el 19/11 se llamaba «Promedio final»). Véase la [decisión de la nota oficial por competencia](../decisiones/2026-11-16-alex-sanchezz-nota-oficial-competencia.md).

Aunque los frames revisados muestran «Personal administrativo» o «Administrador» en la barra lateral, están ubicados en la página `🌟 Docente` de Figma y corresponden al flujo del usuario Docente. La ruta implementada es `/app/docente/calificaciones` y la API exige el rol `DOCENTE`.

La escala, la regla de cálculo y la consolidación no se eligen en esta pantalla. Se leen de `configuracion.anio_escolar`, según la [decisión de parámetros de evaluación por año](../decisiones/2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md).

En **Calificar alumnos**, el docente selecciona únicamente el **Ítem de evaluación** entre todos los del periodo. La nota se registra una sola vez por estudiante e ítem y se reutiliza en sus competencias con los pesos configurados. No se solicita elegir una competencia para calificar, según el ajuste pedido por josezavaleta; véase la [decisión de calificación por ítem](../decisiones/2026-11-06-josezavaleta-calificar-solo-por-item.md).

## Historias de usuario

| HU | Qué | Estado | Reglas principales |
| --- | --- | --- | --- |
| HU-57 | Registro de criterios e ítems de evaluación de un curso | ✅ | El ítem pertenece a un curso, sección y periodo. Tiene nombre, criterio, tipo y una o más competencias del plan. Solo en promedio ponderado requiere pesos de 0,01 a 100 y la suma por competencia no supera 100 %. |
| HU-59 | Registro de calificación de un ítem | ✅ | Una nota por estudiante matriculado e ítem. VIGESIMAL admite 0–20; CUALITATIVA admite AD, A, B o C. Puede adjuntar un PDF, DOC, DOCX, JPG o PNG de hasta 10 MB. |
| HU-62 | Consolidado de notas de un curso | ✅ | Selecciona una o varias competencias y sus ítems; muestra resultados por competencia y promedio general de la selección con la regla del año; descarga una tabla en PDF. |
| HU-60 | Modificación de una calificación | 🚧 | El docente cambia una nota ya registrada con un motivo obligatorio (10 a 500 caracteres), también después de la fecha límite mientras el año esté ACTIVO y el periodo haya empezado. Cada cambio queda en un historial (antes, después, motivo, quién, cuándo). El registro en lote sigue cerrado fuera del plazo. |
| HU-63 | Reporte de notas por curso y grado | 🚧 | Coordinador. Por competencia del grado: una columna por sección y otra del grado completo, con el promedio, su nivel y el % de estudiantes con A o AD. Todos los cursos (nota oficial) o un curso (su aporte); un periodo o la nota final. PDF. **Ver estudiantes** abre el detalle de una sección: la nota oficial de cada competencia por estudiante (del periodo o la final, con «Pendiente» donde falten registros), o **Todo el año** (por competencia, el nivel de cada periodo y la nota final, como un registro consolidado), y su libreta en PDF. **Ítems** abre las notas del estudiante con los ítems que calificó cada docente por periodo (la misma vista del apoderado). Solo lectura y solo el Coordinador; ver la [decisión del 20/11](../decisiones/2026-11-20-alex-sanchezz-coordinador-ve-items.md). |
| HU-64 | Reporte institucional de notas | 🚧 | Coordinador y Administrador. Como un reporte de logros: el porcentaje (y la cantidad) de calificaciones de competencia en cada nivel (AD, A, B, C) y «A o AD», con una barra de distribución, por grado (con sus secciones debajo), por área y todo el colegio. Lo que no se cuenta va en avisos aparte. PDF. Ver la [decisión del 20/11](../decisiones/2026-11-20-alex-sanchezz-reporte-institucional.md). |
| HU-69 | Consulta de notas por el apoderado | 🚧 | Solo sus hijos matriculados. Por periodo: cursos con su docente, lo que aportan a cada competencia y sus ítems; la nota oficial de cada competencia por periodo y final; con el año terminado, el resultado final por niveles de logro. Descarga la libreta en PDF. |

## Pantallas (Figma)

| Flujo | Frames revisados | Implementación |
| --- | --- | --- |
| Configurar ítems | `738:20655` y modal `738:20344` | Botón **Configuración de ítems** → **Configurar ítems**, con vistas **Ítems de evaluación** e **Ítems por competencia** |
| Calificar alumnos | `631:14676`, con estados previos `629:11540` y `629:11893` | Botón **Configuración de ítems** → pestaña **Calificar alumnos** |
| Consolidar y exportar notas | `648:17853` y detalle `631:16187` | Botón **Consolidado de notas**, con los filtros, selección múltiple de competencias e ítems, interruptor de promedio del periodo y tabla agrupada; el selector «Periodo lectivo» agrega «Todo el año» (resumen del año, sin frame en Figma) |
| Modificar una calificación (HU-60) | — (sin frame) | Columna **Acciones** → **Modificar** en «Calificar alumnos»; modal con la nota actual, la nueva, el motivo y el historial. Sigue el modal «Ítem de calificación» |
| Reporte por curso y grado (HU-63) | Coordinador › «Competencias reporte» `349:11639` (fragmento) | `/app/coordinador/calificaciones`, pestaña **Por curso y grado** |
| Reporte institucional (HU-64) | — (sin frame) | Pestaña **Institucional** del Coordinador y `/app/admin/calificaciones` (menú **Académico › Calificaciones** del Administrador) |
| Consulta del apoderado (HU-69) | Apoderado › `467:7656` y «Resultado final» `1105:2091` | `/app/apoderado/calificaciones` |

**Figma frente a las decisiones registradas (HU-63 y HU-69).** Figma muestra «% aprobados», una letra por curso, «Situación final: Aprobado» y «Cursos aprobados 5 de 5». Por la [nota oficial por competencia](../decisiones/2026-11-16-alex-sanchezz-nota-oficial-competencia.md) y el [nivel de logro sin aprobado](../decisiones/2026-11-18-alex-sanchezz-nivel-de-logro-sin-aprobado.md), se implementó así (lo eligió alex-sanchezz el 19/11): «% con A o AD» en lugar de «% aprobados»; la letra del curso es un resumen **referencial** de sus ítems y la nota oficial se muestra por competencia; el resultado final cuenta las competencias por nivel de logro, sin «Aprobado». Las tarjetas «Promedio del periodo» y «Cursos en proceso» pasan a «Competencias con logro esperado o destacado» y «Por reforzar». Véase la [decisión de las HU-60, 63, 64 y 69](../decisiones/2026-11-19-alex-sanchezz-modificacion-reportes-apoderado.md).

Debajo del encabezado **Calificaciones**, dos pestañas con apariencia de botón separan **Configuración de ítems** y **Consolidado de notas**, según el ajuste solicitado por josezavaleta. La primera contiene los filtros de curso y sección, las tarjetas de periodos y las pestañas **Configurar ítems** y **Calificar alumnos**; la segunda muestra únicamente el flujo del consolidado.

La selección múltiple de competencias y el promedio general amplían el selector individual de Figma por petición expresa de josezavaleta. Se conservan los componentes y la estructura visual del consolidado; véase la [decisión del consolidado múltiple](../decisiones/2026-11-06-josezavaleta-consolidado-multiples-competencias.md).

Por solicitud posterior de josezavaleta, el botón del reporte se llama **Descargar PDF** y descarga únicamente ese formato. El PDF incluye curso, grado, sección, año y periodo, la escala y regla anual, encabezados agrupados por competencia y resultados con estado. Usa A4 horizontal y repite los encabezados al paginar. Si hay muchas columnas, las divide en bloques legibles; repite los estudiantes y el promedio general de toda la selección en cada bloque. Véase la [decisión del reporte PDF](../decisiones/2026-11-06-josezavaleta-reporte-notas-pdf.md).

La pantalla conserva los filtros separados de año, curso, grado, sección y área, las tarjetas de periodos y los componentes del catálogo UI del repositorio. Las tablas usan columnas de ancho estable para evitar desplazamientos entre encabezados y celdas. En promedio ponderado, la vista por competencia muestra los porcentajes y una etiqueta verde al completar 100 % o roja si está incompleta; al alcanzar 100 %, la competencia deja de estar disponible para nuevos ítems. En promedio simple y reglas cualitativas, se ocultan el campo y la columna de peso y los indicadores de porcentajes, y no se bloquean altas por completar 100 %. Esta adaptación del diseño responde a la solicitud expresa de josezavaleta; véase la [decisión de pesos solo en ponderado](../decisiones/2026-11-06-josezavaleta-pesos-solo-promedio-ponderado.md).

## Base de datos

- **Esquema:** `academico`
- **Migraciones:** `V11__calificaciones_docente.sql`, `V15__calificaciones_peso_solo_ponderado.sql`, `V22__historial_calificaciones.sql` y `V23__no_evaluado_y_registro_tardio.sql`

| Tabla | Para qué | Reglas destacadas |
| --- | --- | --- |
| `academico.item_evaluacion` | Ítem de un curso, sección y periodo. | Nombre único dentro de curso, sección y periodo; tipo cerrado; claves compuestas garantizan el mismo año y grado. |
| `academico.item_competencia` | Competencias y peso del ítem. | La competencia debe pertenecer a `academico.plan_competencia`. V15 permite peso `NULL` para reglas sin ponderación; los porcentajes no nulos conservan el rango mayor que 0 y menor o igual que 100. El servicio exige peso de 0,01 a 100 únicamente para promedio ponderado. |
| `academico.calificacion_item` | Nota de un estudiante para un ítem. | Una por matrícula e ítem; exactamente un valor numérico o cualitativo; la matrícula debe pertenecer a la sección del ítem. |
| `academico.evidencia_calificacion` | Archivo de evidencia de la nota. | Una por calificación; MIME permitido y tamaño entre 1 byte y 10 MB. |
| `academico.modificacion_calificacion` | Historial de cambios de una nota (HU-60). | Por ítem y matrícula (sobrevive si la nota se borra). Valor anterior vacío = se registró fuera de plazo; valor nuevo vacío = la nota se quitó; `no_evaluado_anterior` y `no_evaluado_nuevo` marcan los cambios desde y hacia «No evaluado» (V23); motivo no vacío. Solo se agregan filas. |

V11 agrega restricciones `UNIQUE` compuestas a `academico.plan_curso`, `configuracion.seccion`, `configuracion.periodo_academico` y `matricula.matricula`. Se usan como destinos de claves foráneas y no cambian los datos existentes.

V15 elimina únicamente `NOT NULL` de `academico.item_competencia.peso_porcentaje` y documenta su uso. Conserva `ck_item_competencia_peso`, las claves e índices y todos los datos existentes. Las asociaciones nuevas o editadas de años sin ponderación guardan `NULL`; los porcentajes históricos conservados se ignoran según la regla anual. Flyway aplica la migración al arrancar el backend.

## Reglas de aplicación

- El backend valida la asignación en `academico.docente_seccion` en cada operación; conocer un identificador de otro curso no concede acceso.
- Los datos históricos se consultan, pero solo se crean o modifican ítems, notas y evidencias cuando el año está `ACTIVO` y la fecha está entre el inicio del periodo y `fecha_limite_notas`, ambos inclusive.
- No se elimina un ítem que ya tiene calificaciones.
- Solo con `PROMEDIO_PONDERADO`, cada asociación exige un porcentaje de 0,01 a 100 y la suma dentro del mismo curso, sección y periodo no puede superar 100 %. La validación se repite en el backend y serializa las altas y ediciones del mismo plan de curso para evitar sobreasignaciones simultáneas. En las demás reglas, el backend ignora porcentajes enviados por clientes anteriores, guarda `NULL` y conserva la validación de competencias válidas y no repetidas.
- Una calificación vacía elimina la nota y su evidencia. Para adjuntar evidencia primero debe existir la calificación.
- **HU-60.** Fuera del lote, una celda se resuelve con la acción de la fila (**Modificar** o **Registrar**), mientras el año esté ACTIVO y el periodo haya empezado (`modificacionHabilitada`): se modifica una nota, se **registra fuera de plazo** una que falta o se marca **«No evaluado»**. Pide motivo de 10 a 500 caracteres, salvo al registrar por primera vez una nota dentro del plazo. Se rechaza un valor igual al actual o de otra escala. La tabla de «Calificar alumnos» **solo registra celdas vacías**: una nota ya guardada (o un «No evaluado») queda bloqueada y se cambia únicamente con **Modificar** y motivo, también dentro del plazo. El backend rechaza con 409 un cambio por lote a una nota existente; repetir el mismo valor o enviarla vacía no cambia nada. Una nota registrada no vuelve a quedar en blanco: se corrige con otro valor o con «No evaluado». Registrar una nota por primera vez no deja historial.
- **«No evaluado».** Resuelve la celda sin nota y con motivo (se matriculó después, licencia, exoneración): el ítem **no cuenta** para ese estudiante; no es un 0. Se ve como «NE» en el consolidado y «No evaluado» en la vista del apoderado.
- **Una nota oficial existe solo cuando está completa** ([decisión del 19/11](../decisiones/2026-11-19-alex-sanchezz-nota-pendiente-hasta-completar.md)). Un ítem que nadie de la sección tiene registrado (ni nota ni «No evaluado») se ignora. Si a un estudiante le falta registrar un ítem que sí cuenta, su nota oficial queda **«Pendiente (3 de 4 ítems)»**, sin número ni letra, antes y después de la fecha límite: la nota de la competencia en el periodo, la final, el aporte final del curso, la libreta, HU-63, HU-64 y el apoderado. La vista de trabajo del docente sí muestra un número, pero sin parecer una nota: en el consolidado, el «Promedio del periodo» de un estudiante con registros faltantes cuenta **como 0 lo que falta** (en la escala vigesimal; en letras no hay 0 y usa solo lo registrado), así que es un piso que solo puede subir. Va **en gris, sin letra ni color de logro**, con «Faltan registros (cuentan como 0) · 70 % del peso calificado» (en promedio ponderado; en las demás reglas, «2 de 3 ítems»), y la celda que falta dice «Por registrar». Los aportes de cada curso en la tarjeta de la competencia siguen la misma regla y dicen «(faltan 1 de 3, cuentan como 0)». Un 0 automático nunca entra en una nota oficial.
- `PROMEDIO_SIMPLE` promedia los ítems calificados. `PROMEDIO_PONDERADO` usa los pesos de los ítems calificados y normaliza por la suma de esos pesos.
- `NIVEL_MAS_FRECUENTE` elige el nivel más repetido; si hay empate, prevalece el nivel de logro mayor. `ULTIMO_NIVEL` toma el último ítem registrado.
- El consolidado agrupa las columnas por las competencias seleccionadas. Los ítems compartidos aparecen en cada grupo al que pertenecen, pero se ofrecen una sola vez en el selector de ítems. La selección de competencias determina los ítems disponibles.
- **Incluir promedio del periodo** agrega el resultado de cada competencia y, si se seleccionó más de una, el promedio general del curso para la selección actual, con su nivel de logro: «A · Logro esperado» en vigesimal o el significado del nivel en la cualitativa, y «Pendiente» sin notas. No se usa «Aprobado» ni «Desaprobado» (ver la [decisión del 18/11](../decisiones/2026-11-18-alex-sanchezz-nivel-de-logro-sin-aprobado.md)). Con una sola competencia se muestra un único promedio. El interruptor desactivado oculta todos los promedios.
- El promedio general se muestra como **referencial** (grupo «Resumen del curso (referencial)»): mezcla competencias y no es una nota oficial. El PDF lo aclara en la línea de parámetros.
- La regla se obtiene de `configuracion.anio_escolar.regla_nota`, también para el promedio general. El promedio simple cuenta cada ítem calificado una sola vez. En el ponderado, el peso de un ítem compartido es la suma de sus pesos en las competencias seleccionadas: `suma(nota × peso) / suma(peso)`. Se excluyen los ítems sin nota, se incluye el cero y se redondea a dos decimales al final; sin notas se muestra `—` y estado Pendiente.
- Para la escala cualitativa, el resultado general usa los ítems únicos con la misma regla anual de frecuencia o último nivel. La consolidación entre periodos sigue siendo un parámetro distinto y no interviene en este reporte de un solo periodo.
- El nivel de logro sale de la tabla de conversión vigente en la fecha de inicio del periodo: la nota se redondea al entero (0,5 sube) y se ubica en sus rangos. Se muestra con su significado (AD «Logro destacado», A «Logro esperado», B «En proceso», C «En inicio») y un color por nivel: AD y A en verde, B en ámbar, C en rojo.
- Las descargas usan el tipo MIME real: `application/pdf` para el consolidado y el tipo guardado para evidencias. La ruta CSV anterior se conserva para compatibilidad con clientes existentes; el frontend solo ofrece PDF.

### Nota de la competencia

Se calcula al consultarla y no se guarda (`CalculoNotas` y `ServicioNotasCompetencia`). Va por sección, en tres pasos:

1. **Ítems → aporte del curso en el periodo:** la regla de nota del año sobre los ítems del curso que apuntan a la competencia. Es el mismo «Promedio del periodo» del consolidado.
2. **Aportes → nota de la competencia en el periodo:** con `PROMEDIO_PONDERADO` se usa el peso de cada curso del plan de evaluación (`academico.plan_competencia.peso_curso`) y se normaliza por los cursos que tienen notas. Con `PROMEDIO_SIMPLE` los cursos aportan por igual. En la escala cualitativa (`NIVEL_MAS_FRECUENTE` o `ULTIMO_NIVEL`) cada curso **vota por su nivel con su peso**: gana el nivel con más peso acumulado y, si empatan, el nivel mayor (Aritmética 60 % B y Geometría 40 % AD → B). Los cursos sin nota no votan. La regla del año (más frecuente o último) se sigue aplicando dentro de cada curso, a sus ítems, y entre periodos.
3. **Notas de periodo → nota final:** `PROMEDIO_PERIODOS` promedia los periodos con nota (en la escala cualitativa aplica la regla de nota a sus niveles). `ULTIMO_PERIODO` toma el último periodo con nota.

Ejemplo con Aritmética 60 % y Geometría 40 %: en el IV bimestre Aritmética aporta 13 y Geometría 16, así que la competencia tiene 13 × 0,6 + 16 × 0,4 = 14,2.

| Situación | Resultado |
| --- | --- |
| Un curso sin ítems que cuenten en el periodo (nadie los tiene registrados) | No participa; la nota se calcula con los demás cursos |
| Al estudiante le falta registrar un ítem que cuenta | La nota del periodo y la final quedan **Pendiente** (sin número ni letra), con «3 de 4 ítems» |
| Un periodo sin ítems todavía | No cuenta para la final |
| Ítem «No evaluado» para el estudiante | No cuenta para él |
| `PROMEDIO_PONDERADO` o escala cualitativa con pesos que no suman 100 % | Sin nota de competencia («—», Pendiente) y con un aviso; los aportes sí se ven |
| Ningún curso tiene notas | «—», Pendiente |

**Nivel de logro (letra).** La nota final de la competencia se muestra **siempre en nivel de logro** (AD, A, B, C), con el número como referencia. En la escala vigesimal, la nota (tal como se muestra, con 2 decimales) se **redondea al entero, 0,5 sube**, y se ubica en los rangos enteros de la tabla de conversión: 16,5 → 17 → A; 13,5 → 14 → A; 13,49 → 13 → B; 17,5 → 18 → AD. Cada periodo usa la versión de la tabla vigente al inicio del periodo; la nota final, la del inicio del último periodo. Si no hay ninguna versión, se usa C 0–10, B 11–13, A 14–17 y AD 18–20. Junto a la letra se muestra su **significado** («Logro esperado», «En proceso»…), no «Aprobado» ni «Desaprobado»: en la evaluación por competencias no se aprueba una competencia; aprobar es la situación final del año (promoción), que todavía no calcula este módulo. El consolidado por curso muestra igual el nivel de su «Promedio del periodo». En la pantalla, la tarjeta **Nota de la competencia** aparece debajo del consolidado de un periodo al generarlo, con una tabla por cada competencia elegida. El interruptor **Ver el aporte de cada curso** muestra cuánto aporta cada curso en cada periodo. El docente ve los aportes de los cursos de sus colegas en la sección, pero no sus ítems.

### Consolidado de un periodo y resumen del año

Cada reporte del docente tiene un solo alcance (ver la [decisión del 19/11 sobre el reporte por periodo](../decisiones/2026-11-19-alex-sanchezz-reporte-docente-por-periodo.md)):

- **Un periodo:** los ítems, el «Promedio del periodo» de este curso y, al final del PDF, la nota oficial de la competencia **en ese periodo**. No trae periodos que aún no terminan ni la nota del año.
- **Todo el año** (opción del selector «Periodo lectivo»): por competencia, el aporte de este curso en cada periodo y su **aporte final**, y la nota de la competencia en cada periodo y la final. El aporte final consolida los periodos del curso con la regla del año; es referencial y es la misma cifra del reporte HU-63 cuando se elige el curso y «Nota final». Queda pendiente mientras a algún periodo le falte un registro. La nota de la competencia **no** es el promedio de los aportes finales: combina los cursos en cada periodo y después junta los periodos. La pantalla y el PDF lo aclaran.
- Los PDF del docente muestran la fecha y hora en que se generaron: la nota de la competencia cambia mientras otros cursos registren notas.

### Reportes (HU-63 y HU-64)

Usan la misma nota oficial de la competencia (`ServicioNotasCompetencia.resultadosSeccion`); `ResumenNotas` la resume por grupo:

- **Promedio:** en vigesimal, promedio simple de los estudiantes con nota y su nivel (redondeo al entero, 0,5 sube); en la escala cualitativa no se promedian niveles: el nivel más frecuente (si empatan, el mayor).
- **«% con A o AD»:** estudiantes con logro esperado o destacado sobre los que tienen nota. No hay «aprobados».
- **Con un curso (HU-63):** cada celda resume lo que ese curso aporta a la competencia; en la nota final sus periodos se consolidan con la regla del año.
- **Institucional (HU-64):** cada nota oficial de competencia de cada estudiante es una **calificación** y cuenta una vez. Los porcentajes de cada nivel se calculan sobre las calificaciones completas. Solo entran los grados con plan de evaluación y las secciones con estudiantes.
- Solo cuentan las notas completas. Una nota pendiente por registros faltantes no entra al promedio ni a los niveles: HU-63 dice cuántos estudiantes tienen notas por registrar y HU-64 las muestra como «+2 por completar» en la fila y en un aviso con las secciones donde faltan. Las áreas que aún no tienen calificaciones no salen en la tabla: se nombran debajo («Aún sin calificaciones en este periodo»).
- Una competencia con pesos de curso incompletos no tiene nota oficial: sale «—» (HU-63 avisa). En HU-64 no entra en las cifras y sale en el aviso «Competencias que no se pueden calcular».
- La tabla de conversión es la vigente al inicio del periodo (o del último periodo para la nota final).

### Consulta del apoderado (HU-69)

- Solo los hijos del usuario (padre, madre o apoderado vinculado, no fallecido) con matrícula MATRICULADO; mismo criterio que el horario del apoderado (HU-51). Otro estudiante o año da 404.
- Por curso: su docente, un **resumen referencial** del periodo (todos sus ítems con la regla del año, como el «Promedio general» del consolidado) y, por competencia, su aporte con los ítems y su nota.
- **Notas por competencia:** la nota oficial por periodo y la final; «Pendiente · 3 de 4 ítems» mientras falte registrar algo. Los ítems sin registrar se ven como «Por registrar».
- El año está **finalizado** si está CERRADO o ya terminó su último periodo: se muestra «Resultado del año» (competencias por nivel) y el botón «Descargar libreta final (PDF)».
- La libreta en PDF (`libreta-notas-<año>.pdf`) lista cada competencia con su área, sus periodos y la nota final en nivel de logro.

## API

Las rutas del docente usan el prefijo `/api/calificaciones/docente` y requieren rol Docente. Los reportes y la consulta del apoderado están al final de esta sección.

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/cursos` | Cursos y secciones asignados, parámetros del año y periodos con estado y conteos. |
| GET | `/cursos/{planCursoId}/secciones/{seccionId}/competencias` | Competencias seleccionadas en el plan del curso. |
| GET | `/cursos/{planCursoId}/secciones/{seccionId}/notas-competencia?competenciaIds=` | Nota oficial de las competencias del curso (todas o las elegidas, separadas por coma): parámetros del año, periodos, cursos de cada competencia con su peso y, por estudiante, la nota de cada periodo con los aportes y la nota final (`nota`, `nivel`, `logro`, `pendiente`, `calificadas`, `total`; cada aporte trae también `pendiente`, `calificadas` y `total`). 400 si una competencia no es del curso. |
| GET · POST | `/cursos/{planCursoId}/secciones/{seccionId}/periodos/{periodoId}/items` | Lista o crea ítems del periodo. |
| PUT · DELETE | `/items/{itemId}` | Edita o elimina un ítem. |
| GET · PUT | `/items/{itemId}/calificaciones` | Lista estudiantes con su nota y guarda el lote de calificaciones. |
| POST · GET · DELETE | `/items/{itemId}/calificaciones/{matriculaId}/evidencia` | Sube, descarga o elimina la evidencia. |
| GET | `/reportes?tipo=&planCursoId=&seccionId=&periodoId=&competenciaIds=&itemIds=&incluirPromedio=` | Con `tipo=COMPETENCIA`, genera el consolidado múltiple. `competenciaIds` e `itemIds` son UUID separados por coma; `incluirPromedio` vale `true` por defecto. Sin `itemIds` usa todos los ítems disponibles. Se mantiene `objetivoId` para clientes de una sola competencia y las variantes `ITEM` y `PERIODO`. |
| PUT | `/items/{itemId}/calificaciones/{matriculaId}` | HU-60: resuelve una celda con `{notaNumerica \| nivelLogro \| noEvaluado: true, motivo}`: modifica, registra fuera de plazo o marca «No evaluado». El motivo (10 a 500) es obligatorio salvo al registrar una nota nueva dentro del plazo. 400 por motivo, valor igual o de otra escala; 409 con el año cerrado o el periodo sin empezar. |
| GET | `/items/{itemId}/calificaciones/{matriculaId}/historial` | HU-60: historial de la nota, del cambio más reciente al más antiguo. |
| GET | `/reportes/exportar` | El mismo consolidado en CSV UTF-8. |
| GET | `/reportes/exportar-pdf` | El mismo consolidado como tabla PDF. Con `tipo=COMPETENCIA`, al final agrega «Nota de la competencia en el periodo»: la nota oficial de cada competencia **solo en ese periodo**, en nivel de logro (p. ej. «A · 15,6/20») y con su estado. No trae otros periodos ni la nota del año. El contexto lleva la fecha y hora de generación. Acepta los mismos filtros, valida la asignación del docente y descarga `consolidado-notas.pdf`. Es la ruta usada por la interfaz. |
| GET | `/cursos/{planCursoId}/secciones/{seccionId}/resumen-anual?competenciaIds=` y `/resumen-anual/pdf` | Resumen del año (opción «Todo el año»). Por competencia, dos grupos de columnas: «Aporte de <curso>» (cada periodo y el aporte final, referencial) y «Nota de la competencia» (cada periodo y la nota final). Claves `<competenciaId>:aporte:<periodoId>`, `:aporte:final`, `:oficial:<periodoId>` y `:oficial:final`; las finales traen `_estado` («A · Logro esperado» o «Pendiente · 3 de 4 ítems»); los periodos pendientes se ven como «Pendiente (3 de 4)». El PDF descarga `resumen-anual-notas.pdf` con la fecha y hora de generación. |

Reportes y apoderado (prefijo `/api/calificaciones`):

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| GET | `/reportes/anios` | Coordinador, Administrador | Años ACTIVO o CERRADO con parámetros, periodos y grados con sus cursos. |
| GET | `/reportes/grado?anioEscolarId=&grado=&planCursoId=&periodoId=` y `/reportes/grado/pdf` | Coordinador | HU-63. Sin `planCursoId`, nota oficial; sin `periodoId`, nota final. |
| GET | `/reportes/secciones/{seccionId}/notas` | Coordinador | Detalle de una sección: `{seccionId, anio, grado, seccion, notas}`, donde `notas` tiene la forma de `notas-competencia` del docente con todas las competencias del grado. 404 si la sección no existe o su año no ha empezado. |
| GET | `/reportes/matriculas/{matriculaId}/libreta` | Coordinador | La libreta en PDF de un estudiante, la misma del apoderado. 404 si la matrícula no existe. |
| GET | `/reportes/institucional?anioEscolarId=&periodoId=` y `/reportes/institucional/pdf` | Coordinador, Administrador | HU-64. |
| GET | `/apoderado/estudiantes` | Apoderado | HU-69: hijos matriculados con sus años. |
| GET | `/apoderado/estudiantes/{id}/notas?anio=` y `/apoderado/estudiantes/{id}/libreta?anio=` | Apoderado | HU-69: notas del año (sin `anio`, el más reciente) y libreta en PDF. |

Cada fila de `GET /items/{itemId}/calificaciones` trae `modificaciones`, `noEvaluado`, `motivoNoEvaluado` y `estado` (`PENDIENTE`, `CALIFICADO` o `NO_EVALUADO`); cada periodo de `GET /cursos` trae `modificacionHabilitada` (HU-60). El historial trae `noEvaluadoAnterior` y `noEvaluadoNuevo`. En HU-63 cada celda trae `pendientes`. En HU-64 cada distribución trae `calificaciones` (las completas, base de los porcentajes) y `porCompletar`; el reporte trae `sinCalcular` (grado y competencia) y todas las áreas, también las que aún no tienen calificaciones.

Las columnas del consolidado múltiple incluyen `competenciaId`, `competencia` y `promedio` para agruparlas en pantalla. Sus claves combinan competencia e ítem y el promedio general usa `resultado_general`; el nivel de logro de cada resultado se devuelve con el sufijo `_estado` (por ejemplo, «A · Logro esperado»). En la nota de la competencia el campo se llama `logro`. En CSV, cada encabezado de ítem o promedio de competencia incluye el nombre de la competencia para distinguir los ítems compartidos. El backend rechaza competencias ajenas al plan e ítems ajenos a la selección. La selección múltiple no requirió tablas ni migraciones adicionales; V15 corresponde al cambio de pesos opcionales.

En las solicitudes de alta y edición, `competencias[].pesoPorcentaje` es obligatorio solo con `PROMEDIO_PONDERADO`; para las demás reglas puede omitirse. En la respuesta puede ser `null`. Los clientes deben decidir si muestran porcentajes a partir de la regla del año, no de la presencia de un peso histórico.

## Código y pruebas

- Backend: `backend/src/main/java/com/galiledu/academico/` (`ServicioCalificaciones`, `ServicioNotasCompetencia`, `dominio/CalculoNotas`, `RepositorioCalificaciones`, `RepositorioPesosPlan`, `CalificacionController`, `ArchivoReporteCalificaciones` y DTO).
- Pruebas de la nota de la competencia: `dominio/CalculoNotasTests.java` (reglas, sin BD), `NotaCompetenciaIT.java` (API y pesos) y `components/__tests__/reporte-calificaciones.test.tsx` (tarjeta del docente).
- Frontend: `frontend/src/features/calificaciones/` y la ruta `frontend/src/app/routes/app/docente/calificaciones.tsx`.
- HU-60: `ServicioCalificaciones.modificarCalificacion` e `historial`, `RepositorioCalificaciones` y `components/dialogo-modificar-calificacion.tsx` (abierto desde `calificar-alumnos.tsx`).
- HU-63 y HU-64: `ServicioReportesNotas`, `RepositorioReportesNotas`, `dominio/ResumenNotas`, `ReporteNotasController`, `components/reportes-notas.tsx` y las rutas `coordinador/calificaciones.tsx` y `admin/calificaciones.tsx`. El PDF reutiliza `ArchivoReporteCalificaciones.pdfResumen`.
- HU-69: `ServicioNotasApoderado`, `CalificacionApoderadoController`, `components/calificaciones-apoderado.tsx` y la ruta `apoderado/calificaciones.tsx`. La pastilla del nivel es `NotaNivel` (`components/ui/tag`).
- Pruebas de las HU-60, 63, 64 y 69: `ModificacionCalificacionIT`, `ReportesYNotasApoderadoIT` (con `DatosNotasPrueba`), `dominio/ResumenNotasTests` y, en el frontend, `dialogo-modificar-calificacion.test.tsx`, `reportes-notas.test.tsx` y `calificaciones-apoderado.test.tsx`.
- Pruebas: `backend/src/test/java/com/galiledu/academico/ServicioCalificacionesTests.java`, `ReporteCompetenciasTests.java`, `ArchivoReporteCalificacionesTests.java`, `CalificacionesIT.java` y `frontend/src/features/calificaciones/` (`components/__tests__/` y `api/__tests__/exportar-reporte.test.ts`). Las pruebas PDF verifican texto, escala cualitativa, ausencia de promedios, descarga y permisos, paginación por estudiantes y columnas; generan una muestra visual en `backend/target/reporte-pdf-demo.pdf` y su PNG.

**Datos de prueba compartidos:** `backend/src/main/resources/db/datos-prueba/R__datos_prueba.sql` prepara a María Fernández (`U20260004`, contraseña común del repositorio) con **Álgebra, Aritmética y Geometría**, en **1.° A de secundaria de 2026**, y tres estudiantes ficticios matriculados: **Ana Lucía García López, Diego José Quispe Mendoza y Camila Sofía Torres Vargas**. Si un curso no tiene ítems en el IV bimestre, agrega dos prácticas con las competencias del plan: 50 % cada una en ponderado y peso nulo en las demás reglas. Las notas quedan disponibles para que el docente las registre.

La carga es idempotente: reutiliza los identificadores de personas y estudiantes de la demo local anterior, respeta las matrículas existentes y actualiza el correlativo solo al crear una nueva. Conserva asignaciones de otros docentes, ítems editados y calificaciones guardadas. No cambia parámetros de años iniciados y respeta la capacidad de la sección. La preparación se realiza únicamente mientras 2026 está activo.

`DatosPruebaDocenteIT` comprueba el acceso a los tres cursos y alumnos y que repetir el bloque no duplique datos ni sobrescriba una nota o ítem editado. `CalificacionesIT` utiliza 1.° B como escenario aislado dentro de su transacción. Los conteos del año 2026 ahora incluyen cinco alumnos matriculados: los dos anteriores y los tres de esta demo. Véase la [decisión de datos compartidos de María](../decisiones/2026-11-06-josezavaleta-datos-prueba-docente-maria.md).

**Demo local de las HU-60, 63, 64 y 69:** `backend/src/main/resources/db/demo/R__demo_calificaciones.sql`. Flyway lo carga desde `classpath:db/demo`, que está en las ubicaciones por defecto de `application.properties`, pero no en las pruebas de integración (`PruebaIntegracion` fija sus ubicaciones) ni en el despliegue (`docker-compose.yml` y `deploy/.env` no lo incluyen). Con 2026 activo, agrega:

- tres estudiantes ficticios en 1.° B (Joaquín Andrés García López, Valentina Rojas Huamán y Sebastián Flores Castillo), con María en Álgebra, Aritmética y Geometría;
- en 1.° A y 1.° B, dos ítems por curso del I al III bimestre (Práctica calificada 40 % y Examen bimestral 60 %) con notas, solo si el curso no tenía ítems en ese periodo;
- dos casos a propósito: a Diego José le falta el «Examen bimestral» de Geometría del III bimestre (sus competencias de Geometría quedan Pendiente y se puede registrar fuera de plazo), y Camila Sofía es «No evaluado» en la práctica de Álgebra del I bimestre;
- la apoderada **Carmen Rosa López Huamán**, madre de Ana Lucía (1.° A) y de Joaquín (1.° B).

| Para probar | Usuario | Contraseña |
| --- | --- | --- |
| HU-69 (apoderada con dos hijos) | `U20269001` | la común del repositorio (`Galiledu2026`) |
| HU-63 y HU-64 (Coordinador) | `U20260005` | igual |
| HU-64 (Administrador) | `U20260001` | igual |
| HU-60 (docente María Fernández) | `U20260004` | igual |

Es idempotente: no cambia notas, ítems ni usuarios que ya existan. Para no cargarlo en local, define `FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/datos-prueba`.

## Pendientes

- [ ] Confirmar con el PO los criterios de aceptación de las HU-60, 63, 64 y 69 (están en Notion; se implementaron según Figma y las decisiones registradas).
- [ ] Los datos de prueba no tienen un apoderado con un hijo calificado: `ReportesYNotasApoderadoIT` vincula a Luis Paredes con Ana Lucía solo dentro de la prueba. Para ver HU-69 con notas en la app, hace falta ese vínculo en `R__datos_prueba.sql`.
- [ ] La situación final del año (promovido, recuperación, permanece) sigue fuera de alcance: requiere un cierre de año que aún no existe.
