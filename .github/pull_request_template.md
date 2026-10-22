## Qué cambia

<!-- 1–3 líneas. Qué HU cubre y qué hace ahora el sistema que antes no hacía. -->

- **HU:** HU-xx
- **Módulo:** <usuarios, matricula…>
- **Pantallas de Figma:** <frame(s)>

## Cómo probarlo

<!-- Pasos para que quien revise lo vea funcionar: usuario de prueba, pantalla, qué hacer, qué debe pasar. -->
1.

## Checklist

- [ ] Sigue las pantallas de Figma (textos, campos, mensajes).
- [ ] Las pantallas usan los componentes de `frontend/src/components/ui` (`docs/guias/componentes-ui.md`); no copié sus estilos y lo que faltaba lo creé ahí, no en el módulo.
- [ ] Los nombres siguen `docs/guias/convenciones.md` (español, sin tildes en el código, mismo nombre en BD/Java/TS).
- [ ] Pruebas nuevas para lo que agregué, y **todas** las pruebas pasan en mi PC (`mvnw verify`, `npm run lint && npm run check-types && npm test`).
- [ ] Si cambia la BD: migración **nueva** `V{n}__...sql` (no edité una existente).
- [ ] Actualicé la ficha del módulo en `docs/modulos/` (y el índice si cambió el estado).
- [ ] Si agregué endpoints: actualicé la tabla de `backend/README.md`.
- [ ] Si tomé una decisión que afecta a otros: entrada en `docs/decisiones/`.
- [ ] Sin contraseñas, tokens ni datos personales reales.
