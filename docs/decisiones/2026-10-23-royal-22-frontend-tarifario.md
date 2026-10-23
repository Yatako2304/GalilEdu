# Frontend del tarifario y calendario (HU-34)

- **Autor:** royal-22
- **Fecha:** 2026-10-23
- **HU / tarea:** HU-34 (pantalla P19 de Figma: "Tarifario y calendario" 842:9352 y el modal "¿Confirmar el tarifario?" 1090:26770)
- **Estado:** terminado (el catálogo de descuentos y la UX de P19 quedan para la iteración 2)

## Qué hice

Frontend de "Configuración de pensiones › Tarifario y calendario" para el Personal administrativo: elegir un año, editar los montos y la política de morosidad, ver el calendario de cuotas que genera el backend y confirmar el tarifario con el modal de Figma. Todo vive en `features/configuracion`, con el mismo patrón de HU-74 y HU-23. No hay cambios en el backend. Reemplaza en parte la [decisión del frontend de HU-23](2026-10-23-royal-22-frontend-periodo-matricula.md): el enlace al tarifario ya no está deshabilitado.

## Archivos principales

- `frontend/src/features/configuracion/api/tarifarios.ts`, `mutaciones-tarifario.ts`
- `frontend/src/features/configuracion/components/tarifario/` (`configuracion-pensiones`, `formulario-tarifario`, `conceptos-montos`, `politica-morosidad`, `calendario-cuotas`, `dialogo-confirmar-tarifario`, `dialogo-cambios-sin-confirmar`, `esquemas.ts`, `formato.ts`…)
- `frontend/src/app/routes/app/administrativo/pensiones-configuracion.tsx`, `pensiones.tsx` (el botón "Configuración" de P4), `frontend/src/app/router.tsx`, `frontend/src/config/paths.ts`
- Compartidos: `components/ui/form/input.tsx` (`startAdornment`)

## Decisiones y por qué

- **Decisión:** el tarifario muestra el estado **"Confirmado"** (verde) además de "En configuración" (ámbar), que es el único que Figma dibuja. "En configuración" sale tanto cuando el año no tiene tarifario (el backend devuelve una propuesta, `existe = false`) como cuando está guardado sin confirmar.
  **Por qué:** con un tarifario confirmado la pantalla no puede seguir diciendo que está en configuración.
- **Decisión:** **solo lectura** como la calcula el backend (año CERRADO o tarifario CONFIRMADO con compromisos de pago): campos deshabilitados, sin botón de confirmar y con un aviso con el `motivoSoloLectura` tal cual. Si `calendarioDesactualizado` es verdadero se avisa, con un texto según se pueda confirmar o no: "Las fechas del año escolar cambiaron después de confirmar este tarifario. Vuelve a confirmarlo para actualizar el calendario." o "Las fechas del año escolar ya no coinciden con este calendario, pero no se puede regenerar porque ya hay compromisos de pago."
  **Por qué:** las reglas usan los datos del backend, nunca el reloj del navegador.
- **Decisión:** los textos del calendario se **generan con el calendario del backend**: la cantidad y los meses ("10 cuotas (marzo–diciembre)") y la excepción del vencimiento ("Último día de cada mes · diciembre: 15/12") salen de las cuotas, no del texto fijo de Figma. La cantidad de cuotas del calendario y del modal es la que manda el backend; el monto de cada cuota y el total siguen a la pensión que se escribe.
  **Por qué:** el calendario depende de las fechas del año y puede tener otra cantidad de meses.
- **Decisión:** el año elegido va en la URL con **`?anioEscolarId=`**. Sin ese parámetro abre el año PLANIFICADO más reciente, luego el ACTIVO y, si todos están cerrados, el más reciente. Cambiar de año en el selector actualiza la URL (`replace`). El enlace "Ir al tarifario" y "Ver tarifario completo" de la Pantalla 13 llevan el año del formulario.
  **Por qué:** se puede compartir o recargar la pantalla en un año concreto, y desde un periodo se llega al tarifario que le corresponde.
- **Decisión:** **guarda de cambios sin confirmar**, porque no hay "Guardar" y lo escrito se pierde: `useBlocker` al salir de la pantalla ("Volver", el menú, el botón atrás), `beforeunload` al cerrar o recargar la pestaña y un aviso al cambiar de año en el selector. Solo se activa mientras el formulario difiere de lo guardado; confirmar, o volver a los valores guardados, la quita.
  **Por qué:** Figma no tiene "Guardar" y un clic accidental en "Volver" borraría el trabajo. Los textos de los avisos (y el del toast "Tarifario confirmado") los redacté yo y están en la descripción del PR.
- **Decisión:** el formulario **valida todos los campos** con los mismos mensajes del backend y los muestra bajo cada campo a la vez; el backend devuelve solo el primer error. El 400 y el 409 del backend siguen mostrándose tal cual como segunda barrera.
  **Por qué:** quien llena el formulario corrige todo de una vez.
- **Decisión:** **"Confirmar tarifario"** no tiene "Guardar" (así está en Figma) y se muestra el resumen del modal antes de enviar; el botón se llama igual también con un tarifario ya confirmado. La cuota de ingreso vacía se envía como `null` y se muestra como "—" en el modal. Tras confirmar, la pantalla se queda en el mismo lugar con lo guardado y la etiqueta "Confirmado".
  **Por qué:** sigue Figma; la UX por reevaluar queda anotada abajo.
- **Decisión:** la pestaña **"Catálogo de descuentos" está deshabilitada** ("Disponible pronto").
  **Por qué:** es la HU-35, de Pagos, y no está asignada a esta pantalla.
- **Decisión:** el botón **"Configuración" de la pantalla de Pensiones** (archivo de P4) pasó de deshabilitado a un enlace a P19; se registró `/pensiones/configuracion` sin afectar a `/pensiones/:estudianteId` (hay una prueba con el router de la app).
  **Por qué:** las rutas estáticas tienen prioridad sobre las dinámicas, y era lo pendiente de HU-23 para esta pantalla.

## Librerías o herramientas nuevas

Ninguna.

## Pendientes / problemas conocidos

- [ ] Catálogo de descuentos (HU-35): habilitar la pestaña de P19.
- [ ] UX de P19 por reevaluar (iteración 2): el texto "Guardar cambios" para un tarifario ya confirmado y un borrador con autoguardado.
- [ ] Pagos: usar `ServicioTarifario.vigente` y `calendario` y exigir el tarifario CONFIRMADO en la cuota de matrícula (ver la [decisión del backend](2026-10-22-royal-22-tarifario-datos-maestros.md)).

## Notas para quien siga

- Para probar una navegación o un cambio de la URL en jsdom con react-router, no se navega de verdad: se sustituyen `useNavigate` o `useSearchParams` con `vi.mock('react-router')`. Con las rutas de la app (que cargan con `lazy`) hay además un `AbortSignal` de jsdom que el `Request` de Node no acepta; mira cómo lo resuelve `app/__tests__/rutas-pensiones.test.tsx`.
- `createAppRouter(queryClient, crear)` acepta la función que crea el router (por defecto, el del navegador) para poder probar las rutas con uno en memoria.
- `Input` con `startAdornment` es decorativo (`aria-hidden`): si la moneda importa para un lector de pantalla, ponla en el nombre accesible, como "Monto de la matrícula, en soles".
