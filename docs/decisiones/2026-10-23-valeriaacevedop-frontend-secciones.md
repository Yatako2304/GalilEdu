# Frontend de secciones y tutores (HU-75)

- **Autor:** valeriaacevedop
- **Fecha:** 2026-10-23
- **HU / tarea:** HU-75 (pantalla CI-09 de Figma, "Modal - Nueva sección" 849:10198 y "Modal - Asignar tutor" 849:10810)
- **Estado:** terminado como MVP (el historial de tutores y las acciones inactivar y reactivar quedan fuera)

## Qué hice

La pestaña Secciones de la página del año escolar: tabla agrupada por grado, "Nueva sección" (desde el botón de arriba o desde el "Agregar sección" de cada grado), "Asignar tutor", "Editar sección" (solo la capacidad) y "Eliminar sección" desde el menú ⋮ de cada sección. Con el año CERRADO no hay botones ni menú; con PLANIFICADO y ACTIVO se puede todo. Para que las pantallas queden como Figma hubo que tocar componentes compartidos (ver abajo).

## Archivos principales

- `frontend/src/features/configuracion/api/secciones.ts` y `mutaciones-seccion.ts`
- `frontend/src/features/configuracion/components/secciones/` (`secciones-pestana`, `secciones-tabla`, `boton-seccion`, `dialogo-seccion`, `dialogo-tutor`, `dialogo-editar-seccion`, `dialogo-eliminar-seccion`, `esquemas-seccion`, `formato`)
- `frontend/src/types/api.ts` (bloque "Configuración · Secciones por grado y tutores (HU-75)")
- Compartidos: `components/ui/form/` (`field-wrapper`, `input`, `select`, `textarea`), `components/ui/date-picker/`, `tailwind.config.cjs`

## Decisiones y por qué

- **Decisión:** el modal "Asignar tutor" no dibuja "Vigente desde" ni "Historial de tutoría", aunque Figma los muestra.
  **Por qué:** el backend del MVP no guarda el historial (la migración V9 quedó fuera del MVP). Mostrar un campo que no se envía engañaría a quien lo usa. Una prueba fija que no aparecen.
  **Alternativas descartadas:** dibujarlos deshabilitados.

- **Decisión:** "Editar sección" y "Eliminar sección" siguen el estilo de los modales de Figma, aunque Figma no los dibuja.
  **Por qué:** el backend ya los ofrece y se pidió tenerlos desde la lista. Editar solo cambia la capacidad, porque el backend no deja cambiar el nombre ni el grado. Eliminar pide confirmación y, si la sección tuvo matrículas, muestra el 409 del backend.
  **Alternativas descartadas:** esperar al diseño. Falta confirmarlos con quien diseña.

- **Decisión:** en "Asignar tutor" los docentes que ya son tutores de otra sección salen deshabilitados, salvo el tutor actual de esa sección; el cotutor no tiene esa regla, pero no puede ser el tutor elegido.
  **Por qué:** es la regla del backend (un docente es tutor de una sola sección ACTIVA por año; el cotutor no cuenta). Se avisa antes de enviar en vez de esperar al 409.

- **Decisión:** el nombre de la sección se pasa a mayúsculas al escribir y los errores salen en vivo, con los mismos mensajes del backend (`esquemas-seccion.ts`).
  **Por qué:** "a" y "A" son la misma sección para el backend; mostrarlo en el campo evita sorpresas. Los errores del servidor (nombre repetido, capacidad menor que los matriculados) salen una sola vez como aviso, por el interceptor compartido.

- **Decisión:** etiqueta flotante de Figma en `Input`, `Select`, `Textarea` y `DatePicker`: la etiqueta corta el borde superior del campo, descansa dentro si el campo está vacío, sin foco y sin `placeholder` (ni adorno inicial), y sube al enfocar, escribir, elegir o si hay `placeholder`.
  **Por qué:** es lo que dibuja Figma. Se aplicó a los cuatro a la vez porque van juntos en las mismas filas (por ejemplo, fecha de nacimiento y nacionalidad en la inscripción); con solo uno cambiado quedaban desalineados.
  **Alternativas descartadas:** dejar la etiqueta arriba en `Select` y `Textarea`, que ya se probó y desalineaba los formularios.

- **Decisión:** `Select` admite opciones deshabilitadas (`disabled`); `Calendar` y `DatePicker` tienen un tamaño compacto (`size="sm"`) y un desplegable que no se limita al ancho del campo.
  **Por qué:** lo primero lo pide "Asignar tutor"; lo segundo, los modales del año, donde el calendario normal agrandaba el modal y empujaba los campos de abajo. El compacto no está en Figma.

- **Decisión:** la sombra de Figma se llama `card-elevation` (`shadow-card-elevation`), no `card`.
  **Por qué:** `shadow-card` chocaba con el color `card` de shadcn y salía blanca (un halo blanco alrededor del desplegable del `Select`, de los avisos y de los tooltips).

- **Decisión:** el Text Area sigue el tamaño Large de Figma (96 px, relleno de 12/16 px, texto de 16 px, etiqueta con estados de foco y hover).
  **Por qué:** es el tamaño predeterminado del componente. Medium y Small no están implementados.

## Pendientes / problemas conocidos

- [ ] Confirmar con quien diseña "Editar sección" y "Eliminar sección", que Figma no dibuja.
- [ ] Inactivar y reactivar una sección (el backend ya los tiene): sin botón en CI-09. El mensaje de eliminar con historial dice "Inactívala".
- [ ] `Select` con la lista vacía muestra una barra en blanco; debería decir "No hay cursos disponibles" o similar.
- [ ] Los colores de foco y hover de la etiqueta flotante se aplicaron solo al Text Area; falta revisar si el Text Field de Figma hace lo mismo.
- [ ] El ícono `supervisor_account` del modal "Asignar tutor" usa `UserCog`; agregarlo a la [tabla de equivalencias de íconos](2026-10-22-yatako-iconos-lucide.md).
- [ ] Componentes compartidos pendientes de discutir (ModalFormulario, ModalConfirmacion, `ConfirmationDialog` controlable, `BotonSeccion`, `NombreRecortado`, tamaños del Text Area): ver "Pendientes de componentes" en la [guía de componentes](../guias/componentes-ui.md).

## Notas para quien siga

- Para probar con datos, la base local necesita más de un docente elegible (solo María Fernández es tutora en el seed). Esos datos y un matriculado más en 5.° A de secundaria se agregaron a la base local de quien lo hizo, no al seed del equipo.
- `EtiquetaFlotante` está en `components/ui/form/field-wrapper.tsx`. Con un adorno inicial (`startAdornment`, como el "S/" del tarifario) o un `placeholder` propio, la etiqueta se queda arriba para no taparlos.
- En las pruebas, las opciones del `Select` aparecen dos veces con `getAllByRole('option')`: las del `<select>` nativo oculto y las de la lista visible. Acota con `within(screen.getByRole('listbox'))`.
