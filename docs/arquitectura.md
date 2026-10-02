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
| Repository (DAO) | Puerto en `aplicacion`, implementación en `infraestructura` | Consultar y guardar agregados. El adaptador demo usa memoria; el definitivo se ajustará al esquema PostgreSQL del equipo de BD. |
| Adapter | `infraestructura` | Conectar correo, pasarela de pagos, S3, SQS y otros servicios sin acoplar el dominio a sus SDK. |
| Inyección de dependencias | Composición de Spring | Entregar al caso de uso las implementaciones de sus puertos. |

Para persistencia elegimos **Repository como variante de DAO**, no dos capas duplicadas de `DAO` y `Repository`. Cuando exista el esquema aprobado, la implementación JPA y sus entidades permanecerán dentro de `infraestructura`; ni el controlador ni otro módulo accederán directamente a ellas. No se definen aquí tablas ni columnas.

El primer flujo está implementado como `AutenticacionController → IniciarSesion / CambiarContrasenaInicial → Dominio`, con puertos `RepositorioAccesos`, `RepositorioCuentas` y `ServicioContrasenas`. El perfil `demo` conecta esos puertos a memoria y bcrypt para probar HTTP sin BD. Fuera de ese perfil, la API funcional permanece cerrada hasta que se integre el acceso a PostgreSQL.

## Límites de trabajo paralelo

- El equipo de base de datos prepara el esquema PostgreSQL 18. Esta rama no contiene SQL, migraciones ni entidades JPA; el acceso a datos se adaptará a su entrega.
- Las llamadas entre módulos deberán expresar operaciones del módulo dueño; no se accederá directamente al repositorio de persistencia de otro módulo.
- El PO validó varios roles por cuenta y contraseña inicial temporal con cambio obligatorio. El flujo de autenticación ya se puede probar en `demo`; faltan el acceso a datos definitivo, el envío real de credenciales y las demás historias de Usuarios. En el perfil normal, solo el chequeo técnico de salud es público.
