# «Mis datos» para cualquier rol y «Datos de la Familia» del apoderado

- **Autor:** scuro-star
- **Fecha:** 2026-11-19
- **HU / tarea:** Ampliación de HU-9 (no está en el backlog de Notion, que solo cubre la edición por el Administrador)
- **Estado:** terminado

## Qué hice

Cualquier usuario, sin importar el rol, ve y edita sus propios datos personales en «Mis datos». Se entra pulsando la tarjeta del usuario abajo en el menú lateral: antes abría un desplegable con solo «Cambiar contraseña»; ahora lleva directo a «Mis datos» (`/app/<rol>/mis-datos`). La pantalla muestra primero los datos de solo lectura; los campos aparecen recién con «Editar datos» («Cancelar» descarta lo escrito, «Guardar cambios» guarda y vuelve a la vista). Al final, un botón lleva a «Cambiar contraseña» (HU-5, sin cambios). Además, el ítem «Datos del Alumno» del menú del apoderado pasó a ser «Familia», que abre «Datos de la Familia» (`/app/apoderado/familia`), de solo vista, como en Figma.

## Archivos principales

- `backend/src/main/java/com/galiledu/usuarios/aplicacion/ServicioUsuarios.java` (`misDatos`, `actualizarMisDatos`)
- `backend/src/main/java/com/galiledu/usuarios/web/AutenticacionController.java` (`GET`/`PUT /auth/mis-datos`)
- `backend/src/test/java/com/galiledu/usuarios/MisDatosIT.java`
- `frontend/src/features/auth/components/mis-datos.tsx`, `frontend/src/app/routes/app/mis-datos.tsx`
- `frontend/src/lib/auth.tsx` (`useMisDatos`, `useActualizarMisDatos`)
- `frontend/src/components/layouts/dashboard-layout.tsx` (tarjeta del usuario ahora es un link directo)
- `frontend/src/config/paths.ts`, `frontend/src/app/router.tsx`
- `backend/src/main/java/com/galiledu/usuarios/aplicacion/ServicioMiFamilia.java`, `VinculosUsuario.java` y `infraestructura/VinculosUsuarioSql.java` (`GET /auth/mi-familia`), `MiFamiliaIT.java`
- `frontend/src/components/personas/datos-persona-vista.tsx` (vista de solo lectura), `frontend/src/features/estudiantes/components/familia-apoderado.tsx`, `frontend/src/app/routes/app/apoderado/familia.tsx`, `frontend/src/config/navigation.ts`
- `frontend/src/components/ui/lista-datos/` (`ListaDatos`, nuevo)
- `frontend/src/testing/mocks/handlers/auth.ts`

## Decisiones y por qué

- **Decisión:** nuevo endpoint `/auth/mis-datos` (en `AutenticacionController`, sin restricción de rol) en vez de reusar `/usuarios/{id}` del Administrador.
  **Por qué:** `UsuarioController` tiene `@PreAuthorize("hasRole('ADMINISTRADOR')")` a nivel de clase; mezclar autoedición ahí habría exigido una excepción por endpoint. `AutenticacionController` ya es el lugar de las acciones sobre la propia cuenta (HU-5), y el `id` se saca del JWT (`ServicioJwt.usuarioActual`), no de la URL, para que nadie pueda editar los datos de otra persona cambiando el id.
  **Alternativas descartadas:** abrir `/usuarios/{id}` a cualquier rol cuando `id` es el propio — más simple, pero mezcla permisos de administración con autoedición en el mismo controlador y expone roles/estado de cuenta en la misma respuesta si no se filtra con cuidado.
- **Decisión:** el formulario reusa `PersonaFields` y `personaUsuarioSchema`, los mismos del formulario "Editar usuario" del Administrador, sin los campos de roles ni estado de cuenta.
  **Por qué:** mismas reglas de formato (DRY, ya validadas en HU-6/HU-9) sin duplicar lógica; evita que un usuario sin ese permiso pueda cambiarse sus propios roles.
- **Decisión:** no hay verificación de duplicados en vivo en el frontend (el Administrador la tiene vía `GET /estudiantes/personas?documento=`); se depende del 409 del backend, mostrado automáticamente por el interceptor de errores de `api-client.ts`.
  **Por qué:** ese endpoint de verificación en vivo es solo para ADMINISTRADOR/PERSONAL_ADMINISTRATIVO y no se puede reusar para los demás roles sin abrirlo también a ellos (lo que se evitó para no ampliar su superficie).
- **Decisión:** el `PUT /auth/mis-datos` devuelve el `UsuarioSesion` completo (no solo los datos de persona), y el frontend lo escribe directo en la caché de React Query con la clave `['authenticated-user']` (la misma que usa internamente `react-query-auth`).
  **Por qué:** así el nombre que aparece en la tarjeta del menú lateral se actualiza al guardar, sin recargar la página ni volver a pedir `/auth/me`.

