# Cambio de contraseña con sesión iniciada (HU-5)

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-16
- **HU / tarea:** HU-5 · Cambio de contraseña (iteración 3, módulo Seguridad)
- **Estado:** terminado

## Qué hice

Con la sesión iniciada, el usuario pulsa su tarjeta (abajo en el menú lateral) y elige «Cambiar contraseña». Escribe su contraseña actual, luego la nueva dos veces y pulsa «Cambiar contraseña». Se cierran sus sesiones en todos los dispositivos y vuelve al login con «Contraseña actualizada correctamente». Son las pantallas de Figma › 🌟 Autenticación (fila HU-5).

## Archivos principales

- `backend/src/main/resources/db/migration/V20__sesiones_desde_cambio_contrasena.sql`: columna `seguridad.usuario.sesiones_desde`.
- `backend/.../usuarios/aplicacion/ServicioCambioContrasena.java`; endpoints en `web/AutenticacionController.java`; `Usuario.reemplazarContrasena`; `VerificadorSesionUsuarios` y `compartido/seguridad/VerificadorSesion` (ahora reciben la fecha de emisión del token).
- `frontend/src/features/auth/components/cambiar-contrasena.tsx`, `pantalla-contrasena.tsx` (compartido con HU-4) y `aviso-contrasena.tsx`; ruta `app/routes/app/cambiar-contrasena.tsx`; menú en `components/layouts/dashboard-layout.tsx`.

## Decisiones y por qué

- **Decisión:** «se cerró la sesión en todos tus dispositivos» (texto del modal de Figma) se cumple de verdad: al cambiar la contraseña se guarda `sesiones_desde` y cada petición rechaza los tokens emitidos antes.
  **Por qué:** el JWT no se guarda en el servidor, así que sin esa fecha un token robado seguiría sirviendo hasta vencer. Se compara en segundos porque así viene la fecha de emisión del token.
  **Alternativas descartadas:** una lista de tokens revocados (más tablas y limpieza); solo borrar el token del navegador actual (no cerraría los otros dispositivos).
- **Decisión:** lo mismo pasa al restablecer la contraseña (HU-4), porque las dos usan `Usuario.reemplazarContrasena`. (Cambiar el correo ya no genera una contraseña nueva: ver la [decisión del 2026-11-19](2026-11-19-scuro-star-correo-sin-credenciales-nuevas.md).)
- **Decisión:** una contraseña actual incorrecta cuenta como intento fallido del inicio de sesión: al quinto, la cuenta se bloquea 15 minutos.
  **Por qué:** si no, alguien con una sesión abierta ajena podría probar contraseñas sin límite.
- **Decisión:** «Cambiar contraseña» vuelve a comprobar la contraseña actual (no confía en que se pasó el paso 1) y exige que la nueva sea distinta.
- **Decisión:** la pantalla va en `/app/cambiar-contrasena`, fuera del menú lateral, como en Figma. La X vuelve al inicio del rol.
- **Decisión:** las pantallas de contraseña comparten piezas (`pantalla-contrasena.tsx`: tarjeta, campo con ojo y formulario de nueva contraseña) y el modal del login es uno solo (`AvisoContrasena`) con dos textos.
- **Decisión:** ícono `RefreshCw` para `sync` del menú (agregado a la tabla de equivalencias).

## Notas para quien siga

- Si cambias la contraseña de alguien desde código, usa `Usuario.reemplazarContrasena`: así se cierran sus sesiones.
