# GalilEdu — instrucciones para asistentes de IA

Repositorio con `backend/` (Spring Boot + PostgreSQL + Flyway) y `frontend/` (React + Vite). El equipo escribe en español: responde y documenta en español.

## Antes de empezar

1. Lee [`docs/README.md`](docs/README.md): dice qué documento responde qué.
2. Lee la ficha del módulo en [`docs/modulos/`](docs/modulos/README.md) y las entradas de [`docs/decisiones/`](docs/decisiones/). No contradigas una decisión registrada sin decirlo explícitamente.
3. La fuente de verdad del diseño son las pantallas de Figma (archivo `l8nXzicItzBnvPjFQP4G9O`). Si un requisito contradice a Figma, manda Figma.

## Al trabajar

- Sigue [`docs/guias/nuevo-modulo.md`](docs/guias/nuevo-modulo.md) y los nombres de [`docs/guias/convenciones.md`](docs/guias/convenciones.md). Copia los patrones de `usuarios` y `estudiantes`.
- BD: solo migraciones **nuevas** en `backend/src/main/resources/db/migration/`; un esquema por módulo.
- Frontend: arma las pantallas con `src/components/ui` ([catálogo](docs/guias/componentes-ui.md)) y los tokens de Tailwind; no copies estilos de un componente y, si falta uno de Figma, créalo en `components/ui`. Íconos solo de `lucide-react`, según la [tabla de equivalencias](docs/decisiones/2026-10-22-yatako-iconos-lucide.md). Un feature no importa de otro.
- Escribe pruebas para lo que agregues ([`docs/guias/pruebas.md`](docs/guias/pruebas.md)).

## Antes de dar algo por terminado

- `backend/`: `.\mvnw.cmd verify` · `frontend/`: `npm run lint && npm run check-types && npm test`.
- Actualiza la ficha del módulo y, si hubo una decisión importante, propone una entrada en `docs/decisiones/` con `docs/decisiones/_plantilla.md`.
