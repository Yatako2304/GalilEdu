# Configuración del año escolar

- **HU:** HU-74, HU-75, HU-23, HU-34, HU-44 (iteración 1)
- **Estado:** 🚧 en curso: las tablas y los datos de prueba ya están; el backend de HU-74, HU-44 y HU-34 está completo y el de HU-75 solo espera el historial de tutores; falta el backend de HU-23 y todo el frontend
- **Última actualización:** 2026-10-22

## Alcance

El Personal administrativo abre cada año escolar y deja lista la base sobre la que trabajan Matrícula, Pagos, Horarios y Calificaciones. Para cada año define:

- los periodos académicos (bimestres o trimestres);
- las secciones de cada grado, con sus tutores;
- las estructuras horarias;
- el periodo de matrícula;
- el tarifario.

Figma pone todo esto bajo **Configuraciones › Año escolar**; el tarifario está en **Pensiones › Configuración**. Solo un año puede estar activo a la vez.

**Figma frente a los RF de Notion:**
- HU-44 habla de "un único bloque horario para toda la institución", pero Figma muestra varias estructuras por año (EH-PRI-26 para primaria y EH-SEC-26 para secundaria). Manda Figma: se guardan varias estructuras y solo la **principal de cada nivel** se usa ([decisión](../decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md)). **Pendiente de comunicar al grupo.**
- RF-113 dice que la configura el Coordinador; en Figma está en el menú del Personal administrativo.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-74 | Año escolar y sus periodos | 🚧 | El año es PLANIFICADO, ACTIVO o CERRADO. El estado del periodo (PLANIFICADO, EN_CURSO, CERRADO) **no se guarda**: se calcula con las fechas y el estado del año ([decisión](../decisiones/2026-10-21-royal-22-estado-periodo-calculado.md)). Mientras el año está planificado se pueden editar sus fechas y cambiar el régimen, y los periodos se regeneran con fechas sugeridas. Al editar un periodo, el que ya empezó no cambia su inicio y el que ya terminó solo amplía su fecha límite de notas. Los docentes registran notas solo en un año ACTIVO, desde que empieza el periodo hasta su fecha límite. **Hecho:** listar, detalle, crear, año actual, editar fechas, cambiar régimen, requisitos para activar y editar periodo; activar y cerrar el año. **Falta:** reutilizar configuración (copiar secciones, estructuras y plan de evaluación). El botón Activar aún no está en Figma (CI-06) y "Cerrar año" solo aparece en CI-12. |
| HU-75 | Secciones por grado | 🚧 | Nombre único por grado y año. Capacidad de 1 a 45. Tutor obligatorio y cotutor opcional al asignarlos; un docente es tutor de una sola sección ACTIVA por año (el cotutor no cuenta). Capacidad no menor a los alumnos que ocupan vacante (RF-50). Eliminar solo sin matrículas ni cambios de sección; si no, inactivar. **Hecho:** backend (listar, crear, capacidad, tutores, eliminar, inactivar/reactivar, docentes elegibles y `listarSeccionesActivasConVacantes` para HU-23). **Falta:** historial de tutores y "Vigente desde" (con su migración) y el frontend. Eliminar una sección borra en cascada sus asignaciones de docentes por cursos (`academico.docente_seccion`); avisar al equipo de Académico. Inactivar no borra nada. |
| HU-44 | Estructura horaria | 🚧 | Un año guarda varias estructuras, cada una de un solo nivel (PRIMARIA o SECUNDARIA); solo la **principal** de cada nivel se usa. La primera de un nivel nace principal; al marcar otra, la anterior baja sola (con una advertencia); la principal no se puede desmarcar ni cambiar de nivel (para eso se elimina y se crea de nuevo) y, si quedan otras del nivel, tampoco se elimina. Solo se crea, edita o elimina con el año PLANIFICADO (409 si no) y la lista y el detalle traen `anioModificable`. Los horarios de bloques y recesos se **calculan**, no se guardan (`CalculadoraJornada`, dominio puro): la jornada, contando los recesos, termina como máximo a las 18:00. Bloques de 1 a 12 por día, de 20 a 120 min; recesos de 5 a 60 min, después de un bloque entre 1 y bloques-1. El código admite solo letras A-Z, números y guion (hasta 20), sin caracteres de control; la hora de entrada se trunca a minutos. Si reducir los bloques por semana de una principal (o marcar como principal una con menos, o crear la primera) deja a un grado del nivel con más horas planificadas (`academico.plan_curso`) que bloques, o si se elimina la única del nivel con planes, responde 409; aumentar nunca se rechaza y crear o editar devuelven `advertencias` cuando cambian los bloques por semana de la principal y hay planes. Los cambios del año se serializan con un bloqueo. **Hecho:** backend (listar, detalle, crear, vista previa, editar y eliminar); no hay migraciones nuevas. **Falta:** el frontend y las diferencias con Figma de los pendientes. Detalle y pendientes del grupo en la [decisión](../decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md). Para quien haga la HU-45: antes de permitir editar o eliminar una estructura, Horarios debe validar que no haya cursos asignados a sus bloques. |
| HU-23 | Fechas de matrícula regular | ⏳ | Un periodo por año. Una vez HABILITADO no se edita, solo se deshabilita. Las vacantes por sección son `seccion.capacidad_maxima`. |
| HU-34 | Tarifario del año | 🚧 | Backend hecho (P19). El tarifario es la tabla maestra del año; los compromisos, las cuotas y los descuentos de cada alumno son de Pagos ([decisión](../decisiones/2026-10-22-royal-22-tarifario-datos-maestros.md)). Pensión única para toda la institución (RF-70). **Confirmar** (no hay Guardar) guarda los montos y la morosidad, regenera el calendario con las fechas del año y deja el tarifario CONFIRMADO; se puede repetir mientras no sea de solo lectura. El calendario se genera, no se edita: una cuota por mes desde el inicio de clases hasta el fin del año, que vence el último día del mes y la última el día 15 (o el fin del año, si es anterior); la matrícula vence el último día del mes anterior al inicio de clases. Al editar las fechas del año (HU-74) el calendario se regenera si el tarifario aún no tiene compromisos; si los tiene, el GET avisa `calendarioDesactualizado`. Validaciones: matrícula ≥ 0, pensión > 0, ingreso opcional ≥ 0, montos con 2 decimales y hasta S/ 99,999,999.99, días de gracia 0–60, recargo y tope 0–100 con el tope obligatorio y recargo ≤ tope, y RF-71 (de 1 a 12 cuotas, dentro del año y sin vencimientos repetidos). Solo lectura (se calcula): año CERRADO (RF-73) o tarifario CONFIRMADO con compromisos. **Falta:** el frontend y que Pagos use `vigente` y `calendario`. |

