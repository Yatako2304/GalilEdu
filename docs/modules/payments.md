# Payments and Pensions

## Owner and scope

- Owner: Luis; branch `develop-luis`.
- Scope: HU-34–HU-42. Current implementation slice: HU-34 / RF-70–RF-73, limited to **initial tariff registration and consultation**, including the separate RF-78 enrollment-fee due date. It is not a complete HU-34 implementation: editing an existing non-closed tariff is not implemented.
- State: partial. Last checked: 2026-10-02 against the PO backlog, `../scripts/pagos.sql`, `../scripts/configuracionInstitucionaldll.sql` and local PostgreSQL 18.

## Current focus

Define safe edits of non-closed tariffs without invalidating existing commitments, and provide school-year creation through Institutional Configuration. Generating an enrollment obligation still requires an agreed Enrollment → Payments application contract; do not infer enrollment state from tables.

## Implemented

- `POST /api/pagos/tarifarios` (administrative staff or administrator) creates one tariff for an existing, non-closed school year. Input: school-year ID, enrollment and monthly pension amounts, **pension-only** installment count and due dates, **separate enrollment-fee due date** (`vencimientoMatricula`), grace days, monthly surcharge percentage and maximum surcharge percentage. RF-71 validations reject duplicate pension dates, a pension-count mismatch and pension dates outside the school year. RF-72's maximum is required. The enrollment-fee date may precede the school-year start; no unapproved window restriction was added. A closed year and a duplicate tariff produce `409`.
- One Spring transaction creates `pagos.tarifario_escolar`, `configuracion_mora`, the two named payment categories and their `detalle_tarifa` rows, `calendario_pagos`, the enrollment-fee calendar entry and every pension calendar entry. Pension is the only one flagged for surcharge. All inserts use bound JDBC values. Failure rolls back the local database operation; it never creates a `pago` or marks a `cuota` paid.
- `GET /api/pagos/tarifarios/{anioEscolarId}` returns the registered tariff and schedule, including for a closed year. Both routes require administrative staff or administrator; other roles receive `403`, anonymous callers `401`, and POST requires CSRF.
- Existing `VerificarMontoConfirmado` and `ConsultaOrdenPagoPostgres` only compare an order's amount for RF-105. They do **not** authenticate gateway messages, confirm transactions or persist payments.
- Evidence: domain/application/security tests pass; an opt-in REST → PostgreSQL integration test creates a temporary school year and tariff, checks the fee and calendar rows, rejects a duplicate, and rolls back. Read-only counts confirmed no test records remained in the local DB.

## Remaining work

| Story | Missing capability / dependency |
| --- | --- |
| HU-34 / RF-73 | Editing a non-closed tariff safely. The SQL does not state how to change tariff/calendar details already referenced by generated installments. Do not rewrite historical obligations by assumption. |
| HU-35 / RF-74–77 | Discount catalog, tier ranges and configurable cap; all distinct from applying a discount to a student's commitment. |
| HU-36 / RF-78–80 | Enrollment must signal confirmation started with an unambiguous student/year/process identity. Its current module lacks this application contract; do not query its private tables or invent enrollment state. |
| HU-37 / RF-81–85 | Generate pension commitments after verified enrollment, including late entry and withdrawal; enrollment events and exact timing are pending. |
| HU-38 / RF-86–90 | Eligibility, one active discount, recalculate unpaid installments and preserve already paid ones. Guardian/relationship information requires a provider contract. |
| HU-39 / RF-91–94 | Overdue debt and pension-only surcharge with grace days and cap. Then expose the debt condition to Enrollment through an application contract. |
| HU-40 / RF-95–98 | Guardian-scoped student debt, installment/payment history and account statement. Requires ownership checks from Users/Enrollment. |
| HU-41 / RF-99–106 | Guardian-scoped order creation, trusted payment-gateway notification/reconciliation, idempotent transaction recording, installment updates and confirmed-payment notification to Enrollment. No gateway adapter or verified callback exists; client claims must never create a paid state. |
| HU-42 / RF-107–108 | Unique receipt numbering and authorized PDF/history after a confirmed payment. |

## Database and cross-module contracts

- External SQL source: `../scripts/pagos.sql`. `pagos.tarifario_escolar` has a unique school-year FK; its children use `configuracion_mora`, `detalle_tarifa`, `calendario_pagos` and `detalle_calendario`. Since `detalle_calendario.numero` is unique across the entire calendar, pensions retain numbers `1..N` and the enrollment-fee entry uses technical number `N+1`; `cantidad_cuotas = N` still counts **only pensions**. This number is not a chronological sequence or an additional pension installment. The script remains outside Git under the database team's ownership.
- Provider `configuracion.aplicacion.puertos.ConsultaAnioEscolar` reads school-year dates/state via its own adapter. Payments depends on that application interface, not on Config's JDBC implementation. No school-year creation endpoint exists yet; local `configuracion.anio_escolar` currently has zero rows, so live tariff registration needs Institutional Configuration to provide a year first.
- A single `@Transactional` use case covers the local tariff writes because both modules use one PostgreSQL database. This is **not** a distributed transaction with a gateway. Future gateway calls must happen outside a held database transaction; only an authenticated, verified result may enter a separate idempotent local state-change transaction. Whether that transaction also invokes Enrollment through a provider contract remains to be agreed and tested. No fake payment, gateway callback or cross-module event is implemented here.
- Payment order/payment uniqueness constraints support RF-103, but constraints alone do not implement verified notifications, reconciliation or automatic enrollment formalization.

## How to verify

- From `backend/`, run the Maven Wrapper `test` or `verify` for isolated tests.
- The opt-in `TarifariosPostgresIntegrationTests` requires `GALILEDU_POSTGRES_TEST=true` and `DB_URL`, `DB_USER`, `DB_PASSWORD`. It creates a school-year fixture inside a rollback-only Spring test transaction. Do not point modifying tests at shared data without coordination.
- The local database currently has zero school years and zero tariff records after the rollback-only test. `POST /api/pagos/tarifarios` cannot be manually demoed there until an actual year is configured.

## Decision log

- 2026-10-02: Choose HU-34 first because it is Must-have and the economic prerequisite for later payment HU. Implement only its initial registration/consultation slice; no gateway or Enrollment simulation.
- 2026-10-02: The two category codes `MATRICULA` and `PENSION` represent the two charges named in RF-70. They are created as required catalog entries within the same transaction if absent; an inactive existing category blocks registration rather than being reactivated implicitly.
- 2026-10-02: School-year dates/state are provided through the Config module's port. The school-year registration workflow itself remains in Config.
- 2026-10-02: User confirmed `cantidad_cuotas` counts only pensions and the enrollment fee has a separate due date. Store the fee date in the same calendar, using technical number `N+1` because the current SQL makes `numero` unique for the entire calendar. Do not interpret this as an extra pension installment.

## Change log

- 2026-10-02: Added separate enrollment-fee due date to HU-34 REST/domain/JDBC and PostgreSQL test following user clarification; pension count remains unchanged.
- 2026-10-02: Added HU-34 REST/use-case/domain/JDBC slice, Config school-year read contract, authorization and isolated/PostgreSQL rollback tests.
