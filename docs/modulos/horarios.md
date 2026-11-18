# Horarios

- **HU:** HU-45 (iteración 2) · HU-46 (2) · HU-49, 50, 51 · HU-48 (3) · (HU-47 en la 3)
- **Estado:** 🚧 HU-45 hecha (backend y pantalla del Coordinador); HU-46 hecha (backend y frontend); HU-49 hecha (backend y frontend); HU-51 hecha (horario del Apoderado); HU-48 (reserva de espacios especiales): backend y frontend terminados
- **Última actualización:** 2026-11-18

## Alcance

El Coordinador arma el horario semanal de cada sección: elige el curso del plan de evaluación y el ambiente de cada bloque. Un **bloque** no es una fila de catálogo: es el par (día, número de bloque) dentro de la jornada de la estructura horaria, y sus horas se calculan (`CalculadoraJornada` de Configuración). El docente de un bloque **no se guarda**: es el de `academico.docente_seccion` para (curso del plan, sección).

Figma: Coordinador › «HU-45: Asignación de docente y curso a un bloque de horario» (619:2757, 286:14702, 248:9344) y su modal «Asignar bloque horario» (863:56080). HU-46: modal con cruce (296:17801). HU-49: horario del docente y del tutor (393:18119), en `/app/docente/horario` y `/app/tutor/horario`. El horario de lectura del Apoderado (HU-51) tiene sus propios endpoints (ver abajo); el del Estudiante (HU-50, Could) todavía usa datos simulados.

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

## Horario del apoderado (HU-51, scuro-star)

