# Horarios

- **HU:** HU-45 (iteración 2) · HU-46 (2) · HU-49, 50, 51 · (HU-47, 48 en la 3)
- **Estado:** 🚧 HU-45 hecha en backend (lectura, opciones y guardado) y con su pantalla en el frontend; **falta HU-46 (cruces)**, que hoy es un stub
- **Última actualización:** 2026-11-06

## Alcance

El Coordinador arma el horario semanal de cada sección: elige el curso del plan de evaluación y el ambiente de cada bloque. Un **bloque** no es una fila de catálogo: es el par (día, número de bloque) dentro de la jornada de la estructura horaria, y sus horas se calculan (`CalculadoraJornada` de Configuración). El docente de un bloque **no se guarda**: es el de `academico.docente_seccion` para (curso del plan, sección).

Figma: Coordinador › «HU-45: Asignación de docente y curso a un bloque de horario» (619:2757, 286:14702, 248:9344) y su modal «Asignar bloque horario» (863:56080). El horario de lectura del Apoderado (HU-51) tiene sus propios endpoints (ver abajo); el del Estudiante (HU-50, Could) todavía usa datos simulados.

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
| POST | `/horarios/secciones/{seccionId}/cruces` | **Pendiente (HU-46).** Contrato abajo |

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

## Para HU-46: validación de cruces (Luciana)

Hoy los cruces no se validan: `ValidadorCrucesStub` devuelve siempre lista vacía, así que se puede poner al mismo docente (o aula) a la misma hora en dos secciones. Todo lo demás ya está cableado; solo faltan estas piezas.

**1. Implementar `ValidadorCruces`** (`com.galiledu.horarios.aplicacion`), borrando `ValidadorCrucesStub` (con los dos beans, Spring no sabe cuál inyectar):

```java
List<CruceDetectado> buscarCruces(UUID anioId, UUID seccionId, List<BloqueSolicitado> solicitados);
List<CruceDetectado> compararCruces(List<BloqueSolicitado> solicitados, List<BloqueOcupado> ocupados); // lógica pura
```

`buscarCruces` pide los ocupados con `OcupacionHorarios.bloquesOcupados(anioId, seccionId)` (ya real: `OcupacionHorariosSql`; devuelve los bloques de **todas las demás secciones** del año) y delega en `compararCruces`.

```java
record BloqueSolicitado(DiaSemana dia, int numeroBloque, LocalTime inicio, LocalTime fin,
                        UUID planCursoId, UUID docenteId, UUID ambienteId)
record BloqueOcupado(UUID seccionId, DiaSemana dia, LocalTime inicio, LocalTime fin, UUID docenteId,
                     UUID ambienteId, String seccion, String curso, String docente, String ambiente)
record CruceDetectado(TipoCruce tipo /* DOCENTE | AMBIENTE */, DiaSemana dia, int numeroBloque,
                      String mensaje, String seccion, String curso)
```

- `numeroBloque` del cruce es el del bloque **solicitado**; `seccion` y `curso` son los de la sección con la que choca (por ejemplo «1.° B» y «Álgebra»). `OcupacionHorarios` ya llena los nombres para armar el mensaje.
- Un bloque **solapa** con otro si es el mismo día y `inicio < otro.fin && otro.inicio < fin`. Se comparan **horas reales**, no números de bloque: cada sección usa su propia estructura (primaria puede empezar a las 08:30 y secundaria a las 08:00). Dos bloques seguidos (`fin == inicio`) no se cruzan.
- Cruce de docente: mismo `docenteId`. Cruce de ambiente: mismo `ambienteId`. `docenteId` puede ser null en un ocupado (curso sin docente): ahí solo puede haber cruce de ambiente.
- Un bloque solicitado puede cruzarse con varios; devuelve un `CruceDetectado` por cada par (el PUT muestra el mensaje del primero).

**2. Endpoint `POST /horarios/secciones/{seccionId}/cruces`** (`HorarioController`, rol COORDINADOR). El modal «Asignar bloque horario» lo llama con el borrador completo, con una pausa de 300 ms, y deshabilita «Asignar» si responde cruces para ese bloque:

```json
// cuerpo
{ "celdas": [ { "dia": "LUN", "numeroBloque": 1, "planCursoId": "…", "ambienteId": "…" } ] }
// respuesta 200: la lista de CruceDetectado (la misma forma que `cruces` de cada celda del GET)
[ { "tipo": "DOCENTE", "dia": "LUN", "numeroBloque": 1, "mensaje": "…", "seccion": "1.° B", "curso": "Álgebra" } ]
```

