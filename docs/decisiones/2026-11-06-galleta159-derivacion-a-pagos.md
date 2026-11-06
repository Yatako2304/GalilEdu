# Derivación de la matrícula a pagos

- **Autor:** Galleta159 (Wilbher Aguirre), con Luis Yataco
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-31 (se apoya en HU-41 y HU-37)
- **Estado:** terminado

## Qué hice

Cuando la matrícula está en `PENDIENTE_PAGO` y ya se generó la cuota, el apoderado ve la tarjeta "Pago de matrícula" con el botón **Pagar matrícula**. El pago usa el flujo de HU-41 con una orden que trae solo la cuota de matrícula. Al confirmarse el pago, la matrícula pasa a `MATRICULADO`, recibe su código `MAT-AAAA-NNNN` y se le generan las pensiones (HU-37). Si el pago se rechaza o expira, la matrícula sigue en `PENDIENTE_PAGO`.

## Archivos principales

- `backend/src/main/java/com/galiledu/matricula/aplicacion/EscuchaPagoMatricula.java`
- `backend/src/main/java/com/galiledu/matricula/aplicacion/ServicioConfirmacionPago.java`
- `backend/src/main/java/com/galiledu/matricula/aplicacion/ServicioEstadoMatricula.java` (campo `pagoMatricula`)
- `frontend/src/features/pagos/components/pago-matricula-apoderado.tsx`
- `frontend/src/app/routes/app/apoderado/matricula.tsx`

## Decisiones y por qué

- **Decisión:** el pago de la matrícula solo se bloquea por las pensiones vencidas **del propio hijo**. La deuda de un hermano no bloquea.
  **Por qué:** así se acordó con el PO el 30/10. Una deuda de otro hijo no debe impedir que este se matricule. Además, esa deuda ya se cobra en su propio cronograma.
  **Alternativas descartadas:** bloquear por la deuda de toda la familia. Es lo que hace hoy la alerta de HU-30 y queda como pendiente de la pareja 4.

- **Decisión:** la matrícula se confirma **después** de guardar el pago. `EscuchaPagoMatricula` escucha el evento `PagoEnLineaAprobado` en la fase `AFTER_COMMIT`. Luego `ServicioConfirmacionPago` hace dos pasos, cada uno en su propia transacción (`REQUIRES_NEW`):
  - `matricular`: cambia el estado y asigna el código, con bloqueo de fila;
  - `generarPensiones`: crea el cronograma.

  **Por qué:** el dinero ya se cobró en la pasarela. Si matricular falla y eso deshiciera el pago, quedaría un cobro real sin registrar. Así, el pago siempre se guarda; si un paso falla, se registra en el log y se puede reintentar. Pagos no depende de matrícula: solo publica el evento.
  **Alternativas descartadas:**
  - matricular dentro de la misma transacción del pago: un error desharía el pago;
  - que `pagos` llame directo a `matricula`: acopla los módulos al revés.

- **Decisión:** confirmar dos veces no repite nada. Ya matriculado, no se crea otro código, no se genera otro cronograma y deja de mostrarse el botón.
  **Por qué:** el aviso de la pasarela puede llegar más de una vez, por el webhook y por la consulta directa.
  **Alternativas descartadas:** ninguna.

## Pendientes / problemas conocidos

- [x] El paso 4 del avance ("Matriculado") se mostraba como paso actual, con el número 4. Ahora, ya matriculado, los 4 pasos salen completados con check, como en Figma (Apoderado › "HU-32 Confirmación de matrícula del estudiante"). El cambio está en `consulta-matricula.tsx`; el backend sigue devolviendo `pasoActual = 4`.
- [ ] HU-30 (pareja 4): la alerta de deuda todavía bloquea por la deuda de toda la familia. Debe seguir la misma regla de esta HU.
- [ ] Crear la cuenta del estudiante al quedar matriculado (V7). No es parte de esta HU.

## Notas para quien siga

- Si una matrícula queda pagada pero sin matricular (por ejemplo, porque falló `generarPensiones`), el error queda en el log. El pago no se pierde.
- Para probar en local con la pasarela simulada, usa la opción **Simular tarjeta rechazada**: la matrícula debe seguir en `PENDIENTE_PAGO`.