Figma: Apoderado (real) › «Horario» (1069:3629: selector del hijo, «Descargar Horario» y matriz) y Apoderado › «HU-51 Consulta de horario de clases de sección para el Apoderado» (429:1456: «Datos del horario» con año académico, grado y sección, y tutora con teléfono y correo). Solo lectura; no toca nada del Coordinador.

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/horarios/apoderado/estudiantes` | Hijos **matriculados** (estado MATRICULADO) del usuario, con sus matrículas `{anio, grado, nivel, seccion}` del año más reciente al más antiguo |
| GET | `/horarios/apoderado/estudiantes/{id}/horario?anio=` | Horario de la sección del hijo ese año (sin `anio`, el más reciente), con el tutor (nombre, celular y correo), la `jornada` y las `clases` (curso, docente y aula). `disponible = false` si el Coordinador aún no guardó el horario. 404 «No encontramos el horario de este estudiante.» si no es su hijo o no está matriculado ese año |

- **Rol APODERADO** (`HorarioApoderadoController`). Un hijo es un estudiante no eliminado vinculado al usuario como padre, madre o apoderado (persona no fallecida).
- **Sin migración.** `HijosDelApoderado` (`HijosDelApoderadoSql`) lee por SQL de solo lectura usuario, vínculo familiar, matrícula y sección, como los demás adaptadores del módulo. La matriz la arma `ServicioHorarios.horarioDeSeccion` (la misma del Coordinador, sin cambios) y `ServicioHorarioApoderado` la reduce a lo que ve el apoderado: sin versión, cruces ni ids del plan.
- **Frontend:** `features/horarios/components/horario-apoderado.tsx` (arma su encabezado porque «Descargar Horario» depende del horario elegido), `api/horario-apoderado.ts` y `lib/horario-apoderado.ts` (pasa la jornada y las clases a la forma de `MatrizHoraria` y arma la página para descargar). Ruta: `app/routes/app/apoderado/horarios.tsx`. La vista del Estudiante (`mi-horario.tsx`, HU-50) no cambió.
- **«Descargar Horario»** abre el horario en una pestaña nueva y el diálogo de impresión del navegador, donde se guarda como PDF (no se agregó una librería de PDF). Deshabilitado si el horario no está disponible.
- **Subgrupos** (HU-47, iteración 3): no se muestran; cada bloque tiene un solo curso.

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
- **Modal «Asignar bloque horario»** (frontend): llama a este endpoint con el borrador de celdas (`useCruces`, `api/cruces.ts`) y deshabilita «Asignar» si hay cruces para ese bloque. La alerta (`dialogo-asignar-bloque.tsx`):
  - Hay **una alerta por tipo** de cruce presente, en rojo (`Alert type="error"`, variante pequeña), primero la de docente y luego la de ambiente. El título sale de `TITULO_CRUCE` (`features/horarios/lib/cruces.ts`, la única fuente): «Docente no disponible» o «Ambiente no disponible». Debajo van los mensajes de ese tipo, tal como los manda el backend.
  - Si hay un cruce de docente, debajo de las alertas aparece la ayuda «Elige otro bloque o cambia el titular en Plan de evaluación.».
  - El campo en conflicto (Docente o Ambiente) se marca con el borde rojo y `aria-invalid="true"`, sin texto de error de formulario. Si la consulta falla, no hay alerta ni se bloquea nada.
  - Pruebas: `alerta-cruces.test.tsx`.
- **Matriz** (frontend): la celda con cruces sigue marcada en amarillo (`ScheduleBlock`, prop `conflict`) y la página muestra «Este horario tiene cruces».

### Concurrencia

El PUT toma el **bloqueo del año** (`bloquearAnioParaHorarios`) **antes** de leer la cabecera y de buscar cruces. Dos guardados simultáneos en secciones distintas se serializan: el segundo ve lo que confirmó el primero y recibe el 409. `CrucesConcurrenciaIT` lo comprueba con dos PUT en paralelo (mismo docente con aulas distintas, y mismo ambiente con docentes distintos): uno responde 200 y el otro 409. La prueba **falla si se quita el bloqueo**: sin él, las 5 corridas de verificación terminaron con los dos PUT en 200 y el cruce guardado. El endpoint de cruces no toma el bloqueo: es un aviso, y la garantía está en el PUT.

### Limitaciones conocidas

- **Cambio de titular en Académico.** Si se cambia el docente de un curso en una sección que ya tiene horario, el cruce no se impide (Académico no consulta Horarios), pero el GET lo muestra en la celda.
- **El día no se valida en el endpoint.** El conversor no compara el día con los días lectivos de la estructura; una celda en un día no lectivo puede dar cruces. Lo valida el PUT («día no lectivo», 400).
- **RF-122 y las reservas.** Los cruces de ambiente de esta HU solo comparan horarios semanales entre secciones. El RF-122 (un espacio especial, una sola sección a la vez, sin superposición bajo ninguna condición) lo cubre la HU-48, que compara también las **reservas** ([ver abajo](#reserva-de-espacios-especiales-hu-48)). El PUT del horario todavía no mira las reservas: ver «Limitaciones conocidas» de la HU-48.
- **Diferencias con Figma.** (a) **Resuelto:** el texto de Figma dice «Seleccione otro docente», pero en el modal el docente no se elige (es el titular de `docente_seccion`); se reemplazó por la ayuda «Elige otro bloque o cambia el titular en Plan de evaluación.». (b) El cruce en la celda de la matriz sigue en **amarillo** (advertencia) porque Figma no diseña ese estado; la alerta del modal sí es roja, como en Figma. (c) `OcupacionHorariosSql` llena el ambiente con su **nombre** y la matriz de Figma muestra el **código** (A-502): queda pendiente para Valeria.

## Horario del docente (HU-49)

Figma: Docente › Horario (393:18119), en `/app/docente/horario` y `/app/tutor/horario` (el mismo frame para las dos rutas). Solo lectura; no toca nada del Coordinador ni del Apoderado.

### Qué ve

- **Solo las clases que dicta:** los bloques guardados donde el usuario es el **titular actual** en `academico.docente_seccion` para (curso, sección). Si Académico cambia el titular, el bloque pasa al nuevo titular y sale del anterior.
- **DOCENTE y TUTOR ven lo mismo:** sus clases de dictado. Un tutor que no dicta ve el estado vacío (`anio` null y `horarios` vacío). **El horario de la sección que tutorea queda fuera de esta HU** (pendiente para consultar al PO).
- **El docente es siempre el usuario que inició sesión** (`ServicioJwt.usuarioActual`); nunca viene en la URL.
- **Ambiente por nombre**, como el resto del módulo.

### Un horario por estructura horaria

La respuesta trae **un horario por estructura**, no uno solo, porque primaria y secundaria pueden tener jornadas distintas (por ejemplo, entrada a las 7:45 y a las 7:30) y porque un horario guardado puede usar una estructura que ya no es la principal. Cada horario trae la jornada de su estructura (`JornadaDeSeccion.deEstructura`) y solo sus clases, para que el frontend no mezcle horas de jornadas distintas en una misma matriz.

- **Orden:** primaria antes que secundaria.
- **Título:** «Primaria» o «Secundaria»; si el docente tiene dos estructuras del mismo nivel, con el código: «Secundaria · EH-SEC-26».
- **Dos clases en una celda:** si un cruce quedó guardado y el docente tiene dos clases en el mismo día y bloque, se devuelven las dos; no se filtran.

### Año por defecto

Sin `anioId`: el año **ACTIVO** si el docente tiene clases en él; si no, el **más reciente con clases**; si no tiene clases en ningún año, `anio` es null y `horarios` está vacío.

### Endpoints (roles DOCENTE y TUTOR, `HorarioDocenteController`)

| Método | Ruta | Respuesta |
| --- | --- | --- |
| GET | `/horarios/docente/anios` | `[{id, anio, estado}]` con los años en que tiene al menos un bloque como titular, del más reciente al más antiguo (`[]` si no tiene). 200, 401, 403 |
| GET | `/horarios/docente/horario?anioId=` | `{anio: {id, anio, estado} \| null, horarios: [{nivel, titulo, jornada: {dias, bloques: [{numero, inicio, fin}], recesos: [{despuesDeBloque, inicio, fin}]}, clases: [{dia, numeroBloque, curso, seccion, ambiente}]}]}`. `anioId` es opcional; con un año sin clases responde 200 con los datos del año y `horarios` vacío. 200; 400 si `anioId` no es un UUID; 401; 403; 404 «El año escolar no existe.» |

`seccion` es la etiqueta «1.° A» (número del grado y nombre de la sección) y `nivel` es `PRIMARIA` o `SECUNDARIA`.

### Diferencias con Figma (393:18119)

- Figma muestra «Prof. …» en cada celda; sobra, porque el docente es el propio usuario. La respuesta no trae el nombre del docente.
- El menú lateral de Figma dice «Administrador»; debería mostrar el rol de quien inició sesión.
- Figma muestra la sección como «3A»; el backend usa «1.° A», como el resto del módulo.

### Pantalla (frontend)

- **Componente:** `features/horarios/components/horario-docente.tsx` (`HorarioDocente`), que usan las dos rutas, `/app/docente/horario` y `/app/tutor/horario`. Título «Horario», selector «Año académico» arriba a la derecha (por defecto, el año que devuelve el backend) y una tarjeta con su `MatrizHoraria` por horario; el título de la tarjeta («Primaria», «Secundaria») solo aparece si hay más de un horario. Estados vacíos: «Aún no tienes horarios asignados.» y «Aún no tienes bloques asignados en este año.». Los errores usan la notificación global.
- **Datos:** `api/horario-docente.ts` (`useAniosDocente` y `useHorarioDocente(anioId?)`) y la conversión `lib/horario-docente.ts` (`aMatrizDelDocente`): deja el docente vacío para no mostrar «Prof.», junta en una sola sesión las dos clases de una misma celda y descarta el sábado.
- **Cambios compatibles en componentes compartidos:** `CursoSesion.seccion` opcional en `types/api.ts`; `MatrizHoraria` pasa `section` a `ScheduleBlock` solo si existe (no cambia el horario del Apoderado ni el del Coordinador); y `ScheduleBlock` usa el índice en la clave de cada curso, para que dos cursos con el mismo nombre en una celda no repitan la clave.
- **Limitaciones:** (a) dos clases en una misma celda (un cruce guardado) se ven juntas bajo el encabezado «Subgrupos» de `ScheduleBlock`; (b) `MatrizHoraria` solo dibuja de lunes a viernes, así que las clases del sábado no se ven.

### Cómo se arma

- **Sin migración.** `ClasesDelDocente` (puerto) y `ClasesDelDocenteSql` (lectura SQL con una sola consulta) traen los bloques del titular y los años en que tiene clases; `ServicioHorarioDocente` los agrupa por estructura, pide cada jornada y arma la respuesta. No usa ni modifica `ServicioHorarios`: el mapeo de la jornada a los DTOs públicos de `DtosHorarios` está en el propio servicio.
- **Año inexistente:** `ServicioHorarioDocente` lo consulta con `AniosYSecciones` (el puerto del selector de años del Coordinador).

## Reserva de espacios especiales (HU-48)

Figma: Coordinador › «Reserva de espacios especiales» (308:1874) y su modal «Nueva reserva» (308:6867 y 308:7130). La ruta del frontend es la de siempre, `/app/coordinador/ambientes` (no se renombró). **Terminada (backend y frontend).** Decisiones y preguntas abiertas: [2026-11-18-royal-22-reservas-espacios-especiales.md](../decisiones/2026-11-18-royal-22-reservas-espacios-especiales.md).

### Objetivo

El Coordinador reserva un espacio físico especial (laboratorio, biblioteca, auditorio…) para **una sección** en una fecha y una franja horaria, con una actividad y un responsable (docente o tutor). RF-122: un espacio especial solo puede estar reservado para una sección a la vez; no se permite superposición.

### Base de datos

- **Esquema:** `horarios` · **Migración:** `V17__horarios_reserva_ambiente.sql`

| Tabla | Para qué |
| --- | --- |
| `horarios.reserva_ambiente` | Una reserva: `ambiente_id` (`RESTRICT`), `seccion_id` (`CASCADE`), `responsable_id`, `actividad` (120), `fecha`, `hora_inicio`, `hora_fin` (horas reales, no números de bloque), `estado` (`ACTIVA` o `CANCELADA`), `creado_por`, `version` (bloqueo optimista) y `creado_en`/`actualizado_en`. La BD solo garantiza `hora_fin > hora_inicio` (`ck_reserva_ambiente_horas`), el estado (`ck_reserva_ambiente_estado`) y las claves foráneas; el resto lo valida el servicio. Índices por (ambiente, fecha), sección y responsable. |

«Pendiente» y «Finalizado» **no se guardan**: se calculan al leer. «Eliminar» es **cancelar**: la fila se conserva como `CANCELADA` y deja de contar para los cruces.

### Endpoints (rol Coordinador, `ReservaAmbienteController`)

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/horarios/reservas?filtro=&page=&size=` | Historial de reservas **activas**, de la más reciente a la más antigua (fecha y hora de inicio descendentes). `filtro` = `SEMANA` (lunes a domingo de la semana de hoy; por defecto), `MES` (mes calendario) o `ANIO` (año calendario). `page` desde 1, `size` 10 por defecto (máximo 100). Responde `{data, meta: {page, size, total, totalPages}}`. 400 si el filtro no existe |
| GET | `/horarios/reservas/opciones` | `{ambientes, responsables}`: los espacios especiales **activos** por código (`AmbienteOpcion`, el mismo de `/secciones/{id}/opciones`) y los usuarios activos, no eliminados, con rol DOCENTE o TUTOR (`Referencia {id, nombre}`) |
| GET | `/horarios/reservas/{id}` | Una reserva. 404 si no existe o ya fue cancelada |
| POST | `/horarios/reservas` | Crea la reserva. Cuerpo `{ambienteId, seccionId, responsableId, actividad, fecha, horaInicio, horaFin}`, todos obligatorios. 201 |
| PUT | `/horarios/reservas/{id}` | Reemplaza los datos. Mismo cuerpo más `version` (la que devolvió la lectura). 200 |
| DELETE | `/horarios/reservas/{id}?version=` | «Eliminar»: cancela. `version` es opcional. 204 |

