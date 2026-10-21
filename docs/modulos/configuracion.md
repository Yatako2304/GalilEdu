# Configuración del año escolar

- **HU:** HU-74, HU-75, HU-23, HU-34, HU-44 (iteración 1)
- **Estado:** 🚧 en curso: las tablas y los datos de prueba ya están; el backend de HU-74 está completo salvo los endpoints de activar y cerrar el año (pendientes de confirmar con el equipo); falta el resto del backend y el frontend
- **Última actualización:** 2026-10-21

## Alcance

El Personal administrativo abre cada año escolar y deja lista la base sobre la que trabajan Matrícula, Pagos, Horarios y Calificaciones. Para cada año define:

- los periodos académicos (bimestres o trimestres);
- las secciones de cada grado, con sus tutores;
- las estructuras horarias;
- el periodo de matrícula;
- el tarifario.

Figma pone todo esto bajo **Configuraciones › Año escolar**; el tarifario está en **Pensiones › Configuración**. Solo un año puede estar activo a la vez.

**Figma frente a los RF de Notion:**
- HU-44 habla de "un único bloque horario para toda la institución", pero Figma muestra varias estructuras por año (EH-PRI-26 para primaria y EH-SEC-26 para secundaria). Manda Figma.
- RF-113 dice que la configura el Coordinador; en Figma está en el menú del Personal administrativo.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-74 | Año escolar y sus periodos | 🚧 | El año es PLANIFICADO, ACTIVO o CERRADO. El estado del periodo (PLANIFICADO, EN_CURSO, CERRADO) **no se guarda**: se calcula con las fechas y el estado del año ([decisión](../decisiones/2026-10-21-royal-22-estado-periodo-calculado.md)). Mientras el año está planificado se pueden editar sus fechas y cambiar el régimen, y los periodos se regeneran con fechas sugeridas. Al editar un periodo, el que ya empezó no cambia su inicio y el que ya terminó solo amplía su fecha límite de notas. Los docentes registran notas solo en un año ACTIVO, desde que empieza el periodo hasta su fecha límite. **Hecho:** listar, detalle, crear, año actual, editar fechas, cambiar régimen, requisitos para activar y editar periodo; activar y cerrar existen en el servicio, sin endpoint. **Falta:** endpoints de activar y cerrar (pendiente de confirmar con el equipo) y reutilizar configuración. |
| HU-75 | Secciones por grado | ⏳ | Nombre único por grado y año. Capacidad de 1 a 45. Tutor y cotutor opcionales. |
| HU-44 | Estructura horaria | ⏳ | Los horarios de bloques y recesos se **calculan**, no se guardan. La jornada termina como máximo a las 18:00 (lo valida el servicio). |
| HU-23 | Fechas de matrícula regular | ⏳ | Un periodo por año. Una vez HABILITADO no se edita, solo se deshabilita. Las vacantes por sección son `seccion.capacidad_maxima`. |
| HU-34 | Tarifario del año | ⏳ | Pensión única para toda la institución (RF-70). Las validaciones de RF-71 (cuotas dentro del año y cantidad igual a la declarada) las hace el servicio. Si el año está CERRADO, el tarifario es de solo lectura (RF-73). |

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
| `configuracion.estructura_horaria` | Código (EH-SEC-26), nivel, si es la principal del nivel, días lectivos (`LUN`…`SAB`), hora de entrada, bloques por día y duración del bloque. Las secciones usan la estructura principal de su nivel (V7). |
| `configuracion.receso` | Recesos de una estructura: después de qué bloque van y cuánto duran. |
| `matricula.periodo_matricula` | Inicio y cierre de la matrícula regular, reglas (bloqueo por morosidad, extemporánea) y estado. |
| `pagos.tarifario` | Cuota de ingreso, matrícula, pensión, cantidad de cuotas y política de morosidad. |
| `pagos.cuota_tarifario` | Calendario de pensiones; no puede haber dos cuotas con el mismo vencimiento. |

**Datos de prueba** (`R__datos_prueba.sql`), con los valores de Figma:
- 2026 ACTIVO (bimestral, III Bimestre en curso), con 10 secciones de secundaria (1.° a 5.°, A y B). María Fernández (`U20260004`) es la tutora de 1.° A.
- Estructuras EH-PRI-26 y EH-SEC-26.
- 2027 PLANIFICADO (trimestral), con la matrícula habilitada del 15/09 al 31/10/2026 y el tarifario en configuración (S/ 450 la matrícula y 10 pensiones de S/ 620).

