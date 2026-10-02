# Class-diagram cardinalities

Source: `Diagrama.drawio.html`, page 9 `Vista_general_V2`. Of 88 identifiable class-to-class connectors in the Draw.io XML, **60 have labels at both ends**, **2 at one end**, and **26 at neither end**. No missing multiplicity is inferred. Original Spanish class names are kept verbatim.

In `A | A per B | B per A | B`, the label beside **A** says how many A instances one B may have; the label beside **B** says how many B instances one A may have. For example, `Persona | 1 | 0..1 | Estudiante` means every Student has one Person and one Person may have zero or one Student profile. Diagram `0...*`/`1...*` is normalized to `0..*`/`1..*`; bare `*` remains as drawn. These are **diagram labels**, not verified PostgreSQL constraints or independently approved business rules.

## Users, security and persons

| A | A per B | B per A | B |
| --- | ---: | ---: | --- |
| `ConfigruacionPermisoRol` | `0..*` | `1` | `Permiso` |
| `Rol` | `1` | `0..*` | `ConfigruacionPermisoRol` |
| `Usuario` | `1` | `1` | `Persona` |
| `Usuario` | `0..*` | `1..*` | `Rol` |
| `RegistroAuditoria` | `0..*` | `0..1` | `Usuario` |
| `Persona` | `1` | `0..1` | `Apoderado` |
| `Persona` | `1` | `0..1` | `Estudiante` |
| `Persona` | `1` | `0..1` | `Docente` |
| `Apoderado` | `1` | `0..*` | `VinculoApoderado` |
| `Estudiante` | `1` | `1..*` | `VinculoApoderado` |

Multiple roles per account and multiple guardian links per student match current decisions, but lifecycle and physical tables still require backlog/SQL verification.

## Payments

| A | A per B | B per A | B |
| --- | ---: | ---: | --- |
| `CompromisoPago` | `0..*` | `1` | `Estudiante` |
| `ParametroMorosidad` | `1` | `1` | `Tarifario` |
| `OrdenPago` | `0..*` | `1` | `Apoderado` |
| `DescuentoConcepto` | `1..*` | `1` | `Descuento` |
| `ConceptoPago` | `1` | `0..*` | `Cuota` |
| `ConceptoPago` | `1` | `0..*` | `DescuentoConcepto` |
| `DescuentoTramo` | `0..*` | `1` | `Descuento` |
| `OrdenPagoDetalle` | `1..*` | `1` | `OrdenPago` |
| `IntentoTransaccion` | `0..*` | `1` | `OrdenPago` |
| `Pago` | `0..1` | `1` | `OrdenPago` |

The Payment–Order link does not authorize creating a payment on button click; a validated, duplicate-safe confirmation flow is still missing.

## Enrollment and cross-module references

| A | A per B | B per A | B |
| --- | ---: | ---: | --- |
| `Matrícula` | `0..*` | `1` | `Estudiante` |
| `Matrícula` | `0..*` | `1` | `AnioEscolar` |
| `Matrícula` | `0..*` | `1` | `Seccion` |
| `Matrícula` | `1` | `0..*` | `DetalleAsistencia` |
| `DocumentoEntregado` | `0..*` | `1` | `PlantillaDocumento` |
| `MiembroSubgrupo` | `0..*` | `1` | `Matrícula` |
| `Calificacion` | `0..*` | `1` | `Matrícula` |
| `NotaPeriodoCompetencia` | `0..*` | `1` | `Matrícula` |
| `NotaFinalCompetencia` | `0..*` | `1` | `Matrícula` |
| `PlantillaDocumento` | `0..*` | `1` | `AnioEscolar` |

## Timetables

| A | A per B | B per A | B |
| --- | ---: | ---: | --- |
| `HorarioSeccion` | `1` | `0..*` | `AsignacionHoraria` |
| `AsignacionHoraria` | `0..*` | `1` | `CargaAcademica` |
| `AsignacionHoraria` | `0..*` | `0..*` | `PeriodoAcademico` |
| `AsignacionHoraria` | `0..*` | `1` | `BloqueHorario` |
| `Subgrupo` | `0..1` | `0..*` | `AsignacionHoraria` |
| `Subgrupo` | `1` | `1..*` | `MiembroSubgrupo` |

