# Horarios

- **HU:** HU-45 (iteración 2) · HU-46 (2) · HU-49, 50, 51 · (HU-47, 48 en la 3)
- **Estado:** 🚧 HU-45 hecha (backend y pantalla del Coordinador) y HU-46 hecha (backend)
- **Última actualización:** 2026-11-06

## Alcance

El Coordinador arma el horario semanal de cada sección: elige el curso del plan de evaluación y el ambiente de cada bloque. Un **bloque** no es una fila de catálogo: es el par (día, número de bloque) dentro de la jornada de la estructura horaria, y sus horas se calculan (`CalculadoraJornada` de Configuración). El docente de un bloque **no se guarda**: es el de `academico.docente_seccion` para (curso del plan, sección).

Figma: Coordinador › «HU-45: Asignación de docente y curso a un bloque de horario» (619:2757, 286:14702, 248:9344) y su modal «Asignar bloque horario» (863:56080). HU-46: modal con cruce (296:17801). HU-49: horario del docente y del tutor (393:18119), en `/app/docente/horario` y `/app/tutor/horario`. El horario de lectura de Estudiante y Apoderado (HU-50/51) es otro endpoint y todavía usa datos simulados.

## Base de datos

- **Esquema:** `horarios` · **Migración:** `V10__horarios.sql`

| Tabla | Para qué |
| --- | --- |
| `horarios.horario_seccion` | Cabecera: una por sección (`UNIQUE`), con su estructura horaria (`ON DELETE RESTRICT`), `version` para el bloqueo optimista y `creado_en`/`actualizado_en`. Nace al primer guardado y muere con la sección (`CASCADE`). |
| `horarios.horario_bloque` | Un curso en (día, número de bloque) de un horario, con su ambiente. `UNIQUE (horario_seccion_id, dia, numero_bloque)`; el plan y el ambiente son `RESTRICT`. |

