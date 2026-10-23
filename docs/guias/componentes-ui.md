# Componentes de interfaz

Todos los componentes de la página **Componentes** de Figma (*🗂️ DESIGN SYSTEM › ・Componentes*) ya existen en `frontend/src/components/ui`. Las pantallas se arman con ellos y con los tokens de Tailwind; no se copian sus estilos.

## Reglas

1. **Si existe, se usa.** Antes de escribir una pestaña, una alerta, una casilla, un círculo con ícono o una tarjeta de indicador, búscalo en la tabla de abajo. No armes uno nuevo con `<div className="…">` dentro de tu módulo, aunque se vea igual.
2. **Si Figma lo tiene y el código no, se crea en `components/ui`**, nunca dentro de `features/<modulo>`. Así lo usan todos y se corrige en un solo lugar. Sigue el patrón de los existentes:
   - Carpeta `components/ui/<nombre>/` con `<nombre>.tsx`, `index.ts` y `<nombre>.stories.tsx`.
   - Un comentario `// Figma > Componentes > <Nombre> (variantes)` sobre el componente.
   - Variantes con `cva`, colores y tamaños solo con tokens (`bg-surface-*`, `text-content-*`, `border-stroke-*`, `text-label-*`…).
   - Una prueba en `components/ui/__tests__/` si tiene lógica (abrir, elegir, agregar).
3. **Si a un componente le falta una variante de Figma, agrégala al componente**, no la sobrescribas con `className` en tu pantalla. `className` es para el espacio alrededor (margen, ancho), no para cambiar colores ni tamaños.
4. **Textos de Figma.** Los componentes traen los textos fijos de Figma ("Arrastra y suelta tu archivo aquí", "Asignar"…). Los textos de tu pantalla también salen de su frame; si no existen, sigue el punto 5 de [convenciones.md](convenciones.md).
5. **Revísalos en Storybook** antes de usarlos: `npm run storybook` dentro de `frontend/`.

## Catálogo

| Figma | Código | Para qué |
| --- | --- | --- |
| Button, Button icon | `Button` (`button`) | Acciones. Variantes `primary`, `secondary`, `primary-light`, `tertiary`, `error`, `error-light`; tamaños `lg`, `md`, `sm`. |
| Text Field, Select, Text Area | `Input`, `Select`, `Textarea` (`form`) | Campos con etiqueta, ayuda y error. Con react-hook-form pasa `registration={register('campo')}`. |
| Tag Field | `TagField` (`form`) | Campo que acumula etiquetas (Enter o coma). Úsalo con `<Controller>`. |
| Checkbox, Checkbox Label | `Checkbox` (`form`) | Casilla con etiqueta y ayuda opcionales. |
| Radio Buttons, Radio Label | `Radio` (`form`) | Opción única; mismo `name` en el grupo. |
| Radio Card, Checkbox Card | `RadioCard`, `CheckboxCard` (`form`) | Opciones grandes en tarjeta, con ícono opcional. |
| Switch | `Switch` (`form`) | Activar o desactivar. |
| Date picker, Picker Cell / Months / Years | `DatePicker`, `Calendar` (`date-picker`) | Fechas en formato `YYYY-MM-DD` (el del backend). `DatePicker` es el campo con el calendario desplegable; úsalo con `<Controller>`. |
| Tab, Tabs | `Tabs variant="underline"` (`tabs`) | Secciones de una ficha. |
| Tab v2, Tabs v2 | `Tabs variant="pill"` (`tabs`) | Cambiar de vista dentro de una pantalla. Pestañas no disponibles: `disabled` + `title: 'Disponible pronto'`. |
| Stepper, Steps | `Stepper` (`stepper`) | Asistentes por pasos. `current` empieza en 1. |
| Alert | `Alert` (`alert`) | Mensajes dentro de la pantalla: `info`, `success`, `warning`, `error`, `default`; botón opcional con `action`. |
| Toast | `useNotifications().addNotification(...)` (`notifications`) | Avisos flotantes después de guardar. Por dentro es un `Alert` con `onClose`. |
| Modal, Modal - CTA | `Dialog`, `ConfirmationDialog` (`dialog`) | Ventanas modales y confirmaciones. |
| Dropdown Item, Dropdown Item List | `DropdownMenu` (`dropdown`) | Menú ⋮ de una fila. |
| cell-table, cell-table-header | `Table` (`table`) | Tablas con columnas declarativas. |
| Pagination, Count | `PaginationBar` (`table`) | "Mostrando 1-10 de 200" con flechas y números. |
| Informative Tag | `Tag` (`tag`) | Etiqueta de solo lectura: `primary`, `info`, `success`, `warning`, `error`, `neutral`. |
| Status | `Status` (`tag`) | Punto + "Activo" / "Inactivo". |
| Selectable Tag(s) | `SelectableTag` (`tag`) | Etiqueta que se marca (filtros). Con `onRemove` muestra la X para quitarla. |
| Stat Card | `StatCard` (`stat-card`) | Indicador (KPI) con ícono, valor y etiqueta. |
| Shortcut Card | `ShortcutCard` (`shortcut-card`) | Acceso rápido a un módulo en los inicios. Recibe la ruta en `to`. |
| Progreso (CI-01 · "Avance del año escolar") | `ProgressBar` (`progress-bar`) | Barra de avance con etiqueta y porcentaje. `value` va de 0 a 100. |
| Ícono en círculo o cuadrado (modales, estados vacíos, Stat Card) | `IconBadge` (`icon-badge`) | Colores `accent`, `success`, `warning`, `error`, `neutral`; tamaños `sm` (número), `md`, `lg`, `xl`. |
| Files, Files_v2, Files - date / time | `FileItem`, `FileIcon`, `fechaArchivo`, `horaArchivo` (`files`) | Archivo con ícono por tipo, dato secundario y acciones. Para el historial usa `version="Versión 1"`. |
| Upload Dropzone | `UploadDropzone` (`files`) | Zona para arrastrar o elegir un archivo. La validación (formato, tamaño) la hace tu pantalla y pasa el mensaje en `error`. |
| Accordion | `Accordion` (`accordion`) | Sección plegable, con ícono opcional. |
| Accordion v2 | `AccordionFolder` (`accordion`) | Carpeta plegable con contador ("18 archivos") y acción opcional. |
| Tooltip | `Tooltip` (`tooltip`) | Ayuda al pasar el mouse o enfocar. Con `description` usa la versión grande. |
| Rating, Rating - item | `Rating` (`rating`) | Estrellas de solo lectura o editables (`onChange`). |
| Bloque horario | `ScheduleBlock` (`schedule-block`) | Celda del horario: curso, subgrupos, recreo u hora libre ("Asignar"). |
| User, Avatar | `Avatar` (`avatar`) | Iniciales o foto del usuario. |
| Sidebar, Sidebar Nav Item, Header, Page Header, Footer | `DashboardLayout`, `ContentLayout` (`components/layouts`) | Estructura de las páginas. Tu pantalla solo usa `ContentLayout` con `title`, `breadcrumbs` y `subtitle`. |
| Spinner | `Spinner` (`spinner`) | Cargando. |

Los nombres de los componentes base (`Button`, `Dialog`, `Table`…) vienen en inglés de la plantilla original; los nuevos siguen el mismo idioma para que el catálogo sea uniforme. Las props y los textos que ve el usuario sí van en español.
