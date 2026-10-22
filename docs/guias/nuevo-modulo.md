# Cómo programar un módulo

Ejemplo: te toca **Matrícula**. La base de datos de la Iteración 1 **ya está hecha** (migraciones V1 a V7, con datos de prueba), igual que la ficha de cada módulo. Tu trabajo es el **backend** y el **frontend** encima de esas tablas. Copia el patrón de los módulos que ya funcionan: `usuarios` (CRUD con modales) y `estudiantes` (formulario por pasos). Los nombres siguen [convenciones.md](convenciones.md).

## 0. Antes de programar

1. Lee la ficha de tu módulo en [`docs/modulos/`](../modulos/README.md). Ahí están las HU, las pantallas de Figma, las tablas y **qué reglas valida la BD y cuáles tu servicio**.
2. Abre esas pantallas en [Figma](https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu). De ahí salen los campos, textos, mensajes y flujos. Si Figma y un RF de Notion se contradicen, manda Figma.
3. Revisa [`docs/decisiones/`](../decisiones/), sobre todo [integración de la BD](../decisiones/2026-10-19-yatako-integracion-bd.md).
4. Haz `git pull` en `main` y arranca el backend: Flyway crea las tablas y carga los datos de prueba.
5. Crea tu rama: `feat/matricula-periodos`.

## 1. Base de datos (ya existe)

- Las tablas de tu módulo están en `backend/src/main/resources/db/migration/` (V4 configuración, V5 matrícula y pagos, V6 catálogo académico, V7 ajustes). **No las cambies desde Java** (`ddl-auto` está apagado): tus entidades deben calzar con las columnas.
- **Si falta una columna o una tabla, no edites una migración existente.** Crea una **nueva** con el siguiente número libre (`V8__matricula_observaciones.sql`) y avisa en el grupo para que nadie use el mismo número. El CI prueba las migraciones contra PostgreSQL real.
- Datos de ejemplo extra: agrégalos a `db/datos-prueba/R__datos_prueba.sql` (idempotentes, con `ON CONFLICT DO NOTHING`).

### Columnas que necesitan un mapeo especial en JPA

| Caso | Ejemplo | Cómo mapearlo |
| --- | --- | --- |
| Tabla en el esquema de tu módulo | `matricula.matricula` | `@Table(schema = "matricula", name = "matricula")` |
| Estados guardados como texto | `estado VARCHAR` con `CHECK` | `@Enumerated(EnumType.STRING)` con un `enum` de los mismos valores |
| Arreglos | `dias_lectivos`, `recursos` | `@JdbcTypeCode(SqlTypes.ARRAY) private List<String> diasLectivos;` |
| Columna calculada por la BD | `pagos.cuota.monto_final` | `@Column(insertable = false, updatable = false)` |
| Dato de otro módulo que solo se lee | nombre del grado, situación del estudiante | `@Formula("(SELECT …)")`. Ver `estudiantes/dominio/Estudiante.java` |
| Referencia a otro módulo | `seccion_id`, `estudiante_id` | Guarda el `UUID`; no hagas `@ManyToOne` a entidades de otro módulo (salvo `Persona`, que es compartida) |

## 2. Backend

Paquete `backend/src/main/java/com/galiledu/matricula/`:

| Carpeta | Qué va | Ejemplo a copiar |
| --- | --- | --- |
| `dominio/` | Entidades JPA y reglas del negocio | `usuarios/dominio/Usuario.java` |
| `infraestructura/` | Repositorios (`JpaRepository`) y acceso a datos | `usuarios/infraestructura/UsuarioRepositorio.java` |
| `aplicacion/` | Servicios (`@Service`, `@Transactional`) y DTOs (`record`) | `usuarios/aplicacion/ServicioUsuarios.java`, `Dtos.java` |
| `web/` | Controladores REST | `usuarios/web/UsuarioController.java` |

Reglas:
- Errores de negocio: `throw ExcepcionNegocio.conflicto("mensaje en español")`. El frontend muestra ese mensaje tal cual. Valida en el servicio **antes** de guardar, para que el usuario reciba un mensaje claro y no un error de la BD.
- Permisos: `@PreAuthorize("hasAnyRole('PERSONAL_ADMINISTRATIVO')")` en el controlador.
- Listas paginadas: devuelve `Pagina.de(...)` (`{ data, meta }`).
- Para usar otro módulo, llama a su **servicio** (ej. `ServicioUsuarios`), no a sus repositorios.
- Agrega tus endpoints a la tabla de [`backend/README.md`](../../backend/README.md).

## 3. Frontend

1. Tipos del contrato en `frontend/src/types/api.ts` (iguales a tus DTOs Java).
2. Feature en `frontend/src/features/matricula/`:
   - `api/`: hooks de React Query (ver `features/usuarios/api/`).
   - `components/`: pantallas y modales armados **solo** con los componentes de `src/components/ui` y los tokens de Tailwind (no inventes colores ni tamaños). Busca cada pieza de Figma en el [catálogo de componentes](componentes-ui.md); si falta, créala en `components/ui`, no en tu módulo.
3. Tu ruta **ya existe**: en `src/app/routes/app/<rol>/` hay un `ModulePlaceholder` con el enlace a su frame de Figma. Reemplázalo. Para una ruta nueva usa `src/config/paths.ts`, `navigation.ts` y `app/router.tsx`.
4. Si tu módulo tenía handlers de MSW en `src/testing/mocks/handlers/`, quítalo de `browserHandlers` cuando el backend ya responda.
5. Un feature **no importa de otro feature** (ESLint lo bloquea). Lo compartido va en `src/components`, `src/config` o `src/utils`.

## 4. Pruebas

- Backend: `MatriculaIT extends PruebaIntegracion` con los casos de la HU (éxito, cada validación y que otro rol reciba `403`).
- Frontend: pruebas de tus esquemas zod y de los componentes principales.
- Detalle en [pruebas.md](pruebas.md).

## 5. Cierre

- Actualiza `docs/modulos/matricula.md` (endpoints, estado, pendientes) y su fila en el índice.
- Si decidiste algo que afecta a otros, crea `docs/decisiones/AAAA-MM-DD-autor-tema.md`.
- Abre el PR siguiendo [contribuir.md](contribuir.md).
