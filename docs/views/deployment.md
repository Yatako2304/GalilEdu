# Deployment view

Source: `Galiledu_vista_despliegue.drawio.html`, page `Vista de despliegue`. This is the **target architecture shown in the diagram**, not proof that AWS infrastructure has been provisioned. Local execution uses the application and PostgreSQL; it does not demonstrate EC2, RDS, S3 or SQS availability.

| Node/location | Diagram artifact | Role |
| --- | --- | --- |
| User PC/mobile | Browser with `Bundle SPA React` | Runs the downloaded client and calls the API; the client is outside this backend delivery. |
| AWS `us-east-1`, Ubuntu LTS EC2 | Nginx, `nginx.conf`, TLS certificate and React bundle | Static delivery and HTTPS reverse proxy. |
| Same EC2, JVM | Spring Boot `galiledu.jar` with embedded Tomcat | One modular-monolith backend; the JAR is an artifact, not a server separate from its JVM. |
| Amazon RDS PostgreSQL | Schemas for Persons, Security, Payments, Enrollment, Institutional Configuration, Academics, Attendance and Timetables | Shared persistence; verify physical names against external scripts. |
| Amazon S3 | `Documentos` bucket | Planned file storage. |
| AWS SQS | Payment-notification queue, email queue and dead-letter queue | Planned asynchronous processing. |
| External systems | Payment gateway, Google identity provider, Brevo SMTP | Planned integrations; do not assume credentials, callbacks or contracts are ready. |

| Connector label | Operational meaning |
| --- | --- |
| `HTTPS/TCP`, `REST/HTTPS` | Secure browser/proxy/API communication. REST names the interface style; HTTPS/TCP is transport. |
| `JDBC/TCP` | Backend–PostgreSQL communication; JDBC is the data-access API over a TCP connection. |
| `Webhook HTTPS/TCP (entrada)` | Gateway callback into backend; authenticate/validate it before accepting payment. |
| `HTTPS/TCP crear transacción` | Outbound request to the gateway. |
| `HTTPS/TCP validar token` | Planned Google identity validation. |
| `SMTP/TCP` | External email delivery. |
| `HTTPS/TCP enviar y recibir` / `HTTPS/TCP archivos` | Backend access to SQS/S3 services. |

Connectors show **intended communication**, not working code. Current backend uses PostgreSQL for Users and one Payments read. Gateway/webhook, S3, SQS, Google and SMTP flows are not complete. Local HTTP Basic must not be publicly exposed without TLS.

The delivered HTML shows one S3 `Documentos` bucket and one EC2 with Nginx/JVM. A later outside-Git **proposal** adds an `Audit Logs` bucket and other resilience/encryption choices; it is not declared approved or deployed here. Confirm the final source before changing this description. `us-east-1` names a region, not availability zones A/B.

Implementation consequences: keep one deployable backend artifact; configure database/secrets per environment outside Git; model external services as adapters behind application ports when their HU is implemented; define webhook authenticity, retries, idempotency and reconciliation before recording payments. Production readiness additionally requires verified TLS, secrets, backups, observability and recovery against nonfunctional requirements.
