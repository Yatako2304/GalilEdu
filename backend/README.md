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
├── migration/      V1…V7 (ver la tabla de abajo)
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

Qué tabla sirve para qué y qué reglas valida la BD: en la ficha de cada módulo ([`docs/modulos/`](../docs/modulos/README.md)). Una migración ya subida **no se edita**: los cambios van en una nueva (V8…).

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
| GET / PUT | `/usuarios/{id}` | Administrador | Detalle / edición (persona + roles) |
| POST | `/usuarios` | Administrador | Alta; devuelve usuario generado (la contraseña va por correo) |
| POST | `/usuarios/{id}/desactivar` · `/reactivar` | Administrador | HU-12 |
| DELETE | `/usuarios/{id}` | Administrador | Borrado lógico |
| GET | `/usuarios/carga-masiva/plantilla` | Administrador | Plantilla `.xlsx` de la carga masiva (HU-8) |
| POST | `/usuarios/carga-masiva/validacion` | Administrador | Revisa el archivo (multipart `archivo`: .xlsx/.xls/.csv ≤ 10 MB) y devuelve errores por fila, sin crear nada |
| POST | `/usuarios/carga-masiva` | Administrador | Crea los usuarios de las filas válidas y devuelve el resumen |
| GET | `/estudiantes?q=&estado=&page=` | Admin, Personal adm. | Lista de estudiantes. `estado`: INSCRITO, MATRICULADO o RETIRADO (se calcula desde la matrícula) |
| GET | `/estudiantes/{id}` | Admin, Personal adm. | Ficha (datos, ficha médica, familia, contactos, hermanos) |
| GET | `/estudiantes/personas?documento=` | Admin, Personal adm. | Busca padre/madre/apoderado ya registrado (404 = persona nueva) |
| POST | `/estudiantes/inscripciones` | Admin, Personal adm. | Registra la inscripción completa (5 pasos). El grado va como código del catálogo: `gradoCodigo` = `PRI-1`…`PRI-6`, `SEC-1`…`SEC-5` |
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
| GET | `/planes-evaluacion?anioEscolarId=&grado=` | Personal adm. | Plan de evaluación del grado (HU-56): tarjetas de CI-17, secciones con tutor y cursos con horas, competencias, escala y docentes asignados |
| GET | `/planes-evaluacion/anios` · `/planes-evaluacion/docentes` | Personal adm. | Años escolares (del más reciente) · usuarios activos con rol Docente |
| GET | `/planes-evaluacion/cursos-disponibles?anioEscolarId=&grado=` | Personal adm. | Cursos activos que se dictan en el grado y aún no están en su plan, con las competencias activas del área |
| GET | `/planes-evaluacion/origenes?anioEscolarId=&grado=` | Personal adm. | Otros planes con cursos, para copiarlos |
| POST | `/planes-evaluacion` | Personal adm. | Agrega un curso `{anioEscolarId, gradoCodigo, cursoId, horasSemanales, competencias: [{competenciaId, escala, reglaNota, consolidacion}]}`. 409 si ya está, si no se dicta en el grado o si las horas superan los bloques |
| POST | `/planes-evaluacion/copias` | Personal adm. | Copia `{anioEscolarId, gradoCodigo, origenAnioEscolarId, origenGradoCodigo}` a un grado vacío, sin docentes |
| GET · PUT · DELETE | `/planes-evaluacion/{id}` | Personal adm. | Detalle del curso en el plan · `{horasSemanales, competencias}` · quitarlo del plan. 409 si el año está cerrado |
| GET · PUT | `/planes-evaluacion/{id}/docentes` | Personal adm. | Docente de cada sección `{asignaciones: [{seccionId, docenteId}]}` (`docenteId` null = sin docente) |
| GET | `/pagos/estado-cuenta?anio=` | Personal adm. | P2 · Estado de cuenta (HU-37): compromisos del año (por defecto el ACTIVO) con sección, antigüedad y deuda vencida de cada alumno, y las tarjetas comprometido, cobrado (%), deuda vencida y cuotas vencidas (solo compromisos VIGENTES) |
| GET | `/pagos/estudiantes/{estudianteId}/compromisos` | Personal adm. | Compromisos de pago del estudiante, del año más reciente al más antiguo (selector de P8, HU-37) |
| GET | `/pagos/compromisos/{id}` | Personal adm. | Resumen (total anual, capital pagado, descuento, pensión, saldo) y cronograma de cuotas; `vencida` se calcula con los días de gracia (HU-37, RF-84, RF-91) |
| POST | `/pagos/matriculas/{matriculaId}/cronograma` | Personal adm. | Genera las pensiones del tarifario para una matrícula MATRICULADO: solo las que vencen después de la matrícula y con el descuento activo (RF-81, RF-82). 409 si ya existen (RF-83), si no está matriculado o si el tarifario no está confirmado |
| POST | `/pagos/compromisos/{id}/retiro` | Personal adm. | Anula las pensiones PENDIENTES que vencen después de `{fechaRetiro}`; las vencidas y parciales se mantienen (RF-85). Devuelve `{cuotasAnuladas}` |
| POST | `/pagos/matriculas/{matriculaId}/cuota-matricula` | Personal adm. | Genera la cuota de matrícula PENDIENTE con el monto y el vencimiento del tarifario del año (y el descuento activo si aplica a MATRICULA); crea el compromiso si no existe (HU-36, RF-78). 409 si ya existe (RF-79), si la matrícula ya está MATRICULADO/RETIRADO/ANULADA o si no hay tarifario |
| GET | `/pagos/matriculas/{matriculaId}/cuota-matricula` | Personal adm. | Concepto, monto, vencimiento, estado, fecha de pago y responsable de la cuota de matrícula (HU-36, RF-80; tarjeta "Pago de matrícula" de B6/B7). 404 si aún no se generó |
| GET | `/configuracion/anios-escolares` | Personal adm. | Años escolares, del más reciente al más antiguo, con régimen, estado y cantidad de periodos (arreglo simple, HU-74) |
| GET | `/configuracion/anios-escolares/{id}` | Personal adm. | El año con sus periodos ordenados por número (HU-74) |
| POST | `/configuracion/anios-escolares` | Personal adm. | Crea un año PLANIFICADO `{anio, fechaInicio, fechaFin, regimen}` y genera sus 4 bimestres o 3 trimestres con fechas sugeridas. 400 si `fechaFin` no es posterior a `fechaInicio`, si el año es muy corto o está fuera de 2000–2100; 409 si el año ya existe |
| GET | `/matricula/apoderado/estudiantes` | Apoderado | Hijos del apoderado con su grado y el estado de su matrícula en el periodo vigente (selector de Figma, HU-27, RF-58) |
| GET | `/matricula/apoderado/estudiantes/{id}/estado` | Apoderado | Periodo vigente (`abierto` según fechas, RF-59), matrícula, paso del stepper, deuda vencida del apoderado y sus hijos con el estudiante al que corresponde (HU-30, RF-64) e impedimentos `PERIODO`, `SIN_SECCION`, `DOCUMENTOS` o `DEUDA` (RF-60). 404 si el estudiante no está a su cargo |

## Formato

Spotless con el formateador de Eclipse (tabulaciones, 120 columnas; ajustes en [`formato-java.prefs`](formato-java.prefs)). `.\mvnw.cmd spotless:apply` formatea; `verify` y el CI fallan si algo no está formateado. El hook de commit lo aplica solo a los `.java` que commiteas.

## Pruebas

```bash
.\mvnw.cmd test      # unitarias (sin BD)
.\mvnw.cmd verify    # unitarias + integración: necesita la base vacía galiledu_test (o TEST_DB_URL)
```

Las de integración heredan de `src/test/java/com/galiledu/PruebaIntegracion.java` y también corren en GitHub Actions.
