# Estructura horaria del año (HU-44, backend)

- **Autor:** valeriaacevedop
- **Fecha:** 2026-10-22
- **HU / tarea:** HU-44 (Figma: "CI-12 · Estructura horaria" y "Modal - Nueva estructura horaria")
- **Estado:** en progreso (backend terminado; falta el frontend)

## Qué hice

Backend de las estructuras horarias de un año escolar: listar, ver el detalle con la jornada calculada, crear, vista previa de la jornada, editar y eliminar. No hay migraciones nuevas: usa `configuracion.estructura_horaria` y `configuracion.receso` tal como quedaron en V4 y V7. Los horarios de cada bloque y receso no se guardan: los calcula una clase de dominio pura. Además hice una tanda de pruebas de entradas equivocadas y maliciosas que encontró y corrigió varios fallos (ver la decisión 8) y dejó otros anotados como pendientes (ver más abajo).

## Archivos principales

- `backend/src/main/java/com/galiledu/configuracion/dominio/`: `EstructuraHoraria`, `Receso`, `DefinicionEstructura`, `CalculadoraJornada`, `DiaSemana` y `DatosReceso`.
- `backend/src/main/java/com/galiledu/configuracion/aplicacion/`: `ServicioEstructurasHorarias`, `DtosEstructurasHorarias` y la interfaz `PlanesDeEvaluacion`.
- `backend/src/main/java/com/galiledu/configuracion/infraestructura/`: `EstructuraHorariaRepositorio` y `PlanesDeEvaluacionSql`.
- `backend/src/main/java/com/galiledu/configuracion/web/EstructuraHorariaController.java`
- Pruebas en `backend/src/test/java/com/galiledu/configuracion/`: `dominio/CalculadoraJornadaTests`, `dominio/EstructuraHorariaTests` y `dominio/EntradasHostilesEstructuraHorariaTests` (reglas); `aplicacion/ServicioEstructurasHorariasTests` (mensajes de la BD con mocks); `EstructuraHorariaRepositorioIT` (mapeo y recesos); `PlanesDeEvaluacionSqlIT`; `ServicioEstructurasHorariasIT`; `EstructuraHorariaIT` (la API); `EstructuraHorariaEntradasHostilesIT` y `EstructuraHorariaConcurrenciaIT` (entradas hostiles y concurrencia, esta última sin transacción de prueba).

## Decisiones y por qué

1. **Varias estructuras por año; solo la principal de cada nivel se usa.**
   **Decisión:** un año puede guardar varias estructuras. Cada una es de un solo nivel (PRIMARIA o SECUNDARIA) y solo la **principal** de cada nivel se usa (por ejemplo, Académico calcula los bloques por semana con ella).
   **Por qué:** Figma muestra varias estructuras por año (EH-PRI-26 y EH-SEC-26, y el modal crea una de talleres de tarde), y V7 ya trae el índice único parcial `ux_estructura_horaria_principal`. La HU habla de "un único bloque horario"; lo interpretamos como "una principal por nivel". **Pendiente de comunicar al grupo.**
   **Alternativas descartadas:** una sola estructura por año (contradice Figma).

2. **Solo se modifica con el año PLANIFICADO.**
   **Decisión:** crear, editar y eliminar responden 409 "Solo se puede modificar la estructura horaria de un año planificado." si el año está ACTIVO o CERRADO. Listar, ver el detalle y la vista previa no dependen del estado. La lista y el detalle traen `anioModificable` para que el frontend deshabilite "+ Nueva", Editar y Eliminar.
   **Por qué:** con el año en marcha, los planes de evaluación, las secciones y la matrícula ya se apoyan en la jornada.
   **Diferencia con Figma:** CI-12 muestra "+ Nueva" con el año ACTIVO; con esta decisión debe quedar deshabilitado.

3. **La primera estructura de un nivel nace principal.**
   **Decisión:** aunque la casilla "Estructura principal" venga sin marcar, la primera estructura del nivel en el año es principal. En las siguientes la casilla decide. Al marcar otra como principal, la anterior del mismo nivel y año pasa a no principal en la misma transacción, y la respuesta lleva una advertencia ("EH-B pasa a ser la estructura principal de primaria; EH-A deja de serlo.").
   **Por qué:** así el nivel nunca queda con estructuras guardadas pero sin principal, y coincide con el valor por defecto de la BD.
   **Diferencia con Figma:** el modal no tiene Nivel ni "Estructura principal" y la BD los exige: se agregaron un selector y una casilla.

