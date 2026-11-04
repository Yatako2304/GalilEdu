# Parámetros de evaluación por año escolar

- **Autor:** josezavaleta
- **Fecha:** 2026-11-04
- **HU / tarea:** HU-56, HU-74
- **Estado:** terminado

## Qué hice

Moví la escala de evaluación, la regla para calcular la nota y la consolidación de periodos al año escolar. Estos parámetros se aplican a todos los cursos y competencias del año. El modal de un curso conserva las horas semanales y la selección de competencias.

## Archivos principales

- `backend/src/main/resources/db/migration/V9__parametros_evaluacion_por_anio.sql`
- `backend/src/main/java/com/galiledu/configuracion/`
- `backend/src/main/java/com/galiledu/academico/`
- `frontend/src/features/configuracion/components/anio-escolar/`
- `frontend/src/features/catalogo-academico/components/dialogo-curso-plan.tsx`

## Cambios en los esquemas de base de datos

La migración `V9__parametros_evaluacion_por_anio.sql` modifica dos tablas y no crea ni elimina ninguna:

| Esquema y tabla | Cambio |
| --- | --- |
| `configuracion.anio_escolar` | Agrega `escala_evaluacion VARCHAR(12)`, `regla_nota VARCHAR(20)` y `consolidacion_periodos VARCHAR(20)`, las tres `NOT NULL` y con restricciones `CHECK`. |
| `academico.plan_competencia` | Conserva `escala`, `regla_nota` y `consolidacion` como legado y les asigna valores por defecto técnicos. La aplicación deja de usarlas como fuente efectiva. |

Los años que existían antes de V9 reciben `VIGESIMAL`, `PROMEDIO_PONDERADO` y `PROMEDIO_PERIODOS`. No se transforma ni se elimina la configuración histórica de las competencias. El detalle de columnas, valores y restricciones está en las fichas de [Configuración](../modulos/configuracion.md#cambio-de-esquema-de-v9) y [Catálogo académico](../modulos/catalogo-academico.md#cambio-de-esquema-de-v9).

## Decisiones y por qué

- **Decisión:** guardar una sola escala, una sola regla de nota y una sola consolidación de periodos en `configuracion.anio_escolar`.
  **Por qué:** son políticas del año escolar y deben regir de forma uniforme para todos sus cursos y competencias.
  **Alternativas descartadas:** mantener valores distintos por competencia, porque permitía configuraciones contradictorias dentro del mismo año.
- **Decisión:** permitir cambios solo antes de `fecha_inicio`; desde el día de inicio los parámetros quedan cerrados, aunque el estado administrativo del año todavía sea PLANIFICADO.
  **Por qué:** el inicio de clases es el límite indicado para que la política de evaluación no cambie durante el año.
  **Alternativas descartadas:** usar únicamente el estado PLANIFICADO, porque un año podría seguir en ese estado después de la fecha de inicio.
- **Decisión:** conservar temporalmente las columnas anteriores de `academico.plan_competencia`, con valores técnicos por defecto, y dejar de leerlas o escribirlas desde la aplicación.
  **Por qué:** evita perder los datos existentes y permite aplicar la migración de forma segura. La fuente efectiva desde V9 es `configuracion.anio_escolar`.
  **Alternativas descartadas:** eliminar las columnas en V9, porque descartaría información histórica sin un proceso de respaldo acordado.
- **Decisión:** el modal del curso muestra horas semanales y casillas de competencias, sin escala, regla de nota ni consolidación por competencia.
  **Por qué:** el usuario solo debe decidir qué competencias evalúa el curso; los demás valores ya vienen del año escolar.
  **Alternativas descartadas:** mantener los tres selectores de Figma. Esta decisión reemplaza explícitamente esa parte del modal a pedido del equipo.

## Pendientes / problemas conocidos

- [ ] Retirar las columnas heredadas de `academico.plan_competencia` en una migración futura, después de acordar cómo respaldar o transformar sus valores históricos.

## Notas para quien siga

Las combinaciones válidas son VIGESIMAL con PROMEDIO_SIMPLE o PROMEDIO_PONDERADO, y CUALITATIVA con NIVEL_MAS_FRECUENTE o ULTIMO_NIVEL. La consolidación admite PROMEDIO_PERIODOS o ULTIMO_PERIODO.
