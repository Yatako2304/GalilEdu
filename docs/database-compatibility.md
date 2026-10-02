# Backend ↔ PostgreSQL compatibility

Recorded against the team scripts and local `galiledu_local` database on **2026-10-02**. The local database had migrations `001_seguridad_y_gestion.sql` through `004_sin_cambio_obligatorio.sql` and `007_correo_persona_unico.sql`; `005` and `006` had not been applied there. These statements describe **that local environment only**, not every teammate's database. Seven test accounts existed only locally and are not in Git. Scripts live outside the repository at `../scripts/` from the `GalilEdu/` root and must be shared by the database team. Do not re-run one-time migrations on that database without checking its migration state.

## Confirmed mapping and gaps

| Area | Database fact | Backend status / action needed |
| --- | --- | --- |
| Documents | `personas.tipo_documento` supports `DNI` and `CARNET_EXTRANJERIA`. | Java enum was aligned. |
| Users and roles | `seguridad.usuario` references `personas.persona`, which owns email; `seguridad.usuario_rol` implements many-to-many roles. The local schema no longer has `seguridad.usuario.email`. | JDBC joins account and person and supports multiple roles. The agreed person-email uniqueness migration is applied locally. |
| Login and own password | `seguridad.usuario` has `username`, `password_hash`, failed attempts and lock expiry, with no mandatory first-change flag. There is an annual counter and a recovery-token table. | REST session login and HTTP Basic authenticate against PostgreSQL; attempts/lock and voluntary own-password change are implemented. Recovery and RF-17 email delivery remain pending. |
| Permissions, audit and errors | `seguridad.permiso`, `rol_permiso`, `registro_auditoria` and `error_sistema` exist. | Current Users routes have fixed administrator authorization. Configurable permission enforcement and audit/error write/query/export flows are **not** complete. |
| Academics | Evaluation items identify a period. Numeric grades are constrained to 0–20 and a grade has one scale. | The use case still needs teacher/section/competency/period authorization, plus consistent audit recording when grades change. |
| Enrollment | `matricula.matricula` has state and registration/confirmation dates; `configuracion.anio_escolar` has school-year dates. | `ConsultaPeriodoMatricula` needs the **enrollment window**, which must not be inferred from school-year dates. Confirm its source for RF-51/RF-59 before writing enrollment logic. |
| Attendance | `asistencia.jornada_asistencia` is identified by academic load and date; details point to a session and enrollment. | An initial port searches only by student/date, possibly matching several sessions. Define the session/load identity before writes (RF-143, HU-65). |
| Timetables | Blocks belong to `configuracion.estructura_horaria`; assignments refer to a `bloque_horario_id` UUID. | The initial port only uses a block number, ambiguous across structures. Include block identity and valid periods before writing assignments. |
| Payments | `pagos.orden_pago` has `codigo_orden` and `monto_total`. | JDBC order lookup and amount comparison were tested against PostgreSQL. Payment confirmation still needs authenticated gateway results, transactions and idempotency (RF-103–RF-108). |

Users already has transactional JDBC writes for registration, person lookup/update, logical deactivation/reactivation and guardian links. Optional PostgreSQL tests cover REST login/lock, role-preserving reactivation and rollback after a failed registration. A healthy connection does not prove the other stories are implemented.

## Why the Security and Academics scripts were adjusted

This section preserves the rationale of the earlier SQL review, **not** instructions to edit or re-run the external scripts.

| Change recorded | Requirement/rationale | Remaining application work |
| --- | --- | --- |
| Email was removed from `seguridad.usuario`; account lookup joins `personas.persona`. | A duplicated email could diverge when RF-22 edits a person and affect Google linking (RF-7). | Maintain identity/account consistency. |
| `username`, bcrypt-compatible `password_hash`, an annual counter and username-format constraint were added. | RF-1 needs local credentials; RF-15 specifies generated annual usernames without duplicates. | Allocation is transactional; check concurrency. |
| `intentos_fallidos` and `bloqueado_hasta` were added; mandatory first-password-change state was removed. | RF-2 requires five failures, 15 minutes and remaining time. RF-16 does not mandate an initial change in the current scope. | Lock and own-password change are implemented; do not reintroduce forced change. |
| Recovery token stores a hash, expiry and consumption. | RF-8–RF-10 require a one-use code valid for 30 minutes without exposing whether an account exists. | Email delivery and atomic consume/reset flow remain pending. |
| Google enablement and optional `google_sub` were represented. | RF-5–RF-7 require configured access and association with an existing account. | Identity verification/integration remain pending. |
| Permission records gained module/action and enabled/mandatory fields; audit and error tables were added. | RF-25–RF-29, RF-36–RF-48 need configurable permissions, change history and error queries. | Approved minimum-permission catalog, writes, queries and exports remain pending. |
| Evaluation items gained `periodo_academico_id`; grade-scale/range and final-grade/enrollment integrity constraints were added. | RF-130/133/134/136–138 need period-aware, single-scale, consistent grades. | The application must still validate teacher, course, section, competency and period state. |

Columns and tables are an implementation of approved capabilities, not names mandated by the PO. Use cases depend on ports; only persistence adapters know this SQL. Controllers must not query these tables directly.

## Open decisions and coordination

- RF-16 calls the generated initial password definitive while RF-17 calls it temporary. The current scope has **no forced first-change flow**. A proposed wording correction exists in `../backlog/correcciones-propuestas/gestion-de-usuarios.md` outside Git; the PO must validate it.
- The class diagram includes `Sesion`, but the backend currently uses local `HttpSession`, not a dedicated session table. Multi-instance deployment requires an explicit shared-session, affinity or stateless-authentication decision; a new table alone does not solve Spring session distribution.
- The scripts do not seed the approved role/permission catalog or mandatory minima. Do not invent them. The precise role-change semantics for multi-role accounts also need PO confirmation.
- An audit table does not make grade/payment changes auditable by itself. The owning use case must record an approved event consistently with the change.
- Academics still needs business validation tying school year, offering, load, competency and enrollment together; foreign keys only prove referenced rows exist.
- External scripts remain outside Git by team decision. Coordinate every schema change with the database team and record the exact version used for tests.