- **Reserva** (`ReservaDeAmbiente`): `{id, ambiente {id, codigo, nombre}, actividad, anioId, gradoCodigo, seccion {id, etiqueta «4.° B»}, responsable {id, nombre}, fecha, horaInicio, horaFin, estado PENDIENTE|FINALIZADO, editable, version}`. `fecha` es «AAAA-MM-DD»; las horas se reciben «HH:mm» (también «HH:mm:ss») y se devuelven «HH:mm».
- **Códigos:** 400 por regla de forma o cuerpo inválido; 401 sin sesión; 403 sin el rol Coordinador; 404 sección o reserva inexistente; 409 por año cerrado, reserva finalizada o cancelada, versión vieja y cruces. Sin 422.
- **Opciones análogas a las de HU-45.** `/reservas/opciones` hace con los espacios especiales lo que `/secciones/{id}/opciones` hace con los ambientes (`AmbientesActivos`), sin modificar el endpoint de la HU-45. Para año, grado y sección el frontend reutiliza `GET /horarios/anios-escolares` y `/anios-escolares/{anioId}/secciones` (traen también secciones `INACTIVA`: la pantalla debe filtrar las `ACTIVA`).

### Reglas

Las validaciones de forma (1 a 3) se hacen antes del bloqueo del año; los cruces (4, 5, 7 y 8) después. Solo se responde el **primer** cruce, en el orden 4, 5, 7a, 7b, 8. Los mensajes de 409 por cruce llevan «Título: texto» (como el PUT del horario).

