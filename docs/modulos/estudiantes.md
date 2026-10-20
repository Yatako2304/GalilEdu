# Estudiantes (inscripción)

- **HU:** HU-7 (iteración 1)
- **Estado:** 🚧 hecha; falta una prueba manual completa desde la pantalla
- **Última actualización:** 2026-10-19

## Alcance

Personal administrativo inscribe a un estudiante de primaria o secundaria en 5 pasos: datos, ficha médica, padres/apoderado, contactos y autorización de salida, y responsable. El estudiante recibe su propia cuenta al quedar matriculado (V7). El **responsable de matrícula** (padre, madre o apoderado) tiene la cuenta de la familia con rol Apoderado: si ya la tiene, solo se vincula; si no, se crea y se le envían las credenciales.

En Notion, HU-7 está en "Seguridad" como "registro de estudiante con apoderado"; seguimos el flujo de Figma, que lo resuelve como inscripción.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-7 | Registro de estudiante con padres/apoderado | 🚧 | Reglas de Figma: al menos padre o madre; si ninguno, apoderado obligatorio; mínimo 2 contactos. |

## Pantallas (Figma)

| Pantalla | Frame en Figma | Ruta |
| --- | --- | --- |
| Listado | Personal Administrativo › "Admin / Estudiantes \| Listado de estudiantes" | `/app/administrativo/estudiantes` |
| Inscripción, pasos 1–5 | "Admin / Estudiantes \| Inscripción \| Paso 1…5" (y variante sin padres) | `/app/administrativo/estudiantes/nueva` |
| Ficha | "Admin / Estudiantes \| Ficha del estudiante - Familia" | `/app/administrativo/estudiantes/:id` |

## Base de datos

- **Esquema:** `estudiantes` · **Migración:** `V2__estudiantes_inscripcion.sql`

| Tabla | Para qué |
| --- | --- |
| `estudiantes.estudiante` | Datos propios del alumno: nacimiento, lengua, procedencia, año académico y grado (`grado_codigo` → `configuracion.grado`, V7). `estado` solo dice ACTIVO o RETIRADO; "Inscrito / Matriculado" se calcula desde su matrícula. Los datos personales están en `personas.persona`. |
| `estudiantes.ficha_medica` | Paso 2. |
| `estudiantes.vinculo_familiar` | Paso 3 y 5: padre/madre/apoderado, vive con él, tutela, **responsable de matrícula**. Permite detectar hermanos. |
| `estudiantes.contacto_emergencia` | Paso 4 (mínimo 2). |
| `estudiantes.autorizado_recojo` | Paso 4. |
| `estudiantes.documento_vinculo` | Documento que acredita al apoderado (archivo en la BD). Migración `V3__estudiantes_documento_apoderado.sql`. |

## API

Ver [backend/README.md](../../backend/README.md#api-prefijo-api): `/estudiantes`, `/estudiantes/{id}`, `/estudiantes/personas?documento=`, `/estudiantes/inscripciones`, `/estudiantes/{id}/apoderado/documento`.

## Código

- Backend: `backend/src/main/java/com/galiledu/estudiantes/`
- Frontend: `frontend/src/features/estudiantes/` (pasos en `components/inscripcion/`)
- Pruebas: `InscripcionIT`; `inscripcion/__tests__/esquema.test.ts`

## Pendientes

- [ ] Prueba manual de los 5 pasos desde la pantalla.
- [ ] "Editar datos" e "Iniciar matrícula" desde la ficha (dependen de Matrícula).
- [x] Documento que acredita al apoderado (carta poder, resolución de tutela…): se adjunta en el paso 3 y se ve o reemplaza desde la ficha. PDF, JPG o PNG de hasta 5 MB.
