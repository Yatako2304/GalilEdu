# Horario de dictado del docente y del tutor

- **Autor:** royal-22
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-49
- **Estado:** terminado (backend)

## Qué hice

Agregué el backend del horario semanal del Docente y del Tutor: ven solo las clases que dictan. `GET /horarios/docente/anios` lista los años en que tienen clases y `GET /horarios/docente/horario?anioId=` devuelve un horario por estructura horaria, con la jornada y las clases de cada una. No hay migración: todo se lee de las tablas que ya existen.

## Archivos principales

- `backend/src/main/java/com/galiledu/horarios/aplicacion/ClasesDelDocente.java` y `infraestructura/ClasesDelDocenteSql.java`
- `backend/src/main/java/com/galiledu/horarios/aplicacion/ServicioHorarioDocente.java` y `DtosHorarioDocente.java`
- `backend/src/main/java/com/galiledu/horarios/web/HorarioDocenteController.java`
- `backend/src/test/java/com/galiledu/horarios/ClasesDelDocenteIT.java`, `HorarioDocenteIT.java` y `aplicacion/ServicioHorarioDocenteTests.java`
- Ficha: [`docs/modulos/horarios.md`](../modulos/horarios.md)

## Decisiones y por qué

- **Decisión:** el horario se separa por **estructura horaria**, no solo por nivel; primaria va antes que secundaria y el título lleva el código de la estructura si hay dos del mismo nivel.
  **Por qué:** cada sección usa la jornada de su propia estructura (primaria y secundaria suelen empezar a horas distintas) y un horario guardado puede seguir usando una estructura que ya no es la principal. Dos estructuras del mismo nivel pueden tener horas distintas, así que agrupar por nivel mezclaría jornadas en una misma matriz.
  **Alternativas descartadas:** un solo horario con las horas de la estructura principal, que mostraría horas falsas; agrupar solo por nivel.

- **Decisión:** el docente se toma **siempre del token** (`ServicioJwt.usuarioActual`); nunca viene en la URL ni en un parámetro.
  **Por qué:** así nadie consulta el horario de otro docente cambiando un id. Es el mismo patrón de `HorarioApoderadoController` y de `/calificaciones/docente`.
  **Alternativas descartadas:** `/horarios/docente/{id}`, que obligaría a validar de quién es cada id.

- **Decisión:** qué clases ve lo define el **titular actual** de `academico.docente_seccion` para (curso, sección), no un docente guardado en el bloque.
  **Por qué:** el bloque no guarda al docente (es una decisión de la HU-45): si Académico cambia el titular, el horario pasa al nuevo sin migrar nada.
  **Alternativas descartadas:** guardar el docente en `horario_bloque`, que dejaría datos desactualizados.

- **Decisión:** el **Tutor ve solo sus clases de dictado**, igual que el Docente. Un tutor que no dicta ve el estado vacío.
  **Por qué:** la HU-49 es el horario de dictado y la ruta del tutor usa el mismo frame de Figma (393:18119) que la del docente. Ser `tutor_id` de una sección no implica dictar en ella.
  **Alternativas descartadas:** mostrar además el horario de la sección que tutorea; queda fuera de esta HU y pendiente de consultar al PO.

- **Decisión:** las celdas con **dos clases no se filtran**: se devuelven las dos.
  **Por qué:** un cruce puede haber quedado guardado (por ejemplo, tras un cambio de titular en Académico, que no consulta Horarios). Ocultarlo haría creer que no hay problema; el frontend decide cómo mostrarlas.
  **Alternativas descartadas:** quedarse con una de las dos o responder un error.

- **Decisión:** **año por defecto:** el ACTIVO si el docente tiene clases en él; si no, el más reciente con clases. Con un `anioId` sin clases se responde 200 con los datos del año y `horarios` vacío; con uno inexistente, 404.
  **Por qué:** el docente entra casi siempre a ver el año en curso, pero si ese año aún no tiene horario no debe ver una pantalla vacía habiendo uno anterior. Un año válido sin clases no es un error.
  **Alternativas descartadas:** el más reciente a secas, que podría ser un año planificado sin clases.

## Pendientes / problemas conocidos

- [ ] Falta el frontend: `/app/docente/horario` y `/app/tutor/horario` (Figma 393:18119).
- [ ] Consultar al PO si el tutor debe ver además el horario de la sección que tutorea.
- [ ] Diferencias con Figma: «Prof. …» en cada celda sobra, el menú lateral dice «Administrador» y la sección se ve «3A» (el backend usa «1.° A»).

## Notas para quien siga

- `ServicioHorarioDocente` no usa `ServicioHorarios`: tiene su propio mapeo de la jornada a los DTOs de `DtosHorarios`. Si cambia el formato de la jornada, hay que cambiarlo en los dos.
- El ambiente va por **nombre**, como en el resto del módulo (el pendiente de mostrar el código aplica también aquí).
- El tutor se reconoce por tener el rol TUTOR, no por ser `tutor_id`; la autorización de los endpoints es `hasAnyRole('DOCENTE', 'TUTOR')`.
