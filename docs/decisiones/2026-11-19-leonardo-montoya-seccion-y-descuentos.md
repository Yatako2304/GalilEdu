# Sección oficial al formalizar la matrícula y correcciones de descuentos

- **Autor:** Leonardo Montoya (con asistencia de IA)
- **Fecha:** 2026-11-19
- **HU / tarea:** correcciones del Product Owner a HU-24/HU-25 (sección) y HU-38 (descuentos)
- **Estado:** terminado (backend)

## Qué hice

- Los GET de matrícula separan la sección oficial (`seccionAsignada`, solo con la matrícula MATRICULADO) de la elegida mientras la matrícula está en proceso (`seccionSeleccionada`): detalle (A5/B6/B7), tabla de A3 y estado del apoderado.
- Un descuento aplicado puede regir "hasta que el estudiante termine el colegio" (`hastaFinEscolaridad`, migración V21).
- El descuento por hermanos solo se aplica al hermano menor; el backend rechaza aplicarlo a otro hermano (409).

## Archivos principales

- `backend/src/main/java/com/galiledu/matricula/aplicacion/DtosMatricula.java` (`SeccionMatricula`)
- `backend/src/main/java/com/galiledu/pagos/aplicacion/ServicioDescuentosAplicados.java`
- `backend/src/main/java/com/galiledu/pagos/infraestructura/ConsultasPagos.java` (`hermanosMatriculados`)
- `backend/src/main/resources/db/migration/V21__descuento_hasta_fin_escolaridad.sql`

## Decisiones y por qué

- **Decisión:** la sección elegida durante el proceso se sigue guardando en `matricula.seccion_id` y **sigue ocupando el cupo** como reserva; lo que cambia es la respuesta (`seccionAsignada` null hasta MATRICULADO).
  **Por qué:** la corrección lo permite si hay un sistema de reserva, y ya lo hay (el estado se llama SECCION_RESERVADA y las vacantes de HU-75 lo cuentan). Si el cupo se ocupara solo al pagar, una sección podría tener más alumnos en proceso que vacantes y alguien se quedaría sin sección después de pagar. Notas, asistencia y horarios ya solo consideran a los MATRICULADO, así que nadie trata la reserva como oficial.
  **Alternativas descartadas:** una columna aparte `seccion_seleccionada_id` y contar el cupo solo con MATRICULADO; habría cambiado las vacantes de Configuración y Horarios (otros equipos) sin ganar nada para el usuario.
- **Decisión:** el campo `seccion` (texto) se mantiene como estaba para no romper el frontend actual.
  **Por qué:** los campos nuevos son adicionales.
- **Decisión:** si la solicitud no trae `hastaFinEscolaridad`, una `vigenciaHasta` vacía significa "hasta que termine el colegio".
  **Por qué:** ya funcionaba así (sin fecha de fin el descuento no vencía) y el frontend actual manda la fecha vacía; con el indicador explícito se aplican las reglas del PO (false exige la fecha; true no la admite).
- **Decisión:** el descuento "hasta que termine el colegio" no se copia a cada año: como es del estudiante y queda ACTIVO, la cuota de matrícula y el cronograma de cada año nuevo ya lo aplican.
  **Por qué:** evita duplicarlo y que el personal lo vuelva a registrar.
- **Decisión:** hermano menor = el hermano MATRICULADO del mismo año con la fecha de nacimiento más reciente. Con 3 o más hermanos, solo el menor; si los menores son gemelos (misma fecha), ambos.
  **Por qué:** lo decidió el equipo mientras el PO no responda (ver pendientes). Cambiarlo es tocar `menores()` en `ServicioDescuentosAplicados`.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO: con 3 hermanos, ¿solo el menor o los dos menores? ¿Gemelos ambos?
- [ ] El egreso del estudiante no existe aún; el descuento deja de aplicarse en la práctica porque no se generan compromisos nuevos.
- [ ] Un descuento por hermanos que sigue activo el año siguiente no se reevalúa si cambia quién es el menor o la cantidad de hermanos.
- [ ] Frontend: mostrar "Por asignar" con `seccionAsignada` null, el check "Hasta que termine el colegio" y el motivo/beneficiario del descuento por hermanos.

## Notas para quien siga

- `SeccionMatricula.asignada()` y `seleccionada()` deciden qué se muestra según el estado; úsalas si agregas otro GET de matrícula.
