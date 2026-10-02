# Backend de GalilEdu

Base técnica del backend. Por ahora no implementa historias de usuario ni expone rutas funcionales.

La organización acordada es un [monolito modular por capas](../docs/arquitectura.md). La primera pieza técnica está en `usuarios/infraestructura/seguridad`: solo permite el chequeo de salud, deniega las demás rutas y deja preparado bcrypt con costo 10. Esto **no** implementa todavía el inicio de sesión.

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

Ejecutar `./mvnw test` o `.\mvnw.cmd test`. La prueba inicial solo verifica que el contexto de Spring arranca. Usa H2 **únicamente en pruebas** para no requerir una instancia de PostgreSQL durante esta verificación; las futuras pruebas de persistencia deberán ejecutarse contra PostgreSQL 18.

Para empaquetar la aplicación, ejecutar `./mvnw package` o `.\mvnw.cmd package`. El archivo resultante será `target/galiledu.jar`.
