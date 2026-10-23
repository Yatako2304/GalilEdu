# Secciones por grado y tutores (HU-75, backend)

- **Autor:** valeriaacevedop
- **Fecha:** 2026-10-22
- **HU / tarea:** HU-75 (también deja listo `listarSeccionesActivasConVacantes` para HU-23)
- **Estado:** terminado como MVP (el frontend está en [la entrada del 2026-10-23](2026-10-23-valeriaacevedop-frontend-secciones.md))

## Qué hice

Backend de las secciones de cada grado en un año escolar: listar por grado, crear, cambiar la capacidad, asignar tutor y cotutor, eliminar, inactivar y reactivar, y listar los docentes elegibles. Incluye el conteo de alumnos que ocupan vacante (`@Formula` con `Seccion.SQL_OCUPA_VACANTE`). No hay migraciones nuevas ni frontend en este PR.

## Archivos principales

- `backend/src/main/java/com/galiledu/configuracion/dominio/Seccion.java`
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/ServicioSecciones.java`
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/MatriculasDeSecciones.java` y `DirectorioDocentes.java` (interfaces)
- `backend/src/main/java/com/galiledu/configuracion/infraestructura/MatriculasDeSeccionesSql.java` y `DirectorioDocentesSql.java` (adaptadores)
- `backend/src/main/java/com/galiledu/configuracion/web/SeccionController.java`
- Pruebas: `backend/src/test/java/com/galiledu/configuracion/` (`SeccionIT`, `ServicioSeccionesIT`, `MatriculasDeSeccionesSqlIT`, `DirectorioDocentesSqlIT`, `dominio/SeccionTests`)

## Decisiones y por qué

- **Decisión:** HU-75 es un MVP: el historial de tutoría y el "Vigente desde" quedan **fuera del MVP (acordado)**, no son deuda de la HU. Si se retoman, irán con la migración V9.
  **Por qué:** necesitan tablas nuevas. Mantener el MVP sin migraciones lo hace revisable y suficiente para las pantallas. La sección guarda solo el tutor y el cotutor actuales.
  **Alternativas descartadas:** meter la migración y el historial en este mismo PR.

- **Decisión:** `configuracion` lee las matrículas y los docentes con SQL de solo lectura, detrás de las interfaces `MatriculasDeSecciones` y `DirectorioDocentes`.
  **Por qué:** esos módulos todavía no ofrecen un servicio. Con la interfaz, el servicio de secciones no conoce el SQL: cuando exista el servicio, solo se reemplaza el adaptador (`MatriculasDeSeccionesSql`, `DirectorioDocentesSql`). Cada adaptador tiene su prueba directa.
  **Alternativas descartadas:** consultar esas tablas desde `ServicioSecciones` o desde el repositorio, que dejaría el SQL ajeno repartido por el módulo.

- **Decisión:** una sección solo se elimina si no tiene matrículas (en cualquier estado) ni cambios de sección (como origen o destino). Si los tiene, responde 409 y se inactiva. Al eliminar, `academico.docente_seccion` se borra en cascada.
  **Por qué:** una sección con historial no puede desaparecer sin perder trazabilidad; inactivarla evita que reciba más alumnos y no borra nada. La cascada de `docente_seccion` es la que ya define el esquema.
  **Alternativas descartadas:** eliminar siempre, o impedir la eliminación si hay asignaciones de docentes por curso.

- **Decisión:** la regla "un docente es tutor de una sola sección por año" aplica solo al tutor y solo a secciones ACTIVAS. El cotutor no cuenta, y las secciones inactivas no ocupan al tutor.
  **Por qué:** un cotutor apoya sin ser el responsable de la sección. Una sección inactiva no recibe alumnos, así que no debe bloquear al docente. Al reactivar se valida la regla: si el tutor ya lo es de otra sección activa, responde 409 y no se le quita el tutor automáticamente. La validación se hace antes de modificar la sección y bajo un bloqueo (`pg_advisory_xact_lock`) por año, para que dos peticiones simultáneas no asignen al mismo tutor.
  **Alternativas descartadas:** contar también las secciones inactivas y al cotutor; quitar el tutor en silencio al reactivar.

## Pendientes / problemas conocidos

- [x] Historial de tutoría y "Vigente desde" (migración V9): fuera del MVP (acordado).
- [x] Frontend de CI-09 y los modales "Nueva sección" y "Asignar tutor".
- [ ] Avisar al equipo de Académico de que eliminar una sección borra en cascada sus asignaciones de docentes por curso (`academico.docente_seccion`). Inactivar no borra nada.
- [ ] Cuando Matrícula y Usuarios ofrezcan un servicio, reemplazar `MatriculasDeSeccionesSql` y `DirectorioDocentesSql`.
- [ ] El manejador de errores compartido responde 500 ante un UUID mal escrito en un parámetro (por ejemplo `?anioEscolarId=abc`). Solo se cubrió el parámetro ausente en `tutores-disponibles`.

## Notas para quien siga

- El dominio lanza `IllegalArgumentException` (400) e `IllegalStateException` (409); `ServicioSecciones` los traduce a `ExcepcionNegocio`. El cliente solo recibe `{ message, errores }`.
- Se valida antes de modificar la sección: las consultas de JPA hacen flush automático y una excepción no deshace los cambios dentro de una transacción de prueba. Si agregas una regla nueva, mantén ese orden.
- Los mensajes de las pruebas de eliminación dependen del texto de `ServicioSecciones`; las pruebas de `MatriculasDeSeccionesSqlIT` existen porque la restricción de la base de datos daría el mismo 409 aunque la consulta fallara.
