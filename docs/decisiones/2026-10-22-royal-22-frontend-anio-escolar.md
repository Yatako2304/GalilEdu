# Frontend del año escolar y sus periodos (HU-74)

- **Autor:** royal-22
- **Fecha:** 2026-10-22
- **HU / tarea:** HU-74 (pantallas CI-01, CI-03 y CI-06 de Figma y sus modales)
- **Estado:** terminado (falta "Reutilizar configuración", ver Pendientes)

## Qué hice

Frontend de "Año escolar" para el Personal administrativo: el listado de años con la tarjeta del año en curso (CI-01) y la página de un año (CI-03 y CI-06) con su cabecera, sus acciones y sus pestañas Periodos, Secciones y Estructura horaria. Incluye los modales Nuevo año, Editar año, Cambiar régimen y Editar periodo, y la tarjeta "Antes de activar el año". Las pestañas Secciones (CI-09) y Estructura horaria (CI-12) quedan como componentes provisionales para quien implemente HU-75 y HU-44, con el contrato `{ anioId, estadoAnio }`. No hay cambios en el backend.

## Archivos principales

- `frontend/src/features/configuracion/api/anios-escolares.ts`, `mutaciones-anio.ts`
- `frontend/src/features/configuracion/components/anio-escolar/` (`anios-lista`, `acciones-anio`, `contenido-anio`, `periodos-pestana`, `requisitos-anio`, `dialogo-anio`, `dialogo-editar-periodo`, `esquema-periodo`…)
- `frontend/src/features/configuracion/components/secciones/secciones-pestana.tsx`, `estructura-horaria/estructura-horaria-pestana.tsx`
- `frontend/src/app/routes/app/administrativo/anio-escolar.tsx`, `anio-escolar-detalle.tsx`
- `frontend/src/components/layouts/content-layout.tsx`, `components/ui/progress-bar/`, `components/ui/button/button.tsx`

## Decisiones y por qué

- **Decisión:** se agregó el botón **"Activar año"** a la cabecera de un año PLANIFICADO. No está en Figma, aunque el backend ya tiene `activar` y los requisitos de CI-06 existen para eso. Va como botón principal, a la derecha de "Editar" (secundario), con confirmación ("¿Activar el año escolar {anio}? Pasará a ser el año en curso y ya no podrás editar sus fechas ni su régimen."). Se habilita solo si `requisitos.puedeActivar` y no hay otro año ACTIVO; si está deshabilitado, un tooltip dice por qué, con los textos del backend.
  **Por qué:** sin el botón, el requisito "Antes de activar el año" no llevaba a ninguna acción y un año planificado no se podía activar desde la pantalla.
- **Decisión:** **"Cerrar año"** está en la cabecera de **todas las pestañas** del año ACTIVO, con confirmación, y siempre habilitado: si aún no vence la fecha límite de notas, el 409 del backend se muestra tal cual. Figma solo lo dibuja en CI-12 (y deshabilitado). Un año CERRADO no tiene acciones.
  **Por qué:** la cabecera es común a las tres pestañas; la regla de cuándo se puede cerrar es del backend y así el frontend no la duplica.
- **Decisión:** **sin selector de año** en la cabecera (CI-06 y CI-12 lo dibujan, CI-03 y CI-09 no). Para cambiar de año se vuelve a "Año escolar" desde las migas.
  **Por qué:** el año ya lo define la ruta y las migas, y Figma es inconsistente entre pantallas. Las migas aceptan ahora un tramo con enlace (`{ label, to }`).
- **Decisión:** **"Editar periodo" valida en el frontend**, bajo cada campo, con los demás periodos del detalle y los mismos textos del backend: cruce con otro periodo (bajo "Fecha de fin"), fechas dentro del año, fin posterior al inicio y límite de notas mayor o igual al fin. Si el periodo ya empezó, la fecha de inicio va deshabilitada; si ya terminó, solo se edita el límite. Esto se decide con el `estado` del periodo que manda el backend, no con la fecha del navegador. El texto de ayuda del límite es "Debe ser igual o posterior al fin del periodo.", y no el "Hasta 21 días" de Figma, que el backend no exige. El backend sigue validando como segunda barrera y su error sale como notificación (el interceptor no cambió).
  **Por qué:** el backend responde un `message` general, no un error por campo, así que el error inline de Figma ("Se cruza con el IV Bimestre…") solo se puede dar validando antes de enviar.
