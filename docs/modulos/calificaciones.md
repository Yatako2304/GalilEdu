# Calificaciones del docente

- **HU:** HU-57, HU-59 y HU-62
- **Estado:** ✅ terminado: backend, frontend, pruebas automatizadas y validación manual implementados
- **Última actualización:** 2026-11-05

## Alcance

El Docente trabaja únicamente con los cursos y secciones que tiene asignados en el plan de evaluación. Desde una sola pantalla puede:

- registrar ítems de evaluación con un criterio, tipo, competencias y peso;
- calificar a cada estudiante matriculado y adjuntar una evidencia pedagógica;
- consultar resultados por ítem, competencia o periodo y exportarlos en CSV.

Aunque los frames revisados muestran «Personal administrativo» o «Administrador» en la barra lateral, están ubicados en la página `🌟 Docente` de Figma y corresponden al flujo del usuario Docente. La ruta implementada es `/app/docente/calificaciones` y la API exige el rol `DOCENTE`.

La escala, la regla de cálculo y la consolidación no se eligen en esta pantalla. Se leen de `configuracion.anio_escolar`, según la [decisión de parámetros de evaluación por año](../decisiones/2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md).

## Historias de usuario

| HU | Qué | Estado | Reglas principales |
| --- | --- | --- | --- |
| HU-57 | Registro de criterios e ítems de evaluación de un curso | ✅ | El ítem pertenece a un curso, sección y periodo. Tiene nombre, criterio, tipo y una o más competencias del plan; cada asociación lleva un peso de 0,01 a 100 y la suma por competencia no supera 100 %. |
| HU-59 | Registro de calificación de un ítem | ✅ | Una nota por estudiante matriculado e ítem. VIGESIMAL admite 0–20; CUALITATIVA admite AD, A, B o C. Puede adjuntar un PDF, DOC, DOCX, JPG o PNG de hasta 10 MB. |
| HU-62 | Consolidado de notas de un curso | ✅ | Selecciona los ítems de una competencia, muestra sus notas y, cuando se solicita, calcula el promedio final con la regla del año; permite exportar CSV. |

## Pantallas (Figma)

| Flujo | Frames revisados | Implementación |
| --- | --- | --- |
| Configurar ítems | `738:20655` y modal `738:20344` | Botón **Configuración de ítems** → **Configurar ítems**, con vistas **Ítems de evaluación** e **Ítems por competencia** |
| Calificar alumnos | `631:14676`, con estados previos `629:11540` y `629:11893` | Botón **Configuración de ítems** → pestaña **Calificar alumnos** |
| Consolidar y exportar notas | `648:17853` y detalle `631:16187` | Botón **Consolidado de notas**, con los filtros, selección múltiple de ítems, interruptor de promedio final y tabla agrupada de Figma |

Debajo del encabezado **Calificaciones**, dos pestañas con apariencia de botón separan **Configuración de ítems** y **Consolidado de notas**, según el ajuste solicitado por josezavaleta. La primera contiene los filtros de curso y sección, las tarjetas de periodos y las pestañas **Configurar ítems** y **Calificar alumnos**; la segunda muestra únicamente el flujo del consolidado.

La pantalla conserva los filtros separados de año, curso, grado, sección y área, las tarjetas de periodos y los componentes del catálogo UI del repositorio. Las tablas usan columnas de ancho estable para evitar desplazamientos entre encabezados y celdas. La vista por competencia agrupa sus ítems, muestra el peso de cada uno e indica con una etiqueta verde si la suma alcanza 100 % o con una etiqueta roja si todavía está incompleta. Cuando una competencia alcanza 100 %, deja de estar disponible para nuevos ítems.

## Base de datos

- **Esquema:** `academico`
- **Migración:** `V11__calificaciones_docente.sql`

