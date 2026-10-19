# Arquitectura base del frontend

- **Autor:** equipo (entrada inicial)
- **Fecha:** 2026-10-18
- **HU / tarea:** configuración inicial del proyecto
- **Estado:** terminado

## Qué hice

Se dejó listo el esqueleto del frontend: rutas por rol, login, layout con menú lateral, design system tomado de Figma, backend simulado y un módulo de referencia (`horarios`, HU-50/51) para copiar al crear los demás.

## Archivos principales

- `src/app/router.tsx`: árbol de rutas y `RoleGuard`.
- `src/config/roles.ts`, `src/config/paths.ts`, `src/config/navigation.ts`: roles, rutas y menú.
- `src/lib/api-client.ts`, `src/lib/auth.tsx`: axios y autenticación.
- `src/components/ui/`: design system.
- `src/features/horarios/`: módulo de referencia.
- `src/testing/mocks/`: backend simulado (MSW).

## Decisiones y por qué

- **Decisión:** estructura basada en **bulletproof-react** (`app` → `features` → `components/lib/types/utils`).
  **Por qué:** cada módulo de negocio queda aislado en `features/`, y ESLint impide que un feature importe de otro. Así varias personas pueden trabajar en módulos distintos sin pisarse.
- **Decisión:** el **rol activo va en la URL** (`/app/<rol>/<módulo>`).
  **Por qué:** un usuario puede tener varios roles (ej. Docente + Tutor). Con el rol en la URL se puede cambiar con el selector, compartir enlaces y proteger rutas con `RoleGuard`.
- **Decisión:** **TanStack React Query** para los datos del servidor y **Zustand** solo para estado global de la UI (ej. notificaciones).
  **Por qué:** React Query ya resuelve caché, carga, errores y recarga de datos; no hace falta guardar datos del backend en un store propio.
- **Decisión:** **MSW** como backend simulado (`VITE_APP_ENABLE_API_MOCKING=true`).
  **Por qué:** permite avanzar el frontend sin esperar al backend. Los handlers en `src/testing/mocks/handlers/` sirven además como **contrato** de los endpoints que el backend debe respetar.
- **Decisión:** formularios con **react-hook-form + zod**; variables de entorno también validadas con zod (`src/config/env.ts`).
  **Por qué:** validación tipada con un solo esquema que sirve para TypeScript y para la validación en tiempo de ejecución.
- **Decisión:** estilos con **Tailwind** y tokens copiados de Figma; variantes de componentes con **class-variance-authority** (`cva`) y `cn()` (`clsx` + `tailwind-merge`).
  **Por qué:** los nombres de colores y tipografías coinciden con Figma, así que pasar un diseño a código es directo.

## Librerías o herramientas nuevas

Ver `package.json`. Las principales son React 18, React Router 7, TanStack Query 5, Zustand, axios, react-hook-form, zod, Radix UI, Tailwind, dayjs, MSW, Vitest, Playwright y Storybook.

## Pendientes / problemas conocidos

- [ ] Casi todos los módulos están como `ModulePlaceholder`. El estado de cada uno está en el README principal, sección 4.
- [ ] Cambiar contraseña (auth) pendiente.
- [ ] Conectar con el backend real (README principal, sección 2).

## Notas para quien siga

- Para crear un módulo nuevo, sigue el paso a paso del README principal (sección 5) y usa `src/features/horarios` como plantilla.
- Los datos simulados se guardan en `localStorage` (`msw-db`). Si algo raro pasa con el login o los datos, bórralo.
- Antes de subir cambios: `npm run lint && npm run check-types && npm test`.
