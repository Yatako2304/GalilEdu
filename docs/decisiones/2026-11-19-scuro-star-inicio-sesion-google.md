# Inicio de sesión con Google y su interruptor para el administrador

- **Autor:** scuro-star
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-3 (inicio de sesión con Google) y HU-2 (activación por el administrador)
- **Estado:** terminado (falta crear el ID de cliente en Google Cloud para usarlo de verdad)

## Qué hice

- Botón «Iniciar sesión con Google» del login de Figma (debajo de «o»). Solo aparece si Google está activado y el servidor tiene las llaves de Google.
- Nueva opción del Administrador, **Seguridad › Configuración de acceso**, con un interruptor para activar o desactivar Google para todo el colegio. Usuario y contraseña siempre funcionan.
- Backend: `GET /auth/metodos`, `POST /auth/google` y `GET/PUT /seguridad/configuracion-acceso`; tabla `seguridad.configuracion_acceso` (V23).

## Archivos principales

- `backend/src/main/java/com/galiledu/usuarios/aplicacion/ServicioAccesoGoogle.java`, `CuentasGoogle.java`
- `backend/src/main/java/com/galiledu/usuarios/infraestructura/CuentasGoogleOAuth.java`, `ConfiguracionAcceso.java`
- `backend/src/main/java/com/galiledu/usuarios/web/ConfiguracionAccesoController.java`, `AutenticacionController.java`
- `backend/src/main/resources/db/migration/V23__configuracion_acceso.sql`
- `frontend/src/features/auth/components/ingreso-google.tsx`, `frontend/src/lib/google.ts`, `frontend/src/lib/auth.tsx`
- `frontend/src/features/usuarios/components/configuracion-acceso.tsx`, `frontend/src/components/ui/logo-google/`

## Decisiones y por qué

- **Decisión:** el interruptor lo maneja el **Administrador** y solo controla Google; usuario y contraseña no se pueden apagar.
  **Por qué:** así lo dice HU-2 en Notion, y apagar las credenciales podría dejar a todos (incluido el administrador) sin poder entrar.
  **Alternativas descartadas:** dos interruptores (Google y credenciales).
- **Decisión:** la pantalla va en el menú del Administrador, en **Seguridad › Configuración de acceso**, armada con `Card`, `Switch`, `Tag` y `Alert`.
  **Por qué:** Figma no tiene pantalla para HU-2 (el menú del Administrador solo tiene Inicio y Usuarios).
  **Alternativas descartadas:** una tarjeta en el Inicio del administrador.
- **Decisión:** entra la cuenta cuyo **correo registrado en GalilEdu** (`personas.persona.correo`, único y sin distinguir mayúsculas) es el de la cuenta de Google, con el correo **verificado por Google**, de cualquier dominio. No se vinculan cuentas aparte.
  **Por qué:** el escenario E-3 pide verificar que la cuenta de Google corresponda a un usuario; el correo ya es único en la base. Cualquier dominio, porque los apoderados usan Gmail personal.
  **Alternativas descartadas:** aceptar solo el dominio del colegio; guardar un identificador de Google por usuario.
- **Decisión:** la cuenta desactivada o bloqueada por intentos fallidos no entra con Google (mismos mensajes que con contraseña; `ServicioAutenticacion.iniciarSesionVerificada`). Un correo sin cuenta responde 404 con «No se encontró una cuenta de GalilEdu asociada a este correo de Google.».
  **Por qué:** Google no debe saltarse lo que el administrador decidió sobre la cuenta.
- **Decisión:** flujo de **código de autorización en ventana emergente** de Google Identity Services (`initCodeClient`, `ux_mode: 'popup'`). El backend canjea el código con la clave secreta en `oauth2.googleapis.com/token` y valida el `id_token` (firma con las llaves públicas de Google, emisor, audiencia y vencimiento) con el `NimbusJwtDecoder` que ya trae Spring Security.
  **Por qué:** permite el botón con el diseño de Figma (el botón oficial de Google no se puede personalizar) y no expone tokens de Google en el navegador.
  **Alternativas descartadas:** el botón oficial «Sign in with Google» (no coincide con Figma); recibir un token de acceso en el frontend.
- **Decisión:** el botón aparece solo si el administrador lo activó **y** el servidor tiene `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET`. Sin las llaves no se puede activar (409) y el interruptor queda bloqueado con un aviso.
  **Por qué:** evita un botón que no funciona.
- **Decisión:** la migración es **V23**, no V21.
  **Por qué:** la rama `feat/asistencia-registro` usa V19 y V20, que ya están en `main` (HU-4 y HU-5), y debe renumerarlas a V21 y V22.

## Librerías o herramientas nuevas

Ninguna. El backend usa `RestClient` y `NimbusJwtDecoder` (ya incluidos); el frontend carga el script de Google (`https://accounts.google.com/gsi/client`) solo cuando Google está activado.

## Pendientes / problemas conocidos

- [ ] Crear el ID de cliente OAuth de Google Cloud (tipo «Aplicación web», orígenes `http://localhost:3000` y la URL pública) y poner `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` en `backend/.env` y en `deploy/.env` del servidor. Nunca en el repositorio.
- [ ] Mientras la app de Google Cloud esté en modo «Prueba», solo entran las cuentas agregadas como usuarios de prueba.

## Notas para quien siga

- En las pruebas Google se simula: `InicioSesionGoogleIT` reemplaza `CuentasGoogle` con `@MockitoBean`; en el frontend se simula `window.google`.
- En modo simulado (MSW) el interruptor funciona, pero `POST /auth/google` siempre responde que no hay cuenta asociada.
- Los endpoints públicos del login se piden sin token (`sinSesion` en `api-client.ts`): un token vencido haría que Spring los rechace.
