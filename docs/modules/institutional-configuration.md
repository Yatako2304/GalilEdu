# Institutional Configuration

## Owner and scope

- Owner: Luis; branch `develop-luis`.
- Scope: HU-43, HU-44, HU-52–HU-56, HU-74 and HU-75. The RF index places related RF-109–RF-113 under Timetables and RF-126–RF-132 under Academics; the HU/module and table ownership still point to Institutional Configuration for the corresponding catalogs.
- State: partial; HU-52 and HU-53 are the first implemented slice. PO acceptance of a story does not mean it is implemented.
- Last checked: 2026-10-02 against `../backlog/` and `../scripts/configuracionInstitucionaldll.sql`.

## Current focus

Reconcile course/grade offerings and the other configuration workflows against the approved HU before coding them. The first catalog slice now has an isolated test and a rollback-only PostgreSQL REST integration test.

## Implemented

- HU-52 / RF-126: `POST/GET/PUT/DELETE /api/configuracion/areas-curriculares[/{id}]` and `POST /api/configuracion/areas-curriculares/{id}/reactivacion` create, query, edit, logically disable and reactivate areas. Name and description are required on writes, despite the nullable SQL description. Responses include `activo`; list includes active and inactive records.
- HU-53 / RF-127: equivalent `/api/configuracion/competencias` routes manage competencies linked to an existing area by database FK. No role-specific controller or SQL string concatenation.
- Both route families allow `PERSONAL_ADMINISTRATIVO` and `ADMINISTRADOR` (administrator has full system configuration access per backlog role catalog). Other roles receive `403`; unauthenticated requests receive `401`. State-changing calls require CSRF. Duplicate names/FK conflicts return sanitized `409`; unknown IDs return `404`.
- Disabling an area does not cascade to its competencies, and the current DDL still permits a competency to reference an inactive area. No implicit cascade or active-parent policy has been invented; this dependency rule needs a PO/team decision before claiming complete catalog lifecycle coverage.
- Evidence: isolated domain, H2 JDBC-mapping and HTTP/security tests pass with `mvn test`. A REST → application → PostgreSQL integration test passed on the local `galiledu_local` database with Spring's test transaction rollback; it covered create/edit/disable/reactivate and confirmed persisted values within the transaction. This does not certify the remaining HU.

## Remaining work

| Story | Current schema fit | Implementation/decision gap |
| --- | --- | --- |
| HU-43 / RF-109–112 | `espacio_fisico` stores code, name, type, capacity, resources and `activo`. | Create/search/edit/deactivate are absent. Capacity reduction must check assigned sections' actual enrollment; deactivation must check availability. Those checks cross Enrollment/Timetables. DB uniqueness covers all codes, while RF-109 mentions active codes only. |
| HU-44 / RF-113–114 | `estructura_horaria`, `bloque_horario`, `receso` store the schedule parameters and generated times. | HU says one structure per institution/year, but schema allows multiple codes per year and has no unique active structure. RF-113 names Coordinator, HU-44 names administrative staff. Block/recess calculation and exclusive active-year behavior require implementation/decision. RF-114's grid belongs to Timetables consumption. |
| HU-52 / RF-126 | `area_curricular` stores name, description and `activo`. Description is nullable in SQL but required by RF-126. | First REST/JDBC slice implemented and verified with rollback-only PostgreSQL test. |
| HU-53 / RF-127 | `competencia` has an area FK, name, description and `activo`. | First REST/JDBC slice implemented and verified with rollback-only PostgreSQL test. |
| HU-54 / RF-128 | `curso` stores area and name; `oferta_curso` associates a course with grade and school year. | RF requires grade(s) when registering a course, but standalone `curso` has no grade association; an annual offer is not necessarily the same catalog association. Resolve this contract before implementation. |
| HU-55 / RF-129 | `tabla_conversion` and `rango_conversion` hold one table and four AD/A/B/C ranges. | SQL does not guarantee full 0–20 coverage or no overlap/gaps between ranges. Validate before marking a table current. |
| HU-56 / RF-130–132 | `oferta_curso` and `configuracion_competencia` hold annual course/grade offers, scales and rules. | No constraint ensures competency and course share an area. `bloqueada` exists but locking semantics need a verified trigger. RF names Coordinator; HU names administrative staff. |
| HU-74 | `anio_escolar` and `periodo_academico` have dates, regime, states and note deadline. | No RF linked. E-40 requires periods inside year, no overlap and one active year; these are not enforced by SQL. State transitions and notification ownership need agreement. |
| HU-75 / RF-49–50 | `seccion` holds year, grade, name and capacity; `tutor_seccion` holds tutor/co-tutor. | HU includes tutor; RF-49/50 live under Enrollment and add deletion/vacancy checks. Coordinate enrollment counts and tutor lifecycle before writes. |

## Database and cross-module contracts

- Source: external `../scripts/configuracionInstitucionaldll.sql`; the local PostgreSQL 18 database currently has all 17 `configuracion` tables. The script remains owned by the database team outside Git.
- First slice: `configuracion.area_curricular` and `configuracion.competencia`. The latter has a foreign key to the former. Both use UUID and logical `activo` state.
- Enrollment consumes school year/section/capacity; Timetables consumes structure, blocks, physical rooms and academic load; Academics consumes periods, competencies, evaluation configuration and conversion. Consumers should call an agreed application contract, not this module's JDBC adapter.
- Do not derive the enrollment window from school-year dates: RF-51 has a separate meaning and the current `configuracion` schema has no enrollment-window columns.

## How to verify

- Run `.\mvnw.cmd test` from `backend/` on Windows for isolated tests. The code was also run with a local Maven installation/cache when the restricted shell could not read cached jars.
- The opt-in `CatalogoCurricularPostgresIntegrationTests` uses `GALILEDU_POSTGRES_TEST=true` and `DB_URL`/`DB_USER`/`DB_PASSWORD`. It was run against local PostgreSQL 18; `@Transactional` rolls back its records. Do not point it at shared data without coordinating with its owner. A successful application start alone does not verify an HU.

## Decision log

- 2026-10-02: Start with HU-52/HU-53 because HU and SQL agree on the main catalog fields and actor; document rather than silently settle conflicts in HU-44, HU-54, HU-56 and HU-74/75.
- 2026-10-02: Treat PO-validated HU text as functional authority; SQL describes the available persistence structure and RF/scenario differences remain visible here.

## Change log

- 2026-10-02: Audited nine institutional-configuration HU against the current PostgreSQL DDL and started the first implementation slice.
- 2026-10-02: Added area/competency domain values, application port and use cases, parameterized JDBC, role-protected REST routes and isolated tests (`mvn test` passed).
- 2026-10-02: REST/JDBC integration test passed against local PostgreSQL 18, with rollback at the end of the test.
