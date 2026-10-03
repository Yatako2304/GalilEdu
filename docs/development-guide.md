# Backend development and collaboration guide

This is the shared working standard for contributors and coding assistants. Read [architecture](architecture.md), the [module board and reporting format](modules/README.md), and [database compatibility](database-compatibility.md) before changing a module. Users is a structural example, not a guarantee that all of its stories are finished. **This delivery is backend REST only.**

## Source and decision order

1. Read the PO-validated HU/RF in `../backlog/` and the current database-team scripts in `../scripts/` (paths from repository root). Both are deliberately outside Git; obtain the same version as the rest of the team.
2. Use the appropriate [diagram view](views/README.md) for vocabulary, boundaries and relationships. A class diagram does not directly define a Java class or PostgreSQL table.
3. If the backlog, diagram, SQL and code disagree, record the exact requirement, affected table/operation and unanswered question in the module report. The PO decides functional behavior; the responsible technical team decides implementation details that do not change that behavior. Do not fill a gap with an assumption.
4. Report status as `not started`, `in progress`, `partial`, `verified` or `blocked by decision`. Compilation or a domain class alone never means a HU is complete.

## One story through the backend

```text
HTTP request → controller + DTO → use case → domain rule
                                   ↓ application port
                              JDBC adapter → PostgreSQL
```

| Concern | Existing Users example | Standard to reuse |
| --- | --- | --- |
| Domain | `backend/src/main/java/com/galiledu/usuarios/dominio/DatosPersonales.java` | Business values and rules without HTTP or SQL. |
| Use case | `backend/src/main/java/com/galiledu/usuarios/aplicacion/GestionUsuarios.java` | Constructor dependencies, orchestration and one transaction per complete business operation. |
| Port | `backend/src/main/java/com/galiledu/usuarios/aplicacion/puertos/RepositorioGestionUsuarios.java` | Required capability, not a SQL/table-shaped interface. |
| REST input | `backend/src/main/java/com/galiledu/usuarios/infraestructura/web/GestionUsuariosController.java` | Explicit DTOs, validation and HTTP response/error mapping. |
| Database output | `backend/src/main/java/com/galiledu/usuarios/infraestructura/persistencia/RepositorioGestionUsuariosPostgres.java` | Parameterized JDBC and row mapping against the supplied schema. |
| Authorization | `backend/src/main/java/com/galiledu/usuarios/infraestructura/seguridad/SeguridadHttpConfig.java` | Explicit route access; new routes require permission and negative tests. |
| Tests | `backend/src/test/java/com/galiledu/usuarios/` | Domain, REST/security and database verification without hardcoded demo users. |

Paths in the table are relative to `GalilEdu/`. Reuse the separation of responsibilities, **not** the Users business rules or a copy of its controller for each role.

## Coding standard

Apply these conventions to **new and changed backend code**. They describe the existing modular, hexagonal Spring Boot structure; do not rename whole modules merely to conform. A deliberate exception belongs in the affected module report.

| Topic | Required convention |
| --- | --- |
| Language and naming | Keep Java identifiers descriptive and consistent with the owning module's established vocabulary. Packages are lowercase; classes/interfaces/records use `PascalCase`, methods and fields `camelCase`, constants `UPPER_SNAKE_CASE`. Keep REST JSON field names and SQL column names stable; map them explicitly instead of leaking database names into the domain. |
| Package ownership | New behavior goes under `com.galiledu.<modulo>/{dominio,aplicacion,infraestructura}`. `aplicacion/puertos` holds interfaces; `infraestructura/web` holds controllers and request/response DTOs; `infraestructura/persistencia` holds JDBC adapters. Shared HTTP plumbing alone belongs under top-level `infraestructura/web`. |
| Encapsulation | Domain state is private and constructed in a valid state. Prefer immutable values/records for small value objects and DTOs. Expose behavior or read-only accessors, not public mutable fields or setters that bypass invariants. Avoid static mutable state and hardcoded production/demo data. |
| Dependency direction | `dominio` depends on neither Spring nor persistence/web packages. `aplicacion` depends on domain and capability-oriented ports, not JDBC or controllers. Adapters implement ports and translate HTTP/SQL shapes at the boundary. Cross-module use cases call a provider's contract; they do not import another module's adapter. |
| Spring wiring | Use constructor injection; no field injection. Put `@Service` on application implementations and `@Repository`/`@Component` on adapters as appropriate. Controllers orchestrate HTTP only; business decisions belong in the use case/domain. Do not introduce an interface for every class: add a port when a boundary needs an interchangeable capability. |
| REST and errors | One endpoint/use case per capability, not per role. Use explicit request/response DTOs and validation; never return entities, password hashes, raw exceptions or SQL errors. Use consistent status codes and the shared error mapping. Document actor and authorization for each new route, with denied-access tests. |
| Persistence | Use the team-owned PostgreSQL schema, explicit column lists and bound JDBC parameters. Keep SQL and row mapping in adapters. Use UUIDs and database types according to the actual scripts; do not enable automatic schema creation. Never commit credentials or local `.env` values. |
| Transactions | Place `@Transactional` at the application operation that must succeed or fail as a whole; avoid splitting one business action across independently committed calls. For competing writes, specify locking/uniqueness and test the conflict path. For retries or external callbacks, define an idempotency key before enabling writes. |
| Tests | Mirror the production package under `src/test/java`; name test classes after the subject and scenarios after behavior. Cover success, invalid input, unauthorized access and failure/rollback. Keep PostgreSQL integration fixtures isolated and rollback-only where possible; never rely on data from a contributor's normal database. Report the exact command and skipped tests. |
| Change size | Keep a change centered on one HU/RF or coherent technical prerequisite. Avoid unrelated rewrites, duplicate role-specific flows, speculative abstractions and new Markdown files when the existing module report or this guide fits. |

