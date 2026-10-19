# GalilEdu frontend — instrucciones para asistentes de IA

Antes de empezar cualquier tarea:

1. Lee `README.md` (instalación, estructura, reglas de ESLint, cómo crear un módulo).
2. Lee `docs/README.md` y las entradas de `docs/registro/` para conocer las decisiones previas del equipo y sus razones. No contradigas una decisión registrada sin mencionarlo explícitamente.

Al trabajar:

- Usa `src/features/horarios` como plantilla para módulos nuevos y respeta las reglas de arquitectura del README (un feature no importa de otro).
- Usa los componentes de `src/components/ui` y los tokens de Tailwind del design system; no inventes colores ni tamaños.
- Antes de dar algo por terminado: `npm run lint && npm run check-types && npm test`.

Al terminar una tarea con cambios relevantes (módulo nuevo, librería nueva, cambio de arquitectura, decisión importante), propón crear una entrada en `docs/registro/` usando `docs/_plantilla.md`.

El equipo escribe en español; responde y documenta en español.
