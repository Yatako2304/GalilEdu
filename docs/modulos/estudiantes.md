# Estudiantes (inscripción)

- **HU:** HU-7 (iteración 1)
- **Estado:** 🚧 hecha; falta una prueba manual completa desde la pantalla
- **Última actualización:** 2026-10-23

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

Ver [backend/README.md](../../backend/README.md#api-prefijo-api): `/estudiantes`, `/estudiantes/{id}`, `/estudiantes/documentos?documento=`, `/estudiantes/personas?documento=`, `/estudiantes/inscripciones`, `/estudiantes/{id}/retirar`, `/estudiantes/{id}/reactivar`, `DELETE /estudiantes/{id}`, `/estudiantes/{id}/apoderado/documento`.

## Código

- Backend: `backend/src/main/java/com/galiledu/estudiantes/`
- Frontend: `frontend/src/features/estudiantes/` (pasos en `components/inscripcion/`)
- Pruebas: `InscripcionIT`, `EstadoEstudianteIT`; `inscripcion/__tests__/esquema.test.ts`, `inscripcion-wizard.test.tsx`, `estudiantes-lista.test.tsx`

## Paso 1 según Figma

El Paso 1 sigue el frame "Admin / Estudiantes | Inscripción | Paso 1 - Estudiante" (manda Figma): secciones Identificación, Lugar de nacimiento, Domicilio y contacto, Datos socioculturales y Procedencia y grado.

- **Lugar de nacimiento:** solo se llenan País, Departamento, Provincia y Distrito (`estudiantes.estudiante`). El campo libre `personas.persona.lugar_nacimiento` ya no se pide al estudiante; sigue existiendo para los usuarios adultos.
- **Departamento, provincia y distrito:** salen del catálogo de ubigeo del INEI (`frontend/src/config/ubigeo.json`, 25 departamentos · 196 provincias · 1893 distritos, cargado bajo demanda por `config/ubigeo.ts`). Son listas encadenadas: la provincia depende del departamento y el distrito de la provincia, y al cambiar un nivel se vacían los de abajo. Solo los departamentos, las provincias y los distritos de Lima y Callao llevan tilde; el resto de distritos queda sin tilde (ñ sí). Datos de [jmcastagnetto/ubigeo](https://github.com/jmcastagnetto/ubigeo) (licencia MIT, Jesús Castagnetto), convertidos a formato título.
- **País distinto de Perú:** Departamento, Provincia y Distrito se deshabilitan, se vacían y se guardan como `null`. País tiene una lista de 195 países con Perú primero.
- **Campos que esperan a otro:** N.° de documento se habilita al elegir el tipo de documento, y Grado al elegir el nivel.
- **Nacionalidad** (Paso 1) solo ofrece Peruana y Extranjera; en padres, apoderados y usuarios sigue la lista completa de `NACIONALIDADES`.
- **Sin valores por defecto:** ningún campo del Paso 1 arranca con valor, salvo "¿Tiene alguna discapacidad?" (No). El grado sugerido por la fecha de nacimiento solo se muestra como aviso: no completa Nivel ni Grado.
- **Celular** no se pide al estudiante (Figma no lo tiene); **Correo** es opcional.
- **Obligatorios:** todo el Paso 1, salvo Apellido materno, Referencia, Teléfono de casa, Correo electrónico, Segunda lengua y Autoidentificación étnica (llevan "(opcional)"). "¿Cuál?" es obligatorio cuando la discapacidad es Sí. La regla vive en `estudianteSchema` de `esquema.ts` y se aplica al presionar "Siguiente". El backend exige lo mismo al registrar (`ServicioEstudiantes.exigirPaso1`) con los mismos mensajes, rechaza departamento, provincia o distrito de nacimiento si el país no es Perú, solo acepta la nacionalidad Peruana o Extranjera, guarda el detalle de discapacidad solo si la respuesta es Sí y limita cada texto al largo de su columna (`DatosEstudiante`).
- **Qué se puede escribir** (`utils/restringir-entrada.ts`): teléfono de casa solo números (hasta 15, como la base de datos); DNI solo números (8) y carné de extranjería letras y números (12); apellidos, nombres y provincia escrita solo letras (con tildes y ñ), espacios, apóstrofo y guion. Aplica también al pegar texto. Los largos salen de `components/personas/limites-persona.ts`, compartido con Nuevo usuario. En los demás pasos (contactos, responsable, padres) aún no se aplica.
- **Campos opcionales con lista** (Segunda lengua, Autoidentificación étnica) se pueden volver a dejar en "Selecciona"; se guardan como `null`.
- **Diferencia con Figma:** el campo "¿Cuál?" de discapacidad aparece al elegir Sí aunque Figma no lo dibuja.
- Figma muestra "Administrador" en el selector de rol del sidebar de esta pantalla, pero es del Personal administrativo: es un error del diseño; la app muestra "Personal administrativo".

## Paso 3 según Figma y CAHU6

Frame "Admin / Estudiantes | Inscripción | Paso 3 - Padres y apoderado". Un padre, madre o apoderado **nuevo** (no encontrado por documento) se valida como un usuario de HU-6, porque el responsable de matrícula tendrá cuenta:

- **Obligatorios (CAHU6-2):** todo salvo apellido materno, teléfono de casa y exalumno. Frontend: `personaUsuarioSchema` en `familiarSchema` (`esquema.ts`). Backend: `DatosPersona.exigirCompletos()`, compartido con "Nuevo usuario"; el mensaje dice de quién es el dato ("Madre: Ingresa el celular") y se revisa antes de guardar nada.
- **Formatos (CAHU6-6, 10, 12, 21, 23, 24):** el N.° de documento solo deja escribir 8 dígitos con DNI o 12 letras y números con carné; nombres y apellidos solo letras; celular 9 dígitos; correo con extensión; teléfono solo números.
- **"País de nacimiento"** con la lista de países (igual que Nuevo usuario), en vez del texto libre "Lugar de nacimiento".
- Un familiar ya registrado (vinculado por documento) no se vuelve a validar: sus datos no se editan aquí.
- **Sin valores por defecto:** el tipo de documento del familiar empieza vacío y el N.° de documento se habilita al elegirlo, como en el Paso 1.
- **"No registra / fallecido(a)":** el motivo es obligatorio (Fallecido(a), No registra u Otro); la observación es opcional.
- **Búsqueda automática:** no hay botón "Buscar"; la persona se busca al salir del N.° de documento, solo si está completo (`documentoCompleto` de `persona-schema.ts`) y una vez por número. Tipo y N.° de documento ocupan media fila cada uno, como los demás datos.
- **Correo no repetido:** al pulsar "Siguiente", el correo de cada familiar nuevo se consulta con `GET /estudiantes/correos?correo=` (sin importar mayúsculas) y no puede repetirse entre ellos; si está ocupado: "El correo ya está registrado en el sistema" (sin decir de quién).
- **Apoderado:** relación obligatoria; con "Otro", "Especificar" también; el tipo de documento que lo acredita es obligatorio. **Adjuntar el archivo no es obligatorio:** se puede adjuntar más adelante. El backend exige la relación.
- **Pendiente (decisión del equipo):** la casilla "Misma dirección que el estudiante" de Figma no se implementa por ahora.

## Paso 4: contactos y autorización de salida

- **Contactos de emergencia** (mínimo 2): nombre, relación y celular obligatorios; teléfono fijo opcional. Nombre y relación solo dejan escribir letras (como nombres y apellidos), el celular 9 dígitos y el teléfono solo números. Largos de `estudiantes.contacto_emergencia` (150 y 60).
- **Autorización de salida:** los tres radios siguen igual. Con "Recogido por persona autorizada", cada fila de "Agregar otra persona autorizada" pide nombre y relación (solo letras); no hay filas hasta pulsar ese botón. Si se cambia a "sale solo" o "movilidad", las filas se quitan y no se envían.
- Backend: mismas reglas en `DatosContacto` y `DatosAutorizado` (con `DatosPersona.SOLO_LETRAS`).

## Paso 5: responsable y registro

- Se listan solo el padre, la madre y el apoderado registrados en el Paso 3; **empieza marcado el primero**. Al elegir uno (o al volver a este paso) se cargan su correo y su celular del Paso 3, que aquí son **solo de vista**: si falta o está mal alguno, el mensaje indica corregirlo en el Paso 3.
- **"Registrar inscripción"** está deshabilitado hasta marcar "Verifiqué los datos…"; ya habilitado, al pulsarlo muestra los errores de responsable, correo y celular.
- Backend: `correoResponsable` y `celularResponsable` son obligatorios en `SolicitudInscripcion`; con ellos se crea la cuenta (rol Apoderado) o se completa la existente.

## Paso 2 según Figma

Frame "Admin / Estudiantes | Inscripción | Paso 2 - Ficha médica". Las reglas están en `fichaMedicaSchema` (`esquema.ts`) y, con los mismos mensajes, en `ServicioEstudiantes.exigirPaso2` y `DatosFichaMedica` del backend.

- **Obligatorios:** tipo de sangre, peso, estatura, tipo de seguro y "¿Hasta cuántos grados de fiebre ha llegado?". Solo las observaciones son opcionales.
- **Formato y rangos** (`LIMITES_FICHA`): peso `###.#` de 5 a 200 kg y estatura `#.##` de 0.50 a 2.50 m. Solo se pueden escribir números y el punto como separador decimal (`decimal()` de `utils/restringir-entrada.ts`): si sobran enteros el punto se pone solo ("118" → "1.18") y al salir del campo se completan los decimales ("22" → "22.0"). La fiebre es un número sin rango con hasta 1 decimal (39 o 39.5), con el mismo filtro pero sin completar.
- **Sí/No** (alergias, convulsiones, enfermedad, medicamento) empiezan en "No", por decisión del equipo.
- **Alergias:** con "Sí" aparecen alimentarias y medicamentosas; al menos una debe tener contenido.
- **Enfermedad y medicamento diario:** su "¿Cuál?" aparece recién con "Sí" y entonces es obligatorio. **Diferencia con Figma**, por decisión del equipo: Figma lo muestra deshabilitado con "No".
- Los detalles de una respuesta "No" no se envían ni se guardan aunque se hubieran escrito antes.

## Documento repetido y estado del estudiante

- **Sin avisos al pasar al Paso 2:** si el documento es de un estudiante eliminado, se sigue sin ningún mensaje; reutilizar su registro es interno.
- **Los mensajes no dicen de quién es el documento:** "El documento ya está registrado dentro del sistema" (estudiante activo u otra persona); si es de un estudiante retirado, se añade que lo reactive desde el listado.

- **Se revisa en el paso 1**, al pulsar "Siguiente" (`GET /estudiantes/documentos`), no al registrar. El aviso sale bajo "N.° de documento" y no deja pasar al paso 2.
- **Libre:** se inscribe normal. **Activo:** no se puede inscribir de nuevo. **Retirado (inactivo):** tampoco; primero hay que reactivarlo. **Eliminado:** sí se puede; al registrar la inscripción el mismo registro (mismo id, con su historial de matrículas y pagos) vuelve a quedar activo y sus datos, ficha médica, familia, contactos y autorizados se reemplazan por los nuevos (`ServicioEstudiantes.estudianteParaInscribir`). Si el documento es de otra persona que no es estudiante (padre, apoderado, usuario) también se rechaza, sin trato especial.
- **Estados:** "inactivo" es `RETIRADO` (`estudiantes.estudiante.estado`, V7); "eliminado" es el borrado lógico `eliminado`. El listado no muestra eliminados.
- **Listado:** cada fila tiene los tres puntos, como en Usuarios: **Ver** (ficha), **Inactivar** o **Reactivar**, y **Eliminar**, los dos con confirmación. El filtro de estado incluye **Retirado**.
- Retirar o eliminar al estudiante no cambia por ahora sus matrículas ni sus pagos.

## Pendientes

- [ ] Prueba manual de los 5 pasos desde la pantalla.
- [ ] **Editar** un estudiante desde el menú de tres puntos (hoy el menú tiene Ver, Inactivar/Reactivar y Eliminar).
- [x] Reglas del servidor del paso 1: obligatorios, solo letras en nombres y apellidos, solo números en teléfono, correo con extensión y coherencia del lugar de nacimiento.
- [ ] El backend no revisa que los valores de las listas (lengua, gestión, forma de ingreso…) ni el año académico (año en curso o siguiente) sean los del catálogo del frontend.
- [ ] "Iniciar matrícula" desde la ficha (depende de Matrícula).
- [ ] **Deuda técnica (siguiente iteración):** pasar a la base de datos las reglas que hoy solo validan frontend y backend: `NOT NULL` en los datos obligatorios de `personas.persona` (CAHU6-2), del estudiante (pasos 1 y 2), de los contactos (celular) y del vínculo del apoderado (relación). Hacerlo con una migración nueva, después de corregir los registros antiguos (anteriores a estas validaciones) que no cumplan.
- [x] Documento que acredita al apoderado (carta poder, resolución de tutela…): se adjunta en el paso 3 y se ve o reemplaza desde la ficha. PDF, JPG o PNG de hasta 5 MB.
