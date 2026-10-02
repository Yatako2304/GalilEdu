# GalilEdu documentation

**Current delivery: backend REST API and PostgreSQL integration only.** The existing frontend is outside this delivery.

> **Repository state (2026-10-02):** the backend slice described here was merged into `main` in commit `d52e0fc`. Other modules remain partial; the PO backlog and database scripts are still supplied separately.

| Need | Read |
| --- | --- |
| Join the team or resume work | [Architecture](architecture.md) → [module board](modules/README.md) → the assigned module report. |
| Implement an HU/RF | [Development guide](development-guide.md) → PO backlog and current database scripts, both shared outside Git. |
| Understand design diagrams | [Views](views/README.md), then the relevant component/class/cardinality/deployment note. |
| Check schema mismatches | [Database compatibility](database-compatibility.md) and the database-team scripts. |
| Continue Users | [Users living report](modules/users.md) and [backend API/startup guide](../backend/README.md). |

```text
docs/
├── README.md
├── architecture.md
├── development-guide.md
├── database-compatibility.md
├── modules/
│   ├── README.md             # status, ownership, mandatory report format
│   └── users.md              # one living report for the active module
└── views/                    # existing diagram notes in English
```

**Documentation rule:** each active module has one `docs/modules/<module>.md`, updated in the same PR as its code. Do not create a file per HU, endpoint or meeting. Record current owner/focus in [modules/README.md](modules/README.md). Cross-cutting decisions belong in architecture; verified code/schema gaps belong in database compatibility.

The **PO-validated backlog** at `../backlog/` determines functional behavior. The **database team scripts** at `../scripts/` determine the current schema. Both paths are relative to the `GalilEdu/` repository root and intentionally outside Git. The original diagrams are also outside Git; these notes do not replace them. Code and tests show what is actually implemented. If a source is unavailable or contradictory, record the question rather than inventing a rule. Never commit `.env`, credentials or real personal data.
