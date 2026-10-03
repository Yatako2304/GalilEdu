# Academics — living module report

This document tracks the **backend implementation of Academic Management (Gestión Académica)** published on `main`. It covers domain, application and JDBC persistence only; **there is no REST adapter yet**. The backlog and database scripts live outside Git at `../backlog/` and `../scripts/` relative to the repository root.

## Owner and scope

| Field | Current value |
| --- | --- |
| Owner | To record. |
| Branch / PR | To record. |
| Assigned HU/RF | Grade-related RF referenced by the scripts and diagrams: RF-130 (scale and rules), RF-133/134 (period-bound registration), RF-136 (configured scale), RF-137/138 (consolidation), RF-36 (audit of grade changes). The exact HU numbers must be checked against the PO backlog. |
| State / last checked | **Partial**, checked 2026-10-02. Use cases and persistence pass unit tests and a PostgreSQL integration test; no HTTP routes exist. |
| Code / tests | `backend/src/main/java/com/galiledu/academica/` and `backend/src/test/java/com/galiledu/academica/`. |

## Current focus

**Unassigned.** Next technical step: the REST adapter (`infraestructura/web`) and route authorization in `SeguridadHttpConfig.java` (shared file — coordinate first).

## Implemented

Each component of the Academic Management component diagram maps to one use case. The diagram's interfaces map to ports.

| Component / interface | Use case or port | Behavior | Evidence |
| --- | --- | --- | --- |
| Criterios de Evaluación (`IItemsEvaluacion`) | `CriteriosEvaluacion` | Register, modify, logically retire and list evaluation items. Only the load's teacher can write. The competency must belong to the load's course offering, the period to the load's school year, and the period must not be `CERRADO`. Weighted-average competencies require a weight. An item that already has grades cannot be retired. Teacher, offering coordinator or `ADMINISTRADOR` can list. | `GestionAcademicaTests` |
| Registro de Calificaciones (`ICalificaciones`, `IAuditoria`) | `RegistroCalificaciones` | Registers a grade, or corrects it if one exists (row locked `FOR UPDATE`). Requirements: the load's teacher only; period `EN_CURSO` and not past `fecha_limite_notas` (America/Lima); the competency's configured scale (RF-136); and an enrollment that is `MATRICULADA`, active and in the load's section/year. Writes `REGISTRAR_CALIFICACION` / `CORREGIR_CALIFICACION` with before/after values to `seguridad.registro_auditoria` **in the same transaction**. Attaches/replaces 0..1 pedagogical evidence (URL/key only). | Unit and PostgreSQL tests |
| Consolidación de Notas (`INotasConsolidadas`) | `ConsolidacionNotas` + domain `CalculadoraNotas` | For each active enrollment of the section, it aggregates item grades into the period grade (`PROMEDIO_SIMPLE`, `PROMEDIO_PONDERADO`, `MODA`, `ULTIMO_LOGRO`). It then recomputes the final grade from the periods available (`ULTIMO_PERIODO`, `PROMEDIO_PERIODOS`) and adds the qualitative equivalent from the current conversion table. It returns students who were calculated, students without grades, and students with an indeterminate result. Allowed for the load's teacher or `ADMINISTRADOR`. | Unit and PostgreSQL tests |
| Consulta y Reportes de Notas (`INotasEstudiante`, `INotasApoderado`, `INotasSeccionTutor`) | `ConsultaNotas` | Grades by item (teacher/coordinator/admin). Item grades, period grades and final grades for one enrollment: the student, a linked guardian, the section's current tutor or admin. | Unit and PostgreSQL tests |
| Carga Académica (`ICargaAcademica`, `IPeriodosAcademicos`, `IMatriculas`) | Port `ConsultaContextoAcademico` | Read-only context: actor/roles, load, competency configuration, periods, enrollments, conversion table, coordinator/tutor/guardian relationships. | `ConsultaContextoAcademicoPostgres` |

