# GalilEdu · Backend

Java 21 · Spring Boot 4.1 · PostgreSQL 18 · Flyway · Spring Security (JWT).
Cómo levantarlo: ver el [README principal](../README.md).

## Configuración

Variables de entorno o `backend/.env` (ignorado por Git; plantilla en [`.env.example`](.env.example)):

| Variable | Para qué | Por defecto |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL | `jdbc:postgresql://localhost:5433/galiledu`, `postgres`, vacío |
| `JWT_SECRET` | Firma de los tokens (mín. 32 caracteres) | secreto de desarrollo — **cámbialo al desplegar** |
| `CORS_ORIGINS` | Origen del frontend | `http://localhost:3000` |
| `FLYWAY_LOCATIONS` | Qué migraciones se aplican | `classpath:db/migration,classpath:db/datos-prueba` — en producción usa solo `classpath:db/migration` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USER`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP para enviar credenciales (RF-17) | sin SMTP, las credenciales se escriben en el log |

## Estructura

```
src/main/java/com/galiledu/
├── compartido/     seguridad (JWT, CORS), errores y paginación comunes
├── personas/       persona (datos personales, validaciones RF-12/14/23)
├── usuarios/       login (HU-1), gestión de cuentas (HU-6, HU-9, HU-12) y carga masiva (HU-8)
├── estudiantes/    inscripción del estudiante (HU-7)
└── academico/      catálogo académico: plan curricular, escala de calificación y plan de evaluación (HU-52 a HU-56)

Configuración, matrícula y pagos: por ahora solo tienen BD (paquetes por crear).

Cada módulo:  dominio/ (entidades JPA, reglas)  aplicacion/ (servicios + DTOs)
              infraestructura/ (repositorios)   web/ (controladores REST)

