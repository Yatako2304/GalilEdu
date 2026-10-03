# GalilEdu backend

Java 21, Spring Boot 4.1.1 and PostgreSQL 18. The backend is a [modular monolith with pragmatic hexagonal structure per module](../docs/architecture.md). Follow the [development guide](../docs/development-guide.md) and [module status board](../docs/modules/README.md). Database-team scripts are shared outside this repository at `../../scripts/`; backend startup does not create or change their tables.

## Current scope

This delivery is **backend REST only**. Users supports PostgreSQL-backed account/person registration, multiple roles, REST session login, HTTP Basic, failed-login lock, own-password change, administrative person read/update/deactivation/reactivation and guardian links. [Users status](../docs/modules/users.md) separates implemented and missing HU/RF. Payments has an initial HU-34 tariff registration/consultation flow and an earlier read-only order amount check; it has **no real payment confirmation**. [Payments status](../docs/modules/payments.md) records the dependency boundaries. Enrollment, Timetables, Academics and Attendance have initial models/use cases but not complete REST/database flows. No product demo accounts or routes are bundled.

The local database currently uses `jdbc:postgresql://localhost:5433/galiledu_local`; each teammate must configure their own `DB_URL`, `DB_USER` and `DB_PASSWORD`. The local schema has the agreed unique person-email migration `007_correo_persona_unico.sql`. See [database compatibility](../docs/database-compatibility.md) for confirmed gaps. JPA is declared in Maven but production persistence currently uses parameterized JDBC; `ddl-auto=validate` does not fully verify the external schema.

## REST API currently available

| Method | Path | Access / behavior |
| --- | --- | --- |
| `GET` | `/actuator/health` | Public health check. |
| `GET` | `/api/auth/csrf` | Public CSRF token/header-name discovery. |
| `POST` | `/api/auth/login` | Active account login, creates HTTP session; all roles. |
| `GET` | `/api/auth/me` | Current account and roles; authenticated. |
| `PUT` | `/api/auth/password` | Voluntary own-password change; authenticated; session invalidated. |
| `POST` | `/api/auth/logout` | End the current session. |
| `POST` | `/api/usuarios` | Transactional registration; administrator. |
| `GET`, `PUT` | `/api/personas/{id}` | Read/update person; administrator. |
| `DELETE` | `/api/personas/{id}` | Logical deactivation; administrator. |
| `POST` | `/api/personas/{id}/reactivacion` | Reactivate inactive account/person without dropping roles; administrator. |
| `POST` | `/api/estudiantes/{id}/apoderados` | Link another existing guardian; administrator. |
| `GET`, `POST` | `/api/configuracion/areas-curriculares` | List/create curricular areas; administrator or administrative staff. |
| `GET`, `PUT`, `DELETE` | `/api/configuracion/areas-curriculares/{id}` | Read/edit/logically disable an area; same roles. |
| `POST` | `/api/configuracion/areas-curriculares/{id}/reactivacion` | Reactivate an area; same roles. |
| `GET`, `POST` | `/api/configuracion/competencias` | List/create competencies linked to an area; same roles. |
| `GET`, `PUT`, `DELETE` | `/api/configuracion/competencias/{id}` | Read/edit/logically disable a competency; same roles. |
| `POST` | `/api/configuracion/competencias/{id}/reactivacion` | Reactivate a competency; same roles. |
| `POST` | `/api/pagos/tarifarios` | Register the initial yearly tariff, pension schedule, separate `vencimientoMatricula` and surcharge parameters atomically; administrator or administrative staff. Requires an existing school year. `cantidadCuotas` counts pensions only. HU-36 enrollment payment generation is not implemented. |
| `GET` | `/api/pagos/tarifarios/{anioEscolarId}` | Read the yearly tariff, including for a closed school year; same roles. |

All state-changing requests, **including REST login**, require CSRF. First call `GET /api/auth/csrf`, then send the returned token in its named header using the same session cookie. `POST /api/auth/login` body: `{"nombreUsuario":"...","contrasena":"..."}`. Success returns account ID, username, person ID and roles; invalid credentials return `401`, and a locked account also receives `segundosRestantes`. HTTP Basic remains available for REST clients. Use TLS outside local development; never send credentials over public HTTP.

`PUT /api/auth/password` body: `{"contrasenaActual":"...","nuevaContrasena":"..."}`. It returns `204` on success and the caller must authenticate again. No first-login forced password change exists. Other roles may use only their **own** login/account/password functions; direct administrative calls are denied with `403`. The database permission matrix is not yet active.

Example registration body (without a student profile):

```json
{
  "persona": {
    "nombres": "Ana",
    "primerApellido": "Quispe",
    "segundoApellido": "Rojas",
    "tipoDocumento": "DNI",
    "numeroDocumento": "12345678",
    "correoElectronico": "ana@example.com",
    "telefono": "987654321"
  },
  "perfiles": ["APODERADO"],
  "apoderados": [],
  "roles": ["APODERADO"]
}
```

For `ESTUDIANTE`, supply at least one UUID of an existing guardian in `apoderados`; several are allowed. Registration currently returns the generated initial password **once** to the administrator with `Cache-Control: no-store`. RF-17 email delivery is not implemented; do not describe this as the final approved credential flow.

## First administrator in a new database

Only when `seguridad.usuario` is empty, prepare the external scripts and configure `backend/.env`, then build and run interactively from `backend/`:

```powershell
.\mvnw.cmd package
java -jar target/galiledu.jar --galiledu.bootstrap-admin=true
```

The one-time bootstrap asks for real administrator details, creates missing catalog roles and displays username/initial password once. Store the credentials securely, stop that process and restart without the bootstrap flag. No test credentials belong in Git.

## Local startup and tests

Use environment variables or `backend/.env` (Git-ignored). Environment variables take precedence; [`.env.example`](.env.example) is a non-secret template. From `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd package
```

`http://localhost:8080/actuator/health` reporting `UP` only shows startup/connection, not completion of a HU. General tests use isolated H2 for applicable cases. Optional real PostgreSQL tests require `GALILEDU_POSTGRES_TEST=true` plus `DB_URL`, `DB_USER`, `DB_PASSWORD` for a **prepared test database**. `IntegracionPostgresLocalTests` reads; `GestionUsuariosPostgresTests` covers registration, REST login/lock, authorization, own-password change and role-preserving reactivation with rolled-back test changes. The package output is `target/galiledu.jar`.
