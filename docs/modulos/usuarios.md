# Usuarios y acceso

- **HU:** HU-1, HU-6, HU-9, HU-12 (iteración 1) · HU-8, HU-10 (2) · HU-3, HU-4, HU-5 (3)
- **Estado:** 🚧 en desarrollo
- **Última actualización:** 2026-10-22

## Alcance

Inicio de sesión de todos los roles y gestión de cuentas por el Administrador. Un usuario puede tener **varios roles** y elige el activo en el menú lateral. Las cuentas no se borran: "Eliminar" es un borrado lógico. El **estudiante no tiene cuenta**: el rol Estudiante existe en el catálogo, pero es un "could have" y no se crea ni se desarrolla en esta iteración (ver [decisión](../decisiones/2026-10-19-yatako-usuarios-y-acceso.md)).

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-1 | Inicio de sesión con usuario y contraseña | ✅ | Bloqueo de 15 min tras 5 intentos (RF-2); redirección por rol (RF-3). |
| HU-6 | Registro de usuario con rol | ✅ | Usuario `U{año}{0001}` y contraseña aleatoria (RF-15, RF-16) enviada por correo (RF-17; sin SMTP va al log). |
| HU-9 | Edición de datos personales | ✅ | Misma validación y control de duplicados (RF-22, RF-23). |
| HU-12 | Desactivación de usuario | ✅ | También reactivación y "Eliminar" (borrado lógico). |
| HU-8 | Carga masiva desde Excel/CSV | ⏸️ | **Oculta en la interfaz** (es un "could have"): se quitaron el botón de Usuarios, el acceso rápido del Inicio y `?carga=1`. El modal (`dialogo-carga-masiva.tsx`), `api/carga-masiva.ts` y los endpoints `/usuarios/carga-masiva` siguen en el código; para reactivarla basta volver a montar el modal en `app/routes/app/admin/usuarios.tsx` y su acceso rápido en `inicio-administrador.tsx`. Cómo funciona: modal de 3 pasos de Figma: plantilla .xlsx, validación por fila sin crear nada, creación de las filas válidas y descarga del detalle de errores (CSV). Mismas reglas que HU-6; varios roles separados por coma; máx. 10 MB y 1000 filas. Diferencia con Figma: el resumen tiene un solo botón "Finalizar" (Figma muestra "Cancelar/Guardar", pero en ese punto los usuarios ya están creados). |
| HU-10 | Cambio de rol | ✅* | Ya se puede desde "Editar usuario"; falta revisar si requiere pantalla propia. |
| HU-4, HU-5 | Recuperar / cambiar contraseña | ⏳ | Pantallas en Figma › Autenticación. |
| HU-3 | Inicio de sesión con Google | ⏳ | |

## Pantallas (Figma)

| Pantalla | Frame en Figma | Ruta |
| --- | --- | --- |
| Login | Autenticación › "Iniciar sesión" | `/auth/login` |
| Inicio del administrador | Administrador › "00 · Home — Administrador" | `/app/admin` |
| Lista de usuarios | Administrador › "Admin / Usuarios \| HU-6 \| HU-7 \| HU-8 - grilla" | `/app/admin/usuarios` |
| Nuevo / editar usuario, confirmaciones, "Usuario registrado" | Administrador › modales "Nuevo Usuario", "Editar usuario"… | modales en `/app/admin/usuarios` |
| Carga masiva (3 pasos) · *oculta* | Administrador › modales "Carga masiva de usuarios" | sin acceso desde la interfaz (antes `/app/admin/usuarios?carga=1`) |

## Base de datos

- **Esquemas:** `personas`, `seguridad` · **Migración:** `V1__personas_y_seguridad.sql`

| Tabla | Para qué |
| --- | --- |
| `personas.persona` | Datos personales de cualquier persona (con o sin cuenta). DNI y correo únicos. |
| `seguridad.usuario` | Cuenta: usuario, contraseña cifrada, estado, borrado lógico, intentos, bloqueo, último acceso. |
| `seguridad.rol` | Catálogo de los 7 roles. |
| `seguridad.usuario_rol` | Roles de cada usuario (varios por usuario). |
| `seguridad.correlativo_usuario` | Último correlativo por año para generar `U2026XXXX` sin duplicados. |

## API

Ver la tabla completa en [backend/README.md](../../backend/README.md#api-prefijo-api): `/auth/*` y `/usuarios/*`.

## Código

- Backend: `backend/src/main/java/com/galiledu/{personas,usuarios}/`, seguridad JWT en `compartido/seguridad/`
- Frontend: `frontend/src/features/{auth,usuarios}/`, `frontend/src/components/personas/`
- Pruebas: `AutenticacionIT`, `GestionUsuariosIT`, `UsuarioTests`, `GeneradorCredencialesTests`; `login-form.test.tsx`, `usuarios-lista.test.tsx`, `persona-schema.test.ts`

## Pendientes

- [ ] Configurar SMTP real para el envío de credenciales.
- [ ] HU-4/HU-5 contraseña, HU-3 Google. El enlace "¿Olvidaste tu contraseña?" del login está oculto (`RECUPERAR_CONTRASENA_DISPONIBLE` en `login-form.tsx`) hasta implementar HU-4: actívalo junto con el endpoint.
- [x] Cortar la sesión al desactivar o eliminar: cada petición verifica que la cuenta siga activa (`compartido/seguridad/VerificadorSesion`).
- [ ] Si se cambian los roles de un usuario conectado, su menú se actualiza recién al volver a iniciar sesión.