| # | Regla | Código | Mensaje |
| --- | --- | --- | --- |
| 1 | El ambiente existe, está activo y es `ESPACIO_ESPECIAL` | 400 | «Elige un espacio especial activo.» |
| 2 | El año de la sección no está `CERRADO` (`PLANIFICADO` y `ACTIVO` se aceptan) | 409 | «No se puede reservar un espacio en un año cerrado.» |
| 2 | Las matrículas que ocupan vacante (todas menos `RETIRADO` y `ANULADA`) caben en el `aforo_maximo` | 400 | «{ambiente} admite hasta {n} personas y la sección tiene {m} matriculados.» |
| 2 | El responsable es elegible (DOCENTE o TUTOR, activo, no eliminado) | 400 | «Elige un docente o tutor activo como responsable.» |
| 3 | `horaFin` posterior a `horaInicio` | 400 | «La hora de fin debe ser posterior a la de inicio.» |
| 3 | `fecha` + `horaInicio` no es anterior a «ahora» (`Clock` de Lima) | 400 | «No se puede reservar en una fecha u hora que ya pasó.» |
| 3 | La fecha está entre `fecha_inicio` y `fecha_fin` del año | 400 | «La fecha debe estar dentro del año escolar {año}.» |
| 3 | La fecha cae dentro de algún `periodo_academico` del año (las vacaciones entre periodos no) | 400 | «La fecha no está dentro de ningún periodo académico del año.» |
| 3 | Día lectivo de la estructura de la sección (la de su horario o, sin él, la principal del nivel) | 400 | «El {día} no es un día de clases de esta sección.» |
| 3 | Las horas caen dentro de la jornada (desde la entrada hasta el fin del último bloque, recesos incluidos) | 400 | «La reserva debe estar dentro de la jornada de la sección, de {inicio} a {fin}.» |
| 4 | Sin otra reserva ACTIVA del mismo ambiente superpuesta (`inicio < otro.fin && otro.inicio < fin`: las contiguas no cruzan) | 409 | «Ambiente no disponible: {ambiente} ya está reservado por {sección} para {actividad} el {día} {dd/MM/aaaa} de {inicio} a {fin}.» |
| 5a | Ninguna **otra** sección tiene ese ambiente en su horario semanal a esa hora (mismo día de la semana, horas reales) | 409 | «Ambiente no disponible: {sección} ya usa {ambiente} para {curso} el {día} de {inicio} a {fin}.» |
| 5b | La **propia** sección, a esa hora: con un aula regular en su horario **se permite** (la clase se traslada al espacio especial) | — | — |
| 5b | …con el **mismo** ambiente en su horario | 400 | «{sección} ya tiene {ambiente} en su horario el {día} de {inicio} a {fin}; no necesita reserva.» |
| 5b | …con **otro** espacio especial en su horario | 409 | «{sección} tiene {curso} en {ambiente} el {día} de {inicio} a {fin}.» |
| 6 | Una reserva cancelada o finalizada no se edita ni se cancela | 409 | «La reserva ya fue cancelada.» · «La reserva ya finalizó y no se puede modificar.» |
| 6 | La `version` de la solicitud debe ser la actual | 409 | «La reserva cambió mientras la editabas. Vuelve a cargarla.» |
| 7a | El responsable no dicta clase a **otra** sección a esa hora (con la propia sí se permite) | 409 | «Responsable no disponible: {docente} ya dicta {curso} con {sección} el {día} de {inicio} a {fin}.» |
| 7b | El responsable no lo es de otra reserva ACTIVA superpuesta | 409 | «Responsable no disponible: {responsable} ya es responsable de {actividad} con {sección} el {día} {dd/MM/aaaa} de {inicio} a {fin}.» |
| 8 | La sección no tiene otra reserva ACTIVA superpuesta | 409 | «Sección no disponible: {sección} ya tiene reservado {ambiente} para {actividad} el {día} {dd/MM/aaaa} de {inicio} a {fin}.» |