## Endpoints (rol Coordinador)

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/horarios/anios-escolares` | Años para el selector: `{id, anio, estado}` (los de `/configuracion` son solo del Personal administrativo) |
| GET | `/horarios/anios-escolares/{anioId}/secciones` | Los grados del catálogo con sus secciones del año. 404 si el año no existe |
| GET | `/horarios/secciones/{seccionId}` | La matriz: sección, `version` (null si no hay horario), `editable` (false con el año cerrado), estructura, jornada y celdas con curso, docente, ambiente y `cruces`. Sin horario guardado usa la estructura principal del nivel y no escribe nada. 404; 409 si no hay horario ni principal |
| GET | `/horarios/secciones/{seccionId}/opciones` | Cursos del plan con `horasSemanales`, `horasAsignadas` y docente, y ambientes activos |
| PUT | `/horarios/secciones/{seccionId}` | Guarda la matriz **completa** `{version, celdas}` en una transacción con el bloqueo del año. La cabecera nueva nace con `version = 0` y cada guardado con cambios sube 1. 400/404/409 (ver la tabla de `backend/README.md`) |
| POST | `/horarios/secciones/{seccionId}/cruces` | Cruces de un borrador de celdas antes de guardar (HU-46): `{celdas}` → lista de cruces, vacía si no hay. No escribe ni bloquea el año. Está en `CrucesHorarioController`, no en `HorarioController`. 400/404/409 (ver [Cruces de horario](#cruces-de-horario-hu-46)) |

## Reglas que valida el PUT (`ReglasDelHorario`)

Celda repetida (400), día no lectivo (400), bloque fuera de la jornada (400), curso de otro año o grado (400), ambiente inexistente o inactivo (400), curso sin docente (409), horas semanales superadas, con 1 bloque = 1 hora (409), año cerrado (409), versión desactualizada (409) y, al final, los **cruces** (409 con el mensaje del primero).

## Cruces de horario (HU-46)

### Reglas

Cubre el **RF-117** (cruce de docente: bloquear y mostrar la sección y el curso en conflicto) y el **RF-118** (cruce de aula: bloquear e indicar qué sección la ocupa).

Un bloque que se quiere guardar **se cruza** con uno ya guardado cuando se cumplen las tres condiciones:

1. **Es de otra sección del mismo año escolar.** Los bloques de la propia sección no cuentan, y los de otro año tampoco.
2. **Es del mismo día y las horas reales se superponen**, con comparación estricta: `inicio_solicitado < fin_ocupado` y `inicio_ocupado < fin_solicitado`. Dos bloques contiguos (uno termina a las 8:45 y el otro empieza a las 8:45) **no** se cruzan.
3. **Comparten docente o ambiente.** Si comparten ambos se informan dos cruces, uno `DOCENTE` y otro `AMBIENTE`.

Detalles que importan:

- **Se comparan horas, no números de bloque.** Cada sección usa la jornada de su propia estructura horaria (la de su cabecera; sin horario guardado, la principal de su nivel). El bloque 2 de secundaria (entrada a las 8:00) cae de 8:45 a 9:30 y el bloque 1 de primaria (entrada a las 8:30) de 8:30 a 9:15: se superponen aunque tengan números distintos.
- **Un `docenteId` o `ambienteId` nulo no cuenta.** Un curso sin docente en la sección, o un bloque sin ambiente, no puede chocar por ese recurso.
- **Un bloque guardado cuyo número ya no existe** en la estructura de su sección no ocupa ninguna hora y se ignora.
- **El docente de lo ya guardado** es el titular actual en `academico.docente_seccion`; no se copia al guardar.
- Los cruces salen ordenados por día, número de bloque y tipo.

### Mensajes

En Figma (nodo `296:17801`) la alerta tiene un **título** y un texto debajo. El título («Docente no disponible», «Ambiente no disponible») lo arma el frontend a partir del `tipo`; el `mensaje` del backend es solo el texto de abajo. Nombra con quién choca y cuándo; las horas son las del bloque ya guardado, sin cero a la izquierda:

| Tipo | Plantilla | Ejemplo |
| --- | --- | --- |
| `DOCENTE` | `{docente} ya dicta {curso} con {seccion} el {día} de {inicio} a {fin}.` | «María Fernández Ríos ya dicta Álgebra con 1.° T1 el lunes de 8:00 a 8:45.» |
| `AMBIENTE` | `{seccion} ya usa {ambiente} para {curso} el {día} de {inicio} a {fin}.` | «1.° T1 ya usa Aula 12 para Álgebra el lunes de 8:00 a 8:45.» |

El texto del ambiente empieza por la sección para no depender del género del nombre del ambiente.

**Dónde lleva título y dónde no:**

- El endpoint de cruces y las `cruces` de cada celda del GET devuelven el `mensaje` **sin título**; el frontend lo arma con el `tipo`.
- El **409 del PUT** devuelve «Título: texto» en una sola notificación, con los títulos `Docente no disponible` y `Ambiente no disponible` (`MensajesCruce.conTitulo`). Ejemplo: «Docente no disponible: María Fernández Ríos ya dicta Álgebra con 1.° T1 el lunes de 8:00 a 8:45.».

Cada cruce es `{tipo, dia, numeroBloque, mensaje, seccion, curso}`: `numeroBloque` es el del bloque que se quiere guardar; `seccion` («1.° T1») y `curso` son los del bloque con el que choca.

### Endpoint: `POST /api/horarios/secciones/{seccionId}/cruces`

Rol Coordinador. Avisa de los cruces de unas celdas **antes de guardar**, incluso si aún no están guardadas. Solo lee: no bloquea el año ni escribe nada.

**Request**

```json
{ "celdas": [{ "dia": "LUN", "numeroBloque": 1, "planCursoId": "…", "ambienteId": "…" }] }
```

- `celdas` es obligatoria y admite como máximo 72.
- En cada celda, `dia`, `planCursoId` y `ambienteId` son obligatorios y `numeroBloque` es 1 o más. La celda es la misma del PUT (`SolicitudCelda`) y tiene sus mismos mensajes.

**Respuesta 200:** lista de cruces, vacía si no hay.

**Códigos**

| Código | Cuándo |
| --- | --- |
| 200 | Con o sin cruces |
| 400 | Falta `celdas` o hay una celda inválida: «Envía las celdas del horario.», «Hay una celda vacía.», «Elige el día de cada bloque.», «Elige el número de cada bloque.», «El número de bloque empieza en 1.», «Elige el curso de cada bloque.», «Elige el ambiente de cada bloque.», «El horario admite hasta 72 bloques.» |
| 403 | Sin el rol Coordinador |
| 404 | «La sección no existe.» |
| 409 | La sección no tiene horario guardado y su nivel no tiene estructura principal (igual que el GET): «Configura la estructura horaria principal de secundaria antes de asignar horarios.» |

**Celdas que no se pueden convertir.** El conversor omite, sin fallar, las celdas de un curso sin docente en la sección, de un curso que no es del plan de la sección y de un bloque que no existe en la jornada. El endpoint devuelve los cruces del resto: el error exacto lo da el PUT.

### Dónde se usa

- **PUT** `/horarios/secciones/{id}`: calcula los cruces de la matriz completa y responde **409 con el primero**, con título: «Docente no disponible: …» o «Ambiente no disponible: …». No guarda nada.
- **GET** `/horarios/secciones/{id}`: cada celda trae sus `cruces`, para marcarlos en la matriz (por ejemplo, un cruce que apareció porque otra sección se guardó después).
- **Modal «Asignar bloque horario»** (frontend): llama a este endpoint con el borrador de celdas (`useCruces`, `api/cruces.ts`) y deshabilita «Asignar» si hay cruces para ese bloque.

### Concurrencia

El PUT toma el **bloqueo del año** (`bloquearAnioParaHorarios`) **antes** de leer la cabecera y de buscar cruces. Dos guardados simultáneos en secciones distintas se serializan: el segundo ve lo que confirmó el primero y recibe el 409. `CrucesConcurrenciaIT` lo comprueba con dos PUT en paralelo (mismo docente con aulas distintas, y mismo ambiente con docentes distintos): uno responde 200 y el otro 409. La prueba **falla si se quita el bloqueo**: sin él, las 5 corridas de verificación terminaron con los dos PUT en 200 y el cruce guardado. El endpoint de cruces no toma el bloqueo: es un aviso, y la garantía está en el PUT.

### Limitaciones conocidas

- **Cambio de titular en Académico.** Si se cambia el docente de un curso en una sección que ya tiene horario, el cruce no se impide (Académico no consulta Horarios), pero el GET lo muestra en la celda.
- **El día no se valida en el endpoint.** El conversor no compara el día con los días lectivos de la estructura; una celda en un día no lectivo puede dar cruces. Lo valida el PUT («día no lectivo», 400).
- **RF-122 adelantado.** El RF-122 (espacios especiales: una sección o subgrupo por bloque) es de la HU-48; la misma comparación de ambientes lo deja adelantado.
- **Diferencias con Figma.** (a) El texto de Figma dice «Seleccione otro docente», pero en el modal el docente no se elige: es el titular de `docente_seccion`; el frontend mostrará otra ayuda. (b) `OcupacionHorariosSql` llena el ambiente con su **nombre** y la matriz de Figma muestra el **código** (A-502): queda pendiente para Valeria.

## Código

- Backend: `backend/src/main/java/com/galiledu/horarios/` (`dominio`: `HorarioSeccion`, `HorarioBloque`; `aplicacion`: `ServicioHorarios`, `ReglasDelHorario`, `ResolutorDeJornada`, `ConversorDeBloquesDeJornada`, `DtosHorarios` y las interfaces `ValidadorCruces`, `OcupacionHorarios` y `ConversorDeBloques`; `infraestructura`: repositorios y los adaptadores SQL de solo lectura `DatosDeSeccionSql`, `PlanDeSeccionSql`, `AmbientesActivosSql`, `AniosYSeccionesSql`, `OcupacionHorariosSql` y `JornadaDeSeccionConfiguracion`; `web`: `HorarioController`). HU-46: `ValidadorCrucesHorario`, `MensajesCruce`, `ServicioCrucesHorario` y `SolicitudCruces` (en `aplicacion`, el validador reemplaza a `ValidadorCrucesStub`) y `web/CrucesHorarioController`. Configuración consulta el uso de una estructura con `UsoDeEstructura` (`UsoDeEstructuraSql`): ver [Configuración](configuracion.md#estructuras-con-horarios-armados-hu-45).
- Frontend: `frontend/src/features/horarios/` (`asignacion-horarios`, `matriz-coordinador`, `dialogo-asignar-bloque`, `barra-acciones`, `datos-del-horario` y las llamadas de `api/`). Ruta: `app/routes/app/coordinador/horarios.tsx`. Los tipos del coordinador están en el feature (`types.ts`): `types/api.ts` tiene los del horario de lectura de la P2.
- Pruebas: `backend/src/test/java/com/galiledu/horarios/` y `frontend/src/features/horarios/`. HU-46: `aplicacion/ValidadorCrucesHorarioTests`, `MensajesCruceTests` y `ServicioCrucesHorarioTests` (sin Spring ni BD), `CrucesHorarioIT` (endpoint), `HorarioConValidadorRealIT` (PUT y GET con el validador real) y `CrucesConcurrenciaIT` (dos guardados a la vez). `HorarioCrucesIT` sigue usando un validador de mentira para el PUT y el GET de la HU-45.

## Pendientes

- [x] **HU-46 (backend):** `ValidadorCruces` real y `POST /horarios/secciones/{id}/cruces`.
- [ ] Decidir con el PO si el PUT debe rechazar solo los cruces de las celdas nuevas o modificadas. Hoy rechaza cualquier cruce de la matriz (decisión 4 de Valeria).
- [ ] Mostrar el ambiente con su código (A-502, como en Figma) en vez de su nombre: `OcupacionHorariosSql` y la matriz.
- [ ] Avisar al Coordinador cuando un cambio de titular en Académico crea un cruce.
- [ ] Horario de lectura de Estudiante y Apoderado (HU-50/51) con datos reales; hoy usa MSW.
- [ ] Subgrupos (HU-47, iteración 3): traerán su propia migración (tabla `grupo`, `horario_bloque.grupo_id` y `docente_id`).
- [ ] Código automático de la estructura horaria y la regla de «principal compatible» (ver [Configuración](configuracion.md)).
- [ ] Registrar en una sola entrada de `docs/decisiones/` las decisiones de la HU-45 (docente derivado, versión, cascade, `DiaSemana` compartido, reglas de Configuración con horarios armados y el hueco de editar hora de entrada, duración o recesos).
