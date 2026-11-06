# Cruces de horario de docente y de ambiente

- **Autor:** royal-22
- **Fecha:** 2026-11-05
- **HU / tarea:** HU-46 (RF-117 y RF-118)
- **Estado:** terminado (backend)

## Qué hice

Reemplacé el `ValidadorCrucesStub`, que no detectaba nada, por el validador real: dos bloques se cruzan si son del mismo día, sus horas reales se superponen y comparten docente o ambiente en cualquier otra sección del año. Agregué `POST /api/horarios/secciones/{id}/cruces`, para que el modal de Valeria avise antes de guardar, y probé el PUT, el GET y la concurrencia con el validador real.

## Archivos principales

- `backend/src/main/java/com/galiledu/horarios/aplicacion/ValidadorCrucesHorario.java` y `MensajesCruce.java`
- `backend/src/main/java/com/galiledu/horarios/aplicacion/ServicioCrucesHorario.java` y `SolicitudCruces.java`
- `backend/src/main/java/com/galiledu/horarios/web/CrucesHorarioController.java`
- `backend/src/test/java/com/galiledu/horarios/CrucesHorarioIT.java`, `HorarioConValidadorRealIT.java`, `CrucesConcurrenciaIT.java`
- Ficha: [`docs/modulos/horarios.md`](../modulos/horarios.md)

## Decisiones y por qué

- **Decisión:** la comparación es lógica pura (`compararCruces`, sin acceso a datos) y la consulta de lo ocupado está aparte (`OcupacionHorarios`, `OcupacionHorariosSql`).
  **Por qué:** la lógica se prueba sin Spring ni BD y con todos los casos de borde; la consulta se prueba contra PostgreSQL.
  **Alternativas descartadas:** comparar dentro de la consulta SQL, que mezclaría las horas de varias estructuras y no se probaría sin BD.

- **Decisión:** se compara por **horas reales** y no por número de bloque, y con `<` estricto (los bloques contiguos no se cruzan).
  **Por qué:** cada sección tiene su propia estructura horaria; el bloque 2 de una puede caer a mitad del bloque 1 de otra (secundaria desde las 8:00 y primaria desde las 8:30). Un bloque que termina cuando empieza el otro no ocupa el mismo tiempo.
  **Alternativas descartadas:** comparar `día + número de bloque`, que no detecta cruces entre niveles y marca cruces falsos entre estructuras distintas.

- **Decisión:** los textos viven en `MensajesCruce` y son solo el texto de la alerta, sin título: «{docente} ya dicta {curso} con {seccion} el {día} de {inicio} a {fin}.» y «{seccion} ya usa {ambiente} para {curso} el {día} de {inicio} a {fin}.». El título («Docente no disponible», «Ambiente no disponible») lo arma el frontend con el `tipo`.
  **Por qué:** en Figma (nodo `296:17801`) la alerta tiene título y texto por separado. El texto del ambiente empieza por la sección para no depender del género del nombre del ambiente. Nombran con quién chocan y cuándo (hora sin cero a la izquierda, día en palabras). El endpoint y el GET devuelven el mensaje sin título; solo el 409 del PUT devuelve «Título: texto» (`MensajesCruce.conTitulo`), con los títulos `Docente no disponible` y `Ambiente no disponible`, porque ahí el mensaje se muestra tal cual en una sola notificación. Valeria aceptó el cambio de esa línea en `ServicioHorarios.validarCruces`.
  **Alternativas descartadas:** incluir el título en todos los mensajes, que el frontend tendría que recortar; devolver solo códigos y que el frontend arme el texto, que repetiría el formato en dos lugares.

- **Decisión:** el endpoint de cruces es de **solo lectura y no toma el bloqueo del año**; la garantía contra cruces concurrentes sigue en el PUT, que toma el bloqueo antes de leer.
  **Por qué:** es un aviso para el borrador; bloquear el año en cada tecla del modal frenaría a todos los que editan horarios. Un cruce que aparezca entre el aviso y el guardado lo atrapa el PUT.
  **Alternativas descartadas:** bloquear el año también en el aviso.

- **Decisión:** las celdas que el conversor no puede convertir (curso sin docente, bloque fuera de la jornada, curso fuera del plan) se **ignoran** y el endpoint devuelve los cruces del resto.
  **Por qué:** acordado con Valeria: el error exacto lo da el PUT, y el aviso no debe fallar mientras el Coordinador aún arma el borrador. Sin estructura principal ni horario guardado sí responde 409, igual que el GET.
  **Alternativas descartadas:** responder 400 o 409 por la primera celda inválida; devolver `[]` también en el 409.

- **Decisión:** en esta iteración el PUT valida los cruces de toda la matriz, no solo de las celdas cambiadas.
  **Por qué:** es lo más seguro y el GET marca en la celda los cruces existentes; el Coordinador los resuelve moviendo o quitando el bloque.
  **Alternativas descartadas:** validar solo las celdas nuevas o modificadas (pendiente de consultar al PO).

- **Decisión:** esta entrega cubre el RF-117 (cruce de docente: bloquear y mostrar la sección y el curso en conflicto) y el RF-118 (cruce de aula: bloquear e indicar qué sección la ocupa).
  **Por qué:** son los requisitos que respaldan la detección de cruces de la HU-46. El RF-122 (espacios especiales, una sección o subgrupo por bloque) es de la HU-48 y queda adelantado.
  **Alternativas descartadas:** ninguna.

## Pendientes / problemas conocidos

- [ ] Si Académico cambia un titular, el cruce no se impide; el GET lo muestra en la celda.
- [ ] El endpoint no valida el día contra los días lectivos; lo valida el PUT.
- [ ] Figma dice «Seleccione otro docente», pero el docente no se elige en el modal (es el titular de `docente_seccion`): el frontend mostrará otra ayuda.
- [ ] `OcupacionHorariosSql` llena el ambiente con su nombre y la matriz de Figma muestra el código (A-502): pendiente para Valeria.
- [ ] Falta el frontend: marcar cruces en la matriz y llamar al endpoint desde el modal.

## Notas para quien siga

- `CrucesConcurrenciaIT` falla si se comenta `estructuras.bloquearAnioParaHorarios(...)` en `ServicioHorarios.guardarHorario`: los dos PUT responden 200. No quites ese bloqueo.
- Las pruebas de concurrencia no usan la transacción de prueba: limpian por SQL las secciones `C1` y `C2` de 2027, y no deben compartir nombres con otras pruebas.