| Tabla | Para qué | Reglas destacadas |
| --- | --- | --- |
| `academico.item_evaluacion` | Ítem de un curso, sección y periodo. | Nombre único dentro de curso, sección y periodo; tipo cerrado; claves compuestas garantizan el mismo año y grado. |
| `academico.item_competencia` | Competencias y peso del ítem. | La competencia debe pertenecer a `academico.plan_competencia`; peso mayor que 0 y menor o igual que 100. |
| `academico.calificacion_item` | Nota de un estudiante para un ítem. | Una por matrícula e ítem; exactamente un valor numérico o cualitativo; la matrícula debe pertenecer a la sección del ítem. |
| `academico.evidencia_calificacion` | Archivo de evidencia de la nota. | Una por calificación; MIME permitido y tamaño entre 1 byte y 10 MB. |

V11 agrega restricciones `UNIQUE` compuestas a `academico.plan_curso`, `configuracion.seccion`, `configuracion.periodo_academico` y `matricula.matricula`. Se usan como destinos de claves foráneas y no cambian los datos existentes.

## Reglas de aplicación

- El backend valida la asignación en `academico.docente_seccion` en cada operación; conocer un identificador de otro curso no concede acceso.
- Los datos históricos se consultan, pero solo se crean o modifican ítems, notas y evidencias cuando el año está `ACTIVO` y la fecha está entre el inicio del periodo y `fecha_limite_notas`, ambos inclusive.
- No se elimina un ítem que ya tiene calificaciones.
- La suma de los pesos de una competencia dentro del mismo curso, sección y periodo no puede superar 100 %. La validación se repite en el backend y serializa las altas y ediciones del mismo plan de curso para evitar sobreasignaciones simultáneas.
- Una calificación vacía elimina la nota y su evidencia. Para adjuntar evidencia primero debe existir la calificación.
- `PROMEDIO_SIMPLE` promedia los ítems calificados. `PROMEDIO_PONDERADO` usa los pesos de los ítems calificados y normaliza por la suma de esos pesos.
- `NIVEL_MAS_FRECUENTE` elige el nivel más repetido; si hay empate, prevalece el nivel de logro mayor. `ULTIMO_NIVEL` toma el último ítem registrado.
- El consolidado muestra una columna por cada ítem seleccionado. Si se activa **Incluir promedio final**, agrega la última columna con el promedio y el estado Aprobado, Desaprobado o Pendiente.
- Para la escala vigesimal, el estado Aprobado empieza en el rango A de la tabla de conversión vigente en la fecha de inicio del periodo. Para la escala cualitativa, A y AD son aprobatorios.
- Las descargas usan el tipo MIME real del archivo: CSV para reportes y el tipo guardado para evidencias.

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
| GET | `/reportes?tipo=&planCursoId=&seccionId=&periodoId=&objetivoId=&itemIds=&incluirPromedio=` | Genera el consolidado de los ítems seleccionados para una competencia. Las variantes `ITEM` y `PERIODO` se mantienen disponibles en el servicio. |
| GET | `/reportes/exportar` | El mismo consolidado en CSV UTF-8. |

## Código y pruebas

- Backend: `backend/src/main/java/com/galiledu/academico/` (`ServicioCalificaciones`, `RepositorioCalificaciones`, `CalificacionController` y DTO).
- Frontend: `frontend/src/features/calificaciones/` y la ruta `frontend/src/app/routes/app/docente/calificaciones.tsx`.
- Pruebas: `backend/src/test/java/com/galiledu/academico/ServicioCalificacionesTests.java`, `CalificacionesIT.java` y `frontend/src/features/calificaciones/components/__tests__/calificaciones-docente.test.tsx`.

**Datos de prueba:** María Fernández (`U20260004`, contraseña común del repositorio) dicta Álgebra en 1.° A. Las pruebas de integración crean sus propios estudiantes, ítems y calificaciones dentro de una transacción para no alterar los conteos ni las secciones compartidas del año escolar.

## Fuera de alcance

- [ ] Las HU-60, HU-63, HU-64 y HU-69 quedan fuera de este alcance.
