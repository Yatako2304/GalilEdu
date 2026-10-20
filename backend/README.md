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
└── academico/      catálogo académico: plan curricular (HU-52, HU-53, HU-54) y escala de calificación (HU-55)

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

## Formato

Spotless con el formateador de Eclipse (tabulaciones, 120 columnas; ajustes en [`formato-java.prefs`](formato-java.prefs)). `.\mvnw.cmd spotless:apply` formatea; `verify` y el CI fallan si algo no está formateado. El hook de commit lo aplica solo a los `.java` que commiteas.

## Pruebas

```bash
.\mvnw.cmd test      # unitarias (sin BD)
.\mvnw.cmd verify    # unitarias + integración: necesita la base vacía galiledu_test (o TEST_DB_URL)
```

Las de integración heredan de `src/test/java/com/galiledu/PruebaIntegracion.java` y también corren en GitHub Actions.
