# Modificación de notas, reportes y consulta del apoderado

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-60, HU-63, HU-64 y HU-69
- **Estado:** terminado; falta confirmar con el PO los criterios de Notion (ver «Pendientes»)

## Qué hice

- **HU-60:** el docente modifica una nota ya registrada con un motivo obligatorio, también fuera del plazo de registro. Cada cambio queda en un historial.
- **HU-63:** el Coordinador ve, por competencia, una columna por sección y otra del grado: promedio, nivel y % de estudiantes con A o AD. Puede filtrar por curso y periodo, y descargarlo en PDF.
- **HU-64:** el Coordinador y el Administrador ven cuántas notas de competencia hay en cada nivel por sección, grado, área y en todo el colegio. También en PDF.
- **HU-69:** el apoderado consulta las notas de cada hijo por periodo, con cursos, ítems y la nota oficial de cada competencia, y descarga la libreta.

## Archivos principales

- `backend/src/main/resources/db/migration/V22__historial_calificaciones.sql`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java` (HU-60)
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioReportesNotas.java` y `dominio/ResumenNotas.java` (HU-63, HU-64)
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioNotasApoderado.java` (HU-69)
- `frontend/src/features/calificaciones/components/dialogo-modificar-calificacion.tsx`, `reportes-notas.tsx` y `calificaciones-apoderado.tsx`
- `frontend/src/components/ui/tag/nota-nivel.tsx`

## Decisiones y por qué

- **Decisión:** una nota ya registrada se modifica con motivo (10 a 500 caracteres) mientras el año esté ACTIVO y el periodo haya empezado, aunque haya pasado `fecha_limite_notas`. El registro en lote sigue cerrado fuera del plazo.
  **Por qué:** la HU pide corregir notas ya registradas. El plazo de registro protege la carga masiva; una corrección puntual y justificada no debe depender de él. Lo eligió alex-sanchezz.
  **Alternativas descartadas:** solo dentro del plazo (no corrige errores detectados después); solicitud al Coordinador con aprobación (más flujo y tablas sin que la HU lo pida).
- **Decisión:** el historial se guarda por ítem y matrícula, no por `calificacion_item.id`.
  **Por qué:** así el historial sobrevive si se quita una nota. Registrar una nota por primera vez no es una modificación.
- **Decisión:** la tabla de «Calificar alumnos» solo **registra** celdas vacías. Una nota ya guardada queda bloqueada y se cambia únicamente con «Modificar» y motivo, también dentro del plazo. El backend rechaza un cambio por lote a una nota existente (409), y una nota registrada no vuelve a quedar en blanco.
  **Por qué:** al principio, dentro del plazo se podía escribir encima en la tabla y el cambio se guardaba con un motivo fijo («Corrección durante el registro de notas»). Eso daba dos caminos para lo mismo y un historial sin motivos reales. Lo pidió alex-sanchezz.
  **Alternativas descartadas:** permitir cambios libres dentro del plazo con un motivo automático.
- **Decisión:** HU-63 resume la **nota oficial de la competencia** (o, con un curso elegido, su aporte) y muestra «% con A o AD» en lugar del «% aprobados» del fragmento de Figma.
  **Por qué:** respeta la [nota oficial por competencia](2026-11-16-alex-sanchezz-nota-oficial-competencia.md) y el [nivel de logro sin aprobado](2026-11-18-alex-sanchezz-nivel-de-logro-sin-aprobado.md).
  **Alternativas descartadas:** promediar niveles en la escala cualitativa, porque inventa una escala numérica. En su lugar se muestra el nivel más frecuente.
- **Decisión:** el Coordinador, y solo él, puede abrir desde HU-63 el **detalle de una sección**: la nota oficial de cada competencia por estudiante y su libreta. Es de solo lectura. El personal administrativo no ve notas, y la vista del Tutor (HU-70) queda para después.
  **Por qué:** un promedio de sección dice dónde hay un problema, pero no quién lo tiene. Las notas son datos sensibles de menores, así que solo las ve quien las necesita por su función (no por su rango). Lo decidió alex-sanchezz.
  **Alternativas descartadas:** dar los reportes o la libreta al personal administrativo.
- **Decisión:** HU-64 cuenta cada nota oficial de competencia de cada estudiante una vez y la reparte en AD, A, B, C o pendiente. Lo ven el Coordinador y el Administrador.
  **Por qué:** no hay frame. Una distribución por nivel es comparable entre grados, áreas y escalas. Que también lo vea el Administrador lo pidió alex-sanchezz.
  **Alternativas descartadas:** un promedio institucional, porque mezcla competencias (ver la decisión del 16/11).
- **Decisión:** en HU-69 se conserva la estructura de Figma, pero la letra de cada curso es un **resumen referencial** de sus ítems. Se agrega la tabla «Notas por competencia» con la nota oficial. Además:
  - el resultado final cuenta las competencias por nivel de logro, sin «Aprobado» ni «cursos aprobados»;
  - «Promedio del periodo» pasa a «Competencias con logro esperado o destacado»;
  - «Cursos en proceso» pasa a «Por reforzar».

  **Por qué:** Figma es anterior a las decisiones del 16/11 y 18/11. Lo eligió alex-sanchezz el 19/11 frente a seguir Figma al pie de la letra.
- **Decisión:** la pastilla de nivel de Figma («❖ Nota (calificación)») se agregó como `NotaNivel` en `components/ui/tag`.
  **Por qué:** la usan el apoderado y los reportes, y AGENTS.md pide crear en `components/ui` lo que falte de Figma.
- **Decisión:** los PDF de los reportes y la libreta reutilizan el generador del consolidado con `ArchivoReporteCalificaciones.pdfResumen`, en lugar de crear otro.
  **Por qué:** mantiene el mismo estilo, la paginación y los colores de nivel.

## Cambios en los esquemas de base de datos

`V22__historial_calificaciones.sql` crea `academico.modificacion_calificacion`:

| Columna | Regla |
| --- | --- |
| `item_evaluacion_id`, `matricula_id` | A qué nota se refiere; el ítem en cascada. |
| `nota_anterior` / `nivel_anterior` | Exactamente uno. |
| `nota_nueva` / `nivel_nuevo` | A lo más uno; ambos `NULL` = la nota se quitó. |
| `motivo` | Obligatorio, no vacío, hasta 500. |
| `modificado_por`, `modificado_en` | Quién y cuándo. |

No cambia tablas existentes.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO los criterios de aceptación de Notion de las cuatro HU. En el repositorio solo estaban los títulos.
- [ ] Revisar con diseño los frames de HU-69 («Aprobado», letra por curso) y agregar frames para HU-60 y HU-64.
- [ ] Agregar a `R__datos_prueba.sql` un apoderado con un hijo calificado para probar HU-69 en la app. Hoy solo lo cubren las pruebas.
- [ ] La situación final del año (promoción) sigue pendiente de un cierre de año.

## Notas para quien siga

- `ServicioNotasCompetencia.resultadosSeccion` es el único cálculo de la nota oficial. Lo usan la tarjeta del docente, los reportes y el apoderado.
- El apoderado no calcula la sección completa: arma sus notas con `RepositorioReportesNotas.itemsDelEstudiante`.
- `@PreAuthorize` del método reemplaza al de la clase en `ReporteNotasController`: `/grado` es solo para el Coordinador.

- Las migraciones se escribieron como V19 y V20 y se renumeraron a **V22** (historial) y **V23** («No evaluado») al integrar `main`, que ya traía V19 a V21. Si aplicaste las versiones viejas en tu base local, Flyway no arranca: hay que corregir el historial de Flyway o crear la base de nuevo.