The domain is plain Java: `ItemEvaluacion`, `CalificacionEstudiante`, `Calificacion`, `EvidenciaPedagogica`, `ConfiguracionCompetencia`, `TablaConversion`/`RangoConversion`, `NotaConsolidada` and the enums. Use cases follow the Users pattern: `@Service` with `@Transactional` and constructor-injected ports. The older `VerificarEscalaCalificacion`/`ConsultaEscalaCompetencia` (lookup by names) is kept but has no adapter; `RegistroCalificaciones` now enforces RF-136 by competency-configuration id.

## Remaining work

| Item | Missing capability or decision |
| --- | --- |
| REST | Controllers/DTOs, error mapping (`AccesoAcademicoDenegadoException` → 403, `SolicitudAcademicaInvalidaException` → 400/409, `NoSuchElementException` → 404), route rules in `SeguridadHttpConfig`. |
| Reports | `IReportesNotas`: section-wide and MINEDU exports. Not implemented. |
| Evidence storage | Upload to S3 (or another store) behind an adapter; only the reference is persisted now. |
| Consolidation scheduling | Whether consolidation runs on demand (current), when a period closes, or both — **PO decision**. |

## Database and cross-module contracts

Checked against the team scripts `gestion.sql`, `configuracionInstitucionaldll.sql`, `matricula.sql`, `personas.sql` and `seguridad.sql` (shared 2026-10-02), and against the local `GaliEdu` database. Writes go only to `gestion_academica.*` plus audit rows in `seguridad.registro_auditoria` (module `GESTION_ACADEMICA`).

`ConsultaContextoAcademicoPostgres` **reads** tables owned by Configuration, Enrollment, Persons and Security. This is a provisional adapter, because those modules do not yet publish application contracts. Once they do, only this adapter changes; the use cases keep the same port. Note that the period-grade row requires its final-grade row (`nota_periodo_competencia` → `nota_final_competencia`). Consolidation therefore upserts a provisional final grade from the periods available, then the period grade.

## How to verify

From `backend/`, run `.\mvnw.cmd test`. `GestionAcademicaPostgresTests` runs only with `GALILEDU_POSTGRES_TEST=true` and `DB_URL`/`DB_USER`/`DB_PASSWORD` pointing to a database with the scripts applied. It creates its own person → load → enrollment chain inside a transaction that is rolled back. On 2026-10-02 the whole suite passed (78 tests, including the PostgreSQL ones), and no rows remained afterwards.

## Decision log

| Date | Decision or open question | Authority/status |
| --- | --- | --- |
| 2026-10-02 | Only the load's teacher writes items/grades; consolidation also allowed to `ADMINISTRADOR`; coordinators read. | Technical default from the component diagram; **confirm with PO**. |
| 2026-10-02 | Grades require an enrollment in state `MATRICULADA` (not `PENDIENTE_PAGO`/`SECCION_RESERVADA`). | **Open for PO.** |
| 2026-10-02 | Qualitative scale does not allow average rules (no approved AD/A/B/C → number mapping). A `MODA` tie is reported as pending, not resolved. | **Open for PO**: tie-break rule. |
| 2026-10-02 | Calculated vigesimal grades are rounded HALF_UP to 2 decimals (`NUMERIC(5,2)`). The qualitative equivalent is left empty if the grade falls in a gap of the table. | Technical; **confirm rounding with PO**. |
| 2026-10-02 | An item with grades cannot be retired. Item course/competency/period cannot be changed — create another item. | Technical safety default. |
| 2026-10-02 | Consolidation works per load. If several teachers share an offering in the same section (`uq_carga` allows it), each consolidation overwrites the competency's final grade. | **Open** for PO/database team. |

## Change log

| Date | Verified change | Evidence / limit |
| --- | --- | --- |
| 2026-10-02 | Added the academic domain, four use cases, five ports and JDBC adapters, plus audit of grade changes. | 20 academic tests (unit + PostgreSQL) passed; no REST yet. |