- **Decisión:** las **pestañas del año usan `useState`**, como Competencias, con `Tabs variant="pill"` y los contadores "Periodos (n)", "Secciones (n)" y "Estructura horaria (n)". La ruta es una sola: `/app/administrativo/anio-escolar/:anioId`, que abre en Periodos.
  **Por qué:** sigue el patrón del repo y el `roleRoutes` del router es plano. Con el estado en `ContenidoAnio`, el "Revisar" de los requisitos puede cambiar de pestaña. Queda pendiente, si hace falta, enlazar una pestaña concreta por URL.
- **Decisión:** **CI-01 no pide `/actual` si la lista no trae un año ACTIVO.** `GET /anios-escolares/actual` responde 404 sin año activo y el interceptor lo mostraría como error. Tras activar o cerrar un año, las mutaciones recargan primero la lista y descartan la consulta `actual` si ya no hay año activo.
  **Por qué:** evita un toast de error en un caso normal (todos los años planificados o cerrados).
- **Decisión:** la tarjeta "Año escolar en curso" muestra **el plazo de notas que vence primero**, siempre con el nombre del periodo. Entre periodos, el primer dato pasa a "Próximo periodo". Los días ("Faltan 14 días", "Falta 1 día", "Vence hoy") son `diasParaLimiteNotas` del backend, sin cálculos con la fecha del navegador. Solo la marca "Hoy" de la línea de tiempo usa la fecha del navegador, y únicamente con el año ACTIVO.
  **Por qué:** con un plazo ampliado de un periodo ya terminado, es el que le importa a quien configura; y mostrar el nombre evita confundir de qué periodo es la fecha.
- **Decisión:** "Reutilizar configuración" del modal Nuevo año **no se dibuja**.
  **Por qué:** el backend no copia secciones, estructuras ni plan de evaluación. Se anotó en Pendientes.
- **Decisión:** el "Revisar" de cada requisito cambia de pestaña (Periodos, Secciones, Estructura horaria; "Tutores asignados" va a Secciones), y el del plan de evaluación lleva a `/app/administrativo/competencias`.
  **Por qué:** el plan de evaluación se configura en Competencias, no en una pestaña del año.
- **Decisión:** `Button` ahora también se ve deshabilitado con `aria-disabled="true"`, además de `disabled`.
  **Por qué:** un botón realmente `disabled` no recibe el foco, así que un tooltip no salía con el teclado. Con `aria-disabled` el botón sigue enfocable y el tooltip explica por qué "Activar año" no se puede usar.
  **Alternativas descartadas:** envolver el botón en un `span` con `tabIndex` (ESLint lo rechaza por accesibilidad).

## Pendientes / problemas conocidos

- [ ] Reutilizar configuración: backend y luego el bloque del modal Nuevo año.
- [ ] Contenido de las pestañas Secciones (HU-75) y Estructura horaria (HU-44), con el contrato `{ anioId, estadoAnio }`.
- [ ] Revisar con diseño: los tonos de la línea de tiempo usan los tokens existentes (no los hex exactos de Figma) y Figma no dibuja los estados vacíos ni de error de estas pantallas.

## Por reevaluar (iteración 2)

- **Pestañas por URL** (`/anio-escolar/:anioId/secciones`): permitiría compartir un enlace y que "Atrás" cambie de pestaña. Hoy se pierde la pestaña al recargar.
- **Selector de año en la cabecera**, si diseño lo confirma para todas las pestañas.

## Notas para quien siga

- Los datos del año salen de `GET /configuracion/anios-escolares/{id}`; tras cualquier cambio, invalida `['configuracion', 'anios']` (`CLAVE_ANIOS`) para refrescar la cabecera, los contadores de las pestañas y los requisitos.
- Los avisos de error del backend salen como notificación (interceptor). Un 409 no debe duplicarse con un aviso propio.
- En las pruebas, navegar de verdad rompe el router de jsdom (su `Request` no acepta el `AbortSignal` de jsdom): sustituye `useNavigate` y comprueba el destino, como en `boton-nuevo-anio.test.tsx`.
- El selector de fecha etiqueta los días con el día sin cero a la izquierda: `'2/03/2027'`, pero `'10/03/2027'`.