The Assignment–Period many-to-many label is transcribed literally. Check HU and schema before creating a join table.

## Academics and attendance

| A | A per B | B per A | B |
| --- | ---: | ---: | --- |
| `CargaAcademica` | `1` | `0..*` | `ItemEvaluacion` |
| `ItemEvaluacion` | `1` | `0..*` | `Calificacion` |
| `Calificacion` | `1` | `0..1` | `EvidenciaPedagogica` |
| `NotaFinalCompetencia` | `1` | `1..*` | `NotaPeriodoCompetencia` |
| `AsistenciaDocente` | `0..*` | `1` | `Docente` |
| `Justificacion` | `0..*` | `1` | `Usuario` |

`JornadaAsistencia`–`DetalleAsistencia` and `DetalleAsistencia`–`Justificacion` have connectors but no multiplicity labels; do not assign one here.

## Institutional Configuration

| A | A per B | B per A | B |
| --- | ---: | ---: | --- |
| `AnioEscolar` | `1` | `1..*` | `PeriodoAcademico` |
| `Grado` | `1` | `0..*` | `Seccion` |
| `Seccion` | `1` | `0..*` | `TutorSeccion` |
| `AreaCurricular` | `1` | `0..*` | `Competencia` |
| `AreaCurricular` | `1` | `0..*` | `Curso` |
| `TablaConversion` | `1` | `4` | `RangoConversion` |
| `OfertaCurso` | `1` | `1..*` | `ConfiguracionCompetencia` |
| `OfertaCurso` | `1` | `0..*` | `CargaAcademica` |
| `OfertaCurso` | `1` | `0..*` | `AsignacionCoordinacion` |
| `EstructuraHoraria` | `1` | `1..*` | `BloqueHorario` |
| `EstructuraHoraria` | `1` | `0..*` | `Receso` |
| `Seccion` | `*` | `1` | `AnioEscolar` |
| `OfertaCurso` | `1..*` | `1` | `AnioEscolar` |
| `OfertaCurso` | `*` | `1` | `Grado` |
| `OfertaCurso` | `*` | `1` | `Curso` |
| `ConfiguracionCompetencia` | `*` | `1` | `Competencia` |
| `CargaAcademica` | `*` | `1` | `Seccion` |
| `EstructuraHoraria` | `0..1` | `1` | `AnioEscolar` |

## Incomplete or unlabeled connectors

`Subgrupo`–`Seccion` has `1` beside `Seccion` only. `Usuario`–`DescuentoAplicado` has `0..1` beside `Usuario` only; its business meaning itself needs clarification.

The other **26 identifiable connectors have no multiplicity at either end**: `ErrorSistema`–`SeveridadError`, `EstadoUsuario`–`Usuario`, `ConfiguracionPagos`–`Descuento`, `DocumentoEntregado`–`Matrícula`, `DocumentoEntregado`–`EstadoDocumento`, `DiaSemana`–`AsignacionHoraria`, `Persona`–`TipoDocumento`, `DetalleAsistencia`–`EstadoAsistencia`, `DetalleAsistencia`–`CondicionAsistencia`, `JornadaAsistencia`–`DetalleAsistencia`, `DetalleAsistencia`–`Justificacion`, `ConfiguracionInstitucionalSeguridad`–`IdentidadGoogle`, `ItemEvaluacion`–`TipoItemEvaluacion`, `PlantillaDocumento`–`TipoDocumento`, `AnioEscolar`–`EstadoAnio`, `PeriodoAcademico`–`EstadoPeriodo`, `Grado`–`NivelEducativo`, `Seccion`–`EstadoSeccion`, `TutorSeccion`–`TipoTutoria`, `RangoConversion`–`ValorCualitativo`, `ConfiguracionCompetencia`–`ReglaConsolidacionPeriodos`, `EspacioFisico`–`TipoEspacio`, `EstructuraHoraria`–`DiaSemana`, `ConfiguracionCompetencia`–`ReglaAgregacionItems`, `ConfiguracionCompetencia`–`TipoEscala`, and `AnioEscolar`–`RegimenPeriodos`. Some point to enums and may be type references rather than persistent associations.

Before designing FKs, nullability, uniqueness or join tables, check the current rendered diagram, PO requirements and database-team scripts. Record disagreements; do not alter SQL silently to fit a drawing.