Al **editar**, las reglas 4, 7b y 8 excluyen la propia reserva. Los textos de 5a y 7a son los de `MensajesCruce`; los de las reservas, `MensajesReserva`. Un ambiente que ya no está activo y aparece en el horario de la propia sección se trata como aula (ver limitaciones).

### Pendiente y Finalizado

Se calculan al leer con el `Clock` de Lima: una reserva **ACTIVA** está **FINALIZADO** cuando `fecha + horaFin <= ahora` y **PENDIENTE** en caso contrario. `editable` es true solo si está activa y no finalizó. Las canceladas no salen del backend en el listado ni en `GET /{id}`.

### Concurrencia

- **Candado del año.** Crear y reprogramar toman `bloquearAnioParaHorarios(anioId)`, el mismo bloqueo del PUT del horario, **antes** de leer reservas y bloques para buscar cruces. Así dos reservas (o una reserva y un guardado de horario) que chocan se serializan: la segunda ve lo que confirmó la primera. `ReservaConcurrenciaIT` lo comprueba con dos POST en paralelo (mismo LAB con secciones distintas y mismo responsable en ambientes distintos): uno responde 201 y el otro 409; **falla si se quita el bloqueo**.
- **Versión.** La reserva usa `@Version`. Editar o cancelar con una versión vieja responde 409; si el conflicto sale al guardar, el servicio captura `ObjectOptimisticLockingFailureException` y responde el mismo 409. Cancelar no toma el candado del año (no agrega ocupación).

### Datos de prueba

`R__datos_prueba.sql` siembra seis reservas de 2026 de María Fernández (U20260004) registradas por el Coordinador (U20260005): tres futuras (19/11 en LAB-01, 20/11 en LAB-02 y 9/12 en BIB-01), dos pasadas que se ven «Finalizado» (3/11 y 12/11) y una cancelada (27/11, no aparece en el listado). `ReservasDePruebaIT` comprueba que respetan las reglas entre sí. Los datos de prueba no traen horarios semanales.

### Frontend (Coordinador)

Figma: «Reserva de espacios especiales» (308:1874, verificado) y «Nueva reserva» (308:6867 y 308:7130). La ruta sigue siendo `/app/coordinador/ambientes` (`paths.app.coordinador.ambientes`, sin renombrar); `app/routes/app/coordinador/ambientes.tsx` solo monta `ReservasEspacios`. El ítem del menú del Coordinador (`config/navigation.ts`) pasó de «Ambientes físicos» a «Reserva de espacios especiales»; el de Administrativo (HU-43) no cambió. La protección por rol es la de siempre (`RoleGuard` del árbol `coordinador`): otro rol vuelve a su inicio.

**Componentes** (`features/horarios/components/`)

| Archivo | Qué es |
| --- | --- |
| `reservas-espacios.tsx` | La pantalla. Arma el `ContentLayout` (título «Reserva de espacios especiales», migas «Horarios / Reserva de espacios especiales» y el botón «Nueva reserva» + a la derecha), la tabla, el modal y la confirmación de eliminar. Guarda el filtro y la página; al cambiar de filtro vuelve a la página 1 y, al eliminar la última fila de una página que no es la primera, vuelve a la anterior. |
| `tabla-reservas.tsx` | Tarjeta «Historial de reservas»: filtros y tabla Espacio, Actividad, Estado, Fecha, Hora y Acciones, con «Mostrando X-Y de N». |
| `dialogo-reserva.tsx` | El modal, para crear («Nueva reserva» / «Reservar») y para editar («Editar reserva» / «Guardar cambios»). react-hook-form + zod, con `noValidate`. |

