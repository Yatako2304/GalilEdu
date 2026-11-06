# Pesos de evaluación y consolidado de notas

- **Autor:** josezavaleta
- **Fecha:** 2026-11-05
- **HU / tarea:** HU-57 y HU-62; corrección del inicio de sesión
- **Estado:** terminado

## Qué hice

Limité la suma de pesos por competencia, curso, sección y periodo a 100 %. Corregí los estados visuales de pesos y adapté el consolidado a la selección de ítems y promedio final opcional. Separé la pantalla en los botones **Configuración de ítems** y **Consolidado de notas**, conforme al ajuste solicitado.

El inicio de sesión limpia el token anterior antes de enviar las credenciales para permitir el acceso después de recrear la base o cambiar la clave de firma.

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/RepositorioCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/web/CalificacionController.java`
- `frontend/src/features/calificaciones/`
- `frontend/src/components/ui/form/multi-select.tsx`
- `frontend/src/components/ui/tag/tag.tsx`
- `frontend/src/lib/auth.tsx`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

- **Decisión:** comprobar la suma antes de crear o editar y bloquear transaccionalmente el plan de curso durante esas operaciones.
  **Por qué:** validar solamente el peso individual permitía acumular 200 %; el bloqueo evita que dos solicitudes concurrentes sobreasignen la misma competencia. La edición excluye el peso del propio ítem.
  **Alternativas descartadas:** limitar solo el formulario, porque una solicitud directa o simultánea podría superar el máximo.
- **Decisión:** mostrar **Pesos incompletos** en rojo delineado, **Pesos completos** en verde y **Pesos excedidos** en rojo para datos anteriores inválidos.
  **Por qué:** mantiene el estado incompleto de Figma y permite identificar excesos ya guardados.
  **Alternativas descartadas:** modificar automáticamente los pesos existentes, porque cambiaría criterios registrados por el docente.
- **Decisión:** calcular el promedio únicamente sobre los ítems seleccionados y usar el mismo modelo al exportar.
  **Por qué:** la tabla y el CSV deben corresponder a la selección que generó el docente. La regla simple o ponderada sigue definida en el año escolar.
  **Alternativas descartadas:** incluir todos los ítems de la competencia aunque se hayan seleccionado solo algunos.
- **Decisión:** mantener la configuración y el registro de notas dentro del primer botón y el consolidado en el segundo.
  **Por qué:** es la organización solicitada por josezavaleta; sustituye el acceso al consolidado como tercera pestaña descrito inicialmente en la [implementación del flujo](2026-11-04-josezavaleta-calificaciones-docente.md).
  **Alternativas descartadas:** añadir otra pestaña al mismo nivel que Configurar ítems y Calificar alumnos.

## Cambios en la base de datos

No cambia ningún esquema, tabla ni migración. Se consultan las tablas existentes para sumar pesos y obtener el umbral A de la conversión vigente al iniciar el periodo; si no existe una conversión se usa 14. Los pesos ya guardados por encima de 100 % requieren corrección del docente.

## Validación

- Frontend: lint, tipos y 886 pruebas correctas, incluidas las de navegación entre los dos botones y generación del consolidado.
- Backend: `mvnw verify` correcto con Java 21 y PostgreSQL de pruebas; 260 pruebas unitarias y 795 de integración contabilizadas, con 5 omisiones existentes y ningún fallo. Incluye rechazo al superar el peso, alta hasta completar 100 %, edición sin duplicar el peso propio y consolidado con selección de ítems y promedio opcional.

## Notas para quien siga

La API admite `itemIds` separados por coma e `incluirPromedio`; estos parámetros se usan tanto para la consulta como para la exportación. No se agregaron dependencias.
