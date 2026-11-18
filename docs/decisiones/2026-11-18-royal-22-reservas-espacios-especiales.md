# Reserva de espacios especiales (HU-48)

- **Autor:** royal-22
- **Fecha:** 2026-11-18
- **HU / tarea:** HU-48 (RF-122) · Figma 308:1874 (historial), 308:6867 y 308:7130 («Nueva reserva»)
- **Estado:** terminado (backend y frontend)

## Qué hice

El Coordinador puede reservar un espacio físico especial (laboratorio, biblioteca, auditorio) para una sección en una fecha y una franja horaria, con una actividad y un responsable. RF-122: un espacio especial no puede estar reservado para dos secciones a la vez, ni chocar con los horarios semanales. Agregué la migración `V17`, la entidad `ReservaAmbiente`, las reglas (`ReglasDeReserva`), el servicio y seis endpoints en `/horarios/reservas`, más datos de prueba y pruebas de todo el flujo. Ficha: [`docs/modulos/horarios.md`](../modulos/horarios.md#reserva-de-espacios-especiales-hu-48).

## Archivos principales

- `backend/src/main/resources/db/migration/V17__horarios_reserva_ambiente.sql`
- `backend/src/main/java/com/galiledu/horarios/aplicacion/ServicioReservasAmbiente.java`, `ReglasDeReserva.java` y `MensajesReserva.java`
- `backend/src/main/java/com/galiledu/horarios/dominio/ReservaAmbiente.java`
- `backend/src/main/java/com/galiledu/horarios/web/ReservaAmbienteController.java`
- `backend/src/test/java/com/galiledu/horarios/ReservaAmbienteIT.java`, `ReservaConcurrenciaIT.java` y `aplicacion/ReglasDeReservaTests.java`
- `backend/src/main/resources/db/datos-prueba/R__datos_prueba.sql` (bloque «HU-48»)
- `frontend/src/features/horarios/components/reservas-espacios.tsx`, `tabla-reservas.tsx` y `dialogo-reserva.tsx`
- `frontend/src/features/horarios/api/reservas.ts`, `lib/reservas.ts` y `types-reservas.ts`
- `frontend/src/app/routes/app/coordinador/ambientes.tsx` y `frontend/src/testing/mocks/handlers/reservas-ambiente.ts`

## Decisiones y por qué

- **Decisión:** la reserva guarda **fecha y horas reales** (`hora_inicio`, `hora_fin`), no un número de bloque.
  **Por qué:** una reserva es de una fecha concreta y no depende de la estructura horaria; los cruces se calculan por horas reales, como en la HU-46. La API recibe `horaInicio` y `horaFin` (el frontend calcula el fin con Hora + Duración).
  **Alternativas descartadas:** reservar por bloque de la jornada (ver «Pendientes con el PO»).

- **Decisión:** «Pendiente» y «Finalizado» se **calculan al leer** (`fecha + horaFin <= ahora` con el `Clock` de Lima); solo se guarda `ACTIVA` o `CANCELADA`.
  **Por qué:** un estado guardado habría que actualizarlo con un proceso programado y se desincroniza con la hora.
  **Alternativas descartadas:** columna `FINALIZADA` actualizada por una tarea.

- **Decisión:** «Eliminar» es **borrado lógico** (`CANCELADA`); la reserva cancelada no aparece en listados ni en `GET /{id}` y no cuenta para los cruces. Una finalizada o cancelada no se edita ni se cancela (409).
  **Por qué:** conserva el rastro y evita perder datos.
  **Alternativas descartadas:** `DELETE` físico.

- **Decisión:** **sin 422**: 400 para reglas de forma (ambiente, aforo, fecha u hora, día lectivo, jornada, responsable) y 409 para estado y conflictos (año cerrado, reserva finalizada o cancelada, versión vieja, cruces 4, 5, 7 y 8), con `ExcepcionNegocio` como el resto del proyecto.
  **Por qué:** ni el código ni `docs/guias/convenciones.md` usan 422, y el PUT del horario ya separa 400 de forma y 409 de estado.
  **Alternativas descartadas:** 422 para las reglas de negocio.

- **Decisión (regla 5, la propia sección):** frente a los horarios semanales, otra sección con ese ambiente da 409; la **propia** sección con un aula regular a esa hora **se permite** (la clase se traslada al espacio especial), con el mismo ambiente da 400 («no necesita reserva») y con otro espacio especial da 409. Un ambiente desactivado en el horario de la propia sección se trata como aula.
  **Por qué:** la reserva puede reemplazar la clase de esa sección, pero no debe duplicar un espacio que ya usa.
  **Alternativas descartadas:** rechazar siempre que la propia sección tenga clase a esa hora.

- **Decisión (regla 7a):** el responsable no puede dictar clase a **otra** sección a esa hora; con la propia sí. Además no puede ser responsable de otra reserva activa superpuesta (7b). Título «Responsable no disponible».
  **Por qué:** coherente con la regla 5b y con el cruce de docente de la HU-46.

- **Decisión:** el aforo se valida con las matrículas que **ocupan vacante** (todas menos `RETIRADO` y `ANULADA`), la misma condición de `Seccion.SQL_OCUPA_VACANTE`. Horarios la copia (no importa la entidad de otro módulo) y una prueba falla si divergen.
  **Alternativas descartadas:** contar solo `MATRICULADO`; importar `Seccion`.

- **Decisión:** `GET /horarios/reservas/opciones` es análogo a `/secciones/{id}/opciones` (HU-45): los espacios especiales activos con `AmbienteOpcion` y los responsables DOCENTE o TUTOR activos. Año, grado y sección reutilizan `/horarios/anios-escolares` y `/anios-escolares/{anioId}/secciones`.
  **Por qué:** la HU-45 no se toca y no se duplica lo que ya existe.
  **Alternativas descartadas:** reutilizar `/secciones/{id}/opciones` (pide una sección, trae cursos del plan y todos los ambientes).

- **Decisión:** la ruta del frontend sigue siendo `coordinador/ambientes` (`paths.app.coordinador.ambientes`); solo se reemplazará el `ModulePlaceholder`.
  **Por qué:** el placeholder ya apunta a este frame (308:1874, HU-48).

- **Decisión (concurrencia):** crear y reprogramar toman el candado del año (`bloquearAnioParaHorarios`, el del PUT del horario) antes de leer reservas y bloques; la reserva usa `@Version` y el conflicto se convierte en 409. Cancelar no toma el candado.
  **Por qué:** serializa reservas y guardados de horarios del mismo año, y `ReservaConcurrenciaIT` falla sin el candado.
  **Alternativas descartadas:** `EXCLUDE` con `btree_gist` en la BD (pide una extensión y la regla 5 no se puede expresar así); 422 (ver arriba).

- **Decisión (frontend, Figma 308:1874 verificado):** los filtros son `SelectableTag` primary, circular y mediano; el estado, `SelectableTag readOnly` circular con **Pendiente = primary** y **Finalizado = info**; el punto del espacio es siempre el mismo azul oscuro; «Nueva reserva» es `secondary` con `Plus` a la derecha, «Editar» `primary-light` pequeño con `Pencil` a la derecha y eliminar un botón de solo ícono `Trash2` con `aria-label="Eliminar reserva"`, ambos deshabilitados si `editable = false`.
  **Por qué:** es lo que dibuja el frame de Figma; los componentes salen del catálogo de `components/ui`.
  **Alternativas descartadas:** badges `warning` y `success`; color del punto según el tipo de espacio.

- **Decisión (frontend):** el modal no tiene campo Año: al crear se usa el año ACTIVO (o, sin él, el más reciente no cerrado) y al editar el `anioId` de la reserva. Grado y Sección van en cascada y se filtran las secciones `INACTIVA`. Editar usa el mismo modal («Editar reserva» / «Guardar cambios») y eliminar, `DialogoConfirmacion` «¿Eliminar la reserva?», porque Figma no los dibuja.
  **Por qué:** el backend recibe la sección y deduce el año; el Coordinador no elige el año en Figma.
  **Alternativas descartadas:** un selector de año en el modal.

- **Decisión (frontend, hora y duración):** la **Hora** son los inicios de bloque de la jornada de la sección elegida (`GET /horarios/secciones/{id}`, ya existente) y la **Duración** es 1×, 2× y 3× la duración del bloque, con las que pasan del fin de la jornada deshabilitadas; `horaFin = horaInicio + duración` se calcula en el cliente. Al editar, la hora o duración guardada se agrega como opción si no coincide. El formato es «08:00 am».
  **Por qué:** no hace falta un endpoint nuevo y las opciones respetan la jornada real de cada sección (primaria y secundaria pueden diferir).
  **Alternativas descartadas:** una lista fija cada 15 minutos; un endpoint nuevo con las horas disponibles.

- **Decisión (frontend, errores):** POST y PUT se silencian en el cliente y los errores se muestran en el modal: el 409 de cruce como `Alert` con el título aparte («Ambiente/Responsable/Sección no disponible»), el 409 de versión con «Recargar» y el 400 de `@Valid` bajo su campo. DELETE sí sale como notificación global.
  **Por qué:** el modal debe seguir abierto para corregir; en la confirmación de eliminar no hay dónde mostrar un aviso.
  **Alternativas descartadas:** notificación global para todos los errores.

- **Decisión (frontend, `components/ui`):** no se modificó ningún componente compartido; el calendario no puede atenuar días, así que el día no lectivo se valida en el cliente con `jornada.dias`.
  **Por qué:** `components/ui` es de todos y el cambio no era necesario; el backend valida el resto.

## Pendientes / problemas conocidos

- [x] **Frontend:** pantalla y modal en `/app/coordinador/ambientes`; el ítem del menú del Coordinador se llama «Reserva de espacios especiales».
- [ ] **El PUT del horario (HU-45/46) no valida contra las reservas existentes:** un horario guardado después puede superponerse a una reserva. Coordinar con HU-45 y el PO.
- [ ] La fecha solo se valida contra año, periodo y día lectivo: no hay feriados.
- [ ] Si HU-43 desactiva un ambiente con reservas futuras, no hay regla definida.

## Pendientes con el PO

- ¿Se reserva por **bloque** de la jornada o por **hora y duración** (lo que hoy se asume)?
- Validación inversa: ¿el PUT del horario debe rechazar un guardado que choque con una reserva?
- ¿«Pendiente» implica una aprobación de la reserva, o solo que aún no ocurrió?
- ¿Se asocia un **curso** a la reserva?
- ¿Las reservas deben verse en los demás horarios (docente, apoderado, sección)?
- ¿Cómo se tratan los **subgrupos** (HU-47)?
- ¿Se valida el **aforo** contra los alumnos de la sección (hoy sí)?
- El modal de **edición** y la confirmación de **eliminar** no están en Figma: se asume el mismo modal que «Nueva reserva» y una confirmación como las de Usuarios.

## Notas para quien siga

- `ReservaConcurrenciaIT` falla si se quita `estructuras.bloquearAnioParaHorarios(...)` de `ServicioReservasAmbiente`: no lo quites.
- Las pruebas HTTP fijan «hoy» en el 3 de marzo de 2027 con `RelojDeReservasConfig` (contexto de Spring propio).
- La condición de vacante de `MatriculadosDeSeccionSql` está copiada a propósito; `MatriculadosDeSeccionSqlIT` la compara con la de Configuración.
