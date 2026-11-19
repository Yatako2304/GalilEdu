# Asistencia

- **HU:** HU-65, HU-67, HU-68 (iteración 3)
- **Estado:** 🚧 en desarrollo: HU-65 hecha (backend y frontend); faltan HU-67 y HU-68
- **Última actualización:** 2026-11-19

## Alcance

El tutor o cotutor de una sección (RF-143: los dos con los mismos permisos) registra cada día la asistencia de sus estudiantes: Presente, Tardanza (con hora de llegada) o Falta. Solo puede corregirla ese mismo día, y cada corrección queda en un historial. Más adelante justificará faltas y tardanzas (HU-67) y sacará el reporte mensual o por bimestre (HU-68).

Decisiones del 19/11 ([decisión](../decisiones/2026-11-19-yatako-asistencia-diaria.md)):
- RF-143 dice "Ausente"; Figma dice **Falta**, y manda Figma.
- La **hora de llegada** es obligatoria en la tardanza.
- La corrección **no pide motivo**: el modal de Figma no lo tiene, y la HU y el RF tampoco lo piden. Se quitó "y el motivo" del aviso de AS-06.
- **Día escolar** = año ACTIVO, fecha dentro del año y de un periodo académico, y día lectivo de la estructura principal del nivel. Es la misma regla de las reservas de Horarios (HU-48): no hay feriados en el sistema.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-65 | Registro y corrección de la asistencia diaria (RF-143) | ✅ | Solo el día de hoy (hora de Lima) y en día escolar. |
| HU-67 | Justificación de inasistencias y tardanzas (RF-145) | ⏳ | Modal de Figma "Justificar inasistencia / tardanza": motivo (Cita médica, Enfermedad, Trámite oficial, Otro), detalle y adjunto opcional (PDF o imagen, hasta 5 MB). Se podrán justificar días pasados con el selector de fecha. |
| HU-68 | Reporte de inasistencias y tardanzas (RF-146) | ⏳ | Sin pantalla en Figma. Por mes **o bimestre** (el plan de pruebas pide bimestre), ordenado por apellidos y exportable a PDF. |

| Código | Criterio de aceptación (HU-65) |
| --- | --- |
| CAHU65-1 | El tutor o cotutor ve a los estudiantes MATRICULADO de su sección, por apellidos, sin marcar mientras no registre el día (AS-01). |
| CAHU65-2 | "Marcar todos como presentes" marca a los que faltan sin cambiar lo ya marcado. |
| CAHU65-3 | No se guarda si falta marcar a alguien: se avisa cuántos y quiénes (AS-02). Una tardanza exige su hora de llegada. |
| CAHU65-4 | Antes de guardar se confirma con el resumen del día (AS-04). Queda quién registró y a qué hora (AS-05). |
| CAHU65-5 | El mismo día se puede corregir: se ven los cambios sin guardar y se confirman uno por uno (AS-06). Cada cambio queda en el historial y la fila muestra "Modificado". |
| CAHU65-6 | Al día siguiente, un día no lectivo, entre bimestres o fuera del año, solo se consulta. |
| CAHU65-7 | Si el tutor y el cotutor guardan a la vez, el segundo recibe "La asistencia cambió mientras la editabas. Vuelve a cargarla." |
| CAHU65-8 | Nadie ve ni edita la asistencia de una sección que no tutora (404); otros roles no entran (403). |

## Pantallas (Figma)

| Pantalla | Frame en Figma | Ruta |
| --- | --- | --- |
| Asistencia de hoy (sin registrar, error, lista) | Tutor › "AS-01 · Asistencia de hoy · sin registrar", "AS-02 · … error: estudiantes sin marcar", "AS-03 · … todos marcados" | `/app/tutor/asistencia` |
| Guardar | Tutor › "Modal - ¿Guardar la asistencia de hoy?" | (modal) |
| Registrada | Tutor › "AS-05 · Asistencia registrada (modo consulta del día)" | `/app/tutor/asistencia` |
| Corregir | Tutor › "AS-06 · Corregir asistencia del día (HU-65)" y "Modal - Confirmar corrección de asistencia" | `/app/tutor/asistencia` |
| Justificar (HU-67) | Tutor › "Frame 57" (modales "Justificar inasistencia" y "Justificar Tardanza") | pendiente |

