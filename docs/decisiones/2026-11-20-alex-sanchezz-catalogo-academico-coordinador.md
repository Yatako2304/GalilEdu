# El Coordinador también gestiona el catálogo académico y la escala

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-20
- **HU / tarea:** HU-52, HU-53, HU-54, HU-55
- **Estado:** terminado (pendiente de confirmar con el PO)

## Qué hice

El Coordinador puede registrar, modificar, desactivar y reactivar áreas, competencias y cursos (HU-52 a HU-54), y consultar y agregar versiones de la tabla de conversión (HU-55), con los mismos permisos que el Personal administrativo. En su menú aparecen **Plan curricular** y **Escala de calificación**, junto al **Plan de evaluación** que ya tenía.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/web/AreaController.java`, `CompetenciaController.java`, `CursoController.java` y `TablaConversionController.java`: `hasAnyRole('PERSONAL_ADMINISTRATIVO', 'COORDINADOR')`.
- `frontend/src/app/routes/app/coordinador/plan-curricular.tsx` y `escala-calificacion.tsx`, más `config/paths.ts`, `config/navigation.ts` y `app/router.tsx`.

## Decisiones y por qué

- **Decisión:** las HU-52 a HU-55 las tienen **los dos roles**, aunque el texto de las HU dice «Personal Administrativo» y en Figma (CI-14 y CI-23) solo aparecen en su menú.
  **Por qué:** es contenido académico, y el Coordinador ya arma el plan de evaluación (HU-56) con esas áreas, competencias y cursos. Sin este acceso no podía crear un curso o una competencia que le faltara. Se sigue el mismo criterio que con HU-56 (excepción del 2026-10-20 en [catalogo-academico.md](../modulos/catalogo-academico.md)).
  **Alternativas descartadas:** pasárselo **solo** al Coordinador, porque le quitaría al Personal administrativo algo que hoy usa y que Figma le asigna, sin la confirmación del PO.
- **Decisión:** en el Coordinador son dos pantallas sueltas, no la página «Competencias» con pestañas del Personal administrativo.
  **Por qué:** es lo mismo que se hizo con el Plan de evaluación (`/app/coordinador/plan-evaluacion`), al que ya enlaza Horarios. Así no cambia esa ruta.
- **Decisión:** los ambientes físicos (HU-43) siguen siendo solo del Personal administrativo.
  **Por qué:** es un tema operativo, no académico.

## Pendientes / problemas conocidos

- [ ] Confirmar con el PO el cambio de actor de las HU-52 a HU-55.
- [ ] Los íconos del menú (`Layers` para Plan curricular y `ArrowLeftRight` para Escala de calificación) no salen de un frame de Figma del Coordinador.
