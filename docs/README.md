# Documentación de GalilEdu

Toda la documentación del proyecto vive aquí, **en español** y en Markdown. Esta página dice qué va en cada lugar, para que nadie tenga que preguntar dónde buscar o dónde escribir.

## ¿Qué va dónde?

| Archivo / carpeta | Responde a… | Quién lo actualiza y cuándo |
| --- | --- | --- |
| [`/README.md`](../README.md) | ¿Qué es GalilEdu y cómo lo levanto? | Solo si cambia cómo se instala o se ejecuta. |
| [`/backend/README.md`](../backend/README.md) | ¿Cómo está armado el backend? Configuración, estructura, lista de endpoints. | Quien agrega endpoints o variables de entorno. |
| [`/frontend/README.md`](../frontend/README.md) | ¿Cómo está armado el frontend? Estructura, design system, scripts. | Quien cambia la estructura o agrega librerías. |
| [`guias/`](guias/) | ¿**Cómo** hago algo? (contribuir, crear un módulo, escribir pruebas) | Cuando cambia el proceso del equipo. |
| [`modulos/`](modulos/README.md) | ¿**Qué** hace cada módulo y en qué estado está? (HU, pantallas, tablas, endpoints) | Quien trabaja en el módulo, en cada PR que lo cambie. |
| [`decisiones/`](decisiones/) | ¿**Por qué** se hizo así? (decisiones y alternativas descartadas) | Quien toma una decisión que afecta a otros. |
| [`/AGENTS.md`](../AGENTS.md) | Instrucciones para asistentes de IA (Claude, Copilot, Cursor…). | Cuando cambian las reglas del equipo. |
| [`/.github/pull_request_template.md`](../.github/pull_request_template.md) | Checklist que aparece al abrir un PR. | Cuando cambian los requisitos para integrar. |

> Regla rápida: **README = cómo usar** · **guías = cómo trabajar** · **módulos = qué hay** · **decisiones = por qué**.

## Guías

- [Cómo contribuir](guias/contribuir.md): ramas, commits, pull requests, CI y revisión.
- [Cómo crear un módulo](guias/nuevo-modulo.md): BD → backend → frontend → pruebas → documentación, paso a paso.
- [Pruebas](guias/pruebas.md): qué se prueba, cómo correrlas y cómo escribir las tuyas.
- [Convenciones de código](guias/convenciones.md): cómo nombrar tablas, clases, endpoints, componentes y variables.
- [Componentes de interfaz](guias/componentes-ui.md): qué componente de `components/ui` corresponde a cada pieza de Figma y qué hacer si falta uno.
- [Despliegue en AWS](guias/despliegue.md): servidor EC2 con Docker y despliegue automático en cada merge a `main`.

## Reglas de escritura

1. **Español**, frases cortas, sin relleno. Si no tienes nada que poner en una sección, bórrala.
2. **Enlaza, no copies.** Si algo ya está explicado en otro archivo, pon el enlace. La información duplicada se desactualiza.
3. **Nombra archivos y rutas concretas** (`backend/src/.../ServicioUsuarios.java`, `V3__matricula.sql`).
4. **La documentación va en el mismo PR que el código.** Un PR que cambia un módulo y no actualiza su `modulos/<modulo>.md` no está terminado.
5. **Nada de secretos:** ni contraseñas reales, ni tokens, ni datos personales reales.
6. **Fuente de verdad del diseño:** las pantallas de [Figma](https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu). Si Figma y un requisito de Notion se contradicen, manda Figma; anótalo en la ficha del módulo.
