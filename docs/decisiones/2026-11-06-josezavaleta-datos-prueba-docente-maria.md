# Datos de prueba compartidos para las calificaciones de María

- **Autor:** josezavaleta
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-57, HU-59 y HU-62 · preparación de la demo compartida
- **Estado:** terminado

## Qué hice

Incorporé al archivo repetible de Flyway los tres estudiantes ficticios usados en la prueba local de María Fernández, sus matrículas en 1.° A de secundaria de 2026 y las asignaciones de Álgebra, Aritmética y Geometría. Cada curso sin ítems en el IV bimestre recibe dos prácticas para registrar notas y generar el consolidado PDF.

## Archivos principales

- `backend/src/main/resources/db/datos-prueba/R__datos_prueba.sql`
- `backend/src/test/java/com/galiledu/academico/DatosPruebaDocenteIT.java`
- `backend/src/test/java/com/galiledu/academico/CalificacionesIT.java`
- `backend/src/test/java/com/galiledu/academico/PlanEvaluacionIT.java`
- `backend/src/test/java/com/galiledu/academico/EsquemaAcademicoIT.java`
- Pruebas de conteos de año escolar y secciones en `backend/src/test/java/com/galiledu/configuracion/`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

- **Decisión:** guardar la demo en `R__datos_prueba.sql`, junto a los datos compartidos existentes.
  **Por qué:** josezavaleta pidió que sus compañeros reciban los estudiantes y cursos al actualizar el repositorio. Flyway vuelve a ejecutar el archivo cuando cambia y su ubicación ya está habilitada en desarrollo y en las pruebas.
  **Alternativas descartadas:** dejar los datos únicamente en la base local o pedir una inserción manual en cada equipo.
- **Decisión:** conservar los UUID deterministas de las personas y estudiantes de la demo local; identificar la matrícula por estudiante y año, asignando el siguiente correlativo solo si falta.
  **Por qué:** permite actualizar una base existente y preparar una nueva sin duplicar estudiantes, códigos o matrículas. Los datos son ficticios: Ana Lucía García López, Diego José Quispe Mendoza y Camila Sofía Torres Vargas.
  **Alternativas descartadas:** usar códigos de matrícula fijos que puedan estar ocupados o regenerar identificadores en cada ejecución.
- **Decisión:** conservar asignaciones existentes y crear las prácticas solo si el curso todavía no tiene ítems en el periodo.
  **Por qué:** volver a cargar la demo debe conservar las notas y los criterios que el docente ya editó. Los pesos nuevos dependen de la regla anual, sin modificarla: 50 % por práctica en ponderado y `NULL` en las demás reglas.
  **Alternativas descartadas:** sobrescribir docentes, eliminar ítems o reiniciar calificaciones al arrancar la aplicación.
- **Decisión:** ajustar los conteos de las pruebas a los cinco alumnos de 2026 y aislar el escenario de la API de calificaciones en 1.° B.
  **Por qué:** 1.° A ya contiene los tres alumnos de la demo. Las pruebas conservan comprobaciones exactas y escenarios propios de alta, edición y eliminación. La prueba de configuración previa a las evaluaciones retira solo los ítems de demo dentro de su transacción.
  **Alternativas descartadas:** omitir pruebas fallidas o esconder la demo a las pruebas mediante una configuración especial.

## Base de datos y carga

Se agregan datos ficticios en `personas.persona`, `estudiantes.estudiante`, `matricula.matricula`, `matricula.correlativo_matricula`, `academico.docente_seccion`, `academico.item_evaluacion` y `academico.item_competencia`. No se agrega otra migración de esquema; se mantiene V15 para los pesos opcionales.

El bloque no cambia estados ni parámetros anuales. Solo prepara la demo cuando 2026 está activo y la sección y el IV bimestre existen; una matrícula nueva requiere capacidad disponible. Los entornos que excluyen `db/datos-prueba` no cargan estos datos.

## Validación

Se comprobó la carga compartida, el acceso a cursos y estudiantes y la repetición del bloque preservando una nota y un ítem editado.

- Backend: `spotless:apply verify` correcto; 314 pruebas unitarias y 1000 pruebas de integración contabilizadas, sin fallos ni errores y con 5 omisiones existentes.
- Frontend: `npm run lint`, `npm run check-types`, `npm test` y `npm run build` correctos; 115 archivos y 1054 pruebas aprobadas.
- El PDF del consolidado también se revisó visualmente como tabla, con encabezados por competencia y promedios.

## Notas para quien siga

Después de actualizar el repositorio, reiniciar el backend aplica V15 y vuelve a cargar el archivo repetible modificado. No se necesita recrear la base. Las prácticas se preparan sin notas para poder probar el registro desde el usuario docente. La cuenta de María conserva las credenciales de prueba habituales del repositorio. No se agregaron dependencias.
