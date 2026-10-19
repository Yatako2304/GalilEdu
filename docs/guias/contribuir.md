# Cómo contribuir

## Ramas

| Rama | Para qué | Reglas |
| --- | --- | --- |
| `main` | Lo que se entrega y se despliega | Protegida: solo entra por pull request con CI en verde. |
| `feat/<modulo>-<tema>` | Trabajo nuevo, ej. `feat/matricula-periodos` | Sale de `main` (o de `feat/integracion` mientras esa base no esté integrada). |
| `fix/<tema>` | Correcciones, ej. `fix/login-bloqueo` | Igual que `feat/`. |

Una rama por HU o por grupo pequeño de HU del mismo módulo. Ramas cortas: mejor 2 PR pequeños que 1 gigante.

## Commits

Formato: `tipo(alcance): qué cambió`, en español e imperativo.

| Tipo | Cuándo |
| --- | --- |
| `feat` | Funcionalidad nueva (`feat(matricula): registrar periodo de matrícula`) |
| `fix` | Corrección de un error |
| `test` | Solo pruebas |
| `docs` | Solo documentación |
| `refactor` | Cambia código sin cambiar comportamiento |
| `chore` | Configuración, dependencias, CI |

## Pull request

1. Antes de abrirlo, corre las pruebas en tu PC (ver [pruebas.md](pruebas.md)):
   ```bash
   cd backend && .\mvnw.cmd verify
   cd frontend && npm run lint && npm run check-types && npm test
   ```
2. Abre el PR hacia `main`. La plantilla trae un checklist: complétalo.
3. **GitHub Actions** corre las pruebas de backend y frontend. Si sale en rojo, revisa el log del job que falló, corrige y vuelve a subir: el PR se actualiza solo.
4. Pide revisión a otra pareja. Se integra cuando el CI está en verde y hay al menos una aprobación.

## Qué debe traer cada PR

- El código con sus **pruebas** (las nuevas no reemplazan a las anteriores: deben pasar todas).
- Si cambia la BD: una **migración nueva** `V{n}__...sql`, nunca la edición de una existente.
- La ficha del módulo actualizada en [`docs/modulos/`](../modulos/README.md).
- Si tomaste una decisión que afecta a otros: una entrada en [`docs/decisiones/`](../decisiones/).

## Proteger `main` (una vez, lo hace el dueño del repositorio)

1. GitHub → **Settings → Branches → Add branch ruleset** (o *Add rule*) para `main`.
2. Activa **Require a pull request before merging** y, si quieren, **Require approvals: 1**.
3. Activa **Require status checks to pass** y agrega **`Backend`** y **`Frontend`**.
4. Opcional: **Require branches to be up to date before merging**.
