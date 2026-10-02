# Arquitectura acordada de GalilEdu

## Estilo

GalilEdu tendrá un frontend React y **un único backend Spring Boot desplegable** (`galiledu.jar`). El backend se organizará como **monolito modular por capas**. Un módulo funcional no es un microservicio ni requiere su propio servidor o base de datos.

La vista de despliegue sitúa Nginx delante del backend y PostgreSQL en RDS. S3, SQS y los servicios externos se integrarán desde el backend cuando las historias correspondientes se implementen. Esta decisión técnica no agrega funcionalidades al backlog validado por el PO.

## Organización interna del backend

Cada módulo funcional que se implemente tendrá estas capas bajo `com.galiledu.<modulo>`:

| Capa | Responsabilidad |
| --- | --- |
| `api` | Entrada HTTP: controladores, solicitudes y respuestas. |
| `aplicacion` | Casos de uso y coordinación de operaciones. |
| `dominio` | Modelo y reglas de negocio respaldadas por el backlog. |
| `infraestructura` | Persistencia, seguridad técnica e integraciones externas. |

Los módulos previstos por la vista de componentes son `usuarios`, `matricula`, `pagos`, `horarios`, `academica` y `asistencia`. Configuración institucional se incorporará cuando se aborde su alcance. Las carpetas y clases se crearán al implementar cada historia, evitando componentes vacíos o reglas inventadas.

## Límites de trabajo paralelo

- El equipo de base de datos puede preparar el esquema PostgreSQL 18 y sus migraciones sin que otros módulos dependan todavía de tablas concretas.
- Las llamadas entre módulos deberán expresar operaciones del módulo dueño; no se accederá directamente al repositorio de persistencia de otro módulo.
- La autenticación y la política de varios roles siguen sujetas a las decisiones vigentes del PO. Hasta implementarlas, la API funcional permanece cerrada por defecto; solo el chequeo técnico de salud puede consultarse sin autenticar.
