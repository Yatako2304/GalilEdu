# Convenciones de código

Cómo nombrar las cosas en BD, backend y frontend. Si una regla no está aquí, copia lo que ya hacen `usuarios` y `estudiantes`. El **formato** no se discute: lo aplican herramientas (Prettier en el frontend, Spotless en el backend, `.editorconfig` en el editor) en cada commit y en el CI. Esta guía cubre lo que una herramienta no puede decidir: los nombres.

## Reglas generales

1. **Dominio en español, framework en inglés.** Lo que viene del colegio (usuario, matrícula, sección, pensión) va en español; lo que viene de la librería se queda como está (`@Service`, `useQuery`, `onClick`, `Controller`).
2. **Sin tildes ni `ñ` en nombres de código:** `anio`, `numeroDocumento`, `contrasena`, `Inscripcion`. Las tildes van solo en textos que ve el usuario y en comentarios.
3. **El mismo nombre en las tres capas.** La columna `numero_documento` es `numeroDocumento` en Java, en el JSON y en TypeScript. Nunca `nroDoc` en un lado y `documento` en otro.
4. **Nombres completos, sin abreviaturas:** `fechaNacimiento`, no `fNac`; `apellidoPaterno`, no `apPat`. Excepciones aceptadas: `id`, `dni`, `url`.
5. **Mensajes al usuario** en español, tuteo, con punto final y con el texto exacto de Figma cuando exista: "El correo ya está registrado.", "Debe ingresar su usuario."

## Base de datos (PostgreSQL)

| Qué | Convención | Ejemplo |
| --- | --- | --- |
| Esquema | Un esquema por módulo, singular y en minúsculas | `matricula`, `pagos`, `seguridad` |
| Tabla | `snake_case`, **singular** | `matricula.periodo_matricula`, `seguridad.usuario_rol` |
| Columna | `snake_case` | `fecha_inicio`, `numero_documento` |
| Clave primaria | `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` | |
| Clave foránea | `<tabla>_id` | `estudiante_id`, `persona_id` |
| Booleano | Pregunta en afirmativo: `es_`, `tiene_`, `ha_` | `es_exalumno`, `tiene_alergias`, `ha_convulsionado` |
| Fechas | `fecha_<qué>` (`DATE`), `<qué>_en` (`TIMESTAMPTZ`) | `fecha_nacimiento`, `creado_en`, `actualizado_en` |
| Estados y tipos | `VARCHAR` + `CHECK` con valores en MAYÚSCULAS | `estado IN ('ACTIVO', 'INACTIVO')` |
| Restricciones | `ck_<tabla>_<regla>`, `ux_<tabla>_<campos>`, `ix_<tabla>_<campos>` | `ck_usuario_estado`, `ux_persona_documento` |
| Migración | `V{n}__<modulo>_<tema>.sql`, nunca editar una ya subida | `V3__matricula_periodos.sql` |

## Backend (Java)

**Paquetes:** `com.galiledu.<modulo>.{dominio, aplicacion, infraestructura, web}`, en minúsculas y en español.

| Qué | Convención | Ejemplo |
| --- | --- | --- |
| Entidad | Sustantivo singular | `Usuario`, `Estudiante`, `VinculoFamiliar` |
| Enum | Singular; valores en `MAYUSCULAS_CON_GUION` | `EstadoUsuario.ACTIVO`, `Rol.PERSONAL_ADMINISTRATIVO` |
| Servicio | `Servicio<Plural o tema>` | `ServicioUsuarios`, `ServicioAutenticacion` |
| Repositorio | `<Entidad>Repositorio` | `UsuarioRepositorio` |
| Controlador | `<Entidad>Controller` | `UsuarioController`, `EstudianteController` |
| DTO (`record`) | Qué representa, sin sufijo `Dto` | `SolicitudUsuario`, `UsuarioResumen`, `UsuarioDetalle` |
| Agrupador de DTOs | `Dtos` / `Dtos<Modulo>` | `Dtos.java`, `DtosEstudiantes.java` |
| Métodos | `camelCase`, verbo en infinitivo | `crearCuenta()`, `buscarPorDocumento()`, `desactivar()` |
| Consultas booleanas | `es…`, `tiene…`, `puede…` | `puedeIniciarSesion()`, `tieneCuenta()` |
| Constantes | `MAYUSCULAS` | `MAX_INTENTOS`, `DURACION_BLOQUEO` |
| Pruebas | `<Clase>Tests` (unitarias), `<Tema>IT` (integración); métodos que se leen como frase | `UsuarioTests.seBloqueaQuinceMinutosAlQuintoIntentoFallido()` |
| Errores | `ExcepcionNegocio.invalido / conflicto / noEncontrado("mensaje")` | |