**Datos y lógica:** `api/reservas.ts` (lista con `keepPreviousData`, detalle, crear, editar y eliminar), `api/opciones-reserva.ts`, `lib/reservas.ts` (formato de hora, opciones de hora y duración, partir «Título: texto», detector del 409 de versión) y `types-reservas.ts`. Año, grado y sección reutilizan `useAniosParaHorarios` y `useGradosParaHorarios` (`api/selectores.ts`) y la jornada, `useHorarioSeccion`; no se creó ningún endpoint nuevo.

**Decisiones de diseño (Figma 308:1874)**

- **Filtros:** `SelectableTag` Color = Primary, Type = Circle, Size = Medium; uno solo seleccionado, relleno oscuro. «Esta semana» viene elegido.
- **Estado:** `SelectableTag readOnly` circular, tamaño medio: **Pendiente = primary** y **Finalizado = info**.
- **Espacio:** un punto del mismo azul oscuro en todas las filas (no depende del tipo), el código en negrita y el nombre debajo, pequeño y gris. **Actividad:** el texto y, debajo, «{sección} · {responsable}».
- **Hora y fecha:** la hora de inicio en 12 horas, «08:00 am» (`formatHora12`, función nueva: la de la matriz horaria da «8:00am»), y la fecha `dd/mm/aaaa` (`formatDay`).
- **Botones:** «Nueva reserva» = `secondary` con `Plus` a la derecha; «Editar» = `primary-light` pequeño con `Pencil` a la derecha; eliminar = botón de solo ícono `Trash2` con `aria-label="Eliminar reserva"`. «Editar» y eliminar quedan **deshabilitados** si `editable = false` (reserva finalizada).
- **Sin campo Año:** al crear se usa el año ACTIVO y, si no hay, el más reciente que no esté CERRADO; al editar, el `anioId` de la reserva. Grado y Sección van en cascada y las secciones `INACTIVA` no se ofrecen.
- **Edición y eliminar no están en Figma:** la edición es el mismo modal con el título «Editar reserva» y el botón «Guardar cambios»; eliminar pide confirmación con `DialogoConfirmacion` «¿Eliminar la reserva?», que nombra el espacio, la fecha y la hora.
- **No se modificó `components/ui`:** el calendario no puede atenuar días no lectivos, así que el día se valida en el cliente con `jornada.dias` y el resto de las reglas, en el backend.

**Hora y duración desde la jornada**

- **Hora:** los inicios de los bloques de la jornada de la sección elegida (`GET /horarios/secciones/{id}`, el mismo de la HU-45, que comparte caché con esa pantalla). Deshabilitada hasta elegir la sección; si la sección no tiene estructura horaria (409), se muestra el mensaje del backend.
- **Duración:** 1×, 2× y 3× la duración del bloque de esa jornada (por ejemplo, 45, 90 y 135 min). Las que pasarían del fin de la jornada, desde la hora elegida, salen deshabilitadas; si la ya elegida deja de caber al cambiar la hora, se limpia.
- **`horaFin` = `horaInicio` + duración**, calculada en el cliente; el backend recibe `horaInicio` y `horaFin`.
- **Al editar,** si la hora o la duración guardadas no coinciden con las opciones (por ejemplo, una reserva de 8:15 en una jornada con bloques a las 8:00 y 8:45), se agregan como opción para no perder datos.
- **Cambiar el grado** vacía la sección, la hora y la duración; cambiar la sección vacía la hora y la duración (la jornada cambia).

**Errores del backend en el modal** (POST y PUT van con `silenciarNotificacion`, así que no salen como notificación):

- Un 409 de cruce llega como «Título: texto»: se muestra un `Alert` de error pequeño con el **título aparte** si el mensaje empieza por «Ambiente no disponible: », «Responsable no disponible: » o «Sección no disponible: »; los demás mensajes (año cerrado, 5b, finalizada…) van sin título.
- Un 409 de versión («La reserva cambió mientras la editabas…») muestra el aviso con la acción **«Recargar»**, que vuelve a pedir la reserva, rellena el formulario y toma la versión nueva.
- Un 400 de `@Valid` (`errores: {campo: mensaje}`) pone cada mensaje bajo su campo (`horaFin` va bajo «Duración»); un 400 de regla va en el aviso. El formulario valida antes con zod y los mismos mensajes del backend.
- Al eliminar, el DELETE **no** se silencia: sus errores salen como notificación global.
- Éxito: se cierra el modal, avisa «Reserva registrada.» o «Reserva actualizada.» (y «Reserva eliminada.») e invalida `['horarios', 'reservas']`.

**Limitaciones del frontend**

