# Class overview — page 9

Source: `Diagrama.drawio.html`, **page 9 `Vista_general_V2`**. An older `Diagrama.json` labels that position `Page-9`; the later HTML is the source used here. This multi-area diagram is a conceptual model, **not** a one-to-one Java/SQL/REST specification. Exact readable multiplicities are in [cardinalities](cardinalities.md).

The following names intentionally preserve the original Spanish diagram labels, including spelling errors, so contributors can locate them.

| Diagram area | Main classes/values |
| --- | --- |
| Security/Users | `Usuario`, `Rol`, `Permiso`, `ConfigruacionPermisoRol`, `TokenRecuperacionPassword`, `Sesion`, `IdentidadGoogle`, `EstadoUsuario`, `ConfiguracionInstitucionalSeguridad`, `RegistroAuditoria`, `ErrorSistema`, `SeveridadError`. |
| Persons | `Persona`, `Estudiante`, `Docente`, `Apoderado`, `VinculoApoderado`, `TipoDocumento`. |
| Enrollment | `Matrícula`, `DocumentoEntregado`, `EstadoDocumento`, `TipoDocumento`, `TipoMatricula`, `EstadoMatricula`. The document type appears in Persons too; do not duplicate a Java enum before checking whether these are distinct catalogs. |
| Payments | `ConfiguracionPagos`, `Tarifario`, `ParametroMorosidad`, `Descuento`, `DescuentoTramo`, `DescuentoConcepto`, `CompromisoPago`, `Cuota`, `DescuentoAplicado`, `ConceptoPago`, `OrdenPago`, `OrdenPagoDetalle`, `IntentoTransaccion`, `Pago`, `PagoDetalle`. |
| Timetables | `HorarioSeccion`, `AsignacionHoraria`, `Subgrupo`, `MiembroSubgrupo`, `DiaSemana`. |
| Academics | `ItemEvaluacion`, `TipoItemEvaluacion`, `Calificacion`, `EvidenciaPedagogica`, `NotaPeriodoCompetencia`, `NotaFinalCompetencia`. Offering/load and scale configuration appear under Institutional Configuration. |
| Attendance | `JornadaAsistencia`, `DetalleAsistencia`, `AsistenciaDocente`, `Justificacion`, `EstadoAsistencia`, `CondicionAsistencia`. |
| Institutional Configuration | `PlantillaDocumento`, `AnioEscolar`, `PeriodoAcademico`, `Grado`, `Seccion`, `TutorSeccion`, `AreaCurricular`, `Competencia`, `Curso`, `TablaConversion`, `RangoConversion`, `OfertaCurso`, `ConfiguracionCompetencia`, `CargaAcademica`, `AsignacionCoordinacion`, `EspacioFisico`, `EstructuraHoraria`, `BloqueHorario`, `Receso`, and their status/scale/rule enums. |

## Contract-relevant fields visible in the diagram

| Class | Visible fields / caution |
| --- | --- |
| `Persona` | `id`, `tipoDocumento`, `numeroDocumento`, `nombres`, `primerApellido`, `segundoApellido`, `correo`, `telefono`. Current request DTO uses `correoElectronico`; adapter maps it. |
| `Usuario` | `id`, `username`, `passwordHash`, `estado`, `intentosFallidos`, `bloqueadoHasta`. Email belongs to Person, not Account. |
| `Estudiante` | `id`, `direccion`, `contactoEmergencia`, `informacionMedica`; no grade field or direct `Estudiante`–`Grado` link is shown on page 9. In an enrollment, grade is reached through its `Seccion`. |
| `Matrícula` | `id`, `estudiante`, `codigoMatricula`, `fechaRegistro`, `fechaConfirmacion`, `tipo`, `estado`, `motivoExcepcion`. |
| `OrdenPago` / `Pago` | Order has code, total, state and gateway transaction ID. Payment has receipt number, principal/surcharge/total, date, state and observation. An order is **not** a confirmed payment. |
| `AsignacionHoraria` / `CargaAcademica` | Assignment has ID, session type and day; load has ID, section ID, teacher ID and active flag. Relations supply further context. |
| `Calificacion` | Numeric or qualitative value and registration/modification dates; scale exclusivity needs backlog/SQL checks. |
| `JornadaAsistencia` / `DetalleAsistencia` | Session date/registration date; detail state, condition, marking time and observation. |
| `AnioEscolar` / `PeriodoAcademico` | School-year dates/regime/state and period name/order/dates/grade deadline/state. **These are not automatically an enrollment window.** |

Visible associations include Account–Person/Role, Guardian–GuardianLink, Enrollment–Student, PaymentOrder–Guardian, TransactionAttempt–Order, SectionSchedule–Assignment, Assignment–AcademicLoad/Period, AcademicLoad–AssessmentItem, Item–Grade and SchoolYear–AcademicPeriod. Draw.io arrow direction is **not** interpreted as code dependency or cardinality. Some endpoints in the XML are auxiliary labels and cannot be reconstructed reliably; check the rendered page, [cardinality notes](cardinalities.md), HU/RF and SQL for a specific relationship.

Translation rule: **diagram class ≠ table ≠ Java class**. A box may map to a domain entity, value object, DTO, table or several objects. `+`/`-` UML visibility does not justify public mutable Java fields. Many-to-many relationships may need join tables; the existing `seguridad.usuario_rol` is one example. Do not generate a CRUD for every box.

Open cautions: `ConfigruacionPermisoRol` is misspelled in the source; `TipoDocumento`/`DiaSemana` appear more than once; `Usuario`–`DescuentoAplicado` has unclear semantics; `Sesion` does not mandate a session table; `RegistroAuditoria` and `IdentidadGoogle` boxes do not prove those integrations work.
