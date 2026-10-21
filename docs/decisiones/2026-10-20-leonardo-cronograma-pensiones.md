# Generación y consulta del cronograma de pensiones (HU-37)

- **Autor:** Leonardo Montoya
- **Fecha:** 2026-10-20
- **HU / tarea:** HU-37 (RF-81 a RF-85)
- **Estado:** terminado

## Qué hice

Backend del compromiso de pago: consultar el cronograma con su resumen, generar las pensiones del tarifario al matricular y anular las pensiones futuras por retiro. Pantalla P8 del Personal administrativo.

## Archivos principales

- `backend/src/main/java/com/galiledu/pagos/aplicacion/ServicioCronograma.java`
- `backend/src/main/java/com/galiledu/pagos/aplicacion/ServicioCompromisos.java`
- `backend/src/main/java/com/galiledu/pagos/infraestructura/ConsultasPagos.java`
- `frontend/src/features/pagos/`

## Decisiones y por qué

- **Decisión:** la generación de pensiones es un método de Pagos (`ServicioCronograma.generarPensiones`) que **Matrícula llama al confirmarse el pago** (HU-31). También existe `POST /pagos/matriculas/{id}/cronograma` para generarlo a mano o reintentar.
  **Por qué:** así lo indica el diagrama de componentes (Matrícula → `IConfirmacionPago` → Pagos), y la HU-37 se puede probar sin esperar a la HU-31.
  **Alternativas descartadas:** generar las cuotas desde Matrícula escribiendo en `pagos.cuota` (rompe el aislamiento entre módulos).
- **Decisión:** "Vencida" no se guarda; se calcula al consultar (pensión impaga después del vencimiento más los días de gracia del tarifario).
  **Por qué:** lo indica `docs/modulos/pagos.md` (RF-91); evita un proceso que actualice estados cada día.
- **Decisión:** `ConsultasPagos` lee `matricula.matricula` por SQL (solo lectura).
  **Por qué:** el módulo de Matrícula aún no tiene servicio. Cuando exista, se reemplaza por una llamada a ese servicio.

## Pendientes / problemas conocidos

- [ ] Enlazar P8 desde P1/P2 (Estado de cuenta) y desde B7 ("Ver compromiso de pago").
- [ ] HU-31: llamar a `generarPensiones` al confirmar el pago de la matrícula.
- [ ] Reemplazar la lectura SQL de matrícula por el servicio de Matrícula.

## Notas para quien siga

- Si el tarifario del año no está CONFIRMADO, la generación responde 409.
- Si el estudiante tiene un descuento ACTIVO que aplica a PENSION, las cuotas se generan ya con la rebaja.
