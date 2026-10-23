# Frontend del periodo de matrícula (HU-23)

- **Autor:** royal-22
- **Fecha:** 2026-10-23
- **HU / tarea:** HU-23 (pantalla 13 de Figma: listado 751:38508, 771:42212 y 772:43484; modal 823:5986; Nuevo periodo 749:4406, 763:41416 y 771:41821; Ver 772:43711)
- **Estado:** terminado (los enlaces a otras pantallas esperan a que existan, ver Pendientes)

## Qué hice

Frontend de "Periodos de matrícula" para el Personal administrativo: el listado con la tarjeta "Vigente" y su modal de configuración, y las páginas Nuevo periodo, Ver y Editar, con sus plantillas de documentos, y la opción Eliminar. Todo vive en `features/configuracion`, con el mismo patrón de HU-74. No hay cambios en el backend.

## Archivos principales

- `frontend/src/features/configuracion/api/periodos-matricula.ts`, `mutaciones-periodo-matricula.ts`, `plantillas.ts`
- `frontend/src/features/configuracion/components/periodo-matricula/` (`periodos-matricula-lista`, `periodos-matricula-tabla`, `formulario-periodo`, `periodo-existente`, `documentos-requeridos`, `enlaces.ts`…)
- `frontend/src/app/routes/app/administrativo/matricula.tsx`, `periodo-matricula-nuevo.tsx`, `periodo-matricula.tsx`, `periodo-matricula-editar.tsx`
- Compartidos: `components/ui/tag/selectable-tag.tsx`, `form/textarea.tsx`, `form/field-wrapper.tsx`, `stat-card/stat-card.tsx`, `icon-badge/icon-badge.tsx`, `button/button.tsx`

## Decisiones y por qué

- **Decisión:** el listado reemplaza el placeholder de `/app/administrativo/matricula` y **conserva la rama `?matriculaId=`** de P4 (detalle de matrícula, `PagoMatriculaCard`) sin cambios. Los formularios van en `/matricula/periodos/nuevo`, `/matricula/periodos/:periodoId` (Ver) y `/matricula/periodos/:periodoId/editar`. No se tocó `navigation.ts`.
  **Por qué:** la ficha daba `/matricula` como ruta de la pantalla 13, pero esa ruta ya la usa P4 para el detalle de pago; así las dos pantallas conviven sin pisarse.
- **Decisión:** el menú ⋮ ofrece **"Ver matrículas" en todos los periodos**, también en los cerrados, aunque Figma no lo dibuja ahí. El menú queda: PROGRAMADO, Ver, Ver matrículas, Editar y Eliminar; ABIERTO, Ver, Ver matrículas y Editar; CERRADO, Ver y Ver matrículas.
  **Por qué:** para consultar el historial de matrículas de los periodos anteriores (HU-71).
- **Decisión:** **"Ver" es el formulario de creación en solo lectura.** Con el periodo PROGRAMADO muestra además "Editar"; con ABIERTO o CERRADO muestra el aviso "Una vez habilitado, el periodo no se puede editar; solo deshabilitar." y "Volver a los periodos de matrícula". "Editar" de un periodo ABIERTO o CERRADO redirige a Ver. "Ver configuración" de la tarjeta Vigente sigue abriendo el modal.
  **Por qué:** solo un PROGRAMADO se edita (el backend responde 409 si no); así el usuario ve el motivo en lugar de un formulario que no podrá guardar.
- **Decisión:** la vista de solo lectura ofrece **"Descargar"** por cada plantilla guardada, que Figma no dibuja.
  **Por qué:** el backend permite descargar la plantilla en cualquier estado y es la única forma de revisarla desde la pantalla.
- **Decisión:** las **plantillas se suben después de guardar el periodo**, con los ids de documento que devuelve el backend (el documento nuevo no tiene id antes). Si una falla, el periodo ya está guardado: se avisa y se pasa a Editar (al crear) o se queda en Editar con lo que quedó guardado (al editar) para reintentar.
  **Por qué:** es el orden que impone el backend; así un error de plantilla no pierde el periodo.
