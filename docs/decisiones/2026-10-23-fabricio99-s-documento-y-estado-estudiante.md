# Documento repetido y estado del estudiante en la inscripción (HU-7)

- **Autor:** fabricio99-s
- **Fecha:** 2026-10-23
- **HU / tarea:** HU-7
- **Estado:** terminado

## Qué hice

El paso 1 de la inscripción ya revisa el documento antes de pasar al paso 2 (antes el error salía recién al registrar). Un estudiante activo o retirado no se puede inscribir de nuevo; uno eliminado sí, y sus datos se actualizan. El listado de estudiantes tiene el menú de tres puntos de Usuarios (Ver, Inactivar o Reactivar, Eliminar).

## Archivos principales

- `backend/src/main/java/com/galiledu/estudiantes/aplicacion/ServicioEstudiantes.java` (`verificarDocumento`, `estudianteParaInscribir`, `retirar`, `reactivar`, `eliminar`)
- `backend/src/main/java/com/galiledu/estudiantes/web/EstudianteController.java`
- `backend/src/test/java/com/galiledu/estudiantes/EstadoEstudianteIT.java`
- `frontend/src/features/estudiantes/components/inscripcion/inscripcion-wizard.tsx`, `estudiantes-lista.tsx`
- `frontend/src/components/ui/dialog/dialogo-confirmacion.tsx` (movido desde Usuarios para compartirlo)

## Decisiones y por qué

- **Decisión:** "inactivo" es el estado `RETIRADO` y "eliminado" es el borrado lógico (`eliminado`). Un estudiante activo, retirado o de otra persona bloquea la inscripción; solo uno eliminado la permite.
  **Por qué:** es la regla del equipo; el documento es único en `personas.persona`, así que no se puede crear una persona nueva con el mismo documento.
  **Alternativas descartadas:** dejar que falle al final (el usuario recorría los 5 pasos para enterarse).
- **Decisión:** con un estudiante eliminado se reutiliza su registro (mismo id): se restaura y se reemplazan sus datos, ficha médica, familia, contactos y autorizados.
  **Por qué:** conserva el historial de matrículas y pagos (cuelgan de `estudiante_id`) y respeta el documento único.
  **Alternativas descartadas:** crear un estudiante nuevo (imposible por el documento único) o conservar la familia anterior (la inscripción nueva trae la suya).
- **Decisión:** retirar o eliminar al estudiante solo cambia su estado; no toca sus matrículas ni sus pagos.
  **Por qué:** esas reglas son del módulo de Matrícula y Pagos y no estaban definidas.

## Pendientes / problemas conocidos

- [ ] "Editar" desde el menú de tres puntos.
- [ ] Qué hacer con la matrícula y los pagos al retirar o eliminar a un estudiante matriculado.
- [ ] Si el documento es de una persona que no es estudiante (padre, apoderado, usuario) solo se muestra que ya existe; no hay trato especial.

## Notas para quien siga

- Las pruebas de integración corren en una sola transacción y el estado "inscrito / matriculado / retirado" es una fórmula SQL: `EstadoEstudianteIT` limpia la caché de Hibernate antes de leer la ficha.
