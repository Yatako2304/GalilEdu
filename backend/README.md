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
├── usuarios/       login (HU-1) y gestión de cuentas (HU-6, HU-9, HU-12)
└── estudiantes/    inscripción del estudiante (HU-7)

Cada módulo:  dominio/ (entidades JPA, reglas)  aplicacion/ (servicios + DTOs)
              infraestructura/ (repositorios)   web/ (controladores REST)

src/main/resources/db/
├── migration/      V1__personas_y_seguridad.sql, V2__estudiantes_inscripcion.sql
└── datos-prueba/   R__datos_prueba.sql (un usuario por rol)
```

Reglas: un esquema de BD por módulo; los controladores no tocan repositorios directamente; los mensajes de error van en español porque el frontend los muestra tal cual (`{ "message": "..." }`).

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
| GET | `/estudiantes?q=&estado=&page=` | Admin, Personal adm. | Lista de estudiantes |
| GET | `/estudiantes/{id}` | Admin, Personal adm. | Ficha (datos, ficha médica, familia, contactos, hermanos) |
| GET | `/estudiantes/personas?documento=` | Admin, Personal adm. | Busca padre/madre/apoderado ya registrado (404 = persona nueva) |
| POST | `/estudiantes/inscripciones` | Admin, Personal adm. | Registra la inscripción completa (5 pasos) |
| POST | `/estudiantes/{id}/apoderado/documento` | Admin, Personal adm. | Sube o reemplaza el documento del apoderado (multipart: `tipo`, `archivo`; PDF/JPG/PNG ≤ 5 MB) |
| GET | `/estudiantes/{id}/apoderado/documento` | Admin, Personal adm. | Descarga ese documento |

## Formato

Spotless con el formateador de Eclipse (tabulaciones, 120 columnas; ajustes en [`formato-java.prefs`](formato-java.prefs)). `.\mvnw.cmd spotless:apply` formatea; `verify` y el CI fallan si algo no está formateado. El hook de commit lo aplica solo a los `.java` que commiteas.

## Pruebas

```bash
.\mvnw.cmd test      # unitarias (sin BD)
.\mvnw.cmd verify    # unitarias + integración: necesita la base vacía galiledu_test (o TEST_DB_URL)
```

Las de integración heredan de `src/test/java/com/galiledu/PruebaIntegracion.java` y también corren en GitHub Actions.