4. **Los horarios se calculan, no se guardan; la jornada termina como máximo a las 18:00.**
   **Decisión:** `CalculadoraJornada` (dominio puro, sin Spring) calcula las filas de bloques y recesos desde la hora de entrada, los bloques por día, la duración y los recesos. Trabaja en minutos desde las 00:00, no con `LocalTime`, para que una jornada que cruza la medianoche no parezca terminar temprano. La jornada, contando los recesos, debe terminar como máximo a las 18:00 ("La jornada debe terminar como máximo a las 18:00."). Reglas: al menos un día lectivo y sin repetir (LUN a SAB), de 1 a 12 bloques por día, bloques de 20 a 120 min, recesos de 5 a 60 min, después de un bloque entre 1 y bloques-1 y sin repetir.
   **Por qué:** los horarios dependen solo de esos datos, y guardarlos obligaría a mantenerlos sincronizados. Los ejemplos de Figma son pruebas: EH-PRI-26 (07:45, 8 bloques de 45, recesos de 20 y 15) sale a las 14:20 con 40 bloques por semana, y la estructura del modal (14:45, 4 bloques de 45, receso de 15) sale a las 18:00 exactas, con 12 bloques por semana.
   **Alternativas descartadas:** guardar el horario de cada bloque; un receso después del último bloque (no tiene sentido).

5. **El nivel no se edita y no se puede desmarcar la principal.**
   **Decisión:** el PUT no recibe el nivel; para cambiar de nivel se elimina la estructura y se crea de nuevo. Un `esPrincipal` en false sobre la principal da 409: para cambiar de principal se marca la otra y la anterior baja sola. Eliminar la principal da 409 si quedan otras estructuras del nivel ("Marca otra como principal primero."); si es la única del nivel, se puede eliminar salvo que haya planes (decisión 6).
   **Por qué:** así un nivel con estructuras guardadas nunca queda sin principal, y no se mueve una estructura de nivel dejando huérfano el anterior.
   **Alternativas descartadas:** permitir cambiar el nivel; dejar el nivel sin principal al eliminarla.

6. **Reglas contra los planes de evaluación, y advertencias de planificación.**
   **Decisión:** Académico guarda las horas semanales por curso (`academico.plan_curso`) y valida que las de un grado no superen los bloques por semana de la principal de su nivel. Para no dejarlo inconsistente, estas operaciones dan 409 si algún grado del nivel tiene más horas planificadas que los bloques resultantes, nombrando el grado de más horas: reducir los bloques por día o los días de una principal, marcar como principal una estructura con menos bloques que la actual, y crear la primera principal de un nivel (esta última es decisión nuestra, no estaba en el requisito). El mensaje es "No se puede reducir la jornada a {n} bloques por semana: {grado} ya tiene {horas} horas de cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos {horas} bloques.". Eliminar la única estructura de un nivel con planes da "No se puede eliminar la estructura principal de {nivel}: hay cursos planificados que dependen de ella. Marca otra como principal primero.". Aumentar los bloques o los días nunca se rechaza, y una estructura que no es principal no se valida contra los planes.
   **Advertencias:** crear y editar devuelven `advertencias` cuando cambian los bloques por semana de la principal y ese nivel tiene planes, tanto al subir como al bajar ("La jornada de secundaria pasa de 30 a 35 bloques por semana. Revisa el plan de evaluación: 1.° de secundaria tiene 28 horas planificadas."). Cambiar la hora de entrada, la duración o los recesos no avisa, porque no mueve los bloques por semana.
   **Cómo se leen los planes:** `ServicioEstructurasHorarias` usa la interfaz `PlanesDeEvaluacion` (en `aplicacion`) y `PlanesDeEvaluacionSql` la implementa con SQL de solo lectura, sin importar nada de `academico`. Cuando Académico ofrezca un servicio equivalente, solo se reemplaza esa clase. No se modificaron `ServicioPlanEvaluacion` ni `ConsultasCatalogo`.
   **Alternativas descartadas:** rechazar también los aumentos; avisar siempre, tenga o no planes el nivel.

