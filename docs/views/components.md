# Component view

Source: `Diagrama de componentes.drawio.html`, page 1 `Componentes`. The drawing distinguishes **33 functional components in seven areas**. Interface labels are conceptual collaboration contracts; they do not prove that a matching Java interface, endpoint or microservice exists. See [actual backend status](../modules/README.md).

| Area / intended package | Responsibilities shown |
| --- | --- |
| Timetables / `horarios` | **Schedule Assignment:** section, teacher, course, room and block. **Overlap Validation:** teacher/room conflicts before saving. **Subgroup Management:** subgroups and members. **Schedule Query:** weekly teacher view and a guardian's child's schedule. |
| Academics / `academica` | **Academic Load:** teacher/course/section/period/competency links. **Assessment Criteria:** items per competency. **Grade Consolidation:** period/final results and conversion. **Grade Entry:** item/evidence entry or correction while the period is open. **Grade Queries/Reports:** authorized queries, exports and institutional reports. |
| Attendance / `asistencia` | Monthly reports; daily attendance entry and same-school-day correction; justifications and status; teacher check-in/check-out. |
| Users / `usuarios` | Roles/permissions; person data, profiles and guardian–student links; queryable audit; error register; account creation/changes/roles/deactivation/reactivation/bulk load; local/Google authentication and password recovery/change. |
| Enrollment / `matricula` | Enrollment windows; section capacity/assignment; document templates/submissions; debt/payment/confirmation/code/late enrollment; guardian notifications. |
| Payments / `pagos` | Discount catalog; fees, calendar and late charges; obligations/installments/debt; payment requests, results and receipts; external gateway integration without duplicate transactions. |
| Institutional Configuration / not yet implemented | School structure, school calendar, annual offerings/assignments and curriculum/conversion catalog. Present in design, not current backend scope. |

## Interpreting connectors

- A **provided** interface is a capability an area offers; a **required** interface is one it needs. Circles, sockets and dependency lines require checking the actual connector, not spatial proximity, before creating a port.
- The web application is a client of backend capabilities. Internal modules remain inside one `galiledu.jar`; a connector does not turn them into independent services.
- Authorization/persons, configuration, enrollment and payments appear as cross-cutting dependencies. Gateway, Google and email appear as external services. **Drawing a connection is not implementation evidence.**
- Audit appears in Users and in some labels of other areas. Earlier design discussion prioritized Users, Payments and Grades; the drawing still includes some `IAuditoria` references near Enrollment and Attendance. Reconcile them with the PO backlog and final diagram before adding audit events. Do not infer them from a line alone.

## HU labels visible on this page

| Area | HU labels | Caution |
| --- | --- | --- |
| Timetables | 45, 46, 49, 51 | Not necessarily the full area backlog. |
| Academics | 57, 59–64, 69, 70 | Verify ownership and authorization of queries. |
| Attendance | 65–68 | Check any audit mention against the backlog. |
| Users | 1–13, 16, 17, 19, 20 | Only part of this area is coded. |
| Enrollment | 23–33, 71, 72 | The full flow is not coded. |
| Payments and Institutional Configuration | No equivalent HU label here | Consult the backlog; absence of a label does not mean absence of requirements. |

For each implementation, link the actual HU/RF, owning component, use case, ports and tables in review. This view alone does not authorize new routes/classes.