- El calendario no marca los días no lectivos ni los fuera de periodo: el día no lectivo se rechaza al guardar y el resto lo valida el backend.
- Solo se muestran las reservas activas; una cancelada no se puede volver a abrir.

### Limitaciones conocidas

- **El PUT del horario (HU-45/46) no valida contra las reservas existentes.** La detección de cruces de reservas contra horarios es de un solo sentido: una reserva se compara con los horarios guardados, pero un horario guardado después puede superponerse a una reserva. Pendiente de coordinar con HU-45 y el PO.
- **Ambiente desactivado en el horario de la propia sección.** Si, a la hora reservada, la propia sección tiene en su horario un ambiente que ya no está activo, ese ambiente no figura entre los activos y se trata como aula regular (regla 5b): se permite la reserva. Consecuencia: un espacio especial desactivado después de armar el horario no la bloquea.
- **La condición de «ocupa vacante» está duplicada.** `MatriculadosDeSeccionSql` copia `Seccion.SQL_OCUPA_VACANTE` (Configuración) porque `Seccion` es una entidad de otro módulo y no se importa. `MatriculadosDeSeccionSqlIT.laCondicionDeVacanteEsLaMismaQueLaDeConfiguracion` falla si divergen.
- **No hay feriados ni días no lectivos sueltos.** La fecha solo se valida contra el año, los periodos académicos y los días lectivos de la estructura.
- **Fuera de alcance:** asociar un curso, mostrar las reservas en otros horarios y subgrupos (HU-47).

### Código y pruebas

- Backend (`horarios/`): `dominio/ReservaAmbiente` y `EstadoReserva`; `aplicacion/ServicioReservasAmbiente`, `ReglasDeReserva` (reglas puras), `MensajesReserva`, `DtosReservas`, `FiltroDeReservas` y los puertos de solo lectura `ResponsablesElegibles`, `CalendarioDelAnio`, `MatriculadosDeSeccion` y `ConsultaDeReservas`; `infraestructura/ReservaAmbienteRepositorio` y los adaptadores `ResponsablesElegiblesSql`, `CalendarioDelAnioSql`, `MatriculadosDeSeccionSql` y `ConsultaDeReservasSql`; `web/ReservaAmbienteController`.
- Frontend (`frontend/src/features/horarios/`): `components/reservas-espacios.tsx`, `tabla-reservas.tsx` y `dialogo-reserva.tsx`; `api/reservas.ts` y `api/opciones-reserva.ts`; `lib/reservas.ts`; `types-reservas.ts`. Ruta: `app/routes/app/coordinador/ambientes.tsx`. Simuladores: `testing/mocks/fixtures/reservas-ambiente.ts` y `handlers/reservas-ambiente.ts` (`reiniciarReservas`, `fijarAhora`), registrados solo en `handlers` (pruebas).
- Pruebas (`backend/src/test/java/com/galiledu/horarios/`): `aplicacion/ReglasDeReservaTests` y `ServicioReservasAmbienteTests` (sin Spring ni BD); `ReservaAmbienteEsquemaIT` (V17), `ReservaAmbienteRepositorioIT`, `ResponsablesElegiblesSqlIT`, `CalendarioDelAnioSqlIT` y `MatriculadosDeSeccionSqlIT` (adaptadores); `ReservaAmbienteIT` (HTTP: reglas 1 a 8, edición, cancelación, filtros, paginación, opciones y accesos 401/403 de los seis endpoints); `ReservaConcurrenciaIT`; `ReservasDePruebaIT` (datos de prueba). Las pruebas HTTP fijan «hoy» en el 3 de marzo de 2027 con `RelojDeReservasConfig`.
- Pruebas del frontend: `components/__tests__/reservas-espacios.test.tsx` (listado, filtros, paginación, crear, errores 400 y 409, editar con versión y «Recargar», eliminar con confirmación, acciones deshabilitadas y opciones de hora y duración; fijan «hoy» en el 18/11/2026 con `vi.setSystemTime`), `api/__tests__/reservas.test.tsx` (hooks y simuladores), `lib/__tests__/reservas.test.ts` y `app/__tests__/rutas-reservas.test.tsx` (la ruta es solo del Coordinador).

## Código

