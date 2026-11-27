# Catálogo académico

- **HU:** HU-52, 53, 54, 55, 56 (iteración 1) · HU-43 (3)
- **Estado:** ✅ hecho: la iteración 1 (HU-52 a HU-56) y HU-43 (iteración 3, ambientes físicos), con backend y frontend
- **Última actualización:** 2026-11-20

## Alcance

El Personal administrativo arma el catálogo curricular. Cada **área** agrupa sus **competencias** y sus **cursos**. Para cada año escolar define el **plan de evaluación**:
- qué cursos se dictan en cada grado y cuántas horas tienen;
- qué competencias evalúa cada curso; la escala, la regla de nota final y la consolidación de periodos se toman del año escolar;
- cuánto aporta cada curso a la nota de una competencia cuando varios cursos la evalúan (vista **Competencias del grado**);
- qué docente dicta cada curso en cada sección.

También mantiene la **tabla de conversión** de vigesimal a AD/A/B/C, que tiene versiones, y el catálogo de **ambientes físicos**.

Las áreas, competencias, cursos y ambientes no se borran: se desactivan. Un área, competencia o curso inactivo ya no se puede agregar a planes nuevos; un ambiente inactivo ya no se ofrece al armar horarios (Horarios solo lee los activos).

**Figma frente a los RF de Notion:**
- RF-130 a RF-132 dicen que el plan lo configura el Coordinador; en Figma está en el menú del Personal administrativo. **Excepción a "manda Figma" (2026-10-20, a pedido del equipo):** lo tienen los dos roles, con los mismos permisos. El Coordinador lo ve en su menú como "Plan de evaluación" (`/app/coordinador/plan-evaluacion`).
- Las HU-52 a HU-55 dicen «Personal Administrativo» y en Figma solo están en su menú. **Excepción a "manda Figma" (2026-11-20):** el Coordinador también las tiene, con los mismos permisos, como **Plan curricular** (`/app/coordinador/plan-curricular`) y **Escala de calificación** (`/app/coordinador/escala-calificacion`). Los ambientes físicos no son del Coordinador. Ver la [decisión](../decisiones/2026-11-20-alex-sanchezz-catalogo-academico-coordinador.md).
- La HU-43 dice «Personal Administrativo» y en Figma está solo en su menú. **Excepción a "manda Figma" (2026-11-26):** el **Administrador** también tiene los ambientes físicos, con los mismos permisos (listar, crear, editar, desactivar y reactivar), en su menú **Configuraciones › Ambientes físicos** (`/app/admin/ambientes`), porque el plan de pruebas del curso los pone en su rol. Ver la [decisión](../decisiones/2026-11-26-fabricio99-s-ambientes-administrador.md).
- RF-126 pide una descripción obligatoria para el área; en Figma es opcional. Aquí manda Figma.
- El modal de Figma muestra escala, agregación de ítems y consolidación por competencia. Esta parte fue reemplazada por la [decisión de parámetros de evaluación por año](../decisiones/2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md): el modal solo permite indicar horas semanales y marcar competencias.

## Historias de usuario

