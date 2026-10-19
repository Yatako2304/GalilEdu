# GalilEdu

Sistema de Información Web para la Gestión Integral de un Colegio de Educación Secundaria.

| Carpeta | Qué hay | Stack |
| --- | --- | --- |
| [`backend/`](backend/README.md) | API REST + migraciones de la BD | Java 21 · Spring Boot 4.1 · PostgreSQL 18 · Flyway · JWT |
| [`frontend/`](frontend/README.md) | Aplicación web por rol | React 18 · Vite · TypeScript · Tailwind · TanStack Query |

**Fuente de verdad del diseño:** las pantallas de [Figma](https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu). Los requisitos de Notion son apoyo; si se contradicen, manda Figma.

---

## Levantar todo en local (≈5 minutos)

**Necesitas:** Java 21, Node 20+ y PostgreSQL 18.

### 1. Base de datos

Crea una base **vacía** llamada `galiledu` (pgAdmin → Databases → Create, o `CREATE DATABASE galiledu;`).
No ejecutes scripts a mano: las tablas y los datos de prueba los crea el backend al arrancar (Flyway).

### 2. Backend → http://localhost:8080/api

```bash
cd backend
copy .env.example .env      # y pon tu DB_URL / DB_PASSWORD
.\mvnw.cmd spring-boot:run
```

### 3. Frontend → http://localhost:3000

```bash
cd frontend
npm install
copy .env.example .env
npm run dev
```

### 4. Entrar

Todas las cuentas de prueba usan la contraseña **`Galiledu2026`**:

| Usuario | Persona | Rol(es) |
| --- | --- | --- |
| `U20260001` | Selene Curo | Administrador |
| `U20260002` | Ana Torres | Personal administrativo |
| `U20260003` | Luis Paredes | Apoderado (padre de Mateo) |
| `U20260004` | María Fernández | Docente + Tutor (prueba el selector de rol) |
| `U20260005` | Carlos Ramírez | Coordinador |
| `U20260006` | Valeria Sánchez | Estudiante |
| `U20260007` | Leonardo Montoya | Administrador **inactivo** (no puede entrar) |

Los datos de prueba están en `backend/src/main/resources/db/datos-prueba/R__datos_prueba.sql`.
Para empezar de cero: borra y vuelve a crear la base `galiledu` y arranca el backend.

---

## Qué está hecho (Iteración 1 · Usuarios y acceso)

| HU | Qué | Dónde |
| --- | --- | --- |
| HU-1 | Login con usuario y contraseña, bloqueo 15 min tras 5 intentos, redirección por rol | `/auth/login` |
| HU-6 | Registro de usuario con uno o varios roles; usuario `U{año}{0001}` y contraseña generados y enviados por correo | Admin → Usuarios → Nuevo usuario |
| HU-7 | Inscripción del estudiante en 5 pasos con padres/apoderado; el responsable recibe la cuenta de la familia | Personal administrativo → Estudiantes |
| HU-9 | Edición de datos personales y roles | Admin → Usuarios → Editar |
| HU-12 | Desactivar / reactivar cuenta (y "Eliminar" como borrado lógico) | Admin → Usuarios |

El resto de módulos del frontend siguen como *placeholder* (ver `frontend/README.md`, sección 4).

## Cómo seguir (para cada pareja)

1. Trabaja en una rama a partir de `feat/integracion`.
2. **BD:** agrega un archivo nuevo `backend/src/main/resources/db/migration/V{n}__descripcion.sql` (nunca edites uno ya aplicado). Usa un esquema por módulo (`matricula`, `pagos`…).
3. **Backend:** crea tu módulo en `backend/src/main/java/com/galiledu/<modulo>/` copiando la estructura de `usuarios` (`dominio`, `aplicacion`, `infraestructura`, `web`). Protege los endpoints con `@PreAuthorize("hasRole('...')")`.
4. **Frontend:** crea tu feature en `frontend/src/features/<modulo>/` copiando `usuarios` o `estudiantes`; los tipos del contrato van en `frontend/src/types/api.ts`.
5. Antes de subir: `.\mvnw.cmd test` en `backend/` y `npm run lint && npm run check-types && npm test` en `frontend/`.

> La carpeta [`docs/`](docs/README.md) describe el backend anterior (antes de rehacer la BD); se conserva solo como referencia.
