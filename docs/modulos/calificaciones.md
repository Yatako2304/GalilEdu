# Calificaciones del docente

- **HU:** HU-57, HU-59 y HU-62
- **Estado:** ✅ terminado: backend, frontend, pruebas automatizadas y validación manual implementados
- **Última actualización:** 2026-11-06

## Alcance

El Docente trabaja únicamente con los cursos y secciones que tiene asignados en el plan de evaluación. Desde una sola pantalla puede:

- registrar ítems de evaluación con un criterio, tipo y competencias; el peso se solicita solo en promedio ponderado;
- calificar a cada estudiante matriculado y adjuntar una evidencia pedagógica;
- consultar resultados por ítem, competencia o periodo y descargar el consolidado como una tabla en PDF.

Aunque los frames revisados muestran «Personal administrativo» o «Administrador» en la barra lateral, están ubicados en la página `🌟 Docente` de Figma y corresponden al flujo del usuario Docente. La ruta implementada es `/app/docente/calificaciones` y la API exige el rol `DOCENTE`.

La escala, la regla de cálculo y la consolidación no se eligen en esta pantalla. Se leen de `configuracion.anio_escolar`, según la [decisión de parámetros de evaluación por año](../decisiones/2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md).

## Historias de usuario

| HU | Qué | Estado | Reglas principales |
| --- | --- | --- | --- |
| HU-57 | Registro de criterios e ítems de evaluación de un curso | ✅ | El ítem pertenece a un curso, sección y periodo. Tiene nombre, criterio, tipo y una o más competencias del plan. Solo en promedio ponderado requiere pesos de 0,01 a 100 y la suma por competencia no supera 100 %. |
| HU-59 | Registro de calificación de un ítem | ✅ | Una nota por estudiante matriculado e ítem. VIGESIMAL admite 0–20; CUALITATIVA admite AD, A, B o C. Puede adjuntar un PDF, DOC, DOCX, JPG o PNG de hasta 10 MB. |
| HU-62 | Consolidado de notas de un curso | ✅ | Selecciona una o varias competencias y sus ítems; muestra resultados por competencia y promedio general de la selección con la regla del año; descarga una tabla en PDF. |

## Pantallas (Figma)

| Flujo | Frames revisados | Implementación |
| --- | --- | --- |
| Configurar ítems | `738:20655` y modal `738:20344` | Botón **Configuración de ítems** → **Configurar ítems**, con vistas **Ítems de evaluación** e **Ítems por competencia** |
| Calificar alumnos | `631:14676`, con estados previos `629:11540` y `629:11893` | Botón **Configuración de ítems** → pestaña **Calificar alumnos** |
| Consolidar y exportar notas | `648:17853` y detalle `631:16187` | Botón **Consolidado de notas**, con los filtros, selección múltiple de competencias e ítems, interruptor de promedio final y tabla agrupada |

Debajo del encabezado **Calificaciones**, dos pestañas con apariencia de botón separan **Configuración de ítems** y **Consolidado de notas**, según el ajuste solicitado por josezavaleta. La primera contiene los filtros de curso y sección, las tarjetas de periodos y las pestañas **Configurar ítems** y **Calificar alumnos**; la segunda muestra únicamente el flujo del consolidado.

La selección múltiple de competencias y el promedio general amplían el selector individual de Figma por petición expresa de josezavaleta. Se conservan los componentes y la estructura visual del consolidado; véase la [decisión del consolidado múltiple](../decisiones/2026-11-06-josezavaleta-consolidado-multiples-competencias.md).

Por solicitud posterior de josezavaleta, el botón del reporte se llama **Descargar PDF** y descarga únicamente ese formato. El PDF incluye curso, grado, sección, año y periodo, la escala y regla anual, encabezados agrupados por competencia y resultados con estado. Usa A4 horizontal y repite los encabezados al paginar. Si hay muchas columnas, las divide en bloques legibles; repite los estudiantes y el promedio general de toda la selección en cada bloque. Véase la [decisión del reporte PDF](../decisiones/2026-11-06-josezavaleta-reporte-notas-pdf.md).

La pantalla conserva los filtros separados de año, curso, grado, sección y área, las tarjetas de periodos y los componentes del catálogo UI del repositorio. Las tablas usan columnas de ancho estable para evitar desplazamientos entre encabezados y celdas. En promedio ponderado, la vista por competencia muestra los porcentajes y una etiqueta verde al completar 100 % o roja si está incompleta; al alcanzar 100 %, la competencia deja de estar disponible para nuevos ítems. En promedio simple y reglas cualitativas, se ocultan el campo y la columna de peso y los indicadores de porcentajes, y no se bloquean altas por completar 100 %. Esta adaptación del diseño responde a la solicitud expresa de josezavaleta; véase la [decisión de pesos solo en ponderado](../decisiones/2026-11-06-josezavaleta-pesos-solo-promedio-ponderado.md).

