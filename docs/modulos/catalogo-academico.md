# Catálogo académico

- **HU:** HU-52, 53, 54, 55, 56 (iteración 1) · HU-43 (3)
- **Estado:** ⏳ por empezar: las tablas y los datos de prueba ya están; faltan backend y frontend
- **Última actualización:** 2026-10-19

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

**Lo valida el servicio:**
- La competencia de un plan debe ser del área del curso.
- La sección del docente debe ser del mismo grado y año.
- Los rangos de la tabla de conversión deben cubrir de 0 a 20 sin vacíos ni cruces.
- Al bajar el aforo de un aula, no puede quedar por debajo de la matrícula de sus secciones (RF-111).

**Datos de prueba:**
- Las 12 áreas de Figma. Formación complementaria está inactiva.
- Las competencias de Matemática, Comunicación y Ciencia y Tecnología.
- Los 16 cursos de secundaria.
- El plan 2026 de 1.° de secundaria, con sus horas y las competencias de Matemática y Comunicación.
- María Fernández (`U20260004`) dicta Álgebra en 1.° A.
- La tabla de conversión vigente desde 2025 y la versión programada para 2027.
- 8 ambientes. 1.° A usa el aula A-101 y 1.° B la A-102.

## Pendientes

- [ ] Backend y frontend.
- [ ] Faltan las competencias de las demás áreas en los datos de prueba. Se cargan desde la pantalla.
