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
├── configuracion/  año escolar y periodos, secciones y tutores, estructuras horarias, periodo de matrícula y tarifario (HU-74, HU-75, HU-44, HU-23, HU-34)
├── academico/      catálogo académico (plan curricular, escala de calificación y plan de evaluación: HU-52 a HU-56) y calificaciones del docente (HU-57, HU-59, HU-62)
├── matricula/      matrícula del apoderado: consulta y estado (HU-27, HU-30)
├── pagos/          compromisos, cuotas, descuentos y deuda del apoderado (HU-36, HU-37, HU-35, HU-38 a HU-40)
└── horarios/       horario semanal de una sección para el Coordinador (HU-45), detección de cruces (HU-46), horario del docente y del tutor (HU-49) y del apoderado (HU-51)

Asistencia todavía no tiene paquete (iteración 3).

Cada módulo:  dominio/ (entidades JPA, reglas)  aplicacion/ (servicios + DTOs)
              infraestructura/ (repositorios)   web/ (controladores REST)

src/main/resources/db/
├── migration/      V1…V8 (ver la tabla de abajo)
└── datos-prueba/   R__datos_prueba.sql (usuarios por rol, año 2026/2027, familia Paredes con matrícula y pagos…)
```

Reglas: un esquema de BD por módulo; los controladores no tocan repositorios directamente; los mensajes de error van en español porque el frontend los muestra tal cual (`{ "message": "..." }`).

## Base de datos

| Migración | Esquemas | Qué cambia |
| --- | --- | --- |
| `V1__personas_y_seguridad.sql` | `personas`, `seguridad` | Personas, usuarios, roles |
| `V2__estudiantes_inscripcion.sql` | `estudiantes` | Estudiante, ficha médica, familia, contactos, autorizados |
| `V3__estudiantes_documento_apoderado.sql` | `estudiantes` | Documento que acredita al apoderado |
| `V4__configuracion_anio_escolar.sql` | `configuracion`, `matricula`, `pagos` | Año escolar, periodos, grados, secciones, estructura horaria, periodo de matrícula, tarifario |
| `V5__matricula_y_pagos.sql` | `matricula`, `pagos` | Matrícula, documentos, compromiso de pago, cuotas, descuentos, órdenes de pago, pagos y constancias |
| `V6__catalogo_academico.sql` | `academico`, `configuracion` | Áreas, competencias, cursos, plan de evaluación, tabla de conversión, ambientes |
| `V7__ajustes_integracion.sql` | varios | Grado del estudiante del catálogo, estado ACTIVO/RETIRADO, estructura horaria por nivel, descuento por estudiante, formato del código de matrícula |
| `V8__configuracion_periodo_sin_estado.sql` | `configuracion` | Elimina `periodo_academico.estado`: el estado del periodo se calcula con las fechas |
| `V9__parametros_evaluacion_por_anio.sql` | `configuracion`, `academico` | Parámetros globales de evaluación del año; conserva las columnas anteriores del plan como legado |
| `V10__horarios.sql` | `horarios` | Horario semanal de una sección (`horario_seccion`) y sus bloques asignados (`horario_bloque`) |
| `V11__calificaciones_docente.sql` | `academico` | Ítems, competencias evaluadas, calificaciones y evidencias del docente; claves compuestas con curso, periodo, sección y matrícula |

Qué tabla sirve para qué y qué reglas valida la BD: en la ficha de cada módulo ([`docs/modulos/`](../docs/modulos/README.md)). Una migración ya subida **no se edita**: los cambios van en una nueva (V11…).

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
| POST | `/planes-evaluacion` | Personal adm. · Coordinador | Agrega un curso `{anioEscolarId, gradoCodigo, cursoId, horasSemanales, competencias: [{competenciaId}]}`. La escala, regla de nota y consolidación se toman del año escolar. 409 si ya está, si no se dicta en el grado o si las horas superan los bloques |
| POST | `/planes-evaluacion/copias` | Personal adm. · Coordinador | Copia `{anioEscolarId, gradoCodigo, origenAnioEscolarId, origenGradoCodigo}` a un grado vacío, sin docentes |
| GET · PUT · DELETE | `/planes-evaluacion/{id}` | Personal adm. · Coordinador | Detalle del curso en el plan · `{horasSemanales, competencias}` · quitarlo del plan. 409 si el año está cerrado |
| GET · PUT | `/planes-evaluacion/{id}/docentes` | Personal adm. · Coordinador | Docente de cada sección `{asignaciones: [{seccionId, docenteId}]}` (`docenteId` null = sin docente) |
| GET | `/horarios/anios-escolares` | Coordinador | Selector de año de la pantalla de asignación de horarios: `[{id, anio, estado}]`, del más reciente al más antiguo. Es la lectura de Horarios porque `/configuracion/anios-escolares` es solo del Personal administrativo |
| GET | `/horarios/anios-escolares/{anioId}/secciones` | Coordinador | Selectores de grado y sección: los 11 grados del catálogo `[{codigo, nombre, nivel, numero, cantidadSeccionesActivas, secciones: [{id, nombre, estado}]}]` con las secciones que tienen en el año (los de primaria, vacíos, si no las tienen). 404 si el año no existe |
| GET | `/horarios/secciones/{seccionId}` | Coordinador | Matriz del horario de la sección (HU-45): `{seccion, version, editable, estructura, jornada, celdas}`. Con horario guardado usa su estructura (aunque ya no sea la principal); sin él, la principal del nivel, `version = null` y sin celdas (no escribe nada). `editable` es false con el año CERRADO (sigue siendo 200). Cada celda trae curso, docente (o null), ambiente y `cruces`. 404 si la sección no existe; 409 si no hay horario ni estructura principal del nivel |
| GET | `/horarios/secciones/{seccionId}/opciones` | Coordinador | Modal «Asignar bloque horario»: `{ambientePorDefectoId, cursos, ambientes}`. Cursos del plan del grado y año con `horasSemanales`, `horasAsignadas` (bloques ya guardados) y docente (o null); ambientes activos. 404 si la sección no existe |
| PUT | `/horarios/secciones/{seccionId}` | Coordinador | Guarda la matriz COMPLETA `{version, celdas: [{dia, numeroBloque, planCursoId, ambienteId}]}` y responde como el GET. `version` es la que devolvió el GET (null si no había horario). Una transacción con el bloqueo del año; aplica por diferencia (los bloques sin cambios conservan su id); la cabecera nueva nace con `version = 0` y cada guardado con cambios la sube exactamente 1; uno idéntico no la sube. 400 por celda repetida, día no lectivo, bloque fuera de la jornada, curso de otro año o grado o ambiente inexistente o inactivo; 404 si la sección no existe; 409 si el año está cerrado, la versión cambió («Otra persona modificó el horario…»), el curso no tiene docente, supera sus horas semanales, hay cruces o falta la estructura principal |
| POST | `/horarios/secciones/{seccionId}/cruces` | Coordinador | Avisa de los cruces de un borrador antes de guardar (HU-46). Cuerpo `{celdas: [{dia, numeroBloque, planCursoId, ambienteId}]}` (obligatoria, máximo 72); responde `[{tipo, dia, numeroBloque, mensaje, seccion, curso}]`, vacía si no hay. Cruce = otra sección del año, mismo día, horas reales superpuestas (los contiguos no cuentan) y mismo docente o ambiente. Solo lee: no bloquea el año. Ignora las celdas que no puede convertir (curso sin docente, bloque fuera de la jornada). 400 por celda inválida; 404 si la sección no existe; 409 si no hay horario ni estructura principal del nivel |
| GET | `/horarios/apoderado/estudiantes` | Apoderado | Hijos matriculados (HU-51): `[{estudianteId, nombreCompleto, matriculas: [{anio, grado, nivel, seccion}]}]`. Solo matrículas MATRICULADO de los estudiantes vinculados al usuario (padre, madre o apoderado no fallecido), del año más reciente al más antiguo |
| GET | `/horarios/apoderado/estudiantes/{id}/horario?anio=` | Apoderado | Horario de la sección del hijo ese año (sin `anio`, el más reciente): `{estudianteId, nombreCompleto, anio, grado, nivel, seccion, tutor {nombre, celular, correo}, disponible, jornada, clases: [{dia, numeroBloque, curso, docente, aula}]}`. `disponible = false` (sin jornada ni clases) si el Coordinador aún no guardó el horario. 404 si no es su hijo o no está matriculado ese año |
| GET | `/horarios/docente/anios` | Docente, Tutor | Años en que el usuario que inició sesión tiene al menos un bloque como titular de `docente_seccion` (HU-49): `[{id, anio, estado}]`, del más reciente al más antiguo; `[]` si no dicta |
| GET | `/horarios/docente/horario?anioId=` | Docente, Tutor | Sus clases de dictado, un horario por estructura horaria (primaria antes que secundaria) (HU-49): `{anio {id, anio, estado} o null, horarios: [{nivel, titulo, jornada {dias, bloques, recesos}, clases: [{dia, numeroBloque, curso, seccion, ambiente}]}]}`. Sin `anioId`, el año ACTIVO si tiene clases y, si no, el más reciente con clases; con un año sin clases, `horarios` vacío. Si una celda tiene dos clases, se devuelven las dos. 400 si `anioId` no es un UUID; 404 «El año escolar no existe.» |
| GET | `/calificaciones/docente/cursos` | Docente | Cursos y secciones asignados, con los parámetros del año y sus periodos (HU-57, HU-59, HU-62) |
| GET | `/calificaciones/docente/cursos/{planCursoId}/secciones/{seccionId}/competencias` | Docente | Competencias del plan del curso asignado |
| GET · POST | `/calificaciones/docente/cursos/{planCursoId}/secciones/{seccionId}/periodos/{periodoId}/items` | Docente | Lista o registra ítems con criterio, tipo, competencias y pesos |
| PUT · DELETE | `/calificaciones/docente/items/{itemId}` | Docente | Edita o elimina un ítem; no elimina si ya tiene notas |
| GET · PUT | `/calificaciones/docente/items/{itemId}/calificaciones` | Docente | Lista estudiantes y guarda sus notas según la escala del año |
| POST · GET · DELETE | `/calificaciones/docente/items/{itemId}/calificaciones/{matriculaId}/evidencia` | Docente | Sube, descarga o elimina una evidencia PDF, DOC, DOCX, JPG o PNG de hasta 10 MB |
| GET | `/calificaciones/docente/reportes` · `/reportes/exportar` | Docente | Consolidado JSON o CSV: `tipo=COMPETENCIA`, `objetivoId`, `itemIds` separados por coma e `incluirPromedio`; conserva las variantes ITEM y PERIODO |
| GET | `/pagos/estado-cuenta?anio=` | Personal adm. | P2 · Estado de cuenta (HU-37): compromisos del año (por defecto el ACTIVO) con sección, antigüedad y deuda vencida de cada alumno, y las tarjetas comprometido, cobrado (%), deuda vencida y cuotas vencidas (solo compromisos VIGENTES) |
| GET | `/pagos/estudiantes/{estudianteId}/compromisos` | Personal adm. | Compromisos de pago del estudiante, del año más reciente al más antiguo (selector de P8, HU-37) |
| GET | `/pagos/compromisos/{id}` | Personal adm. | Resumen (total anual, capital pagado, descuento, pensión, saldo) y cronograma de cuotas; `vencida` se calcula con los días de gracia y cada cuota trae `diasAtraso` y `recargo` (HU-37, RF-84, RF-91, RF-92) |
| GET | `/pagos/compromisos/{id}/pagos` | Personal adm. | Pestaña "Pagos" de P8: pagos que cubrieron cuotas del compromiso, del más reciente al más antiguo, con constancia, monto, capital, recargo y cuotas |
| GET | `/pagos/compromisos/{id}/morosidad` | Personal adm. | P11 · Morosidad (HU-39): pensiones vencidas de la más antigua a la más reciente con días de atraso, capital pendiente, recargo (la tasa del tarifario por cada 30 días completos sobre el capital, sin pasar del tope) y total; `fecha` del cálculo y totales con el recargo separado del capital y la política del tarifario (RF-91 a RF-93). Se calcula al consultar |
| GET | `/pagos/descuentos` | Personal adm. | P21 · Catálogo de descuentos (HU-35, RF-76), activos e inactivos, por denominación. `porcentaje` en PORCENTAJE_UNICO; `tramos` `[{hermanosDesde, hermanosHasta (null = o más), porcentaje}]` en POR_HERMANOS |
| POST · PUT | `/pagos/descuentos` · `/pagos/descuentos/{id}` | Personal adm. | Registra o edita `{nombre, descripcion, tipo, aplicacion, porcentaje, tramos, conceptos, condicion}`. 400 si el porcentaje no está entre 1 y el límite, sin conceptos, con menos de 2 tramos o tramos superpuestos (RF-74, RF-75, RF-77); 409 si la denominación ya existe. Editar no cambia lo ya aplicado (RF-76) |
| POST | `/pagos/descuentos/{id}/desactivacion` · `/activacion` | Personal adm. | Desactiva o reactiva el descuento; el inactivo no se puede aplicar, pero no afecta a los compromisos que ya lo tienen (RF-76) |
| GET · PUT | `/pagos/descuentos/configuracion` | Personal adm. | Límite de descuento de la institución `{porcentajeMaximo}` (1–100, RF-77) |
| GET | `/pagos/compromisos/{id}/descuentos-aplicables` | Personal adm. | "Añadir descuento" (HU-38, RF-86): `hermanosMatriculados` (contando al estudiante), el descuento `actual` y los descuentos ACTIVOS con el porcentaje que le tocaría; un descuento por hermanos sin tramo llega con `aplicable: false` y el motivo |
| POST | `/pagos/compromisos/{id}/descuentos` | Personal adm. | Aplica `{descuentoId, motivo, vigenciaDesde?, vigenciaHasta?, reemplazar}` al estudiante: rebaja las cuotas PENDIENTES sin pagos de los conceptos cubiertos que vencen en la vigencia (RF-88) y guarda nombre, %, motivo, usuario y fecha (RF-89). 409 si ya tiene uno y `reemplazar` es false (RF-87), si está inactivo o si ningún tramo le corresponde. Devuelve `{descuento, cuotasRecalculadas}` |
| GET | `/pagos/compromisos/{id}/descuento` | Personal adm. | Pestaña "Descuento" de P10: descuento `activo` del estudiante, `cuotasConDescuento` en el compromiso e `historial` (ACTIVO, REEMPLAZADO, REVOCADO) |
| POST | `/pagos/descuentos-aplicados/{id}/revocacion` | Personal adm. | Revoca `{motivo}` el descuento y restituye el monto original de las cuotas pendientes; las pagadas no cambian (RF-90). 409 si ya no está vigente |
| POST | `/pagos/matriculas/{matriculaId}/cronograma` | Personal adm. | Genera las pensiones del tarifario para una matrícula MATRICULADO: solo las que vencen después de la matrícula y con el descuento activo (RF-81, RF-82). 409 si ya existen (RF-83), si no está matriculado o si el tarifario no está confirmado |
| POST | `/pagos/compromisos/{id}/retiro` | Personal adm. | Anula las pensiones PENDIENTES que vencen después de `{fechaRetiro}`; las vencidas y parciales se mantienen (RF-85). Devuelve `{cuotasAnuladas}` |
| POST | `/pagos/matriculas/{matriculaId}/cuota-matricula` | Personal adm. | Genera la cuota de matrícula PENDIENTE con el monto y el vencimiento del tarifario vigente del año (y el descuento activo si aplica a MATRICULA); crea el compromiso si no existe (HU-36, RF-78). 409 si ya existe (RF-79), si la matrícula ya está MATRICULADO/RETIRADO/ANULADA o si el tarifario del año no existe o no está CONFIRMADO (HU-34) |
| GET | `/pagos/pago-en-linea/cuotas` | Apoderado | Cuotas con saldo de cada hijo, con capital y recargo por mora por separado; `seleccionable` y `requiere` (pensiones anteriores que se pagan junto) para pagar en orden (HU-41) |
| POST | `/pagos/pago-en-linea/ordenes` | Apoderado | `{cuotaIds}` → crea la orden `OP-AAAA-NNNNNN` con capital y recargo, pone las cuotas EN_PROCESO y devuelve la página de pago de la pasarela (`urlPago`). 400 si salta una pensión anterior, 409 si una cuota ya está en proceso o pagada, 404 si no es de un hijo suyo (HU-41) |
| GET | `/pagos/pago-en-linea/ordenes/{codigo}` | Apoderado | Resultado de la orden (EN_PROCESO, APROBADA, RECHAZADA o EXPIRADA) y sus constancias; si sigue en proceso, consulta a la pasarela (HU-41) |
| POST | `/pagos/pago-en-linea/ordenes/{codigo}/cancelar` | Apoderado | El apoderado volvió sin pagar: cierra la sesión en la pasarela y libera las cuotas (HU-41) |
| POST | `/pagos/pago-en-linea/simulador/{codigo}` | Apoderado | Solo con la pasarela simulada: `{aprobar: true|false}` (HU-41) |
| POST | `/pagos/pasarela/stripe/notificaciones` | Stripe (sin sesión) | Webhook de Stripe, validado con la firma `Stripe-Signature`; vuelve a consultar la sesión antes de registrar el pago (HU-41) |
| GET | `/pagos/matriculas/{matriculaId}/cuota-matricula` | Personal adm. | Concepto, monto, vencimiento, estado, fecha de pago y responsable de la cuota de matrícula (HU-36, RF-80; tarjeta "Pago de matrícula" de B6/B7). 404 si aún no se generó |
| GET | `/configuracion/anios-escolares` | Personal adm. | Años escolares, del más reciente al más antiguo, con régimen, estado, cantidad de periodos, secciones activas y estructuras horarias (arreglo simple, HU-74) |
| GET | `/configuracion/anios-escolares/actual` | Personal adm. | Card del año en curso (CI-01): periodo en curso, periodo ya terminado con notas abiertas, próximo periodo, secciones activas, alumnos matriculados, estructuras y avance (%). Los periodos son `null` si no aplican. 404 si no hay año activo |
| GET | `/configuracion/anios-escolares/{id}` | Personal adm. | El año con sus periodos ordenados por número; cada periodo trae `duracionSemanas`, su `estado` (se calcula con las fechas) y `registroNotasAbierto` (HU-74) |
| POST | `/configuracion/anios-escolares` | Personal adm. | Crea un año PLANIFICADO `{anio, fechaInicio, fechaFin, regimen, escalaEvaluacion, reglaNota, consolidacionPeriodos}` y genera sus 4 bimestres o 3 trimestres con fechas sugeridas. 400 si las fechas, el año o la combinación de evaluación no son válidos; 409 si el año ya existe |
| PUT | `/configuracion/anios-escolares/{id}/parametros-evaluacion` | Personal adm. | Cambia `{escalaEvaluacion, reglaNota, consolidacionPeriodos}` antes de `fechaInicio`. 400 si la regla no corresponde a la escala; 404 si no existe; 409 desde el día de inicio |
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
| POST | `/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla` | Personal adm. | Sube o reemplaza la plantilla (multipart, parte `archivo`): PDF, DOCX, JPG o PNG de hasta 10 MB; devuelve el documento. 400 si no es válida; 404 si el documento no es del periodo; 409 si el periodo no está PROGRAMADO |
| GET | `/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla` | Personal adm. | Descarga la plantilla con su nombre y tipo (`Content-Disposition: attachment`), en cualquier estado del periodo. 404 si no tiene |
| DELETE | `/configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla` | Personal adm. | Quita la plantilla y devuelve el documento. 404 si no tiene; 409 si el periodo no está PROGRAMADO |
| GET | `/matricula/apoderado/estudiantes` | Apoderado | Hijos del apoderado con su grado y el estado de su matrícula en el periodo vigente (selector de Figma, HU-27, RF-58) |
| GET | `/matricula/apoderado/estudiantes/{id}/estado` | Apoderado | Periodo vigente (`estado` PROGRAMADO/ABIERTO/CERRADO y `abierto`, calculados por Configuración, RF-59), matrícula, paso del stepper, deuda vencida **del estudiante** (la de un hermano no lo bloquea, HU-30 corregida) e impedimentos `PERIODO`, `INSCRIPCION` (sin matrícula y no inscrito para el año del periodo), `DOCUMENTOS` o `DEUDA` (RF-60). Sin matrícula y sin impedimentos: `puedeContinuar = true` y `pasoActual = 1` (el apoderado la inicia). `pagoMatricula` (HU-31): cuota de matrícula con `puedePagar` y `motivo`, o null si aún no existe. 404 si el estudiante no está a su cargo |
| GET | `/matricula/apoderado/estudiantes/{id}/documentos` | Apoderado | Documentos de la matrícula del hijo: nombre, si tienen plantilla y lo ya subido (`archivo`); `puedeSubir` es true solo con la matrícula esperando documentos (SECCION_RESERVADA) y el periodo abierto (HU-28, HU-29). Sin matrícula: `estado = null`, documentos sin archivo y `puedeSubir` true si puede iniciarla |
| GET | `/matricula/apoderado/estudiantes/{id}/documentos/{documentoId}/plantilla` | Apoderado | Descarga la plantilla (PDF, DOCX, JPG o PNG) para firmarla. 404 si el documento no tiene |
| GET | `/matricula/apoderado/estudiantes/{id}/documentos/{documentoId}/archivo` | Apoderado | Ve el archivo ya subido. 404 si aún no se subió |
| PUT | `/matricula/apoderado/estudiantes/{id}/documentos/{documentoId}` | Apoderado | Sube o reemplaza el documento firmado (multipart, campo `archivo`: PDF, JPG o PNG ≤ 10 MB); queda EN_REVISION y devuelve los documentos. 409 si no está en SECCION_RESERVADA, el periodo no está abierto o el documento ya está APROBADO (los aprobados no se reemplazan) |
| POST | `/matricula/apoderado/estudiantes/{id}/matricula` | Apoderado | "Enviar a validación" cuando el hijo aún no tiene matrícula (multipart: un campo por documento, con el id del documento como nombre). Con todos los documentos y sin impedimentos crea la matrícula con la sección que asigna el sistema (la primera del grado con cupo) y la deja EN_VALIDACION, en la bandeja del Personal administrativo. 400 si falta un documento (no se guarda nada), 409 si ya tiene matrícula o hay un impedimento |
| POST | `/matricula/apoderado/estudiantes/{id}/enviar-validacion` | Apoderado | "Enviar a validación": con todos los documentos subidos y sin impedimentos (deuda, periodo) pasa SECCION_RESERVADA a EN_VALIDACION y devuelve el estado. 409 con el primer impedimento si no se puede |
| GET | `/matricula/periodos/{periodoId}` | Personal administrativo | Aviso del periodo (`estado` PROGRAMADO/ABIERTO/CERRADO, `cierre`) e indicadores de A3: estudiantes, matriculados, en proceso y que requieren acción (HU-24). 404 si no existe |
| GET | `/matricula/periodos/{periodoId}/matriculas?texto=&grado=&estado=&orden=&direccion=&page=&size=` | Personal administrativo | Tabla de A3: **solo quienes ya tienen matrícula** en el año del periodo (sin anuladas ni retiradas). `texto` busca por nombre, documento o código; `orden`: estudiante, grado, seccion o estado (lista cerrada); paginada |
| GET | `/matricula/periodos/{periodoId}/estudiantes?texto=` | Personal administrativo | Buscador de «Nueva matrícula»: hasta 8 estudiantes activos inscritos para el año del periodo (`anioAcademico`) y **sin matrícula** (HU-24) |
| GET | `/matricula/periodos/{periodoId}/estudiantes/{id}/preparacion` | Personal administrativo | Datos que muestra el modal: estudiante, tipo (NUEVO/CONTINUADOR), sección que se asignaría (primera del grado con cupo, orden alfabético; `null` si no hay vacantes), apoderado (el responsable de matrícula), contacto de emergencia, resumen médico y los documentos que pide el periodo. 400 si está inscrito para otro año; 409 si ya tiene matrícula |
| GET | `/matricula/periodos/{periodoId}/estudiantes/{id}/secciones` | Personal administrativo | Secciones activas del grado del alumno (etiqueta, tutor, capacidad, ocupadas) para «Cambiar sección» de Nueva matrícula; `seleccionadaId` es la propuesta |
| GET | `/matricula/periodos/{periodoId}/documentos/{documentoId}/plantilla` | Personal administrativo | Descarga la plantilla del documento (botón «Plantilla» en Nueva matrícula y en el detalle). 404 si no tiene |
| POST | `/matricula/periodos/{periodoId}/matriculas` (multipart) | Personal administrativo | «Guardar» de Nueva matrícula: campo `estudianteId`, `seccionId` opcional y un archivo por documento ya subido, con el id del documento requerido como nombre de campo (PDF/JPG/PNG, máx. 10 MB). Crea la matrícula en `SECCION_RESERVADA`; si la sección elegida está llena (o no se eligió) usa la primera del grado con cupo y responde `seccionReasignada: true`. 409 si el periodo no está ABIERTO, si ya tiene matrícula o si no hay vacantes |
| GET | `/matricula/matriculas/{id}` | Personal administrativo | Detalle A5/B6/B7: datos, sección, `pasoActual` (2 reservada/en validación, 3 pendiente de pago, 4 matriculado), documentos con su archivo y estado, y `puedeCambiarSeccion` / `puedeEditarDocumentos` |
| GET | `/matricula/matriculas/{id}/secciones` | Personal administrativo | Secciones del grado con su ocupación; la actual va en `seleccionadaId` |
| PUT | `/matricula/matriculas/{id}/seccion` | Personal administrativo | `{seccionId}`. Mueve la matrícula (mismo grado, con cupo, distinta de la actual) y deja la fila en `matricula.cambio_seccion` sin motivo. Con el periodo cerrado solo se permite a los `MATRICULADO`. 409 sin cupo o no permitido; 400 si es la misma o de otro grado |
| GET | `/matricula/matriculas/{id}/documentos/{documentoId}/archivo` | Personal administrativo | Archivo guardado, en línea (`Content-Disposition: inline`). 404 si aún no se subió |
| PUT | `/matricula/matriculas/{id}/documentos/{documentoId}` (multipart, campo `archivo`) | Personal administrativo | Sube o reemplaza el documento; queda `EN_REVISION` sin observación. 409 si la matrícula está retirada o anulada |
| PUT | `/matricula/matriculas/{id}/documentos/{documentoId}/observacion` (`{ observacion }`, obligatoria, ≤ 500) | Personal administrativo | Observa un documento EN_REVISION: queda `OBSERVADO` con el motivo y la matrícula vuelve de EN_VALIDACION a SECCION_RESERVADA para que el apoderado lo reemplace. 400 sin motivo; 409 si la matrícula no está en validación o el documento no está en revisión |
| POST | `/matricula/matriculas/{id}/validar` | Personal administrativo | "¿Aprobar documentación?": aprueba los documentos en revisión y pasa EN_VALIDACION a PENDIENTE_PAGO. 409 si no está en validación o hay documentos observados |
| GET | `/pagos/apoderado/estudiantes` | Apoderado | HU-40, RF-95: hijos a cargo (cualquier padre, madre o apoderado vinculado) con `codigo` (DNI), nombres, apellidos, grado y sección de su matrícula más reciente, `saldo` pendiente y `cuotasVencidas`, por apellido |
| GET | `/pagos/apoderado/estudiantes/{id}/compromisos` | Apoderado | Compromisos del hijo (selector "Compromiso 2026"). 404 si no es su hijo |
| GET | `/pagos/apoderado/compromisos/{id}` | Apoderado | RF-96: el mismo detalle de P8 (montos ya rebajados, sin el motivo del descuento) con `recargo` por cuota. 404 si no es de su hijo |
| GET | `/pagos/apoderado/compromisos/{id}/pagos` | Apoderado | RF-97: pagos con fecha, monto y constancia, del más reciente al más antiguo. 404 si no es de su hijo |

**Estructuras horarias (HU-44):** `advertencias` (en crear y editar) avisa de que cambiar los bloques por semana de la principal obliga a revisar el plan de evaluación, o de que otra estructura pasa a ser la principal; no rechaza nada. Detalle en [`docs/decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md`](../docs/decisiones/2026-10-22-valeriaacevedop-estructura-horaria.md).

## Formato

Spotless con el formateador de Eclipse (tabulaciones, 120 columnas; ajustes en [`formato-java.prefs`](formato-java.prefs)). `.\mvnw.cmd spotless:apply` formatea; `verify` y el CI fallan si algo no está formateado. El hook de commit lo aplica solo a los `.java` que commiteas.

## Pruebas

```bash
.\mvnw.cmd test      # unitarias (sin BD)
.\mvnw.cmd verify    # unitarias + integración: necesita la base vacía galiledu_test (o TEST_DB_URL)
```

Las de integración heredan de `src/test/java/com/galiledu/PruebaIntegracion.java` y también corren en GitHub Actions.