Se resuelve con `ConversorDeBloques.aBloquesSolicitados(seccionId, celdas)` y luego `ValidadorCruces.buscarCruces(...)`. 404 si la sección no existe. No escribe nada. Mientras no exista, el frontend ignora el error (no muestra alerta ni bloquea al usuario).

**3. Qué ya hacen los demás con tu implementación**
- El **PUT** pasa todas las celdas por `buscarCruces` y responde 409 con el mensaje del primer cruce, sin escribir nada (probado con un validador falso en `HorarioCrucesIT`).
- El **GET** reparte los cruces en cada celda (`celdas[].cruces`) y la matriz marca esas celdas en amarillo con el mensaje.
- El conversor **omite** las celdas cuyo curso no tiene docente; el PUT ya las rechaza antes (409), así que no llegan al validador.

**4. Decisiones que te tocan**
- Con tu implementación, el PUT rechaza el guardado si **cualquier** celda de la matriz tiene cruce, también las que no se tocaron. Si un horario ya guardado quedó cruzado (por ejemplo, porque se cambió la hora de entrada de una estructura), el Coordinador no podrá guardar hasta arreglarlo. Decide si se rechazan solo los cruces de celdas nuevas o modificadas.
- El GET calcula los cruces cada vez que se abre la matriz (una consulta de ocupación del año). Si pesa, se puede cachear dentro de la petición.

**5. Pruebas de aceptación sugeridas** (datos de prueba: 2026, 1.° de secundaria A y B)
1. María Fernández (`U20260004`) dicta Álgebra en 1.° A y 1.° B: el mismo día y bloque en ambas → cruce de DOCENTE; en otro día u hora no.
2. Mismo ambiente (por ejemplo «Aula 101») a la misma hora en dos secciones → cruce de AMBIENTE.
3. Primaria (entrada 08:30) y secundaria (08:00) con un docente compartido: el bloque 2 de una solapa con el 1 y el 2 de la otra aunque tengan distinto número.
4. Bloques contiguos (`fin == inicio`) → sin cruce.
5. La sección que se edita no se cruza consigo misma (`OcupacionHorarios` ya la excluye).
6. Un ocupado sin docente: no hay cruce de docente, pero sí de ambiente si coincide.

## Código

- Backend: `backend/src/main/java/com/galiledu/horarios/` (`dominio`: `HorarioSeccion`, `HorarioBloque`; `aplicacion`: `ServicioHorarios`, `ReglasDelHorario`, `ResolutorDeJornada`, `ConversorDeBloquesDeJornada`, `DtosHorarios` y las interfaces `ValidadorCruces`, `OcupacionHorarios` y `ConversorDeBloques`; `infraestructura`: repositorios y los adaptadores SQL de solo lectura `DatosDeSeccionSql`, `PlanDeSeccionSql`, `AmbientesActivosSql`, `AniosYSeccionesSql`, `OcupacionHorariosSql` y `JornadaDeSeccionConfiguracion`; `web`: `HorarioController`). Configuración consulta el uso de una estructura con `UsoDeEstructura` (`UsoDeEstructuraSql`): ver [Configuración](configuracion.md#estructuras-con-horarios-armados-hu-45).
- Frontend: `frontend/src/features/horarios/` (`asignacion-horarios`, `matriz-coordinador`, `dialogo-asignar-bloque`, `barra-acciones`, `datos-del-horario` y las llamadas de `api/`). Ruta: `app/routes/app/coordinador/horarios.tsx`. Los tipos del coordinador están en el feature (`types.ts`): `types/api.ts` tiene los del horario de lectura de la P2.
- Pruebas: `backend/src/test/java/com/galiledu/horarios/` (HU-51: `HorarioApoderadoIT`) y `frontend/src/features/horarios/` (HU-51: `horario-apoderado.test.tsx` y `lib/__tests__/horario-apoderado.test.ts`).

## Pendientes

- [ ] **HU-46:** `ValidadorCruces` real y `POST /horarios/secciones/{id}/cruces` (ver arriba).
- [x] Horario del Apoderado (HU-51) con datos reales.
- [ ] Horario del Estudiante (HU-50, Could): sigue con MSW (`/horarios/mi-seccion`).
- [ ] Subgrupos (HU-47, iteración 3): traerán su propia migración (tabla `grupo`, `horario_bloque.grupo_id` y `docente_id`).
- [ ] Código automático de la estructura horaria y la regla de «principal compatible» (ver [Configuración](configuracion.md)).
- [ ] Registrar en una sola entrada de `docs/decisiones/` las decisiones de la HU-45 (docente derivado, versión, cascade, `DiaSemana` compartido, reglas de Configuración con horarios armados y el hueco de editar hora de entrada, duración o recesos).