7. **Bloqueo por año y flush entre degradar y promover.**
   **Decisión:** todos los cambios de estructuras de un año se serializan con `pg_advisory_xact_lock` sobre el id del año (con una clave distinta de la de las secciones, para que no se esperen entre sí). Al editar o eliminar, el año se bloquea antes de leer la estructura, para no trabajar con una copia que otra petición ya cambió. Todo se valida antes de modificar. Al cambiar la principal se baja la anterior y se hace `flush` antes de guardar la nueva.
   **Por qué:** el índice único parcial solo admite una principal por nivel y año, y Hibernate inserta antes de actualizar: sin el `flush`, la nueva principal chocaría con la anterior. Se comprobó quitándolo: falla con "Otra persona acaba de cambiar…". Además, al editar, los recesos que siguen en el mismo bloque se reutilizan (solo cambia su duración) en lugar de borrarlos y volver a crearlos, para no chocar con `UNIQUE (estructura_horaria_id, despues_de_bloque)`. `actualizado_en` se mueve de forma explícita, porque los recesos son otra tabla y Hibernate no dispararía la actualización de la estructura.
   **Alternativas descartadas:** confiar solo en las restricciones de la BD (daría errores en lugar de esperar).

8. **Entradas del usuario: lo que se corrigió tras las pruebas hostiles.**
   **Decisión:** el código, ya normalizado (sin espacios al borde y en mayúsculas), solo admite letras A-Z sin tilde, números y guion, hasta 20 caracteres ("El código solo admite letras, números y guion."). Ni el código ni la descripción admiten caracteres de control en medio (NUL, saltos de línea, tabulaciones, escape): "El texto contiene caracteres no válidos.". La hora de entrada se trunca a minutos al normalizar, así lo guardado y lo calculado coinciden. `esPrincipal` es opcional al editar: ausente significa "no cambiar", true marca como principal y false sobre la principal da 409. Un día null o en blanco da "Elige solo días válidos: de LUN a SAB.". Cuando la BD rechaza el guardado, solo el índice de la principal da el mensaje de "Otra persona acaba de cambiar…"; el código repetido da su propio mensaje y cualquier otra restricción da un mensaje genérico, sin nombres internos.
   **Por qué:** las pruebas hostiles encontraron un 500 con un NUL en el código (PostgreSQL lo rechaza), un 409 engañoso con un NUL en la descripción, códigos hechos solo de caracteres invisibles, rutas o letras de otros alfabetos que se ven iguales que las latinas, una hora con segundos que se guardaba distinta de lo que se calculaba y un `NullPointerException` latente en el dominio con un `null` en los recesos (también corregido).
   **Alternativas descartadas:** sanear el texto en silencio (se prefirió rechazarlo con un mensaje).

9. **Lo que no se pudo verificar.**
   - La carrera real entre dos servidores o procesos distintos: la concurrencia se probó con 6 hilos en una misma aplicación y transacciones reales, no con varias instancias.
   - La carrera entre este chequeo contra los planes y las horas que Académico guarda al mismo tiempo: Académico no toma ningún bloqueo, así que queda como limitación conocida.
   - Las ramas del `catch` de `DataIntegrityViolationException` solo se cubrieron con mocks (el mensaje de cada restricción); con el año bloqueado no se pueden provocar desde la API.
   - El frontend y `frontend/src/types/api.ts`: no se tocaron.
   - Ver también "No verificado hoy", más abajo.

## Pendientes / problemas conocidos

### Pendientes del grupo

- [ ] **(a) `ManejadorErrores` devuelve 500 en lugar de 400, 404, 405 y 415.** Su `@ExceptionHandler(Exception.class)` atrapa excepciones de Spring que deberían ser errores del cliente. Casos: un UUID mal escrito en la ruta (500 en vez de 400), una ruta que no existe, como la que termina en barra (500 en vez de 404), un método no permitido (500 en vez de 405) y un `Content-Type` que no es JSON o falta (500 en vez de 415). Un rol sin permiso con un UUID mal escrito también recibe 500 en vez de 403. **Corrección propuesta:** un handler para `MethodArgumentTypeMismatchException` (400) y otro para las excepciones que implementan `ErrorResponse` (405, 415, 404), antes del genérico. Afecta a todos los módulos. Hay 5 métodos con `@Disabled` ("Pendiente compartido: ManejadorErrores devuelve 500; se activa cuando se corrija") en `EstructuraHorariaEntradasHostilesIT`; se activan al corregirlo.
- [ ] **(b) Configuración de Jackson.** Acepta números con decimales (5.5 pasa a 5, 45.5 a 45), números escritos como texto (`"6"`) y notación científica (1e2 pasa a 100), y acepta claves duplicadas en el JSON (gana la última). **Propuesta:** desactivar la coerción de decimales y de textos a número y activar la detección estricta de claves duplicadas en Jackson. Los nombres exactos de las opciones dependen de la versión (Spring Boot 4.1.1 usa Jackson 3) y no los verifiqué. Es configuración compartida.
- [ ] **(c) No hay límite de tamaño del cuerpo en el servidor.** Un código de 10 MB se lee entero en memoria y recién entonces se rechaza por largo. **Propuesta:** limitar el tamaño de la petición en el servidor o en un filtro.
- [ ] Comunicar al grupo la interpretación de la decisión 1 ("un único bloque horario" como "una principal por nivel").
- [ ] Frontend: CI-12 y el modal, con las diferencias con Figma (el modal no tiene Nivel ni "Estructura principal"; CI-12 no marca cuál es la principal ni dibuja Editar y Eliminar; "+ Nueva" debe quedar deshabilitado con el año activo). Y `frontend/src/types/api.ts`.
- [ ] La activación del año no exige una principal por nivel: solo cuenta cuántas estructuras hay. Un año puede activarse con una estructura de un solo nivel. Se dejó como estaba.
- [ ] HU-45 (Horarios): antes de permitir editar o eliminar una estructura, Horarios debe validar que no haya cursos asignados a sus bloques.

