# "Exalumno" solo se pide a padres y apoderados

- **Autor:** scuro-star (con asistencia de IA)
- **Fecha:** 2026-11-05
- **HU / tarea:** HU-6, HU-9 · observación del PO en la validación de la iteración 1
- **Estado:** terminado

## Qué hice

"Nuevo usuario" y "Editar usuario" solo muestran la casilla "Es exalumno(a) del colegio" si el usuario tiene el rol Apoderado (aunque también tenga otros, por ejemplo Docente). Como ese rol no se asigna desde "Nuevo usuario", ahí nunca aparece. En la inscripción (padre, madre y apoderado) se sigue pidiendo.

## Archivos principales

- `frontend/src/components/personas/persona-fields.tsx`: nueva opción `sinExalumno`.
- `frontend/src/features/usuarios/components/dialogo-usuario.tsx`: pasa `sinExalumno` cuando el usuario no tiene el rol Apoderado.

## Decisiones y por qué

- **Decisión:** en el formulario de usuario, mostrar "exalumno" solo a quien tiene el rol Apoderado, con una opción de `PersonaFields`, sin quitar el dato del backend ni de la base.
  **Por qué:** el PO indicó que el dato solo importa para los padres y apoderados (por ejemplo, para descuentos). El campo `es_exalumno` es de la persona y lo sigue usando la inscripción.
  **Alternativas descartadas:** borrar el campo de la persona (lo necesita la inscripción).

## Notas para quien siga

- Si al usuario no se le muestra "exalumno", el valor que ya tenía se conserva al guardar, porque el formulario lo mantiene aunque no lo muestre.
- La plantilla de carga masiva de usuarios (HU-8, oculta) todavía tiene la columna "Exalumno (Sí / No)"; si se reactiva, conviene quitarla también.