- Backend: `backend/src/main/java/com/galiledu/horarios/` (`dominio`: `HorarioSeccion`, `HorarioBloque`; `aplicacion`: `ServicioHorarios`, `ReglasDelHorario`, `ResolutorDeJornada`, `ConversorDeBloquesDeJornada`, `DtosHorarios` y las interfaces `ValidadorCruces`, `OcupacionHorarios` y `ConversorDeBloques`; `infraestructura`: repositorios y los adaptadores SQL de solo lectura `DatosDeSeccionSql`, `PlanDeSeccionSql`, `AmbientesActivosSql`, `AniosYSeccionesSql`, `OcupacionHorariosSql` y `JornadaDeSeccionConfiguracion`; `web`: `HorarioController`). HU-46: `ValidadorCrucesHorario`, `MensajesCruce`, `ServicioCrucesHorario` y `SolicitudCruces` (en `aplicacion`, el validador reemplaza a `ValidadorCrucesStub`) y `web/CrucesHorarioController`. HU-49: `ClasesDelDocente`, `ServicioHorarioDocente` y `DtosHorarioDocente` (en `aplicacion`), `ClasesDelDocenteSql` (en `infraestructura`) y `web/HorarioDocenteController`. Configuración consulta el uso de una estructura con `UsoDeEstructura` (`UsoDeEstructuraSql`): ver [Configuración](configuracion.md#estructuras-con-horarios-armados-hu-45).
- Frontend: `frontend/src/features/horarios/` (`asignacion-horarios`, `matriz-coordinador`, `dialogo-asignar-bloque`, `barra-acciones`, `datos-del-horario` y las llamadas de `api/`; `lib/cruces.ts` tiene los títulos de las alertas de cruce). Ruta: `app/routes/app/coordinador/horarios.tsx`. HU-49: `components/horario-docente.tsx`, `api/horario-docente.ts` y `lib/horario-docente.ts`; rutas `app/routes/app/docente/horario.tsx` y `app/routes/app/tutor/horario.tsx`. Los tipos del coordinador están en el feature (`types.ts`): `types/api.ts` tiene los del horario de lectura de la P2.
- Pruebas: `backend/src/test/java/com/galiledu/horarios/` y `frontend/src/features/horarios/`. HU-46: `aplicacion/ValidadorCrucesHorarioTests`, `MensajesCruceTests` y `ServicioCrucesHorarioTests` (sin Spring ni BD), `CrucesHorarioIT` (endpoint), `HorarioConValidadorRealIT` (PUT y GET con el validador real) y `CrucesConcurrenciaIT` (dos guardados a la vez); en el frontend, `alerta-cruces.test.tsx`. `HorarioCrucesIT` sigue usando un validador de mentira para el PUT y el GET de la HU-45. HU-49: `ClasesDelDocenteIT` (el puerto contra la BD), `aplicacion/ServicioHorarioDocenteTests` (sin Spring ni BD) y `HorarioDocenteIT` (los dos endpoints). en el frontend, `horario-docente.test.tsx`, `lib/__tests__/horario-docente.test.ts`, `matriz-horaria-seccion.test.tsx` y `components/ui/__tests__/schedule-block-claves.test.tsx`. HU-51: `HorarioApoderadoIT`, `horario-apoderado.test.tsx` y `lib/__tests__/horario-apoderado.test.ts`.

## Pendientes

- [x] **HU-46 (backend):** `ValidadorCruces` real y `POST /horarios/secciones/{id}/cruces`.
- [x] **HU-46 (frontend):** alertas por tipo con título, ayuda del docente y campo en conflicto marcado en el modal.
- [x] Horario del Apoderado (HU-51) con datos reales.
- [x] **HU-49 (backend):** `GET /horarios/docente/anios` y `GET /horarios/docente/horario`.
- [x] **HU-49 (frontend):** pantallas `/app/docente/horario` y `/app/tutor/horario` (Figma 393:18119) con los endpoints nuevos.
- [ ] Consultar al PO si el tutor debe ver además el horario de la sección que tutorea (hoy solo ve sus clases de dictado).
- [ ] Horario del Estudiante (HU-50, Could): sigue con MSW (`/horarios/mi-seccion`).
- [ ] Decidir con el PO si el PUT debe rechazar solo los cruces de las celdas nuevas o modificadas. Hoy rechaza cualquier cruce de la matriz (decisión 4 de Valeria).
- [ ] Mostrar el ambiente con su código (A-502, como en Figma) en vez de su nombre: `OcupacionHorariosSql` y la matriz.
- [ ] Avisar al Coordinador cuando un cambio de titular en Académico crea un cruce.
- [ ] Subgrupos (HU-47, iteración 3): traerán su propia migración (tabla `grupo`, `horario_bloque.grupo_id` y `docente_id`).
- [x] **HU-48 (frontend):** pantalla «Reserva de espacios especiales» (Figma 308:1874) y modal «Nueva reserva» (308:6867, 308:7130) en `/app/coordinador/ambientes`; el ítem del menú del Coordinador se llama ahora «Reserva de espacios especiales».
- [ ] **HU-48:** que el PUT del horario (HU-45/46) valide contra las reservas existentes (coordinar con HU-45 y el PO) y las demás preguntas de [la decisión](../decisiones/2026-11-18-royal-22-reservas-espacios-especiales.md#pendientes-con-el-po).
- [ ] Código automático de la estructura horaria y la regla de «principal compatible» (ver [Configuración](configuracion.md)).
- [ ] Registrar en una sola entrada de `docs/decisiones/` las decisiones de la HU-45 (docente derivado, versión, cascade, `DiaSemana` compartido, reglas de Configuración con horarios armados y el hueco de editar hora de entrada, duración o recesos).
