# Peso de cada curso en la nota de una competencia

- **Autor:** alex-sanchezz
- **Fecha:** 2026-11-05
- **HU / tarea:** HU-56
- **Estado:** terminado; el cálculo está en la [decisión de la nota final](2026-11-06-alex-sanchezz-nota-final-competencia.md)

## Qué hice

Agregué al plan de evaluación cuánto aporta cada curso a la nota de una competencia. Se configura en el modal del curso, junto a cada competencia marcada, y una tabla nueva debajo de los cursos muestra los pesos de cada competencia y si están completos.

Este cambio solo guarda y valida el peso. La nota de la competencia entre cursos se agregó después, en la [decisión de la nota final](2026-11-06-alex-sanchezz-nota-final-competencia.md).

## Archivos principales

- `backend/src/main/resources/db/migration/V13__peso_curso_competencia.sql`
- `backend/src/main/java/com/galiledu/academico/aplicacion/ServicioPlanEvaluacion.java`
- `backend/src/main/java/com/galiledu/academico/infraestructura/RepositorioPesosPlan.java`
- `frontend/src/features/catalogo-academico/components/dialogo-curso-plan.tsx`
- `frontend/src/features/catalogo-academico/components/pesos-competencias.tsx`
- `docs/modulos/catalogo-academico.md`

## Decisiones y por qué

- **Decisión:** configurar el peso en el plan de evaluación, con los roles que ya lo gestionan (Personal administrativo y Coordinador).
  **Por qué:** el plan ya define qué competencias evalúa cada curso, y varios cursos de una competencia pueden tener docentes distintos.
  **Alternativas descartadas:** que lo defina el docente, porque decidiría cuánto pesa el curso de otro; ponerlo en el año escolar, porque ahí solo hay parámetros iguales para todos los cursos; usar las horas semanales, porque el colegio no podría elegir el porcentaje y un curso pesaría igual en todas sus competencias.
- **Decisión:** agregar el campo «Peso en %» al modal del curso, aunque no aparece en el modal de Figma.
  **Por qué:** es una excepción a «manda Figma» que eligió alex-sanchezz y falta confirmar con el equipo: sin este dato no se puede combinar la nota de varios cursos con pesos distintos. Sigue a la [decisión de parámetros por año](2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md), que ya había cambiado ese modal.
  **Alternativas descartadas:** esperar un diseño nuevo en Figma, porque dejaría pendiente la configuración.
- **Decisión:** los pesos suman **hasta** 100 % entre los cursos del mismo grado y año. Un peso puede quedar vacío, y entonces la competencia se muestra con **Pesos incompletos**, aunque los demás cursos sumen 100 %.
  **Por qué:** el modal edita un curso a la vez, así que exigir 100 % exacto impediría guardar. Permitir el vacío evita bloquear el alta de un curso cuando la competencia ya está completa.
  **Alternativas descartadas:** exigir exactamente 100 % al guardar y editar todos los cursos en una sola pantalla, porque es un flujo nuevo que Figma no contempla.
- **Decisión:** los planes existentes reparten el 100 % en partes iguales entre los cursos de cada competencia.
  **Por qué:** es un punto de partida neutro y deja los pesos completos, sin datos nulos.
  **Alternativas descartadas:** dejarlos vacíos, porque obligaría a reconfigurar todos los planes; repartir según las horas, porque supone un criterio que nadie eligió.
- **Decisión:** guardar el peso con SQL (`RepositorioPesosPlan`) y no como campo de `CompetenciaEvaluada`.
  **Por qué:** al cambiar un valor de esa colección, JPA borra e inserta la fila, y el borrado falla si la competencia tiene ítems que apuntan a ella (`academico.item_competencia`).
  **Alternativas descartadas:** convertir la colección en una entidad propia, porque es un cambio mayor para un solo dato.
- **Decisión:** serializar los cambios de pesos de un grado y año con un bloqueo consultivo de PostgreSQL.
  **Por qué:** la suma cruza varios cursos, y un grado sin cursos no tiene filas que bloquear con `FOR UPDATE`, el mecanismo que usan los pesos de los ítems.
  **Alternativas descartadas:** validar sin bloqueo, porque dos guardados simultáneos podrían superar 100 %.

## Pendientes / problemas conocidos

- [x] Calcular la nota de la competencia entre cursos: resuelto en la [decisión de la nota final](2026-11-06-alex-sanchezz-nota-final-competencia.md).
- [ ] Confirmar con el PO que el peso solo se aplica con `PROMEDIO_PONDERADO`, igual que el peso de los ítems. Con otra regla el campo se guarda, pero no debería usarse.

## Notas para quien siga

`peso_curso` en `NULL` significa «sin definir» y no cuenta para el total. Copiar un plan conserva los pesos; si se salta un curso inactivo, sus competencias quedan incompletas, nunca excedidas.
