# Recuperación de contraseña con código por correo (HU-4)

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-16
- **HU / tarea:** HU-4 · Recuperación de contraseña (iteración 3, módulo Seguridad)
- **Estado:** terminado

## Qué hice

Quien olvidó su contraseña pulsa «¿Olvidaste tu contraseña?» en el login, escribe su correo, recibe un código de 6 dígitos, lo valida y elige una contraseña nueva. Al terminar vuelve al login con el modal «Contraseña restablecida correctamente». Son las pantallas de Figma › 🌟 Autenticación (fila HU-4).

## Archivos principales

- `backend/src/main/resources/db/migration/V19__recuperacion_contrasena.sql`: tabla `seguridad.codigo_recuperacion`.
- `backend/.../usuarios/aplicacion/ServicioRecuperacionContrasena.java`, `infraestructura/CodigosRecuperacion.java`, `dominio/PoliticaContrasena.java`; endpoints en `web/AutenticacionController.java`; correo en `infraestructura/NotificadorCredenciales.java` (`enviarCodigoRecuperacion`).
- `frontend/src/features/auth/components/recuperar-contrasena.tsx` (los 3 pasos), `pantalla-contrasena.tsx` (piezas compartidas con HU-5) y `aviso-contrasena.tsx` (modal); `src/lib/contrasena.ts`; `src/components/ui/form/codigo-verificacion.tsx`.

## Decisiones y por qué

- **Decisión:** un código de 6 dígitos por correo (no un enlace).
  **Por qué:** así está en Figma («Código de verificación», «Reenviar código (60 s)»). Reemplaza el formulario anterior, que pedía un «enlace».
- **Decisión:** el código vence a los 10 minutos, admite 5 intentos, solo vale el último y se puede pedir otro cada 60 segundos. Se guarda su hash (BCrypt), nunca el código.
  **Por qué:** con 6 dígitos, el límite de intentos y el vencimiento son lo que impide adivinarlo. Los 60 segundos son los de Figma y evitan llenar el correo de mensajes.
- **Decisión:** si el correo no es de ninguna cuenta se dice «El correo no está asociado a ninguna cuenta.».
  **Por qué:** es el mensaje de Figma. Permite saber si un correo está registrado. La alternativa más segura, una respuesta igual para todos, la descartamos para seguir el diseño; si el PO la prefiere, basta cambiar `ServicioRecuperacionContrasena.cuentaDe`.
- **Decisión:** «Validar código» comprueba el código y «Restablecer contraseña» lo vuelve a comprobar antes de guardar. No hay un segundo token.
  **Por qué:** es más simple y nadie puede saltarse el paso 2 llamando directo al paso 3.
- **Decisión:** la regla de contraseña es la de Figma: mínimo 8 caracteres, con mayúscula, minúscula, número y carácter especial; sin espacios y con un máximo de 64. Está en `PoliticaContrasena` (backend) y `lib/contrasena.ts` (frontend) para reutilizarla en HU-5.
- **Decisión:** restablecer la contraseña quita el bloqueo por intentos fallidos de inicio de sesión y cierra las sesiones abiertas (igual que HU-5).
  **Por qué:** quien se bloqueó por olvidarla debe poder entrar con la nueva sin esperar 15 minutos.
- **Decisión:** si el servidor de correo rechaza el envío, se responde 503 y no queda ningún código guardado. Sin SMTP configurado, el código se escribe en el log del backend, igual que las credenciales.
- **Decisión:** el correo del código es HTML: logo de GalilEdu en la esquina, franja con los colores del logo y el código grande y centrado. Lleva también una versión de solo texto.
  **Por qué:** se ve como un correo oficial del colegio y el código se lee de un vistazo. Usa tablas y estilos en línea para que se vea igual en Gmail y Outlook. El logo va incrustado en el correo (Content-ID `logo-galiledu`), no como enlace, porque el servidor no tiene una dirección pública fija. La plantilla está en `backend/src/main/resources/correo/codigo-recuperacion.html`. El correo de credenciales de una cuenta nueva usa el mismo diseño (`correo/credenciales.html`): usuario y contraseña grandes en el recuadro, el botón «Ingresar a GalilEdu» (`URL_FRONTEND` + `/auth/login`) y el aviso de cambiar la contraseña.
- **Decisión:** el código se escribe en un componente nuevo `CodigoVerificacion` (`components/ui/form`), que no existía.
- **Decisión:** «Las contraseñas no coinciden.» y «Volver al código de verificación» (cuando el código venció en el paso 3) no están en Figma; se agregaron para que el usuario sepa qué corregir.

## Pendientes / problemas conocidos

- [x] HU-5 reutiliza `PoliticaContrasena`, `lib/contrasena.ts` y `pantalla-contrasena.tsx` (ver su [decisión](2026-11-16-scuro-star-cambio-contrasena.md)).

## Notas para quien siga

- Para probar en local sin correo real, deja `MAIL_PASSWORD` vacío en `backend/.env`: el código sale en la terminal del backend («Código de recuperación de …»). En el modo simulado del frontend (MSW) el código válido es `123456`.
