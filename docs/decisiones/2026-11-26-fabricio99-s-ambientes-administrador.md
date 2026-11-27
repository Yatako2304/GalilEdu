# Ambientes físicos también para el Administrador

- **Autor:** fabricio99-s
- **Fecha:** 2026-11-26
- **HU / tarea:** HU-43
- **Estado:** terminado

## Qué hice

El Administrador puede listar, crear, editar, desactivar y reactivar ambientes físicos, igual que el Personal administrativo. Tiene la misma pantalla en su menú, en **Configuraciones › Ambientes físicos** (`/app/admin/ambientes`).

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/web/AmbienteController.java`: `hasAnyRole('ADMINISTRADOR', 'PERSONAL_ADMINISTRATIVO')`.
- `frontend/src/features/catalogo-academico/components/pagina-ambientes.tsx`: la página, compartida por las dos rutas.
- `frontend/src/app/routes/app/admin/ambientes.tsx`, `config/paths.ts`, `config/navigation.ts` y `app/router.tsx`.

## Decisiones y por qué

- **Decisión:** el Administrador tiene los ambientes con los mismos permisos que el Personal administrativo, sin quitárselos a este.
  **Por qué:** el plan de pruebas del curso («Plan de Pruebas 26-2») pone «Listar, agregar, modificar y desactivar espacio» en el rol Administrador, y es con lo que el profesor revisa. La HU-43 y Figma los ponen en el Personal administrativo, que los sigue teniendo.
  **Alternativas descartadas:** moverlos solo al Administrador (rompe la HU-43 y el flujo de Figma); dejarlos solo en el Personal administrativo y explicarlo en el plan (el profesor revisa por rol).
- **Decisión:** la página se movió a `features/catalogo-academico/components/pagina-ambientes.tsx` y las dos rutas solo la muestran.
  **Por qué:** evitar copiar la pantalla; cualquier cambio vale para los dos roles.

## Notas para quien siga

- En Figma el menú del Administrador no tiene esta sección: es una excepción a «manda Figma», como la del Coordinador con el plan curricular (2026-11-20).
