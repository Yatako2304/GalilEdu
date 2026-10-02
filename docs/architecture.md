# GalilEdu backend architecture

Last checked: **2026-10-02** against backend commit `d52e0fc` on `main`. This document records agreed technical decisions, not completion of every user story. The [module board](modules/README.md) is the single source for implementation status; the [diagram notes](views/README.md) describe the design views.

## Delivery scope and sources of truth

- This delivery covers the **Spring Boot backend, its REST API, and PostgreSQL integration only**. A React client exists in the repository but is not part of the current work or acceptance checks.
- The Product Owner (PO)-validated backlog determines functional behavior. It is shared outside Git at `../backlog/` relative to the `GalilEdu/` repository root. A story marked complete in that backlog is validated as a requirement, not necessarily implemented.
- The database team owns the PostgreSQL scripts, shared outside Git at `../scripts/`. Diagram classes are not table definitions. Do not invent a table, column, or business rule when a source is missing or contradictory; record the question and obtain the responsible decision.
- The original diagrams are outside Git. [Text descriptions of the views](views/README.md) are useful orientation, not evidence that a connector or integration is implemented.

## Architecture decisions

GalilEdu is a **modular monolith**: one Spring Boot 4.1.x process, Java 21, and one `galiledu.jar` artifact. Modules are functional boundaries inside that process, not microservices. PostgreSQL 18 is the target database; the local installation currently uses port 5433, configurable per environment.

Each module follows a pragmatic **hexagonal architecture**, organized by module first and responsibility second:

```text
REST client → infrastructure/web → application → domain
                                   application → port ← infrastructure/persistence
                                                      ← other adapters
```

| Package inside a module | Responsibility |
| --- | --- |
| `dominio` | Business concepts, state and invariants; no Spring, HTTP or SQL. |
| `aplicacion` | Use cases, orchestration and transaction boundaries. |
| `aplicacion/puertos` | Interfaces describing capabilities required by use cases. |
| `infraestructura/web` | REST controllers, request/response DTOs and HTTP error mapping. |
| `infraestructura/persistencia` | PostgreSQL/JDBC adapters and parameterized SQL. |
| `infraestructura/seguridad` and other adapters | Authentication or external integrations when required by a story. |

Dependencies point inward: infrastructure knows application/domain; domain does not know infrastructure. A controller invokes a use case, never another module's JDBC adapter. Cross-module work uses an agreed contract owned by the provider module. Do not create a controller, service, repository or separate implementation **per role**: the operation is shared and authorization decides who may call it.

`Repository`/DAO is one pattern here: a port plus its adapter, not two parallel persistence layers. Immutable DTOs/value objects may be Java `record`s; identity-bearing domain objects encapsulate state instead of exposing mutable public fields. HTTP DTOs are not domain entities.

This is a pragmatic implementation: `GestionUsuarios` uses Spring `@Service` and `@Transactional` in the application package, so that package is not fully framework-independent. The domain is plain Java. The Maven file includes Spring Data JPA, but implemented database adapters currently use `JdbcOperations`; there are no production JPA entities or `JpaRepository` implementations. `ddl-auto=validate` is not a substitute for schema verification against PostgreSQL.

## Current code layout

```text
backend/src/main/java/com/galiledu/
├── usuarios/{dominio,aplicacion/puertos,infraestructura/{web,persistencia,seguridad}}/
├── pagos/{dominio,aplicacion,infraestructura/persistencia}/
├── horarios/{dominio,aplicacion}/
├── matricula/{dominio,aplicacion}/
├── academica/{dominio,aplicacion}/
├── asistencia/{dominio,aplicacion}/
└── infraestructura/web/                 # shared HTTP configuration
```

Create packages when a story needs real code, not empty placeholders. The institutional-configuration area appears in the design but does not yet have a complete backend module. Tests mirror production packages under `backend/src/test/java/com/galiledu/`.

## Agreed user/security decisions

- A person owns identity and email; `seguridad.usuario` is the login account. One account may have multiple roles. Student, teacher and guardian are person profiles/relationships, not a one-role-per-account constraint. One student may have several guardians.
- There is **no mandatory password change at first login** in the current scope. Voluntary self-service password change exists.
- Current REST security allows active accounts to log in and change their own password. Administrative person/account routes require `ADMINISTRADOR`. The database permission matrix is not yet enforced by those routes; see [Users status](modules/users.md).
- `GET /api/auth/csrf` and health checks are public. `POST /api/auth/login` is public but requires CSRF; it creates an HTTP session. Five failed attempts trigger a 15-minute lock, with remaining seconds returned in a `401` response. State-changing requests require CSRF. HTTP Basic is also available locally and must not be exposed without TLS.
- Passwords are stored as bcrypt hashes. Registration currently returns the initial password once with `Cache-Control: no-store`; email delivery required by RF-17 is **not implemented**. This is a delivery limitation, not an approved final credential-distribution flow.

See the [development guide](development-guide.md) for implementation standards and the [database compatibility notes](database-compatibility.md) for confirmed schema gaps. Keep implementation status in [modules/README.md](modules/README.md), not in a second table here.
