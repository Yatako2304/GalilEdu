# GalilEdu

Sistema de gestión escolar. El desarrollo comienza por el [backend](backend/README.md).

La decisión técnica de organización está en [Arquitectura](docs/arquitectura.md): monolito modular por capas.

## Estado actual

- Backend base en Spring Boot 4.1.1 y Java 21.
- PostgreSQL 18 definido como base de datos objetivo.
- Gestión de Usuarios tiene un primer flujo de login y cambio de contraseña ejecutable en el perfil `demo`, sin BD. El acceso a datos definitivo se integrará cuando el equipo entregue el esquema PostgreSQL.

La elección de IDE es libre: IntelliJ IDEA y VS Code usan el mismo código y el mismo Maven Wrapper del proyecto.
