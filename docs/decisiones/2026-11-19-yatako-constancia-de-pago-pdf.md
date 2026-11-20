# Constancia de pago en PDF y tabla de Pagos por constancia

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-42 (RF-107, RF-108)
- **Estado:** terminado

## Qué hice

El apoderado descarga en PDF la constancia de cada pago desde Apoderado › Pensiones › pestaña **Pagos** (botón ⬇ por fila). El PDF se genera en el backend con PDFBox, a partir de los datos ya guardados del pago (`pagos.pago` y `pago_detalle`). No hay migraciones nuevas.

## Archivos principales

- `backend/src/main/java/com/galiledu/pagos/infraestructura/ArchivoConstanciaPago.java`
- `backend/src/main/java/com/galiledu/pagos/infraestructura/ConsultasPagos.java` (`constancia`, `detalleConstancia`)
- `backend/src/main/java/com/galiledu/pagos/aplicacion/ServicioDeudaApoderado.java` (`constancia`, `constanciaPdf`)
- `backend/src/main/java/com/galiledu/pagos/web/PagosApoderadoController.java` (`GET /pagos/apoderado/pagos/{id}/constancia`)
- `frontend/src/features/pagos/components/tabla-pagos.tsx` y `pensiones-apoderado.tsx`
- `frontend/src/features/pagos/api/pensiones-apoderado.ts` (`descargarConstancia`)

## Decisiones y por qué

- **Decisión:** la tabla de Pagos sigue siendo **una fila por pago** (Fecha, Constancia, Cuotas, Monto de cuotas, Recargo, Total pagado, Estado) y solo se le agregó la columna de descarga. **Se aparta de Figma**, que en "HU-42 Constancia de pago y HU-40" y en "P9 · Pagos" muestra una fila por concepto con Referencia y Capital.
  **Por qué:** desde HU-41 un pago puede cubrir varias cuotas del mismo estudiante y tiene **una sola constancia** ([decisión de la pasarela](2026-11-04-yatako-pasarela-de-pagos.md)). Con una fila por pago, cada botón descarga un PDF distinto. Lo revisamos con capturas de la tabla real frente a Figma y el equipo eligió mantenerla (19/11). La tabla ya estaba así desde el commit `cceb986` (06/11), sin estar documentado.
  **Alternativas descartadas:** una fila por concepto como en Figma (la misma constancia se repetiría en varias filas y descargaría el mismo PDF).
- **Decisión:** el botón de descarga solo aparece en la vista del apoderado: `TablaPagos` lo muestra si recibe `onDescargar`. En P9 (personal administrativo) no aparece.
  **Por qué:** HU-42 y RF-108 son del apoderado, y Figma P9 no tiene el ícono de descarga.
- **Decisión:** el PDF se arma en el backend con PDFBox y el mismo estilo que el reporte de notas ([decisión](2026-11-06-josezavaleta-reporte-notas-pdf.md)): A4 vertical, encabezado azul, una fila por cuota y los totales al final. Si hay muchas cuotas, la tabla continúa en otra página.
  **Por qué:** los montos salen de lo guardado en el pago, sin recalcular nada, y la constancia es igual para cualquier navegador. PDFBox ya estaba en el `pom.xml`.
  **Alternativas descartadas:** generarlo en el navegador o con el diálogo de impresión.
- **Decisión:** Figma no tiene el diseño del PDF, así que el contenido sale del RF-107: número, estudiante y documento, año con grado y sección, fecha y hora del pago (Lima), referencia, estado (y la observación si quedó OBSERVADO), las cuotas con su concepto, vencimiento, monto y recargo, y los totales. En el PDF se usa "Monto" y "Monto de cuotas" en vez de "capital", como en el resto de la interfaz ([pagos.md](../modulos/pagos.md)).
- **Decisión:** un pago inexistente o de otra familia responde **404**, igual que un compromiso ajeno en HU-40.

## Pendientes / problemas conocidos

- [ ] El PDF del estado de cuenta (RF-98) sigue pendiente; no es parte de HU-42.

## Notas para quien siga

- Para correr el backend en una PC con espacios en la carpeta del usuario (por ejemplo `C:\Users\ASESORES 4`), Java no puede crear el socket local del cliente HTTP de Stripe ("Unable to establish loopback connection"). Arranca con `-Djdk.net.unixdomain.tmpdir=C:\galiledu-tmp` (o `JAVA_TOOL_OPTIONS`).
- Si tu `backend/.env` tiene `PASARELA_PAGOS=stripe`, `PagoEnLineaIT` y `DerivacionPagosIT` fallan en local porque esperan la pasarela simulada. Corre `mvnw verify` con `PASARELA_PAGOS=simulada`.
