# Arquitectura acordada de GalilEdu

## Estilo

GalilEdu tendrá un frontend React y **un único backend Spring Boot desplegable** (`galiledu.jar`). Usaremos **Spring MVC para la interfaz HTTP** y organizaremos el backend como **monolito modular por capas**. Son decisiones compatibles: MVC describe la interacción web; los módulos y capas ordenan el código interno. Un módulo funcional no es un microservicio ni requiere su propio servidor o base de datos.

En este proyecto la **vista** es la aplicación React; el **controlador** es un `@RestController` de Spring MVC que recibe HTTP y devuelve JSON; el **modelo** comprende los datos y reglas del dominio que el caso de uso emplea y traduce a DTOs de respuesta. No usaremos plantillas HTML renderizadas por Spring. `Service` y `Repository` del esquema clásico apoyan al modelo y al controlador, pero no son letras adicionales de «MVC». [Spring MVC documenta sus controladores anotados, incluidos `@RestController`](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html).

La vista de despliegue sitúa Nginx delante del backend y PostgreSQL en RDS. S3, SQS y los servicios externos se integrarán desde el backend cuando las historias correspondientes se implementen. Esta decisión técnica no agrega funcionalidades al backlog validado por el PO.

## Organización interna del backend

Cada módulo funcional que se implemente tendrá estas capas bajo `com.galiledu.<modulo>`:

| Capa | Responsabilidad |
| --- | --- |
| `api` | Entrada HTTP: controladores, solicitudes y respuestas. |
| `aplicacion` | Casos de uso y coordinación de operaciones. |
| `dominio` | Modelo y reglas de negocio respaldadas por el backlog. |
| `persistencia` | Adaptadores de almacenamiento: repositorio temporal en memoria y, después, implementación JPA ajustada al esquema aprobado. |
| `infraestructura` | Seguridad técnica e integraciones externas que no son almacenamiento, como correo o pasarela de pagos. |

La organización es **por módulo primero y por responsabilidad después**. Así no se mezclan, por ejemplo, todos los controladores de Matrícula, Usuarios y Pagos en una sola carpeta global. La estructura existente es:

```text
src/main/java/com/galiledu/
├── GalilEduBackendApplication.java
├── usuarios/
│   ├── api/                  # HTTP y DTO de entrada/salida
│   ├── aplicacion/           # Casos de uso
│   │   └── puertos/          # Contratos requeridos por los casos de uso
│   ├── dominio/              # Modelo y reglas
│   ├── persistencia/demo/    # Adaptador temporal en memoria
│   └── infraestructura/seguridad/
└── horarios/
    └── dominio/              # Primeras reglas; aún no hay API ni repositorio
```

El esquema didáctico `controllers → services → repositories → entities → base de datos` se interpreta aquí así: `controllers` corresponde a `api`; `services` a `aplicacion` y, para las reglas, a `dominio`; `repositories` se divide en el **contrato** de `aplicacion/puertos` y el **adaptador** de `persistencia`; `entities` no es una capa de ejecución. Una entidad del negocio vive en `dominio`, mientras que una clase `@Entity` para mapear tablas, si se necesita, vivirá en `persistencia` cuando exista el esquema. Los DTOs HTTP van en `api`; configuración y excepciones se ubican junto a la responsabilidad que atienden, no en carpetas globales por defecto.

Las dependencias previstas son `api → aplicacion → dominio`; `persistencia` implementa los puertos que necesita `aplicacion`. El dominio no conoce HTTP, Spring ni PostgreSQL. Las pruebas bajo `src/test/java` reflejan los mismos paquetes de producción.

Esta distribución por funcionalidad sigue el ejemplo de [estructura de código de Spring Boot 4.1](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html); la separación entre interfaz Repository y clase mapeada se apoya en la [referencia de Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/getting-started.html). Las subcarpetas internas y los nombres en español son la convención acordada para GalilEdu, no una obligación del framework.

Los módulos previstos por la vista de componentes son `usuarios`, `matricula`, `pagos`, `horarios`, `academica` y `asistencia`. Configuración institucional se incorporará cuando se aborde su alcance. Las carpetas y clases se crearán al implementar cada historia, evitando componentes vacíos o reglas inventadas.

## Criterios de modelado y encapsulación

