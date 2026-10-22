# Periodo de matrícula regular: estado calculado y un solo periodo sin cerrar (HU-23)

- **Autor:** royal-22
- **Fecha:** 2026-10-22
- **HU / tarea:** HU-23 (pantalla 13 de Figma: fechas, vacantes por sección y documentos requeridos con plantillas)
- **Estado:** en progreso

## Qué hice

Backend de la pantalla 13 "Matrícula › Configuración de matrícula": listar los periodos de matrícula, ver el vigente, crear, editar y eliminar un periodo, y subir, descargar y quitar la plantilla de cada documento requerido. Un solo "Guardar configuración" guarda las fechas, las vacantes de cada sección y los documentos. No hay migración nueva: `matricula.periodo_matricula` y `matricula.documento_requerido` ya alcanzan. El código vive en el módulo `configuracion`, como el tarifario ([decisión de HU-34](2026-10-22-royal-22-tarifario-datos-maestros.md)).

## Archivos principales

- `backend/src/main/java/com/galiledu/configuracion/dominio/PeriodoMatricula.java`, `DocumentoRequerido.java`, `PlantillaDocumento.java`, `EstadoPeriodoMatricula.java`, `EstadoVisiblePeriodoMatricula.java`
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/ServicioPeriodoMatricula.java`, `DtosPeriodoMatricula.java`, `EtiquetasSecciones.java`
- `backend/src/main/java/com/galiledu/configuracion/infraestructura/PeriodoMatriculaRepositorio.java`, `ConsultasPeriodoMatricula.java`
- `backend/src/main/java/com/galiledu/configuracion/web/PeriodoMatriculaController.java`
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/ServicioTarifario.java` (`TarifarioVigente` ahora incluye `tarifarioId`)

## Decisiones y por qué

- **Decisión:** el estado visible del periodo (PROGRAMADO, ABIERTO, CERRADO) **se calcula** con el bean `Clock`; no se guarda. PROGRAMADO si ahora es anterior al inicio, ABIERTO entre el inicio y el cierre, CERRADO después o si está guardado como CERRADO. "Guardar configuración" deja el periodo HABILITADO; EN_CONFIGURACION no se usa porque Figma no tiene "guardar borrador".
  **Por qué:** igual que el estado de los periodos académicos ([decisión del 21/10](2026-10-21-royal-22-estado-periodo-calculado.md)), un estado guardado habría que actualizarlo con una tarea programada. Con el `Clock` las pruebas fijan el día.
- **Decisión:** **un solo periodo sin cerrar** (PROGRAMADO o ABIERTO). Crear otro mientras existe uno responde 409: "Ya existe un periodo de matrícula {año} sin cerrar." Al guardar, los periodos HABILITADO ya vencidos se marcan CERRADO en la BD.
  **Por qué:** `ConsultasMatricula.periodoVigente()` (de P4) asume que hay un único HABILITADO y toma el más reciente. Con esta regla sigue devolviendo el correcto sin cambiar su código. Un periodo vencido sigue guardado como HABILITADO hasta el siguiente guardado, pero ya no es "sin cerrar" y no estorba.
- **Decisión:** **solo lectura desde que abre**. Solo un periodo PROGRAMADO se edita o se elimina; ABIERTO y CERRADO son de solo lectura. El "Editar" de la lista para un periodo abierto abre la vista de solo lectura con el aviso del modal ("Una vez habilitado, el periodo no se puede editar; solo deshabilitar.").
  **Por qué:** es lo que dice el modal de Figma y lo que pide la HU. Figma ofrece "Editar" en un periodo abierto, así que el frontend lo lleva a la vista de solo lectura en lugar de ocultarlo.
- **Decisión:** el **tarifario del año debe estar CONFIRMADO** para crear y para editar (409: "Confirma el tarifario {año} antes de habilitar la matrícula."), y el año debe tener secciones activas (400).
  **Por qué:** la tarjeta de monto de la pantalla sale de `ServicioTarifario.vigente`, y la matrícula no tiene sentido sin monto ni vacantes. Un tarifario EN_CONFIGURACION no es la señal de que Pagos pueda usarlo.
- **Decisión:** las **vacantes se validan antes de escribir** (1 a 45, no menos que `alumnosOcupanVacante`, sección activa del año, sin repetir) y recién entonces se guardan con `ServicioSecciones.cambiarCapacidad`, una por sección, en la misma transacción que el periodo. La lista `vacantes` es parcial: las secciones que no vienen conservan su capacidad. Vacantes de una sección = `seccion.capacidad_maxima`; no hay otra columna.
  **Por qué:** un 400 no debe dejar nada a medias. Si algo falla de forma inesperada a mitad de camino, `@Transactional` deshace todo. No se modificó `ServicioSecciones`.
