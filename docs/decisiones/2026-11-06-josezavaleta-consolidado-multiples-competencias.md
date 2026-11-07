# Consolidado de varias competencias y promedio general

- **Autor:** josezavaleta
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-62 · selección múltiple de competencias
- **Estado:** terminado

## Qué hice

Amplié el consolidado del docente para seleccionar varias competencias y los ítems que pertenecen a ellas. La tabla muestra un grupo por competencia, su promedio final y un promedio general de la selección. La exportación CSV utiliza los mismos filtros y resultados.

Esta entrada reemplaza el alcance de selección de una sola competencia de la [implementación inicial](2026-11-04-josezavaleta-calificaciones-docente.md) y amplía la [corrección del consolidado](2026-11-05-josezavaleta-correcciones-calificaciones.md). La selección múltiple modifica el selector individual de Figma por solicitud expresa de josezavaleta; conserva los componentes UI y la organización de la pantalla.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/DtosCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/web/CalificacionController.java`
- `frontend/src/features/calificaciones/components/reporte-calificaciones.tsx`
- `frontend/src/features/calificaciones/api/calificaciones.ts`
- `frontend/src/components/ui/form/multi-select.tsx`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

- **Decisión:** permitir varias competencias y ofrecer los ítems de su unión sin duplicados en el selector; agrupar las columnas por competencia.
  **Por qué:** el docente pidió consultar varios resultados en un solo reporte y mantener visible qué competencia evalúa cada ítem.
  **Alternativas descartadas:** una tabla separada por competencia que requiera generar y exportar varios reportes.
- **Decisión:** usar `configuracion.anio_escolar.regla_nota` para los resultados por competencia y el general, con el interruptor existente para incluirlos.
  **Por qué:** josezavaleta confirmó que necesita ambos resultados y que el cálculo depende de los parámetros anuales. No se agrega una regla editable al reporte ni se usa la consolidación entre periodos.
  **Alternativas descartadas:** escoger manualmente la regla en esta pantalla o promediar los resultados ya redondeados de las competencias.
- **Decisión:** calcular el general sobre los ítems calificados seleccionados. En el promedio simple cada ítem cuenta una sola vez; en el ponderado su peso es la suma de sus asociaciones con las competencias seleccionadas. Se calcula `suma(nota × peso) / suma(peso)` y se redondea solo al final.
  **Por qué:** un ítem compartido tiene una única nota y pesos específicos por competencia; la fórmula conserva esos pesos. Si todas las competencias y notas están completas, equivale a dar la misma participación a cada competencia con peso total 100 %.
  **Alternativas descartadas:** duplicar un ítem compartido en el promedio simple o asignar pesos nuevos a las competencias.
- **Decisión:** aplicar también la regla cualitativa anual sobre los ítems únicos y conservar los estudiantes sin calificaciones.
  **Por qué:** la escala anual admite nivel más frecuente o último nivel; una nota pendiente no debe convertirse en cero ni desaparecer del reporte. El cero numérico sí cuenta.
  **Alternativas descartadas:** forzar el cálculo numérico o excluir al estudiante cuando no tiene notas.
- **Decisión:** añadir `competenciaIds` a las rutas de reporte y exportación y mantener `objetivoId` individual. Las columnas incluyen metadatos del grupo y claves distintas para los ítems compartidos.
  **Por qué:** permite ampliar el frontend sin romper clientes anteriores. El CSV distingue las columnas mediante el nombre de la competencia.
  **Alternativas descartadas:** cambiar las rutas existentes o mezclar columnas homónimas sin identificar su competencia.
- **Decisión:** ajustar el componente compartido `MultiSelect` para envolver etiquetas largas y evitar la activación implícita del primer botón «Quitar» al abrir el campo.
  **Por qué:** las competencias tienen nombres extensos y las pruebas detectaron que el `label` envolvente podía desmarcar la primera opción al abrir el menú.
  **Alternativas descartadas:** corregir cada formulario por separado o permitir que abrir el selector cambie sus valores.

## Cambios en la base de datos

No se modifica ningún esquema, tabla, dato de negocio ni migración. Se consultan los parámetros del año y las asociaciones existentes en `academico.item_competencia`. Las pruebas de integración usan su propia transacción en la base de pruebas.

## Validación

- Backend: verificación completa con Java 21 y PostgreSQL de pruebas, 307 pruebas unitarias y 976 de integración contabilizadas; cero fallos y cinco omisiones existentes. Las pruebas nuevas cubren las cuatro reglas anuales, ítems compartidos, selección parcial, notas pendientes y cero, resultados por competencia y general, parámetros inválidos y exportación; se mantienen las comprobaciones de permisos de la API.
- Frontend: lint y tipos correctos; 112 archivos y 1.037 pruebas correctas. Incluye selección múltiple sin desmarcar opciones al abrir el menú, ítems compartidos sin duplicados en el selector, agrupación de columnas, estado de cada promedio, limpieza de filtros, ocultación de promedios y exportación de la selección generada.

## Notas para quien siga

El promedio general corresponde a las competencias e ítems seleccionados en el periodo actual. Con una sola competencia se muestra únicamente su promedio. Al cambiar competencias, curso, sección o periodo se descarta el reporte generado para evitar exportar resultados de otra selección. No se agregaron dependencias.