- **Decisión:** al editar, **cambiar o quitar una plantilla se aplica al guardar el periodo**, no al momento.
  **Por qué:** el backend sube y quita plantillas con endpoints inmediatos, pero el usuario espera que "Guardar configuración" sea lo que confirma todo; así "Cancelar" descarta cualquier cambio. Un documento con entregas no se puede quitar.
- **Decisión:** los enlaces a pantallas que **aún no existen** quedan deshabilitados con el aviso "Disponible pronto", controlados por constantes en `components/periodo-matricula/enlaces.ts`: `MATRICULAS_DEL_PERIODO_DISPONIBLE` (A3 de Matrícula, `/matricula/periodos/:periodoId/matriculas`) y `TARIFARIO_DISPONIBLE` (P19, `/pensiones/configuracion`: "Ir al tarifario" y "Ver tarifario completo"). Las rutas están en `paths.ts` y no están registradas en el router.
  **Por qué:** no se crea un placeholder en pantallas de otros equipos; al hacerlas basta con registrar la ruta y cambiar la constante.
- **Decisión:** los estados de la tabla usan **`SelectableTag` con `readOnly`**, en lugar de una variante nueva de `Tag`: una pastilla informativa (`<span>`, no un botón) con el relleno claro de Figma.
  **Por qué:** reutiliza el componente de Figma "Selectable Tag" y no agrega otro.
- **Decisión:** `/vigente` solo se pide si alguna fila de la lista completa (`size=100`) trae `vigente = true`; tras cada cambio se recarga primero la lista y, si ya no queda vigente, se descarta esa consulta.
  **Por qué:** el backend responde 404 sin periodo vigente y el interceptor lo mostraría como error.
- **Decisión:** el formulario valida con los mismos mensajes del backend y muestra su error bajo el campo. Las horas se ofrecen cada 30 minutos más las 11:59 p. m. (y la actual si es otra). La barra de duración cuenta los días reales entre las fechas (01/12 → 15/02 son 76, no los 71 de Figma). "Guardar configuración" se deshabilita si el año no tiene el tarifario confirmado.
  **Por qué:** evita enviar lo que el backend rechazaría y que el ejemplo de Figma no cuadre con sus fechas.

## Librerías o herramientas nuevas

Ninguna. Íconos de `lucide-react`, con las equivalencias nuevas agregadas a la [tabla de la decisión de lucide](2026-10-22-yatako-iconos-lucide.md).

## Pendientes / problemas conocidos

- [ ] "Ver matrículas": cuando P4 haga la A3 en `/matricula/periodos/:periodoId/matriculas`, registrar la ruta y poner `MATRICULAS_DEL_PERIODO_DISPONIBLE = true`.
- [ ] Tarifario (P19): al hacer la pantalla, registrar `/pensiones/configuracion`, poner `TARIFARIO_DISPONIBLE = true` y habilitar el botón "Configuración" de la pantalla de Pensiones (archivo de P4, avisarles).
- [ ] Deshabilitar un periodo y editar uno abierto (iteración 2 del backend).

## Notas para quien siga

- Con `Textarea` `showCount` en un formulario de react-hook-form, úsalo **controlado** (`value`/`onChange`): así el contador sigue a `reset()`.
- Las celdas de `Table` (`Cell`) se usan como componentes: si se definen dentro del componente que renderiza la tabla, se vuelven a montar con cada render y un campo dentro pierde el foco al escribir. Defínelas fuera (ver `tabla-vacantes.tsx`).
- En las pruebas, MSW no lee el cuerpo `FormData` en jsdom: para comprobar una subida de plantilla, espía `api.post`. Y `navigate` real rompe el router de prueba: sustituye `useNavigate` (y `Navigate`) con `vi.mock('react-router')`.
