# Tarifario del año escolar: datos maestros en Configuración (HU-34)

- **Autor:** royal-22
- **Fecha:** 2026-10-22
- **HU / tarea:** HU-34 (E-20, RF-70 a RF-73)
- **Estado:** en progreso

## Qué hice

Backend de la pantalla P19 "Tarifario y calendario": ver el tarifario de un año (o una propuesta si aún no existe) y confirmarlo. El tarifario guarda los montos de matrícula y pensión, la cuota de ingreso, la política de morosidad y el calendario de cuotas de pensión. `ServicioTarifario` también ofrece `vigente(anioId)` y `calendario(anioId)` para que otros módulos lean el tarifario sin SQL. No hay migración nueva: el modelo de V4 alcanza.

## Archivos principales

- `backend/src/main/java/com/galiledu/configuracion/dominio/Tarifario.java`, `CuotaTarifario.java`, `CalendarioCuotas.java`
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/ServicioTarifario.java`
- `backend/src/main/java/com/galiledu/configuracion/infraestructura/ConsultasTarifario.java`
- `backend/src/main/java/com/galiledu/configuracion/web/TarifarioController.java`
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/ServicioAnioEscolar.java` (`editar` regenera el calendario)

## Decisiones y por qué

- **Decisión:** el tarifario es la **tabla maestra** del año (configuración base, de Configuración); los compromisos, las cuotas y los descuentos aplicados son **instancias por alumno** (operación, de Pagos), que Pagos genera a partir del tarifario. Configuración no crea compromisos.
  **Por qué:** separa quién define las reglas del año de quién las aplica a cada alumno, y deja a Pagos como único dueño de los compromisos.
- **Decisión:** los datos maestros del año (el tarifario y, más adelante, el periodo de matrícula) viven en el módulo `configuracion`, aunque sus tablas estén en los esquemas `pagos` y `matricula`. `Tarifario` y `CuotaTarifario` son entidades JPA con `@Table(schema = "pagos")` sobre las tablas de V4; el paquete `pagos` no se tocó.
  **Por qué:** así lo declara `docs/modulos/configuracion.md`, y las fechas y el estado del año, de los que depende el calendario, son del mismo módulo. Pagos solo lee `pagos.tarifario` por SQL o con `@Formula`, sin mapearla como entidad, así que no hay doble mapeo.
  **Alternativas descartadas:** ponerlo en `pagos`: el calendario dependería de `AnioEscolar`, que es de otro módulo.
- **Decisión:** el **calendario se genera**, no se edita: una cuota por mes desde el mes de inicio de clases hasta el de fin; vence el último día del mes y la última el día 15 (o el fin del año, si es anterior); `numero` correlativo desde 1, `nombre` "Pensión de <mes>" y `cantidad_cuotas` igual al número de cuotas. La matrícula vence el último día del mes anterior al inicio de clases. Reproduce exactamente las cuotas de los datos de prueba 2026 y 2027.
  **Por qué:** es la regla de P19 ("Último día de cada mes · diciembre: 15/12") y de la [decisión del 19/10](2026-10-19-yatako-integracion-bd.md); generarlo evita que el calendario contradiga las fechas del año.
- **Decisión:** al **editar las fechas del año** (HU-74), si el año tiene tarifario sin compromisos se regenera su calendario y el vencimiento de la matrícula (los montos, la morosidad y el estado no cambian). Si el tarifario ya tiene compromisos no se toca, y el GET devuelve `calendarioDesactualizado = true` para que P19 lo informe. Si el calendario nuevo no fuera válido (por ejemplo, más de 12 cuotas), la edición responde 400 con un mensaje que lo explica.
  **Por qué:** las cuotas de los alumnos copian su monto y su fecha, así que cambiar el calendario con compromisos los dejaría desalineados.
