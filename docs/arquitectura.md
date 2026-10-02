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

## Patrones para implementar los módulos

La **capa de negocio** no será una carpeta adicional: comprende `aplicacion` (casos de uso) y `dominio` (reglas e invariantes). Separarlas evita mezclar una regla como «una cuenta nueva exige cambiar su contraseña» con la coordinación de correo, repositorio y transacciones.

| Patrón | Ubicación | Uso en GalilEdu |
| --- | --- | --- |
| Controller / DTO | `api` | Recibir solicitudes y devolver respuestas; no guardar datos ni decidir reglas del negocio. |
| Application Service / Use Case | `aplicacion` | Ejecutar una historia, coordinar el dominio y las dependencias, y definir la transacción. |
| Entidades y Value Objects | `dominio` | Mantener estados y validaciones propios del negocio sin depender de Spring o JPA. |
| Repository (DAO) | Puerto en `aplicacion`, implementación en `infraestructura` | Consultar y guardar agregados. La implementación puede usar Spring Data JPA cuando el esquema PostgreSQL esté listo. |
| Adapter | `infraestructura` | Conectar correo, pasarela de pagos, S3, SQS y otros servicios sin acoplar el dominio a sus SDK. |
| Inyección de dependencias | Composición de Spring | Entregar al caso de uso las implementaciones de sus puertos. |

Para persistencia elegimos **Repository como variante de DAO**, no dos capas duplicadas de `DAO` y `Repository`. La interfaz de Spring Data JPA y las entidades JPA permanecerán dentro de `infraestructura`; ni el controlador ni otro módulo accederán directamente a ellas. Los puertos y adaptadores se crearán al implementar el primer caso de uso que los necesite, sin asumir todavía nombres de tablas o columnas.

Flujo orientativo, **no implementado aún**: `Controller → Caso de uso → Dominio`; el caso de uso invoca un puerto `Repository`, cuya implementación JPA accede a PostgreSQL.

## Límites de trabajo paralelo

- El equipo de base de datos puede preparar el esquema PostgreSQL 18 y sus migraciones sin que otros módulos dependan todavía de tablas concretas.
- Las llamadas entre módulos deberán expresar operaciones del módulo dueño; no se accederá directamente al repositorio de persistencia de otro módulo.
- El PO validó las decisiones de varios roles por cuenta y contraseña inicial temporal con cambio obligatorio. El dominio de `usuarios` ya representa esas reglas, pero aún falta integrarlo con persistencia, casos de uso y autenticación HTTP. Hasta entonces, la API funcional permanece cerrada por defecto; solo el chequeo técnico de salud puede consultarse sin autenticar.
