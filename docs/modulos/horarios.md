# Horarios

- **HU:** HU-45, HU-46, HU-49 · (HU-47, HU-48, HU-50, HU-51)
- **Estado:** 🚧 HU-46 hecha (backend); HU-45 y HU-49 por completar
- **Última actualización:** 2026-11-05

## Alcance

El Coordinador arma el horario semanal de cada sección: elige para cada día y bloque de la jornada un curso del plan y un ambiente. El docente de cada celda no se elige: es el titular del curso en esa sección (`academico.docente_seccion`). Esta ficha detalla la **detección de cruces (HU-46)**, que impide que un docente o un ambiente estén en dos lugares a la vez. Las partes de la HU-45 (matriz, modal, versiones) y de la HU-49 (horario del docente y del tutor) están por completar.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-45 | Armar el horario de una sección | 🚧 | Por completar (Valeria). El backend (GET, opciones y PUT) está en la [tabla de API](../../backend/README.md). |
| HU-46 | Detectar cruces de docente y de ambiente | ✅ backend | Cubre el RF-117 y el RF-118. Ver [Cruces de horario](#cruces-de-horario-hu-46). Falta el modal del frontend, que llamará al endpoint con el borrador. |
| HU-49 | Horario del docente y del tutor | ⏳ | Por completar. |

## Pantallas (Figma)

Archivo de Figma `l8nXzicItzBnvPjFQP4G9O` (nodos):

| Pantalla | Nodo en Figma | Ruta |
| --- | --- | --- |
| Asignación de horarios, vacía | `619:2757` | `/app/coordinador/horarios` (hoy un marcador de posición) |
| Asignación de horarios, con datos | `248:9344` | `/app/coordinador/horarios` |
| Modal «Asignar bloque horario» | `802:12011` | `/app/coordinador/horarios` |
| Modal con cruce (HU-46) | `296:17801` | `/app/coordinador/horarios` |
| Horario del docente y del tutor (HU-49) | `393:18119` | `/app/docente/horario` y `/app/tutor/horario` |

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
- **Modal «Asignar bloque horario»** (frontend, pendiente): llama a este endpoint con el borrador de celdas antes de guardar.

### Concurrencia

El PUT toma el **bloqueo del año** (`bloquearAnioParaHorarios`) **antes** de leer la cabecera y de buscar cruces. Dos guardados simultáneos en secciones distintas se serializan: el segundo ve lo que confirmó el primero y recibe el 409. `CrucesConcurrenciaIT` lo comprueba con dos PUT en paralelo (mismo docente con aulas distintas, y mismo ambiente con docentes distintos): uno responde 200 y el otro 409. La prueba **falla si se quita el bloqueo**: sin él, las 5 corridas de verificación terminaron con los dos PUT en 200 y el cruce guardado. El endpoint de cruces no toma el bloqueo: es un aviso, y la garantía está en el PUT.

### Limitaciones conocidas

- **Cambio de titular en Académico.** Si se cambia el docente de un curso en una sección que ya tiene horario, el cruce no se impide (Académico no consulta Horarios), pero el GET lo muestra en la celda.
- **El día no se valida en el endpoint.** El conversor no compara el día con los días lectivos de la estructura; una celda en un día no lectivo puede dar cruces. Lo valida el PUT («día no lectivo», 400).
- **RF-122 adelantado.** El RF-122 (espacios especiales: una sección o subgrupo por bloque) es de la HU-48; la misma comparación de ambientes lo deja adelantado.
- **Diferencias con Figma.** (a) El texto de Figma dice «Seleccione otro docente», pero en el modal el docente no se elige: es el titular de `docente_seccion`; el frontend mostrará otra ayuda. (b) `OcupacionHorariosSql` llena el ambiente con su **nombre** y la matriz de Figma muestra el **código** (A-502): queda pendiente para Valeria.

## Base de datos

- **Esquema:** `horarios` · **Migraciones:** `V10__horarios.sql`

| Tabla | Para qué |
| --- | --- |
| `horarios.horario_seccion` | Cabecera del horario de una sección: su estructura horaria y su `version` |
| `horarios.horario_bloque` | Bloques asignados: día, número de bloque, curso del plan y ambiente |

La HU-46 no agrega tablas ni migraciones: lee `horarios`, `academico.docente_seccion`, `configuracion` y `personas` (nombre del docente).

## API

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| GET | `/horarios/secciones/{seccionId}` | Coordinador | Matriz de la sección, con los `cruces` de cada celda (HU-45) |
| GET | `/horarios/secciones/{seccionId}/opciones` | Coordinador | Cursos y ambientes del modal (HU-45) |
| PUT | `/horarios/secciones/{seccionId}` | Coordinador | Guarda la matriz completa; 409 con el primer cruce (HU-45, HU-46) |
| POST | `/horarios/secciones/{seccionId}/cruces` | Coordinador | Cruces de un borrador de celdas, sin guardar (HU-46) |

El detalle de los tres primeros está en [`backend/README.md`](../../backend/README.md).

## Código

- Backend: `backend/src/main/java/com/galiledu/horarios/`
  - HU-46: `aplicacion/ValidadorCruces` (interfaz), `ValidadorCrucesHorario` (lógica pura y consulta), `MensajesCruce`, `CruceDetectado`, `TipoCruce`, `ServicioCrucesHorario`, `SolicitudCruces`; `infraestructura/OcupacionHorariosSql` (bloques ocupados con las horas de la estructura de cada sección); `web/CrucesHorarioController`.
- Frontend: `frontend/src/features/horarios/` (por completar)
- Pruebas: `backend/src/test/java/com/galiledu/horarios/`
  - HU-46: `aplicacion/ValidadorCrucesHorarioTests` y `ServicioCrucesHorarioTests` (sin Spring ni BD), `CrucesHorarioIT` (endpoint), `HorarioConValidadorRealIT` (PUT y GET con el validador real), `CrucesConcurrenciaIT` (dos guardados a la vez). `HorarioCrucesIT` sigue usando un validador de mentira para probar el PUT y el GET de la HU-45.

## Pendientes

- [ ] Completar la parte de la HU-45 (Valeria): alcance, reglas del horario, pantallas y frontend.
- [ ] Completar la HU-49 (horario del docente y del tutor).
- [ ] Valeria: que `OcupacionHorariosSql` (y la matriz) muestren el código del ambiente, como en Figma, y no su nombre.
- [ ] Frontend de la HU-46: marcar los cruces en la matriz y llamar al endpoint desde el modal con el borrador.
- [ ] Decidir con Académico cómo avisar al Coordinador cuando un cambio de titular crea un cruce.