Code review should enforce the boundary and observable behavior, not whitespace preferences. Follow the formatting already present in the edited file; if the team later adopts a formatter, configure it once for the whole backend and record that change here.

## Implementing an HU/RF

1. In the module's single `docs/modules/<module>.md` report, identify HU/RF, actor, accepted input/result, errors, affected tables and unresolved decisions.
2. Choose the module that owns the behavior. If another module needs it, agree on an exposed application contract, data and transaction boundary first. Do not import the other module's persistence implementation or query its private tables directly.
3. Put domain invariants in `dominio`; encapsulate state. Do not use JPA entities or HTTP DTOs as a substitute for a domain model.
4. Define the use case in `aplicacion` and its needed interfaces in `aplicacion/puertos`. Ports must not mention `JdbcTemplate`, `ResultSet`, controllers or SQL.
5. Implement adapters against the current scripts. Bind client values as JDBC parameters (`?`); never concatenate user input into SQL. Check constraints, concurrency and idempotency. Use one clear transaction for a multi-row business operation; read-only queries may be marked `readOnly`.
6. Add the REST adapter only when the story requires it: explicit DTOs, request validation, stable status codes and sanitized errors. Review route authorization, CSRF, `401`/`403`, and exposure of personal or secret data.
7. Test business rules, HTTP/security and persistence. H2 is useful for isolated tests but is not equivalent to PostgreSQL types, schemas or constraints. Integration tests that write must use a prepared test database and roll back/clean up safely.
8. Update the **same module report in the same change**. Update this architecture guide only for a shared decision; update database compatibility only for a confirmed code/schema difference.

## Cross-cutting rules already used

- **Transactions:** Users registration creates person, profiles, links, account and roles atomically. Failed-login attempts and lock state must persist even when authentication fails; `AutenticadorPostgres` supplies that transaction boundary.
- **Identity:** `personas.persona` stores identity/email, `seguridad.usuario` stores account state, and `seguridad.usuario_rol` allows several roles. Student/teacher/guardian are person profiles or links.
- **Credentials:** bcrypt hashes only; no plaintext password in logs or Git. `backend/.env` is local and ignored; `.env.example` must not contain a real secret.
- **Security:** `/api/auth/me` and voluntary own-password change are for any authenticated role; managing other accounts/persons requires administrator access. `POST /api/auth/login` is public but requires CSRF. The configurable database permission matrix is not active yet. Do not add `permitAll` to bypass a failing test.
- **Schema:** the local database has the agreed unique person-email migration. Other environments need coordinated scripts. Backend startup does not create or modify the schema.
- **External systems:** S3, SQS, payment gateway, Google and email belong behind adapters when their HU is implemented. Their presence in a deployment diagram is not evidence of working integration.
- **No product demo data:** test fixtures may exist in an isolated local database, but no demo repository or committed test credentials belong in production code.

## Parallel work and documentation rules

- Assign **one module owner and a concrete HU/RF** before work starts. Record owner, branch/PR and current focus in [modules/README.md](modules/README.md). The owner updates **one living report** `docs/modules/<module>.md` with code changes. Do not create a file per HU, endpoint, meeting or experiment.
- Coordinate shared files before editing: `SeguridadHttpConfig.java`, `backend/pom.xml`, `application.properties`, `docs/architecture.md` and `docs/database-compatibility.md`. Re-run tests after merging work that touches them.
- One REST operation is shared by allowed roles. Authorization varies by role; avoid duplicated controllers/use cases per role.
- The database team owns scripts outside Git. If a required column/constraint is absent, record the mismatch and request a migration; do not silently alter a shared database or invent a new schema.
- Cross-module contracts must identify the provider, caller, inputs, results/errors and transaction responsibility. In particular, Payments owns verifiable payment confirmation; Enrollment consumes an agreed result and must not trust a client-declared payment. Academic owns grade changes and coordinates audit writes as part of a consistent operation. These boundaries do **not** mean those flows are already implemented.
- A PR description should name the HU/RF, changed REST routes, tables, authorization, tests executed, limitations and module-report update. Review the diff for secrets, real data and unrelated edits.

## Environment and verification

Use Java 21, the Maven Wrapper and PostgreSQL 18. The current local PostgreSQL listens on `localhost:5433`, but every contributor configures `DB_URL`, `DB_USER` and `DB_PASSWORD` for their environment. See [backend/README.md](../backend/README.md) for startup and API details.

From `backend/` on Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Optional PostgreSQL tests require a prepared **test database** and their documented environment flag; read a test before enabling it. A successful build or `/actuator/health = UP` is not proof that an HU works end to end.

### Contribution checklist

- [ ] HU/RF and acceptance criteria match the current PO backlog.
- [ ] Module owner, use case, port and adapters are separated where needed.
- [ ] SQL is parameterized and checked against current team scripts.
- [ ] Authorization, CSRF, validation, sanitized errors and sensitive data are checked.
- [ ] Transactions, repetition and failures are tested for writes.
- [ ] Relevant unit/REST/PostgreSQL tests were run; unverified behavior is stated.
- [ ] The module report and any affected shared decision/schema note were updated.
