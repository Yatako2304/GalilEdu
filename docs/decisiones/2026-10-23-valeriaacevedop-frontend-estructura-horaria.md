# Frontend de la estructura horaria (HU-44)

- **Autor:** valeriaacevedop
- **Fecha:** 2026-10-23
- **HU / tarea:** HU-44 (pantalla CI-12 de Figma, 850:10152, y "Modal - Nueva estructura horaria", 850:10657)
- **Estado:** terminado (falta comunicar al grupo las diferencias con Figma; ver Pendientes)

## Qué hice

La pestaña Estructura horaria de la página del año escolar: la lista "Estructuras del año", la tarjeta de la estructura elegida con sus seis datos y su jornada semanal, el modal "Nueva estructura horaria" con la vista previa de la jornada, y Editar y Eliminar. "Nueva", Editar y Eliminar se bloquean con el año no modificable (solo un año PLANIFICADO admite cambios), con el motivo en un tooltip. El backend ya estaba hecho ([decisión](2026-10-22-valeriaacevedop-estructura-horaria.md)).

## Archivos principales

- `frontend/src/features/configuracion/api/estructuras-horarias.ts` y `mutaciones-estructuras.ts`
- `frontend/src/features/configuracion/components/estructura-horaria/` (`estructura-horaria-pestana`, `lista-estructuras`, `tarjeta-estructura`, `grilla-jornada`, `dialogo-estructura`, `vista-previa-jornada`, `dialogo-eliminar-estructura`, `esquemas-estructura`, `formato`)
- `frontend/src/types/api.ts` (bloque "Configuración · Estructura horaria (HU-44)")
- `frontend/src/hooks/use-debounced-value.ts`
- Compartidos: `components/ui/form/input.tsx` y `field-wrapper.tsx`, y `components/secciones/boton-seccion.tsx` (del mismo feature)

## Decisiones y por qué

- **Decisión:** la vista previa del modal es una consulta (`useQuery` con POST), no una mutación, y se pide cuando el usuario hace una pausa de 400 ms (`useDebouncedValue`).
  **Por qué:** no cambia nada en el servidor. Como consulta, la caché guarda la respuesta de cada formulario, una respuesta lenta de un formulario viejo no pisa a la actual, y mientras llega la nueva se sigue viendo la anterior. La pausa evita una petición por cada tecla.
  **Alternativas descartadas:** `useMutation` (la última respuesta en llegar gana, aunque sea de un formulario viejo).

- **Decisión:** la vista previa solo recibe los recesos completos y válidos (después de un bloque entre 1 y bloques-1, de 5 a 60 minutos, sin repetir bloque), y no se pide con hora, bloques o duración inválidos: se sigue viendo la última, atenuada, con "Corrige los datos marcados para actualizar la vista previa.".
  **Por qué:** con un receso a medio llenar el backend responde `jornada: null` y la vista previa desaparecía. Así, agregar una fila vacía no cambia lo que se manda y no recarga nada.
  **Alternativas descartadas:** calcular la jornada en el frontend (duplicaría las reglas del backend). Los horarios posteriores a un receso nuevo los sigue calculando el backend.

- **Decisión:** el modal agrega el selector **Nivel** y la casilla **Estructura principal**, que Figma no dibuja; CI-12 agrega la etiqueta **Principal**.
  **Por qué:** la BD exige el nivel y marca la principal de cada nivel. Sin esos campos no se puede crear una estructura de secundaria ni elegir cuál se usa.

- **Decisión:** Editar y Eliminar (Figma no los dibuja) son botones en la cabecera de la tarjeta, y editar reutiliza el modal con los datos de la estructura: el nivel va bloqueado y la principal no se puede desmarcar (sale marcada y bloqueada).
  **Por qué:** el backend no cambia el nivel y responde 409 al desmarcar la principal; es mejor no ofrecerlo que dejar que falle. Para cambiar de principal se marca otra estructura del nivel.

- **Decisión:** al editar, el aviso "Ya existe una estructura con el código…" de la vista previa se oculta mientras el código no cambie.
  **Por qué:** la vista previa del backend no sabe que se edita y lo dice para el código de la propia estructura. Si se cambia por el de otra, el aviso es cierto y se muestra.

- **Decisión:** el bloqueo de "Nueva", Editar y Eliminar lo decide `anioModificable` del backend, no el estado del año.
  **Por qué:** es la regla del backend (solo un año PLANIFICADO admite cambios) y evita repetirla.

- **Decisión:** `Input type="time"` mantiene la etiqueta arriba; la etiqueta flotante no se parte en dos líneas y, en reposo, se recorta con puntos suspensivos si no cabe.
  **Por qué:** el navegador siempre dibuja "--:--" y la etiqueta bajaría encima; y en campos angostos (tres por fila) una etiqueta larga pisaba el valor.

- **Decisión:** `BotonSeccion` acepta un `motivo` propio y lo usa también Estructura horaria (se importa desde `../secciones`).
  **Por qué:** es el mismo botón bloqueado con tooltip. Queda pendiente subirlo a un lugar compartido del feature (ver Pendientes).

## Pendientes / problemas conocidos

- [ ] Comunicar al grupo las diferencias con Figma: Nivel y Estructura principal en el modal, la etiqueta "Principal", Editar y Eliminar (con su comportamiento) y "Nueva" bloqueado con el año no modificable.
- [ ] La vista previa del backend no recibe el id de la estructura que se edita (avisa "Ya existe…" para su propio código); el frontend lo oculta. Sin parche, la vista previa podría aceptar ese id.
- [ ] La hora de entrada se ve en el formato del navegador (12 o 24 horas, según el sistema); al backend siempre llega `HH:mm`.
- [ ] Subir `BotonSeccion` y el marco de los modales a un lugar compartido; ver "Pendientes de componentes" en la [guía de componentes](../guias/componentes-ui.md).
- [ ] "Bloques por semana" en la vista previa muestra 0 mientras no haya días marcados (el backend lo calcula como bloques por días).

## Notas para quien siga

- Para probar hace falta un año PLANIFICADO. Con los datos de prueba, 2026 es ACTIVO (todo bloqueado) y 2027 es PLANIFICADO y no tiene estructuras.
- En las pruebas, el "min" va dentro de la etiqueta del campo y aparece solo con un valor: busca esos campos con una expresión regular (`/Duración del bloque/`). Los manejadores de MSW registrados después de `mostrar()` son los que responden.
- La vista previa decide si hay con qué calcular y qué manda con la misma copia retrasada del formulario: decidirlo con lo escrito ahora y mandar la copia vieja pedía una vista previa desactualizada.