- Las cuentas y asignaciones horarias tienen identidad y transiciones: se modelan como clases con campos `private final` y métodos que expresan operaciones del dominio. No se generan setters públicos ni se envían estas clases directamente como respuestas HTTP.
- Los `record` se reservan para datos de solicitud/respuesta, resultados inmutables y objetos de valor sin identidad. Un componente de `record` genera un campo privado y un método de lectura público; no equivale a un atributo público mutable. Las colecciones se copian defensivamente cuando corresponde.
- Los casos de uso reciben sus dependencias por constructor e implementan reglas sin conocer PostgreSQL. Las interfaces de repositorio son contratos, no implementaciones de persistencia.
- El hash de contraseña solo se usa internamente para verificar o guardar credenciales; no se incluye en DTOs HTTP ni en `toString()`. Los adaptadores de persistencia futuros no deben filtrar el modelo de base de datos a la API.

Estos criterios siguen la [especificación de `record` de Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html), la [recomendación de inyección por constructor de Spring Boot](https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html) y la distinción entre [entidades y objetos de valor en DDD](https://learn.microsoft.com/en-us/azure/architecture/microservices/model/tactical-domain-driven-design). La última fuente presenta DDD en microservicios; aquí aplicamos únicamente el criterio de modelado de dominio dentro de un monolito modular.

## Patrones para implementar los módulos

La **capa de negocio** no será una carpeta adicional: comprende `aplicacion` (casos de uso) y `dominio` (reglas e invariantes). Separarlas evita mezclar una regla como «una cuenta nueva exige cambiar su contraseña» con la coordinación de correo, repositorio y transacciones.

| Patrón | Ubicación | Uso en GalilEdu |
| --- | --- | --- |
| Controller / DTO | `api` | Recibir solicitudes y devolver respuestas; no guardar datos ni decidir reglas del negocio. |
| Application Service / Use Case | `aplicacion` | Ejecutar una historia, coordinar el dominio y las dependencias, y definir la transacción. |
| Entidades y Value Objects | `dominio` | Mantener estados y validaciones propios del negocio sin depender de Spring o JPA. |
| Repository (DAO) | Puerto en `aplicacion`, implementación en `persistencia` | Consultar y guardar agregados. El adaptador demo usa memoria; el definitivo se ajustará al esquema PostgreSQL del equipo de BD. |
| Adapter | `infraestructura` | Conectar correo, pasarela de pagos, S3, SQS y otros servicios sin acoplar el dominio a sus SDK. |
| Inyección de dependencias | Composición de Spring | Entregar al caso de uso las implementaciones de sus puertos. |

Para persistencia elegimos **Repository como variante de DAO**, no dos capas duplicadas de `DAO` y `Repository`. Cuando exista el esquema aprobado, la implementación JPA y sus entidades de mapeo permanecerán dentro de `persistencia`; ni el controlador ni otro módulo accederán directamente a ellas. No se definen aquí tablas ni columnas.

El primer flujo está implementado como `AutenticacionController → IniciarSesion / CambiarContrasenaInicial → Dominio`, con puertos `RepositorioAccesos`, `RepositorioCuentas` y `ServicioContrasenas`. El perfil `demo` conecta esos puertos a memoria y bcrypt para probar HTTP sin BD. Fuera de ese perfil, la API funcional permanece cerrada hasta que se integre el acceso a PostgreSQL.

En `horarios/dominio` comenzó la validación de cruces de docente y aula de RF-117/RF-118, sin API ni acceso a datos todavía. La validación recibe asignaciones existentes; la operación transaccional de consultar, validar y guardar se incorporará al desarrollar la historia completa.

## Límites de trabajo paralelo

- El equipo de base de datos prepara el esquema PostgreSQL 18. Esta rama no contiene SQL, migraciones ni entidades JPA; el acceso a datos se adaptará a su entrega.
- Las llamadas entre módulos deberán expresar operaciones del módulo dueño; no se accederá directamente al repositorio de persistencia de otro módulo.
- El PO validó varios roles por cuenta y contraseña inicial temporal con cambio obligatorio. El flujo de autenticación ya se puede probar en `demo`; faltan el acceso a datos definitivo, el envío real de credenciales y las demás historias de Usuarios. En el perfil normal, solo el chequeo técnico de salud es público.