## Base de datos

- **Esquema:** `academico`
- **Migraciones:** `V11__calificaciones_docente.sql` y `V15__calificaciones_peso_solo_ponderado.sql`

| Tabla | Para qué | Reglas destacadas |
| --- | --- | --- |
| `academico.item_evaluacion` | Ítem de un curso, sección y periodo. | Nombre único dentro de curso, sección y periodo; tipo cerrado; claves compuestas garantizan el mismo año y grado. |
| `academico.item_competencia` | Competencias y peso del ítem. | La competencia debe pertenecer a `academico.plan_competencia`. V15 permite peso `NULL` para reglas sin ponderación; los porcentajes no nulos conservan el rango mayor que 0 y menor o igual que 100. El servicio exige peso de 0,01 a 100 únicamente para promedio ponderado. |
| `academico.calificacion_item` | Nota de un estudiante para un ítem. | Una por matrícula e ítem; exactamente un valor numérico o cualitativo; la matrícula debe pertenecer a la sección del ítem. |
| `academico.evidencia_calificacion` | Archivo de evidencia de la nota. | Una por calificación; MIME permitido y tamaño entre 1 byte y 10 MB. |

V11 agrega restricciones `UNIQUE` compuestas a `academico.plan_curso`, `configuracion.seccion`, `configuracion.periodo_academico` y `matricula.matricula`. Se usan como destinos de claves foráneas y no cambian los datos existentes.

V15 elimina únicamente `NOT NULL` de `academico.item_competencia.peso_porcentaje` y documenta su uso. Conserva `ck_item_competencia_peso`, las claves e índices y todos los datos existentes. Las asociaciones nuevas o editadas de años sin ponderación guardan `NULL`; los porcentajes históricos conservados se ignoran según la regla anual. Flyway aplica la migración al arrancar el backend.

## Reglas de aplicación

- El backend valida la asignación en `academico.docente_seccion` en cada operación; conocer un identificador de otro curso no concede acceso.
- Los datos históricos se consultan, pero solo se crean o modifican ítems, notas y evidencias cuando el año está `ACTIVO` y la fecha está entre el inicio del periodo y `fecha_limite_notas`, ambos inclusive.
- No se elimina un ítem que ya tiene calificaciones.
- Solo con `PROMEDIO_PONDERADO`, cada asociación exige un porcentaje de 0,01 a 100 y la suma dentro del mismo curso, sección y periodo no puede superar 100 %. La validación se repite en el backend y serializa las altas y ediciones del mismo plan de curso para evitar sobreasignaciones simultáneas. En las demás reglas, el backend ignora porcentajes enviados por clientes anteriores, guarda `NULL` y conserva la validación de competencias válidas y no repetidas.
- Una calificación vacía elimina la nota y su evidencia. Para adjuntar evidencia primero debe existir la calificación.
- `PROMEDIO_SIMPLE` promedia los ítems calificados. `PROMEDIO_PONDERADO` usa los pesos de los ítems calificados y normaliza por la suma de esos pesos.
- `NIVEL_MAS_FRECUENTE` elige el nivel más repetido; si hay empate, prevalece el nivel de logro mayor. `ULTIMO_NIVEL` toma el último ítem registrado.
- El consolidado agrupa las columnas por las competencias seleccionadas. Los ítems compartidos aparecen en cada grupo al que pertenecen, pero se ofrecen una sola vez en el selector de ítems. La selección de competencias determina los ítems disponibles.
- **Incluir promedio final** agrega el resultado de cada competencia y, si se seleccionó más de una, el promedio general del curso para la selección actual, con estado Aprobado, Desaprobado o Pendiente. Con una sola competencia se muestra un único promedio. El interruptor desactivado oculta todos los promedios.
- La regla se obtiene de `configuracion.anio_escolar.regla_nota`, también para el promedio general. El promedio simple cuenta cada ítem calificado una sola vez. En el ponderado, el peso de un ítem compartido es la suma de sus pesos en las competencias seleccionadas: `suma(nota × peso) / suma(peso)`. Se excluyen los ítems sin nota, se incluye el cero y se redondea a dos decimales al final; sin notas se muestra `—` y estado Pendiente.
- Para la escala cualitativa, el resultado general usa los ítems únicos con la misma regla anual de frecuencia o último nivel. La consolidación entre periodos sigue siendo un parámetro distinto y no interviene en este reporte de un solo periodo.
- Para la escala vigesimal, el estado Aprobado empieza en el rango A de la tabla de conversión vigente en la fecha de inicio del periodo. Para la escala cualitativa, A y AD son aprobatorios.
- Las descargas usan el tipo MIME real: `application/pdf` para el consolidado y el tipo guardado para evidencias. La ruta CSV anterior se conserva para compatibilidad con clientes existentes; el frontend solo ofrece PDF.