## API REST

| Qué | Convención | Ejemplo |
| --- | --- | --- |
| Ruta | Sustantivo en **plural**, español, `kebab-case` | `/usuarios`, `/estudiantes`, `/periodos-matricula` |
| Recurso | `/<plural>/{id}` | `GET /usuarios/{id}` |
| Sub-recurso | `/<plural>/{id}/<plural>` | `/estudiantes/{id}/contactos` |
| Acción que no es CRUD | `POST /<plural>/{id}/<verbo>` | `POST /usuarios/{id}/desactivar` |
| Métodos | `GET` lee, `POST` crea o ejecuta una acción, `PUT` reemplaza, `DELETE` elimina | |
| JSON | `camelCase`, mismos nombres que el DTO | `{"numeroDocumento": "41234567"}` |
| Filtros y paginación | `q` (texto), filtros por nombre de campo, `page` (desde 1), `size` | `/usuarios?q=ana&estado=ACTIVO&page=1&size=8` |
| Lista paginada | `{ data: [...], meta: { page, size, total, totalPages } }` | `Pagina.de(...)` |
| Error | `{ message, errores: { campo: mensaje } }` | |
| Códigos | 200 ok · 201 creado · 204 sin contenido · 400 inválido · 401 sin sesión · 403 sin permiso · 404 no existe · 409 duplicado | |

Excepción: `/auth/login`, `/auth/me` y `/auth/logout` se quedan así porque es la convención que el frontend ya usaba.

## Frontend (TypeScript / React)

ESLint ya obliga a que **archivos y carpetas** estén en `kebab-case` y a que un feature no importe de otro.

| Qué | Convención | Ejemplo |
| --- | --- | --- |
| Carpeta de feature | Módulo en `kebab-case` | `features/usuarios`, `features/anio-escolar` |
| Archivo | `kebab-case` | `dialogo-usuario.tsx`, `get-usuarios.ts` |
| Componente | `PascalCase`, en español | `UsuariosLista`, `DialogoUsuario`, `FichaEstudiante` |
| Ruta (página) | `<Algo>Route` con `export default` | `AdminUsuariosRoute` |
| Hook de consulta | `use<Recurso>` | `useUsuarios`, `useFichaEstudiante` |
| Hook de cambio | `use<Verbo><Recurso>` | `useCrearUsuario`, `useCambiarEstadoUsuario` |
| Función de API | `get<Recurso>`, `<verbo><Recurso>` | `getUsuarios`, `buscarPersonaPorDocumento` |
| Tipos | `PascalCase`, **iguales al DTO Java** | `UsuarioResumen`, `SolicitudInscripcion` |
| Esquema zod | `<algo>Schema` | `personaSchema`, `inscripcionSchema` |
| Props | `<Componente>Props` | `UsuariosListaProps` |
| Variables y funciones | `camelCase`, en español | `rolesElegidos`, `alternarRol()` |
| Booleanos | `es…`, `tiene…`, `puede…`, o el estado (`activo`, `cargando`) | `soloLectura`, `sinPadres` |
| Manejadores | `on<Evento>` en props, verbo en el componente | `onCerrar={cerrar}` |
| Constantes de catálogo | `MAYUSCULAS` | `TIPOS_DOCUMENTO`, `DISTRITOS_LIMA` |
| Pruebas | `__tests__/<archivo>.test.ts(x)`, descripción en español | `test('el DNI debe tener 8 dígitos', …)` |

Excepción heredada: `ROLES` (`src/config/roles.ts`) y algunos componentes base de `src/components/ui` vienen de bulletproof-react en inglés; no se renombran.

## Git

Ramas y commits: ver [contribuir.md](contribuir.md) (`feat/matricula-periodos`, `feat(matricula): registrar periodo`).

## Qué evitar

- Mezclar idiomas en un mismo nombre: ~~`getEstudiantesList`~~ → `getEstudiantes`.
- Sufijos que no aportan: ~~`UsuarioDTO`~~, ~~`IServicioUsuarios`~~, ~~`UsuarioEntity`~~.
- Nombres genéricos: ~~`data`~~, ~~`info`~~, ~~`temp`~~, ~~`handleClick2`~~. Di qué es: `usuarioCreado`, `abrirEditar`.
- Números mágicos: ~~`if (intentos >= 5)`~~ → `if (intentos >= MAX_INTENTOS)`.
- Comentarios que repiten el código. Comenta el **por qué** (la HU, la regla de negocio, la pantalla de Figma).
