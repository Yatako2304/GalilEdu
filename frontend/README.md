# GalilEdu · Frontend

Sistema de Información Web para la Gestión Integral de un Colegio de Educación Secundaria.

- **Base:** [bulletproof-react](https://github.com/alan2207/bulletproof-react) (`apps/react-vite`)
- **Diseño:** [Figma · GalilEdu](https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu)
- **Stack:** React 18 · TypeScript · Vite · Tailwind · React Router 7 · TanStack Query · React Hook Form + Zod · Axios · MSW (backend simulado) · Vitest · Storybook

---

## 1. Cómo correrlo

```bash
npm install
cp .env.example .env     # en Windows: copy .env.example .env
npm run dev              # http://localhost:3000
```

Necesitas el **backend corriendo** (ver el [README principal](../README.md)): login, usuarios y estudiantes usan la API real en `VITE_APP_API_URL`.
Con `VITE_APP_ENABLE_API_MOCKING=true`, [MSW](https://mswjs.io) solo simula los módulos que **aún no tienen backend** (hoy: horarios). Cuando tu módulo tenga endpoints reales, quita sus handlers de `browserHandlers` en `src/testing/mocks/handlers/index.ts`.

Usuarios de prueba: ver la tabla del [README principal](../README.md#usuarios-de-prueba) (contraseña `Galiledu2026`). Los tests (`npm test`) siguen usando MSW para todo, incluido el login.

### Scripts

| Comando                 | Qué hace                                       |
| ----------------------- | ---------------------------------------------- |
| `npm run dev`           | Servidor de desarrollo                         |
| `npm run build`         | Build de producción (`dist/`)                  |
| `npm run lint`          | ESLint (incluye reglas de arquitectura)        |
| `npm run check-types`   | Revisión de tipos                              |
| `npm test`              | Tests unitarios/integración (Vitest)           |
| `npm run storybook`     | Catálogo de componentes UI                     |
| `npm run generate`      | Genera un componente (plop)                    |

---

## 2. Conexión con el backend

- Contratos: los tipos de los DTO están en `src/types/api.ts` y deben coincidir con los DTO Java (`backend/src/main/java/com/galiledu/*/aplicacion/`). La lista de endpoints está en [backend/README.md](../backend/README.md#api-prefijo-api).
- **Autenticación JWT** (`src/lib/auth.tsx`, `src/lib/api-client.ts`): `POST /auth/login` → `{ jwt, user }`; el token se guarda en `localStorage` y se envía como `Authorization: Bearer <jwt>`. `GET /auth/me` → `{ data: user }`.
- Errores: el backend responde `{ message }` en español y el cliente lo muestra como notificación (salvo en el login, que lo muestra bajo el formulario).
- Listas paginadas: `{ data, meta: { page, size, total, totalPages } }` → usa `PaginationBar` de `src/components/ui/table`.

---

## 3. Estructura (bulletproof-react)

```
src/
├── app/                    # capa de aplicación
│   ├── routes/             #   páginas (una por ruta), agrupadas por rol
│   │   ├── auth/           #   login, recuperar contraseña
│   │   └── app/<rol>/      #   admin, administrativo, coordinador, docente, tutor, apoderado, estudiante
│   ├── provider.tsx        #   React Query, errores, auth, notificaciones
│   └── router.tsx          #   árbol de rutas + RoleGuard por rol
├── assets/                 # logo
├── components/             # componentes compartidos
│   ├── layouts/            #   AuthLayout, DashboardLayout (sidebar por rol), ContentLayout (título de página)
│   ├── ui/                 #   design system: button, form, card, tag, avatar, table, dialog, drawer...
│   └── module-placeholder/ #   pantalla "en construcción" para rutas pendientes
├── config/
│   ├── env.ts              #   variables de entorno validadas con zod
│   ├── paths.ts            #   todas las rutas de la app
│   ├── roles.ts            #   roles y su segmento de URL
│   └── navigation.ts       #   menú lateral de cada rol (copiado de Figma)
├── features/               # MÓDULOS de negocio (uno por carpeta)
│   ├── auth/
│   └── horarios/           #   ← módulo de referencia ya implementado (HU-50/51)
│       ├── api/            #     llamadas al backend + hooks de React Query
│       └── components/     #     componentes del módulo
├── lib/                    # api-client (axios), auth, authorization, react-query
├── testing/                # utilidades de test + backend simulado (MSW)
│   └── mocks/
│       ├── handlers/       #   endpoints simulados, uno por módulo
│       ├── fixtures/       #   datos de ejemplo
│       └── seed.ts         #   usuarios de prueba
├── types/api.ts            # tipos de los DTO del backend
└── utils/
```

### Reglas que ESLint hace cumplir

- Un **feature no importa de otro feature** (si dos módulos comparten algo, súbelo a `components/`, `lib/` o `types/`).
- Dirección única: `app` → `features` → `components/lib/types/utils`. Lo compartido nunca importa de `features` ni de `app`.
- Archivos y carpetas en `kebab-case`.

### Roles y rutas

La URL define el rol activo: `/app/<rol>/<módulo>`. Ejemplo: `/app/estudiante/horarios`.
Si un usuario tiene varios roles (ej. Docente + Tutor), el selector bajo el logo cambia de rol. `RoleGuard` impide entrar a rutas de un rol que el usuario no tiene.

| Rol (Figma)             | Ruta base               | Módulos del menú                                         |
| ----------------------- | ----------------------- | -------------------------------------------------------- |
| Administrador           | `/app/admin`            | Usuarios, Estudiantes                                    |
| Personal Administrativo | `/app/administrativo`   | Matrícula, Pensiones, Año escolar, Competencias, Ambientes físicos |
| Coordinador             | `/app/coordinador`      | Horarios, Calificaciones, Ambientes físicos              |
| Docente                 | `/app/docente`          | Horario, Calificaciones, Asistencia                      |
| Tutor                   | `/app/tutor`            | Horario, Calificaciones, Asistencia                      |
| Apoderado               | `/app/apoderado`        | Datos del Alumno, Horarios, Calificaciones, Matrícula, Pensiones |
| Estudiante              | `/app/estudiante`       | Datos del Alumno, Horarios, Calificaciones               |

---

## 4. Estado de los módulos

El estado de cada módulo (HU, pantallas, endpoints) está en [`docs/modulos/`](../docs/modulos/README.md). Las rutas que aún no se implementan muestran un `ModulePlaceholder` con sus HU y un enlace al frame de Figma.

---

## 5. Cómo implementar un módulo nuevo

Sigue la guía completa (BD → backend → frontend → pruebas) en [`docs/guias/nuevo-modulo.md`](../docs/guias/nuevo-modulo.md). Lo específico del frontend:

1. **Tipos** del contrato en `src/types/api.ts`, iguales a los DTO del backend.
2. **API** en `src/features/<modulo>/api/`: hooks de React Query; las mutaciones invalidan la query al terminar (ver `features/usuarios/api/`).
3. **Componentes** en `src/features/<modulo>/components/`, armados con `src/components/ui` (ver el [catálogo](../docs/guias/componentes-ui.md)) y con los textos del frame de Figma. No copies estilos de un componente: si falta uno de Figma, créalo en `components/ui`.
4. **Ruta**: reemplaza el `ModulePlaceholder` en `src/app/routes/app/<rol>/`. ¿Ruta o rol nuevo? `config/paths.ts`, `config/navigation.ts` y `app/router.tsx`.
5. **Pruebas** en `components/__tests__/` (ver [`docs/guias/pruebas.md`](../docs/guias/pruebas.md)).
6. `npm run lint && npm run check-types && npm test` antes de subir.

---

## 6. Design system

Los tokens vienen de Figma (*🗂️ DESIGN SYSTEM › Foundations*) y están en `tailwind.config.cjs` + `src/index.css`:

- **Colores primitivos:** `deep-blue-*`, `vibrant-blue-*`, `growth-green-*`, `warm-yellow-*`, `turquoise-*`, `slate-*`, `red-*`, `amber-*`.
- **Semánticos:** `bg-surface-*`, `text-content-*`, `border-stroke-*` (ej. `bg-surface-accent-light`, `text-content-accent-primary`).
- **Tipografía (Inter):** `text-h4`, `text-h6`, `text-label-1..3`, `text-body-1..4`, `text-caption-1..2`.
- **Efectos:** `shadow-card` y `shadow-event` (Card-shadow y Event-shadow de Figma).
- **Componentes:** todos los de la página Componentes de Figma están en `src/components/ui`; la tabla Figma → código está en [`docs/guias/componentes-ui.md`](../docs/guias/componentes-ui.md). Revísalos en Storybook (`npm run storybook`).
- **Si agregas un tamaño de letra** en `tailwind.config.cjs`, regístralo también en `src/utils/cn.ts`; si no, `cn()` lo confunde con un color y borra el color del texto.