### Huecos que quedaron sin corregir (informe de pruebas hostiles)

No rompen nada; las pruebas documentan el comportamiento actual.

- Coerción de números (5.5, `"6"`, 1e2): depende de la configuración de Jackson (pendiente b).
- Claves duplicadas en el JSON: se acepta y gana la última (pendiente b).
- Cuerpo de 10 MB: se lee entero antes de rechazarlo (pendiente c).
- Mil días inválidos: se rechazan con el primer error ("Día lectivo no válido: D0."), pero la vista previa devolvería los mil errores.
- Las respuestas del firewall de Spring Security (`..`, `%2e%2e`, `%00`, `%2F` en la ruta) y los 401 (sin token o con token inválido) llevan el cuerpo vacío, no `{message, errores}`.
- El largo del código se mide ya normalizado: once `ß` se rechazan por largo porque al pasar a mayúscula valen 22 caracteres ("SS" × 11).
- La vista previa con jornada desde 23:59 (ver "No verificado hoy").

### No verificado hoy

- [ ] **Rollback tras bajar la principal.** Al crear una estructura como principal, el servicio baja a la anterior y hace `flush` antes de guardar la nueva. Si el guardado fallaba después, la transacción real debía deshacer también la baja. Se comprobó antes de las correcciones de la decisión 8, con un NUL en la descripción como causa del fallo. Hoy ese NUL se rechaza al validar, antes de bajar a la anterior, así que el camino ya no se puede provocar desde la API y solo lo cubre la validación previa (`EstructuraHorariaConcurrenciaIT.unaCreacionPrincipalRechazadaNoDejaDesmarcadaALaAnterior` comprueba que la anterior sigue siendo principal, pero ya no ejerce el rollback). Si aparece otra causa de fallo en el guardado, hay que volver a probarlo.
- [ ] **Vista previa con jornada desde 23:59.** Con 12 bloques de 120 min y 11 recesos de 60 min desde las 23:59, la vista previa muestra horas pasada la medianoche como "58:59", que no es una hora válida. Crear la estructura da 400 por pasar de las 18:00 y no se guarda nada, así que solo afecta a cómo se ve la vista previa. **Propuesta:** mostrar "día siguiente" (por ejemplo "10:59 · día siguiente") o cortar la jornada en las 24:00.

## Notas para quien siga

- El dominio lanza `IllegalArgumentException` (400) e `IllegalStateException` (409) y `ServicioEstructurasHorarias` los traduce a `ExcepcionNegocio`, como en las secciones. El cliente solo recibe `{ message, errores }`.
- La respuesta de la lista es `{anioModificable, estructuras}`; crear y editar devuelven `{estructura, advertencias}`; el detalle trae la jornada calculada en `jornada`.
- Las solicitudes no llevan anotaciones de validación a propósito: la vista previa se usa con el formulario a medio llenar y responde 200 con la lista de errores. Las reglas están en `EstructuraHoraria.detectarErrores`, que sirve a la vista previa (todos los errores) y a crear y editar (el primero).
- La vista previa solo exige que el año exista: no mira si el año admite cambios ni los planes.
- `EstructuraHorariaConcurrenciaIT` no usa la transacción de prueba (cada petición confirma de verdad) y borra por SQL todo lo que crea en 2027. Si la agregas a otra suite, conserva ese `@AfterEach`.