| HU | Qué | Estado | Notas |
| --- | --- | --- | --- |
| HU-52 | Áreas curriculares | ✅ | Nombre único, sin importar mayúsculas. Descripción opcional. Al desactivar un área, sus competencias y cursos quedan como están. |
| HU-53 | Competencias de un área | ✅ | Nombre único dentro del área. Se agregan al final de la lista. |
| HU-54 | Cursos de un área y sus grados | ✅ | Nombre único y al menos un grado. No se puede quitar un grado en el que el curso ya está en un plan de evaluación. |
| HU-55 | Escala de calificación | ✅ | Las versiones no se editan: se agrega una nueva con su fecha "Vigente desde", que no puede ser anterior a hoy. Si la fecha es futura, queda **programada**. El estado (vigente, programada, anterior) se calcula con la fecha de hoy y no se guarda. **Diferencia con Figma:** debajo de la tabla vigente agregamos "Otras versiones" (programadas y anteriores), porque CI-23 no muestra dónde ver la versión programada ni el historial que menciona el modal. |
| HU-56 | Plan de evaluación | ✅ | Por grado y año: agregar cursos con sus horas y competencias, configurarlos, quitarlos, asignar docentes por sección y copiar el plan de otro grado o año a un grado vacío (sin docentes). Un año CERRADO es de solo lectura. El modal del curso solo pide las horas semanales y las competencias; la escala, la regla de nota y la consolidación se definen una vez en el año escolar. **Diferencias con Figma:** (1) se retiraron los tres selectores por competencia según la [decisión](../decisiones/2026-11-04-josezavaleta-parametros-evaluacion-por-anio.md); (2) la columna Estado dice "Configurado" o "Sin configurar" (curso sin competencias); (3) el menú de cada curso tiene "Configurar competencias", "Asignar docentes" y "Quitar del plan"; (4) "Copiar plan" solo aparece cuando el grado está vacío. |
| HU-56 | Peso de cada curso por competencia | ✅ | Vista **Competencias del grado**, debajo de los cursos: cada competencia con los cursos que la evalúan y su peso. **Ajustar pesos** edita todos los cursos de la competencia a la vez y exige que sumen exactamente 100 %. Con un solo curso, el peso es 100 % automático. Cuenta con promedio ponderado (pondera las notas) y en la escala de letras (cada curso vota por su nivel con su peso; si empatan, gana el nivel mayor). Con promedio simple la vista dice que los cursos aportan por igual. **No está en Figma:** véase la [decisión de la nota oficial por competencia](../decisiones/2026-11-16-alex-sanchezz-nota-oficial-competencia.md). |
| HU-43 | Ambientes físicos | ✅ | Aulas regulares y espacios especiales con aforo y recursos; tarjetas, lista con filtros, alta, edición, desactivar y reactivar. **Diferencias con Figma:** (1) el **código no se ingresa**: lo genera el backend (`A-001` para aula regular, `E-001` para espacio especial), no cambia y la interfaz no lo muestra, aunque Figma todavía muestra el campo «Código» y la columna; (2) el **nombre es único** (sin importar mayúsculas ni espacios sobrantes, incluso entre inactivos), cosa que Figma no dice; (3) Figma no define el modal «¿Desactivar …?»: desactivar se permite aunque el ambiente esté en uso y el frontend pide una confirmación genérica; (4) los mensajes de nombre repetido, ambiente inexistente y aforo por debajo de la matrícula los redactamos nosotros. Ver la [decisión](../decisiones/2026-11-18-valeriaacevedop-ambientes-fisicos.md). |

## Pantallas (Figma)

| Pantalla | Frame en Figma | Ruta |
| --- | --- | --- |
| Plan curricular | Personal Administrativo › "CI-14 · Plan curricular: áreas, competencias y cursos (HU-52, 53, 54)" y modales "Nueva área", "Nueva competencia", "Nuevo curso", "¿Desactivar …?" | `/app/administrativo/competencias` · `/app/coordinador/plan-curricular` |
| Plan de evaluación | "CI-17 · Plan de evaluación — 1.° de secundaria 2026 (HU-56)", modal "Aritmética · 1.° de secundaria", "CI-20 · … sin cursos" | pestaña · `/app/coordinador/plan-evaluacion` (no está en Figma) |
| Escala de calificación | "CI-23 · Escala de calificación — tabla de conversión vigente (HU-55)", modal "Nueva versión de la tabla de conversión" | pestaña · `/app/coordinador/escala-calificacion` |
| Ambientes físicos | "CI-26 · Ambientes físicos (HU-43)" (nodo `857:10215`) y modal "Nuevo ambiente físico" (nodo `857:11500`). El modal "¿Desactivar …?" no está en Figma | `/app/administrativo/ambientes` y `/app/admin/ambientes` (Administrador) |

## Base de datos

- **Esquemas:** `academico` y `configuracion` (ambientes) · **Migraciones:** `V6__catalogo_academico.sql`, `V9__parametros_evaluacion_por_anio.sql`, `V18__peso_curso_competencia.sql`. HU-43 **no agregó migraciones**: usa `configuracion.ambiente` tal como la creó V6.

