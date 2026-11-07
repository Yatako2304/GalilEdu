# Código de la estructura horaria generado por el backend

- **Autor:** valeriaacevedop
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-44 (ajustes de UI/UX y cambio de contrato)
- **Estado:** terminado

## Qué hice

El código de la estructura horaria deja de pedirse en el modal: lo genera el backend al crear (`EH-PRI-27`, `EH-SEC-27-2`…). Además, el modal abre con la hora de inicio en 07:30, la vista previa nunca muestra horas de 24:00 en adelante, y «Hora de entrada» pasó a verse como «Hora de inicio».

## Archivos principales

- `backend/src/main/java/com/galiledu/configuracion/aplicacion/ServicioEstructurasHorarias.java` (`generarCodigo`)
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/DtosEstructurasHorarias.java`
- `frontend/src/features/configuracion/components/estructura-horaria/` (`dialogo-estructura.tsx`, `esquemas-estructura.ts`, `vista-previa-jornada.tsx`)

## Decisiones y por qué

- **Decisión:** el formato es `EH-{PRI|SEC}-{año a 2 dígitos}`; si ya existe en el mismo año, sufijo `-2`, `-3`… La generación va dentro del bloqueo por año que ya usan las estructuras.
  **Por qué:** así dos creaciones simultáneas no producen el mismo código, y la unicidad por año de la BD sigue siendo la red de seguridad.
  **Alternativas descartadas:** generarlo en el frontend (no es seguro con concurrencia) o con una secuencia de BD (los códigos ya existentes no la respetarían).
- **Decisión:** `codigo` se **ignora** en las solicitudes de crear y editar, en lugar de quitarlo del DTO.
  **Por qué:** rompe menos: un cliente viejo que lo siga enviando no recibe errores. Spring ya ignora campos desconocidos, pero se dejó el campo documentado como ignorado.
  **Alternativas descartadas:** quitarlo del DTO (también habría funcionado, pero cambia más código sin ganar nada).
- **Decisión:** al editar, el código se conserva; la vista previa ya no valida ni avisa del código repetido.
  **Por qué:** el usuario no lo escribe, así que no hay nada que avisar.
- **Decisión:** la sigla de un nivel distinto de Primaria y Secundaria no está definida (hoy no existen). El código usa las tres primeras letras del nivel como respaldo.
  **Por qué:** se pidió consultar antes de inventar una sigla; como no hay otros niveles, queda para cuando existan.
- **Decisión:** el frontend muestra «—» para cualquier hora desde las 24:00 (el backend suma sin dar la vuelta al día) y marca en rojo los bloques que terminan después de las 18:00. No se cambió la validación, solo la presentación.
- **Decisión:** cambia el texto visible «hora de entrada» por «hora de inicio», incluido el mensaje 400 «Ingresa la hora de inicio.». Los campos de la API (`horaEntrada`), DTOs, columnas y migraciones no se renombraron.

## Pendientes / problemas conocidos

- [ ] Definir la sigla cuando se agregue un nivel nuevo.
- [ ] Los comentarios y javadoc que dicen «hora de entrada» no se cambiaron (solo cambian textos visibles).

## Notas para quien siga

- Los códigos que ya existían (`EH-PRI-26`, `PRI-2027`…) no se tocaron; solo los nuevos siguen este formato.
- El modal de edición muestra el código como texto de solo lectura. Editar no está en Figma.
