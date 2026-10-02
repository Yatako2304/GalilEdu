# GalilEdu

Sistema de gestión escolar. El desarrollo comienza por el [backend](backend/README.md).

La decisión técnica de organización está en [Arquitectura](docs/arquitectura.md): monolito modular por capas.

## Estado actual

- Backend base en Spring Boot 4.1.1 y Java 21.
- PostgreSQL 18 definido como base de datos objetivo.
- Gestión de Usuarios ya tiene reglas de dominio probadas; aún faltan sus casos de uso, persistencia y API para completar las historias de extremo a extremo.

La elección de IDE es libre: IntelliJ IDEA y VS Code usan el mismo código y el mismo Maven Wrapper del proyecto.
