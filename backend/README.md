# Backend de GalilEdu

Base técnica del backend. Ya incluye reglas de dominio de Usuarios, pero todavía no expone rutas funcionales ni implementa historias de extremo a extremo.

La organización acordada es un [monolito modular por capas](../docs/arquitectura.md). En `usuarios/dominio` están las validaciones de RF-12, el formato de RF-15 y el estado de cuenta con varios roles, cambio inicial obligatorio y desactivación lógica. En `usuarios/infraestructura/seguridad` solo se permite el chequeo de salud, se deniegan las demás rutas y se deja preparado bcrypt con costo 10. Esto **no** implementa todavía el registro, la persistencia ni el inicio de sesión.

El primer caso de uso en `usuarios/aplicacion` es `CambiarContrasenaInicial`. Se comunica mediante `RepositorioCuentas` (puerto Repository/DAO) y `ServicioContrasenas` (puerto de hash). El adaptador bcrypt ya implementa el segundo; las pruebas usan un repositorio en memoria solo dentro del test. Falta implementar el adaptador JPA, conectar el caso de uso a Spring y exponerlo por API tras resolver la autenticación. Por ahora ninguna ruta funcional está abierta.

## Versiones

- Java 21 LTS.
- Spring Boot 4.1.1 (línea 4.1.x).
- PostgreSQL 18 para desarrollo y despliegue.
- Maven 3.9.16, descargado por Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Ejecutar localmente

1. Instalar JDK 21 y PostgreSQL 18; crear una base de datos y un usuario para GalilEdu.
2. Definir `DB_URL`, `DB_USER` y `DB_PASSWORD` como variables de entorno o en la configuración de ejecución del IDE. [`.env.example`](.env.example) muestra el formato; Spring Boot no lo carga automáticamente.
3. Desde esta carpeta, ejecutar `./mvnw spring-boot:run` en macOS/Linux o `.\mvnw.cmd spring-boot:run` en PowerShell.
4. Comprobar `http://localhost:8080/actuator/health`.

No almacenar contraseñas ni cadenas de conexión reales en Git. El esquema todavía no existe: las migraciones se añadirán junto con la primera funcionalidad de datos; JPA está configurado para **validar**, no para crear ni modificar tablas.

## Pruebas

Ejecutar `./mvnw test` o `.\mvnw.cmd test`. Hay pruebas del dominio de Usuarios, de la configuración HTTP y del arranque de Spring. H2 se usa **únicamente en pruebas** para no requerir una instancia de PostgreSQL durante esta verificación; las futuras pruebas de persistencia deberán ejecutarse contra PostgreSQL 18.

Para empaquetar la aplicación, ejecutar `./mvnw package` o `.\mvnw.cmd package`. El archivo resultante será `target/galiledu.jar`.