| Tabla | Para qué |
| --- | --- |
| `academico.area` | Área curricular, activa o inactiva. |
| `academico.competencia` | Competencia de un área; el nombre no se repite dentro del área. |
| `academico.curso` | Curso de un área. |
| `academico.curso_grado` | Grados en los que se dicta el curso. |
| `academico.plan_curso` | Curso que se dicta en un grado y año, con sus horas semanales (de 1 a 20). La BD impide planificar un curso en un grado donde no se dicta. |
| `academico.plan_competencia` | Competencia que evalúa el curso en ese plan. Desde V9 la relación efectiva solo necesita `plan_curso_id` y `competencia_id`. Las columnas anteriores de escala, regla y consolidación se conservan temporalmente para no perder información, pero la aplicación ya no las usa. Desde V18, `peso_curso` (`NUMERIC(5,2)`, `NULL` = sin definir, `ck_plan_competencia_peso_curso`: mayor que 0 y hasta 100) dice cuánto aporta el curso a la nota de la competencia; entre los cursos del grado y año suma 100. Se guarda con SQL (`RepositorioPesosPlan`), no en la colección de JPA. |
| `academico.docente_seccion` | Docente que dicta un curso del plan en una sección. |
| `academico.tabla_conversion`, `tabla_conversion_nivel` | Versiones de la tabla de conversión, con su fecha de vigencia y los rangos de AD, A, B y C (de 0 a 20). |
| `configuracion.ambiente` | Aula regular (aforo máximo 45) o espacio especial, con sus recursos (`VARCHAR(60)[]`). `codigo` es `UNIQUE`; el **nombre no tiene restricción en la BD** (lo valida el servicio). Además, `configuracion.seccion.ambiente_id` indica el aula de cada sección y Horarios guarda el ambiente de cada bloque (`horarios.horario_bloque.ambiente_id`). La entidad `Ambiente` vive en `academico` y mapea esta tabla del esquema `configuracion`. |

### Cambio de esquema de V9

V9 no crea ni elimina tablas en `academico`. En `academico.plan_competencia` mantiene `escala`, `regla_nota` y `consolidacion` para conservar la información histórica, junto con sus restricciones `CHECK`. Solo agrega estos valores técnicos por defecto para que los registros nuevos sigan siendo compatibles con la estructura heredada:

| Columna heredada | Tipo | Valor por defecto desde V9 | Fuente efectiva en la aplicación |
| --- | --- | --- | --- |
| `escala` | `VARCHAR(12)` | `VIGESIMAL` | `configuracion.anio_escolar.escala_evaluacion` |
| `regla_nota` | `VARCHAR(20)` | `PROMEDIO_SIMPLE` | `configuracion.anio_escolar.regla_nota` |
| `consolidacion` | `VARCHAR(20)` | `PROMEDIO_PERIODOS` | `configuracion.anio_escolar.consolidacion_periodos` |

