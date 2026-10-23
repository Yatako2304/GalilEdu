# Íconos: lucide-react con equivalencias a los Material Icons de Figma

- **Autor:** yatako
- **Fecha:** 2026-10-22
- **HU / tarea:** transversal (diseño)
- **Estado:** terminado

## Qué hice

Figma dibuja los íconos con *Material Icons* (página "・Material Icons"), pero el frontend usa `lucide-react` desde el inicio. Se decidió **no cambiar de librería** y fijar una tabla de equivalencias para que todos usemos el mismo ícono de lucide para cada ícono de Figma.

## Decisiones y por qué

- **Decisión:** los íconos se importan solo de `lucide-react`. No se agregan otras librerías de íconos ni SVG sueltos copiados de Figma (salvo los íconos de tipo de archivo de `components/ui/files`, que son ilustraciones y vienen de Figma).
  **Por qué:** cambiar a Material tocaría todas las pantallas a pocos días del MVP. Con una sola librería los íconos al menos se ven iguales entre sí.
  **Alternativas descartadas:** pasar a Material Symbols (`material-symbols` o `@mui/icons-material`): más trabajo y una dependencia más; mezclar las dos librerías: se notaría la diferencia de estilo.
- **Decisión:** el color del ícono sale de los tokens `text-icon-*` (Figma › Semantics › `icon/*`), no de `text-content-*` ni de colores sueltos.
  **Por qué:** Figma tiene colores propios para íconos y así quedan iguales en todas las pantallas.
- **Decisión:** el tamaño se copia del nodo de Figma: `size-4` (16 px) en botones y campos, `size-5` (20 px) en listas y acordeones, `size-6` (24 px) en alertas y círculos de ícono. Dentro de `IconBadge`, `StatCard` y `Alert` no se pone tamaño: el componente ya lo define.
- **Decisión:** si un ícono de Figma no está en la tabla, se busca en https://lucide.dev el más parecido, se usa y **se agrega a la tabla en el mismo PR**.

## Tabla de equivalencias

| Figma (Material Icons) | lucide-react | Dónde aparece |
| --- | --- | --- |
| `home` | `Home` | Menú › Inicio |
| `expand_more` / `expand_less` | `ChevronDown` (rotado 180°) | Select, Accordion, selector de hijo |
| `chevron_right` | `ChevronRight` | Shortcut Card, breadcrumbs |
| `arrow_back_ios` / `arrow_forward_ios` | `ChevronLeft` / `ChevronRight` | Paginación, calendario |
| `add` | `Plus` | "+ Nueva …", Asignar |
| `add_circle_outline` | `CirclePlus` | "Agregar competencia", "Agregar curso" |
| `edit` | `Pencil` | Editar |
| `delete` | `Trash2` | Eliminar, Quitar |
| `block` / `do_not_disturb` | `CircleOff` | Desactivar |
| `close` | `X` | Cerrar modal, Toast, quitar etiqueta |
| `check` / `done` | `Check` | Opción elegida, paso completado |
| `check_circle` | `CircleCheck` | Alerta y estado de éxito |
| `info` | `Info` | Alerta por defecto |
| `content_paste` | `ClipboardList` | Alerta informativa |
| `warning` | `TriangleAlert` | Advertencia |
| `report` | `OctagonAlert` | Error |
| `search` | `Search` | Buscadores |
| `visibility` / `visibility_off` | `Eye` / `EyeOff` | Ver, mostrar contraseña |
| `file_download` | `Download` | Descargar |
| `file_upload` / `cloud_upload` | `Upload` / `CloudUpload` | Subir archivo, dropzone |
| `attach_file` | `Paperclip` | Adjuntar documento |
| `folder` | `Folder` | Accordion v2 |
| `event` / `calendar_today` / `calendar_month` | `CalendarDays` | Fechas, Horarios, Periodo de matrícula |
| `event_available` | `CalendarCheck` | Año escolar en curso |
| `event_edit` | `CalendarClock` | Modal Editar periodo |
| `star` | `Star` | Rating, Calificaciones |
| `mail` | `Mail` | Correo |
| `phone` | `Phone` | Teléfono |
| `person_add` | `UserPlus` | Nuevo usuario |
| `group` | `Users` | Estudiantes, usuarios |
| `logout` | `LogOut` | Cerrar sesión |
| `public` | `Globe` | Radio Card / Checkbox Card con ícono |
| `bookmark_border` | `Bookmark` | Selectable Tag con ícono |
| `category` | `Layers` | KPI "Áreas activas" |
| `menu_book` | `BookOpen` | KPI "Cursos activos" |
| `more_vert` | `EllipsisVertical` | Menú ⋮ de una fila |
| `settings` | `Settings` | "Ver configuración" de la tarjeta del periodo vigente |
| `description` | `FileText` | Documentos requeridos |
| `payments` | `Banknote` | Monto de matrícula |
| `error` (círculo con !) | `CircleAlert` | Requisito pendiente de "Antes de activar el año" |
| `lock` | `Lock` | "Cerrar año" |
| `arrow_forward` | `ArrowRight` | "Ver detalle" |
| `swap_horiz` | `ArrowLeftRight` | "Cambiar régimen" |
| `view_sidebar` | `PanelLeft` | Ocultar o mostrar el menú lateral |

## Notas para quien siga

- Antes de usar un ícono revisa esta tabla; si ya hay uno para ese caso, no elijas otro (por ejemplo, para error es `OctagonAlert`, no `CircleAlert` ni `AlertCircle`).
- Los íconos decorativos llevan `aria-hidden="true"`. Un botón que solo tiene ícono necesita `aria-label` o un texto `sr-only`.
