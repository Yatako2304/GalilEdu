# Architecture diagram notes

These are English interpretations of diagrams supplied outside Git. They explain design intent, **not** completed code. The PO backlog remains authoritative for behavior; the [module board](../modules/README.md) records what exists.

| View | Source examined | Use |
| --- | --- | --- |
| [Components](components.md) | `Diagrama de componentes.drawio.html`, page 1 `Componentes` | Module responsibilities and conceptual interfaces. |
| [Classes](classes.md) | `Diagrama.drawio.html`, page 9 `Vista_general_V2` | Concepts, fields and visible relationships. |
| [Cardinalities](cardinalities.md) | Same page 9 | Explicit multiplicities, including unlabeled edges. |
| [Deployment](deployment.md) | `Galiledu_vista_despliegue.drawio.html`, page `Vista de despliegue` | Target nodes, artifacts, technologies and protocols. |

The original HTML files were received under `C:/Users/ASESORES 4/Downloads/`, outside Git. A later **proposal** also exists at `../../diagramas/GalilEdu_vista_despliegue_propuesta.drawio`, outside this repository; do not treat it as the approved or deployed version without confirmation. Ask the team for originals when exact positions/connectors matter. Read architecture → relevant view → PO HU/RF → current SQL before implementing.