- El "Selector de asistencia" (Presente | Tardanza | Falta) no está en Componentes. Se creó `SegmentedControl` en `components/ui`.
- El "Cód." de Figma es el número de documento del estudiante, porque la ficha no tiene otro código (igual que en HU-40).
- El botón de fecha todavía no abre nada: el selector de días llega con HU-67. "Reporte mensual" está deshabilitado hasta HU-68.
- La columna "Justificación" de AS-05 llega con HU-67.

## Base de datos

- **Esquema:** `asistencia` · **Migraciones:** `V19__asistencia.sql`

| Tabla | Para qué |
| --- | --- |
| `asistencia.registro_diario` | Una toma de asistencia por sección y día (`UNIQUE (seccion_id, fecha)`), con quién la registró y la corrigió y cuándo. `version` sirve para el bloqueo optimista. |
| `asistencia.asistencia_estudiante` | Estado de cada matrícula ese día. La BD exige la hora en la tardanza y la rechaza en los demás estados (`ck_asistencia_estudiante_hora`). |
| `asistencia.correccion` | Historial de correcciones: estado y hora de antes y después, quién y cuándo. Solo cambios reales (`ck_correccion_cambio`). |

## API

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| GET | `/asistencia/secciones` | Tutor | Secciones de las que es tutor o cotutor (año activo). |
| GET | `/asistencia/secciones/{id}/dias/{fecha}` | Tutor | La asistencia de ese día, con `editable` y `motivoNoEscolar`. |
| PUT | `/asistencia/secciones/{id}/dias/{fecha}` | Tutor | Registra (`version: null`) o corrige (`version`) la de hoy con todos los estudiantes. |

Detalle de respuestas y errores en [`backend/README.md`](../../backend/README.md).

## Código

- Backend: `backend/src/main/java/com/galiledu/asistencia/`:
  - `dominio/RegistroAsistencia`, `AsistenciaEstudiante`, `CorreccionAsistencia`, `MarcaAsistencia`, `EstadoAsistencia`;
  - `aplicacion/ServicioAsistencia`, `ReglaDiaEscolar` y los puertos de solo lectura `SeccionesTutoradas` y `CalendarioEscolar`;
  - `infraestructura/` (SQL de solo lectura sobre Configuración y Matrícula, y los repositorios);
  - `web/AsistenciaController`.
- Frontend: `frontend/src/features/asistencia/` (`asistencia-seccion.tsx`, `tabla-asistencia.tsx`, `dialogos-asistencia.tsx`, `lib/asistencia.ts`) y `components/ui/segmented-control`.
- Pruebas:
  - Backend: `AsistenciaIT` (reloj fijo el martes 15/09/2026), `RegistroAsistenciaTests`, `ReglaDiaEscolarTests`.
  - Frontend: `asistencia-seccion.test.tsx`, `lib/asistencia.test.ts`, `segmented-control.test.tsx`.
- **Datos de prueba:** María Fernández (`U20260004`, Docente y Tutor) es tutora de 1.° de secundaria A de 2026, con 3 estudiantes matriculados. Para verla, elige el rol **Tutor** y abre Asistencia.

## Pendientes

- [ ] HU-67: selector de fecha, columna "Justificación" y modal de justificación.
- [ ] HU-68: reporte por mes o bimestre con PDF.
- [ ] El rol Tutor no se asigna solo al designar tutor o cotutor ([usuarios.md](usuarios.md)). Un cotutor sin el rol no ve el menú, y la API le responde 403 porque exige el rol TUTOR. Es de Usuarios.
- [ ] El inicio del tutor de Figma ("00 · Home · Tutor") con el aviso "Aún no registras la asistencia de hoy" no es parte de HU-65.
