# GalilEdu

Sistema de Información Web para la Gestión Integral de un Colegio de Educación Secundaria.

| Carpeta | Qué hay | Stack |
| --- | --- | --- |
| [`backend/`](backend/README.md) | API REST y migraciones de la BD | Java 21 · Spring Boot 4.1 · PostgreSQL 18 · Flyway · JWT |
| [`frontend/`](frontend/README.md) | Aplicación web por rol | React 18 · Vite · TypeScript · Tailwind · TanStack Query |
| [`docs/`](docs/README.md) | Guías, fichas de módulos y decisiones del equipo | Markdown |

**Diseño:** las pantallas de [Figma](https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu) son la fuente de verdad.

---

## Levantar todo en local

**Necesitas:** Java 21, Node 20+ y PostgreSQL 18.

1. **Base de datos.** Crea una base **vacía** `galiledu` (pgAdmin → Databases → Create). Las tablas y los datos de prueba los crea el backend al arrancar; no ejecutes scripts a mano.
2. **Backend** → http://localhost:8080/api
   ```bash
   cd backend
   copy .env.example .env      # pon tu DB_URL y DB_PASSWORD
   .\mvnw.cmd spring-boot:run
   ```
3. **Frontend** → http://localhost:3000
   ```bash
   cd frontend
   npm install
   copy .env.example .env
   npm run dev
   ```

### Usuarios de prueba

Contraseña de todos: **`Galiledu2026`**.

| Usuario | Persona | Rol(es) |
| --- | --- | --- |
| `U20260001` | Selene Curo | Administrador |
| `U20260002` | Ana Torres | Personal administrativo |
| `U20260003` | Luis Paredes | Apoderado (padre de Mateo) |
| `U20260004` | María Fernández | Docente + Tutor (prueba el selector de rol) |
| `U20260005` | Carlos Ramírez | Coordinador |
| `U20260006` | Valeria Sánchez | Estudiante (matriculada en 5.° A de 2026) |
| `U20260007` | Leonardo Montoya | Administrador **inactivo** (no puede entrar) |

Están en `backend/src/main/resources/db/datos-prueba/R__datos_prueba.sql`. Para empezar de cero, borra y vuelve a crear la base `galiledu`.

---

## Documentación

| Quiero… | Ve a |
| --- | --- |
| Saber qué documento responde qué | [`docs/README.md`](docs/README.md) |
| Ver el estado de cada módulo y sus HU | [`docs/modulos/`](docs/modulos/README.md) |
| Empezar mi módulo (BD → backend → frontend) | [`docs/guias/nuevo-modulo.md`](docs/guias/nuevo-modulo.md) |
| Saber cómo subir cambios (ramas, commits, PR, CI) | [`docs/guias/contribuir.md`](docs/guias/contribuir.md) |
| Escribir y correr pruebas | [`docs/guias/pruebas.md`](docs/guias/pruebas.md) |
| Saber cómo nombrar tablas, clases, endpoints y variables | [`docs/guias/convenciones.md`](docs/guias/convenciones.md) |
| Publicar la app en AWS (EC2) | [`docs/guias/despliegue.md`](docs/guias/despliegue.md) |
| Entender por qué algo se hizo así | [`docs/decisiones/`](docs/decisiones/) |
| Configuración y endpoints del backend | [`backend/README.md`](backend/README.md) |
| Estructura y design system del frontend | [`frontend/README.md`](frontend/README.md) |

Cada pull request a `main` pasa por **GitHub Actions**, que corre todas las pruebas de backend y frontend; si alguna falla, no se integra. Al integrarse, el workflow **Despliegue** publica la nueva versión en el servidor.