## Endpoints (HU-74, rol Personal administrativo)

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/configuracion/anios-escolares` | Lista de años (arreglo simple) con régimen, estado y cantidad de periodos |
| GET | `/configuracion/anios-escolares/{id}` | Detalle del año con sus periodos |
| POST | `/configuracion/anios-escolares` | Crea un año PLANIFICADO `{anio, fechaInicio, fechaFin, regimen}` y genera sus periodos |
| GET | `/configuracion/anios-escolares/actual` | Card del año en curso (CI-01): periodo en curso, periodo con notas abiertas, próximo periodo, secciones, alumnos matriculados, estructuras y avance. 404 si no hay año activo |
| PUT | `/configuracion/anios-escolares/{id}` | Cambia las fechas `{fechaInicio, fechaFin}` de un año PLANIFICADO y regenera sus periodos (CI-06 · Editar) |
| PUT | `/configuracion/anios-escolares/{id}/regimen` | Cambia el régimen `{regimen}` de un año PLANIFICADO y regenera sus periodos (CI-06 · Cambiar régimen) |
| GET | `/configuracion/anios-escolares/{id}/requisitos` | Requisitos de "Antes de activar el año" (CI-06): 3 obligatorios y 2 recomendados |
| PUT | `/configuracion/periodos-academicos/{id}` | Edita un periodo `{nombre, fechaInicio, fechaFin, fechaLimiteNotas}` (Modal · Editar periodo) |

No hay endpoints para activar ni cerrar el año (`PENDIENTE(grupo)`: Figma no tiene botón "Activar" y "Cerrar año" solo aparece en CI-12) ni para abrir o cerrar un periodo (su estado se calcula). `ServicioAnioEscolar` ya tiene `activar` y `cerrar`, y `periodoEnCurso(anioId)` y `registroNotasAbierto(periodoId)` para que Calificaciones consulte los periodos.

**Cómo se generan los periodos:** BIMESTRAL = 4 y TRIMESTRAL = 3, numerados desde 1 ("I Bimestre"…). Los días del año se reparten en partes iguales (los sobrantes van a los primeros periodos); quedan seguidos, sin cruzarse, y el último termina en `fechaFin`. La fecha límite de notas es `fin + 5 días` (`PeriodoAcademico.DIAS_LIMITE_NOTAS`, **pendiente de confirmar** con el colegio). Un año necesita al menos 2 días por periodo (8 bimestral, 6 trimestral).

## Código

- Backend: `backend/src/main/java/com/galiledu/configuracion/` (`AnioEscolar`, `PeriodoAcademico`, `ServicioAnioEscolar`, `ConsultasConfiguracion`, `AnioEscolarController`, `PeriodoAcademicoController`). El "hoy" sale del bean `Clock` de `compartido/RelojConfig` (America/Lima).
- Frontend: `frontend/src/features/configuracion/` (por crear)
- Pruebas: `backend/src/test/java/com/galiledu/configuracion/` (`AnioEscolarIT` y `PeriodoAcademicoIT` la API, `ServicioAnioEscolarIT` el servicio, `ConsultasConfiguracionIT` las consultas, `dominio/AnioEscolarTests` las reglas del dominio y `EsquemaConfiguracionIT` las reglas de la BD). Las IT que dependen de la fecha extienden `PruebaIntegracionConfiguracion`, que fija el reloj en 15/09/2026.

## Pendientes

- [ ] HU-74: endpoints de activar y cerrar el año (`PENDIENTE(grupo)`; el servicio ya los tiene) y reutilizar configuración (copiar secciones, estructuras y plan de evaluación).
- [ ] Backend de HU-75, HU-44, HU-23 y HU-34, y el frontend de las 5 HU.
- [ ] Confirmar con el colegio los 5 días entre el fin del periodo y la fecha límite de notas.
- [ ] `estudiantes.estudiante` guarda el nivel y el grado como texto ("1.° grado"). Cuando Matrícula asigne secciones, se enlazará con `configuracion.grado`.
- [ ] Los documentos requeridos de la matrícula (HU-21/22) no están en esta migración.