## Pantallas (Figma)

| Pantalla | Frame en Figma | Ruta |
| --- | --- | --- |
| Años escolares | Personal Administrativo › "CI-01 · Año escolar — listado de años (HU-74)" | `/app/administrativo/anio-escolar` |
| Nuevo año | "Modal - Nuevo año escolar" | modal |
| Periodos | "CI-03 · Año escolar 2026 › Periodos (HU-74)", "CI-06 · Año escolar 2027 (planificado)", "Modal - Cambiar régimen de periodos" | pestaña |
| Secciones | "CI-09 · Secciones por grado y tutores (HU-75)", "Modal - Nueva sección" | pestaña |
| Estructura horaria | "CI-12 · Estructura horaria (HU-44)", "Modal - Nueva estructura horaria" | pestaña |
| Periodo de matrícula | "13 · Matrícula › Configuración de matrícula (HU-21, HU-22, HU-23)" | `/app/administrativo/matricula` |
| Tarifario | "P19 · Configuración › Tarifario y calendario 2027" | `/app/administrativo/pensiones` |

## Base de datos

- **Esquemas:** `configuracion`, `matricula`, `pagos` · **Migración:** `V4__configuracion_anio_escolar.sql`

| Tabla | Para qué |
| --- | --- |
| `configuracion.anio_escolar` | Año, fechas, régimen y estado. Un índice único impide que haya dos años ACTIVO. |
| `configuracion.periodo_academico` | Bimestres o trimestres, con su fecha límite de notas. Su estado (PLANIFICADO, EN_CURSO, CERRADO) **no se guarda**: se calcula con las fechas y el estado del año (V8). |
| `configuracion.grado` | Catálogo fijo con los 11 grados (`PRI-1`…`PRI-6`, `SEC-1`…`SEC-5`). Se llena en la migración. |
| `configuracion.seccion` | Sección de un grado en un año: nombre, capacidad máxima (= vacantes), tutor, cotutor y estado. |
| `configuracion.estructura_horaria` | Código (EH-SEC-26), nivel, si es la principal del nivel, días lectivos (`LUN`…`SAB`), hora de entrada, bloques por día y duración del bloque. Las secciones usan la estructura principal de su nivel (V7). Solo la principal de cada nivel se usa (índice único parcial, V7); Académico calcula con ella los bloques por semana. HU-44 no agregó migraciones. |
| `configuracion.receso` | Recesos de una estructura: después de qué bloque van y cuánto duran (uno por bloque como máximo). Se borran en cascada con su estructura. |
| `matricula.periodo_matricula` | Inicio y cierre de la matrícula regular, reglas (bloqueo por morosidad, extemporánea) y estado. |
| `pagos.tarifario` | Cuota de ingreso, matrícula, pensión, cantidad de cuotas y política de morosidad. |
| `pagos.cuota_tarifario` | Calendario de pensiones; no puede haber dos cuotas con el mismo vencimiento. |

