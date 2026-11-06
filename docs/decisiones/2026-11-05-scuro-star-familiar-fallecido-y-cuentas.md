# Familiar fallecido, mayoría de edad y una cuenta por familiar

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-05
- **HU / tarea:** HU-7 (inscripción) y HU-6/HU-9 (cuentas) · modificaciones 2, 3 y 4 de la validación de la iteración 1 con el PO
- **Estado:** terminado (falta probarlo desde la pantalla)

## Qué hice

- **Fallecido (2):** "No registra" y "Fallecido(a)" son casillas distintas. El fallecido se registra solo con sus datos de identidad (documento, apellidos, nombres, fecha de nacimiento, nacionalidad, país de nacimiento, sexo, estado civil y grado de instrucción; no se muestran los de contacto, trabajo, domicilio ni exalumno), pero no tiene cuenta, no puede ser responsable de matrícula, no aparece para recoger al estudiante y no cuenta como presente para la regla del apoderado. Los motivos de "No registra" son "No se conoce", "No ha reconocido al estudiante" y "Otro" (con observación obligatoria).
- **Mayor de edad (3):** el padre, la madre y el apoderado deben tener 18 años o más, también si están fallecidos (un padre no puede ser menor de edad).
- **Cuentas (4):** cada padre, madre o apoderado presente tiene su cuenta con rol Apoderado, sea o no el responsable de matrícula.

## Archivos principales

- `backend/src/main/resources/db/migration/V12__persona_fallecido.sql`: columna `personas.persona.fallecido`.
- `backend/.../estudiantes/aplicacion/ServicioEstudiantes.java`: reglas de la inscripción y de las cuentas.
- `backend/.../estudiantes/infraestructura/EstudianteRepositorio.java`: `esFamiliarPresenteDeOtro`.
- `backend/.../usuarios/aplicacion/ServicioUsuarios.java`: `retirarRolApoderado` (reemplaza a `desactivarCuentaDe`).
- `frontend/src/features/estudiantes/components/inscripcion/` (`esquema.ts`, `pasos-familia.tsx`, `inscripcion-wizard.tsx`) y `ficha-estudiante.tsx`.

## Decisiones y por qué

- **Decisión:** "fallecido" se guarda en la persona (`personas.persona`), no en el vínculo con el estudiante.
  **Por qué:** es un hecho de la persona; si tiene dos hijos en el colegio, se marca una vez y lo ven ambos.
  **Alternativas descartadas:** columna en `estudiantes.vinculo_familiar` (habría que marcarlo por cada hijo); reutilizar `motivo_no_registra` con la persona registrada (mezcla dos conceptos).
- **Decisión:** "presente" = registrado y no fallecido. Las reglas que hablan de personas vivas (responsable, recojo, cuentas, regla del apoderado) usan "presente"; las que solo muestran datos (resumen, ficha) siguen mostrando al fallecido con su etiqueta.
  **Por qué:** el PO pidió registrar los datos del fallecido sin darle acceso ni responsabilidades.
- **Decisión:** al editar, quien deja de ser familiar presente (lo quitan, pasa a "No registra" o se marca fallecido) pierde el rol Apoderado, salvo que lo siga siendo de otro estudiante; si era su único rol, la cuenta queda inactiva.
  **Por qué:** antes se desactivaba la cuenta completa del responsable anterior, aunque tuviera otro rol (por ejemplo, Docente). Ahora el responsable puede cambiar sin quitarle la cuenta a nadie, porque todos los familiares presentes tienen la suya.
- **Decisión:** no se puede marcar fallecido a alguien que es responsable de matrícula de otro estudiante; primero hay que cambiar al responsable allí.
  **Por qué:** evita dejar a un hermano con un responsable fallecido.
- **Decisión:** las inscripciones guardadas antes de este cambio no crean cuentas de golpe: cada padre recibe la suya cuando se edite esa inscripción.
  **Por qué:** crear todas a la vez enviaría muchos correos.

## Pendientes / problemas conocidos

- [ ] Modificación 5 (el responsable solo para lo de pagos): revisar qué acciones de matrícula y pagos debe hacer solo el responsable ahora que todos los familiares tienen cuenta.
- [ ] Confirmar con el PO los textos exactos de los motivos y si al fallecido se le pide "exalumno" (hoy sí se muestra).
- [ ] Avisar a quienes trabajan en `estudiantes`, `matricula` y `pagos` que existe `personas.persona.fallecido`.

## Notas para quien siga

- `DatosFamiliar.presente()` y `esFallecido()` (backend) y `presente()` de `esquema.ts` (frontend) son la misma regla: úsalas en lugar de `registra()` cuando se trate de personas vivas.
- La respuesta de inscribir y editar trae `cuentas` (una por familiar presente, con `nueva` si se acaba de crear); `usuarioResponsable` y `cuentaNueva` se mantienen para no romper a quien los use.
