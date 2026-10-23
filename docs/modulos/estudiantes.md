# Estudiantes (inscripción)

- **HU:** HU-7 (iteración 1)
- **Estado:** 🚧 hecha; falta una prueba manual completa desde la pantalla
- **Última actualización:** 2026-10-22

## Alcance

Personal administrativo inscribe a un estudiante de primaria o secundaria en 5 pasos: datos, ficha médica, padres/apoderado, contactos y autorización de salida, y responsable. El estudiante **no tiene cuenta**: el rol Estudiante es un "could have" y no se desarrollan sus pantallas en esta iteración (ver [decisión](../decisiones/2026-10-19-yatako-usuarios-y-acceso.md)). El **responsable de matrícula** (padre, madre o apoderado) tiene la cuenta de la familia con rol Apoderado: si ya la tiene, solo se vincula; si no, se crea y se le envían las credenciales.

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

## Paso 1 según Figma

El Paso 1 sigue el frame "Admin / Estudiantes | Inscripción | Paso 1 - Estudiante" (manda Figma): secciones Identificación, Lugar de nacimiento, Domicilio y contacto, Datos socioculturales y Procedencia y grado.

- **Lugar de nacimiento:** solo se llenan País, Departamento, Provincia y Distrito (`estudiantes.estudiante`). El campo libre `personas.persona.lugar_nacimiento` ya no se pide al estudiante; sigue existiendo para los usuarios adultos.
- **Departamento, provincia y distrito:** salen del catálogo de ubigeo del INEI (`frontend/src/config/ubigeo.json`, 25 departamentos · 196 provincias · 1893 distritos, cargado bajo demanda por `config/ubigeo.ts`). Son listas encadenadas: la provincia depende del departamento y el distrito de la provincia, y al cambiar un nivel se vacían los de abajo. Solo los departamentos, las provincias y los distritos de Lima y Callao llevan tilde; el resto de distritos queda sin tilde (ñ sí). Datos de [jmcastagnetto/ubigeo](https://github.com/jmcastagnetto/ubigeo) (licencia MIT, Jesús Castagnetto), convertidos a formato título.
- **País distinto de Perú:** Departamento, Provincia y Distrito se deshabilitan, se vacían y se guardan como `null`. País tiene una lista de 195 países con Perú primero.
- **Campos que esperan a otro:** N.° de documento se habilita al elegir el tipo de documento, y Grado al elegir el nivel.
- **Nacionalidad** (Paso 1) solo ofrece Peruana y Extranjera; en padres, apoderados y usuarios sigue la lista completa de `NACIONALIDADES`.
- **Sin valores por defecto:** ningún campo del Paso 1 arranca con valor, salvo "¿Tiene alguna discapacidad?" (No). El grado sugerido por la fecha de nacimiento solo se muestra como aviso: no completa Nivel ni Grado.
- **Celular** no se pide al estudiante (Figma no lo tiene); **Correo** es opcional.
- **Obligatorios:** todo el Paso 1, salvo Apellido materno, Referencia, Teléfono de casa, Correo electrónico, Segunda lengua y Autoidentificación étnica (llevan "(opcional)"). "¿Cuál?" es obligatorio cuando la discapacidad es Sí. La regla vive en `estudianteSchema` de `esquema.ts` y se aplica al presionar "Siguiente"; el backend aún no exige estos campos.
- **Qué se puede escribir** (`utils/restringir-entrada.ts`): teléfono de casa solo números (hasta 9); DNI solo números (8) y carné de extranjería letras y números (12); apellidos, nombres y provincia escrita solo letras (con tildes y ñ), espacios, apóstrofo y guion. Aplica también al pegar texto. En los demás pasos (contactos, responsable, padres) aún no se aplica.
- **Campos opcionales con lista** (Segunda lengua, Autoidentificación étnica) se pueden volver a dejar en "Selecciona"; se guardan como `null`.
- **Diferencia con Figma:** el campo "¿Cuál?" de discapacidad aparece al elegir Sí aunque Figma no lo dibuja.
- Figma muestra "Administrador" en el selector de rol del sidebar de esta pantalla, pero es del Personal administrativo: es un error del diseño; la app muestra "Personal administrativo".

## Pendientes

- [ ] Prueba manual de los 5 pasos desde la pantalla.
- [ ] "Editar datos" e "Iniciar matrícula" desde la ficha (dependen de Matrícula).
- [x] Documento que acredita al apoderado (carta poder, resolución de tutela…): se adjunta en el paso 3 y se ve o reemplaza desde la ficha. PDF, JPG o PNG de hasta 5 MB.