## API

Todas las rutas usan el prefijo `/api/calificaciones/docente` y requieren rol Docente.

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/cursos` | Cursos y secciones asignados, parámetros del año y periodos con estado y conteos. |
| GET | `/cursos/{planCursoId}/secciones/{seccionId}/competencias` | Competencias seleccionadas en el plan del curso. |
| GET · POST | `/cursos/{planCursoId}/secciones/{seccionId}/periodos/{periodoId}/items` | Lista o crea ítems del periodo. |
| PUT · DELETE | `/items/{itemId}` | Edita o elimina un ítem. |
| GET · PUT | `/items/{itemId}/calificaciones` | Lista estudiantes con su nota y guarda el lote de calificaciones. |
| POST · GET · DELETE | `/items/{itemId}/calificaciones/{matriculaId}/evidencia` | Sube, descarga o elimina la evidencia. |
| GET | `/reportes?tipo=&planCursoId=&seccionId=&periodoId=&competenciaIds=&itemIds=&incluirPromedio=` | Con `tipo=COMPETENCIA`, genera el consolidado múltiple. `competenciaIds` e `itemIds` son UUID separados por coma; `incluirPromedio` vale `true` por defecto. Sin `itemIds` usa todos los ítems disponibles. Se mantiene `objetivoId` para clientes de una sola competencia y las variantes `ITEM` y `PERIODO`. |
| GET | `/reportes/exportar` | El mismo consolidado en CSV UTF-8. |
| GET | `/reportes/exportar-pdf` | El mismo consolidado como tabla PDF; acepta los mismos filtros, valida la asignación del docente y descarga `consolidado-notas.pdf`. Es la ruta usada por la interfaz. |

Las columnas del consolidado múltiple incluyen `competenciaId`, `competencia` y `promedio` para agruparlas en pantalla. Sus claves combinan competencia e ítem y el promedio general usa `resultado_general`; el estado de cada resultado se devuelve con el sufijo `_estado`. En CSV, cada encabezado de ítem o promedio de competencia incluye el nombre de la competencia para distinguir los ítems compartidos. El backend rechaza competencias ajenas al plan e ítems ajenos a la selección. La selección múltiple no requirió tablas ni migraciones adicionales; V15 corresponde al cambio de pesos opcionales.

En las solicitudes de alta y edición, `competencias[].pesoPorcentaje` es obligatorio solo con `PROMEDIO_PONDERADO`; para las demás reglas puede omitirse. En la respuesta puede ser `null`. Los clientes deben decidir si muestran porcentajes a partir de la regla del año, no de la presencia de un peso histórico.

## Código y pruebas

- Backend: `backend/src/main/java/com/galiledu/academico/` (`ServicioCalificaciones`, `RepositorioCalificaciones`, `CalificacionController`, `ArchivoReporteCalificaciones` y DTO).
- Frontend: `frontend/src/features/calificaciones/` y la ruta `frontend/src/app/routes/app/docente/calificaciones.tsx`.
- Pruebas: `backend/src/test/java/com/galiledu/academico/ServicioCalificacionesTests.java`, `ReporteCompetenciasTests.java`, `ArchivoReporteCalificacionesTests.java`, `CalificacionesIT.java` y `frontend/src/features/calificaciones/` (`components/__tests__/` y `api/__tests__/exportar-reporte.test.ts`). Las pruebas PDF verifican texto, escala cualitativa, ausencia de promedios, descarga y permisos, paginación por estudiantes y columnas; generan una muestra visual en `backend/target/reporte-pdf-demo.pdf` y su PNG.

**Datos de prueba:** María Fernández (`U20260004`, contraseña común del repositorio) dicta Álgebra en 1.° A. Las pruebas de integración crean sus propios estudiantes, ítems y calificaciones dentro de una transacción para no alterar los conteos ni las secciones compartidas del año escolar.

## Fuera de alcance

- [ ] Las HU-60, HU-63, HU-64 y HU-69 quedan fuera de este alcance.