- **Decisión:** `bloquear_por_morosidad` y `permitir_extemporanea` se guardan **explícitamente en `true`**.
  **Por qué:** el formulario de Figma no tiene campo para ellas, y los datos de prueba de los dos periodos las tienen en TRUE. El `DEFAULT` de la BD sería distinto (`permitir_extemporanea` es FALSE), así que se escribe el valor a la vista en lugar de depender de él. Si más adelante Figma agrega los campos, se vuelven parte de la solicitud.
- **Decisión:** las **plantillas se guardan en la BD** (`plantilla_contenido` BYTEA, con su nombre y tipo) y **no se cargan con la entidad**: `DocumentoRequerido` solo mapea el nombre y el tipo (`insertable = false, updatable = false`) y `ConsultasPeriodoMatricula` escribe y lee el contenido con SQL. Se suben aparte de "Guardar configuración" (POST multipart por documento), solo con el periodo PROGRAMADO, y se descargan siempre. Solo XLSX, XLS o CSV de hasta 10 MB, validados por extensión y por la firma del contenido; se guarda el tipo canónico de la extensión.
  **Por qué:** listar documentos no debe leer megabytes. Guardar el archivo en la BD evita un servicio de archivos que el proyecto aún no tiene. La plantilla se sube después de guardar porque un documento nuevo no tiene id hasta entonces.
- **Decisión:** el GET de la plantilla **no** se abre al rol Apoderado.
  **Por qué:** las rutas de `/configuracion` son del Personal administrativo. El apoderado debe descargarla desde una ruta de matrícula que valide que el documento corresponde a su matrícula (pendiente de P4, que puede usar `ServicioPeriodoMatricula.descargarPlantilla`).
- **Decisión:** **eliminar es estricto**: 409 si el año tiene cualquier matrícula (incluso anulada) o hay documentos entregados. Un documento con entregas no se puede quitar al editar.
  **Por qué:** las entregas y las matrículas cuelgan del periodo y del año; borrarlos perdería historial.
- **Decisión:** las fechas se piden con hora y se interpretan en **America/Lima**; se guardan como instante (`timestamptz`).
  **Por qué:** Figma pide fecha y hora de inicio y de cierre.

## Pendientes / problemas conocidos

- [ ] Matrícula (P4): reemplazar `periodoVigente()` y `OffsetDateTime.now()` por `ServicioPeriodoMatricula.vigente()` y `estadoEn()`, y exponer la descarga de plantillas del apoderado (HU-28) en una ruta de matrícula que valide su matrícula.
- [ ] `ManejadorErrores` responde 500 por un parámetro faltante (`MissingServletRequestParameterException`), un UUID mal formado (`MethodArgumentTypeMismatchException`) y un archivo de más de 10 MB (`MaxUploadSizeExceededException`). Corregir en un PR aparte.
- [ ] Frontend de la pantalla 13.
- [ ] Figma se contradice en algunos textos (el contador de la descripción dice 0/2000 y 0/100, las fechas del modal no coinciden con las del listado, "71 días"). Se usó 100 caracteres y los días reales entre las dos fechas. Confirmar con diseño.

## Por reevaluar (iteración 2)

- **Deshabilitar un periodo:** el modal dice "solo deshabilitar", pero no hay botón en Figma ni endpoint. Habría que decidir qué pasa con las matrículas ya iniciadas y cómo se vuelve a habilitar.
- **Editar un periodo abierto:** hoy es solo lectura. Evaluar con el diseño si se permite ampliar la fecha de cierre o las vacantes.
- **Pagos y Matrícula:** que usen `ServicioPeriodoMatricula.vigente()` y `estadoEn()`, y `ServicioTarifario.vigente()` (ahora con `tarifarioId`) para crear compromisos.

## Notas para quien siga

- Para saber qué periodo está vigente usa `ServicioPeriodoMatricula.vigente()`; para el estado en un instante, `estadoEn(periodoId, instante)`. No uses el estado guardado: un HABILITADO vencido sigue guardado hasta el siguiente guardado.
- Cuando una prueba mezcle SQL con JPA, llama a `entityManager.clear()` después del SQL (también afecta a `alumnosOcupanVacante`, que es un `@Formula`). Las IT extienden `PruebaIntegracionConfiguracion` y fijan el día con `RelojPrueba`.
- `DocumentoRequerido.plantilla_contenido` no está mapeado a propósito: no lo agregues a la entidad.
