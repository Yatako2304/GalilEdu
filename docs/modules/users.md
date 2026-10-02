# Users / Security — living module report

This document tracks **local backend REST implementation**, not PO validation of requirements or code already merged into `main`. The features below were checked in the local working tree of `feat/arquitectura-seguridad-base` and are **not yet available from `main`**. The backlog and database scripts live outside Git at `../backlog/` and `../scripts/` relative to the repository root. The current delivery excludes frontend work.

## Owner and scope

| Field | Current value |
| --- | --- |
| Owner | To record when the next HU is assigned. |
| Branch / PR | To record. |
| Assigned HU/RF | No new HU assigned; existing implementation covers parts of HU-1, HU-5–HU-7, HU-9, HU-12 and HU-13. |
| State / last checked | **Partial**, checked 2026-10-02. REST and PostgreSQL tests passed for the implemented slice; the complete Users backlog is not implemented. |
| Code / tests | `backend/src/main/java/com/galiledu/usuarios/` and matching `backend/src/test/java/com/galiledu/usuarios/`. |

## Current focus

**Unassigned.** A technical candidate is a paginated administrative person/account search to locate records for HU-9 and HU-12, but the team must confirm priority before coding. This is not an in-progress task or a PO-approved new rule.

## Implemented

| HU/RF | Actor | REST contract / behavior | Coverage and evidence |
| --- | --- | --- | --- |
| HU-1 / RF-1, RF-2, RF-4 | Any active account | `POST /api/auth/login` creates a session; `GET /api/auth/me` returns account/roles. HTTP Basic is also supported. Five failed attempts lock access for 15 minutes; a blocked login returns `401` with `segundosRestantes`. | Backend integration tested against PostgreSQL. Redirect by role is not part of the backend delivery. |
| HU-5 / RF-11 | Own authenticated account, any role | `PUT /api/auth/password` checks the current password, changes only the caller's password and invalidates the session. | Voluntary, not mandatory at first login; tests cover allowed/denied paths. |
| HU-6 / RF-12–RF-16 | Administrator | `POST /api/usuarios` creates person, selected profiles, guardian links, account and multiple roles in one transaction; validates duplicate document/email and generates an annual username plus bcrypt-hashed initial password. | **Partial HU:** RF-17 email delivery is missing. The initial password is returned once to the administrator with `Cache-Control: no-store`. |
| HU-7 / RF-18 | Administrator | Registration requires an existing guardian for a student and allows several; `POST /api/estudiantes/{id}/apoderados` adds a later link. | Tested with two guardians in PostgreSQL. |
| HU-9 / RF-22–RF-23 | Administrator | `GET`/`PUT /api/personas/{id}` reads/updates person details and rejects duplicate document/email. | **Partial HU:** only lookup by UUID exists; no administrative search/list endpoint. |
| HU-12 / RF-30 | Administrator | `DELETE /api/personas/{id}` logically deactivates the person/account without deleting history. | PostgreSQL test verifies inactive state. A user-facing confirmation (RF-31) is outside this backend delivery. |
| HU-13 / RF-32 | Administrator | `POST /api/personas/{id}/reactivacion` restores an inactive person/account, preserving assigned roles/permissions. | PostgreSQL test verifies restored login, unchanged roles, CSRF and access control. |

The management routes share one controller/use-case/persistence path; there is **no duplicate backend per role**. Spring Security restricts administrative operations to `ADMINISTRADOR`. State-changing session requests require CSRF. The database-backed configurable permission matrix is not active yet.

## Remaining work

| HU/RF | Missing backend capability or decision |
| --- | --- |
| HU-2/3, RF-5–RF-7 | Institutional Google-login enablement and verified identity linking for an already registered account. External configuration/integration is absent. |
| HU-4, RF-8–RF-10 | Non-enumerating recovery request, one-use 30-minute code, email delivery, atomic consumption and password reset REST flow. Token table exists; flow does not. |
| HU-6 / RF-17 | Deliver registration credentials to the registered email. Current one-time admin response is not this requirement. |
| HU-8 / RF-19–RF-21 | `.xlsx`, `.xls` and `.csv` bulk import, per-row validation/guardian resolution, summary and errors file. |
| HU-9 | Administrative search/listing to locate a record without knowing its UUID. Define filters/pagination and minimal returned data. |
| HU-10 / RF-24–RF-25 | Change roles of an active multi-role account and audit actor, time and before/after. Ask the PO whether the operation replaces the set or adds/removes roles. |
| HU-11 / RF-26–RF-29 | Query/edit role permissions and enforce changes immediately, including approved mandatory minima. Tables exist; route authorization is currently fixed in code. |
| HU-16/17 / RF-36–RF-41 | Consistent audit writes for grade and financial changes plus authorized audit search/detail. Coordinate event contract with Academics and Payments. |
| HU-19/20 / RF-45–RF-48 | Capture/query errors by date, severity and module; export the authorized filtered set as CSV/Excel without leaking technical details. |

Do not implement unspecified role aliases or mandatory permissions by assumption. The backlog alternates `Estudiante`/`Alumno` and mentions `Director`; a catalog decision is required. RF-16 references RNF-4, which concerns least privilege, not a password-composition policy.

## Database and cross-module contracts

Identity/email reside in `personas.persona`; account state in `seguridad.usuario`; `seguridad.usuario_rol` supports multiple roles. Student/teacher/guardian are person profiles and guardian links, not exclusive account roles. Registration and reactivation use transactional JDBC adapters with bound SQL parameters. Audit for Academics/Payments must be coordinated with the owning modules; do not invent audit events from diagram connectors. See [confirmed schema notes](../database-compatibility.md).

## How to verify

From `backend/` on Windows, run `.\mvnw.cmd test`. The optional `GestionUsuariosPostgresTests` requires a prepared **test database**, `DB_URL`, `DB_USER`, `DB_PASSWORD` and `GALILEDU_POSTGRES_TEST=true`; its writes are rolled back. The general test suite and that PostgreSQL integration test passed on 2026-10-02. Re-run after merging; do not publish `.env`, passwords or real person records. See [backend startup/API guide](../../backend/README.md).

## Decision log

| Date | Decision or open question | Authority/status |
| --- | --- | --- |
| 2026-10-02 | Multiple roles per account and multiple guardians per student are allowed. | Team/product decision; implemented in current registration model. |
| 2026-10-02 | No mandatory initial password change in this scope. | Team decision; voluntary change remains available. |
| 2026-10-02 | Role-change semantics and mandatory permission catalog remain undefined. | **Open for PO**; do not invent. |
| 2026-10-02 | Backlog and SQL remain outside this Git repository. | Team decision; contributors must obtain current copies separately. |

## Change log

| Date | Verified change | Evidence / limit |
| --- | --- | --- |
| 2026-10-02 | Added REST session login, lock-time response and administrative reactivation. | PostgreSQL integration tests passed; no recovery/OAuth/permission matrix implied. |
| 2026-10-02 | Registration, multiple profiles/roles, guardian links, person read/update/deactivation and voluntary own-password change are present. | General and optional PostgreSQL tests; email delivery and search/listing remain missing. |
