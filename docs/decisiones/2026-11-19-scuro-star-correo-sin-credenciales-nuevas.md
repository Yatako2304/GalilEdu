# Cambiar el correo no cambia el usuario ni la contraseña

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-7 (editar la inscripción) y HU-9 (editar usuario)
- **Estado:** terminado

## Qué hice

Antes, al editar la inscripción y cambiar el correo de un padre, madre o apoderado con cuenta, el sistema le generaba una contraseña nueva y se la enviaba al correo nuevo. Ahora conserva su usuario y su contraseña: no se genera nada ni se envía ningún correo. En «Editar usuario» del administrador ya funcionaba así.

## Archivos principales

- `backend/.../estudiantes/aplicacion/ServicioEstudiantes.java` (`actualizarFamiliar`).
- `backend/.../usuarios/aplicacion/ServicioUsuarios.java`: se quitó `reenviarCredenciales`, que ya nadie usaba.
- `backend/src/test/.../estudiantes/EdicionEstudianteIT.java`: la prueba ahora comprueba que la persona sigue entrando con sus credenciales de siempre y que no se envía ningún correo.

## Decisiones y por qué

- **Decisión:** el cambio de correo no toca la cuenta.
  **Por qué:** la persona ya tiene su usuario y su contraseña; cambiarlos sin que lo pida la deja sin acceso hasta que revise el correo nuevo y cierra sus sesiones. Si olvidó su contraseña, la recupera con HU-4 («¿Olvidaste tu contraseña?»), que envía el código al correo actual.
  **Alternativas descartadas:** enviar solo un aviso de «tu correo cambió» (no lo pidió nadie y sería un correo nuevo del sistema).
