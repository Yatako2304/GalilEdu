# docs — Bitácora del equipo

Aquí cada integrante registra **lo que hizo, qué decidió y por qué**.
El objetivo es que cualquier persona (o una IA) pueda leer esta carpeta y entender el contexto del proyecto antes de tocar código, sin tener que preguntar.

> El [README principal](../README.md) explica **cómo** está armado el proyecto.
> Esta carpeta explica **por qué** está así y **qué pasó** en el camino.

## Estructura

```
docs/
├── README.md        ← este archivo (reglas)
├── _plantilla.md    ← copia esto para cada entrada nueva
└── registro/        ← una entrada por tarea/decisión
    └── AAAA-MM-DD-autor-tema.md
```

## Cómo agregar una entrada

1. Copia `_plantilla.md` dentro de `registro/`.
2. Nómbrala `AAAA-MM-DD-autor-tema.md`, en minúsculas y con guiones.
   Ejemplo: `2026-10-18-yatako-modulo-usuarios.md`.
3. Llénala **al terminar** la tarea (o al tomar una decisión importante), y súbela en el mismo commit/PR que el código.

## Reglas

- **Una entrada por tarea o decisión.** No edites las entradas de otros; si algo cambió, crea una nueva que diga "reemplaza a `<archivo>`".
- **Explica el porqué**, no solo el qué. El código ya dice qué se hizo; lo valioso es la razón y las alternativas que se descartaron.
- **Sé concreto:** nombra archivos (`src/features/usuarios/...`), librerías y versiones.
- **Corto es mejor.** Si no tienes nada que poner en una sección, bórrala.
- **Nada de secretos:** no pongas contraseñas reales, tokens ni datos personales.

## Para usarlo con una IA

Al empezar una sesión, pídele algo como:

> Lee `README.md` y todos los archivos de `docs/registro/` antes de empezar. Luego ayúdame con …

Muchas herramientas (Claude Code, Cursor, Copilot, Codex…) leen automáticamente [`AGENTS.md`](../AGENTS.md) (o `CLAUDE.md`, que lo importa) en la raíz, que ya les indica todo esto. Si tu IA no lo hace, pégale el mensaje de arriba.
