# Backend de GalilEdu

Backend en Java 21 y Spring Boot 4.1.1, organizado como [monolito modular por capas](../docs/arquitectura.md). PostgreSQL 18 es la base de datos objetivo, pero el equipo de BD aún está definiendo su esquema. Este repositorio no define tablas, migraciones ni un adaptador de acceso a datos definitivo.

El primer corte de Gestión de Usuarios incluye dominio, puertos Repository/DAO, casos de uso para iniciar sesión y cambiar la contraseña temporal, y una API HTTP. Para poder ejecutarlos antes de recibir el esquema, el perfil `demo` usa un repositorio **volátil en memoria**. Se pierde toda la información al reiniciar y **no debe desplegarse en producción**.

## Ejecutar el flujo demo sin base de datos

Instala JDK 21 y, desde esta carpeta, configura `DEMO_ADMIN_TEMP_PASSWORD` como variable de entorno del proceso o de la configuración de ejecución del IDE. Debe ser una contraseña de prueba; no la guardes en Git. Luego ejecuta:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

El usuario de prueba es `U<anio_actual>0001` (por ejemplo, `U20260001` en 2026). Se genera una sola cuenta con rol `ADMINISTRADOR` y cambio inicial de contraseña obligatorio.

El flujo HTTP es:

1. `GET /api/auth/csrf` para obtener el encabezado y token CSRF, conservando la cookie de sesión.
2. `POST /api/auth/login` con `{"nombreUsuario":"U20260001","contrasena":"..."}` y el encabezado CSRF. Conserva la misma cookie.
3. `GET /api/auth/me` para consultar roles y si el cambio de contraseña sigue pendiente.
4. `POST /api/auth/initial-password` con `{"contrasenaTemporal":"...","nuevaContrasena":"..."}` y CSRF.
5. `POST /api/auth/logout` con CSRF para cerrar la sesión.

`GET /actuator/health` permite comprobar el arranque. La cuenta demo y sus cambios no persisten. El bloqueo tras cinco intentos fallidos dura quince minutos; la sesión expira tras treinta minutos de inactividad. El adaptador en memoria no representa las garantías transaccionales ni de concurrencia que habrá de proporcionar el adaptador definitivo.

## Integración posterior con PostgreSQL

Los casos de uso dependen de `RepositorioCuentas` y `RepositorioAccesos`, no de tablas. Cuando el equipo entregue el esquema, implementaremos esos puertos en `infraestructura`, conectaremos los casos de uso y la API al perfil real y probaremos la integración contra PostgreSQL 18. No se debe reutilizar el repositorio demo para producción.

Los valores `DB_URL`, `DB_USER` y `DB_PASSWORD` del [ejemplo de entorno](.env.example) están reservados para esa integración. Spring Boot no carga `.env.example` automáticamente. No almacenar credenciales reales en Git.

## Pruebas y empaquetado

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Hay pruebas unitarias del dominio y de los casos de uso, además de pruebas HTTP del perfil `demo`. El artefacto generado es `target/galiledu.jar`.