- **Decisión:** **confirmar sin guardar**. Figma solo tiene Volver y "Confirmar tarifario"; no hay Guardar. Confirmar crea o actualiza el tarifario, regenera el calendario con las fechas actuales del año y lo deja CONFIRMADO. El estado EN_CONFIGURACION existe en la BD, pero en el flujo de P19 solo se ve en pantalla ("En configuración" = el año aún no tiene tarifario; el GET devuelve una propuesta con `existe = false`, con los valores del año anterior si hay). La única fila EN_CONFIGURACION es la de 2027 en los datos de prueba. CONFIRMADO es la señal de que Pagos puede usarlo.
  **Por qué:** sigue Figma tal cual y evita tarifarios a medias guardados.
- **Decisión:** **solo lectura calculada**, no guardada: un año CERRADO (RF-73) o un tarifario CONFIRMADO con compromisos de pago. Un tarifario CONFIRMADO sin compromisos se puede volver a confirmar; uno EN_CONFIGURACION siempre se puede confirmar, aunque ya tenga un compromiso. Los compromisos se detectan con un `EXISTS` sobre `pagos.compromiso_pago` en `ConsultasTarifario` (marcado como temporal: reemplazar por un método de Pagos si lo exponen).
  **Por qué:** `ServicioCuotaMatricula` no exige CONFIRMADO, así que Pagos puede crear un compromiso sobre un tarifario EN_CONFIGURACION; si ese caso fuera de solo lectura nunca se podría confirmar y el cronograma (que sí exige CONFIRMADO) quedaría bloqueado. El caso se cierra cuando Pagos exija CONFIRMADO en `ServicioCuotaMatricula`.
- **Decisión:** **validar antes de modificar**. `confirmar` y la regeneración por edición del año primero validan los datos y el calendario nuevo sin tocar la entidad, y solo si todo es válido hacen `quitarCuotas()` → `flush()` → `confirmar()` o `regenerarCalendario()`. El flush evita que Hibernate inserte antes de borrar y choque con `uq_cuota_tarifario_numero` y `uq_cuota_tarifario_vencimiento`; las cuotas de los alumnos (`pagos.cuota`) no apuntan a `cuota_tarifario`, así que regenerar no las afecta.
  **Por qué:** un 400 no debe dejar cuotas borradas, haya o no rollback.
- **Decisión:** `ServicioTarifario.confirmar` devuelve el **primer** error, cada condición con su mensaje; el frontend validará cada campo antes de enviar.
  **Por qué:** es lo más simple y suficiente con la validación del lado del cliente.

## Pendientes / problemas conocidos

- [ ] Pagos: `ServicioCuotaMatricula` debería exigir tarifario CONFIRMADO, como `ServicioCronograma`, y puede pasar a usar `ServicioTarifario.vigente` y `calendario` en lugar de leer las tablas por SQL.
- [ ] Reemplazar `ConsultasTarifario.tieneCompromisos` por un método de Pagos si lo exponen.
- [ ] Frontend de P19.

## Por reevaluar (iteración 2)

- **Borrador con autoguardado:** guardar el tarifario EN_CONFIGURACION mientras se edita y que "Confirmar" solo lo publique. Se haría cuando `ServicioCuotaMatricula` exija CONFIRMADO; hoy un borrador guardado podría usarse antes de tiempo para generar una cuota de matrícula.
- **UX de P19:** con un tarifario ya confirmado, el botón sigue diciendo "Confirmar tarifario" aunque solo guarda cambios, y Figma solo dibuja el estado "En configuración". Evaluar con el diseño: el texto "Guardar cambios" para el tarifario confirmado, el borrador con autoguardado, el aviso de cambios sin guardar al salir o al cambiar de año, y la vista de solo lectura (año cerrado o con compromisos).

## Notas para quien siga

- Para leer el tarifario desde otro módulo usa `ServicioTarifario.vigente(anioId)` y `calendario(anioId)`; devuelven vacío si el año no tiene un tarifario CONFIRMADO.
- La relación `cuotas` de `Tarifario` usa `cascade = ALL` y `orphanRemoval`: para regenerar siempre quita, haz flush y vuelve a crear.
