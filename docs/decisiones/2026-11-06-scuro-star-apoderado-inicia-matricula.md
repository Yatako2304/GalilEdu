# El apoderado inicia la matrícula sin esperar al colegio

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-28, HU-29 (documentos del apoderado) y su relación con HU-24 (asignación de sección)
- **Estado:** terminado

## Qué hice

Antes, el apoderado no podía hacer nada hasta que el Personal administrativo creara la matrícula con «Nueva matrícula» («El colegio aún no reserva una sección para este estudiante»). Ahora, con el periodo abierto, el apoderado entra al paso 1 «Datos y documentos», elige los 4 documentos firmados y pulsa «Enviar a validación». En ese momento se crea la matrícula con la sección que asigna el sistema y queda EN_VALIDACION, en la bandeja del Personal administrativo. «Nueva matrícula» del colegio sigue igual, para cuando la familia la hace en el colegio.

## Archivos principales

- `backend/.../matricula/aplicacion/ServicioMatriculaApoderado.java`: `iniciar` y la vista de documentos sin matrícula.
- `backend/.../matricula/aplicacion/ServicioEstadoMatricula.java`: sin matrícula, `puedeContinuar` y paso 1 si nada lo impide; impedimento `INSCRIPCION` (reemplaza a `SIN_SECCION`).
- `backend/.../matricula/web/MatriculaApoderadoController.java`: `POST /matricula/apoderado/estudiantes/{id}/matricula`.
- `frontend/src/features/matricula/components/documentos-apoderado.tsx` y `api/documentos-apoderado.ts` (`useIniciarMatricula`).
- Pruebas: `MatriculaIniciadaPorApoderadoIT.java`, `documentos-apoderado.test.tsx` y `consulta-matricula.test.tsx`.

## Decisiones y por qué

- **Decisión:** el apoderado no espera a que el colegio reserve la sección.
  **Por qué:** Figma › Apoderado › «HU-28 Descarga de documentación de matrícula» muestra al apoderado en el paso 1 con el periodo abierto y la sección «Se asigna al confirmar la matrícula». El PO pidió que la sección la elija el sistema. El texto de HU-24 en el backlog está desactualizado.
- **Decisión:** la matrícula se crea recién al «Enviar a validación», con todos los documentos a la vez. Hasta entonces los archivos quedan en la pantalla del apoderado.
  **Por qué:** así a la bandeja del Personal administrativo solo llega lo que está listo para revisar (EN_VALIDACION), no matrículas a medio llenar. Tampoco se ocupan vacantes de quien abandona el proceso.
  **Alternativas descartadas:** crear la matrícula al subir el primer documento (llenaría la bandeja de borradores y ocuparía vacantes); un estado nuevo «borrador» (migración y cambios en todos los listados).
  **Costo:** si el apoderado cierra la página antes de enviar, debe volver a elegir los archivos. La pantalla lo avisa: «Se guardan al enviarlos a validación».
- **Decisión:** la sección se asigna con la regla que ya usa «Nueva matrícula»: la primera sección del grado con cupo, en orden alfabético. El apoderado sigue viendo «Por definir» hasta que la matrícula esté MATRICULADO.
  **Por qué:** se reutiliza `ServicioMatriculasPeriodo.crear` (cupo, tipo NUEVO/CONTINUADOR, documentos y paso a EN_VALIDACION) sin duplicar reglas. El colegio puede cambiarla con «Cambiar sección».
- **Decisión:** solo puede iniciarla quien está inscrito para el año del periodo, como en «Nueva matrícula». Si no, el impedimento es `INSCRIPCION`.
- **Decisión:** si la matrícula ya existe (la creó el colegio o devolvió un documento observado), se sigue subiendo documento por documento, como antes.
- **Decisión:** `spring.servlet.multipart.max-request-size` sube de 11 MB a 45 MB. Cada archivo sigue limitado a 10 MB.
  **Por qué:** los 4 documentos viajan en un solo pedido (también en «Nueva matrícula», que ya los mandaba juntos y fallaba si sumaban más de 11 MB).

## Pendientes / problemas conocidos

- [ ] Un estudiante continuador debe tener su inscripción con el año del periodo para iniciar la matrícula (misma regla que «Nueva matrícula»).

## Notas para quien siga

- Para probarlo con los datos de prueba hay que quitar la matrícula 2027 de Mateo (la creó el colegio) o inscribir un estudiante nuevo para 2027, y tener el periodo 2027 abierto.
