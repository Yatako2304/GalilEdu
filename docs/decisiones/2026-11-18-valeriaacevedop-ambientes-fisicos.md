# Ambientes físicos (HU-43): código generado, nombre único y aforo frente a la matrícula

<!-- Copia este archivo como docs/decisiones/AAAA-MM-DD-autor-tema.md (minúsculas y guiones).
     Una entrada por decisión o tarea importante. No edites entradas de otros: si algo cambió,
     crea una nueva que diga "reemplaza a <archivo>". -->

- **Autor:** valeriaacevedop
- **Fecha:** 2026-11-18
- **HU / tarea:** HU-43 (Ambientes físicos, módulo Catálogo académico)
- **Estado:** en progreso: el backend está terminado; falta el frontend

## Qué hice

El backend de los ambientes físicos del colegio (Figma CI-26): tarjetas, lista con filtros, alta, edición, desactivar y reactivar, en `academico` y sobre la tabla `configuracion.ambiente` que ya creó V6, **sin migraciones**. El código de cada ambiente lo genera el backend (`A-001`, `E-001`) y el nombre es único. Bajar el aforo de un aula se valida contra la matrícula de las secciones que la usan (RF-111).

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/dominio/` (`Ambiente`, `TipoAmbiente`, `EstadoAmbiente`, `CodigoAmbiente`)
- `backend/src/main/java/com/galiledu/academico/infraestructura/` (`AmbienteRepositorio`, `ConsultasAmbientes`)
- `backend/src/main/java/com/galiledu/academico/aplicacion/` (`ServicioAmbientes`, `DtosAmbientes`)
- `backend/src/main/java/com/galiledu/academico/web/AmbienteController.java`
- `backend/src/test/java/com/galiledu/academico/` (`AmbienteIT`, `AmbienteCrearIT`, `AmbienteConcurrenciaIT`, `ServicioAmbientesIT`, `ServicioAmbientesCrearIT`, `ConsultasAmbientesIT`, `AmbienteRepositorioIT`, `AmbienteMapeoIT`, `AmbientesDePrueba` y `dominio/`)

## Decisiones y por qué

- **Decisión:** el código **no se ingresa**: lo genera el backend, con prefijo `A` para aula regular y `E` para espacio especial y el formato `A-001`. Es el mayor número que ya existe con ese prefijo, más uno. Es **inmutable** (la columna no se actualiza) y de solo lectura en la API.
  **Por qué:** así no se repite ni se escribe mal, y el usuario solo trabaja con el nombre. Los códigos de los datos de prueba (`A-101`, `LAB-01`, `AUD-01`…) no siguen ese formato: no se renombraron y el generador ignora los que no encajan, así que con la semilla el primero es `A-206` y `E-001`.
  **Alternativas descartadas:** pedirlo en el formulario, como muestra Figma (permite repetidos y errores de escritura), y una secuencia de la BD (los códigos existentes no la respetarían).
- **Decisión:** si dos altas calculan el mismo código a la vez, la restricción `UNIQUE` (`ambiente_codigo_key`) lo rechaza y `crear` **reintenta hasta 3 veces**, cada intento en su propia transacción (`REQUIRES_NEW`, con `saveAndFlush`). Solo se reintenta por esa restricción: otra violación o el tercer choque dan 409 «No pudimos registrar el ambiente. Inténtalo de nuevo.».
  **Por qué:** en PostgreSQL una violación de unicidad deja abortada la transacción en curso, y dentro de ella no se puede reintentar. Antes de añadir el bloqueo del nombre se midió: 25 choques reales de código en 25 rondas, todos resueltos por el reintento. Hoy es la segunda barrera (ver el bloqueo, más abajo).
- **Decisión:** el **nombre es único** sin importar mayúsculas ni espacios sobrantes, entre todos los ambientes (activos e inactivos, de cualquier tipo), y lo valida el **servicio**: 409 `Ya existe un ambiente llamado "X".`. Las tildes no se normalizan («Cómputo» y «Computo» son nombres distintos).
  **Por qué:** un ambiente inactivo se puede reactivar, así que su nombre sigue ocupado. La BD no tenía restricción de nombre y no se quiso agregar una migración en esta HU.
- **Decisión:** el alta y la edición toman un **bloqueo consultivo** de PostgreSQL (`AmbienteRepositorio.bloquearNombres`, con `pg_advisory_xact_lock`) antes de comprobar el nombre.
  **Por qué:** la restricción del código solo frena a dos altas del mismo tipo (`A-…` contra `E-…` no chocan). `AmbienteConcurrenciaIT`, con dos peticiones reales de dos hilos, mostró que dos altas con el **mismo nombre y distinto tipo** daban las dos 201 y dejaban dos ambientes iguales. Con el bloqueo la segunda espera a que la primera confirme y entonces ve el nombre: una da 201 y la otra 409. Es el mismo mecanismo de secciones y estructuras horarias, con otra clave. Lo mismo vale para dos renombres al mismo nombre y para un alta contra un renombre.
  **Alternativas descartadas:** el `UNIQUE INDEX` (ver Pendientes), porque requiere una migración.
- **Decisión:** **desactivar y reactivar son idempotentes** (200 aunque ya estuviera en ese estado, como `Area`) y desactivar se **permite aunque el ambiente esté en uso**.
  **Por qué:** es coherente con Horarios, que solo ofrece los ambientes activos (`AmbientesActivos`) y no cambia nada de lo ya guardado: las secciones y los bloques siguen apuntando al ambiente inactivo. Figma no define el modal «¿Desactivar …?» (el frontend pedirá una confirmación genérica), y no hay un endpoint de uso.
- **Decisión:** **no se edita** el código ni el tipo; en el `PUT` solo cambian el nombre, el aforo y los recursos. Si el cuerpo trae `codigo`, `tipo` o `estado`, se ignoran. El estado cambia solo con desactivar y reactivar.
  **Por qué:** el código identifica al ambiente en Horarios, y cambiar el tipo cambiaría el límite de 45 de un aula que ya tiene secciones.
- **Decisión:** **aforo frente a la matrícula (RF-111):** al **bajar** el aforo, no puede quedar por debajo de los matriculados de **una** de las secciones que usan el ambiente: se compara con la más ocupada, no con la suma. Cuentan las secciones `ACTIVA` de años escolares que no están `CERRADO`, y las matrículas que ocupan vacante, es decir, todas menos `RETIRADO` y `ANULADA` (también una reserva o un pago pendiente). Subir el aforo, o bajarlo hasta exactamente los matriculados, se permite.
  **Por qué:** una sección inactiva o de un año cerrado ya no ocupa el aula. La definición de «ocupa vacante» es la de `Seccion.SQL_OCUPA_VACANTE` (Configuración); `academico` no puede importar esa clase y la repite en `ConsultasAmbientes`, con un comentario que apunta al original. Si allí cambia la regla, hay que cambiarla aquí.
- **Decisión:** el aula regular admite como máximo 45 (también lo exige `ck_ambiente_aforo` en la BD); el aforo debe ser mayor que 0 y caber en `SMALLINT`. Los recursos se recortan, se quitan los vacíos y los repetidos (sin importar mayúsculas) y cada uno admite hasta 60 caracteres; no hay tope de cantidad. `q` busca solo en el nombre, sin distinguir mayúsculas. La lista es simple, sin paginar, como `GET /areas`.
- **Decisión:** los **mensajes de error los redactamos nosotros porque Figma no los define**: el de aforo menor que la matrícula («No puedes bajar el aforo a 25: la sección A de 1.° de secundaria tiene 28 estudiantes matriculados.»), el de nombre repetido, el de ambiente inexistente («El ambiente no existe.») y el de fallo al registrar. Solo el del tope de 45 sale de Figma (el helper del modal: «Personas. Un aula regular admite como máximo 45.»).
- **Decisión:** **Figma sigue mostrando el campo «Código \*» del modal** (con el error «El código LAB-01 ya está registrado.») **y la columna «Código»** de la tabla. Ya no aplican, porque el código no se ingresa ni se muestra en la pantalla; cuando se haga el frontend hay que quitarlos o pedir que Figma se actualice.

## Pendientes / problemas conocidos

- [ ] **Frontend de HU-43:** la ruta `/app/administrativo/ambientes` sigue siendo un placeholder. Los tipos van en `frontend/src/types/api.ts` (archivo compartido: avisar antes de editarlo).
- [ ] **`UNIQUE INDEX (lower(btrim(nombre)))`** en `configuracion.ambiente`: queda pendiente porque requiere una migración nueva (reservar el número `V{n}` antes de crearla). Con el índice, el bloqueo `bloquearNombres` podría quedar como defensa adicional o retirarse. Si se agrega, la prueba **`AmbienteRepositorioIT.laBaseNoImpideDosAmbientesConElMismoNombre` debe cambiar**: hoy documenta que la base **no** lo impide, y pasaría a exigir el rechazo con `rechazaPor` por esa restricción.
- [ ] **Con Luciana (Horarios):** cuando exista la tabla de reservas de la HU-48, `ConsultasAmbientes.seccionMasOcupada` debe contarlas al validar el aforo (RF-111). Hoy solo cuenta secciones.
- [ ] **Con Luciana (Horarios):** qué se muestra de un ambiente en Horarios, el **nombre** o el **código**. Hoy `OcupacionHorariosSql` usa el nombre y la matriz de Figma muestra el código (A-502); como el código ya no se ve en el catálogo, conviene decidirlo antes de mostrarlo.
- [ ] Cuando Horarios y Matrícula ofrezcan un servicio, `ConsultasAmbientes` debería llamarlo en vez de leer sus tablas con SQL.

## Notas para quien siga

- Las pruebas que llaman a `crear` **confirman de verdad** (una transacción por intento): `ServicioAmbientesCrearIT`, `AmbienteCrearIT` y `AmbienteConcurrenciaIT` corren sin transacción de prueba y limpian con `AmbientesDePrueba` (solo lo que crean, nunca la semilla; dejan libres `A-206` y `E-001`). Sus nombres empiezan con `ZZ-IT `. El resto se deshace solo.
- Iniciar sesión en esas pruebas también confirma `ultimo_acceso`; `AmbientesDePrueba` lo guarda y lo restaura.
- Se probó con mutaciones que las pruebas fallan si se quita el reintento del código, el tope de 45, el bloqueo del nombre, o si se valida el nombre después de guardar o la entidad se modifica antes de validar. La menos protegida es «validar el nombre después de guardar», que solo detecta una prueba de interacción (`unNombreRepetidoNoSeReintenta`), porque el resultado visible es el mismo.
- La entidad `Ambiente` vive en `academico` pero mapea la tabla `configuracion.ambiente`, como dice la ficha del módulo.
