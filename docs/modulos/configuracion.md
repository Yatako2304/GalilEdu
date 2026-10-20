# Configuración del año escolar

- **HU:** HU-74, HU-75, HU-23, HU-34, HU-44 (iteración 1)
- **Estado:** ⏳ por empezar: las tablas y los datos de prueba ya están; faltan backend y frontend
- **Última actualización:** 2026-10-19

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
| HU-74 | Año escolar y sus periodos | ⏳ | Estados: año PLANIFICADO/ACTIVO/CERRADO; periodo PLANIFICADO/EN_CURSO/CERRADO. Mientras el año está planificado se puede cambiar el régimen, y los periodos se regeneran. |
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
| `configuracion.periodo_academico` | Bimestres o trimestres, con su fecha límite de notas y su estado. |
| `configuracion.grado` | Catálogo fijo con los 11 grados (`PRI-1`…`PRI-6`, `SEC-1`…`SEC-5`). Se llena en la migración. |
| `configuracion.seccion` | Sección de un grado en un año: nombre, capacidad máxima (= vacantes), tutor, cotutor y estado. |
| `configuracion.estructura_horaria` | Código (EH-SEC-26), días lectivos (`LUN`…`SAB`), hora de entrada, bloques por día y duración del bloque. |
| `configuracion.receso` | Recesos de una estructura: después de qué bloque van y cuánto duran. |
| `matricula.periodo_matricula` | Inicio y cierre de la matrícula regular, reglas (bloqueo por morosidad, extemporánea) y estado. |
| `pagos.tarifario` | Cuota de ingreso, matrícula, pensión, cantidad de cuotas y política de morosidad. |
| `pagos.cuota_tarifario` | Calendario de pensiones; no puede haber dos cuotas con el mismo vencimiento. |

**Datos de prueba** (`R__datos_prueba.sql`), con los valores de Figma:
- 2026 ACTIVO (bimestral, III Bimestre en curso), con 10 secciones de secundaria (1.° a 5.°, A y B). María Fernández (`U20260004`) es la tutora de 1.° A.
- Estructuras EH-PRI-26 y EH-SEC-26.
- 2027 PLANIFICADO (trimestral), con la matrícula habilitada del 15/09 al 31/10/2026 y el tarifario en configuración (S/ 450 la matrícula y 10 pensiones de S/ 620).

## Código

- Backend: `backend/src/main/java/com/galiledu/configuracion/` (por crear)
- Frontend: `frontend/src/features/configuracion/` (por crear)
- Pruebas: `backend/src/test/java/com/galiledu/configuracion/EsquemaConfiguracionIT.java` (reglas de la BD)

## Pendientes

- [ ] Backend y frontend de las 5 HU.
- [ ] La sección no indica qué estructura horaria usa. Se agregará cuando Horarios lo necesite (Iteración 2).
- [ ] `estudiantes.estudiante` guarda el nivel y el grado como texto ("1.° grado"). Cuando Matrícula asigne secciones, se enlazará con `configuracion.grado`.
- [ ] Los documentos requeridos de la matrícula (HU-21/22) no están en esta migración.