src/main/resources/db/
├── migration/      V1…V8 (ver la tabla de abajo)
└── datos-prueba/   R__datos_prueba.sql (usuarios por rol, año 2026/2027, familia Paredes con matrícula y pagos…)
```

Reglas: un esquema de BD por módulo; los controladores no tocan repositorios directamente; los mensajes de error van en español porque el frontend los muestra tal cual (`{ "message": "..." }`).

## Base de datos

| Migración | Esquemas | Qué crea |
| --- | --- | --- |
| `V1__personas_y_seguridad.sql` | `personas`, `seguridad` | Personas, usuarios, roles |
| `V2__estudiantes_inscripcion.sql` | `estudiantes` | Estudiante, ficha médica, familia, contactos, autorizados |
| `V3__estudiantes_documento_apoderado.sql` | `estudiantes` | Documento que acredita al apoderado |
| `V4__configuracion_anio_escolar.sql` | `configuracion`, `matricula`, `pagos` | Año escolar, periodos, grados, secciones, estructura horaria, periodo de matrícula, tarifario |
| `V5__matricula_y_pagos.sql` | `matricula`, `pagos` | Matrícula, documentos, compromiso de pago, cuotas, descuentos, órdenes de pago, pagos y constancias |
| `V6__catalogo_academico.sql` | `academico`, `configuracion` | Áreas, competencias, cursos, plan de evaluación, tabla de conversión, ambientes |
| `V7__ajustes_integracion.sql` | varios | Grado del estudiante del catálogo, estado ACTIVO/RETIRADO, estructura horaria por nivel, descuento por estudiante, formato del código de matrícula |
| `V8__configuracion_periodo_sin_estado.sql` | `configuracion` | Elimina `periodo_academico.estado`: el estado del periodo se calcula con las fechas |

Qué tabla sirve para qué y qué reglas valida la BD: en la ficha de cada módulo ([`docs/modulos/`](../docs/modulos/README.md)). Una migración ya subida **no se edita**: los cambios van en una nueva (V9…).

## API (prefijo `/api`)

Todas requieren `Authorization: Bearer <jwt>` salvo el login.

| Método | Ruta | Rol | Qué hace |
| --- | --- | --- | --- |
| POST | `/auth/login` | público | `{username, password}` → `{jwt, user}`. 401 credenciales, 423 bloqueada |
| GET | `/auth/me` | cualquiera | `{data: user}` |
| POST | `/auth/logout` | público | 204 (el token se borra en el frontend) |
| GET | `/usuarios?q=&rol=&estado=&page=&size=` | Administrador | Lista paginada `{data, meta}` |
| GET | `/usuarios/resumen` | Administrador | Datos del Inicio del administrador |
| GET | `/usuarios/roles` | Administrador | Roles asignables |
| GET / PUT | `/usuarios/{id}` | Administrador | Detalle / edición (persona + roles). El detalle trae `estudiantesACargo` (Apoderado) y `tutorDe` (Tutor, año escolar en curso). Mismas reglas que el alta; conserva Apoderado, Tutor y Estudiante si ya los tiene |
| POST | `/usuarios` | Administrador | Alta; devuelve usuario generado (la contraseña va por correo). Exige todos los datos personales salvo apellido materno, teléfono de casa y exalumno (CAHU6); no acepta los roles Apoderado, Tutor ni Estudiante |
| POST | `/usuarios/{id}/desactivar` · `/reactivar` | Administrador | HU-12 |
| DELETE | `/usuarios/{id}` | Administrador | Borrado lógico |
| GET | `/usuarios/carga-masiva/plantilla` | Administrador | Plantilla `.xlsx` de la carga masiva (HU-8) |
| POST | `/usuarios/carga-masiva/validacion` | Administrador | Revisa el archivo (multipart `archivo`: .xlsx/.xls/.csv ≤ 10 MB) y devuelve errores por fila, sin crear nada |
| POST | `/usuarios/carga-masiva` | Administrador | Crea los usuarios de las filas válidas y devuelve el resumen |
| GET | `/estudiantes?q=&estado=&page=` | Admin, Personal adm. | Lista de estudiantes. `estado`: INSCRITO, MATRICULADO o RETIRADO (se calcula desde la matrícula) |
| GET | `/estudiantes/{id}` | Admin, Personal adm. | Ficha (datos, ficha médica, familia, contactos, hermanos) |
| GET | `/estudiantes/{id}/edicion` | Admin, Personal adm. | "Editar": la inscripción con la forma de `SolicitudInscripcion` (`datos`), más `matriculado`, `conCuenta` y `acreditacion` |
| PUT | `/estudiantes/{id}` | Admin, Personal adm. | "Editar": mismas validaciones que la inscripción. Matriculado no cambia grado ni año; si cambia el responsable, el anterior queda inactivo (salvo que lo sea de otro estudiante); si cambia el correo de un familiar con cuenta, se le envía una contraseña nueva |
| GET | `/estudiantes/personas?documento=` | Admin, Personal adm. | Busca padre/madre/apoderado ya registrado (404 = persona nueva). `cuentaEliminada` indica si su usuario está eliminado |
| GET | `/estudiantes/documentos?documento=` | Admin, Personal adm. | Paso 1: ¿se puede inscribir un estudiante con ese documento? `estado`: `LIBRE` y `REUTILIZABLE` (eliminado) sí; `ESTUDIANTE_ACTIVO`, `ESTUDIANTE_RETIRADO` y `OTRA_PERSONA` no (`permitido`, `mensaje`) |
| GET | `/estudiantes/correos?correo=` | Admin, Personal adm. | Paso 3: `{"disponible": true|false}`, si ninguna persona tiene ese correo (sin importar mayúsculas); no dice de quién es |
| POST | `/estudiantes/inscripciones` | Admin, Personal adm. | Registra la inscripción completa (5 pasos). El grado va como código del catálogo: `gradoCodigo` = `PRI-1`…`PRI-6`, `SEC-1`…`SEC-5`. Con el documento de un estudiante **eliminado** no crea uno nuevo: lo restaura (mismo id) y reemplaza sus datos, ficha médica, familia, contactos y autorizados por los de la inscripción; con uno activo o retirado responde 409 |
| POST | `/estudiantes/{id}/retirar` | Admin, Personal adm. | "Inactivar": pasa a RETIRADO (409 si ya lo está). 204 |
| POST | `/estudiantes/{id}/reactivar` | Admin, Personal adm. | Quita el retiro (409 si no está retirado). 204 |
| DELETE | `/estudiantes/{id}` | Admin, Personal adm. | Borrado lógico: deja de salir en el listado y en la ficha, y conserva su historial. 204 |
| POST | `/estudiantes/{id}/apoderado/documento` | Admin, Personal adm. | Sube o reemplaza el documento del apoderado (multipart: `tipo`, `archivo`; PDF/JPG/PNG ≤ 5 MB) |
| GET | `/estudiantes/{id}/apoderado/documento` | Admin, Personal adm. | Descarga ese documento |
| GET | `/areas/resumen` | Personal adm. | Áreas, competencias y cursos activos de los registrados, y el año escolar activo (CI-14) |
| GET | `/areas` | Personal adm. | Áreas en orden, con cuántas competencias y cursos tiene cada una |
| GET / PUT | `/areas/{id}` | Personal adm. | Detalle con competencias y cursos / edición `{nombre, descripcion}` (HU-52) |
| POST | `/areas` | Personal adm. | Nueva área (HU-52). 409 si el nombre ya existe |
| POST | `/areas/{id}/desactivar` · `/reactivar` | Personal adm. | Cambia el estado del área |
| POST | `/areas/{id}/competencias` | Personal adm. | Nueva competencia `{nombre, descripcion}` (HU-53). 409 si el área está inactiva o el nombre ya está en el área |
| PUT | `/competencias/{id}` | Personal adm. | Edita la competencia |
| POST | `/competencias/{id}/desactivar` · `/reactivar` | Personal adm. | Cambia el estado de la competencia |
| POST | `/areas/{id}/cursos` | Personal adm. | Nuevo curso `{nombre, grados: ["SEC-1", …]}` (HU-54). 409 si el área está inactiva o el nombre ya existe |
| PUT | `/cursos/{id}` | Personal adm. | Edita el curso. 409 si quita un grado en el que ya está planificado |
| POST | `/cursos/{id}/desactivar` · `/reactivar` | Personal adm. | Cambia el estado del curso |
| GET | `/tablas-conversion` | Personal adm. | Versiones de la tabla de conversión (HU-55), de la más nueva a la más antigua; `estado`: PROGRAMADA, VIGENTE o ANTERIOR (se calcula con la fecha de hoy) |
| GET | `/tablas-conversion/vigente` | Personal adm. | La versión que rige hoy. 404 si no hay ninguna |
| POST | `/tablas-conversion` | Personal adm. | Nueva versión `{vigenteDesde, niveles: [{nivel, notaDesde, notaHasta}]}`. 400 si la fecha es pasada o los rangos no cubren 0–20 sin vacíos ni cruces; 409 si ya hay una versión con esa fecha |
| GET | `/planes-evaluacion?anioEscolarId=&grado=` | Personal adm. · Coordinador | Plan de evaluación del grado (HU-56): tarjetas de CI-17, secciones con tutor y cursos con horas, competencias, escala y docentes asignados |
| GET | `/planes-evaluacion/anios` · `/planes-evaluacion/docentes` | Personal adm. · Coordinador | Años escolares (del más reciente) · usuarios activos con rol Docente |
| GET | `/planes-evaluacion/cursos-disponibles?anioEscolarId=&grado=` | Personal adm. · Coordinador | Cursos activos que se dictan en el grado y aún no están en su plan, con las competencias activas del área |
| GET | `/planes-evaluacion/origenes?anioEscolarId=&grado=` | Personal adm. · Coordinador | Otros planes con cursos, para copiarlos |
| POST | `/planes-evaluacion` | Personal adm. · Coordinador | Agrega un curso `{anioEscolarId, gradoCodigo, cursoId, horasSemanales, competencias: [{competenciaId, escala, reglaNota, consolidacion}]}`. 409 si ya está, si no se dicta en el grado o si las horas superan los bloques |
| POST | `/planes-evaluacion/copias` | Personal adm. · Coordinador | Copia `{anioEscolarId, gradoCodigo, origenAnioEscolarId, origenGradoCodigo}` a un grado vacío, sin docentes |
| GET · PUT · DELETE | `/planes-evaluacion/{id}` | Personal adm. · Coordinador | Detalle del curso en el plan · `{horasSemanales, competencias}` · quitarlo del plan. 409 si el año está cerrado |
| GET · PUT | `/planes-evaluacion/{id}/docentes` | Personal adm. · Coordinador | Docente de cada sección `{asignaciones: [{seccionId, docenteId}]}` (`docenteId` null = sin docente) |
| GET | `/pagos/estado-cuenta?anio=` | Personal adm. | P2 · Estado de cuenta (HU-37): compromisos del año (por defecto el ACTIVO) con sección, antigüedad y deuda vencida de cada alumno, y las tarjetas comprometido, cobrado (%), deuda vencida y cuotas vencidas (solo compromisos VIGENTES) |
| GET | `/pagos/estudiantes/{estudianteId}/compromisos` | Personal adm. | Compromisos de pago del estudiante, del año más reciente al más antiguo (selector de P8, HU-37) |
| GET | `/pagos/compromisos/{id}` | Personal adm. | Resumen (total anual, capital pagado, descuento, pensión, saldo) y cronograma de cuotas; `vencida` se calcula con los días de gracia (HU-37, RF-84, RF-91) |
| POST | `/pagos/matriculas/{matriculaId}/cronograma` | Personal adm. | Genera las pensiones del tarifario para una matrícula MATRICULADO: solo las que vencen después de la matrícula y con el descuento activo (RF-81, RF-82). 409 si ya existen (RF-83), si no está matriculado o si el tarifario no está confirmado |
| POST | `/pagos/compromisos/{id}/retiro` | Personal adm. | Anula las pensiones PENDIENTES que vencen después de `{fechaRetiro}`; las vencidas y parciales se mantienen (RF-85). Devuelve `{cuotasAnuladas}` |
| POST | `/pagos/matriculas/{matriculaId}/cuota-matricula` | Personal adm. | Genera la cuota de matrícula PENDIENTE con el monto y el vencimiento del tarifario vigente del año (y el descuento activo si aplica a MATRICULA); crea el compromiso si no existe (HU-36, RF-78). 409 si ya existe (RF-79), si la matrícula ya está MATRICULADO/RETIRADO/ANULADA o si el tarifario del año no existe o no está CONFIRMADO (HU-34) |
| GET | `/pagos/matriculas/{matriculaId}/cuota-matricula` | Personal adm. | Concepto, monto, vencimiento, estado, fecha de pago y responsable de la cuota de matrícula (HU-36, RF-80; tarjeta "Pago de matrícula" de B6/B7). 404 si aún no se generó |
| GET | `/configuracion/anios-escolares` | Personal adm. | Años escolares, del más reciente al más antiguo, con régimen, estado, cantidad de periodos, secciones activas y estructuras horarias (arreglo simple, HU-74) |
| GET | `/configuracion/anios-escolares/actual` | Personal adm. | Card del año en curso (CI-01): periodo en curso, periodo ya terminado con notas abiertas, próximo periodo, secciones activas, alumnos matriculados, estructuras y avance (%). Los periodos son `null` si no aplican. 404 si no hay año activo |
| GET | `/configuracion/anios-escolares/{id}` | Personal adm. | El año con sus periodos ordenados por número; cada periodo trae `duracionSemanas`, su `estado` (se calcula con las fechas) y `registroNotasAbierto` (HU-74) |
| POST | `/configuracion/anios-escolares` | Personal adm. | Crea un año PLANIFICADO `{anio, fechaInicio, fechaFin, regimen}` y genera sus 4 bimestres o 3 trimestres con fechas sugeridas. 400 si `fechaFin` no es posterior a `fechaInicio`, si el año es muy corto o está fuera de 2000–2100; 409 si el año ya existe |
| GET | `/configuracion/anios-escolares/{anioId}/secciones?nivel=` | Personal adm. | Grados del año con sus secciones, tutor, cotutor y alumnos que ocupan vacante (HU-75). `nivel` opcional; arreglo simple |
| POST | `/configuracion/anios-escolares/{anioId}/secciones` | Personal adm. | Nueva sección `{gradoCodigo, nombre, capacidadMaxima}`. 409 si el nombre ya existe en el grado o el año no es modificable |
| PUT | `/configuracion/secciones/{id}` | Personal adm. | Cambia la capacidad `{capacidadMaxima}` (1–45). 400 si es menor a los alumnos que ya ocupan vacante (RF-50) |
| PUT | `/configuracion/secciones/{id}/tutores` | Personal adm. | Tutor y cotutor `{tutorId, cotutorId}`. 400 si no son docentes activos o son el mismo usuario; 409 si el tutor ya lo es de otra sección activa del año |
| DELETE | `/configuracion/secciones/{id}` | Personal adm. | Elimina la sección (204). 409 si tiene matrículas o cambios de sección: "La sección tiene matrículas o cambios de sección registrados. Inactívala para que no reciba más alumnos." |
| POST | `/configuracion/secciones/{id}/inactivar` · `/reactivar` | Personal adm. | Cambia el estado. Reactivar da 409 si su tutor ya lo es de otra sección activa |
| GET | `/configuracion/secciones/tutores-disponibles?anioEscolarId=` | Personal adm. | Docentes elegibles con `esTutorEsteAnio`. 400 sin `anioEscolarId` |
| PUT | `/configuracion/anios-escolares/{id}` | Personal adm. | Cambia las fechas `{fechaInicio, fechaFin}` y regenera los periodos con fechas sugeridas. 400 si `fechaFin` no es posterior o el año es muy corto; 404 si no existe; 409 si el año no está PLANIFICADO |
| PUT | `/configuracion/anios-escolares/{id}/regimen` | Personal adm. | Cambia el régimen `{regimen}` y regenera los periodos con fechas sugeridas. 404 si no existe; 409 si el año no está PLANIFICADO |
| GET | `/configuracion/anios-escolares/{id}/requisitos` | Personal adm. | Card "Antes de activar el año" (CI-06): `{puedeActivar, obligatorios, recomendados}`, cada requisito con `{codigo, texto, cumplido, detalle}`. 404 si no existe |
| POST | `/configuracion/anios-escolares/{id}/activar` | Personal adm. | Pasa el año de PLANIFICADO a ACTIVO y devuelve el detalle. 404 si no existe; 409 si no está PLANIFICADO, si otro año sigue activo ("El año 2026 sigue activo. Ciérralo antes de activar este.") o si faltan requisitos obligatorios ("Faltan requisitos obligatorios: …"). El botón Activar aún no está en Figma (CI-06) |
| POST | `/configuracion/anios-escolares/{id}/cerrar` | Personal adm. | Cierra un año ACTIVO y devuelve el detalle. 404 si no existe; 409 si no está ACTIVO o si aún no vence la fecha límite de notas del último periodo ("Los docentes pueden registrar notas hasta el dd/MM/yyyy."). "Cerrar año" solo aparece en CI-12 |
| PUT | `/configuracion/periodos-academicos/{id}` | Personal adm. | Edita el periodo `{nombre, fechaInicio, fechaFin, fechaLimiteNotas}`. 400 si hay fechas fuera del año, cruce con otro periodo (`Se cruza con el IV Bimestre, que empieza el 19/10/2026.`) o límite anterior al fin; 404 si no existe; 409 si el año está cerrado, el periodo ya empezó (no cambia su inicio) o ya terminó (solo cambia la fecha límite de notas) |
| GET | `/configuracion/tarifarios?anioEscolarId=` | Personal adm. | P19: conceptos y montos, morosidad y calendario de cuotas con su total, más `estado`, `existe`, `soloLectura` (con `motivoSoloLectura`) y `calendarioDesactualizado`. Si el año no tiene tarifario devuelve una propuesta (`existe = false`) con los valores del año anterior o vacíos y el calendario generado con las fechas del año. 404 si el año no existe |
| POST | `/configuracion/tarifarios/{anioEscolarId}/confirmar` | Personal adm. | Botón "Confirmar tarifario" `{montoCuotaIngreso, montoMatricula, montoPension, diasGracia, recargoPorcentaje, topeRecargoPorcentaje}`: crea o actualiza el tarifario, regenera el calendario con las fechas del año y lo deja CONFIRMADO. 400 con el primer dato inválido; 409 si el año está CERRADO o el tarifario CONFIRMADO ya tiene compromisos de pago; 404 si el año no existe (HU-34, RF-70 a RF-73) |
| GET | `/configuracion/anios-escolares/{anioId}/estructuras-horarias` | Personal adm. | Estructuras del año (HU-44, CI-12). Devuelve `{anioModificable, estructuras}`, un objeto y no un arreglo: cada estructura trae código, descripción, nivel, `esPrincipal`, días lectivos, hora de entrada y de salida, bloques por día, duración, recesos y bloques por semana. 404 si el año no existe |
| GET | `/configuracion/estructuras-horarias/{id}` | Personal adm. | CI-12 · Jornada semanal: lo anterior más `anioModificable` y la `jornada` calculada (filas `BLOQUE` y `RECESO` con inicio, fin y duración). 404 si no existe |
| POST | `/configuracion/anios-escolares/{anioId}/estructuras-horarias` | Personal adm. | Crea (201) `{codigo, descripcion, nivel, esPrincipal, diasLectivos, horaEntrada, bloquesPorDia, duracionBloqueMin, recesos: [{despuesDeBloque, duracionMin}]}` y devuelve `{estructura, advertencias}`. La primera del nivel nace principal aunque `esPrincipal` sea false. 400 si falla una regla de forma (código solo A-Z, números y guion, hasta 20; sin caracteres de control; días, bloques, duración, recesos, hora límite de las 18:00); 409 si el año no está planificado ("Solo se puede modificar la estructura horaria de un año planificado."), el código ya existe en el año o los bloques por semana quedan por debajo de las horas planificadas de un grado; 404 si el año no existe |
| POST | `/configuracion/anios-escolares/{anioId}/estructuras-horarias/vista-previa` | Personal adm. | Mismos campos que crear; no guarda. Responde 200 `{valida, errores, jornada}`: `errores` trae todos los problemas del formulario y `jornada` viene aunque pase de las 18:00 (null si faltan datos). No mira el estado del año ni los planes. 404 si el año no existe |
| PUT | `/configuracion/estructuras-horarias/{id}` | Personal adm. | Edita los mismos campos que crear, sin `nivel` (no se cambia), y regenera los recesos; devuelve `{estructura, advertencias}`. `esPrincipal` es opcional: ausente conserva la marca, true marca esta como principal (la anterior baja) y false sobre la principal da 409. 400 si falla una regla de forma; 404 si no existe; 409 si el año no está planificado, el código ya existe, se desmarca la principal o los bloques por semana quedan por debajo de las horas planificadas |
| DELETE | `/configuracion/estructuras-horarias/{id}` | Personal adm. | Elimina la estructura (204). 404 si no existe; 409 si el año no está planificado, si es la principal y quedan otras del nivel ("Marca otra como principal primero.") o si es la única del nivel y hay cursos planificados |
| GET | `/configuracion/periodos-matricula?anio=&estado=&page=&size=` | Personal adm. | Listado de la pantalla 13, del periodo más reciente al más antiguo: `{id, nombre, anio, inicio, cierre, matriculas, estado, vigente}`. `estado` (PROGRAMADO, ABIERTO o CERRADO) se calcula con el reloj; `anio` y `estado` filtran. Paginado (HU-23) |
| GET | `/configuracion/periodos-matricula/vigente` | Personal adm. | Tarjeta "Vigente": el periodo ABIERTO ahora o, si no hay, el próximo PROGRAMADO, con las reglas, las vacantes totales y las matrículas registradas. 404 si no hay ninguno |
| GET | `/configuracion/periodos-matricula/anios-disponibles` | Personal adm. | Selector "Año escolar" del formulario: años PLANIFICADO o ACTIVO sin periodo de matrícula |
| GET | `/configuracion/periodos-matricula/nuevo?anioEscolarId=` | Personal adm. | Formulario "Nuevo periodo de matrícula" vacío: secciones con su capacidad, monto del tarifario y resumen. 400 sin `anioEscolarId`; 404 si el año no existe; 409 si el año ya tiene periodo o no está PLANIFICADO/ACTIVO |
| GET | `/configuracion/periodos-matricula/{id}` | Personal adm. | Formulario de un periodo guardado, con `editable` y `eliminable`. Abierto o cerrado es de solo lectura, con el resumen del modal "Configuración · Matrícula". 404 si no existe |
| POST | `/configuracion/periodos-matricula` | Personal adm. | "Guardar configuración" `{anioEscolarId, fechaInicio, horaInicio, fechaCierre, horaCierre, vacantes: [{seccionId, capacidad}], documentos: [{nombre, descripcion}]}` (201): deja el periodo HABILITADO, guarda las vacantes en las secciones y cierra los HABILITADO vencidos. 400 con el primer dato inválido (nada se guarda); 409 si el tarifario del año no está CONFIRMADO, hay otro periodo sin cerrar o el año ya tiene periodo; 404 si el año no existe |
| PUT | `/configuracion/periodos-matricula/{id}` | Personal adm. | Edita un periodo PROGRAMADO (mismo cuerpo; `anioEscolarId` se ignora; `documentos[].id` conserva el documento y su plantilla). 400 con un dato inválido o un documento ajeno; 409 si ya abrió o cerró, si el tarifario no está CONFIRMADO o si se quita un documento con entregas |
| DELETE | `/configuracion/periodos-matricula/{id}` | Personal adm. | Elimina un periodo PROGRAMADO (204). 409 si ya abrió o cerró, o si el año tiene alguna matrícula o documentos entregados |
| POST | `/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla` | Personal adm. | Sube o reemplaza la plantilla (multipart, parte `archivo`): XLSX, XLS o CSV de hasta 10 MB; devuelve el documento. 400 si no es válida; 404 si el documento no es del periodo; 409 si el periodo no está PROGRAMADO |
| GET | `/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla` | Personal adm. | Descarga la plantilla con su nombre y tipo (`Content-Disposition: attachment`), en cualquier estado del periodo. 404 si no tiene |
| DELETE | `/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla` | Personal adm. | Quita la plantilla y devuelve el documento. 404 si no tiene; 409 si el periodo no está PROGRAMADO |
| GET | `/matricula/administrativo/periodos/{periodoId}/matriculas` | Personal adm. | Matrículas reales del año asociado al periodo, con sus ids, estudiante, grado, estado y datos de contacto. La sección y el tutor solo se devuelven cuando la matrícula ya está MATRICULADO o RETIRADO; permite navegar hasta la tarjeta de HU-36 sin depender de ids de prueba |
| GET | `/matricula/administrativo/matriculas/{matriculaId}` | Personal adm. | Detalle administrativo de una matrícula real para A5/B6/B7. Usa el mismo `matriculaId` que los endpoints de pago de HU-36. 404 si no existe |
| GET | `/matricula/apoderado/estudiantes` | Apoderado | Hijos del apoderado con su grado y el estado de su matrícula en el periodo vigente (selector de Figma, HU-27, RF-58) |
| GET | `/matricula/apoderado/estudiantes/{id}/estado` | Apoderado | Periodo vigente (`abierto` según fechas, RF-59), matrícula, paso del stepper, deuda vencida del apoderado y sus hijos con el estudiante al que corresponde (HU-30, RF-64) e impedimentos `PERIODO`, `SIN_SECCION`, `DOCUMENTOS` o `DEUDA` (RF-60). 404 si el estudiante no está a su cargo |

**Estructuras horarias (HU-44):** `advertencias` (en crear y editar) avisa de que cambiar los bloques por semana de la principal obliga a revisar el plan de evaluación, o de que otra estructura pasa a ser la principal; no rechaza nada. Detalle en [`docs/decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md`](../docs/decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md).

## Formato

Spotless con el formateador de Eclipse (tabulaciones, 120 columnas; ajustes en [`formato-java.prefs`](formato-java.prefs)). `.\mvnw.cmd spotless:apply` formatea; `verify` y el CI fallan si algo no está formateado. El hook de commit lo aplica solo a los `.java` que commiteas.

## Pruebas

```bash
.\mvnw.cmd test      # unitarias (sin BD)
.\mvnw.cmd verify    # unitarias + integración: necesita la base vacía galiledu_test (o TEST_DB_URL)
```

Las de integración heredan de `src/test/java/com/galiledu/PruebaIntegracion.java` y también corren en GitHub Actions.
