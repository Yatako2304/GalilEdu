# Enrollment

## Owner and scope

- Owner: Luis; branch `develop-luis`.
- Scope: HU-23–HU-33 and related RF-49–RF-69. State: partial. Last checked: 2026-10-02 against the PO backlog, page 9 `Vista_general_V2` of the class diagram, `../scripts/matricula.sql`, `../scripts/configuracionInstitucionaldll.sql`, `../scripts/personas.sql` and the existing backend.

## Current focus

Have the database team review and apply `../scripts/migraciones/008_periodo_matricula.sql` so the HU-23 endpoint works outside rollback-only tests. Then agree on guardian-scoped status/debt/document contracts for HU-27/HU-30 before payment initiation. No client-controlled payment confirmation will be added.

## Implemented

- `PUT /api/matriculas/periodos/{anioEscolarId}` and `GET /api/matriculas/periodos/{anioEscolarId}` configure/consult the **enrollment** window (HU-23/RF-51). They validate the year through Configuration's `ConsultaAnioEscolar` port and persist by parameterized JDBC to the prepared migration table. GET reports whether a date falls inside the period; the default date is in `America/Lima`. Only administrator/administrative staff can call these routes. This is not the guardian-scoped HU-27 view. The new migration is **prepared, not applied** to the existing local database, so these routes are not yet usable there.
- `POST /api/matriculas/reservas` and `GET /api/matriculas/reservas/{id}` implement the administrative reservation slice of HU-24/RF-52–RF-53. The POST checks an active student through Users, obtains active same-year/same-grade sections through Configuration, locks candidate section rows, counts active reservations/pending/confirmed enrollments, chooses the requested section if it has capacity or the first available alphabetical alternative, and writes `SECCION_RESERVADA` with no enrollment code or confirmation date. Existing student/year enrollment or no remaining capacity returns `409`. `matricula.matricula` also has a unique student/year constraint as a concurrency backstop. The reservation is one local transaction.
- Authorization/CSRF, domain/application tests and opt-in REST → PostgreSQL tests passed. The PostgreSQL fixtures and test-only enrollment-window DDL were rolled back; no migration was installed and no demo data persisted.

## Remaining work

| Story | Missing capability / dependency |
| --- | --- |
| HU-23 / RF-51 | Review/apply the prepared migration to each target database. `configuracion.anio_escolar.fecha_inicio/fecha_fin` are school-year dates, not enrollment dates. The endpoint was tested only with a transactionally created/rolled-back matching table. |
| HU-24 / RF-52–RF-53 | The diagram and SQL place `grado_id` on `Seccion`, not `Estudiante`. The selected section determines the grade, and alphabetical fallback stays within it. There is no separate historical/official grade eligibility check for a new student. If the PO requires that validation, Users/Configuration must provide its source before claiming full RF-52 coverage. Section reservations occupy capacity until a later workflow changes/deactivates them; release/expiry is not yet defined. |
| HU-27 / RF-58–RF-60 | Guardian-scoped status query needs the real enrollment window, the student's authorized guardian link, section/grade and reasoned debt/document blockers. |
| HU-28–HU-29 / RF-61–RF-63 | Document templates and secured file storage/download; validate required documents and format/size. Database metadata is not a file-storage implementation. |
| HU-30 / RF-64 | Payments must expose debt for the guardian and all linked children; Enrollment must not read Payments tables directly. |
| HU-31–HU-32 / RF-65–RF-68 | Payment initiation, verified confirmation, unique enrollment code and idempotent formalization require a cross-module agreement. The conversation selected automatic formalization after verified payment, while the current RF-65/RF-68 wording gives a conflicting order; do not silently treat the proposed correction as PO approval. RF-67's insurance type has no column in `personas.estudiante` or `matricula.matricula`. |
| HU-33 / RF-69 | Notification after real confirmation requires an email adapter and reliable dispatch; it is not part of the initial reservation slice. |
| HU-25–HU-26 | Exceptional enrollment and section changes depend on the same capacity/window rules; they are not a shortcut around payment confirmation. |

## Database and cross-module contracts

- `matricula.matricula` owns the enrollment record and unique `(estudiante_id, anio_id)` constraint; states are `SECCION_RESERVADA`, `PENDIENTE_PAGO`, `MATRICULADA`. Do not create a final enrollment code or `MATRICULADA` state while payment is unverified. The initial regular reservation records `tipo=REGULAR`; exceptional enrollment is a separate pending HU-25 workflow.
- Users/Persons provides `ConsultaEstudianteActivo` for reservation. It owns guardian–student links, plus the student's additional address/contact/medical data; a separate contract is needed for guardian authorization and later updates.
- Institutional Configuration provides `ConsultaAnioEscolar` and `SeccionesParaMatricula`. The latter locks active candidate section rows in stable order before Enrollment counts occupied seats; both modules use the same local PostgreSQL transaction. Page 9 of the class diagram shows `Matrícula → Seccion → Grado` and no direct `Estudiante → Grado` association. This supports section-derived grade, **not** an independent grade eligibility rule.
- The user authorized preparing `../scripts/migraciones/008_periodo_matricula.sql` outside Git, in the database team's script directory. It creates one enrollment window per school year with a valid date range and a year FK. It has not been applied; the database team must receive/review it before deployment or a manual call to the period endpoint.
- Payments owns debt, the enrollment-fee obligation, authenticated payment result and pension generation. Gateway claims from a client are not a trusted confirmation. Enrollment and Payments can share one local PostgreSQL transaction for synchronous in-process operations, but an external gateway call must not be held inside it.
- Academic, Attendance and Timetables consume a confirmed enrollment identifier; their existence does not justify marking a reservation as confirmed.

## How to verify

- From `backend/`, run Maven `test` for isolated/domain/security tests. Opt-in `PeriodosMatriculaPostgresIntegrationTests` and `ReservasMatriculaPostgresIntegrationTests` require `GALILEDU_POSTGRES_TEST=true` and local `DB_URL`, `DB_USER`, `DB_PASSWORD`. Both are rollback-only. The period test creates the proposed table **inside its transaction**, then rolls the DDL back; it does not apply migration `008`.

## Decision log

- 2026-10-02: User authorized a separate enrollment-window migration; prepared `008_periodo_matricula.sql`, without applying it to the local database.
- 2026-10-02: Reviewed the actual page 9 class diagram. Student has no grade field/edge; Enrollment points to Section, which owns grade. Administrative section choice determines grade for this reservation slice; it does not certify prior-grade eligibility.
- 2026-10-02: Start with HU-23 period configuration and HU-24 section reservation rather than a fake paid enrollment. Trusted payment, guardian ownership and complete HU-27–HU-32 flows remain pending.

## Change log

- 2026-10-02: Added HU-23/HU-24 REST/application/domain/JDBC slices, Users/Configuration provider ports, authorization and isolated/PostgreSQL rollback tests; prepared external migration `008`.
- 2026-10-02: Created the living handoff report for the Enrollment MVP and recorded cross-module/schema dependencies.
