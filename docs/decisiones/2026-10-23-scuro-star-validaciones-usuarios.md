# Validaciones de "Nuevo / Editar usuario" y reingreso de usuarios eliminados

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-10-23
- **HU / tarea:** HU-6, HU-9, HU-12
- **Estado:** terminado

## Qué hice

Validaciones del formulario de usuario según los criterios CAHU6-* de Notion. Roles Apoderado y Tutor de solo lectura en el modal. Si se registra el documento de un usuario eliminado, se reactiva esa cuenta en lugar de dar error. Los avisos (toasts) ahora salen por delante de los modales. En "Ver/Editar usuario" se muestran, solo de vista, los estudiantes a cargo de un apoderado y las secciones de un tutor.

## Archivos principales

- `frontend/src/components/personas/persona-schema.ts`: `personaUsuarioSchema`.
- `frontend/src/components/personas/persona-fields.tsx`: filtros de entrada, tipo de documento y "País de nacimiento".
- `frontend/src/features/usuarios/components/dialogo-usuario.tsx`: roles de solo lectura.
- `frontend/src/config/catalogos.ts`: `NACIONALIDADES` = Peruana / Extranjera.
- `frontend/src/components/ui/notifications/notifications.tsx`: `z-[100]`.
- `frontend/src/features/usuarios/components/vinculos-usuario.tsx`: estudiantes a cargo y tutoría.
- `frontend/src/features/usuarios/api/verificar-documento.ts`: documento repetido.
- `backend/src/main/java/com/galiledu/usuarios/aplicacion/ServicioUsuarios.java` y `dominio/Usuario.java`: `crear`, `eliminar`, `restaurar`.

## Decisiones y por qué

- **Decisión:** el número de documento se valida aunque haya otros errores (`z.custom().superRefine()` en vez de `.superRefine()` sobre el objeto).
  **Por qué:** zod no ejecuta el `superRefine` del objeto si algún campo falla, y el error del DNI no aparecía junto a los demás.
- **Decisión:** en "Nuevo usuario" el campo es "País de nacimiento" y la nacionalidad no tiene valor por defecto. En las demás pantallas sigue "Lugar de nacimiento".
  **Por qué:** así lo pidió el equipo para el formulario de usuarios.
- **Decisión:** Apoderado y Tutor se muestran con candado y no se pueden marcar ni quitar desde el modal.
  **Por qué:** se asignan al inscribir un estudiante y al designar tutor de una sección. Al guardar se conservan.
- **Decisión:** al eliminar un usuario se pone en `null` el correo de su persona; si luego se registra el mismo documento, se reactiva esa cuenta (mismo código `U…`) con los datos, roles y una contraseña nueva. Para quien registra es igual que crear un usuario.
  **Por qué:** el DNI y el correo de `personas.persona` son únicos, así que un eliminado bloqueaba volver a registrar a esa persona.
  **Alternativas descartadas:** borrar la persona (rompe el historial) o mostrar un error pidiendo reactivarla.

- **Decisión:** los datos adicionales del apoderado (relación, vive con el estudiante, patria potestad, responsable de matrícula, documento) se muestran dentro de cada estudiante a cargo, no como campos de la persona.
  **Por qué:** se guardan por vínculo (`estudiantes.vinculo_familiar`), así que pueden ser distintos para cada estudiante. Se modifican desde la ficha del estudiante, no desde Usuarios.
- **Decisión:** el detalle del usuario (`GET /usuarios/{id}`) traerá `estudiantesACargo` y `tutorDe`, en lugar de que Usuarios llame a las APIs de Estudiantes y Secciones.
  **Por qué:** un feature no importa de otro y el Administrador no tiene acceso a `/configuracion/secciones` (solo Personal administrativo).
  **Alternativas descartadas:** dar al Administrador acceso a secciones.
- **Decisión:** el tutor solo ve las secciones del año escolar actual.
  **Por qué:** es lo que importa al gestionar la cuenta; los años anteriores quedan en Configuración.

- **Decisión:** el documento repetido se revisa al pulsar Guardar con `GET /estudiantes/personas?documento=` y se muestra debajo del campo, junto con los demás errores, siempre con el mismo mensaje: "Ya existe un usuario registrado con este documento".
  **Por qué:** antes el formulario parecía correcto y el error recién salía como notificación cuando respondía el backend. No hay un endpoint de Usuarios para esto y la búsqueda de usuarios no filtra por documento.
  **Cambio en el backend:** esa respuesta (`PersonaEncontrada`) ahora trae `cuentaEliminada` (`ServicioUsuarios.cuentaEliminada`), para que el documento de un usuario eliminado no dé error al crear.

## Pendientes / problemas conocidos

- [ ] Backend de `estudiantesACargo` y `tutorDe` en `GET /usuarios/{id}` (por ahora solo está el frontend; las secciones salen vacías).
- [ ] El correo es de la persona, no de la cuenta: si esa persona también es padre o apoderado de un estudiante, al eliminar su usuario también pierde el correo ahí.

## Notas para quien siga

- La reactivación es interna: no hay mensaje ni pantalla para ella y no tiene pruebas propias.