**Datos de prueba** (`R__datos_prueba.sql`), con los valores de Figma:
- 2026 ACTIVO (bimestral, III Bimestre en curso), con 10 secciones de secundaria (1.° a 5.°, A y B). María Fernández (`U20260004`) es la tutora de 1.° A.
- Estructuras EH-PRI-26 y EH-SEC-26.
- 2027 PLANIFICADO (trimestral), con la matrícula habilitada del 15/09 al 31/10/2026 y el tarifario en configuración (S/ 450 la matrícula y 10 pensiones de S/ 620).

## Endpoints (HU-74, HU-34 y HU-44, rol Personal administrativo)

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/configuracion/anios-escolares` | Lista de años (arreglo simple) con régimen, estado y cantidad de periodos |
| GET | `/configuracion/anios-escolares/{id}` | Detalle del año con sus periodos |
| POST | `/configuracion/anios-escolares` | Crea un año PLANIFICADO `{anio, fechaInicio, fechaFin, regimen}` y genera sus periodos |
| GET | `/configuracion/anios-escolares/actual` | Card del año en curso (CI-01): periodo en curso, periodo con notas abiertas, próximo periodo, secciones, alumnos matriculados, estructuras y avance. 404 si no hay año activo |
| PUT | `/configuracion/anios-escolares/{id}` | Cambia las fechas `{fechaInicio, fechaFin}` de un año PLANIFICADO y regenera sus periodos (CI-06 · Editar) |
| PUT | `/configuracion/anios-escolares/{id}/regimen` | Cambia el régimen `{regimen}` de un año PLANIFICADO y regenera sus periodos (CI-06 · Cambiar régimen) |
| GET | `/configuracion/anios-escolares/{id}/requisitos` | Requisitos de "Antes de activar el año" (CI-06): 3 obligatorios y 2 recomendados |
| POST | `/configuracion/anios-escolares/{id}/activar` | Activa un año PLANIFICADO; 409 si otro año sigue activo o faltan requisitos obligatorios |
| POST | `/configuracion/anios-escolares/{id}/cerrar` | Cierra un año ACTIVO; 409 si aún no vence la fecha límite de notas del último periodo |
| PUT | `/configuracion/periodos-academicos/{id}` | Edita un periodo `{nombre, fechaInicio, fechaFin, fechaLimiteNotas}` (Modal · Editar periodo) |
| GET | `/configuracion/tarifarios?anioEscolarId=` | P19 del año: conceptos y montos, morosidad, calendario con total, `estado`, `existe`, `soloLectura` con su motivo y `calendarioDesactualizado`. Sin tarifario devuelve una propuesta (`existe = false`) con los valores del año anterior o vacíos. 404 si el año no existe |
| POST | `/configuracion/tarifarios/{anioEscolarId}/confirmar` | Confirmar tarifario `{montoCuotaIngreso, montoMatricula, montoPension, diasGracia, recargoPorcentaje, topeRecargoPorcentaje}`: lo crea o lo actualiza y queda CONFIRMADO. 400 si un dato no es válido; 409 si es de solo lectura; 404 si el año no existe |
| GET | `/configuracion/anios-escolares/{id}/estructuras-horarias` | CI-12: `{anioModificable, estructuras}`. Cada estructura trae código, descripción, nivel, `esPrincipal`, días lectivos, hora de entrada y de salida, bloques por día, duración, recesos y bloques por semana. 404 si el año no existe |
| GET | `/configuracion/estructuras-horarias/{id}` | CI-12 · Jornada semanal: lo anterior más `anioModificable` y la `jornada` calculada (filas BLOQUE y RECESO con inicio, fin y duración). 404 si no existe |
| POST | `/configuracion/anios-escolares/{id}/estructuras-horarias` | Crea (201) `{codigo, descripcion, nivel, esPrincipal, diasLectivos, horaEntrada, bloquesPorDia, duracionBloqueMin, recesos[{despuesDeBloque, duracionMin}]}` y responde `{estructura, advertencias}`. 400 si una regla de forma falla; 409 si el año no está planificado, el código ya existe o los bloques quedan por debajo de las horas planificadas; 404 si el año no existe |
| POST | `/configuracion/anios-escolares/{id}/estructuras-horarias/vista-previa` | Mismos campos que crear; no guarda. Responde 200 `{valida, errores, jornada}` (la jornada viene aunque pase de las 18:00). No mira el estado del año ni los planes; 404 si el año no existe |
| PUT | `/configuracion/estructuras-horarias/{id}` | Edita (sin `nivel`) y regenera los recesos; responde `{estructura, advertencias}`. `esPrincipal` es opcional: ausente conserva la marca, true marca como principal y false sobre la principal da 409. 400; 404; 409 si el año no está planificado, el código se repite o los planes no caben |
| DELETE | `/configuracion/estructuras-horarias/{id}` | Elimina (204). 409 si el año no está planificado, si es la principal y quedan otras del nivel, o si es la única del nivel y hay cursos planificados; 404 si no existe |

No hay endpoints para abrir o cerrar un periodo (su estado se calcula). Los de activar y cerrar el año existen, aunque el botón Activar aún no está en Figma (CI-06) y "Cerrar año" solo aparece en CI-12. `ServicioAnioEscolar` además tiene `periodoEnCurso(anioId)` y `registroNotasAbierto(periodoId)` para que Calificaciones consulte los periodos.

**Estructuras horarias (HU-44):** la vista previa (`valida`, `errores`, `jornada`) permite que el modal muestre la "Vista previa de la jornada" sin repetir las reglas en el frontend. `advertencias` avisa de que cambiar los bloques por semana de la principal obliga a revisar el plan de evaluación; no rechaza nada. Los horarios se calculan en minutos desde las 00:00 y no se guardan. Los planes se leen con la interfaz `PlanesDeEvaluacion` (`PlanesDeEvaluacionSql`, solo lectura, sin importar nada de `academico`).

**Para otros módulos (sin endpoint):** `ServicioTarifario.vigente(anioId)` devuelve los montos, el vencimiento de la matrícula y la morosidad, y `ServicioTarifario.calendario(anioId)` las cuotas de pensión (número, nombre y vencimiento) con su monto. Los dos devuelven vacío si el año no tiene un tarifario CONFIRMADO. Pagos puede usarlos en lugar de leer `pagos.tarifario` y `pagos.cuota_tarifario` por SQL.

**Cómo se generan los periodos:** BIMESTRAL = 4 y TRIMESTRAL = 3, numerados desde 1 ("I Bimestre"…). Los días del año se reparten en partes iguales (los sobrantes van a los primeros periodos); quedan seguidos, sin cruzarse, y el último termina en `fechaFin`. La fecha límite de notas es `fin + 5 días` (`PeriodoAcademico.DIAS_LIMITE_NOTAS`, **pendiente de confirmar** con el colegio). Un año necesita al menos 2 días por periodo (8 bimestral, 6 trimestral).

## Código

- Backend: `backend/src/main/java/com/galiledu/configuracion/` (`AnioEscolar`, `PeriodoAcademico`, `ServicioAnioEscolar`, `ConsultasConfiguracion`, `AnioEscolarController`, `PeriodoAcademicoController`; HU-75: `Seccion`, `ServicioSecciones`, `SeccionController`; HU-34: `Tarifario`, `CuotaTarifario`, `CalendarioCuotas`, `ServicioTarifario`, `ConsultasTarifario` y `TarifarioController`; HU-44: `EstructuraHoraria`, `Receso`, `DefinicionEstructura`, `CalculadoraJornada`, `ServicioEstructurasHorarias`, `PlanesDeEvaluacion` con `PlanesDeEvaluacionSql`, `EstructuraHorariaRepositorio` y `EstructuraHorariaController`). El "hoy" sale del bean `Clock` de `compartido/RelojConfig` (America/Lima).
- Frontend: `frontend/src/features/configuracion/` (por crear)
- Pruebas: `backend/src/test/java/com/galiledu/configuracion/` (`AnioEscolarIT` y `PeriodoAcademicoIT` la API, `ServicioAnioEscolarIT` el servicio, `ConsultasConfiguracionIT` las consultas, `dominio/AnioEscolarTests` las reglas del dominio y `EsquemaConfiguracionIT` las reglas de la BD). HU-34: `dominio/CalendarioCuotasTests` y `dominio/TarifarioTests` (reglas), `TarifarioRepositorioIT` (mapeo), `ServicioTarifarioIT` (servicio) y `TarifarioIT` (API). HU-44: `dominio/CalculadoraJornadaTests`, `dominio/EstructuraHorariaTests` y `dominio/EntradasHostilesEstructuraHorariaTests` (reglas), `aplicacion/ServicioEstructurasHorariasTests` (mensajes de la BD con mocks), `EstructuraHorariaRepositorioIT` (mapeo y recesos), `PlanesDeEvaluacionSqlIT` (lectura de planes), `ServicioEstructurasHorariasIT` (servicio), `EstructuraHorariaIT` (API) y `EstructuraHorariaEntradasHostilesIT` y `EstructuraHorariaConcurrenciaIT` (entradas hostiles y concurrencia; esta última sin transacción de prueba). Las IT que dependen de la fecha extienden `PruebaIntegracionConfiguracion`, que fija el reloj en 15/09/2026.

## Pendientes

- [ ] HU-74: reutilizar configuración (copiar secciones, estructuras y plan de evaluación).
- [ ] Backend de HU-23, y el frontend de las 5 HU.
- [ ] HU-75: historial de tutores y "Vigente desde" (con su migración).
- [ ] HU-44: comunicar al grupo la interpretación de "un único bloque horario" como "una principal por nivel" ([decisión](../decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md)).
- [ ] HU-44 (frontend), diferencias con Figma: el modal no tiene **Nivel** ni **Estructura principal** y la BD los exige (se agregan un selector y una casilla); CI-12 no marca cuál es la principal (el dato viene en `esPrincipal`); no dibuja las acciones **Editar** y **Eliminar**; y muestra **"+ Nueva"** con el año activo, que debe quedar deshabilitado (se usa `anioModificable`).
- [ ] HU-44: la activación del año no exige una principal por nivel; solo cuenta cuántas estructuras hay (`ServicioAnioEscolar`, sin cambios). Un año puede activarse con una estructura de un solo nivel.
- [ ] HU-45 (Horarios): antes de permitir editar o eliminar una estructura, Horarios debe validar que no haya cursos asignados a sus bloques.
- [ ] **Pendiente del grupo (a):** `ManejadorErrores` devuelve 500 en lugar de 400, 404, 405 y 415 (UUID mal escrito en la ruta, ruta inexistente, método no permitido, `Content-Type` incorrecto, y un rol sin permiso con un UUID mal escrito). Corrección propuesta: un handler para `MethodArgumentTypeMismatchException` (400) y otro para las excepciones que implementan `ErrorResponse`, antes del genérico. Hay 5 pruebas con `@Disabled` en `EstructuraHorariaEntradasHostilesIT` que se activan al corregirlo.
- [ ] **Pendiente del grupo (b):** la configuración de Jackson acepta números con decimales (5.5 pasa a 5), números como texto (`"6"`) y claves duplicadas (gana la última). Propuesta: desactivar la coerción de decimales y de textos a número, y activar la detección estricta de claves duplicadas.
- [ ] **Pendiente del grupo (c):** no hay límite de tamaño de cuerpo en el servidor; un cuerpo de 10 MB se lee entero antes de rechazarlo.
- [ ] HU-44: huecos sin corregir y puntos "no verificado hoy" (rollback tras bajar la principal; vista previa con jornada desde 23:59, que muestra "58:59"): ver la [decisión](../decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md).
- [ ] HU-34: Pagos debe exigir el tarifario CONFIRMADO en `ServicioCuotaMatricula` (hoy solo `ServicioCronograma` lo exige) y puede pasar a usar `ServicioTarifario.vigente` y `calendario`. Mientras tanto, un tarifario EN_CONFIGURACION con un compromiso se puede confirmar.
- [ ] UX de P19 por reevaluar (iteración 2): con un tarifario ya confirmado, el botón sigue diciendo 'Confirmar tarifario' aunque solo guarda cambios; Figma solo dibuja el estado 'En configuración'. Evaluar con el diseño: texto 'Guardar cambios' para el tarifario confirmado, un borrador con autoguardado, el aviso de cambios sin guardar al salir o al cambiar de año, y la vista de solo lectura (año cerrado o con compromisos).
- [ ] Confirmar con el colegio los 5 días entre el fin del periodo y la fecha límite de notas.
- [ ] `estudiantes.estudiante` guarda el nivel y el grado como texto ("1.° grado"). Cuando Matrícula asigne secciones, se enlazará con `configuracion.grado`.
- [ ] Los documentos requeridos de la matrícula (HU-21/22) no están en esta migración.
