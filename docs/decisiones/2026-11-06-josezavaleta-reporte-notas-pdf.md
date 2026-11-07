# Descarga del consolidado como tabla PDF

- **Autor:** josezavaleta
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-62 · presentación del reporte descargado
- **Estado:** terminado

## Qué hice

Cambié la descarga del consolidado para entregar un PDF con una tabla de notas. Incluye los datos del curso y periodo, la regla anual, encabezados por competencia, notas y promedios con sus estados. El botón **Descargar PDF** entrega un único archivo PDF.

Esta decisión reemplaza el formato CSV visible descrito en la [implementación inicial](2026-11-04-josezavaleta-calificaciones-docente.md) y en el [consolidado múltiple](2026-11-06-josezavaleta-consolidado-multiples-competencias.md). El cambio del botón y del formato se hace por solicitud expresa de josezavaleta después de revisar la descarga en Excel.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/infraestructura/ArchivoReporteCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/web/CalificacionController.java`
- `backend/pom.xml`
- `frontend/src/features/calificaciones/api/calificaciones.ts`
- `frontend/src/features/calificaciones/components/reporte-calificaciones.tsx`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

- **Decisión:** descargar directamente `consolidado-notas.pdf` desde `/reportes/exportar-pdf` y mantener el botón del consolidado como único acceso a la descarga.
  **Por qué:** el usuario pidió únicamente un PDF con una tabla legible. El CSV abierto en su Excel colocaba el contenido en una sola columna por la configuración del separador y no conserva estilos de tabla.
  **Alternativas descartadas:** agregar un selector de formatos, una descarga Excel o depender del diálogo de impresión del navegador.
- **Decisión:** generar el PDF en el backend a partir del mismo modelo de reporte y sus filtros; obtener contexto y parámetros desde el curso asignado y el periodo.
  **Por qué:** asegura que las notas, los promedios y los permisos coincidan con la consulta en pantalla. La capa de presentación no calcula promedios ni acepta una regla enviada por el cliente.
  **Alternativas descartadas:** volver a calcular las notas en el archivo o capturar la tabla visible como una imagen.
- **Decisión:** usar A4 horizontal, columnas agrupadas, filas alternadas, promedios destacados y estados en verde, rojo o gris; repetir encabezados y numerar páginas.
  **Por qué:** permite leer e imprimir la tabla sin solapamientos. Cuando hay más de siete columnas de notas, se divide en bloques con los estudiantes repetidos. Si existe promedio general, aparece en cada bloque e indica que corresponde a toda la selección.
  **Alternativas descartadas:** reducir la letra indefinidamente o recortar las columnas que no caben.
- **Decisión:** mantener la ruta CSV anterior para clientes existentes y no modificar su contrato.
  **Por qué:** la petición cambia el formato ofrecido por la interfaz y no requiere romper integraciones anteriores. El ajuste preliminar del separador CSV se descartó al confirmar el requerimiento de PDF.
  **Alternativas descartadas:** eliminar la ruta existente o introducir una cabecera especial de Excel en los CSV anteriores.

## Librerías o herramientas nuevas

- **Apache PDFBox 3.0.8**, licencia Apache 2.0, para generar, leer y renderizar el PDF con Java. La fuente Unicode Liberation Sans se carga desde los recursos incluidos por PDFBox y se incrusta en el documento; no necesita fuentes instaladas en el servidor.

## Cambios en la base de datos

No se modifican tablas, esquemas, migraciones ni datos. El PDF consulta las mismas asignaciones y parámetros anuales existentes.

## Validación

- Backend: verificación completa correcta con Java 21 y PostgreSQL de pruebas; 311 pruebas unitarias y 978 de integración contabilizadas, cero fallos y cinco omisiones existentes. Las pruebas nuevas verifican el documento PDF, contexto, notas, estados, tildes, escala cualitativa, ausencia de promedios, paginación por estudiantes y columnas, roles y parámetros inválidos.
- Frontend: lint y tipos correctos; 113 archivos y 1.038 pruebas correctas. Se comprueba la descarga con filtros idénticos y nombre/extensión PDF, además de la selección múltiple y los promedios.
- Revisión visual de la muestra PDF renderizada: tabla sin solapamientos, encabezados agrupados legibles, notas alineadas, promedios destacados, estados con color y pie con número de página.

## Notas para quien siga

La muestra ficticia para revisar la presentación se genera en `backend/target/reporte-pdf-demo.pdf` y `reporte-pdf-demo.png`; no se versiona. El PDF conserva el resultado numérico calculado por el servicio y muestra `/20` cuando corresponde a la escala vigesimal. La pantalla y el archivo usan los mismos filtros generados, aunque luego se cambien los controles sin volver a generar el reporte.
