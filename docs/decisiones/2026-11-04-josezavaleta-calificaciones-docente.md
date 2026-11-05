# Registro y reporte de calificaciones del docente

- **Autor:** josezavaleta
- **Fecha:** 2026-11-04
- **HU / tarea:** HU-57, HU-59, HU-62
- **Estado:** terminado

## Qué hice

Implementé el flujo del Docente para definir ítems de evaluación, registrar notas con evidencia pedagógica y consultar o exportar el reporte del curso. El acceso se limita a las asignaciones del docente y la ventana de edición se rige por el año y el plazo de notas del periodo. La validación integral incluyó el guardado del plan de competencias de Álgebra y las descargas de archivos desde el navegador.

## Archivos principales

- `backend/src/main/resources/db/migration/V11__calificaciones_docente.sql`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/RepositorioCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioPlanEvaluacion.java`
- `frontend/src/features/calificaciones/`
- `frontend/src/lib/api-client.ts`
- `docs/modulos/calificaciones.md`

## Cambios en los esquemas de base de datos

La migración `V11__calificaciones_docente.sql` crea cuatro tablas en `academico`:

| Tabla | Clave y relación principal |
| --- | --- |
| `item_evaluacion` | Curso del plan + sección + periodo; conserva año y grado para claves foráneas compuestas. |
| `item_competencia` | Ítem + competencia del plan + peso porcentual. |
| `calificacion_item` | Ítem + matrícula de la misma sección; nota numérica o nivel de logro. |
| `evidencia_calificacion` | Relación uno a uno con la calificación; guarda metadatos y contenido binario. |

También agrega claves únicas compuestas a tablas existentes para verificar relaciones cruzadas:

| Tabla existente | Restricción nueva | Uso |
| --- | --- | --- |
| `academico.plan_curso` | `(id, anio_escolar_id, grado_codigo)` | El ítem usa el año y grado del plan. |
| `configuracion.seccion` | `(id, anio_escolar_id, grado_codigo)` | La sección del ítem pertenece al mismo año y grado. |
| `configuracion.periodo_academico` | `(id, anio_escolar_id)` | El periodo pertenece al año del curso. |
| `matricula.matricula` | `(id, seccion_id)` | La calificación solo enlaza estudiantes de la sección del ítem. |

No se editó ninguna migración anterior.

## Decisiones y por qué

- **Decisión:** tratar los frames revisados como flujo del Docente aunque el encabezado o la barra lateral de Figma muestre Personal administrativo o Administrador.
  **Por qué:** los frames están en la página `🌟 Docente`, las tres HU indican usuario Docente y el equipo confirmó expresamente ese rol.
  **Alternativas descartadas:** exponer el flujo al Personal administrativo, porque ampliaría permisos fuera del alcance de las HU.
- **Decisión:** guardar el ítem por curso del plan, sección y periodo, y asociarlo a una o más competencias del mismo plan.
  **Por qué:** la pantalla filtra por sección y periodo; un docente puede trabajar criterios distintos con cada grupo.
  **Alternativas descartadas:** un ítem global por curso y año, porque mezclaría el registro de secciones distintas.
- **Decisión:** usar el peso de la relación ítem–competencia únicamente cuando la regla del año sea `PROMEDIO_PONDERADO`.
  **Por qué:** conserva el campo «Peso en %» de Figma y respeta que la regla se define para todo el año escolar.
  **Alternativas descartadas:** guardar la regla en cada competencia o ítem, porque contradice la decisión previa de parámetros globales.
- **Decisión:** almacenar una evidencia por calificación, dentro de PostgreSQL, con límite de 10 MB y tipos PDF, DOC, DOCX, JPG y PNG.
  **Por qué:** mantiene la evidencia unida transaccionalmente a la nota y cubre el alcance actual sin incorporar almacenamiento externo.
  **Alternativas descartadas:** una evidencia por ítem o varias por nota, porque HU-59 y Figma muestran un archivo por estudiante e ítem.
- **Decisión:** permitir cambios solo durante el año `ACTIVO`, desde el inicio del periodo hasta su fecha límite de notas.
  **Por qué:** reutiliza la regla temporal ya documentada en Configuración y preserva el historial al cerrar el plazo.
  **Alternativas descartadas:** permitir edición durante todo el año activo, porque ignoraría el cierre de cada periodo.
- **Decisión:** exportar CSV UTF-8 desde el mismo modelo que alimenta la tabla del reporte.
  **Por qué:** evita diferencias entre lo que se ve y lo que se descarga, y no añade una librería nueva.
  **Alternativas descartadas:** generar XLSX en esta iteración; no lo exige la HU ni el frame revisado.
- **Decisión:** sincronizar las competencias del plan por identificador y conservar las relaciones que no cambiaron.
  **Por qué:** los ítems dependen de `plan_competencia`; eliminar toda la colección antes de recrearla rompía la configuración de cursos que ya tenían ítems.
  **Alternativas descartadas:** borrar en cascada los ítems al configurar el plan, porque eliminaría trabajo académico ya registrado.
- **Decisión:** solicitar cualquier tipo MIME en las operaciones del cliente HTTP que descargan un `blob`.
  **Por qué:** los reportes devuelven `text/csv` y las evidencias pueden ser PDF, documentos o imágenes; forzar `application/json` hacía que Spring rechazara la respuesta.
  **Alternativas descartadas:** configurar una cabecera distinta en cada descarga, porque repetiría la misma regla y dejaría otros archivos expuestos al mismo error.

## Pendientes / problemas conocidos

- [ ] Confirmar con el equipo si un empate en `NIVEL_MAS_FRECUENTE` debe conservar el nivel mayor; esa es la regla aplicada actualmente y está documentada en la ficha del módulo.

## Notas para quien siga

Los reportes históricos permanecen visibles después del plazo. Para crear, editar o eliminar, el servicio vuelve a comprobar el docente asignado, el estado del año y la fecha límite; los controles deshabilitados del frontend son solo una ayuda visual.
