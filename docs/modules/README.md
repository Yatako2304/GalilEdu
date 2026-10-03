# Module ownership and status

Last reviewed: **2026-10-02**. The verified Users/backend, Institutional Configuration and initial Payments slices are published on `main`; `develop-luis` remains Luis's working branch. This is the single project-level board for progress. `partial` means useful code exists but the module's full backlog is not implemented. A PO-approved/complete HU is **not** necessarily coded. Fill in actual owners and branches/PRs when the team assigns work; do not invent them.

| Module | Verified backend state | Owner / branch or PR | Current HU or focus | Living report |
| --- | --- | --- | --- | --- |
| Users / Security | Partial REST login and basic account/person management against PostgreSQL; available on `main`. | To assign | To agree; candidate: administrative search/listing. | [users.md](users.md) |
| Payments | Partial HU-34 tariff registration/consultation with pension schedule and separate enrollment-fee due date, plus earlier read-only order amount comparison; isolated and rollback-only PostgreSQL tests pass. | Luis / `develop-luis` | Resolve safe tariff edits, school-year creation and Enrollment contract; gateway payments remain unimplemented. | [payments.md](payments.md) |
| Enrollment | Initial: model and period-query port; no complete REST/database flow. | Unassigned | Agree with Payments and database team. Product decision: formalize automatically after confirmed payment; not implemented. | Create `enrollment.md` when work starts. |
| Timetables | Initial: overlap rules and availability-query port. | Unassigned | Identify block/structure correctly before persistence. | Create `timetables.md` when work starts. |
| Academics | Partial on `main`: domain, use cases and JDBC persistence for items, grades with audit, consolidation and queries; no REST. | To assign | Next: REST adapter and route authorization. | [academics.md](academics.md) |
| Attendance | Initial: states and a correction use case. | Unassigned | Identify the session/load unambiguously before writes. | Create `attendance.md` when work starts. |
| Institutional Configuration | Partial HU-52/HU-53 curricular catalog REST/JDBC slice; isolated and rollback-only PostgreSQL tests pass. | Luis / `develop-luis` | Resolve HU-54 course/grade mapping for the next slice. | [institutional-configuration.md](institutional-configuration.md) |

Other Institutional Configuration workflows remain pending. Do not create a CRUD just because a diagram shows a component.

## Documentation rule

Each module with assigned work maintains **exactly one living file** at `docs/modules/<module>.md`. The owner updates it **in the same PR as code**. Do not create separate Markdown files per HU, endpoint, sprint, meeting or experiment. Add a new file only when a module actually starts; link it from this table. Any exceptional historical or cross-cutting document requires team agreement and a link from the relevant living document. Shared decisions belong in [architecture](../architecture.md), confirmed schema differences in [database compatibility](../database-compatibility.md), and coding/workflow rules in the [development guide](../development-guide.md).

## Required report format

Keep these headings, in this order, so another person or AI can resume without searching through chat history:

1. `Owner and scope` — owner, branch/PR, assigned HU/RF, last checked date, module state (`not started`, `in progress`, `partial`, `verified`, `blocked`).
2. `Current focus` — the **one task currently being worked on**, or `Unassigned`; include next action and blocker.
3. `Implemented` — HU/RF, allowed actor, REST method/path, behavior, important tables/contracts and test evidence. Mark partial coverage explicitly.
4. `Remaining work` — next HU/RF and exact missing capability. Separate an implementation gap from a decision awaiting the PO, database team or another module.
5. `Database and cross-module contracts` — scripts/version checked, tables used and provider/consumer boundary. Never copy whole external scripts into the report.
6. `How to verify` — relevant commands, database prerequisites and limits of the tests; never credentials.
7. `Decision log` — dated, short decisions with source/authority; keep unresolved questions visible.
8. `Change log` — newest first; date, concrete change and evidence/PR. It is a handoff log, not a copy of every commit.

The [Users report](users.md) is the filled example. When a contributor changes a module's state or focus, update **this table and that module's file**, not a second status list in architecture. Read the [development guide](../development-guide.md#parallel-work-and-documentation-rules) before touching shared security/configuration files.
