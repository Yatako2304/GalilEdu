# Catálogo académico

- **HU:** HU-52, 53, 54, 55, 56 (iteración 1) · HU-43 (3)
- **Estado:** 🚧 en desarrollo: plan curricular (HU-52, 53, 54) y escala de calificación (HU-55) hechos; faltan HU-56 y HU-43
- **Última actualización:** 2026-10-20

## Alcance

El Personal administrativo arma el catálogo curricular. Cada **área** agrupa sus **competencias** y sus **cursos**. Para cada año escolar define el **plan de evaluación**:
- qué cursos se dictan en cada grado y cuántas horas tienen;
- qué competencias evalúa cada curso, con su escala, su regla de nota final y cómo se consolidan los periodos;
- qué docente dicta cada curso en cada sección.

También mantiene la **tabla de conversión** de vigesimal a AD/A/B/C, que tiene versiones, y el catálogo de **ambientes físicos**.

Las áreas, competencias, cursos y ambientes no se borran: se desactivan, y entonces ya no se pueden agregar a planes nuevos.

**Figma frente a los RF de Notion:**
- RF-130 a RF-132 dicen que el plan lo configura el Coordinador; en Figma está en el menú del Personal administrativo.
- RF-126 pide una descripción obligatoria para el área; en Figma es opcional.

En ambos casos manda Figma.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-52 | Áreas curriculares | ✅ | Nombre único, sin importar mayúsculas. Descripción opcional. Al desactivar un área, sus competencias y cursos quedan como están. |
| HU-53 | Competencias de un área | ✅ | Nombre único dentro del área. Se agregan al final de la lista. |
| HU-54 | Cursos de un área y sus grados | ✅ | Nombre único y al menos un grado. No se puede quitar un grado en el que el curso ya está en un plan de evaluación. |
| HU-55 | Escala de calificación | ✅ | Las versiones no se editan: se agrega una nueva con su fecha "Vigente desde", que no puede ser anterior a hoy. Si la fecha es futura, queda **programada**. El estado (vigente, programada, anterior) se calcula con la fecha de hoy y no se guarda. **Diferencia con Figma:** debajo de la tabla vigente agregamos "Otras versiones" (programadas y anteriores), porque CI-23 no muestra dónde ver la versión programada ni el historial que menciona el modal. |
| HU-56 | Plan de evaluación | ⏳ | Pestaña con `ModulePlaceholder`. |
| HU-43 | Ambientes físicos | ⏳ | Iteración 3. |

## Pantallas (Figma)

| Pantalla | Frame en Figma | Ruta |
| --- | --- | --- |
| Plan curricular | Personal Administrativo › "CI-14 · Plan curricular: áreas, competencias y cursos (HU-52, 53, 54)" y modales "Nueva área", "Nueva competencia", "Nuevo curso", "¿Desactivar …?" | `/app/administrativo/competencias` |
| Plan de evaluación | "CI-17 · Plan de evaluación — 1.° de secundaria 2026 (HU-56)", modal "Aritmética · 1.° de secundaria", "CI-20 · … sin cursos" | pestaña |
| Escala de calificación | "CI-23 · Escala de calificación — tabla de conversión vigente (HU-55)", modal "Nueva versión de la tabla de conversión" | pestaña |
| Ambientes físicos | "CI-26 · Ambientes físicos (HU-43)", modales "Nuevo ambiente físico" y "¿Desactivar …?" | `/app/administrativo/ambientes` |

## Base de datos

- **Esquemas:** `academico` y `configuracion` (ambientes) · **Migración:** `V6__catalogo_academico.sql`

| Tabla | Para qué |
| --- | --- |
| `academico.area` | Área curricular, activa o inactiva. |
| `academico.competencia` | Competencia de un área; el nombre no se repite dentro del área. |
| `academico.curso` | Curso de un área. |
| `academico.curso_grado` | Grados en los que se dicta el curso. |
| `academico.plan_curso` | Curso que se dicta en un grado y año, con sus horas semanales (de 1 a 20). La BD impide planificar un curso en un grado donde no se dicta. |
| `academico.plan_competencia` | Competencia que evalúa el curso en ese plan. Tiene una escala (VIGESIMAL o CUALITATIVA). Su regla de nota final depende de la escala: promedio simple o ponderado si es vigesimal, nivel más frecuente o último nivel si es cualitativa. La consolidación puede ser PROMEDIO_PERIODOS o ULTIMO_PERIODO. |
| `academico.docente_seccion` | Docente que dicta un curso del plan en una sección. |
| `academico.tabla_conversion`, `tabla_conversion_nivel` | Versiones de la tabla de conversión, con su fecha de vigencia y los rangos de AD, A, B y C (de 0 a 20). |
| `configuracion.ambiente` | Aula regular (aforo máximo 45) o espacio especial, con sus recursos. Además, `configuracion.seccion.ambiente_id` indica el aula de cada sección. |