- **Decisión:** la ruta cuelga del rol activo (`/app/docente/mis-datos`, `/app/admin/mis-datos`…) y se agrega a todos los roles dentro de `roleRoutes`, en vez de una ruta suelta `/app/mis-datos`.
  **Por qué:** el menú lateral toma el rol activo del segmento `/app/<rol>` (`useActiveRole`) y, sin rol, no se dibuja: en `/app/mis-datos` la pantalla quedaba sin menú ni tarjeta de usuario. Dentro de `roleRoutes` además queda protegida por el `RoleGuard` de cada rol.
  **Alternativas descartadas:** recordar el último rol activo (en `sessionStorage`) para usarlo fuera de `/app/<rol>`: agrega estado oculto y cambia el comportamiento del menú en toda la app.
- **Decisión:** el ícono de la tarjeta «Contraseña» usa `IconBadge` (también se corrigió en «Configuración de acceso», HU-2), en vez de copiar sus clases.
  **Por qué:** `AGENTS.md` pide no copiar estilos de un componente de `components/ui`.

- **Decisión:** primero se ven los datos y se editan recién con «Editar datos» (antes la pantalla abría directo con el formulario).
  **Por qué:** pedido de Selene: con el formulario siempre abierto es fácil cambiar algo por accidente, o escribir y olvidar guardar. El formulario se monta al pulsar «Editar datos», así siempre parte de lo guardado.
- **Decisión:** la familia del apoderado tiene su propia pantalla, «Datos de la Familia», en el ítem **«Familia»** del menú lateral (antes «Datos del Alumno»), y no dentro de «Mis datos». Es **de solo vista**, aunque Figma dibuja campos editables y «Guardar».
  **Por qué:** pedido de Selene: «Mis datos» debe ser igual para todos (solo los datos personales), y el menú de Figma del apoderado ya tiene «Familia». La familia no se edita («eso lo puede editar, no la familia»): los datos de los demás familiares los corrige el colegio desde la ficha del estudiante. El backend lo protege con `@PreAuthorize("hasRole('APODERADO')")`. «Datos del Alumno» conserva su ruta y se sigue abriendo desde el acceso rápido «Estudiante» del Inicio.
  **Alternativas descartadas:** mostrar la familia dentro de «Mis datos» con el rol Apoderado activo (fue la primera versión; mezclaba los datos propios con los de otras personas).
- **Decisión:** la familia no es una tabla: se arma con la inscripción de cada estudiante a cargo (padre, madre, apoderado, contactos de emergencia). Los hermanos con el mismo padre, madre y apoderado forman una sola familia, nombrada con los apellidos del estudiante; los contactos repetidos entre hermanos se muestran una vez.
  **Por qué:** es lo que guarda la inscripción (`estudiantes.vinculo_familiar`). Se lee con SQL de solo lectura desde `usuarios` (como «estudiantes a cargo»), porque `estudiantes` ya depende de `usuarios`.
  **Diferencias con Figma:** la primera pestaña se llama «Hijos(as)» (no «Familia») y lista los estudiantes a cargo y sus contactos de emergencia. No muestra distrito ni dirección: en la base el domicilio es de cada persona, no de la familia (padre y madre pueden vivir en lugares distintos, y cada uno se ve en su pestaña). No hay departamento, provincia, urbanización ni teléfono de la familia porque la inscripción no los guarda. Los datos del vínculo que cambian por estudiante (vive con él, patria potestad, responsable de matrícula) no se muestran. El selector de familia solo aparece si hay más de una. Los contactos de emergencia se registran por estudiante; se juntan los de los hermanos sin repetir, así que no se ve de cuál hijo es cada uno.
- **Decisión:** nuevo componente `ListaDatos` en `components/ui` para los pares etiqueta / valor de solo lectura.
  **Por qué:** ese estilo ya estaba escrito a mano en `ficha-estudiante.tsx` y `vinculos-usuario.tsx`; `AGENTS.md` pide crear en `components/ui` lo que falte en vez de copiar estilos. Esas dos fichas pueden pasarse a `ListaDatos` más adelante.

## Pendientes / problemas conocidos

- [ ] Correr `.\mvnw.cmd verify` en una máquina con salida a Maven Central (ver nota abajo): incluye Spotless (formato), `MisDatosIT` y `MiFamiliaIT`.
- [ ] Contactos de emergencia: decidir si se muestran por hijo (hoy se juntan los de los hermanos y no se ve de quién es cada uno).
- [ ] Pasar `ficha-estudiante.tsx` y `vinculos-usuario.tsx` a `ListaDatos`.

## Notas para quien siga

- Esta pantalla y el flujo de acceso **no están en Figma** (que solo tiene login, recuperación y cambio de contraseña) ni en el backlog de Notion de HU-9 (que es explícitamente "Como administrador..."). Se construyó con componentes del sistema de diseño (`Card`, `Button`, `PersonaFields`), igual que se hizo con "Configuración de acceso" en HU-2.
- Verificación: el frontend pasó `lint`, `check-types` y todas las pruebas. El backend se revisó a mano (firmas, datos de prueba que usan las pruebas, largo de línea de 120 y orden de imports de Spotless) y se pasó por `javac` para detectar errores de tipos en el código nuevo, pero `mvnw verify` no se pudo correr en el entorno de Claude porque no tiene salida a Maven Central.
