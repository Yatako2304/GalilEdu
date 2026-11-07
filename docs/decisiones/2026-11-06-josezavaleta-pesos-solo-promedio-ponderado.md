# Pesos de ítems solo con promedio ponderado

- **Autor:** josezavaleta
- **Fecha:** 2026-11-06
- **HU / tarea:** HU-57 y HU-62 · configuración de ítems según la regla anual
- **Estado:** terminado

## Qué hice

Adapté la creación, edición y vista de ítems para solicitar porcentajes únicamente cuando el año escolar usa `PROMEDIO_PONDERADO`. El promedio simple y las reglas cualitativas permiten asociar competencias sin peso, ocultan sus columnas e indicadores y no bloquean nuevos ítems al completar 100 %.

Esta decisión reemplaza el límite y los indicadores aplicados sin distinguir la regla en la [corrección de pesos](2026-11-05-josezavaleta-correcciones-calificaciones.md). Se adapta el formulario de Figma por petición expresa de josezavaleta. La fuente de la regla sigue siendo el año escolar y se conserva el bloqueo de sus parámetros después de iniciado.

## Archivos principales

- `backend/src/main/resources/db/migration/V15__calificaciones_peso_solo_ponderado.sql`
- `backend/src/main/java/com/galiledu/academico/aplicacion/DtosCalificaciones.java`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioCalificaciones.java`
- `frontend/src/features/calificaciones/components/items-evaluacion.tsx`
- `frontend/src/features/calificaciones/components/dialogo-item-evaluacion.tsx`
- `frontend/src/features/calificaciones/components/calificaciones-docente.tsx`
- `frontend/src/types/api.ts`
- `docs/modulos/calificaciones.md`

## Decisiones y por qué

- **Decisión:** mostrar y validar los pesos solo si `configuracion.anio_escolar.regla_nota` es `PROMEDIO_PONDERADO`, también al editar y al elegir competencias disponibles.
  **Por qué:** el promedio simple da la misma participación a cada ítem calificado; un porcentaje no afecta su cálculo y completar 100 % no debe impedir agregar ítems. Las reglas cualitativas tampoco utilizan pesos.
  **Alternativas descartadas:** ocultar solo el campo mientras el backend siguiera exigiendo porcentajes o bloqueando altas.
- **Decisión:** permitir omitir `pesoPorcentaje` en el contrato y validar el porcentaje en el servicio después de consultar la regla anual.
  **Por qué:** una validación incondicional del DTO no puede decidir si el porcentaje corresponde al año del curso. En ponderado sigue siendo obligatorio y conserva el rango y el límite acumulado.
  **Alternativas descartadas:** confiar en la regla enviada por el frontend o quitar las comprobaciones de ponderación en todos los años.
- **Decisión:** guardar `NULL` cuando no se pondera, ignorar porcentajes de clientes anteriores en esas reglas y mantener los datos históricos hasta su edición.
  **Por qué:** representa que no existe un peso, evita introducir porcentajes ficticios y conserva los datos previamente registrados. Al editar, el servicio normaliza las asociaciones según la regla anual.
  **Alternativas descartadas:** guardar 1 % o 100 % como valor técnico, o borrar porcentajes históricos mediante una actualización masiva.
- **Decisión:** al combinar ítems compartidos, sumar sus pesos solo para promedio ponderado.
  **Por qué:** los ítems de promedio simple y reglas cualitativas pueden tener peso nulo y deben seguir contando una sola vez en el resultado general. El PDF utiliza ese mismo resultado sin recalcularlo.
  **Alternativas descartadas:** asignar pesos artificiales para evitar valores nulos en el reporte.

## Cambios en los esquemas de base de datos

La migración nueva `V15__calificaciones_peso_solo_ponderado.sql` modifica una columna del esquema `academico`:

| Tabla y columna | Cambio | Conserva |
| --- | --- | --- |
| `item_competencia.peso_porcentaje` | Elimina `NOT NULL` y agrega un comentario de uso según la regla anual. | Tipo `NUMERIC(5, 2)`, valores existentes, `ck_item_competencia_peso`, claves foráneas, clave primaria e índices. |

El `CHECK` sigue rechazando porcentajes no nulos menores o iguales que cero o mayores que 100. La obligatoriedad del peso en promedio ponderado se valida en el servicio. No se edita ninguna migración anterior ni se modifican notas, evidencias o parámetros anuales existentes.

## Validación

- Backend: verificación completa correcta con Java 21 y PostgreSQL de pruebas; 314 pruebas unitarias y 989 de integración contabilizadas, cero fallos y cinco omisiones existentes. Incluye alta y edición sin peso, porcentajes históricos, múltiples ítems aun con 100 % anterior, reglas cualitativas, rango y obligatoriedad del ponderado, migración y promedios/PDF con pesos nulos.
- Frontend: lint y tipos correctos; 113 archivos y 1.045 pruebas correctas. Los casos nuevos comprueban el formulario sin peso, edición sin enviar porcentajes anteriores, ausencia de indicadores y bloqueo en las reglas no ponderadas y conservación del límite e indicadores en promedio ponderado.

## Notas para quien siga

Reiniciar el backend aplica V15 con Flyway. Coordinar su número antes de integrar si otra rama incorpora una V15. En contratos, `pesoPorcentaje` de respuesta es anulable; en solicitud se omite para reglas sin ponderación. La regla efectiva se obtiene del año escolar, no del valor guardado ni de los parámetros heredados de `plan_competencia`. No se agregaron dependencias.