El backend ya no recibe, lee ni decide estos tres valores por competencia. `academico.plan_competencia` representa efectivamente la relación entre `plan_curso_id` y `competencia_id`; retirar las columnas heredadas requiere otra migración y una decisión sobre sus valores históricos. Las columnas nuevas y restricciones del año están documentadas en [Configuración](configuracion.md#cambio-de-esquema-de-v9).

**Lo valida el servicio del plan curricular (`ServicioPlanCurricular`):**
- Los nombres no se repiten, sin importar mayúsculas: ni las áreas, ni los cursos, ni las competencias dentro de su área.
- A un área inactiva no se le agregan competencias ni cursos: primero hay que reactivarla.
- Un curso no puede perder un grado en el que ya está planificado (lo exige `fk_plan_curso_grado`).

**Lo valida la escala de calificación (`TablaConversion.validarRangos`, también en el frontend):**
- Están los 4 niveles, sin repetir.
- C empieza en 0, cada nivel empieza justo después del anterior (sin vacíos ni cruces) y AD termina en 20.
- La fecha "Vigente desde" no es anterior a hoy y no se repite.

**Lo valida el plan de evaluación (`ServicioPlanEvaluacion`):**
- Solo se agregan cursos activos, de áreas activas, que se dictan en el grado y que aún no están en su plan.
- Las competencias son del área del curso y no se repiten. Una competencia inactiva solo se conserva si el curso ya la evaluaba.
- Al configurar un curso se insertan y eliminan únicamente las competencias que cambiaron; las que permanecen seleccionadas conservan sus ítems. Una competencia que ya tiene ítems de evaluación no se puede quitar del plan.
- La suma de horas del grado no supera los bloques semanales de la estructura horaria principal de su nivel (bloques por día × días lectivos). Si el año aún no tiene estructura, no hay límite.
- La sección del docente es del mismo grado y año, y el docente es un usuario activo con rol Docente.
- Solo se copia a un grado sin cursos. Se copian los cursos y las competencias que siguen activos, sin los docentes.
- Un año escolar CERRADO no se modifica.
- Los pesos de una competencia se guardan todos juntos: deben incluir a todos los cursos que la evalúan y sumar exactamente 100 %. Después de agregar, configurar, quitar o copiar cursos, una competencia que queda en un solo curso recibe el 100 %; un curso nuevo en una competencia compartida queda sin peso hasta ajustarlo (la tarjeta muestra cuántas competencias tienen pesos incompletos). Copiar un plan copia los pesos. Los cambios de pesos de un grado se serializan con un bloqueo consultivo.

**Lo valida el servicio de ambientes (`ServicioAmbientes`, HU-43):**
- **Código:** lo genera el servicio y no se ingresa ni se edita. El prefijo es `A` (aula regular) o `E` (espacio especial) y el formato `A-001`: el mayor número que ya existe con ese prefijo, más uno. Los códigos que no siguen ese formato (`LAB-01`, `AUD-01`) no cuentan, así que con los datos de prueba el primero es `A-206` y `E-001`. Si otra petición se lleva el mismo código a la vez, la restricción `UNIQUE` lo rechaza y el servicio **reintenta hasta 3 veces**, cada intento en su propia transacción; solo se reintenta por esa restricción y, con otra violación o al tercer choque, responde 409 «No pudimos registrar el ambiente. Inténtalo de nuevo.».
- **Nombre:** obligatorio, hasta 80 caracteres, sin espacios sobrantes. Es **único** sin importar mayúsculas ni espacios sobrantes (las tildes sí cuentan), entre todos los ambientes, activos e inactivos y de cualquier tipo: 409 `Ya existe un ambiente llamado "X".`. La BD no tiene restricción de nombre: dos peticiones simultáneas se serializan con un bloqueo consultivo (`bloquearNombres`) que toman el alta y la edición.
- **Aforo:** mayor que 0 (y cabe en `SMALLINT`, hasta 32767); un aula regular admite como máximo 45 («Un aula regular admite como máximo 45.», el texto de Figma).
- **Recursos:** se recortan, se quitan los vacíos y los repetidos (sin importar mayúsculas; queda el primero) y cada uno admite hasta 60 caracteres. No hay tope de cantidad.
- **Qué no se edita:** el código y el tipo. Si el cuerpo del `PUT` los trae, se ignoran.
- **Desactivar y reactivar** son idempotentes (200 aunque ya estuviera en ese estado) y desactivar se permite **aunque el ambiente esté en uso**: sus secciones y horarios siguen apuntando a él y Horarios solo ofrece los activos (`AmbientesActivos`).
- **Aforo frente a la matrícula (RF-111):** ver la nota siguiente.

**Nota RF-111.** Al **bajar** el aforo de un ambiente, no puede quedar por debajo de los matriculados de **una** de las secciones que lo usan: se compara con la sección que tiene más matriculados, no con la suma. Si no se cumple, 409 «No puedes bajar el aforo a 25: la sección A de 1.° de secundaria tiene 28 estudiantes matriculados.» (el texto no está en Figma). Qué cuenta:
- **Secciones:** solo las `ACTIVA` de años escolares que no están `CERRADO` (`PLANIFICADO` y `ACTIVO` sí). Una sección inactiva o de un año cerrado ya no ocupa el aula.
- **Matrículas:** todas las que ocupan vacante, es decir, todas menos `RETIRADO` y `ANULADA`; por eso una reserva (`SECCION_RESERVADA`), una en validación o con pago pendiente también cuentan. Es la misma definición de `Seccion.SQL_OCUPA_VACANTE` (Configuración); `academico` no puede importarla y la repite en `ConsultasAmbientes`, con un comentario que apunta al original.
- Subir el aforo, o bajarlo hasta exactamente los matriculados, se permite. Aún no se cuentan las reservas de espacios de la HU-48 (ver Pendientes).

**Datos de prueba:**
- Las 12 áreas de Figma. Formación complementaria está inactiva.
- Las competencias de Matemática, Comunicación y Ciencia y Tecnología.
- Los 16 cursos de secundaria.
- El plan 2026 de 1.° de secundaria, con sus horas y las competencias de Matemática y Comunicación. «Resuelve problemas de cantidad» la evalúan Aritmética (60 %) y Geometría (40 %); las demás competencias tienen un solo curso (100 %).
- María Fernández (`U20260004`) dicta Álgebra en 1.° A.
- La tabla de conversión vigente desde 2025 y la versión programada para 2027.
- 8 ambientes (`A-101`, `A-102`, `A-201`, `A-205` inactiva, `LAB-01`, `LAB-02`, `BIB-01`, `AUD-01`). 1.° A usa el aula A-101 y 1.° B la A-102. Sus códigos no siguen el formato nuevo (`A-001`/`E-001`): no se renombraron y el generador ignora los que no encajan.

## API

Rol: Personal administrativo; las rutas de `/planes-evaluacion` también las usa el Coordinador. La tabla completa está en [backend/README.md](../../backend/README.md#api-prefijo-api).

| Método | Ruta | Qué hace |
| --- | --- | --- |
| GET | `/areas/resumen` | Tarjetas de CI-14 (activos de los registrados) y el año escolar activo |
| GET | `/areas` | Áreas con su número de competencias y cursos |
| GET | `/areas/{id}` | Área con sus competencias y cursos (grados en el orden del catálogo) |
| POST · PUT | `/areas` · `/areas/{id}` | Crea o edita un área |
| POST | `/areas/{id}/competencias` · `/areas/{id}/cursos` | Agrega una competencia o un curso al área |
| PUT | `/competencias/{id}` · `/cursos/{id}` | Edita una competencia o un curso |
| POST | `/{areas,competencias,cursos}/{id}/desactivar` · `/reactivar` | Cambia el estado |
| GET | `/tablas-conversion` | Versiones de la tabla de conversión, de la más nueva a la más antigua, con su estado |
| GET | `/tablas-conversion/vigente` | La versión que rige hoy |
| POST | `/tablas-conversion` | Nueva versión `{vigenteDesde, niveles: [{nivel, notaDesde, notaHasta}]}` (HU-55) |
| GET | `/planes-evaluacion?anioEscolarId=&grado=` | Plan del grado: tarjetas, secciones y cursos (HU-56) |
| GET | `/planes-evaluacion/anios` · `/docentes` | Años escolares para el selector · docentes activos |
| GET | `/planes-evaluacion/cursos-disponibles?anioEscolarId=&grado=` | Cursos que se pueden agregar, con las competencias activas de su área |
| GET | `/planes-evaluacion/origenes?anioEscolarId=&grado=` | Planes que se pueden copiar |
| POST | `/planes-evaluacion` · `/planes-evaluacion/copias` | Agrega un curso con `{anioEscolarId, gradoCodigo, cursoId, horasSemanales, competencias: [{competenciaId}]}` · copia otro plan a un grado vacío |
| GET · PUT · DELETE | `/planes-evaluacion/{id}` | Detalle del curso en el plan · cambia `{horasSemanales, competencias: [{competenciaId}]}` · quitarlo |
| GET · PUT | `/planes-evaluacion/{id}/docentes` | Docente de cada sección |
| GET | `/planes-evaluacion/pesos-competencias?anioEscolarId=&grado=` | Vista "Competencias del grado": cursos de cada competencia con su peso, total y si está completa |
| PUT | `/planes-evaluacion/pesos-competencias/{competenciaId}` | Guarda `{anioEscolarId, gradoCodigo, pesos: [{planCursoId, pesoCurso}]}`; deben sumar 100 |
| GET | `/ambientes/resumen` | Tarjetas de CI-26 `{activos, total, aulasRegulares, espaciosEspeciales, aforoTotal}`: cuentan solo ambientes activos y no dependen de los filtros de la lista (HU-43) |
| GET | `/ambientes?tipo=&estado=&q=` | Lista simple, sin paginar, por nombre. `tipo` (`AULA_REGULAR`, `ESPACIO_ESPECIAL`), `estado` (`ACTIVO`, `INACTIVO`) y `q` (busca solo en el nombre) son opcionales. Cada fila trae `{id, codigo, nombre, tipo, aforoMaximo, recursos, estado}`; el código es de solo lectura. 400 con un tipo o estado desconocido |
| POST | `/ambientes` | Nuevo ambiente `{tipo, nombre, aforoMaximo, recursos}` (201); el código sale generado. 400 con datos inválidos; 409 si el nombre ya existe |
| PUT | `/ambientes/{id}` | Edita `{nombre, aforoMaximo, recursos}`; el código y el tipo no cambian. 400 con datos inválidos; 404; 409 si el nombre ya existe o el aforo queda por debajo de la matrícula de una sección que lo usa (RF-111) |
| POST | `/ambientes/{id}/desactivar` · `/reactivar` | Cambia el estado (200 aunque ya estuviera en ese estado). Desactivar se permite aunque esté en uso. 404 |

## Código

- Backend: `backend/src/main/java/com/galiledu/academico/` (`ServicioPlanCurricular`, `ServicioEscalaCalificacion`, `ServicioPlanEvaluacion` y sus controladores). Lo que se lee de otros módulos (años, secciones, estructura horaria, docentes) está en `infraestructura/ConsultasCatalogo.java`.
  - **Ambientes (HU-43):** `dominio/{Ambiente, TipoAmbiente, EstadoAmbiente, CodigoAmbiente}`, `infraestructura/{AmbienteRepositorio, ConsultasAmbientes}`, `aplicacion/{ServicioAmbientes, DtosAmbientes}` y `web/AmbienteController`. `ConsultasAmbientes` lee con SQL de solo lectura las secciones que usan un aula y sus matrículas (RF-111).
- Frontend: `frontend/src/features/catalogo-academico/` (`PlanCurricular`, `EscalaCalificacion`, `PlanEvaluacion`, `CompetenciasGrado` y sus modales). La ruta `src/app/routes/app/administrativo/competencias.tsx` tiene las 3 pestañas; `src/app/routes/app/coordinador/plan-evaluacion.tsx` muestra solo el plan de evaluación.
  - **Ambientes (HU-43), archivos nuevos:**
    - `api/ambientes.ts`: consultas (resumen y lista con `tipo`, `estado` y `q`) y mutaciones (crear, editar, desactivar y reactivar), con invalidación de consultas. Crear y editar muestran sus errores en el modal, por eso silencian el toast global.
    - `components/ambientes.tsx`: la página (tarjetas, filtros y tabla). La dividen `ambientes-resumen.tsx` (4 tarjetas), `ambientes-filtros.tsx` (buscador, chips de tipo y filtro de estado, con Activos preseleccionado), `ambientes-tabla.tsx` (columnas Nombre, Tipo, Aforo máx., Recursos, Estado y menú ⋮; sin columna Código) y `ambientes-orden.ts` (orden en el cliente).
    - `components/dialogos-ambientes.tsx`: modal de crear y editar, con `esquemas-ambientes.ts` (zod). El 409 de nombre repetido se muestra en el campo Nombre y el de aforo bajo la matrícula, en Aforo máximo.
    - `components/dialogo-cambiar-estado-ambiente.tsx`: confirmación de desactivar y reactivar (Figma no tiene un modal propio), con sus avisos.
    - Los tipos `AmbienteResumen`, `ResumenAmbientes`, `SolicitudAmbiente` y `SolicitudEdicionAmbiente` están en `src/types/api.ts`.
    - `components/pagina-ambientes.tsx`: la página completa (título, migas «Configuración › Ambientes físicos», descripción y el botón «Nuevo ambiente») con el modal. La usan las rutas `src/app/routes/app/administrativo/ambientes.tsx` y `src/app/routes/app/admin/ambientes.tsx`.
- Pruebas: `backend/src/test/java/com/galiledu/academico/` (`PlanCurricularIT`, `EscalaCalificacionIT`, `PlanEvaluacionIT`, `dominio/TablaConversionTests`) y `frontend/src/features/catalogo-academico/components/__tests__/`.
  - **Ambientes (frontend):** `api/__tests__/ambientes.test.tsx`, `components/__tests__/{ambientes.test.tsx, ambientes-orden.test.ts, dialogos-ambientes.test.tsx, esquemas-ambientes.test.ts}` y `src/app/routes/app/administrativo/__tests__/ambientes.test.tsx`.
  - **Ambientes (backend):** `dominio/{AmbienteTests, CodigoAmbienteTests}`; `AmbienteMapeoIT` (mapeo del arreglo `recursos`), `AmbienteRepositorioIT`, `ConsultasAmbientesIT` (RF-111), `ServicioAmbientesIT`, `ServicioAmbientesCrearIT` (reintento del código con un choque simulado), `AmbienteIT` (API con rollback), `AmbienteCrearIT` (`POST /ambientes`) y `AmbienteConcurrenciaIT` (dos peticiones reales a la vez).
  - **Las que confirman de verdad:** `crear` usa una transacción por intento (`REQUIRES_NEW`), así que `ServicioAmbientesCrearIT`, `AmbienteCrearIT` y `AmbienteConcurrenciaIT` corren **sin transacción de prueba** (`@Transactional(propagation = NOT_SUPPORTED)`) y limpian lo que crean con `AmbientesDePrueba`: solo las filas nuevas, nunca las de la semilla, y dejan libres `A-206` y `E-001`. Los nombres de esas pruebas empiezan con `ZZ-IT `.

## Pendientes

- [x] Backend y frontend del plan curricular (HU-52, 53, 54).
- [x] Escala de calificación (HU-55).
- [x] Plan de evaluación (HU-56).
- [x] HU-43 Ambientes físicos (iteración 3): backend (API, reglas y pruebas).
- [x] HU-43 Ambientes físicos: **frontend** (listado CI-26, modal de crear y editar, confirmación de desactivar y reactivar, y la ruta).
- [ ] HU-43: agregar el `UNIQUE INDEX (lower(btrim(nombre)))` en `configuracion.ambiente` (requiere una migración nueva). Hoy la unicidad del nombre la valida el servicio y el bloqueo `bloquearNombres` cubre la carrera. Al agregarlo, la prueba `AmbienteRepositorioIT.laBaseNoImpideDosAmbientesConElMismoNombre` debe cambiar para exigir el rechazo por esa restricción.
- [ ] HU-43 con Luciana: cuando exista la tabla de reservas de la HU-48, `ConsultasAmbientes.seccionMasOcupada` debe contarlas al validar el aforo (RF-111); hoy solo cuenta secciones.
- [ ] HU-43 con Luciana: qué muestra Horarios de un ambiente, el nombre o el código (hoy `OcupacionHorariosSql` usa el nombre y la matriz de Figma muestra el código). Como el código ya no se ve en el catálogo, conviene decidirlo antes de mostrarlo.
- [ ] Cuando Configuración tenga su servicio, `ConsultasCatalogo` debería llamarlo en vez de leer sus tablas con SQL.
- [ ] Comparar con Figma los textos exactos de los modales del plan curricular (placeholders y descripciones).
- [ ] Faltan las competencias de las demás áreas en los datos de prueba. Se cargan desde la pantalla.