**Lo valida el servicio del plan curricular (`ServicioPlanCurricular`):**
- Los nombres no se repiten, sin importar mayúsculas: ni las áreas, ni los cursos, ni las competencias dentro de su área.
- A un área inactiva no se le agregan competencias ni cursos: primero hay que reactivarla.
- Un curso no puede perder un grado en el que ya está planificado (lo exige `fk_plan_curso_grado`).

**Lo valida la escala de calificación (`TablaConversion.validarRangos`, también en el frontend):**
- Están los 4 niveles, sin repetir.
- C empieza en 0, cada nivel empieza justo después del anterior (sin vacíos ni cruces) y AD termina en 20.
- La fecha "Vigente desde" no es anterior a hoy y no se repite.

**Lo validará el servicio (HU-56, HU-43):**
- La competencia de un plan debe ser del área del curso.
- La sección del docente debe ser del mismo grado y año.
- Al bajar el aforo de un aula, no puede quedar por debajo de la matrícula de sus secciones (RF-111).

**Datos de prueba:**
- Las 12 áreas de Figma. Formación complementaria está inactiva.
- Las competencias de Matemática, Comunicación y Ciencia y Tecnología.
- Los 16 cursos de secundaria.
- El plan 2026 de 1.° de secundaria, con sus horas y las competencias de Matemática y Comunicación.
- María Fernández (`U20260004`) dicta Álgebra en 1.° A.
- La tabla de conversión vigente desde 2025 y la versión programada para 2027.
- 8 ambientes. 1.° A usa el aula A-101 y 1.° B la A-102.

## API

Rol: Personal administrativo. La tabla completa está en [backend/README.md](../../backend/README.md#api-prefijo-api).

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/areas/resumen` | Tarjetas de CI-14 (activos de los registrados) y el año escolar activo |
| GET | `/areas` | Áreas con su número de competencias y cursos |
| GET | `/areas/{id}` | Área con sus competencias y cursos (grados en el orden del catálogo) |
| POST · PUT | `/areas` · `/areas/{id}` | Crea o edita un área |
| POST | `/areas/{id}/competencias` · `/areas/{id}/cursos` | Agrega una competencia o un curso al área |
| PUT | `/competencias/{id}` · `/cursos/{id}` | Edita una competencia o un curso |
| POST | `/{areas,competencias,cursos}/{id}/desactivar` · `/reactivar` | Cambia el estado |
| GET | `/tablas-conversion` | Versiones de la tabla de conversión, de la más nueva a la más antigua, con su estado |
| GET | `/tablas-conversion/vigente` | La versión que rige hoy |
| POST | `/tablas-conversion` | Nueva versión `{vigenteDesde, niveles: [{nivel, notaDesde, notaHasta}]}` (HU-55) |

## Código

- Backend: `backend/src/main/java/com/galiledu/academico/` (`ServicioPlanCurricular`, `ServicioEscalaCalificacion`, `AreaController`, `CompetenciaController`, `CursoController`, `TablaConversionController`).
- Frontend: `frontend/src/features/catalogo-academico/` (`PlanCurricular`, `EscalaCalificacion` y sus modales). La ruta `src/app/routes/app/administrativo/competencias.tsx` tiene las 3 pestañas.
- Pruebas: `backend/src/test/java/com/galiledu/academico/` (`PlanCurricularIT`, `EscalaCalificacionIT`, `dominio/TablaConversionTests`) y `frontend/src/features/catalogo-academico/components/__tests__/`.

## Pendientes

- [x] Backend y frontend del plan curricular (HU-52, 53, 54).
- [x] Escala de calificación (HU-55).
- [ ] HU-56 Plan de evaluación y HU-43 Ambientes físicos.
- [ ] Comparar con Figma los textos exactos de los modales del plan curricular (placeholders y descripciones).
- [ ] Faltan las competencias de las demás áreas en los datos de prueba. Se cargan desde la pantalla.
