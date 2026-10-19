# Cómo crear un módulo

Ejemplo: la pareja de **Matrícula** empieza HU-23. Copia siempre el patrón de los módulos que ya funcionan: `usuarios` (CRUD con modales) y `estudiantes` (formulario por pasos). Los nombres siguen [convenciones.md](convenciones.md).

## 0. Antes de programar

1. Abre las pantallas del módulo en [Figma](https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu): de ahí salen los campos, textos, mensajes y flujos.
2. Copia [`docs/modulos/_plantilla.md`](../modulos/_plantilla.md) como `docs/modulos/matricula.md` y llena *Alcance* y *Pantallas*. Agrégalo al índice [`docs/modulos/README.md`](../modulos/README.md).
3. Crea tu rama: `feat/matricula-periodos`.

## 1. Base de datos

- Archivo nuevo: `backend/src/main/resources/db/migration/V{siguiente}__matricula_periodos.sql`.
- Un **esquema por módulo**: `CREATE SCHEMA IF NOT EXISTS matricula;` y tablas `matricula.<tabla>`. Agrega el esquema a `spring.flyway.schemas` en `application.properties`.
- Nombres en español y `snake_case`; `id UUID DEFAULT gen_random_uuid()`; `creado_en`/`actualizado_en`; reglas simples como `CHECK` o `UNIQUE`.
- Puedes referenciar tablas de otros módulos (`REFERENCES estudiantes.estudiante (id)`).
- **Nunca edites una migración ya subida**: si te equivocaste, crea otra `V{n+1}`.
- Datos de ejemplo para la demo: agrégalos a `db/datos-prueba/R__datos_prueba.sql` (idempotentes, con `ON CONFLICT DO NOTHING`).

## 2. Backend

Paquete `backend/src/main/java/com/galiledu/matricula/`:

| Carpeta | Qué va | Ejemplo a copiar |
| --- | --- | --- |
| `dominio/` | Entidades JPA y reglas del negocio | `usuarios/dominio/Usuario.java` |
| `infraestructura/` | Repositorios (`JpaRepository`) y acceso a datos | `usuarios/infraestructura/UsuarioRepositorio.java` |
| `aplicacion/` | Servicios (`@Service`, `@Transactional`) y DTOs (`record`) | `usuarios/aplicacion/ServicioUsuarios.java`, `Dtos.java` |
| `web/` | Controladores REST | `usuarios/web/UsuarioController.java` |

Reglas:
- Errores de negocio: `throw ExcepcionNegocio.conflicto("mensaje en español")`. El frontend muestra ese mensaje tal cual.
- Permisos: `@PreAuthorize("hasAnyRole('PERSONAL_ADMINISTRATIVO')")` en el controlador.
- Listas paginadas: devuelve `Pagina.de(...)` (`{ data, meta }`).
- Para usar otro módulo, llama a su **servicio** (ej. `ServicioUsuarios`), no a sus repositorios.
- Agrega tus endpoints a la tabla de [`backend/README.md`](../../backend/README.md).

## 3. Frontend

1. Tipos del contrato en `frontend/src/types/api.ts` (iguales a tus DTOs Java).
2. Feature en `frontend/src/features/matricula/`:
   - `api/`: hooks de React Query (ver `features/usuarios/api/`).
   - `components/`: pantallas y modales con los componentes de `src/components/ui` y los tokens de Tailwind (no inventes colores ni tamaños).
3. Ruta en `src/app/routes/app/<rol>/...` reemplazando el `ModulePlaceholder`, y en `src/config/paths.ts` / `navigation.ts` si hace falta.
4. Si tu módulo tenía handlers de MSW en `src/testing/mocks/handlers/`, quítalo de `browserHandlers` cuando el backend ya responda.
5. Un feature **no importa de otro feature** (ESLint lo bloquea). Lo compartido va en `src/components`, `src/config` o `src/utils`.

## 4. Pruebas

- Backend: `MatriculaIT extends PruebaIntegracion` con los casos de la HU (éxito, validaciones, permisos).
- Frontend: pruebas de tus esquemas zod y de los componentes principales.
- Detalle en [pruebas.md](pruebas.md).

## 5. Cierre

- Actualiza `docs/modulos/matricula.md` (endpoints, tablas, estado, pendientes).
- Si decidiste algo que afecta a otros, crea `docs/decisiones/AAAA-MM-DD-autor-tema.md`.
- Abre el PR siguiendo [contribuir.md](contribuir.md).
