# Pruebas automáticas de GalilEdu

> Archivo generado con `node docs/generar-pruebas.mjs`. No se edita a mano: se vuelve a generar cuando cambien los tests. Es la lista de pruebas que existen, no el resultado de ejecutarlas: para ver cuáles pasan, corre las pruebas y luego `node docs/generar-pruebas.mjs --resultados` (genera `docs/pruebas-resultados.md`, que no se sube al repo).

Cada funcionalidad tiene pruebas en el **backend** (Java: de integración contra PostgreSQL real y unitarias de reglas de negocio) y en el **frontend** (Vitest + Testing Library, con la API simulada). Cada línea de abajo es una prueba que se ejecuta en cada validación.

## Resumen

| Funcionalidad | Backend | Frontend | Total |
|---|---:|---:|---:|
| [Estudiantes (inscripción, edición, estado)](#estudiantes-inscripción-edición-estado) | 47 | 77 | 124 |
| [Usuarios y acceso (login, Google, contraseñas, carga masiva)](#usuarios-y-acceso-login-google-contraseñas-carga-masiva) | 110 | 49 | 159 |
| [Matrícula](#matrícula) | 107 | 58 | 165 |
| [Pagos y descuentos](#pagos-y-descuentos) | 81 | 42 | 123 |
| [Horarios y reservas de ambientes](#horarios-y-reservas-de-ambientes) | 307 | 164 | 471 |
| [Configuración (año escolar, periodos, secciones, tarifario)](#configuración-año-escolar-periodos-secciones-tarifario) | 649 | 504 | 1153 |
| [Asistencia (registro, justificaciones, reportes)](#asistencia-registro-justificaciones-reportes) | 55 | 15 | 70 |
| [Académico (calificaciones, catálogo, ambientes)](#académico-calificaciones-catálogo-ambientes) | 327 | 134 | 461 |
| [Base común (rutas, componentes, utilidades)](#base-común-rutas-componentes-utilidades) | 9 | 188 | 197 |
| **Total** | **1692** | **1231** | **2923** |

Cómo correrlas:

```bash
# backend (necesita Docker para PostgreSQL)
cd backend && ./mvnw verify
# frontend
cd frontend && npm test
```

## Estudiantes (inscripción, edición, estado)

47 pruebas de backend y 77 de frontend.

### Backend

#### Edición estudiante · integración (10)

`backend/src/test/java/com/galiledu/estudiantes/EdicionEstudianteIT.java`

- La edición trae los datos con la forma de la inscripción
- Edita los datos del mismo estudiante y de sus familiares
- Aplica las mismas validaciones que la inscripción
- No se puede usar el documento de otra persona
- Un estudiante matriculado no cambia de grado ni de año
- Al cambiar el responsable ambos conservan su cuenta
- Quien deja de ser familiar presente queda inactivo y si vuelve se reactiva
- Si se marca fallecida su cuenta queda inactiva y deja de ser responsable
- Al cambiar el correo de un familiar conserva su usuario y su contraseña
- Conserva el documento del apoderado si es la misma persona

#### Estado estudiante · integración (7)

`backend/src/test/java/com/galiledu/estudiantes/EstadoEstudianteIT.java`

- Un documento sin registrar está libre
- Un estudiante activo no se puede inscribir de nuevo
- Un estudiante retirado no se puede inscribir como nuevo hasta reactivarlo
- Un estudiante eliminado se reinscribe y sus datos se actualizan
- Un estudiante eliminado y retirado también se reinscribe
- El documento de una persona que no es estudiante se rechaza
- Solo administración puede cambiar el estado

#### Inscripción · integración (30)

`backend/src/test/java/com/galiledu/estudiantes/InscripcionIT.java`

- Inscribe con padre existente y crea cuenta a la madre responsable
- Si el responsable ya tiene cuenta solo se vincula
- Sin padres exige apoderado
- Exige los datos obligatorios del paso1
- Válida lugar de nacimiento nacionalidad y nombres
- Un familiar nuevo necesita los mismos datos que un usuario
- Válida contactos y personas autorizadas del paso4
- Exige el correo y el celular del responsable en el paso5
- Consulta si un correo ya está asignado sin importar mayúsculas
- El apoderado necesita su relación con el estudiante
- Exige los datos obligatorios del paso2
- Válida rangos y formatos del paso2
- Los detalles de una respuesta no no se guardan
- Sin discapacidad no guarda el detalle
- Un texto más largo que su columna da400
- Exige dos contactos de emergencia
- Busca persona por documento
- El listado filtra por la situación que sale de la matrícula
- El docente no puede inscribir
- Sube y descarga el documento del apoderado
- El usuario del apoderado muestra al estudiante y su acreditación
- Rechaza archivos que no son pdf ni imagen
- Sin apoderado no se puede subir documento
- Cada familiar presente tiene cuenta aunque no sea el responsable
- El padre fallecido se registra con sus datos pero sin cuenta
- Un fallecido no puede ser responsable de matrícula
- Si ningún padre está presente exige apoderado
- El familiar debe ser mayor de edad
- El fallecido también debe ser mayor de edad
- Con motivo otro la observación es obligatoria

### Frontend

#### Estudiantes lista (7)

`frontend/src/features/estudiantes/components/__tests__/estudiantes-lista.test.tsx`

- el menú de un estudiante activo ofrece Ver, Editar, Retirar y Eliminar
- el menú de un estudiante retirado ofrece Reactivar en vez de Retirar
- Retirar pide confirmación y llama a retirar
- Reactivar llama a reactivar
- Eliminar pide confirmación y llama a DELETE
- cancelar la confirmación no cambia nada
- el filtro de estado incluye Retirado y se envía al backend

#### Familia apoderado (5)

`frontend/src/features/estudiantes/components/__tests__/familia-apoderado.test.tsx`

- la pestaña «Hijos(as)» lista los estudiantes y los contactos de emergencia, sin distrito ni dirección
- padre, madre y apoderado en sus pestañas; quien consulta lleva «(tú)»
- es de solo vista: no hay campos ni botones para editar
- con más de una familia aparece el selector para elegir cuál ver
- sin estudiantes a cargo lo dice

#### Ficha estudiante (2)

`frontend/src/features/estudiantes/components/__tests__/ficha-estudiante.test.tsx`

- al ver un estudiante se abre en Datos personales
- tras registrar una inscripción se abre en Familia

#### Reglas de validación de la inscripción (51)

`frontend/src/features/estudiantes/components/inscripcion/__tests__/esquema.test.ts`

**gradoSugerido (edad al 31 de marzo del año académico)**

- nacido el %s para %i → %s %s
- sin fecha, en edad de inicial o fuera de rango no sugiere nada
**inscripcionSchema (reglas de Figma)**

- una inscripción completa es válida
- ningún campo del Paso 1 trae valor por defecto, salvo la discapacidad (No)
- si nace fuera del Perú no se piden departamento, provincia ni distrito
- si nace en el Perú, departamento, provincia y distrito son obligatorios
- el tipo de documento vacío no oculta los demás errores del paso
- el Paso 1 exige todos los campos salvo correo, referencia, teléfono, segunda lengua y etnia
- apellido materno, referencia, teléfono de casa, correo, segunda lengua y etnia son opcionales
- si tiene discapacidad, "¿Cuál?" es obligatorio
- la fecha de nacimiento debe ser pasada
- sin padre ni madre exige un apoderado
- si el padre no registra, pide el motivo
- Paso 4: nombre, relación y celular del contacto son obligatorios; el teléfono fijo no
- Paso 4: formatos del contacto (letras, teléfono y celular)
- Paso 4: una persona autorizada agregada exige nombre y relación
- Paso 4: los autorizados solo se envían si sale con persona autorizada
- Paso 5: responsable, correo, celular y verificación son obligatorios
- Paso 5: formato del correo y del celular del responsable
- Paso 3: el apoderado exige relación, "Especificar" con Otro y tipo de documento; el archivo no
- exige al menos 2 contactos de emergencia
**Paso 3 · padres y apoderado (CAHU6)**

- un familiar nuevo pide todo salvo apellido materno, teléfono de casa y exalumno
- aplica los formatos de CAHU6 (DNI, celular, correo)
- el tipo de documento del familiar no trae valor por defecto
- solo el sexo del padre y de la madre trae valor por defecto; las casillas empiezan sin marcar
- el apoderado también debe ser mayor de edad
- una fecha futura solo dice que debe ser pasada (no "mayor de edad")
- el padre, la madre y el apoderado deben ser mayores de edad
- "Fallecido(a)" ya no es un motivo de "No registra"
- con el motivo "Otro", la observación es obligatoria
- un fallecido solo necesita sus datos de identidad y no cuenta como presente
- si ninguno está presente (fallecido y no registra) exige un apoderado
- un fallecido no se envía como que vive con el estudiante
- un familiar ya registrado (vinculado) no se vuelve a validar
**Paso 2 · ficha médica**

- exige tipo de sangre, peso, estatura, seguro y fiebre máxima
- acepta los extremos de cada rango con el formato ###.# y #.##
- con alergias, al menos una de las dos
- si padece una enfermedad o toma un medicamento, "¿Cuál?" es obligatorio
- los detalles de una respuesta "No" no se envían
**Editar: desdeEdicion (guardado → formulario)**

- carga los datos guardados con el formato del formulario
- ida y vuelta: lo cargado se vuelve a enviar igual y es válido
- al editar, un familiar ya registrado envía sus datos (se pueden corregir) y se valida
- la casilla "Verifiqué los datos…" empieza sin marcar: hay que volver a confirmarla
- una relación del apoderado que no está en la lista vuelve como "Otro" + Especificar
**aSolicitud (formulario → contrato del backend)**

- una persona ya registrada solo envía su id
- normaliza celulares, números y respuestas Sí/No
- "no registra" se envía con su motivo y sin persona
**documento del apoderado**

- acepta PDF, JPG o PNG de hasta 5 MB
- rechaza otros formatos y archivos de más de 5 MB
- si la relación es "Otro", se envía el texto de "Especificar"
**avisoCuentasNuevas**

- nombra solo los usuarios que se crearon

#### Familiar fallecido (2)

`frontend/src/features/estudiantes/components/inscripcion/__tests__/familiar-fallecido.test.tsx`

- la madre fallecida se registra con sus datos, pero no recoge al estudiante, no es responsable ni tiene usuario
- el padre y la madre (aunque esté fallecida) deben ser mayores de edad: el aviso sale al salir de la fecha

#### Inscripción wizard (10)

`frontend/src/features/estudiantes/components/inscripcion/__tests__/inscripcion-wizard.test.tsx`

- paso 3: sin botón "Buscar", busca al salir del N.° de documento y solo si está completo
- un documento libre deja pasar al paso 2
- un estudiante activo no deja pasar y el aviso sale en el N.° de documento
- al salir del N.° de documento ya avisa que existe, sin pulsar "Siguiente"
- un selector y la fecha vacíos se marcan al salir del campo, sin pulsar "Siguiente"
- un estudiante retirado (inactivo) no se puede inscribir como nuevo
- un estudiante eliminado sí deja continuar (sus datos se actualizan al registrar)
- si el documento no pasa, se vuelve a consultar al corregirlo
- N.° de documento y Grado esperan al tipo de documento y al nivel
- el teléfono de casa solo acepta números, hasta 15 (el largo de la base de datos)

## Usuarios y acceso (login, Google, contraseñas, carga masiva)

110 pruebas de backend y 49 de frontend.

### Backend

#### Autenticación · integración (9)

`backend/src/test/java/com/galiledu/usuarios/AutenticacionIT.java`

- Login correcto devuelve token y roles
- El usuario no distingue mayúsculas
- Contraseña incorrecta da401 con mensaje de figma
- Usuario inexistente da el mismo mensaje
- Cuenta inactiva no puede entrar
- Se bloquea al quinto intento fallido
- Campos vacios dan400
- Me sin token da401
- Me con token devuelve el usuario

#### Cambio contraseña · integración (7)

`backend/src/test/java/com/galiledu/usuarios/CambioContrasenaIT.java`

- Con la contraseña actual correcta se puede continuar
- Una contraseña actual incorrecta da el mensaje de figma
- Al cambiarla entra con la nueva y se cierran las sesiones abiertas
- La nueva debe cumplir la regla y ser distinta de la actual
- Cambiar vuelve a comprobar la contraseña actual
- Al quinto intento fallido se bloquea como en el inicio de sesión
- Sin sesión no se puede

#### Carga masiva · integración (6)

`backend/src/test/java/com/galiledu/usuarios/CargaMasivaIT.java`

- Descarga la plantilla
- Validar informa errores por fila sin crear nada
- Cargar crea solo las filas validas
- Lee la plantilla de excel
- Rechaza archivos sin las columnas
- Solo el administrador puede cargar

#### Gestion usuarios · integración (26)

`backend/src/test/java/com/galiledu/usuarios/GestionUsuariosIT.java`

- Crea usuario con nombre generado y varios roles
- Rechaza documento duplicado
- Rechaza correo duplicado sin importar mayúsculas
- Válida formato de datos
- Detalle del apoderado trae sus estudiantes a cargo
- Un estudiante eliminado ya no sale a cargo
- Detalle del tutor trae sus secciones del año en curso
- Tutoria incluye cotutoria y excluye secciones inactivas y otros años
- Un estudiante retirado sale a cargo como retirado
- Exige todos los datos del usuario
- Válida teléfono de casa y correo con extensión
- Edita datos y roles
- Apoderado tutor y estudiante no se asignan desde el formulario
- Registrar el documento de un eliminado reactiva su cuenta
- Editar con correo de otro usuario da409
- Desactivado no puede entrar y reactivado si
- No puede desactivarse ni eliminarse a si mismo
- Eliminar es logico y desaparece de la lista
- Lista filtra por rol estado y texto
- Resumen del inicio
- Desactivar corta la sesión abierta de inmediato
- Cambiar el rol cambia los permisos del token ya emitido
- Quitar el rol administrador corta sus permisos de inmediato
- Los roles solo se cambian en usuarios activos
- Eliminar corta la sesión abierta
- Solo el administrador gestiona usuarios

#### Inicio sesión google · integración (13)

`backend/src/test/java/com/galiledu/usuarios/InicioSesionGoogleIT.java`

- Por defecto google está desactivado y el boton no se muestra
- El administrador activa google y el boton aparece
- Solo el administrador cambia la configuración
- Sin llaves de google no se puede activar pero si desactivar
- Con el mismo correo registrado entra con los permisos de su rol
- Con google desactivado no entra ni consulta a google
- Un correo que no es de ningún usuario no entra
- Un correo que google no verifico no entra
- Una cuenta desactivada no entra con google
- Una cuenta bloqueada por intentos fallidos tampoco entra con google
- Si google rechaza el código no entra
- Sin código responde400
- Usuario y contraseña siguen funcionando con google activado

#### Mi familia · integración (4)

`backend/src/test/java/com/galiledu/usuarios/MiFamiliaIT.java`

- Los hermanos con el mismo padre madre y apoderado son una sola familia
- Muestra padre madre y apoderado y marca a quien consulta
- Los contactos de emergencia de los hermanos no se repiten
- Solo el apoderado puede verla

#### Mis datos · integración (7)

`backend/src/test/java/com/galiledu/usuarios/MisDatosIT.java`

- Cualquier rol ve sus propios datos
- Cualquier rol actualiza sus propios datos sin tocar roles
- Funciona para otro rol distinto de administrador
- Exige los datos completos como en hu9
- Rechaza documento ya registrado a otra persona
- Rechaza correo ya registrado a otra persona
- Sin sesión no puede ver ni editar

#### Recuperación contraseña · integración (12)

`backend/src/test/java/com/galiledu/usuarios/RecuperacionContrasenaIT.java`

- Con un correo registrado envia un código que vence en diez minutos
- El correo no distingue mayúsculas
- Un correo sin cuenta da el mensaje de figma
- Una cuenta desactivada no puede recuperarla
- Solo se puede pedir otro código cada sesenta segundos y anula el anterior
- Con el código correcto restablece la contraseña y el código no se reutiliza
- Un código incorrecto da el mensaje de figma y al quinto intento se anula
- Un código vencido no sirve
- Sin haber pedido un código no se verifica nada
- Una contraseña que no cumple la regla se rechaza sin gastar el código
- Restablecer quita el bloqueo por intentos fallidos
- Un correo mal escrito se rechaza antes de buscar

#### Generador credenciales · unitaria (3)

`backend/src/test/java/com/galiledu/usuarios/dominio/GeneradorCredencialesTests.java`

- Nombre usuario sigue el formato de rf15
- Rechaza correlativos fuera de rango
- Contraseña tiene mayuscula minuscula digito y simbolo

#### Política contraseña · unitaria (4)

`backend/src/test/java/com/galiledu/usuarios/dominio/PoliticaContrasenaTests.java`

- Acepta las que cumplen la regla de figma
- Rechaza las que no cumplen
- No admite espacios ni más de64 caracteres
- Una vacia o nula no cumple

#### Usuario · unitaria (3)

`backend/src/test/java/com/galiledu/usuarios/dominio/UsuarioTests.java`

- Se bloquea quince minutos al quinto intento fallido
- Un acceso exitoso reinicia los intentos
- Desactivado o eliminado no puede iniciar sesión

#### Archivo usuarios · unitaria (6)

`backend/src/test/java/com/galiledu/usuarios/infraestructura/ArchivoUsuariosTests.java`

- Reconoce encabezados sin importar tildes ni mayúsculas
- Lee csv con punto y coma y comillas
- Ignora filas vacias
- Exige las columnas obligatorias
- Rechaza formatos no soportados
- La plantilla se puede volver a leer

#### Cuentas google o auth · unitaria (3)

`backend/src/test/java/com/galiledu/usuarios/infraestructura/CuentasGoogleOAuthTests.java`

- Con id de cliente y clave secreta está configurado
- Sin alguna de las llaves no está configurado
- Sin configurar no llama a google

#### Notificador credenciales · unitaria (7)

`backend/src/test/java/com/galiledu/usuarios/infraestructura/NotificadorCredencialesTests.java`

- La plantilla lleva el código el nombre escapado y ningún marcador sin reemplazar
- Envia un correo html con el logo incrustado y la versión de texto
- Sin smtp no envia nada y el código va al log
- Si el servidor rechaza el correo avisa con false
- La plantilla de credenciales lleva usuario contraseña y enlace sin marcadores
- Envia las credenciales en un correo html con el logo y el enlace al login
- Sin smtp las credenciales solo van al log

### Frontend

#### Aviso contraseña (3)

`frontend/src/features/auth/components/__tests__/aviso-contrasena.test.tsx`

- HU-4: muestra «Contraseña restablecida correctamente» y «Continuar» lo cierra
- HU-5: muestra «Contraseña actualizada correctamente» y que se cerró la sesión
- sin tipo no se ve

#### Cambiar contraseña (4)

`frontend/src/features/auth/components/__tests__/cambiar-contrasena.test.tsx`

- con la contraseña actual correcta pasa a «Nueva contraseña» y la cambia
- una contraseña actual incorrecta muestra «Contraseña incorrecta.» debajo del campo
- un error al guardar se muestra bajo el botón
- la X vuelve al inicio

#### Ingreso google (5)

`frontend/src/features/auth/components/__tests__/ingreso-google.test.tsx`

- con Google desactivado no aparece el botón
- con Google activado entra con el código que devuelve la ventana de Google
- si el correo de Google no es de ningún usuario muestra el mensaje del backend
- si el usuario cierra la ventana de Google no muestra ningún error
- si la ventana de Google falla muestra un aviso para reintentar

#### Login form (3)

`frontend/src/features/auth/components/__tests__/login-form.test.tsx`

- muestra el error del backend debajo de la contraseña
- el botón está deshabilitado hasta llenar usuario y contraseña
- muestra "¿Olvidaste tu contraseña?" con el enlace a la recuperación (HU-4)

#### Mis datos (5)

`frontend/src/features/auth/components/__tests__/mis-datos.test.tsx`

- primero muestra los datos tal cual, sin campos para escribir
- «Editar datos» muestra los campos; al guardar vuelve a la vista con los datos nuevos
- «Cancelar» descarta lo escrito y no guarda nada
- conserva «exalumno» aunque la casilla no se muestre (solo se pide a apoderados)
- es igual para el apoderado: «Mis datos» no muestra la familia

#### Recuperar contraseña (7)

`frontend/src/features/auth/components/__tests__/recuperar-contrasena.test.tsx`

- el flujo completo: pide el código, lo valida y restablece la contraseña
- un correo sin cuenta muestra el mensaje de Figma debajo del campo
- un correo mal escrito no se envía
- un código incorrecto muestra «Código de verificación incorrecto.»
- «Reenviar código» espera 60 segundos y luego pide un código nuevo
- la contraseña debe cumplir la regla de Figma y repetirse igual
- la X vuelve a iniciar sesión

#### Carga masiva (3)

`frontend/src/features/usuarios/api/__tests__/carga-masiva.test.ts`

**validarArchivoCarga (HU-8)**

- acepta .xlsx, .xls y .csv
- rechaza otros formatos y archivos de más de 10 MB
- detalleErroresCsv escapa comillas y comas

#### Verificar documento (4)

`frontend/src/features/usuarios/api/__tests__/verificar-documento.test.ts`

- documento libre
- documento ya registrado (usuario, persona o estudiante): mismo mensaje
- documento de un usuario eliminado: se puede volver a registrar, pero no usar al editar
- con formato inválido no consulta

#### Configuración acceso (3)

`frontend/src/features/usuarios/components/__tests__/configuracion-acceso.test.tsx`

- usuario y contraseña siempre están activos y Google se activa con el interruptor
- se puede desactivar Google
- sin las llaves de Google en el servidor el interruptor queda bloqueado y se avisa

#### Diálogo usuario (9)

`frontend/src/features/usuarios/components/__tests__/dialogo-usuario.test.tsx`

- Apoderado y Tutor están bloqueados; los demás roles se pueden elegir
- apoderado y tutor: muestra sus estudiantes a cargo y sus secciones
- un usuario sin esos roles no muestra estudiantes a cargo ni tutoría
- HU-10: en una cuenta inactiva los roles no se pueden cambiar, pero los datos sí
- HU-10: en una cuenta activa los roles se pueden cambiar
- el documento de otro usuario se marca en el campo junto con los demás errores
- "Nuevo usuario" no pide si es exalumno del colegio
- si el usuario es apoderado (aunque tenga otro rol), sí pide si es exalumno
- si el usuario no es apoderado, no pide si es exalumno

#### Usuarios lista (3)

`frontend/src/features/usuarios/components/__tests__/usuarios-lista.test.tsx`

- muestra usuarios con su rol, el contador de roles extra y el estado
- el filtro de estado se envía al backend
- el menú de una fila abre "Editar" con el id del usuario

## Matrícula

107 pruebas de backend y 58 de frontend.

### Backend

#### Datos matrícula apoderado · integración (2)

`backend/src/test/java/com/galiledu/matricula/DatosMatriculaApoderadoIT.java`

- Muestra los datos del estudiante y del apoderado
- Con la matrícula confirmada muestra la sección

#### Derivación pagos · integración (6)

`backend/src/test/java/com/galiledu/matricula/DerivacionPagosIT.java`

- Muestra la cuota de matrícula para pagar aunque un hermano deba
- Con la matrícula confirmada el apoderado ve los cambios de sección sin quien los hizo
- Pagar la matrícula usa el pago en linea y avisa que matrícula pagar
- Al confirmarse el pago queda matriculado con código y pensiones
- Con pensiones vencidas propias no puede pagar la matrícula
- Sin cuota de matrícula no hay bloque de pago

#### Esquema matrícula pagos · integración (11)

`backend/src/test/java/com/galiledu/matricula/EsquemaMatriculaPagosIT.java`

- El apoderado llega a sus hijos sus matrículas y su deuda vencida
- El compromiso tiene la matrícula diez pensiones y una constancia por pago
- Un estudiante tiene una sola matrícula por año
- Una matrícula matriculada necesita su código
- Una matrícula con su código si se registra
- No se genera dos veces la cuota de matrícula
- Un descuento de varios años rebaja las cuotas y el monto final se calcula solo
- Una cuota rebajada dice que descuento la rebajo
- Solo hay un descuento activo por estudiante
- Una transacción de la pasarela no se registra dos veces
- Un documento de matrícula no puede pesar más de10 mb

#### Matrícula apoderado documentos · integración (9)

`backend/src/test/java/com/galiledu/matricula/MatriculaApoderadoDocumentosIT.java`

- Lista los documentos con su plantilla y lo ya subido
- Con el período cerrado no puede subir
- Descarga la plantilla del documento
- Sube y luego ve su documento y rechaza otros formatos
- Con un documento observado solo reemplaza ese y los aprobados quedan bloqueados
- No puede enviar a validación sin todos los documentos
- Con todos los documentos la envia a validación y ya no se puede editar
- No toca los documentos de un estudiante que no está a su cargo
- Solo el apoderado usa estos endpoints

#### Matrícula apoderado · integración (6)

`backend/src/test/java/com/galiledu/matricula/MatriculaApoderadoIT.java`

- Lista los hijos del apoderado
- La deuda de la hermana no bloquea la matrícula de mateo
- La sección solo se muestra con la matrícula confirmada
- La deuda de Sofía bloquea su propia matrícula
- No puede ver un estudiante que no está a su cargo
- Otro rol no puede usar la vista del apoderado

#### Matrícula detalle · integración (21)

`backend/src/test/java/com/galiledu/matricula/MatriculaDetalleIT.java`

- El detalle trae alumno sección paso y documentos
- Una matrícula que no existe es404
- Lista las secciones del grado con su ocupación
- Cambia de sección libera la vacante y deja el historial
- El cambio de sección exige un motivo válido
- El historial lista los cambios del más reciente al más antiguo
- El historial solo lo ve el personal administrativo y da no encontrado si no existe
- No cambia a una sección llena ni a la misma ni con el período cerrado
- Sube y luego ve el documento y al reemplazarlo vuelve a revisión
- Si el colegio completa todos los documentos la matrícula pasa a validación
- Observar un documento devuelve la matrícula al apoderado y solo ese documento queda observado
- Al reemplazar el documento observado la matrícula vuelve a validación
- No se observa sin motivo ni fuera de validación
- Validar aprueba los documentos y deja la matrícula pendiente de pago
- No se válida una matrícula con documentos observados o no en validación
- Sin tarifario confirmado no se válida y se explica el motivo
- Un documento aprobado no se reemplaza
- Ver un documento no subido es404 y un archivo inválido es400
- La ficha del estudiante ubica su matrícula vigente
- Un estudiante sin matrícula devuelve matrícula id nulo
- Solo el personal administrativo lo ve

#### Matrícula documentos administrativo · integración (6)

`backend/src/test/java/com/galiledu/matricula/MatriculaDocumentosAdministrativoIT.java`

- Elimina el documento subido
- Un documento que no se subio es404
- En validación la matrícula vuelve a esperar documentos
- Un documento aprobado no se elimina
- Con la matrícula pendiente de pago no se elimina
- El apoderado no usa este endpoint

#### Matrícula extemporanea · integración (9)

`backend/src/test/java/com/galiledu/matricula/MatriculaExtemporaneaIT.java`

- Con el período cerrado registra la matrícula extemporanea con su motivo
- El motivo y la sección son obligatorios
- La sección debe ser del grado y tener cupo
- Con el período abierto no se registra extemporanea
- Si el período no la permite se rechaza
- No duplica la matrícula del año
- El apoderado completa una extemporanea aunque el período este cerrado
- Una matrícula regular con el período cerrado sigue bloqueada
- Otro rol no registra extemporaneas

#### Matrícula iniciada por apoderado · integración (8)

`backend/src/test/java/com/galiledu/matricula/MatriculaIniciadaPorApoderadoIT.java`

- Sin matrícula puede empezar en el paso1
- Al enviar todos los documentos se crea la matrícula en validación
- Sin todos los documentos no se crea nada
- No se inicia dos veces
- Sin matrícula no se sube uno por uno ni se envia por separado
- Con el período cerrado no puede iniciar
- Un hijo inscrito para otro año no puede iniciar
- No inicia la matrícula de un estudiante que no está a su cargo

#### Matrículas período · integración (20)

`backend/src/test/java/com/galiledu/matricula/MatriculasPeriodoIT.java`

- El período trae su estado y los indicadores
- El listado muestra solo a quienes tienen matrícula
- Un estudiante inscrito sin matrícula no aparece ni cuenta
- Filtra por texto grado y estado
- Ordena y página con una lista cerrada de columnas
- Ordena por actividad reciente y muestra la fecha de la última actividad
- Subir un documento cuenta como actividad de la matrícula
- Un período que no existe es404
- El buscador muestra solo a inscritos sin matrícula
- Al elegir al alumno muestra sus datos la sección y los documentos
- El personal administrativo descarga la plantilla de un documento
- Con todos los documentos la matrícula nace en validación
- Guarda la matrícula con sección reservada y sus documentos
- Sin documentos también se guarda
- Sin cupo en la sección lo asigna a la siguiente del grado
- Respeta la sección elegida y si está llena la reasigna
- Con el período cerrado no se asignan secciones
- No se matrícula dos veces ni a un estudiante de otro año
- Rechaza archivos que no son pdf ni imagen o que no son del período
- Solo el personal administrativo lo ve

#### Sección asignada matrícula · integración (5)

`backend/src/test/java/com/galiledu/matricula/SeccionAsignadaMatriculaIT.java`

- En proceso la sección es solo seleccionada en el detalle
- Con la matrícula formalizada la sección queda asignada
- Una matrícula anulada no queda relacionada con la sección
- La tabla del período separa asignada y seleccionada
- El apoderado ve la sección por asignar hasta que se formalice

#### Escucha pago matrícula · unitaria (4)

`backend/src/test/java/com/galiledu/matricula/aplicacion/EscuchaPagoMatriculaTests.java`

- Matrícula y genera las pensiones
- Si ya estaba matriculado no genera otra vez las pensiones
- Un error en una matrícula no detiene las demas
- Un pago sin matrículas no hace nada

### Frontend

#### Consulta matrícula (11)

`frontend/src/features/matricula/components/__tests__/consulta-matricula.test.tsx`

- muestra el periodo, grado, sección y estado asignados
- muestra la sección oficial y su tutor cuando la matrícula está formalizada
- permite elegir otro estudiante a cargo
- informa cuando el periodo cerró y aún no existe una sección
- aclara que la deuda de otro hijo bloquea la matrícula seleccionada
- indica cuando la deuda pertenece al hijo seleccionado
- con la documentación validada avisa de la cuota y muestra el pago
- ya matriculado, el avance muestra los 4 pasos completados
- la constancia y el cronograma solo se ofrecen con la matrícula confirmada
- sin matrícula y sin impedimentos el apoderado la empieza en el paso 1
- muestra los cambios de sección de la matrícula confirmada

#### Detalle matrícula (18)

`frontend/src/features/matricula/components/__tests__/detalle-matricula.test.tsx`

- muestra datos, estado de cada documento y deja Validar deshabilitado
- con el periodo cerrado y la sección reservada no se puede cambiar de sección
- una matrícula retirada no permite subir ni reemplazar documentos
- sube un documento pendiente
- HU-72: elimina un documento subido después de confirmar
- HU-72: no ofrece eliminar un documento aprobado o con pago pendiente
- HU-25: muestra que la matrícula es extemporánea y su motivo
- cambia de sección tras confirmar
- HU-26: el historial muestra los cambios con su motivo y quién los hizo
- HU-26: sin cambios previos no se muestra la tarjeta de historial
- descarga la plantilla de los documentos que la tienen
- observa un documento en revisión con su motivo
- un documento aprobado u observado no se puede observar
- valida la matrícula tras confirmar "¿Aprobar documentación?"
- un doble clic en "Aprobar" envía una sola validación
- Validar queda deshabilitado mientras haya un documento observado
- un documento aprobado ya no se reemplaza ni muestra su plantilla
- matriculado: el último paso del indicador figura completado

#### Diálogo nueva matrícula (9)

`frontend/src/features/matricula/components/__tests__/dialogo-nueva-matricula.test.tsx`

- se llama "Nueva matrícula" y empieza buscando al alumno
- al elegirlo muestra sus datos, su sección y los documentos pendientes
- cambia la sección propuesta y la envía al guardar
- sube un documento, lo muestra con Ver y Reemplazar y lo envía al guardar
- descarga la plantilla de un documento que la tiene
- rechaza un archivo que no es PDF ni imagen
- sin vacantes en el grado avisa y no deja guardar
- al reabrir el modal vuelve a consultar y ya propone la sección recién creada
- HU-25: registra una matrícula extemporánea con sección elegida y motivo

#### Documentos apoderado (6)

`frontend/src/features/matricula/components/__tests__/documentos-apoderado.test.tsx`

- lista los documentos pendientes con su plantilla y no deja enviar
- sube el documento firmado y rechaza un archivo que no es PDF ni imagen
- con todos los documentos subidos envía a validación
- en validación solo se ven los documentos, sin acciones de carga
- con un documento observado solo se reemplaza ese y no deja enviar hasta corregirlo
- sin matrícula guarda los archivos en la pantalla y al enviar crea la matrícula con todos

#### Matrículas período (7)

`frontend/src/features/matricula/components/__tests__/matriculas-periodo.test.tsx`

- muestra el aviso con la fecha de cierre real del periodo, no una fija
- con el periodo cerrado el aviso pasa a advertencia
- lista a quienes ya tienen matrícula, con indicadores del servidor
- sin matrículas dice que aún no hay estudiantes en el periodo
- el buscador, el filtro de grado y el orden se piden al servidor
- por defecto ordena por actividad reciente, lo último primero, y muestra la fecha
- se puede volver al orden por nombre y a la actividad reciente

#### Sección del estudiante (6)

`frontend/src/features/matricula/components/__tests__/seccion-del-estudiante.test.tsx`

- muestra el historial de cambios de sección cuando hubo cambios
- sin cambios no aparece el historial
- muestra la sección del estudiante y el botón para cambiarla
- si la matrícula ya no permite cambiar de sección, el botón queda deshabilitado
- un estudiante sin matrícula no tiene sección ni botón
- tras cambiar de sección, al abrir otra vez el modal ya marca la nueva como actual

#### Constancia matrícula (1)

`frontend/src/features/matricula/lib/constancia-matricula.test.ts`

- la constancia incluye alumno, código y escapa el HTML

## Pagos y descuentos

81 pruebas de backend y 42 de frontend.

### Backend

#### Catálogo descuentos · integración (9)

`backend/src/test/java/com/galiledu/pagos/CatalogoDescuentosIT.java`

- Lista el catálogo ordenado por denominación
- Registra un descuento de porcentaje único
- Rechaza un descuento sin conceptos o sobre el limite
- No repite la denominación
- El descuento por hermanos exige dos tramos que no se superpongan
- Edita los tramos del descuento por hermanos
- Desactiva y reactiva un descuento
- El limite de la institución se puede cambiar
- Otro rol no puede ver el catálogo

#### Compromiso pago · integración (11)

`backend/src/test/java/com/galiledu/pagos/CompromisoPagoIT.java`

- Lista los compromisos del estudiante
- Muestra el cronograma con resumen y cuota vencida
- Compromiso inexistente devuelve404
- Otro rol no puede ver el compromiso
- Genera las diez pensiones al matricular y no las duplica
- No genera cronograma si la matrícula no está formalizada
- El retiro anula solo las pensiones que vencen después
- Otro rol no puede generar el cronograma
- Estado de cuenta del año lista los compromisos con su deuda
- La morosidad muestra la pensión vencida con su recargo separado
- Morosidad de un compromiso inexistente devuelve404

#### Cuota ingreso · integración (4)

`backend/src/test/java/com/galiledu/pagos/CuotaIngresoIT.java`

- Al estudiante nuevo se le genera la cuota de ingreso con la de matrícula
- Al continuador no se le cobra ingreso
- Sin monto de ingreso en el tarifario no se genera
- La cuota de ingreso se cobra una sola vez

#### Cuota matrícula · integración (5)

`backend/src/test/java/com/galiledu/pagos/CuotaMatriculaIT.java`

- Genera la cuota de matrícula con el tarifario y no la duplica
- No genera la cuota si el tarifario no está confirmado
- Consulta una cuota de matrícula pagada
- No genera cuota si la matrícula ya está formalizada
- Otro rol no puede generar la cuota

#### Descuento aplicado · integración (7)

`backend/src/test/java/com/galiledu/pagos/DescuentoAplicadoIT.java`

- Lista los descuentos aplicables segun los hermanos matriculados
- Aplica el descuento solo a las cuotas pendientes
- Reemplazar el descuento vigente exige confirmación
- Revocar restituye el monto original
- No aplica un descuento inactivo ni uno sin tramo
- El motivo es obligatorio
- El apoderado no puede aplicar descuentos

#### Descuento hasta fin escolaridad · integración (5)

`backend/src/test/java/com/galiledu/pagos/DescuentoHastaFinEscolaridadIT.java`

- Se aplica sin fecha de fin hasta que termine el colegio
- Con fecha de fin la fecha es obligatoria
- Hasta fin de escolaridad no admite fecha de fin
- Con fecha de fin guarda la fecha
- Sin el indicador una fecha vacia sigue siendo sin fin

#### Descuento hermano menor · integración (4)

`backend/src/test/java/com/galiledu/pagos/DescuentoHermanoMenorIT.java`

- Al hermano mayor no le corresponde el descuento por hermanos
- Al hermano menor si le corresponde
- El backend rechaza aplicarlo al hermano que no corresponde
- Si los menores son gemelos ambos lo reciben

#### Deuda apoderado · integración (11)

`backend/src/test/java/com/galiledu/pagos/DeudaApoderadoIT.java`

- Lista los hijos con su saldo ordenados por apellido
- Muestra el cronograma con el recargo de la cuota vencida
- Muestra el historial de pagos del más reciente al más antiguo
- Lista los compromisos de su hija
- No ve la deuda de un estudiante ajeno
- Un compromiso inexistente devuelve404
- Descarga la constancia de un pago en pdf
- No descarga la constancia de otra familia
- Un pago inexistente devuelve404
- El personal no descarga constancias desde la vista del apoderado
- El personal usa su propia vista

#### Pago en linea · integración (14)

`backend/src/test/java/com/galiledu/pagos/PagoEnLineaIT.java`

- Muestra las cuotas por pagar con capital y recargo separados
- Crea la orden con el recargo y deja la cuota en proceso
- Exige pagar las pensiones en orden
- Una cuota en proceso no entra a otra orden
- Pago aprobado registra el pago con constancia y recargo
- Una notificación repetida no duplica el pago
- Pago rechazado devuelve las cuotas a su estado
- Cancelar libera las cuotas
- Si cierra la pasarela puede retomar o cancelar el pago desde pensiones
- Una orden vencida ya no se ofrece para retomar
- La orden sin pagar expira a los treinta minutos
- Si la pasarela confirma otro monto el pago queda observado
- No se pueden pagar cuotas ajenas ni inexistentes
- La orden de otro usuario no se ve

#### Cuota · unitaria (5)

`backend/src/test/java/com/galiledu/pagos/dominio/CuotaTests.java`

- Dentro de la gracia no hay recargo
- Cobra la tasa por cada treinta días completos como en figma
- Nunca pasa del tope
- Se calcula sobre el capital con descuento
- Una cuota que no es pensión no genera mora

#### Archivo constancia pago · unitaria (3)

`backend/src/test/java/com/galiledu/pagos/infraestructura/ArchivoConstanciaPagoTests.java`

- Incluye los datos del pago las cuotas el recargo y el total
- Un pago observado muestra el estado y su observación
- Muchas cuotas continuan en otra página y el total va al final

#### Firma stripe · unitaria (3)

`backend/src/test/java/com/galiledu/pagos/infraestructura/FirmaStripeTests.java`

- Acepta una firma válida
- Rechaza otro secreto otro cuerpo y firma vieja
- Convierte soles a centimos

### Frontend

#### Catálogo descuentos (7)

`frontend/src/features/pagos/components/__tests__/catalogo-descuentos.test.tsx`

- muestra el catálogo y el formato de los porcentajes
- crea un descuento de porcentaje único sin validar los tramos ocultos
- permite cambiar el límite institucional de descuento
- el formulario por hermanos empieza con dos tramos y permite agregar otro
- permite quitar un tramo mientras conserva al menos una fila editable
- avisa si Desde es mayor que Hasta y retira el aviso al corregirlo
- rechaza tramos superpuestos y acepta rangos separados en cualquier orden

#### Descuento compromiso (1)

`frontend/src/features/pagos/components/__tests__/descuento-compromiso.test.tsx`

- muestra la vigencia hasta que el estudiante termine el colegio

#### Diálogo aplicar descuento (3)

`frontend/src/features/pagos/components/__tests__/dialogo-aplicar-descuento.test.tsx`

- avisa el reemplazo del descuento vigente y exige el motivo
- permite mantener el descuento hasta que el estudiante termine el colegio
- explica que el descuento por hermanos corresponde al hermano menor

#### Formato (4)

`frontend/src/features/pagos/components/__tests__/formato.test.ts`

- formatea montos en soles como en Figma
- la cuota vencida se muestra como Vencida aunque su estado sea PENDIENTE
- el buscador encuentra por nombre sin tildes y por DNI
- la antigüedad se muestra en años

#### Morosidad compromiso (2)

`frontend/src/features/pagos/components/__tests__/morosidad-compromiso.test.tsx`

- muestra el monto pendiente y el recargo por separado con sus totales
- muestra el estado vacío cuando no hay cuotas vencidas

#### Pago matrícula apoderado (5)

`frontend/src/features/pagos/components/__tests__/pago-matricula-apoderado.test.tsx`

- paga solo la cuota de matrícula con el pago en línea
- no deja pagar y explica el motivo
- con la matrícula pagada no muestra el botón
- sin cuota generada muestra la tarjeta explicando cuándo se podrá pagar
- muestra el mismo detalle que ve el colegio

#### Pago matrícula card (3)

`frontend/src/features/pagos/components/__tests__/pago-matricula-card.test.tsx`

- muestra la cuota generada con los datos del backend
- genera la cuota cuando todavía no existe
- con la matrícula confirmada muestra su código

#### Pensiones apoderado (5)

`frontend/src/features/pagos/components/__tests__/pensiones-apoderado.test.tsx`

- muestra la deuda y cambia a un hijo sin compromiso
- marca las cuotas en orden y paga las seleccionadas
- sin cuotas por pagar, "Pagar seleccionadas" queda deshabilitado
- con un pago abierto ofrece continuarlo o cancelarlo
- descarga la constancia del pago en PDF desde la pestaña Pagos

#### Resultado pago (3)

`frontend/src/features/pagos/components/__tests__/resultado-pago.test.tsx`

- tras pagar la matrícula ofrece ver la matrícula de ese hijo
- un pago de pensiones no ofrece ver la matrícula
- volver a pensiones abre las pensiones del estudiante que pagó

#### Selección cuotas (4)

`frontend/src/features/pagos/components/__tests__/seleccion-cuotas.test.ts`

**alternarCuota (las pensiones se pagan en orden)**

- marcar una pensión marca también las anteriores pendientes
- desmarcar una pensión desmarca las que dependen de ella
- una cuota que no se puede pagar no se marca
- une los nombres como en Figma

#### Tablas pagos (5)

`frontend/src/features/pagos/components/__tests__/tablas-pagos.test.tsx`

- el cronograma muestra el recargo de la cuota vencida y términos claros
- el historial distingue el monto de cuotas, el recargo y el total pagado
- sin onDescargar (P9) no ofrece descargar la constancia
- con onDescargar cada pago tiene su botón de descarga
- mientras se descarga, el botón de ese pago queda deshabilitado

## Horarios y reservas de ambientes

307 pruebas de backend y 164 de frontend.

### Backend

#### Calendario del año sql · integración (4)

`backend/src/test/java/com/galiledu/horarios/CalendarioDelAnioSqlIT.java`

- Trae el año activo con sus cuatro bimestres en orden
- Trae el año planificado con sus tres trimestres
- Los días limite del año y de un período son inclusivos y el hueco no cuenta
- Un año inexistente no devuelve nada

#### Clases del docente · integración (8)

`backend/src/test/java/com/galiledu/horarios/ClasesDelDocenteIT.java`

- Trae sus clases de varias secciones con la estructura y el nivel de cada una
- Ordena por día de la semana y no alfabeticamente
- No trae las clases de otro docente ni las de un curso cuyo titular cambio
- Un tutor que no dicta no tiene clases aunque sea tutor de una sección con horario
- Solo trae el año pedido
- Años devuelve solo los años con clases del docente
- Años de un docente sin clases es vacio
- Una sección sin horario guardado no aporta clases

#### Conversor de bloques · integración (2)

`backend/src/test/java/com/galiledu/horarios/ConversorDeBloquesIT.java`

- Calcula las horas reales y omite la celda sin docente
- Sin celdas devuelve vacio

#### Cruces concurrencia · integración (2)

`backend/src/test/java/com/galiledu/horarios/CrucesConcurrenciaIT.java`

- Dos secciones con el mismo docente al mismo bloque una gana y la otra recibe cruce de docente
- Dos secciones con el mismo ambiente al mismo bloque una gana y la otra recibe cruce de ambiente

#### Cruces horario · integración (13)

`backend/src/test/java/com/galiledu/horarios/CrucesHorarioIT.java`

- Sin cruces responde200 y una lista vacia
- El mismo docente en dos secciones al mismo día y bloque es un cruce de docente
- El mismo ambiente con docentes distintos es un cruce de ambiente
- Bloques de jornadas distintas que se superponen se cruzan
- Bloques de jornadas distintas que solo se tocan no se cruzan
- Un bloque de otro año escolar no cruza
- El mismo par de bloques en el mismo año si cruza
- Las celdas que el conversor omite no hacen fallar y se devuelven los cruces del resto
- Una sección inexistente responde404
- Una celda sin día responde400
- Más de72 celdas responde400
- Sin el rol de coordinador responde403
- Sin estructura principal ni horario guardado responde409

#### Estructura con horarios · integración (14)

`backend/src/test/java/com/galiledu/horarios/EstructuraConHorariosIT.java`

- Eliminar una estructura con horarios responde409
- Eliminar una estructura con cabecera sin bloques responde409 con el mismo mensaje
- Eliminar una estructura sin horarios sigue funcionando
- Eliminar la principal cuando ya no tiene horarios sigue sujeta a sus reglas de siempre
- Quitar un día que usan los horarios responde409
- Quitar días que nada usa funciona
- Reducir los bloques por debajo del mayor usado responde409 con el día y el bloque
- Reducir justo hasta el mayor usado funciona
- Agregar días o bloques nunca se rechaza
- Marcar otra como principal mientras la actual tiene horarios responde409
- Crear una nueva principal mientras la actual tiene horarios también responde409
- Marcar otra como principal sin horarios en la actual funciona
- Editar la principal sin cambiar su marca con horarios funciona
- Los horarios de otro año ni de otra estructura afectan

#### Horario apoderado · integración (6)

`backend/src/test/java/com/galiledu/horarios/HorarioApoderadoIT.java`

- Lista solo los hijos matriculados
- Muestra el horario de la sección del hijo con su tutor
- Sin año usa el más reciente y sin horario guardado no está disponible
- No muestra el horario de quien no es su hijo
- No muestra un año en que no está matriculado
- Solo el apoderado puede consultarlo

#### Horario con validador real · integración (3)

`backend/src/test/java/com/galiledu/horarios/HorarioConValidadorRealIT.java`

- Guardar un bloque que choca con otra sección responde409 y no guarda nada
- Guardar sin choques sigue funcionando
- El get trae el cruce en la celda que choca

#### Horario concurrencia · integración (2)

`backend/src/test/java/com/galiledu/horarios/HorarioConcurrenciaIT.java`

- Dos put con la misma versión uno gana y el otro recibe409
- Dos primeros guardados a la vez uno crea la cabecera y el otro recibe409 sin error interno

#### Horario cruces · integración (2)

`backend/src/test/java/com/galiledu/horarios/HorarioCrucesIT.java`

- Un cruce hace que el put responda409 y no escriba nada
- El get repart en cada celda su cruce

#### Horario docente · integración (13)

`backend/src/test/java/com/galiledu/horarios/HorarioDocenteIT.java`

- Docente con clases en dos secciones de secundaria recibe un horario secundaria
- Docente con primaria y secundaria de distinta hora recibe dos horarios con primaria primero
- No ve las clases de otro docente
- Sin año id devuelve el año activo si tiene clases y con año id devuelve ese año
- Con año id sin clases devuelve los datos del año y horarios vacio
- Docente sin clases en ningún año recibe año null y horarios vacio
- Años lista solo los años con clases del más reciente al más antiguo
- Un usuario solo tutor que es titular puede consultar y ve sus clases
- Un tutor que no dicta ve el estado vacio
- Coordinador y apoderado reciben acceso403 en los dos endpoints
- Sin sesión responde401
- Con un año que no existe responde404
- Con un año id que no es uuid responde400

#### Horario escritura · integración (25)

`backend/src/test/java/com/galiledu/horarios/HorarioEscrituraIT.java`

- Año cerrado responde409
- Versión desactualizada responde409
- Versión nula con cabecera existente responde409
- Versión con valor sin cabecera responde409
- Celda repetida responde400
- Día no lectivo responde400
- Bloque fuera de la jornada responde400
- Bloque cero responde400
- Curso de otro grado o año responde400
- Ambiente inactivo o inexistente responde400
- Curso sin docente responde409
- Horas semanales excedidas responde409
- Sin estructura principal responde409
- Otro rol recibe403
- Sección inexistente responde404
- Un cuerpo sin celdas o con un día inválido responde400
- Primer guardado crea la cabecera con la estructura principal y versión cero
- Cada guardado con cambios sube la versión exactamente uno
- Un guardado identico no sube la versión
- Guardar renueva actualizado en
- Los bloques sin cambio conservan su id
- Celdas vacias sin cabecera no crean nada
- Una cabecera existente conserva su estructura aunque ya no sea la principal
- Vaciar el horario elimina todos los bloques y sube la versión
- Bloques de curso cuenta los bloques del curso en la sección

#### Horario lectura · integración (10)

`backend/src/test/java/com/galiledu/horarios/HorarioLecturaIT.java`

- Con cabecera devuelve la matriz con docente ambiente y curso
- Sin cabecera con principal devuelve la jornada sin guardar nada
- Sin cabecera y sin principal responde409 con el nivel de la sección
- Con cabecera usa la estructura guardada aunque ya no sea la principal
- Con el año cerrado responde200 con editable falso
- Sección inexistente responde404
- Una celda sin docente llega con docente nulo
- Otro rol recibe403 en los dos endpoints
- Opciones trae los cursos con sus horas asignadas y docente
- Opciones solo ofrece ambientes activos y sin aula no hay por defecto

#### Horario repositorio · integración (3)

`backend/src/test/java/com/galiledu/horarios/HorarioRepositorioIT.java`

- Guarda y lee un horario con un bloque
- Las entidades son las de la sección y no otras
- Los stubs del contrato se inyectan y no hacen nada

#### Matriculados de sección sql · integración (4)

`backend/src/test/java/com/galiledu/horarios/MatriculadosDeSeccionSqlIT.java`

- Cuenta las matrículas de la sección
- Una sección sin matrículas tiene cero
- Ocupan vacante todos los estados menos retirado y anulada
- La condición de vacante es la misma que la de configuración

#### Ocupación horarios sql · integración (2)

`backend/src/test/java/com/galiledu/horarios/OcupacionHorariosSqlIT.java`

- Devuelve los bloques de las otras secciones con las horas de su estructura
- Excluye la sección indicada y no mira otros años

#### Reserva ambiente esquema · integración (14)

`backend/src/test/java/com/galiledu/horarios/ReservaAmbienteEsquemaIT.java`

- Una reserva válida se guarda activa y con versión cero
- La hora de fin no puede ser igual a la de inicio
- La hora de fin no puede ser anterior a la de inicio
- El estado no puede ser finalizada
- Una reserva cancelada se guarda
- La actividad no admite más de120 caracteres
- La actividad admite exactamente120 caracteres
- El ambiente debe existir
- La sección debe existir
- El responsable debe existir
- Quien registra la reserva debe existir
- Un ambiente con reservas no se elimina
- Un ambiente sin reservas si se elimina
- Al eliminar la sección se borran sus reservas

#### Reserva ambiente · integración (51)

`backend/src/test/java/com/galiledu/horarios/ReservaAmbienteIT.java`

- Crea la reserva con sus datos y el usuario que la registra
- Una reserva contigua a otra del mismo ambiente se permite
- Un aula regular se rechaza con un400
- Un espacio especial inactivo se rechaza con un400
- Un ambiente inexistente se rechaza con un400
- Más alumnos que el aforo se rechaza con un400
- Tantos alumnos como el aforo se acepta
- Con el año cerrado se responde409
- Un responsable que no es docente ni tutor se rechaza con un400
- Una fecha y hora pasada se rechaza con un400
- Una fecha fuera del año se rechaza con un400
- Una fecha entre dos períodos se rechaza con un400
- Un día que no es lectivo se rechaza con un400
- Una reserva fuera de la jornada se rechaza con un400
- La hora de fin debe ser posterior a la de inicio con un400
- Otra reserva del mismo ambiente superpuesta se rechaza con un409
- Una reserva cancelada no cuenta para los cruces
- Otra sección con el ambiente en su horario se rechaza con un409
- La propia sección con un aula regular en su horario se permite
- La propia sección con el mismo ambiente en su horario se rechaza con un400
- La propia sección con otro espacio especial en su horario se rechaza con un409
- Un responsable con clase en otra sección se rechaza con un409
- Un responsable con clase en la propia sección se permite
- Un responsable de otra reserva superpuesta se rechaza con un409
- Una sección con otra reserva superpuesta se rechaza con un409
- Edita la reserva y sube la versión
- Editar con una versión vieja se rechaza con un409
- Editar contra otra reserva superpuesta se rechaza con un409
- Editar una reserva finalizada se rechaza con un409
- Editar una reserva cancelada se rechaza con un409
- Editar una reserva inexistente responde404
- Cancelar responde204 y la reserva desaparece del listado y de la lectura
- Una reserva cancelada libera el ambiente
- Cancelar otra vez o una finalizada se rechaza con un409
- Cancelar con una versión vieja se rechaza con un409
- Cancelar una reserva inexistente responde404
- Sin filtro lista la semana de hoy de la más reciente a la más antigua
- El filtro semana es lo mismo que no poner filtro
- El filtro mes lista el mes calendario
- El filtro año lista el año calendario sin las canceladas ni las de otro año
- La paginación divide el listado y da los totales
- Un filtro desconocido se rechaza con un400
- Obtener devuelve la reserva y una inexistente responde404
- Las opciones solo traen espacios especiales activos por código y responsables docentes o tutores
- Un cuerpo vacio se rechaza con un400 con un mensaje por campo
- La actividad en blanco o muy larga se rechaza con un400
- Una hora o una fecha mal escrita se rechaza con un400
- Editar sin la versión se rechaza con un400
- Una sección inexistente responde404
- Sin token cada endpoint responde401
- Solo el coordinador entra cada otro rol recibe403 en cada endpoint

#### Reserva ambiente repositorio · integración (10)

`backend/src/test/java/com/galiledu/horarios/ReservaAmbienteRepositorioIT.java`

- Guarda la reserva activa con sus datos y su versión cero
- Cada cambio sube la versión y cancelar conserva la fila
- Superpuestas por ambiente solo trae las activas de la fecha que se cruzan
- Dos franjas contiguas no se superponen
- Una franja contenida o que contiene a la otra si se superpone
- Superpuestas por sección y por responsable filtran por su clave
- Las canceladas no cuentan para la sección ni para el responsable
- Al editar se excluye la propia reserva en las tres consultas
- La entidad rechaza datos incompletos o horas invertidas
- Está finalizada cuando llega la hora de fin y una cancelada no lo está

#### Reserva concurrencia · integración (2)

`backend/src/test/java/com/galiledu/horarios/ReservaConcurrenciaIT.java`

- Dos secciones piden el mismo laboratorio a la misma hora una gana y la otra recibe cruce de ambiente
- El mismo responsable en dos ambientes a la misma hora una gana y la otra recibe cruce de responsable

#### Reservas de prueba · integración (7)

`backend/src/test/java/com/galiledu/horarios/ReservasDePruebaIT.java`

- Hay tres futuras dos pasadas y una cancelada
- Una futura cae está semana y otra este mes
- Regla1 el ambiente es un espacio especial activo
- Regla2 las secciones son del2026 y caben en el aforo
- Regla3 la fecha es lectiva dentro de un período y la jornada de la sección
- Reglas4_7by8 sin superposiciones entre las activas por ambiente responsable y sección
- El responsable es María y el coordinador quien las registro

#### Responsables elegibles sql · integración (6)

`backend/src/test/java/com/galiledu/horarios/ResponsablesElegiblesSqlIT.java`

- Un docente con dos roles aparece una sola vez con su nombre completo
- Quien solo es tutor también es elegible
- Un docente inactivo o eliminado no es elegible
- Quien no es docente ni tutor no es elegible
- Un usuario inexistente no es elegible
- Los elegibles salen ordenados por nombre

#### Selectores horarios · integración (5)

`backend/src/test/java/com/galiledu/horarios/SelectoresHorariosIT.java`

- El coordinador ve los años con su estado
- El coordinador ve los grados con sus secciones del año
- Un año sin secciones devuelve los grados vacios
- Un año que no existe responde404
- Otros roles recibe403 en las dos rutas

#### Uso de estructura sql · integración (3)

`backend/src/test/java/com/galiledu/horarios/UsoDeEstructuraSqlIT.java`

- Devuelve el mayor bloque de cada día sumando todas las secciones que usan la estructura
- Ignora los horarios de otras estructuras
- Una cabecera sin bloques cuenta como horario pero no como uso de días

#### Mensajes cruce · unitaria (3)

`backend/src/test/java/com/galiledu/horarios/aplicacion/MensajesCruceTests.java`

- Cada tipo tiene su título
- Con título antepone el título del tipo al mensaje de un cruce de docente
- Con título antepone el título del tipo al mensaje de un cruce de ambiente

#### Reglas de reserva · unitaria (39)

`backend/src/test/java/com/galiledu/horarios/aplicacion/ReglasDeReservaTests.java`

- Un espacio especial activo se puede reservar
- Un ambiente inexistente o inactivo se rechaza con un400
- Un aula regular se rechaza con un400
- Con el año cerrado no se reserva y se responde409
- Un año planificado o activo se acepta
- Más alumnos que el aforo se rechaza con un400
- Tantos alumnos como el aforo se acepta
- Una reserva dentro del año el período el día lectivo y la jornada se acepta
- La hora de fin debe ser posterior a la de inicio
- Una fecha y hora pasada se rechaza con un400
- Iniciar justo ahora no es pasado
- Una fecha fuera del año se rechaza con un400
- Una fecha entre dos períodos se rechaza con un400
- Los días limite de un período se aceptan
- Un día que no es lectivo se rechaza con un400
- Una reserva fuera de la jornada se rechaza con un400
- Una reserva pendiente se puede editar
- Una reserva finalizada no se edita y se responde409
- Una reserva cancelada no se edita y se responde409
- Sin reservas ni bloques no hay cruces
- Otra reserva del mismo ambiente superpuesta es un cruce de ambiente
- Dos reservas contiguas no se cruzan
- Otro ambiente otra fecha o ninguna reserva del mismo ambiente no se cruzan
- Otra sección con el ambiente en su horario es un cruce de ambiente
- Otra sección con el ambiente justo antes o después o en otro día no se cruza
- La clase de la propia sección en un aula regular se permite
- El mismo ambiente en el horario de la propia sección se rechaza con un400 sin título
- Otro espacio especial en el horario de la propia sección se rechaza con un409
- Un ambiente desconocido en el horario de la propia sección no se trata como espacio especial
- La clase de la propia sección fuera de la franja no importa
- Un responsable con clase en otra sección es un cruce con el título responsable no disponible
- Las clases del responsable con la propia sección se ignoran
- La clase de otro docente o contigua o en otro día no cruza al responsable
- Un responsable de otra reserva superpuesta es un cruce
- Una sección con otra reserva superpuesta es un cruce
- Al editar la reserva no se cruza con si misma
- Al editar si se cruza con las demas reservas
- Los cruces salen en el orden de las reglas
- Cada tipo de cruce tiene su código y su título

#### Servicio cruces horario · unitaria (3)

`backend/src/test/java/com/galiledu/horarios/aplicacion/ServicioCrucesHorarioTests.java`

- Sin sección responde404 sin consultar nada
- Busca los cruces con el año y la sección correctos y devuelve lo del validador
- Si el conversor omite todo devuelve lo que dice el validador sin fallar

#### Servicio horario docente · unitaria (10)

`backend/src/test/java/com/galiledu/horarios/aplicacion/ServicioHorarioDocenteTests.java`

- Agrupa las clases por estructura con la jornada de cada una
- Primaria va antes que secundaria aunque la primera clase sea de secundaria
- Con dos estructuras del mismo nivel el título lleva el código
- Sin año usa el activo si tiene clases aunque haya uno más reciente
- Sin año y sin activo con clases usa el más reciente
- Sin clases en ningún año responder año null y horarios vacio
- Un año inexistente responde404
- Un año sin clases devuelve horarios vacio con los datos del año
- Dos clases en la misma celda se conservan
- Años del docente delega en el puerto

#### Servicio reservas ambiente · unitaria (28)

`backend/src/test/java/com/galiledu/horarios/aplicacion/ServicioReservasAmbienteTests.java`

- Crea con el creador y devuelve la reserva leida
- Toma el bloqueo del año antes de cargar reservas y bloques y guarda después
- Las reglas de forma fallan antes de tomar el bloqueo y no guardan nada
- Un responsable no elegible se rechaza con un400 antes del bloqueo
- Una sección inexistente responde404
- Un año cerrado responde409 sin bloquear
- Un cruce responde409 con título y no guarda
- El cruce de la misma sección con el mismo ambiente se rechaza con un400
- Ahora se toma del reloj inyectado para la regla3
- Reprogramar con una versión vieja409 y no bloquea el año
- Reprogramar excluye la propia reserva en las tres busquedas
- Reprogramar una reserva finalizada o cancelada responde409 sin bloquear
- Un conflicto de versión al guardar se convierte en409
- Reprogramar una reserva inexistente responde404
- Cancelar deja la reserva cancelada sin tomar el bloqueo
- Cancelar con una versión vieja409
- Cancelar con la versión actual se permite
- Cancelar una finalizada o ya cancelada responde409
- Cancelar una reserva inexistente responde404
- Sin filtro lista la semana de hoy de lunes a domingo
- El filtro mes cubre el mes calendario de hoy
- El filtro año cubre el año calendario de hoy
- La semana en domingo y en lunes queda completa
- La página se convierte en desplazamiento y el tamano se acota
- El estado y el editable se calculan con el reloj
- Una reserva que termina justo ahora ya está finalizada
- Obtener una reserva cancelada o inexistente responde404
- Las opciones solo traen espacios especiales por código y los responsables

#### Validador cruces horario · unitaria (13)

`backend/src/test/java/com/galiledu/horarios/aplicacion/ValidadorCrucesHorarioTests.java`

- Sin ocupados no hay cruces
- El cruce de docente dice con quien choca y cuando
- El cruce de ambiente dice quien lo ocupa y cuando
- Un mismo par puede cruzarse en docente y en ambiente
- Los bloques contiguos no se cruzan
- El mismo recurso en otro día no se cruza
- Jornadas distintas que se superponen se cruzan
- Sin docente en algún lado no hay cruce de docente
- Sin ambiente en algún lado no hay cruce de ambiente
- Un solicitado que choca con varios ocupados da un cruce por cada uno
- Los cruces salen por día luego por bloque y con el docente antes que el ambiente
- Buscar cruces pide los ocupados del año excluyendo la sección
- Buscar cruces sin solicitados no consulta nada

### Frontend

#### Reservas (api) (31)

`frontend/src/features/horarios/api/__tests__/reservas.test.tsx`

**sesión y rol**

- sin sesión responde 401
- cada rol que no es Coordinador recibe 403 en los seis endpoints
**listado**

- sin filtro lista la semana de hoy (lunes a domingo), de la más reciente a la más antigua
- el filtro MES suma las dos finalizadas, que salen como FINALIZADO y no editables
- el filtro ANIO trae las cinco activas y nunca la cancelada
- la paginación divide el listado y calcula los totales
- un filtro desconocido responde 400
- «hoy» se puede mover: el 25 de nov. la semana ya no trae las del 19 y 20
- una reserva cuya hora de fin ya llegó pasa a FINALIZADO
**opciones y detalle**

- las opciones traen los espacios especiales por código y a María
- «opciones» no se confunde con un id
- una reserva se lee por id; una inexistente o cancelada responde 404
**crear**

- 201 con la reserva, que aparece en el listado y se guarda
- 400 con un error por campo cuando falta algo o la actividad es muy larga
- 400 con una fecha u hora pasada, un ambiente que no es espacio especial o horas invertidas
- 404 con una sección que no existe
- 409 con «Título: texto» si el ambiente ya está reservado a esa hora
- 409 de responsable cuando María ya tiene otra reserva a esa hora
- una reserva contigua a otra no se cruza
**editar**

- 200 con los datos nuevos y la versión subida; no choca consigo misma
- 409 con una versión vieja
- 409 si la reserva ya finalizó
- 404 si no existe y 400 si falta la versión
- 409 de ambiente al mover la reserva a una franja ocupada
**eliminar**

- 204: la reserva queda cancelada y desaparece del listado y de la lectura
- 409 si ya está cancelada, si ya finalizó o si la versión es vieja
- con la versión actual se cancela; una inexistente responde 404
**reinicio entre pruebas**

- crear y cancelar cambian el listado en memoria…
- …y la siguiente prueba vuelve a las seis sembradas
**hooks**

- useReservas pide el filtro y la página, y crear invalida la lista
- un 409 al crear no avisa por notificación: lo recibe quien llama

#### Alerta cruces (5)

`frontend/src/features/horarios/components/__tests__/alerta-cruces.test.tsx`

- cruce de docente: alerta «Docente no disponible» con su mensaje, la ayuda y el campo Docente marcado
- cruce de ambiente: alerta «Ambiente no disponible» con su mensaje, el campo Ambiente marcado y sin la ayuda del docente
- con cruces de los dos tipos hay dos alertas, primero la del docente y luego la del ambiente
- sin cruces no hay alertas de cruce ni campos marcados
- con un cruce «Asignar» sigue deshabilitado

#### Asignación horarios (34)

`frontend/src/features/horarios/components/__tests__/asignacion-horarios.test.tsx`

- los selectores van en cascada: nada viene elegido y cada uno se habilita con el anterior
- el selector de grado solo ofrece los grados con secciones
- si el año no tiene grados con secciones, el selector de grado queda sin opciones y deshabilitado
- las tarjetas llevan su título en Body/Body-1/Medium (18 px, medium)
- ningún campo de la pantalla usa placeholder: los vacíos muestran solo su etiqueta
- modo lectura: celdas asignadas con curso, docente y aula; las vacías son grises, sin «+» y no se abren
- los días salen de la jornada: con sábado aparece la columna del sábado
- «Editar horario» no aparece si el año está cerrado (editable = false)
- un horario con cruces marca la celda y avisa en la página con el mensaje del cruce
- sin horario ni estructura principal muestra el mensaje del backend como estado vacío
- una sección que no existe muestra el mensaje del backend
- modo edición: la barra pasa a «Cancelar» y «Guardar cambios» y las celdas vacías muestran «+»
- «Cancelar» descarta el borrador y vuelve a modo lectura
- modal: el docente es un campo deshabilitado que se llena al elegir el curso y el ambiente va con el aula de la sección
- modal: un curso completo o sin docente está deshabilitado y el sin docente lleva un aviso con enlace a Plan de evaluación
- modal: una sección sin aula no trae ambiente por defecto y hay que elegir uno antes de asignar
- modal: «Asignar» pone el bloque en el borrador, suma sus horas y avisa que hay cambios
- modal: «Actualizar» y «Quitar» aparecen al abrir un bloque asignado, y las horas no cuentan el bloque editado
- modal: «Cancelar» cierra sin cambiar el borrador
- modal: un cruce que devuelve el backend se muestra y deshabilita «Asignar»
- modal: si la consulta de cruces falla (404 o red) no hay alerta ni notificación y se puede asignar
- guardar: un solo PUT con la versión del GET y todas las celdas del borrador; al terminar vuelve a lectura
- guardar con el primer horario envía version null
- un 409 de versión deja la edición abierta con el mensaje y una forma de recargar
- un 400 del PUT se muestra tal cual y no cambia de modo
- cambiar de sección con cambios sin guardar pide confirmación; sin cambios no
- cambiar de año o de grado con cambios sin guardar también pide confirmación
- no aparece nada de subgrupos en la pantalla
- modal: el campo Docente vacío muestra solo su etiqueta, sin texto de placeholder
- modal: con muchos cursos sin docente el aviso es compacto y la lista completa va en el título
- modal: el aviso de cruce también es la variante pequeña
- modal: la lista del Ambiente flota fuera del diálogo (en el body) y elegir una opción no cierra el modal
- modal: el aviso de cursos sin docente va arriba de los cursos
- modal: si la sección no tiene cursos en el plan muestra el mensaje y el enlace a Plan de evaluación

#### Horario apoderado (components) (4)

`frontend/src/features/horarios/components/__tests__/horario-apoderado.test.tsx`

- muestra el horario del primer hijo con su grado, sección y tutora
- al elegir otro hijo muestra su sección; sin horario armado avisa y no deja descargar
- sin hijos matriculados lo dice
- "Descargar Horario" abre el horario para imprimirlo o guardarlo en PDF

#### Horario docente (components) (7)

`frontend/src/features/horarios/components/__tests__/horario-docente.test.tsx`

- docente solo con secundaria: una matriz sin título, con curso, aula y sección, y sin «Prof.»
- primaria y secundaria: dos tarjetas con su título, primaria primero, cada una con su jornada
- al cambiar el año pide ese año y dibuja su horario
- un año sin clases lo dice y deja el selector para cambiar de año
- un docente sin años con clases lo dice y no muestra el selector
- mientras carga muestra el indicador y no el vacío
- el tutor ve la misma pantalla que el docente (las dos rutas usan este componente)

#### Matriz horaria sección (4)

`frontend/src/features/horarios/components/__tests__/matriz-horaria-seccion.test.tsx`

- con sección, la celda la muestra junto al aula
- con varios cursos, las secciones se unen con « · »
- sin sección, la celda queda igual que antes
- una sesión sin sección en el horario de siempre no dibuja ninguna

#### Matriz horaria (2)

`frontend/src/features/horarios/components/__tests__/matriz-horaria.test.tsx`

- formatHora usa el formato de 12 horas de Figma
- pinta los días, bloques, recreos y subgrupos

#### Reservas espacios (34)

`frontend/src/features/horarios/components/__tests__/reservas-espacios.test.tsx`

- la pantalla lleva el título, las migas, «Nueva reserva» y la tarjeta «Historial de reservas»
- lista la semana de hoy: espacio con código y nombre, actividad con sección y responsable, estado, fecha y hora de 12 horas
- las columnas son Espacio, Actividad, Estado, Fecha, Hora y Acciones
- el filtro «Esta semana» viene elegido y es el único marcado
- cambiar de filtro pide ese rango: este mes suma las finalizadas y este año trae las cinco activas
- la paginación pide la página elegida y al cambiar de filtro se vuelve a la primera
- sin reservas en el rango muestra el estado vacío y no la paginación
- una reserva finalizada trae «Finalizado» con «Editar» y «Eliminar reserva» deshabilitados; las pendientes, habilitados
- «Nueva reserva» abre el modal con sus campos, sin campo de año; hora y duración esperan a la sección
- el modal ofrece solo espacios especiales por nombre, a María y los grados con secciones activas
- las secciones inactivas no se ofrecen
- la hora ofrece los inicios de bloque de la jornada y la duración 1×, 2× y 3× el bloque, sin las que pasan del fin
- cambiar la hora a una desde la que la duración elegida no cabe, la limpia
- cambiar el grado vacía la sección, la hora y la duración
- crear con éxito: cierra el modal, avisa «Reserva registrada.», guarda la hora de fin calculada y se ve en el listado
- «Cancelar» cierra el modal sin guardar
- un formulario vacío muestra un mensaje en español bajo cada campo y no llama al backend
- una actividad de más de 120 caracteres se rechaza en el formulario
- un día que no es lectivo se rechaza en el cliente con el nombre del día
- el calendario no ofrece fechas anteriores a hoy
- un 409 por cruce se muestra en el modal con el título aparte y el modal sigue abierto
- el 409 de responsable lleva su título
- un 409 sin título (año cerrado) va como texto del aviso, sin título
- un 400 de regla (por ejemplo, la sección no tiene ese día) va en el aviso del modal
- un 400 de @Valid con errores por campo pone cada mensaje bajo su campo (la hora de fin va bajo «Duración»)
- los errores de crear no salen como notificación global: se ven solo en el modal
- «Editar» abre el mismo modal como «Editar reserva», con los datos de la reserva y «Guardar cambios»
- al editar, la hora guardada (8:15, que no es inicio de bloque) se conserva como opción
- editar con éxito manda la versión, avisa «Reserva actualizada.» y el listado muestra el cambio
- un 409 de versión avisa y «Recargar» vuelve a leer la reserva y permite guardar con la versión nueva
- «Eliminar reserva» pide confirmación con el espacio, la fecha y la hora; «Cancelar» no llama al backend
- confirmar elimina la reserva: avisa «Reserva eliminada.» y desaparece del listado
- eliminar la última fila de una página que no es la primera vuelve a la anterior
- un error al eliminar (409) sale como notificación global y la reserva sigue en el listado

#### Borrador (6)

`frontend/src/features/horarios/lib/__tests__/borrador.test.ts`

- conCelda reemplaza la del mismo día y bloque y sinCelda la quita
- hayCambios detecta una celda nueva, quitada o con otro curso o ambiente
- el cuerpo del PUT lleva la versión y todas las celdas ordenadas, sin nombres ni docente
- las horas asignadas parten de las del backend, suman lo que el borrador cambió y no cuentan el bloque editado
- formatRango escribe el sufijo una vez cuando es el mismo
- un 409 de versión se reconoce por su estado y su mensaje

#### Horario apoderado (lib) (4)

`frontend/src/features/horarios/lib/__tests__/horario-apoderado.test.ts`

- pone cada recreo justo después de su bloque
- el docente va con "Prof." como en Figma
- convierte las clases a sesiones de la matriz (LUN → LUNES)
- la página para descargar trae los datos del horario y escapa el texto

#### Horario docente (lib) (7)

`frontend/src/features/horarios/lib/__tests__/horario-docente.test.ts`

- pone cada recreo justo después de su bloque
- cada estructura usa su propia jornada
- convierte los días del backend a los de la matriz (LUN → LUNES)
- la sección y el aula van en el curso, y el docente va vacío
- dos clases en la misma celda quedan juntas en una sola sesión
- las clases del sábado se descartan porque la matriz solo dibuja de lunes a viernes
- sin clases deja las filas y ninguna sesión

#### Reservas (lib) (26)

`frontend/src/features/horarios/lib/__tests__/reservas.test.ts`

**formato de horas**

- formatHora12: dos dígitos, espacio y am/pm
- minutos y horas se convierten de ida y vuelta
- calcularHoraFin suma la duración a la hora de inicio
- duracionEnMinutos es la resta de dos horas
**opciones de la jornada**

- finDeJornada y duracionDelBloque salen de los bloques
- las horas son los inicios de bloque, en formato de 12 horas
- al editar, una hora guardada que no es inicio de bloque se agrega en su lugar
- una hora guardada que ya es inicio de bloque no se repite
- las duraciones son 1×, 2× y 3× la del bloque
- las duraciones que pasan del fin de la jornada salen deshabilitadas
- terminar justo al final de la jornada se permite
- al editar, una duración guardada que no es múltiplo se agrega y queda habilitada
- una jornada sin bloques no ofrece duraciones
**día lectivo**

- diaDeLaSemana da el código de LUN a SAB y null el domingo
- esDiaLectivo compara con los días de la jornada
**mensaje de día no lectivo**

- nombra el día en minúscula, como el backend
**año, grado y sección**

- se reserva en el año ACTIVO
- sin año ACTIVO, el más reciente que no esté CERRADO
- si todos están CERRADOS o no hay años, no hay año para reservar
- seccionesActivas descarta las INACTIVAS
- gradosConSeccionesActivas deja solo los grados con al menos una activa
**errores del backend**

- partirMensaje separa el título de los tres cruces
- un mensaje sin esos títulos se deja entero y sin título
- el título solo cuenta al principio y seguido de «: »
- el 409 de versión se reconoce por el estado y el texto
- erroresDeCampos devuelve los errores por campo del 400, o undefined

## Configuración (año escolar, periodos, secciones, tarifario)

649 pruebas de backend y 504 de frontend.

### Backend

#### Año escolar (configuración) · integración (41)

`backend/src/test/java/com/galiledu/configuracion/AnioEscolarIT.java`

- Crea un año bimestral planificado con cuatro períodos
- Crea un año trimestral con tres períodos
- El año creado aparece en el listado y en el detalle
- Lista los años de los datos de prueba del más reciente al más antiguo
- El detalle trae los períodos ordenados por número
- El detalle trae los parametros comunes y bloquea los del año que ya empezo
- Configura los parametros antes de que empiece el año
- No modifica los parametros cuando el año ya empezo
- Rechaza una regla que no corresponde a la escala del año
- El estado del período se calcula con las fechas y el reloj de hoy
- Detalle de un año que no existe da404
- Rechaza un año duplicado
- Rechaza fecha de fin no posterior a la de inicio
- Rechaza un año muy corto para el regimen
- Válida el rango del año y los campos obligatorios
- Solo el personal administrativo puede usar la configuración
- El año actual trae el período en curso los conteos y el avance
- Entre dos períodos la card muestra el plazo de notas y el próximo período
- Sin año activo no hay año actual
- Edita las fechas de un año planificado y regenera sus períodos
- Editar el año válida las fechas
- Solo se edita un año planificado
- Editar un año que no existe da404
- Cambia el regimen de un año planificado y regenera los períodos con fechas sugeridas
- Solo se cambia el regimen de un año planificado
- El regimen es obligatorio y debe ser válido
- Cambiar el regimen de un año que no existe da404
- Los requisitos del año planificado muestran los obligatorios y los recomendados
- Un año con sus requisitos obligatorios puede activarse
- Los requisitos de un año que no existe dan404
- El listado y el detalle traen las cantidades de secciones y estructuras
- Activa un año planificado con sus requisitos y sin otro activo
- No activa un año mientras otro sigue activo
- No activa un año sin sus requisitos obligatorios
- Solo activa un año planificado
- Activar un año que no existe da404
- Cierra el año cuando ya vencio el plazo de notas del último período
- No cierra el año antes de que venza el plazo de notas
- Solo cierra un año activo
- Cerrar un año que no existe da404
- Activar y cerrar solo lo hace el personal administrativo

#### Bloqueo año para horarios · integración (2)

`backend/src/test/java/com/galiledu/configuracion/BloqueoAnioParaHorariosIT.java`

- Dentro de una transacción bloquea el año
- Fuera de una transacción falla

#### Consultas configuración · integración (10)

`backend/src/test/java/com/galiledu/configuracion/ConsultasConfiguracionIT.java`

- El listado trae las secciones activas y las estructuras de cada año
- Los conteos del año activo salen de los datos de prueba
- Los conteos del año planificado no tienen estructuras ni plan
- Un año que no existe tiene todos los conteos en cero
- Las secciones inactivas no cuentan ni como sección ni como grado
- Asignar un tutor descuenta las secciones sin tutor
- Los codigos de las estructuras van en orden
- Los alumnos matriculados salen de la matrícula del año
- Los alumnos retirados o anulados no cuentan como matriculados
- El repositorio encuentra año por estado

#### Directorio docentes sql · integración (3)

`backend/src/test/java/com/galiledu/configuracion/DirectorioDocentesSqlIT.java`

- Los elegibles son los usuarios activos con rol docente o tutor sin repetir a los que tienen ambos
- Devuelve el nombre completo de cada usuario pedido sea cual sea su estado
- Ignora los ids que no existen y una lista vacia da un mapa vacio

#### Esquema configuración · integración (11)

`backend/src/test/java/com/galiledu/configuracion/EsquemaConfiguracionIT.java`

- Los datos de prueba traen el año activo con sus períodos y secciones
- El período no guarda su estado porque se calcula con las fechas
- Solo un año puede estar activo
- Acepta una configuración cualitativa compatible para todo el año
- La escala del año solo acepta valores conocidos
- La regla de nota debe corresponder a la escala del año
- La consolidación del año solo acepta valores conocidos
- No se repite el nombre de una sección en el mismo grado
- La capacidad de una sección va de1 a45
- Dos cuotas del tarifario no pueden vencer el mismo día
- Los días lectivos solo aceptan lunes a sábado

#### Estructura horaria concurrencia · integración (7)

`backend/src/test/java/com/galiledu/configuracion/EstructuraHorariaConcurrenciaIT.java`

- Varias creaciones simultaneas del mismo nivel generan codigos distintos
- Varias peticiones que marcan como principal estructuras distintas dejan una sola principal
- Crear dos estructuras principales a la vez deja una sola principal
- Eliminar y editar a la vez la misma estructura no deja nada a cedias
- Una creación principal rechazada no deja desmarcada a la anterior
- Un nul en la descripción da400 y no culpa a otra persona
- Editar con un nul en la descripción no cambia nada en la base

#### Estructura horaria entradas hostiles · integración (59)

`backend/src/test/java/com/galiledu/configuracion/EstructuraHorariaEntradasHostilesIT.java`

- La descripción en el limite se acepta y una más se rechaza
- Una descripción vacia o nula o solo de blancos se guarda como sin descripción
- El código que llega al crear se ignora y el backend genera el suyo
- Una clave duplicada de código también se ignora
- Las claves duplicadas se aceptan y gana la última
- Un cuerpo de10 mb se rechaza por el largo de la descripción sin escribir nada
- Editar con otro código lo ignora y conserva el que tenía
- Un nul dentro del texto se rechaza con su mensaje sin escribir nada
- Un nul dentro del texto en la vista previa lista el error
- Los caracteres de control en medio se rechazan sin escribir nada
- Las cadenas de inyección en la descripción se guardan literales y las tablas siguen
- Los días invalidos se rechazan sin escribir nada
- Los días validos se guardan en orden de semana
- Mil días invalidos se rechazan sin fallar ni filtrar nada
- Los bloques por día fuera de rango o mal escritos se rechazan
- Los bloques por día validos o convertibles se guardan
- Un boolean como bloques por día se rechaza sin fallar
- La duración del bloque fuera de rango o mal escrita se rechaza
- La duración válida o convertible se guarda
- Un boolean como duración se rechaza sin fallar
- Los recesos fuera de rango se rechazan
- Los recesos en el borde se aceptan
- Una hora de entrada mal escrita se rechaza sin escribir nada
- La hora de entrada vacia o nula o ausente se rechaza con su mensaje
- La hora con segundos se trunca a minutos y se guarda igual que se muestra
- Las horas limite con la regla de las1800
- La jornada maxima desde las2359 no desborda ni da horas negativas
- Los recesos nulos vacios desordenados o con campos de más se aceptan
- Los recesos mal formados se rechazan sin escribir nada
- Un uuid mal formado en la ruta debe dar4xx limpio en los tres endpoints
- Una ruta que rechaza el firewall da400 con cuerpo vacio
- Un identificador vacio en la ruta da4xx
- El uuid nulo y uno aleatorio dan404 con su mensaje
- Un uuid en mayúsculas es válido y encuentra la estructura
- El id de un año como id de estructura y viceversa dan404
- Una estructura de un año activo o cerrado rechaza editar y eliminar sin cambiar nada
- Un año inexistente da404 en listar crear y vista previa
- Cuando la ruta y el cuerpo son invalidos gana primero lo que se evalua antes
- Un cuerpo ilegible se rechaza sin escribir nada
- La basura al final del json se rechaza sin escribir nada
- Un tipo de contenido que no es json debe dar4xx
- Un método no permitido debe dar4xx sin tocar la base
- Un cuerpo en un get se ignora
- Los campos de más en editar no se aplican
- Editar sin enviar es principal conserva la marca que ya tiene
- Un false explicito sobre la principal sigue dando409 y no cambia nada
- Un true explicito marca como principal y baja la anterior
- Sin token o con token inválido ningún endpoint responde algo que no sea4xx limpio
- Un token con la firma alterada se rechaza
- Los roles sin permiso reciben siempre403 sin importar si la estructura existe
- El rechazo por rol es igual para una estructura que existe y una que no
- Un rol sin permiso no debe ver un error distinto por un uuid mal escrito
- Eliminar dos veces la misma estructura da404 en la segunda
- Editar una estructura eliminada da404
- Marcar como principal la que ya lo es no cambia nada ni avisa
- Cambiar el nivel por un campo extra no tiene efecto
- Crear eliminar y volver a crear el mismo código en el mismo año funciona
- Con planes al limite los bloques iguales a las horas se aceptan y uno menos se rechaza
- Ninguna respuesta de error filtra detalles internos

#### Estructura horaria (configuración) · integración (29)

`backend/src/test/java/com/galiledu/configuracion/EstructuraHorariaIT.java`

- Lista las estructuras del año con sus indicadores
- Un año planificado sin estructuras se marca modificable
- El detalle trae la jornada calculada fila por fila
- Crea una estructura y la primera del nivel nace como principal
- Marcar como principal al crear baja la anterior y avisa
- La jornada puede terminar a las1800 exactas y no un minuto después
- Rechaza con su mensaje cada regla de forma del formulario
- El código lo genera el backend y agrega sufijo si ya existe
- Crear sin el campo código funciona
- La vista previa calcula la jornada del modal y no guarda nada
- La vista previa lista los errores y muestra la salida real pasada las1800
- La vista previa de un formulario vacio responde200 con los errores
- La vista previa funciona en un año activo porque no guarda
- Edita los datos y regenera los recesos
- Editar con datos invalidos da400 y no cambia nada
- Editar con otro código lo ignora y conserva el que tenía
- No se puede desmarcar la principal y marcar otra la cambia y avisa
- Reducir la principal por debajo de las horas planificadas da409
- Cambiar los bloques de la principal con planes avisa que cambia la planificación
- Marcar como principal una con menos bloques que los planes da409
- Elimina una estructura y responde204
- La principal con otras del nivel no se elimina hay que marcar otra
- La única del nivel con planes no se puede eliminar pero sin planes si
- Un año activo rechaza crear editar y eliminar
- Lo que no existe da404 con su mensaje
- Solo el personal administrativo puede usar las estructuras horarias
- Un uuid mal escrito en la ruta devuelve el código que da el manejador compartido
- Un json ilegible da400
- Una hora mal escrita da400

#### Estructura horaria repositorio · integración (17)

`backend/src/test/java/com/galiledu/configuracion/EstructuraHorariaRepositorioIT.java`

- Guarda los días lectivos como arreglo de postgre sql
- Lee los días lectivos de una fila insertada por sql
- Lee la estructura de figma con sus recesos en orden
- Guarda los recesos junto con la estructura
- Un receso que sigue en el mismo bloque solo cambia de duración
- Se puede quitar un receso y volver a ponerlo en el mismo bloque
- Una edición puede quitar todos los recesos y pedir otro en un bloque que tenía
- Editar solo los recesos mueve la fecha de actualización en la base
- Cambiar la principal de un nivel con flush entre degradar y promover
- La base rechaza dos principales del mismo nivel y año
- La base admite una principal por cada nivel
- Lista las estructuras del año por código
- Sabe si un código ya existe en el año
- Al editar el código no choca con la propia estructura
- Encuentra la principal de cada nivel y año
- Una estructura que no es principal no cuenta como principal del nivel
- Cuenta las estructuras de un nivel en el año

#### Matrículas de secciones sql · integración (6)

`backend/src/test/java/com/galiledu/configuracion/MatriculasDeSeccionesSqlIT.java`

- Una sección sin matrículas ni cambios no tiene nada
- Una sección con una matrícula matriculada tiene matrículas
- Una matrícula retirada o anulada sigue contando para el borrado
- Un cambio de sección cuenta para la sección de origen
- Un cambio de sección cuenta para la sección de destino
- Una sección que no participa en el cambio no se ve afectada

#### Período académico · integración (18)

`backend/src/test/java/com/galiledu/configuracion/PeriodoAcademicoIT.java`

- Edita un período futuro y devuelve el período con su estado calculado
- El cambio queda guardado en el detalle del año
- Un período que ya empezo no cambia su fecha de inicio
- Un período que ya empezo si cambia su fin y su fecha limite
- Un período que ya termino solo cambia la fecha limite de notas
- Ampliar la fecha limite de un período terminado reabre el registro de notas
- El plazo ampliado de un período terminado aparece en la card del año actual
- Un año cerrado no edita sus períodos
- El nombre es obligatorio
- El nombre no puede pasar de30 caracteres
- Las fechas son obligatorias
- Las fechas del período deben estar dentro del año
- Un período no puede cruzarse con otro
- La fecha de fin debe ser posterior a la de inicio
- La fecha limite no puede ser anterior al fin del período
- La fecha limite si puede caer dentro del período siguiente
- Editar un período que no existe da404
- Solo el personal administrativo edita períodos

#### Período matrícula (configuración) · integración (23)

`backend/src/test/java/com/galiledu/configuracion/PeriodoMatriculaIT.java`

- El listado trae los períodos del más reciente al más antiguo con su estado
- El listado se filtra por año y por estado y se página
- Vigente es el período abierto y da404 si no hay ninguno
- Los años disponibles son los que aún no tienen período
- El formulario nuevo trae las secciones del año
- El formulario nuevo pide el año y rechaza uno que ya tiene período
- Un período abierto se ve en solo lectura con el resumen del modal
- Crea el período con sus vacantes y documentos
- Sin tarifario confirmado no se habilita la matrícula
- No se crea un período mientras otro sigue sin cerrar
- Una vacante inválida da400 y no cambia nada
- Los datos invalidos dan400
- Edita un período programado
- Un período abierto o cerrado no se edita
- Elimina un período programado sin matrículas
- No se elimina un período programado si el año ya tiene matrículas
- Un período abierto no se elimina y uno inexistente da404
- Sube descarga y quita la plantilla de un documento
- Una plantilla inválida da400 y no se guarda
- La plantilla de un período abierto solo se descarga
- La plantilla de un documento ajeno o inexistente da404
- Solo el personal administrativo configura la matrícula
- Sin token todo da401

#### Período matrícula repositorio · integración (12)

`backend/src/test/java/com/galiledu/configuracion/PeriodoMatriculaRepositorioIT.java`

- Lee el periodo2027 habilitado con sus cuatro documentos
- El periodo2026 está guardado como cerrado
- Guarda un período con sus documentos y lo edita sin chocar con los unique
- Un período sin documentos se guarda y se elimina con sus documentos
- Guarda lee reemplaza y quita la plantilla de un documento
- Guardar la plantilla de un documento que no existe devuelve false
- Editar un período no borra la plantilla de los documentos que se conservan
- Cuenta las matrículas que ocupan vacante de cada año
- Las matrículas anuladas no ocupan vacante pero si impiden eliminar
- Un año sin matrículas no las tiene
- Detecta las entregas de documentos de un período
- Lista los años planificados o activos que aún no tienen período

#### Planes de evaluación sql · integración (8)

`backend/src/test/java/com/galiledu/configuracion/PlanesDeEvaluacionSqlIT.java`

- Devuelve el grado que se pasa con su nombre y sus horas
- Un grado con tantas horas como bloques no se pasa
- Con varios grados pasados elige el de más horas
- Con las mismas horas elige el grado de menor orden
- Suma las horas de todos los cursos del grado antes de compararlas
- Un nivel sin planes no tiene grado que exceda ni hay planes
- Los planes de un año no cuentan para otro
- Los planes de un nivel no cuentan para el otro

#### Restaurador reloj prueba · unitaria (3)

`backend/src/test/java/com/galiledu/configuracion/RestauradorRelojPruebaTests.java`

- Después de una prueba el reloj vuelve a su fecha inicial
- Sin reloj de prueba en el contexto no hace nada
- Sin contexto cargado no lo carga

#### Sección (configuración) · integración (39)

`backend/src/test/java/com/galiledu/configuracion/SeccionIT.java`

- Lista los once grados del año incluidos los que no tienen secciones
- Cada sección trae capacidad alumnos que ocupan vacante tutor y estado
- Filtra por nivel sin importar mayúsculas ni minúsculas
- Rechaza un nivel que no existe
- Listar de un año que no existe da404
- Un año cerrado se puede consultar
- Crea una sección activa sin tutores y el nombre se guarda en mayúsculas
- Una sección nueva puede llamarse como la de otro grado o la de otro año
- Rechaza un nombre repetido en el mismo grado y año
- Válida el nombre la capacidad y el grado de la sección nueva
- Crear en un año que no existe da404
- Cambia la capacidad de una sección
- La capacidad no baja de los alumnos que ocupan vacante
- Una matrícula retirada no impide bajar la capacidad
- Válida el rango de la capacidad
- Asigna tutor y cotutor y el cotutor se puede quitar
- El tutor es obligatorio y el cotutor debe ser distinto
- Tutor igual al cotutor da400 aunque el tutor ya tenga otra sección
- Tutor y cotutor deben ser docentes o tutores activos
- Un usuario con rol tutor sin rol docente también puede ser tutor
- Un docente es tutor de una sola sección por año
- El tutor de una sección puede confirmarse en la misma sección y ser tutor en otro año
- La regla del tutor único no aplica al cotutor
- Reactivar con el tutor ocupado en otra sección se rechaza y se resuelve cambiando el tutor
- Una asignación rechazada no deja cambios en la sección
- Lista los docentes y tutores activos marcando quienes ya son tutores del año
- El tutor de una sección inactiva no aparece como tutor del año
- Docentes elegibles de un año que no existe da404
- Docentes elegibles sin el año escolar da400
- Inactiva y reactiva una sección sin perder sus alumnos
- No se inactiva una sección inactiva ni se reactiva una activa
- Elimina una sección sin matrículas en un año activo y sus docentes del plan se borran en cascada
- Elimina una sección de un año planificado
- No elimina una sección con matrículas
- Una matrícula retirada ya no ocupa vacante pero impide eliminar la sección
- Un cambio de sección impide eliminar la sección de origen y la de destino
- Un año cerrado no admite ningún cambio en sus secciones
- Una sección que no existe da404 en cada operación
- Solo el personal administrativo puede usar las secciones

#### Servicio año escolar · integración (27)

`backend/src/test/java/com/galiledu/configuracion/ServicioAnioEscolarIT.java`

- El detalle trae las cantidades y los períodos con estado duración y registro de notas
- El año actual trae el período en curso los conteos y el avance
- Ampliar el plazo de notas de un período terminado lo muestra aparte del período en curso
- Entre dos períodos la card muestra el plazo de notas abierto y el próximo período
- Después del último período solo queda el plazo de notas y luego nada
- Sin año activo no hay año actual
- Editar las fechas de un año planificado regenera sus períodos
- Editar las fechas válida el rango y el estado del año
- Cambiar el regimen de un año planificado regenera los períodos sin chocar con la restricción de número
- Cambiar el regimen solo se puede en un año planificado
- Los requisitos del año planificado muestran lo que falta
- Los requisitos del año activo se cumplen los obligatorios
- Los requisitos de un año que no existe dan404
- Editar un período futuro cambia sus fechas y devuelve el período calculado
- Un período que ya empezo no cambia su fecha de inicio
- Un período no puede cruzarse con otro
- Ampliar la fecha limite de un período terminado reabre el registro de notas
- Un período terminado no cambia nombre ni fechas
- Editar un período que no existe da404
- El período en curso solo existe en el año activo
- El registro de notas está abierto solo en el período en curso del año activo
- No se activa un año mientras otro sigue activo
- No se activa un año sin sus requisitos obligatorios
- Se activa un año planificado con sus requisitos
- Solo se puede activar un año planificado
- No se cierra el año antes de que venza el plazo de notas del último período
- Solo se puede cerrar un año activo

#### Servicio estructuras horarias (configuración) · integración (49)

`backend/src/test/java/com/galiledu/configuracion/ServicioEstructurasHorariasIT.java`

- Lista las estructuras de un año con sus indicadores
- Un año planificado sin estructuras se marca modificable
- Listar de un año que no existe da404
- El detalle trae la jornada calculada fila por fila
- El detalle de una estructura que no existe da404
- La primera estructura de un nivel nace como principal aunque no se marque
- La segunda del nivel no es principal salvo que se marque
- Marcar como principal al crear baja la anterior en la misma transacción y avisa
- Cada nivel tiene su propia principal
- Guarda los recesos y calcula la salida
- El código lo genera el backend con la sigla del nivel y el año
- Si el código ya existe en el año agrega el sufijo2 y3
- Un código que llega en la solicitud de crear se ignora
- Dos años distintos pueden repetir el código base
- Rechaza con mensaje los días lectivos invalidos
- Rechaza con mensaje los bloques y la duración fuera de rango
- Rechaza con mensaje los recesos invalidos
- La jornada puede terminar a las1800 exactas pero no un minuto después
- Rechaza un nivel que no existe
- Crear en un año que no existe da404
- Un año activo rechaza crear editar y eliminar
- Edita los datos y regenera los recesos sin cambiar el nivel
- Editar conserva el código aunque la solicitud traiga otro
- Editar con datos invalidos da400 y no cambia nada
- Editar una estructura que no existe da404
- No se puede desmarcar la principal hay que marcar otra
- Marcar otra como principal baja la anterior y avisa que la actual pasa a serlo
- Reducir la principal por debajo de las horas planificadas da409
- Con varios grados pasados el mensaje nombra al de más horas
- Reducir hasta las horas planificadas se acepta y avisa
- Aumentar los bloques nunca se rechaza pero avisa si el nivel tiene planes
- Sin planes en el nivel no hay advertencias ni rechazos
- Cambiar entrada duración o recesos no avisa porque no mueven los bloques
- Una estructura que no es principal no se válida contra los planes
- Marcar como principal una con menos bloques que los planes da409 y no cambia nada
- Marcar como principal una con más bloques avisa que cambia la planificación
- Crear una principal con menos bloques que los planes da409 y no cambia nada
- La primera principal de un nivel tampoco puede quedar por debajo de los planes
- Elimina una estructura que no es principal con sus recesos
- La principal con otras del nivel no se elimina hay que marcar otra
- La única del nivel sin planes se puede eliminar
- La única del nivel con planes no se puede eliminar
- Eliminar una estructura que no existe da404
- La vista previa del modal de figma calcula la jornada y no guarda nada
- La vista previa pasada las1800 muestra la salida real y el error
- La vista previa de un formulario vacio lista los errores sin fallar
- La vista previa no pide ni válida el código y solo avisa del nivel inválido
- La vista previa no mira el estado del año ni los planes
- La vista previa de un año que no existe da404

#### Servicio período matrícula · integración (32)

`backend/src/test/java/com/galiledu/configuracion/ServicioPeriodoMatriculaIT.java`

- Vigente es el abierto ahora o el próximo programado o vacio
- Estado en calcula el estado de un período en cualquier instante
- El listado trae el estado calculado las matrículas y el vigente
- El listado se filtra por año y por estado y se página
- Los años disponibles son los planificados o activos sin período
- El período abierto se ve en solo lectura con el resumen del modal
- El monto del formulario sale del tarifario confirmado
- El formulario nuevo trae las secciones del año con su capacidad
- El formulario nuevo rechaza un año que ya tiene período o está cerrado
- Crea el período de un año nuevo con vacantes y documentos y cierra los vencidos
- Sin tarifario confirmado no se habilita la matrícula
- Un año que ya tiene período no se habilita otra vez
- Sin secciones activas no se habilita la matrícula
- No se crea un período mientras otro sigue sin cerrar
- Solo se habilita la matrícula de un año planificado o activo
- Las fechas y los documentos invalidos se rechazan sin guardar nada
- Una vacante inválida no cambia nada ni guarda el período
- Una vacante igual al tope o al número de ocupantes es válida
- Edita un período programado sus fechas vacantes y documentos
- Editar puede retirar un documento y agregar otro con el mismo nombre
- Un período abierto no se edita y uno cerrado tampoco
- Editar con datos invalidos no cambia nada
- No se quita un documento que ya tiene entregas
- Elimina un período programado sin matrículas
- No se elimina un período programado si el año ya tiene matrículas
- Una matrícula anulada también impide eliminar
- Un período abierto o cerrado no se elimina
- Sube reemplaza y quita la plantilla de un documento de un período programado
- La plantilla solo admite pdf docx jpg o png de hasta10 mb
- La plantilla solo se cambia con el período programado pero se descarga siempre
- La plantilla de un documento ajeno o inexistente da404
- Editar el período conserva la plantilla de los documentos que se mantienen

#### Servicio secciones · integración (7)

`backend/src/test/java/com/galiledu/configuracion/ServicioSeccionesIT.java`

- El conteo incluye la matrícula de Sofía
- Una matrícula retirada o anulada no ocupa vacante
- Una matrícula en proceso ocupa vacante desde que se reserva la sección
- Lista las secciones activas por grado y nombre con su tutor capacidad y alumnos
- No incluye las secciones inactivas
- Una matrícula reservada cuenta como alumno que ocupa vacante
- Listar secciones de un año que no existe da404

#### Servicio tarifario · integración (20)

`backend/src/test/java/com/galiledu/configuracion/ServicioTarifarioIT.java`

- Sin tarifario propone los valores del año anterior y el calendario del año
- Sin tarifario ni año anterior la propuesta viene vacia con el calendario generado
- El tarifario2026 es de solo lectura por tener compromisos
- El tarifario2027 está en configuración y se puede editar
- Ver un año que no existe da404
- Confirma el tarifario2027 guardando los datos y dejandolo confirmado
- Confirmar crea el tarifario de un año que no lo tenía
- Confirmar otra vez un tarifario guardado regenera el calendario sin chocar con los unique
- Confirmar rechaza los datos invalidos con su mensaje
- Un dato inválido no borra ni cambia nada del tarifario guardado
- Un tarifario confirmado con compromisos no se puede volver a confirmar
- Un tarifario en configuración con un compromiso si se puede confirmar
- Un año cerrado conserva su tarifario como registro historico
- Un año cerrado sin tarifario tampoco permite crearlo
- Vigente y calendario solo existen para un tarifario confirmado
- Editar las fechas del año regenera el calendario del tarifario confirmado sin tocar montos ni estado
- Editar las fechas también regenera el calendario de un tarifario en configuración
- Con compromisos no se regenera el calendario y el tarifario avisa que quedo desactualizado
- Editar las fechas rechaza un calendario que no seria válido explicando la relación con el tarifario
- Editar las fechas de un año sin tarifario no crea ninguno ni falla

#### Tarifario (configuración) · integración (15)

`backend/src/test/java/com/galiledu/configuracion/TarifarioIT.java`

- El tarifario2027 está en configuración con su calendario
- El tarifario2026 es de solo lectura por tener compromisos
- Un año sin tarifario devuelve una propuesta con los valores del año anterior
- Un año sin tarifario ni año anterior devuelve la propuesta vacia
- Un año cerrado es de solo lectura aunque no tenga tarifario
- El tarifario de un año que no existe da404
- Confirma el tarifario guarda los datos y queda confirmado
- Confirmar dos veces regenera el calendario sin chocar con los unique
- Confirmar crea el tarifario de un año que no lo tenía
- Confirmar rechaza los datos invalidos sin tocar el tarifario
- Confirmar un tarifario con compromisos da409
- Confirmar un año cerrado da409
- Confirmar en un año que no existe da404
- Editar las fechas del año regenera el calendario del tarifario
- Solo el personal administrativo usa el tarifario

#### Tarifario repositorio · integración (5)

`backend/src/test/java/com/galiledu/configuracion/TarifarioRepositorioIT.java`

- Lee el tarifario2026 confirmado con su calendario
- Lee el tarifario2027 en configuración
- Busca el tarifario por el número del año
- Solo el tarifario con compromisos los tiene
- Guarda un tarifario nuevo y regenera su calendario sin chocar con los unique

#### Etiquetas secciones · unitaria (6)

`backend/src/test/java/com/galiledu/configuracion/aplicacion/EtiquetasSeccionesTests.java`

- La etiqueta usa el número del grado y el nombre de la sección
- El resumen de un solo nivel es el de figma
- Un solo grado no lleva rango
- Los nombres se unen con comas y una y
- Con dos niveles se indica cada uno
- Sin secciones el resumen está vacio

#### Servicio estructuras horarias (aplicación) · unitaria (3)

`backend/src/test/java/com/galiledu/configuracion/aplicacion/ServicioEstructurasHorariasTests.java`

- El indice de la principal da el mensaje de otra persona
- La restricción del código da el mensaje de código repetido
- Cualquier otra restricción da un mensaje generico sin nombres internos

#### Año escolar (dominio) · unitaria (53)

`backend/src/test/java/com/galiledu/configuracion/dominio/AnioEscolarTests.java`

- Los parametros de evaluación solo cambian antes de iniciar el año
- Un año bimestral genera cuatro bimestres numerados desde uno
- Un año trimestral genera tres trimestres numerados desde uno
- El año nace planificado y sus períodos aún no empiezan
- Los períodos cubren el año sin cruzarse ni dejar huecos
- Cada período queda dentro del año y termina después de empezar
- La fecha limite de notas es cinco días después del fin del período
- Los días que sobran se reparten entre los primeros períodos
- Editar las fechas regenera los períodos dentro del nuevo rango
- Cambiar el regimen regenera los períodos con la nueva cantidad y nombres
- Quitar los períodos vacia la lista
- Regenerar después de quitar no duplica los períodos
- Solo un año planificado se puede modificar
- Con los días minimos cada período sigue terminando después de empezar
- El período está planificado antes de empezar en curso entre sus fechas y cerrado después
- Un año cerrado cierra todos sus períodos aunque la fecha este dentro
- El registro de notas está abierto hasta la fecha limite inclusive
- Un año cerrado no admite registro de notas
- Un año planificado no tiene períodos en curso ni registro de notas aunque las fechas incluyan hoy
- Un período futuro de un año activo está planificado y sin registro de notas
- Un período terminado con la fecha limite vigente está cerrado pero aún admite notas
- La duración en semanas se redondea como en figma
- El regimen dice su unidad en plural
- El período en curso es el que incluye hoy
- El período con notas abiertas es uno ya terminado nunca el período en curso
- Con varios períodos terminados con notas abiertas se elije el más antiguo
- Entre dos períodos las notas siguen abiertas en el que termino y el próximo es el que empieza
- Sin períodos por empezar ni plazos vigentes no hay nada que mostrar
- Un año planificado o cerrado no tiene período con notas abiertas ni próximo
- Fuera del año o con el año cerrado no hay período en curso
- El avance es el porcentaje entero de días transcurridos entre0 y100
- Un período futuro cambia nombre fechas y limite
- Un año activo también permite editar un período futuro
- Un año cerrado no edita sus períodos
- Un período que ya empezo no cambia su fecha de inicio
- Un período que ya empezo si cambia nombre fin y limite
- Un período que ya termino solo amplia la fecha limite de notas
- El nombre es obligatorio
- Las fechas del período deben estar dentro del año
- El período no puede cruzarse con otro
- Un período puede terminar el día anterior al inicio del siguiente
- La fecha de fin debe ser posterior a la de inicio
- La fecha limite no puede ser anterior al fin pero puede igualarlo
- La fecha limite si puede caer dentro del período siguiente
- Todas las fechas son obligatorias
- Un período que no es del año se rechaza
- Una edición rechazada no cambia el período
- Activar pasa el año planificado a activo sin tocar los períodos
- Solo se puede activar un año planificado
- Solo se puede cerrar un año activo
- No se cierra el año hasta pasar la fecha limite de notas del último período
- El día después de la fecha limite del último período se cierra el año
- Un año cerrado no se reabre

#### Calculadora jornada · unitaria (7)

`backend/src/test/java/com/galiledu/configuracion/dominio/CalculadoraJornadaTests.java`

- Primaria de figma entra a las0745y sale a las1420
- El orden en que llega la lista de recesos no cambia el resultado
- Secundaria de figma sale a las1450
- El modal de figma termina exactamente a las1800
- Sin recesos los bloques van seguidos
- Pasada la medianoche la salida no da la vuelta
- Formatea las horas con dos digitos

#### Calendario cuotas · unitaria (6)

`backend/src/test/java/com/galiledu/configuracion/dominio/CalendarioCuotasTests.java`

- Reproduce el calendario de2026 de los datos de prueba
- Reproduce el calendario de2027 de los datos de prueba
- Si el año termina antes del dia15 del último mes la última cuota vence el día de fin
- Un año que termina en otro mes también tiene la última cuota el dia15
- Un año de un solo mes tiene una cuota
- La matrícula vence el último día del mes anterior al inicio de clases

#### Entradas hostiles estructura horaria · unitaria (31)

`backend/src/test/java/com/galiledu/configuracion/dominio/EntradasHostilesEstructuraHorariaTests.java`

- Un código sin contenido útil se rechaza como vacio
- Un código de solo caracteres invisibles se rechaza por no ser letras numeros ni guion
- El código en el limite de20 se acepta y con uno más se rechaza
- La descripción en el limite de100 se acepta y con una más se rechaza
- Una descripción solo de blancos se guarda como sin descripción
- Los codigos de la hu y los del modal de figma son validos
- Los codigos especiales se normalizan y solo pasan si quedan en aa z numeros y guion
- La eszett que se alarga ass hace que el largo se cuente ya normalizado
- Los emojis y las letras de otros alfabetos ya no se aceptan como código
- El mismo código con y sin espacios y en otra capitalización es el mismo código
- Normalizada es idempotente
- El código y la descripción rechazan los caracteres de control en medio
- El mensaje de control no se repite si el código y la descripción lo tienen
- Los controles al borde de la descripción se recortan y no se rechazan
- El código rechaza rutas barras y espacios internos
- Las cadenas de inyección son datos en la descripción y se rechazan como código
- Un código que válida nunca supera la columna de20 ni tiene caracteres fuera de la regla
- Los días vacios nulos o nulos dentro del arreglo se rechazan
- Los días en minúsculas se aceptan y los nombres completos o no existentes se rechazan
- Los días repetidos se rechazan y los seis días se aceptan en orden de semana
- Mil días repetidos o mil invalidos se rechazan sin fallar
- Los bloques y la duración en los bordes y fuera de ellos
- Los recesos en los bordes y fuera de ellos
- La jornada maxima posible no desborda ni da horas negativas
- Las horas limite con la regla de las1800
- La hora con segundos se trunca a minutos al normalizar
- Los recesos nulos vacios o duplicados no fallan en la normalización
- Un null dentro de los recesos no debe revertar el dominio
- Cinco mil recesos se rechazan sin fallar
- Los recesos desordenados se guardan en orden de bloque
- Crear con datos que no validan no deja la estructura a cedias

#### Estructura horaria (dominio) · unitaria (27)

`backend/src/test/java/com/galiledu/configuracion/dominio/EstructuraHorariaTests.java`

- Crea la estructura de figma con sus recesos y sus bloques por semana
- El modal de figma es12 bloques por semana con tres días
- Normaliza el código la descripción y el orden de los días
- El código es obligatorio y admite hasta20 caracteres
- El código se válida ya normalizado sin contar los espacios del borde
- La descripción es opcional y admite hasta100 caracteres
- Exige al menos un día lectivo sin repetir y solo de lunes a sábado
- Los bloques por día van de1 a12
- La duración del bloque va de20 a120 minutos
- El receso dura de5 a60 minutos
- El receso va después de un bloque entre1 y bloques menos1
- Con un solo bloque no se puede poner un receso
- No permite dos recesos después del mismo bloque
- La jornada puede terminar exactamente a las1800
- Un minuto después de las1800 se rechaza y la vista previa conserva la jornada
- Una jornada que cruza la medianoche se rechaza
- Sin datos para calcular no hay jornada
- Detecta todos los errores juntos para la vista previa
- El nivel es primaria o secundaria
- Solo un año planificado se modifica
- Editar cambia los datos pero no el nivel ni la principal
- Editar conserva los recesos que siguen en el mismo bloque y solo cambia su duración
- Tras editar los recesos están en orden y la jornada es correcta sin recargar de la base
- Con bloques invalidos no se muestra un rango de receso sin sentido
- Editar solo los recesos actualiza la fecha de actualización
- Editar con datos invalidos no cambia nada
- Marcar y desmarcar como principal

#### Período matrícula (dominio) · unitaria (25)

`backend/src/test/java/com/galiledu/configuracion/dominio/PeriodoMatriculaTests.java`

- Guardar la configuración deja el período habilitado con los flags explicitos
- Las fechas se guardan en hora de lima
- Un período sin documentos es válido
- Es programado antes del inicio
- Es abierto desde el inicio hasta el cierre ambos incluidos
- Es cerrado después del cierre
- Un período guardado como cerrado es cerrado aunque la fecha este dentro del rango
- Un período que nunca se habilito nunca está abierto
- Un habilitado que ya vencio se detecta para marcarlo cerrado
- La fecha y la hora de inicio y cierre son obligatorias
- El cierre debe ser posterior al inicio
- El inicio no puede ser anterior a ahora
- El nombre del documento es obligatorio
- El nombre admite hasta100 caracteres
- La descripción es opcional y admite hasta100 caracteres
- El nombre es único en el período sin distinguir mayúsculas ni espacios
- Una descripción vacia se guarda como nula
- Un período programado cambia sus fechas y sus documentos
- Editar quita los documentos que no vienen
- Retirar los ausentes deja solo los que vienen por id
- Un documento que no es del período se rechaza
- Editar con datos invalidos no cambia nada
- Un período abierto no se edita ni se elimina
- Un período cerrado no se edita ni se elimina
- Un período programado se edita y se elimina

#### Plantilla documento · unitaria (12)

`backend/src/test/java/com/galiledu/configuracion/dominio/PlantillaDocumentoTests.java`

- Acepta un pdf
- Acepta un docx
- Acepta un jpg o jpeg
- Acepta un png
- La extensión no distingue mayúsculas
- Rechaza otros tipos
- Rechaza un archivo cuyo contenido no corresponde a la extensión
- Rechaza un archivo vacio o nulo
- Admite hasta10 mb y rechaza más
- Sin nombre de archivo se rechaza
- Guarda solo el nombre del archivo sin la ruta
- El nombre del archivo admite hasta200 caracteres

#### Sección (dominio) · unitaria (17)

`backend/src/test/java/com/galiledu/configuracion/dominio/SeccionTests.java`

- La sección nace activa y sin tutores
- El nombre se guarda sin espacios y en mayúsculas
- Rechaza un nombre vacio
- Rechaza un nombre de más de cinco caracteres
- Rechaza un nombre con simbolos
- Exige un grado y un año
- La capacidad va de uno a cuarenta y cinco
- Cambia la capacidad si no baja de los alumnos que ocupan vacante
- No baja la capacidad por debajo de los alumnos que ocupan vacante
- Al cambiar la capacidad también se válida el rango
- Asigna tutor y cotutor
- El cotutor es opcional y se puede quitar
- El tutor es obligatorio
- El cotutor debe ser distinto del tutor
- Inactiva y reactiva la sección
- No se inactiva una sección inactiva ni se reactiva una activa
- Un año cerrado no se modifica

#### Tarifario (dominio) · unitaria (19)

`backend/src/test/java/com/galiledu/configuracion/dominio/TarifarioTests.java`

- Un tarifario nuevo no está confirmado
- Confirmar guarda los datos genera el calendario y deja el tarifario confirmado
- La cuota de ingreso es opcional
- Un tarifario confirmado se puede confirmar otra vez con otros datos
- Un dato inválido no cambia el tarifario
- Regenerar el calendario conserva montos morosidad y estado
- La matrícula es obligatoria y no puede ser negativa
- La matrícula puede ser cero
- La pensión es obligatoria y mayor que cero
- La cuota de ingreso no puede ser negativa
- Los montos admiten como maximo2 decimales y el limite de la columna
- Los días de gracia van de0 a60
- El recargo va de0 a100
- El tope de recargo es obligatorio y va de0 a100
- El recargo no puede ser mayor que el tope
- El calendario no puede tener más de12 cuotas
- Un año con una cuota fuera del año se rechaza
- No permite dos cuotas con la misma fecha de vencimiento
- Un calendario vacio se rechaza

### Frontend

#### Años escolares (7)

`frontend/src/features/configuracion/api/__tests__/anios-escolares.test.tsx`

- la lista trae los años y no consulta el año en curso si no hay uno ACTIVO
- con un año ACTIVO sí consulta el año en curso
- el detalle y los requisitos se consultan por id, y los requisitos solo con el año PLANIFICADO
- sin id no consulta el detalle
- cada mutación llama a su endpoint con su cuerpo
- al cerrar el único año ACTIVO recarga la lista y no vuelve a pedir el año en curso (evita el 404)
- al activar un año recarga la lista y también el año en curso

#### Estructuras horarias (14)

`frontend/src/features/configuracion/api/__tests__/estructuras-horarias.test.tsx`

- la lista es un objeto con `anioModificable` y las estructuras del año
- con el año no modificable la lista lo indica
- sin id de año no consulta la lista
- el detalle trae la jornada calculada, con bloques y recesos
- sin estructura elegida no pide el detalle
- la vista previa manda el formulario tal cual, también a medio llenar
- la jornada llega aunque pase de las 18:00
- sin formulario no pide la vista previa
- mientras llega la vista previa nueva se sigue mostrando la anterior, y una respuesta vieja no pisa a la nueva
- crear manda el cuerpo, devuelve las advertencias y recarga la lista y los datos del año
- un 409 al crear (código repetido) se avisa una sola vez y no se llama a onSuccess ni se recarga
- editar manda el cuerpo sin nivel a la estructura elegida y recarga su detalle
- eliminar responde 204, recarga la lista y no vuelve a pedir el detalle de la estructura eliminada
- un 409 al eliminar (la principal con otras del nivel) se avisa una sola vez y no se recarga

#### Mutaciones período matrícula (4)

`frontend/src/features/configuracion/api/__tests__/mutaciones-periodo-matricula.test.tsx`

- cada mutación llama a su endpoint con su cuerpo
- al crear avisa con el periodo guardado
- si el backend rechaza, no avisa de éxito y el mensaje sale como notificación
**al eliminar el periodo vigente**

- recarga la lista y no vuelve a pedir /vigente (evita el 404)

#### Mutaciones tarifario (4)

`frontend/src/features/configuracion/api/__tests__/mutaciones-tarifario.test.tsx`

- confirma con el cuerpo de la solicitud y deja en la consulta lo que devolvió el backend, sin volver a pedirlo
- vuelve a pedir el tarifario de los otros años, que parten de los valores de este
- refresca el detalle de un periodo de matrícula, que depende del tarifario confirmado
- si el backend rechaza (409 o 400), no avisa de éxito y su mensaje sale como notificación

#### Períodos matrícula (8)

`frontend/src/features/configuracion/api/__tests__/periodos-matricula.test.tsx`

- el listado pide la página con sus filtros y deja fuera los vacíos
- la lista completa pide todos los periodos de una vez
- /vigente solo se pide si hay un periodo vigente: sin él respondería 404 y saldría como error
- con un periodo vigente sí pide /vigente
- los años disponibles salen de /anios-disponibles
- el formulario nuevo se pide para el año elegido, y no se pide sin año
- el detalle de un periodo se pide por id
- la plantilla se descarga como archivo

#### Plantillas (9)

`frontend/src/features/configuracion/api/__tests__/plantillas.test.ts`

**emparejarPlantillas**

- une cada archivo con su documento por nombre, no por posición
- ignora mayúsculas y espacios en los bordes, como el backend
- descarta un archivo cuyo documento no quedó guardado
- subirPlantilla envía el archivo en la parte "archivo"
- quitarPlantilla pide el DELETE del documento
**subirPlantillas**

- las sube en orden y devuelve sin fallos
- si una falla sigue con las demás y dice cuál falló
- sin pendientes no hace ninguna petición
- descargarPlantilla guarda el archivo con el nombre que se subió

#### Secciones (7)

`frontend/src/features/configuracion/api/__tests__/secciones.test.tsx`

- la consulta trae todos los grados del año, también los que no tienen secciones
- sin id de año no consulta las secciones
- los docentes elegibles se piden con el año y traen quién ya es tutor
- los docentes elegibles no se piden mientras el modal está cerrado
- crear una sección la envía al año y recarga las secciones y los datos del año
- asignar tutores envía el tutor y el cotutor en null, y recarga lo mismo
- si el backend rechaza la creación (409) no se ejecuta onSuccess ni se recarga nada

#### Tarifarios (4)

`frontend/src/features/configuracion/api/__tests__/tarifarios.test.tsx`

- pide el tarifario del año con su anioEscolarId
- sin año elegido no pide nada (sin handler, una petición haría fallar la prueba)
- una propuesta sin tarifario del año anterior llega con los montos vacíos
- un año inexistente (404) no entrega datos y el mensaje sale como notificación

#### Acciones año (14)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/acciones-anio.test.tsx`

**año PLANIFICADO**

- muestra "Editar" (secundario) y "Activar año" (principal), en ese orden
- Activar año pide confirmación con el texto de Figma y activa al confirmar
- Cancelar en la confirmación no activa nada
- con otro año ACTIVO, "Activar año" queda deshabilitado y el tooltip explica por qué
- con requisitos obligatorios pendientes, se deshabilita y nombra los que faltan
- "Editar" abre el modal con el año
**año ACTIVO**

- solo ofrece "Cerrar año", sin pedir la lista ni los requisitos
- Cerrar año pide confirmación y cierra al confirmar
- si el backend responde 409, se ve su mensaje y el modal se cierra
- un año CERRADO no tiene acciones
**motivoSinActivar**

- otro año activo manda sobre todo lo demás
- mientras carga o si falla, avisa y no deja activar
- sin requisitos pendientes devuelve null
- con requisitos pendientes los lista

#### Años lista (7)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/anios-lista.test.tsx`

- con un año ACTIVO muestra la tarjeta "Año escolar en curso" con los datos de Figma
- entre periodos la tarjeta muestra el próximo periodo y el plazo de notas abierto
- la tabla lista los años del más reciente al más antiguo, con su estado
- el menú ⋮ de una fila solo ofrece "Ver detalle"
- sin un año ACTIVO no hay tarjeta y no se consulta el año en curso
- sin años registrados muestra el estado vacío
- si la lista falla muestra un aviso en la pantalla

#### Boton nuevo año (2)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/boton-nuevo-anio.test.tsx`

- abre el modal con el año siguiente al último registrado
- al crear el año abre su página

#### Contenido año (6)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/contenido-anio.test.tsx`

- muestra las cuatro pestañas con sus contadores y abre en Periodos
- configura una sola escala, regla y consolidación para todo el año
- bloquea los parámetros cuando el año ya empezó
- cada pestaña muestra el contenido de su pantalla de Figma
- año ACTIVO: sin la tarjeta de requisitos, y sin pedirlos
- año PLANIFICADO: muestra los requisitos sobre las pestañas y "Revisar" cambia de pestaña

#### Diálogo año (10)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/dialogo-anio.test.tsx`

- el modal cerrado no muestra nada
- Nuevo: abre con el año siguiente al último, sus fechas y Bimestral, sin "Reutilizar configuración"
- Nuevo: al cambiar el año las fechas se recalculan
- Nuevo: si el usuario ya cambió una fecha, cambiar el año no la pisa
- Nuevo: crea el año con el régimen elegido, avisa y cierra
- Nuevo: un año fuera de rango se rechaza bajo el campo y no se envía
- Nuevo: si el backend rechaza (año repetido), el modal sigue abierto y el aviso sale como notificación
- Cancelar cierra el modal sin enviar nada
- Editar: título con el año, año fijo, sin régimen y con el aviso de periodos
- Editar: guarda solo las fechas

#### Diálogo cambiar regimen (5)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/dialogo-cambiar-regimen.test.tsx`

- abre con el otro régimen marcado y señala el actual, como en Figma
- cambia el régimen, avisa y cierra
- con el régimen actual elegido no se puede enviar
- si el backend rechaza, el modal sigue abierto
- Cancelar cierra sin enviar nada

#### Diálogo editar período (11)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/dialogo-editar-periodo.test.tsx`

- abre con los datos del periodo, el año y la alerta de Figma
- guarda los cambios, avisa y cierra
**validación en el formulario, bajo cada campo**

- el nombre es obligatorio
- un cruce con otro periodo se avisa bajo la fecha de fin
- el fin debe ser posterior al inicio
- las fechas deben estar dentro del año escolar
- la fecha límite no puede ser anterior al fin
- un periodo en curso no deja cambiar la fecha de inicio
- un periodo terminado solo deja cambiar la fecha límite de notas
- si el backend rechaza, el modal sigue abierto y el aviso sale como notificación
- Cancelar cierra sin enviar nada

#### Esquema período (10)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/esquema-periodo.test.ts`

- acepta un periodo válido
- el límite puede ser el mismo día del fin
- nombre y fechas son obligatorios
- el nombre no pasa de 30 caracteres
- el fin debe ser posterior al inicio
- las fechas deben estar dentro del año escolar y el error va en el campo que se pasa
- un cruce con otro periodo se avisa bajo la fecha de fin, con el mismo texto del backend
- dos periodos que se tocan en un día también se cruzan
- la fecha límite no puede ser anterior al fin del periodo
- cada campo muestra a lo sumo un error

#### Esquemas (año escolar) (12)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/esquemas.test.ts`

**valores iniciales del modal Nuevo año escolar (Figma)**

- propone el año siguiente al último registrado, con sus fechas y Bimestral
- el último año es el mayor, aunque la lista venga desordenada
- sin años registrados propone el siguiente al actual
- las fechas sugeridas son 01/03 y 15/12 del año
**anioEscrito**

- solo reconoce un año completo y dentro del rango
**nuevoAnioSchema (mensajes del backend)**

- acepta los datos completos
- el año es obligatorio y va de 2000 a 2100
- las fechas son obligatorias
- el fin debe ser posterior al inicio y el error va en la fecha de fin
- el régimen debe ser Bimestral o Trimestral
- la regla de cálculo debe corresponder a la escala
**edicionAnioSchema**

- solo valida las fechas

#### Formato (año escolar) (14)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/formato.test.ts`

**textos del régimen (Figma: CI-01 y modal Nuevo año escolar)**

- nombre del régimen y periodos al año
- la columna Periodos usa la unidad del régimen
**fechas y subtítulo de la cabecera**

- rango de fechas con día/mes/año
- subtítulo como en CI-03 y CI-06
**etiquetas de estado**

- año: Planificado, Activo y Cerrado
- periodo: Planificado, En curso y Cerrado
- diasRestantes: plural, singular y el mismo día
- anioActivo devuelve el año ACTIVO o undefined
**datosDePeriodos (tarjeta "Año escolar en curso" de CI-01)**

- con un periodo en curso muestra ese periodo y su límite, como en Figma
- entre periodos muestra el próximo y el periodo con notas abiertas
- con un plazo ampliado y otro periodo en curso, el límite es el del periodo que vence primero
- sin periodos ni plazos, avisa sin inventar fechas
- el último día del plazo dice "Vence hoy"
- textos de los datos de la tarjeta y de la tabla

#### Linea de tiempo (6)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/linea-de-tiempo.test.tsx`

- porcentajeDelAnio: cada día ocupa el mismo ancho y el fin llega hasta el final de su día
- coloca cada periodo según sus fechas y muestra el inicio y el fin del año
- cada segmento indica su estado, para quien no ve el color
- con el año ACTIVO marca "Hoy" en el centro del día
- no marca "Hoy" si el día cae fuera del año
- no marca "Hoy" si el año no está activo

#### Períodos pestaña (6)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/periodos-pestana.test.tsx`

- año ACTIVO: explica el régimen y el límite de notas, y no ofrece cambiar el régimen
- la tabla muestra cada periodo con sus fechas, duración y estado
- muestra la línea de tiempo del año
- el menú ⋮ de un periodo ofrece "Editar periodo" y abre el modal
- año PLANIFICADO: ofrece "Cambiar régimen" y lo explica
- año CERRADO: los periodos son de solo lectura, sin menú ⋮

#### Requisitos año (7)

`frontend/src/features/configuracion/components/anio-escolar/__tests__/requisitos-anio.test.tsx`

- muestra los cinco requisitos con su título y detalle, cumplidos o pendientes
- "Revisar" lleva a la pestaña de cada requisito
- el plan de evaluación se revisa en Competencias, donde se configura
- con otro año ACTIVO muestra "El año 2026 sigue activo"
- sin otro año activo no muestra ese aviso
- si los requisitos no cargan, la tarjeta no aparece
- destinoDeRevision: cuatro requisitos cambian de pestaña y el plan va a Competencias

#### Acciones estructura (16)

`frontend/src/features/configuracion/components/estructura-horaria/__tests__/acciones-estructura.test.tsx`

**botones Editar y Eliminar de la tarjeta**

- con el año modificable están habilitados
- con el año no modificable quedan bloqueados, no abren nada y un tooltip dice por qué
**Editar estructura horaria**

- abre con los datos de la estructura ya puestos
- el nivel no se puede cambiar
- la principal no se puede desmarcar y lo explica
- la vista previa arranca con la jornada que ya tiene la estructura
- guarda los cambios sin el nivel, avisa, cierra y deja elegida la estructura
- marcar como principal una que no lo es se envía en el cuerpo
- cada advertencia del backend sale como notificación de advertencia
- un 409 (por ejemplo, los planes no caben) se muestra una sola vez y el modal sigue abierto
- los errores en vivo siguen funcionando al editar
- Cancelar cierra sin enviar nada
**Eliminar estructura horaria**

- pide confirmación con el código de la estructura antes de eliminar
- al confirmar elimina, avisa, cierra y la lista pasa a la primera que queda
- la principal con otras del nivel da 409: se avisa una sola vez y no se elimina
- Cancelar no elimina

#### Diálogo estructura (33)

`frontend/src/features/configuracion/components/estructura-horaria/__tests__/dialogo-estructura.test.tsx`

**al abrir**

- muestra el título, el subtítulo con el año y los campos vacíos, salvo la hora de inicio que abre en 07:30
- ofrece los seis días sin marcar, el nivel y la casilla de estructura principal
- la vista previa pide completar los datos y repite el aviso de las 18:00, sin pedir nada al backend
**validaciones en vivo**

- los bloques fuera de 1 a 12 y la duración fuera de 20 a 120 avisan con los mensajes del backend
- desmarcar el último día avisa que hace falta al menos uno
- con el formulario vacío no envía y pide cada dato
**recesos**

- "Agregar receso" suma una fila y "Quitar" la saca
- con 4 bloques por día el receso ofrece "Después del bloque 1" a "3"
- al bajar los bloques por día se revisan los recesos ya puestos
- la duración del receso fuera de 5 a 60 avisa con el mensaje del backend
**vista previa de la jornada**

- con la hora, los bloques y la duración pide la jornada al backend y la dibuja
- no se pide en cada tecla: escribir "45" seguido manda una sola petición
- si la jornada pasa de las 18:00 se ve igual, con la salida real y el aviso en rojo
- los bloques que terminan después de las 18:00 se marcan en rojo con su hora normal
- una jornada que pasa de medianoche nunca muestra horas como 24:, 25: o 28:
- un error del backend que el formulario no cubre se muestra; los que ya salen en su campo no se repiten
**crear**

- manda el cuerpo correcto, avisa, deja elegida la nueva y cierra el modal
- marcar "Estructura principal" se envía en el cuerpo
- cada advertencia del backend sale como una notificación de advertencia con su texto
- un 409 se muestra una sola vez y el modal sigue abierto con lo escrito
- Cancelar cierra el modal sin enviar nada
**la vista previa no desaparece al editar**

- agregar una fila de receso vacía no pide nada nuevo ni oculta la jornada
- al completar el receso aparece entre los bloques ya dibujados, que se actualizan en su lugar
- mientras llega la respuesta nueva se sigue viendo la anterior, marcada como actualizando
- quitar el receso vuelve a pedir la jornada sin él
- con datos que no alcanzan para calcular se conserva la última vista previa y se avisa
- al corregir los datos la vista previa vuelve a actualizarse y desaparece el aviso
**un receso nuevo no sale en error hasta su primera entrada**

- al agregar la fila no hay ningún error
- al elegir "Después del bloque…" la duración, todavía vacía, no sale en error
- con la fila nueva vacía, cambiar los bloques por día tampoco la pone en error
- el error aparece con la primera entrada inválida, y se va al corregirla
- al enviar con el receso vacío sí se avisa de lo que falta
- un receso ya escrito sigue revisándose al cambiar los bloques, aunque haya otra fila vacía

#### Esquemas estructura (43)

`frontend/src/features/configuracion/components/estructura-horaria/__tests__/esquemas-estructura.test.ts`

- los datos del modal de Figma pasan sin mensajes
- una estructura sin descripción ni recesos también es válida
**código**

- el formulario ya no tiene código: lo genera el backend
**descripción**

- es opcional
- admite hasta 100 caracteres
**nivel**

- acepta %s
- sin elegir pide el nivel
**días lectivos**

- al menos uno
- acepta %j
**hora de inicio**

- una hora vacía o mal escrita pide la hora de inicio (%j)
- acepta %s
**bloques por día**

- acepta enteros de 1 a 12 (%j)
- fuera de 1 a 12 o que no es un entero da el mensaje del backend (%j)
**duración del bloque**

- acepta %s
- fuera de 20 a 120 o que no es un entero da el mensaje del backend (%j)
**recesos**

- la duración acepta %s
- la duración fuera de 5 a 60 o que no es un entero da el mensaje del backend (%j)
- con 4 bloques por día un receso puede ir después del bloque %s
- con 4 bloques por día un receso después del bloque %j da "entre 1 y 3"
- el mensaje de la posición nombra el último bloque posible
- con un solo bloque por día no hay dónde poner un receso
- con bloques por día inválidos no se avisa de la posición: ya avisa el campo de bloques
- dos recesos después del mismo bloque se avisan en el segundo
- recesos en bloques distintos son válidos
- cada receso avisa lo suyo: uno bien y otro mal
- un formulario vacío avisa cada campo obligatorio una sola vez
- al abrir el modal la hora de inicio es 07:30 y no hay nada más precargado ni recesos
**cuerpos que se envían**

- crear convierte los números y ordena los días por la semana
- una descripción vacía se envía como null
- editar no lleva el nivel y conserva la marca de principal
- la vista previa acepta el formulario completo con los mismos datos que crear
- la vista previa con el formulario a medio llenar manda null en lo que falta
**recesos en la vista previa: solo los completos y válidos**

- un receso completo y válido se manda
- un receso %s no se manda
- sin bloques por día válidos no hay dónde poner recesos
- agregar una fila vacía no cambia lo que se manda
- con dos recesos después del mismo bloque solo se manda el primero
**puedeCalcularseLaJornada**

- con hora, bloques y duración válidos
- no, %s
- el nivel y los días no hacen falta para calcular
- una hora mal escrita no se manda en la vista previa
**valoresDeEstructura: abrir el modal en modo edición**

- escribe todo como lo escribiría el usuario: textos, días en orden y recesos en orden
- los valores que abre el modal son válidos y vuelven al mismo cuerpo que se guardó

#### Estructura horaria pestaña (16)

`frontend/src/features/configuracion/components/estructura-horaria/__tests__/estructura-horaria-pestana.test.tsx`

- la lista muestra el título, cuántas hay registradas y una tarjeta por estructura
- cada tarjeta marca si es la principal de su nivel
- abre con la primera estructura elegida y muestra sus datos
- la jornada semanal tiene una columna por día lectivo, los bloques con su horario y los recesos en toda la fila
- al elegir otra estructura pide su detalle y muestra sus días y su jornada
- con el año modificable "Nueva" está habilitado
- con el año no modificable "Nueva" queda bloqueado y un tooltip dice por qué
- el bloqueo lo decide `anioModificable` del backend, no el estado del año
- sin estructuras avisa y no muestra ninguna tarjeta
- con una sola estructura el contador va en singular
- si no se pueden cargar las estructuras avisa sin mostrar la lista
- si no se puede cargar el detalle avisa en la tarjeta y la lista sigue
- "Nueva" abre el modal "Nueva estructura horaria"
- con el año no modificable "Nueva" no abre el modal
- al crear una estructura el modal se cierra, la lista se recarga y la nueva queda elegida
- el aviso de que no hay estructuras es texto simple, no una caja que parezca un campo

#### Formato (estructura horaria) (5)

`frontend/src/features/configuracion/components/estructura-horaria/__tests__/formato.test.ts`

- los días seguidos van como rango y los demás separados por comas
- los días se ordenan por la semana aunque lleguen en otro orden
- la cantidad de días y de estructuras va en singular cuando es una
- la jornada de la tarjeta lleva entrada, salida, bloques y duración
- los bloques por día y los recesos se escriben como en Figma

#### Card período vigente (6)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/card-periodo-vigente.test.tsx`

- muestra el periodo vigente con sus fechas, año y matrículas registradas
- un periodo programado dice cuándo abre
- sin periodo vigente no hay tarjeta y no se pide /vigente (respondería 404 y saldría como error)
**botón "Ver matrículas"**

- mientras la pantalla de Matrícula no exista queda deshabilitado, con el aviso "Disponible pronto"
- con la pantalla disponible es un enlace a las matrículas del periodo
- "Ver configuración" abre el modal con la configuración del periodo

#### Diálogo resumen configuración (7)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/dialogo-resumen-configuracion.test.tsx`

- un periodo abierto: sus datos, sus reglas y el aviso de solo lectura, como en Figma
- las reglas salen del periodo vigente: sin bloqueo por morosidad ni matrícula extemporánea
- un periodo programado todavía se puede editar: no dice que sea de solo lectura
- sin tarifario confirmado no inventa un monto
- si la configuración no carga, avisa en el modal
- "Cerrar" cierra el modal
- cerrado no pide la configuración

#### Esquemas (período matrícula) (14)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/esquemas.test.ts`

**año, fechas y horas**

- acepta un periodo completo
- hay que elegir el año académico
- sin fecha, el aviso va en la fecha; con fecha pero sin hora, en la hora
- el cierre debe ser posterior al inicio, contando la hora
- no pide el orden de las fechas mientras falte alguna
**vacantes**

- son obligatorias
- van de 1 a 45 y son un número entero
- no pueden ser menos que los alumnos que ya ocupan vacante
- cada sección muestra su propio error
- totalDeVacantes suma lo escrito y no cuenta lo que aún no es un número
**validarDocumento**

- el nombre es obligatorio y de hasta 100 caracteres
- el nombre no se repite, sin distinguir mayúsculas ni espacios de los bordes
- la descripción es opcional y de hasta 100 caracteres
- solicitudDePeriodo arma el cuerpo del POST: vacantes en número y documentos sin la plantilla

#### Formato (período matrícula) (15)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/formato.test.ts`

**fechas en hora de Lima**

- un instante UTC se ve como la fecha y la hora del colegio, como en Figma
- cerca de la medianoche cambia de día igual que en Lima, no que en UTC
- las horas de la mañana y del mediodía
**lineaDeApertura**

- un periodo abierto, como en Figma
- un periodo programado dice cuándo abre
- un periodo cerrado dice cuándo cerró
- los estados de la tabla usan los colores de Figma
- la columna Matrículas muestra "-" solo mientras el periodo no abre
- textos de la tarjeta vigente y del modal de configuración
**formulario: fechas cortas y duración**

- día y mes como en la tarjeta "Periodo de matrícula" de Figma
- los días son los que hay entre las dos fechas (76, no los 71 de Figma)
**formulario: horas**

- etiquetas de 12 horas, como "10:00 a.m." de Figma
- el backend manda segundos y se reciben sin ellos
- cada 30 minutos del día, y las 11:59 p. m. al final
- si el valor actual no está en la lista, se agrega en su lugar

#### Formulario período (28)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/formulario-periodo.test.tsx`

**al abrir**

- el primer año disponible ya está elegido y se cargan sus secciones
- con otro año en la lista se puede elegir y se cargan sus secciones
- sin años disponibles avisa y deja el formulario deshabilitado
- si las secciones del año no cargan, avisa en la pantalla
**fechas, horas y duración**

- al elegirlas, el indicador y la barra muestran el rango y los días reales
- el selector de hora ofrece cada 30 minutos y las 11:59 p. m.
**vacantes**

- al cambiarlas, el total se actualiza
- una vacante inválida se rechaza bajo su campo y no se envía nada
- una vacante vacía pide el dato
**validación al guardar**

- sin fechas ni horas avisa bajo cada campo y no envía nada
- con la fecha pero sin la hora, el aviso va en la hora
- el cierre debe ser posterior al inicio
**documentos requeridos**

- se agrega con su descripción (con contador) y se cuenta en el indicador
- el nombre es obligatorio y no se repite
- se quita con la papelera
- una plantilla de otro formato o de más de 10 MB se rechaza en la zona de carga
- una plantilla válida queda a la vista hasta guardar y se muestra en el documento
**guardar**

- envía el periodo con sus fechas, vacantes y documentos, avisa y vuelve al listado
- con las vacantes cambiadas envía las capacidades nuevas
- las plantillas elegidas se suben después de guardar, con los ids que devuelve el backend
- si una plantilla falla, avisa y pasa a editar el periodo ya guardado para reintentar
- el rechazo del backend (%s) se muestra tal cual y el formulario sigue abierto
- "Cancelar" vuelve al listado sin guardar
**año sin tarifario confirmado**

- avisa en la tarjeta del monto, no muestra monto y no deja guardar
- el enlace al tarifario está deshabilitado con "Disponible pronto" hasta que exista esa pantalla
- con la pantalla disponible, el aviso enlaza al tarifario de ese año
**"Ver tarifario completo"**

- mientras P19 no exista queda deshabilitado, con "Disponible pronto"
- con P19 disponible es un enlace al tarifario de ese año

#### Período existente (17)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/periodo-existente.test.tsx`

**ver (solo lectura)**

- un periodo programado muestra todo sin poder cambiarlo, con "Editar" y "Volver"
- un periodo %s avisa que no se edita y solo ofrece volver
- la plantilla guardada se descarga con su nombre de archivo
- si el periodo no carga, lo avisa
**editar**

- un periodo abierto o cerrado no se edita: redirige a "Ver"
- abre con lo guardado: año fijo, fechas, horas, vacantes y documentos
- un documento con entregas no se puede quitar; los demás sí
- guarda los cambios sin cambiar el año, con los ids de los documentos, avisa y vuelve al listado
- las reglas de las vacantes se validan antes de enviar
- un documento quitado deja de enviarse
- un documento nuevo viaja sin id y su plantilla se sube con el id que devuelve el backend
- "Cambiar plantilla" sube la nueva al guardar; no antes
- una plantilla de formato no permitido se rechaza en el documento
- "Quitar plantilla" la quita al guardar, no antes, y se puede deshacer eligiendo otra
- si una plantilla falla, avisa, no sale de la edición y deja volver a intentar
- el rechazo del backend (409) se muestra tal cual y la edición sigue abierta
- "Cancelar" vuelve al listado sin guardar

#### Períodos matrícula lista (17)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/periodos-matricula-lista.test.tsx`

- la tabla muestra cada periodo con sus fechas en hora de Lima, matrículas y estado
- el estado es una pastilla informativa, no un botón
- el filtro "Año escolar" ofrece los años de todos los periodos y se envía al backend
- el filtro "Estado" ofrece los tres estados y se envía al backend
- con muchos periodos se pagina y se vuelve a la primera página al filtrar
- sin periodos muestra el estado vacío y no hay paginación
- sin resultados para los filtros lo dice
- si el listado falla muestra un aviso en la pantalla
**menú ⋮ de cada fila**

- programado: Ver, Ver matrículas (deshabilitada, "Disponible pronto"), Editar y Eliminar
- abierto: Ver, Ver matrículas y Editar (abre la vista de solo lectura), sin Eliminar
- cerrado: solo Ver y Ver matrículas, sin Editar
- Ver y Editar llevan a su ruta con el id del periodo
- "Ver matrículas" deshabilitada no navega
- con la pantalla de Matrícula disponible, "Ver matrículas" se habilita y lleva a ella
**eliminar un periodo programado**

- pide confirmación y, al confirmar, elimina el periodo, avisa y quita la fila
- "Cancelar" cierra sin eliminar
- si el backend lo rechaza (409) muestra su mensaje tal cual, cierra y el periodo sigue

#### Plantilla (5)

`frontend/src/features/configuracion/components/periodo-matricula/__tests__/plantilla.test.ts`

- acepta PDF, DOCX, JPG y PNG, sin importar las mayúsculas
- rechaza otros formatos
- el límite es de 10 MB: justo 10 MB sirve y un byte más no
- un archivo vacío no sirve
- los formatos del selector de archivos coinciden con los permitidos

#### Acciones sección (18)

`frontend/src/features/configuracion/components/secciones/__tests__/acciones-seccion.test.tsx`

- el menú ⋮ ofrece Asignar tutor, Editar sección y Eliminar sección
**Asignar tutor**

- muestra el título y el texto de Figma con los tutores actuales elegidos
- no muestra "Vigente desde" ni el historial, que el MVP no guarda
- deshabilita a quien ya es tutor de otra sección, pero no al tutor de esta
- elegir una opción deshabilitada no la selecciona
- el cotutor ofrece "Sin cotutor" y no deja elegir al tutor
- guarda el tutor y el cotutor elegidos, avisa y cierra
- sin tutor no envía y pide elegir uno
- un error del backend se muestra una sola vez y el modal sigue abierto
- Cancelar cierra sin enviar
**Editar sección**

- abre con la capacidad actual y el número de alumnos
- envía la capacidad nueva como número, avisa y cierra
- la capacidad %s da el aviso de Figma y no se envía
- un error del backend (menor que los matriculados) sale una sola vez y el modal sigue abierto
**Eliminar sección**

- pide confirmación antes de eliminar
- al confirmar elimina, avisa y cierra
- si la sección tiene historial, el 409 del backend se ve una vez y el diálogo sigue abierto
- Cancelar no elimina

#### Diálogo sección (12)

`frontend/src/features/configuracion/components/secciones/__tests__/dialogo-seccion.test.tsx`

- "Nueva sección" abre el modal con su título, el año y los campos vacíos
- "Agregar sección" de un grado abre el modal con ese grado elegido
- el modal ofrece los grados del año
- el nombre se pasa a mayúsculas mientras se escribe
- los errores salen en vivo, al escribir, con los mensajes de Figma
- con los campos vacíos no envía y pide cada dato
- crea la sección con el cuerpo correcto, avisa y cierra el modal
- desde "Agregar sección" envía el grado que ya traía
- un error del backend (nombre repetido) se muestra una sola vez y el modal sigue abierto
- Cancelar cierra el modal sin enviar nada
- al volver a abrirlo no conserva lo escrito antes
- año CERRADO: ningún botón abre el modal

#### Esquemas sección (14)

`frontend/src/features/configuracion/components/secciones/__tests__/esquemas-seccion.test.ts`

- unos datos válidos pasan sin mensajes
- el grado es obligatorio
- el nombre vacío o de solo espacios pide el nombre (%j)
- el nombre acepta letras y números hasta 5 caracteres (%j)
- el nombre admite hasta 5 caracteres
- el nombre solo admite letras y números (%j)
- con un nombre largo y con signos se muestra primero el aviso del largo
- la capacidad acepta enteros de 1 a 45 (%j)
- la capacidad vacía la pide (%j)
- la capacidad fuera de 1 a 45 o que no es un entero da el mensaje de Figma (%j)
- cada campo da un solo mensaje aunque varias reglas fallen a la vez
- el nombre se pasa a mayúsculas al recibir lo escrito, sin quitar espacios
- al abrir el modal no hay nada precargado, salvo el grado si viene de un grado
- el cuerpo que se envía recorta el nombre y convierte la capacidad a número

#### Formato (secciones) (4)

`frontend/src/features/configuracion/components/secciones/__tests__/formato.test.ts`

- la etiqueta de la sección lleva su grado: "1.° A"
- las secciones activas van en plural, salvo una
- el porcentaje de matriculados es redondeado y nunca divide por cero
- el estado de la sección se ve como Activa o Inactiva

#### Secciones pestaña (10)

`frontend/src/features/configuracion/components/secciones/__tests__/secciones-pestana.test.tsx`

- muestra el título, el botón "Nueva sección" y las columnas de Figma
- cada grado es una fila de grupo con sus secciones activas y su botón "Agregar sección"
- "Nueva sección" es el botón principal y "Agregar sección" es terciario
- cada sección muestra su etiqueta, capacidad, matriculados, tutor, cotutor y estado
- un nombre largo de tutor o cotutor se recorta en una línea y se ve completo al pasar el mouse
- la barra de matriculados refleja los alumnos sobre la capacidad
- año ACTIVO: los botones están habilitados y cada sección tiene su menú ⋮ con "Asignar tutor"
- año CERRADO: los botones para agregar quedan deshabilitados y no hay menú ⋮
- año PLANIFICADO también admite cambios
- si no se pueden cargar las secciones avisa sin mostrar la tabla

#### Configuración pensiones (14)

`frontend/src/features/configuracion/components/tarifario/__tests__/configuracion-pensiones.test.tsx`

**año que abre la pantalla**

- sin año en la dirección, el PLANIFICADO más reciente
- con ?anioEscolarId= abre ese año
- al cambiar de año en el selector pide el de ese año
**un tarifario en configuración (Figma 2027)**

- muestra los conceptos y montos con su nombre accesible en soles
- muestra la política de morosidad
- el calendario lista las cuotas con su vencimiento, su monto y el total
- los campos se pueden editar; la pestaña de descuentos está disponible y hay "Volver" y "Confirmar tarifario"
**otros estados del tarifario**

- una propuesta sin datos del año anterior deja los campos vacíos y el calendario sin montos
- de solo lectura: muestra el motivo del backend y no ofrece confirmar
- calendario desactualizado y se puede confirmar: avisa que hay que volver a confirmar
- calendario desactualizado y de solo lectura: avisa que no se puede regenerar
- un tarifario que no carga lo avisa en la pantalla
- sin años escolares lo dice y no pide ningún tarifario
- si los años no cargan, lo avisa en la pantalla

#### Confirmar tarifario (14)

`frontend/src/features/configuracion/components/tarifario/__tests__/confirmar-tarifario.test.tsx`

**edición**

- el calendario sigue a la pensión que se escribe: cada cuota y el total
- con la pensión vacía, el calendario queda sin monto
- los datos inválidos se avisan bajo cada campo y no se pide confirmación ni se envía nada
**modal de confirmación**

- resume lo que se va a confirmar, como en Figma
- sin cuota de ingreso, la muestra con "—" y la envía como null; "1 día" va en singular
- "Cancelar" cierra el modal sin confirmar y deja lo escrito
- "Confirmar" envía los montos y la morosidad, avisa "Tarifario confirmado" y deja el formulario con lo guardado
- si el backend rechaza (409), muestra su mensaje tal cual, cierra el modal y deja lo escrito
**cambios sin confirmar**

- al cerrar o recargar la pestaña, el navegador avisa solo si hay cambios
- al confirmar ya no hay cambios pendientes
- "Volver" con cambios pide confirmación; "Seguir editando" se queda con lo escrito
- "Salir sin confirmar" deja seguir con la navegación
- al cambiar de año con cambios pide confirmación; "Seguir editando" se queda en el mismo año
- "Cambiar de año" descarta lo escrito y abre el otro año

#### Esquemas (tarifario) (13)

`frontend/src/features/configuracion/components/tarifario/__tests__/esquemas.test.ts`

- acepta los valores de Figma
**montos**

- la matrícula y la pensión son obligatorias; la cuota de ingreso, no
- no pueden ser negativos
- admiten hasta 2 decimales; ceros de más no cuentan
- no pasan de S/ 99999999.99
- la pensión debe ser mayor que cero; la matrícula y el ingreso pueden ser 0
**política de morosidad**

- los tres campos son obligatorios
- los días de gracia son enteros de 0 a 60
- el recargo y el tope van de 0 a 100 %
- el recargo y el tope admiten hasta 2 decimales
- el recargo no puede ser mayor que el tope (el aviso va en el recargo)
- muestra un mensaje por cada campo con error, no solo el primero como el backend
- solicitudDeTarifario pasa los textos a números y deja la cuota de ingreso vacía en null

#### Formato (tarifario) (16)

`frontend/src/features/configuracion/components/tarifario/__tests__/formato.test.ts`

- los estados usan los colores acordados: ámbar y verde
- las fechas y los meses salen de la fecha, sin pasar por la zona horaria
**resumenDeCuotas**

- cuenta las cuotas y dice entre qué meses, como en Figma
- con otra cantidad de cuotas sigue al calendario, no al texto de Figma
**vencimientoDePension**

- con la última cuota el 15, la dice como en Figma
- si todas vencen el último día del mes, no hay excepción
- una última cuota en otro mes y día sigue al calendario
- sin cuotas no hay vencimiento que mostrar
**anioPorDefecto**

- abre con el año que pide la dirección, si existe
- si no pide uno (o no existe), el PLANIFICADO más reciente
- sin PLANIFICADO, el ACTIVO
- con todos cerrados, el más reciente; sin años, ninguno
**valores del formulario**

- un tarifario guardado se muestra tal cual, como texto
- una propuesta sin datos del año anterior deja los campos vacíos
- aNumero devuelve null mientras lo escrito no sea un número
- el total no arrastra errores de coma flotante

## Asistencia (registro, justificaciones, reportes)

55 pruebas de backend y 15 de frontend.

### Backend

#### Asistencia · integración (16)

`backend/src/test/java/com/galiledu/asistencia/AsistenciaIT.java`

- La tutora ve su sección
- Sin registrar todos están sin marcar y se puede editar
- Registra la asistencia con la hora de la tardanza
- No guarda si falta marcar a un estudiante
- La tardanza exige la hora de llegada
- Rechaza estudiantes de otra sección
- Solo se registra la asistencia de hoy
- Un sábado no es día de clases
- Entre bimestres son vacaciones
- Corrige el mismo día y deja historial
- Una corrección con una versión vieja se rechaza
- Registrar dos veces sin versión se rechaza
- Una corrección sin cambios no se guarda
- Al día siguiente ya no se puede corregir
- Una sección ajena no se ve
- Solo el tutor usa asistencia

#### Justificación (asistencia) · integración (10)

`backend/src/test/java/com/galiledu/asistencia/JustificacionIT.java`

- Justifica una falta con motivo detalle y sustento
- Una tardanza se justifica sin sustento
- Un presente no se justifica
- El motivo es obligatorio y otro pide detalle
- El sustento debe ser pdf o imagen de hasta5 mb
- No se justifica dos veces
- Se justifica un día pasado
- Una falta justificada ya no se corrige
- Un día sin registro no tiene nada que justificar
- Otro rol no justifica

#### Reporte asistencia · integración (7)

`backend/src/test/java/com/galiledu/asistencia/ReporteAsistenciaIT.java`

- Consolida el mes por apellidos con las justificadas aparte
- Por período cuenta todo el bimestre
- Un mes sin registros sale en cero
- Sin parametros es el mes actual
- Un mes fuera del año se rechaza
- Exporta el reporte en pdf
- Una sección ajena no se ve

#### Rango reporte · unitaria (5)

`backend/src/test/java/com/galiledu/asistencia/aplicacion/RangoReporteTests.java`

- Sin parametros es el mes de hoy
- El primer y último mes se recortan al año escolar
- Un período usa sus fechas
- Un mes o período ajeno al año se rechaza
- Solo se ofrecen los meses y períodos que ya empezaron

#### Regla día escolar · unitaria (4)

`backend/src/test/java/com/galiledu/asistencia/aplicacion/ReglaDiaEscolarTests.java`

- Un martes de bimestre es día escolar
- El primer y último día del período cuentan
- Un año que no está activo no tiene días escolares
- Fuera del año entre bimestres y sábado no son días escolares

#### Justificación (dominio) · unitaria (6)

`backend/src/test/java/com/galiledu/asistencia/dominio/JustificacionTests.java`

- Una falta se justifica con motivo y detalle limpio
- Un detalle en blanco queda vacio
- Un presente no se justifica
- Sin motivo o con otro sin detalle no se justifica
- El detalle tiene como maximo500 caracteres
- El adjunto guarda nombre tipo y tamano

#### Registro asistencia · unitaria (5)

`backend/src/test/java/com/galiledu/asistencia/dominio/RegistroAsistenciaTests.java`

- La tardanza exige hora y los demas estados la descartan
- Sin estado no se puede marcar
- Corregir solo registra los cambios y quien los hizo
- Cambiar solo la hora de la tardanza también es una corrección
- Sin cambios no se marca como corregido

#### Archivo reporte asistencia · unitaria (2)

`backend/src/test/java/com/galiledu/asistencia/infraestructura/ArchivoReporteAsistenciaTests.java`

- Incluye la sección el rango las filas y los totales
- Una sección grande continua en otra página con el total al final

### Frontend

#### Asistencia sección (8)

`frontend/src/features/asistencia/components/__tests__/asistencia-seccion.test.tsx`

- AS-01 a AS-04: avisa a quién falta marcar, exige la hora de la tardanza y guarda
- "Marcar todos como presentes" respeta lo ya marcado
- AS-05 y AS-06: corrige el mismo día y confirma solo los cambios
- un día que no es escolar se ve sin poder marcar
- sin sección a cargo lo dice
- justifica una falta con motivo y detalle desde la columna Justificación
- "Otro" pide el detalle y el sustento solo acepta PDF o imagen
- el botón de fecha abre el calendario y consulta otro día

#### Reporte asistencia (2)

`frontend/src/features/asistencia/components/__tests__/reporte-asistencia.test.tsx`

- muestra el mes actual por apellidos con las justificadas aparte
- "Por bimestre" pide el periodo más reciente y el selector cambia el mes

#### Asistencia (5)

`frontend/src/features/asistencia/lib/asistencia.test.ts`

- el botón de fecha dice el día como en Figma
- "hoy" es la fecha de Lima aunque en UTC ya sea el día siguiente
- cuenta presentes, tardanzas, faltas y sin marcar del borrador
- solo cuenta como cambio lo que difiere de lo guardado
- una tardanza sin hora no se puede guardar

## Académico (calificaciones, catálogo, ambientes)

327 pruebas de backend y 134 de frontend.

### Backend

#### Ambiente concurrencia · integración (8)

`backend/src/test/java/com/galiledu/academico/AmbienteConcurrenciaIT.java`

- Dos aulas al mismo tiempo dan201 con codigos distintos y consecutivos
- Dos espacios especiales al mismo tiempo dan201 con codigos distintos y consecutivos
- Un aula y un espacio al mismo tiempo no se estorban
- Dos con el mismo nombre y tipo dan una201 y una409
- Dos con el mismo nombre y distinto tipo también dan una201 y una409
- Dos con el mismo nombre aunque cambien mayúsculas y espacios dan una201 y una409
- Dos renombres al mismo nombre al mismo tiempo dan una200 y una409
- Una alta y un renombre al mismo nombre al mismo tiempo dan un ganador y un perdedor

#### Ambiente crear · integración (10)

`backend/src/test/java/com/galiledu/academico/AmbienteCrearIT.java`

- Crea un aula con el código generado y los datos normalizados
- El alta queda confirmada y aparec en el listado y en las tarjetas
- El código que envie el cliente se ignora porque lo genera el sistema
- Cada tipo lleva su propio prefijo y correlativo
- Un espacio especial admite más de45 y un aula regular exactamente45
- Un nombre repetido da409 sin distinguir mayúsculas ni espacios y no crea nada
- Un aula de46 da400 con el texto de figma y no crea nada
- Los datos invalidos dan400 con su mensaje y no crean nada
- Sin sesión da401 y no crea nada
- Otros roles reciben403 y no crean nada

#### Ambiente (académico) · integración (22)

`backend/src/test/java/com/galiledu/academico/AmbienteIT.java`

- Sin sesión da401
- Otros roles reciben403
- El resumen trae4 tarjetas con solo los activos y el total de registrados
- El resumen no cambia con los filtros de la lista
- Sin filtros lista todos con su código generado de solo lectura
- Filtra por tipo
- Filtra por estado
- La búsqueda es por nombre sin distinguir mayúsculas y se combina con los otros filtros
- Un tipo o un estado desconocido da400
- Edita nombre aforo y recursos y no cambia el código ni el tipo
- Un espacio especial puede superar45
- Editar con nombre de otro ambiente da409 sin distinguir mayúsculas ni espacios
- Un aula de46 da400 con el texto de figma
- Los datos invalidos dan400 con su mensaje
- Un dato inválido no deja nada cambiado
- Bajar el aforo por debajo de la matrícula da409 y hasta la matrícula se permite
- Editar un ambiente que no existe da404
- Desactivar y reactivar son idempotentes y se permiten con el ambiente en uso
- Un ambiente desactivado sigue en la lista y sale de las tarjetas activas
- Desactivar o reactivar un ambiente que no existe da404
- La base rechaza un aula de46 aunque no pase la api
- La base rechaza un aforo de cero aunque no pase la api

#### Ambiente mapeo · integración (5)

`backend/src/test/java/com/galiledu/academico/AmbienteMapeoIT.java`

- Guarda y lee los recursos como arreglo de postgre sql
- Sin recursos guarda un arreglo vacio no nulo
- Lee los ambientes de los datos de prueba
- Editar y guardar actualiza el arreglo y no toca código ni tipo
- Llena el id y las fechas al guardar

#### Ambiente repositorio · integración (12)

`backend/src/test/java/com/galiledu/academico/AmbienteRepositorioIT.java`

- Lista todos los ambientes activos e inactivos por nombre
- El nombre se compara sin distinguir mayúsculas
- El nombre repetido incluye los ambientes inactivos
- Con tildes solo se igualan las mayúsculas que la base sabe convertir
- Ignorar al propio ambiente deja editar sin cambiar el nombre
- La base no impide dos ambientes con el mismo nombre
- Los codigos del prefijo a no incluyen los que empiezan con otras letras
- Con los codigos de la semilla el siguiente de aula es a206 y el de espacio especial e001
- Un código nuevo entra en la siguiente consulta de la misma transacción
- La base rechaza un código repetido con la restricción del código
- Guardar no escribe en la base hasta el flush
- Memoria transacción y confirmado

#### Archivo reporte calificaciones · unitaria (5)

`backend/src/test/java/com/galiledu/academico/ArchivoReporteCalificacionesTests.java`

- Genera una tabla legible con competencias promedios y tildes
- Un promedio con registros faltantes muestra el aviso completo sin encimarse
- Página estudiantes y columnas sin omitir notas ni reducir la letra
- Conserva niveles cualitativos y notas pendientes
- Exporta sin promedios y conserva el estado vacio

#### Calificaciones · integración (19)

`backend/src/test/java/com/galiledu/academico/CalificacionesIT.java`

- Lista solo los cursos asignados al docente
- Registra un item con la competencia configurada para algebra
- Rechaza un item cuando la competencia ya tiene cien por ciento
- Editar no duplica el peso del propio item
- Registra más items sin peso en los años que no son ponderados
- Edita un item simple sin conservar un porcentaje sin uso
- Exige un peso válido cuando el año es ponderado
- La base permite peso nulo y conserva el rango de los porcentajes
- El promedio simple y el pdf funcionan con items compartidos sin peso
- Consolida los items seleccionados con promedio opcional
- Consulta el item las notas y el reporte
- Consolida varias competencias y exporta los mismos promedios
- Varias competencias sin promedio muestran solo los items
- Rechaza listas invalidas y datos ajenos en el consolidado multiple
- Exporta el mismo reporte en csv
- Un rol administrativo no puede usar el flujo del docente
- Descarga el consolidado como pdf con los mismos resultados
- El pdf válida el rol y las competencias antes de generarse
- La base impide calificar una matrícula de otra sección

#### Consultas ambientes · integración (18)

`backend/src/test/java/com/galiledu/academico/ConsultasAmbientesIT.java`

- Un aula sin secciones no tiene sección ocupada
- Un ambiente que no existe no tiene sección ocupada
- Una sección sin aula no cuenta para ningún ambiente
- Una sección sin matrículas cuenta cero y es el resultado
- Retirado y anulada no cuentan
- Una sección solo con retirados y anulados cuenta cero
- Todos los demas estados ocupan vacante
- Las matrículas de otra sección no se mezclan
- Una sección inactiva no cuenta
- Con una inactiva muy llena gana la activa aunque tenga menos
- Una sección de un año cerrado no cuenta
- Con un año cerrado muy lleno gana la sección del año vigente
- Un año planificado y uno activo si cuentan
- Con varias secciones devuelve la de más matriculados y no la suma
- Un empate se resuelve siempre igual por año grado y nombre
- Las filas de la prueba solo las ve la transacción que las escribio
- Con los datos de prueba el aula a101 es de la1 a secundaria
- Un aula inactiva sin secciones de los datos de prueba no tiene sección ocupada

#### Datos prueba docente · integración (2)

`backend/src/test/java/com/galiledu/academico/DatosPruebaDocenteIT.java`

- María puede calificar a tres alumnos en tres cursos
- Volver a cargar la demo no duplica datos ni sobrescribe notas o items

#### Escala calificación · integración (8)

`backend/src/test/java/com/galiledu/academico/EscalaCalificacionIT.java`

- La vigente trae sus niveles de mayor a menor
- Una versión futura queda programada y aparece primero en la lista
- Una versión desde hoy pasa a ser la vigente
- Rechaza fechas pasadas y repetidas
- Rechaza rangos con vacios o cruces
- Válida notas y fecha
- Otros roles reciben403
- Otros roles reciben403

#### Esquema académico · integración (8)

`backend/src/test/java/com/galiledu/academico/EsquemaAcademicoIT.java`

- El docente llega a sus cursos secciones competencias y aula
- El plan de1ro de secundaria tiene16 cursos
- No se planifica un curso en un grado donde no se dicta
- Una competencia cualitativa no usa reglas numericas
- El peso de un curso en una competencia va de0 a100
- Los rangos de la tabla de conversión van de0 a20
- Un aula regular admite como maximo45
- El código de un ambiente no se repite

#### Modificación calificación · integración (9)

`backend/src/test/java/com/galiledu/academico/ModificacionCalificacionIT.java`

- Modifica fuera del plazo con motivo y deja historial
- El período cerrado permite modificar pero no registrar en lote
- Exige motivo suficiente y un valor distinto
- Fuera de plazo registra con motivo una nota que faltaba
- No evaluado exige motivo y saca el item del cálculo del estudiante
- El registro en lote no borra un no evaluado
- Dentro del plazo el lote solo registra y una nota guardada se cambia con modificar
- El promedio del período cuenta como cero lo que falta sin letra y con el peso calificado
- Solo el docente modifica

#### Nota competencia · integración (11)

`backend/src/test/java/com/galiledu/academico/NotaCompetenciaIT.java`

- El plan muestra los cursos de cada competencia con su peso
- Guarda los pesos de todos los cursos si suman100
- Rechaza pesos que no suman100 o que olvidan un curso
- Un curso nuevo en una competencia deja los pesos incompletos hasta ajustarlos
- Al quitar una competencia de un curso el que queda solo aporta el100
- El docente no edita los pesos
- La nota de la competencia combina los cursos con su peso
- El resumen del año muestra el aporte final del curso y la nota de la competencia
- En escala de letras cada curso vota con su peso
- Con pesos incompletos no se calcula la nota de la competencia
- El docente solo ve las competencias de su curso

#### Plan curricular · integración (20)

`backend/src/test/java/com/galiledu/academico/PlanCurricularIT.java`

- Resumen cuenta activos de los registrados
- Lista las áreas en orden con sus conteos
- Detalle trae competencias y cursos con sus grados
- Crea área al final y sin descripción
- Rechaza área con nombre repetido sin importar mayúsculas
- Exige nombre del área
- Edita área pero no con el nombre de otra
- Desactiva y reactiva área
- Área inexistente da404
- Agrega competencia al final del área
- El nombre de la competencia no se repite en el área
- No agrega competencias ni cursos a un área inactiva
- Edita y desactiva competencia
- Crea curso con sus grados en orden del catálogo
- Válida nombre y grados del curso
- No quita un grado en el que el curso ya está planificado
- Desactiva y reactiva curso
- Otros roles reciben403
- Otros roles reciben403
- Sin sesión da401

#### Plan evaluación · integración (18)

`backend/src/test/java/com/galiledu/academico/PlanEvaluacionIT.java`

- El plan de1ro trae sus tarjetas y cursos
- Un plan vacio no tiene cursos
- El detalle trae las competencias del área y cuales evalua
- Los cursos disponibles excluyen los que ya están en el plan
- Agrega un curso con sus competencias
- Rechaza cursos repetidos de otra área o que no se dictan en el grado
- Configura horas y competencias
- Agrega una competencia a algebra sin eliminar la que ya usa un item
- No quita una competencia que ya tiene items de evaluación
- Las horas no superan los bloques de la estructura
- Quita un curso del plan
- Un año cerrado no se modifica
- Asigna docentes por sección
- Solo se asignan docentes activos a secciones del grado
- Copia el plan a un grado vacio sin docentes
- Solo copia a un plan vacio y salta cursos inactivos
- El coordinador también gestiona el plan
- Otros roles reciben403

#### Reporte competencias · unitaria (9)

`backend/src/test/java/com/galiledu/academico/ReporteCompetenciasTests.java`

- Aplica la regla anual por competencia y al general sin duplicar items
- Aplica las reglas cualitativas anuales
- Respeta los items seleccionados y normaliza sus pesos
- Cuenta la nota cero y también como cero lo que falta registrar
- Oculta todos los promedios cuando se desactiva el interruptor
- Los resultados no ponderados admiten asociaciones sin peso
- Admite una sola competencia sin repetir el promedio
- Rechaza competencias ajenas al curso
- Rechaza items ajenos a las competencias seleccionadas

#### Reportes y notas apoderado · integración (12)

`backend/src/test/java/com/galiledu/academico/ReportesYNotasApoderadoIT.java`

- El reporte del grado resume la nota oficial por sección y grado
- Con un curso muestra lo que ese curso aporta
- Sin período usa la nota final y se descarga en pdf
- El curso debe ser del grado
- El coordinador ve la nota de cada estudiante de una sección y su libreta
- Solo el coordinador ve el detalle de una sección
- Solo el coordinador ve el detalle de una sección
- El reporte institucional cuenta cada nota de competencia una vez
- Permisos de los reportes
- Permisos de los reportes
- El apoderado ve las notas de su hijo por curso y competencia
- El apoderado no ve a estudiantes que no son suyos

#### Servicio ambientes crear · integración (16)

`backend/src/test/java/com/galiledu/academico/ServicioAmbientesCrearIT.java`

- Crea un aula con el siguiente código y los datos normalizados
- El alta queda confirmada en la base
- Cada tipo lleva su propio prefijo y correlativo
- Un espacio especial admite más de45 y un aula regular solo hasta45
- El código no depende de lo que escriba el usuario
- Rechaza un nombre repetido sin distinguir mayúsculas ni espacios sobrantes
- El nombre repetido incluye los inactivos y los de otro tipo
- Un nombre repetido no se reintenta
- También rechaza el nombre de un ambiente creado antes
- Los datos invalidos dan400 sin abrir transacción ni crear nada
- Si otra petición se lleva el código reintenta con una transacción nueva y obtiene el siguiente
- También reintenta con un espacio especial
- Con tres choques seguidos da409 sin crear nada
- Con dos choques y un tercero limpio alcanza el tercer intento
- Otra violación de la base da409 sin reintentar
- Una excepción que no es de la base no se traga

#### Servicio ambientes · integración (27)

`backend/src/test/java/com/galiledu/academico/ServicioAmbientesIT.java`

- El resumen cuenta solo los activos y el total incluye los inactivos
- Un ambiente nuevo suma a su tarjeta y uno inactivo solo al total
- El resumen no depende de los filtros de la lista
- Sin filtros lista todos por nombre con su código de solo lectura
- Filtra por tipo y por estado
- La búsqueda es por nombre sin distinguir mayúsculas ni espacios sobrantes
- La búsqueda no mira el código ni los recursos
- La búsqueda no iguala tildes distintas
- Una búsqueda en blanco no filtra
- Los filtros se combinan
- Actualiza nombre aforo y recursos y no toca código tipo ni estado
- Puede conservar su propio nombre aunque cambie la capitalización
- Un espacio especial puede superar45
- Rechaza el nombre de otro ambiente sin distinguir mayúsculas ni espacios
- Los datos invalidos dan400 con el mensaje del dominio
- Un dato inválido no deja nada modificado ni siquiera lo que si era válido
- Un ambiente que no existe es404
- No baja el aforo por debajo de la matrícula de una sección que usa el aula
- Permite bajar el aforo hasta exactamente la matrícula
- Subir el aforo no consulta la matrícula
- Retirados y anulados no impiden bajar el aforo
- Una sección inactiva no impide bajar el aforo
- Un aula sin secciones baja el aforo libremente
- Desactivar se permite aunque el ambiente este en uso y reactivar lo devuelve
- Desactivar y reactivar son idempotentes
- Desactivar no cambia los demas datos
- Un ambiente desactivado sigue en la lista y sal de las tarjetas activas

#### Servicio calificaciones · unitaria (5)

`backend/src/test/java/com/galiledu/academico/ServicioCalificacionesTests.java`

- No permite modificar un item fuera del plazo de notas
- Guarda una nota vigesimal del estudiante de la sección
- La escala vigesimal rechaza un nivel cualitativo
- Calcula el resultado ponderado de una competencia
- Oculta cursos que no pertenecen al docente

#### Ambiente (dominio) · unitaria (36)

`backend/src/test/java/com/galiledu/academico/dominio/AmbienteTests.java`

- Crea un ambiente activo con los datos normalizados
- Un espacio especial admite más de45
- Un aula regular admite exactamente45
- Acepta un aforo de uno
- Sin tipo o sin código lo rechaza
- Rechaza un nombre vacio o solo espacios
- El nombre admite hasta80 caracteres
- Rechaza un nombre de81 caracteres
- El largo del nombre se mide después de recortar
- El largo se cuenta en puntos de código como la base
- Reduce los espacios repetidos pero conserva mayúsculas y tildes
- Rechaza caracteres de control en el nombre
- Rechaza un aforo que no es mayor que cero
- Rechaza un aula regular de más de45
- Un aforo mayor que la columna se rechaza en vez de truncarse
- Quita vacios y repetidos sin distinguir mayúsculas y conserva el orden y el primero
- Un repetido se detecta aún con espacios internos distintos
- Sin tildes no se normalizan
- Una lista nula o vacia es ningún recurso
- Un recurso admite hasta60 caracteres
- Rechaza un recurso de61 caracteres
- Rechaza un recurso largo aún si otro igual ya estaba en la lista
- Rechaza caracteres de control en un recurso
- La lista original no se modifica ni se comparte
- Los recursos de la entidad no se pueden modificar por fuera
- Edita nombre aforo y recursos y no toca código ni tipo ni estado
- Editar con un aula regular respeta el limite de45 y un espacio especial no
- Sin ningún recurso puede quedar vacio
- Si un dato es inválido no cambia ninguno de los otros
- Editar con nombre vacio no cambia nada
- Desactivar y reactivar cambian el estado
- Desactivar y reactivar son idempotentes
- Desactivar no toca los demas datos
- La clase no ofrece forma de cambiar el código ni el tipo
- Las columnas de código y tipo no se actualizan
- Mapea la tabla de ambientes en el esquema de configuración

#### Cálculo notas · unitaria (21)

`backend/src/test/java/com/galiledu/academico/dominio/CalculoNotasTests.java`

- Combina los cursos con su peso en cada período y promedia los períodos
- Dentro de cada curso usa el peso de los items
- Un curso sin items que cuenten no participa en el período
- Un registro faltante deja la nota pendiente sin número
- Un registro faltante en un período deja la final pendiente
- El aporte final del curso espera que todos sus períodos esten completos
- Con pesos incompletos no hay nota de competencia pero si aportes
- Con promedio simple los cursos pesan igual
- Con último período la final es el último período con nota
- Un período sin items no cuenta para la final
- En la escala cualitativa cada curso vota con su peso
- Con último nivel los cursos también votan con su peso
- Un empate de votos lo gana el nivel mayor
- En la escala cualitativa con pesos incompletos no hay nota
- Un curso sin items no vota
- En la escala cualitativa un nivel faltante deja la nota pendiente
- Sin notas no hay nota ni es pendiente
- Los pesos son completos solo si todos tienen peso y suman100
- El logro es el significado del nivel sin aprobado ni desaprobado
- Usa los rangos de la versión de la tabla y en la cualitativa ya es un nivel
- El nivel se decide con la nota que se muestra

#### Código ambiente · unitaria (15)

`backend/src/test/java/com/galiledu/academico/dominio/CodigoAmbienteTests.java`

- Con la semilla el primer aula es a206
- Con la semilla el primer espacio especial es e001
- Suma uno al mayor con el prefijo
- No depende del orden de la lista
- Sin codigos previos empieza en uno
- Una lista nula equivale a sin codigos
- Con solo codigos de otro prefijo empieza en uno
- Pasa de tres cifras sin cortarse
- Rellena con ceros hasta tres cifras
- Ignora los codigos que no siguen el formato
- Un número demasiado grande no rompe el cálculo
- Sin tipo lo rechaza
- Conserva el valor y se muestra como texto
- Un código con formato inválido no se puede construir
- Un código nulo no se puede construir

#### Resumen notas · unitaria (4)

`backend/src/test/java/com/galiledu/academico/dominio/ResumenNotasTests.java`

- Promedia las notas vigesimales y cuenta quienes tienen ao ad
- En la escala cualitativa no promedia niveles sino que toma el más frecuente
- Sin notas queda pendiente sin porcentaje
- La distribución cuenta cada nivel las pendientes y las que aún no tienen notas

#### Tabla conversión · unitaria (7)

`backend/src/test/java/com/galiledu/academico/dominio/TablaConversionTests.java`

- Acepta la tabla de figma
- C debe empezar en cero
- Ad debe terminar en veinte
- Detecta un vacio entre niveles
- Detecta un cruce entre niveles
- Exige los cuatro niveles sin repetir
- Desde no puede ser mayor que hasta

### Frontend

#### Exportar reporte (1)

`frontend/src/features/calificaciones/api/__tests__/exportar-reporte.test.ts`

- descarga únicamente el PDF con los filtros del reporte generado

#### Calificaciones apoderado (3)

`frontend/src/features/calificaciones/components/__tests__/calificaciones-apoderado.test.tsx`

- muestra el periodo en curso con los cursos, sus ítems y la nota de cada competencia
- con el año terminado muestra el resultado final por niveles de logro, sin «Aprobado»
- sin hijos matriculados lo dice

#### Calificaciones docente (9)

`frontend/src/features/calificaciones/components/__tests__/calificaciones-docente.test.tsx`

**Calificaciones del docente**

- califica por ítem sin filtrar competencias ni duplicar los ítems compartidos
- permite agregar ítems sin pesos con %s y peso anterior %s
- permite editar un ítem de promedio simple sin enviar porcentajes anteriores
- mantiene el bloqueo al 100 % y los indicadores en promedio ponderado
- mantiene el indicador rojo de pesos incompletos solo en promedio ponderado
- agrupa configuración y calificación bajo el primer botón y separa el consolidado
- registra un ítem con criterio, tipo, competencia y peso
- genera el consolidado desde su propio botón sin mostrar los controles de configuración
- permite volver a calificar alumnos después de abrir el consolidado

#### Calificar alumnos (1)

`frontend/src/features/calificaciones/components/__tests__/calificar-alumnos.test.tsx`

- una nota guardada queda bloqueada en la tabla y el lote solo envía las nuevas

#### Diálogo modificar calificación (5)

`frontend/src/features/calificaciones/components/__tests__/dialogo-modificar-calificacion.test.tsx`

- muestra la nota actual y el historial de cambios
- exige un motivo suficiente antes de enviar
- envía la nueva nota con su motivo y cierra
- «No evaluado» no pide nota, pero sí motivo
- dentro del plazo, registrar una nota nueva no pide motivo

#### Reporte calificaciones (8)

`frontend/src/features/calificaciones/components/__tests__/reporte-calificaciones.test.tsx`

**Consolidado con varias competencias**

- selecciona varias competencias, agrupa sus notas y exporta los mismos filtros
- un promedio con registros faltantes va en gris, sin letra, con el peso calificado
- permite omitir los promedios y limpiar la selección
- descarta el reporte anterior al cambiar las competencias y ajusta los ítems disponibles
**Nota de la competencia**

- con un registro faltante la nota oficial queda pendiente, sin número ni letra
- muestra la nota oficial por periodo y la final, con el aporte de cada curso
- avisa cuando los pesos de los cursos no suman 100 %
- «Todo el año» genera el resumen con el aporte final del curso y la nota de la competencia

#### Reportes notas (6)

`frontend/src/features/calificaciones/components/__tests__/reportes-notas.test.tsx`

- el reporte por grado muestra cada competencia por sección y en todo el grado
- al elegir un curso pide su aporte y lo aclara
- el reporte institucional muestra porcentajes por nivel, por grado, sección y área
- el coordinador abre una sección y ve la nota de cada estudiante, con su libreta
- «Todo el año» muestra el nivel de cada periodo y la nota final de cada estudiante
- el coordinador ve los ítems que calificó el docente, sin poder cambiarlos

#### Ambientes (api) (10)

`frontend/src/features/catalogo-academico/api/__tests__/ambientes.test.tsx`

- el resumen trae las 4 tarjetas con el total de registrados
- sin filtros la lista se pide sin parámetros
- los filtros de tipo, estado y búsqueda viajan como parámetros
- una búsqueda en blanco y los filtros vacíos no se envían, y la búsqueda se recorta
- cambiar un filtro vuelve a consultar y no mezcla las listas
- crear envía el tipo, el nombre, el aforo y los recursos, sin código, y recarga las tarjetas y las listas
- editar envía nombre, aforo y recursos a /ambientes/{id} y recarga lo mismo
- desactivar y reactivar llaman a su ruta sin cuerpo y recargan las tarjetas y las listas
- si el backend rechaza el alta (409) no se ejecuta onSuccess, no se recarga nada y el error llega a onError sin notificación
- si el backend rechaza el cambio de aforo (409) no se recarga nada y el error llega a onError sin notificación

#### Ambientes orden (7)

`frontend/src/features/catalogo-academico/components/__tests__/ambientes-orden.test.ts`

- por nombre, sin distinguir mayúsculas ni tildes y con los números en orden natural
- por aforo, como número y no como texto
- por tipo y por estado, con el texto que se ve en la tabla
- por recursos, por el texto de la lista
- un empate se desempata por nombre y no se invierte al cambiar de dirección
- no modifica la lista original
- solo se ordena por las columnas visibles, no por el id ni el código

#### Ambientes (components) (24)

`frontend/src/features/catalogo-academico/components/__tests__/ambientes.test.tsx`

- muestra las 4 tarjetas con los textos de Figma
- las tarjetas no cambian con los filtros de la lista
- las columnas van en el orden de Figma y no hay columna Código
- cada fila muestra nombre, tipo, aforo, recursos como chips y estado
- un ambiente sin recursos muestra una raya
- abre con los activos y ordenados por nombre, y envía estado=ACTIVO
- el pie dice cuántos se muestran de los registrados
- el filtro de estado abre en Activos y permite Todos e Inactivos
- los chips filtran por tipo y el marcado se ve con aria-pressed
- el buscador filtra por nombre y espera a que se haga una pausa para consultar
- el buscador no tiene texto de ayuda: Figma solo dibuja la lupa
- si no hay ambientes activos dice «No hay ambientes disponibles.»
- si los filtros no devuelven resultados dice «No hay ambientes que coincidan con los filtros.»
- cambiar el estado o el tipo y no encontrar nada también es «no coinciden con los filtros»
- muestra un indicador mientras carga
- si la lista falla muestra el error y permite reintentar
- se ordena en el cliente al pulsar un encabezado y se invierte al pulsarlo otra vez
- la fila de un ambiente inactivo se ve atenuada y la de uno activo no
- desactivar pide confirmación y, al confirmar, desactiva y recarga la lista y las tarjetas
- cancelar la confirmación no envía nada
- un ambiente inactivo ofrece Reactivar
- Editar llama a onEditar con el ambiente de la fila
- sin modal de edición, Editar aparece deshabilitado
- el botón «Nuevo ambiente» solo se muestra si hay modal para abrir

#### Dialogos ambientes (23)

`frontend/src/features/catalogo-academico/components/__tests__/dialogos-ambientes.test.tsx`

- muestra el modal de Figma: título, ayuda, tipos y botones, sin campo Código
- los campos de texto abren vacíos y sin texto de ejemplo
- Recursos muestra la ayuda «Separa cada recurso con una coma.» bajo el campo, como la del aforo
- al editar, la ayuda de Recursos también se muestra
- al abrir no hay ningún tipo elegido: no se adivina un tipo que luego no se puede cambiar
- enviar vacío muestra los errores en cada campo y no envía nada
- un aula regular de más de 45 se rechaza con el texto de Figma y un espacio especial no
- el aforo debe ser un número entero mayor que 0
- registra el ambiente: envía los recursos como lista, sin código, avisa y cierra
- el nombre y los recursos se normalizan antes de enviar
- un ambiente sin recursos se envía con la lista vacía
- el 409 de nombre repetido se muestra en el campo Nombre, una sola vez y sin cerrar el modal
- tras el 409, corregir el nombre y volver a enviar registra el ambiente
- otro error del servidor se muestra en una alerta del modal con su mensaje
- si no hay respuesta del servidor, la alerta dice que se vuelva a intentar
- Cancelar cierra sin enviar nada
- editar abre con los datos del ambiente, el tipo bloqueado y el botón «Guardar cambios»
- guardar los cambios envía solo nombre, aforo y recursos, avisa y cierra
- al editar se puede dejar el ambiente sin recursos
- un aula no puede pasar de 45 al editarla, aunque el tipo no se pueda tocar
- un espacio especial sí puede pasar de 45 al editarlo
- el 409 de nombre repetido al editar también se muestra en el campo Nombre
- bajar el aforo por debajo de la matrícula (409) se muestra en el campo Aforo máximo

#### Escala calificación (2)

`frontend/src/features/catalogo-academico/components/__tests__/escala-calificacion.test.tsx`

- muestra la tabla vigente y las otras versiones
- la nueva versión parte de la vigente y valida los rangos antes de enviar

#### Esquemas ambientes (20)

`frontend/src/features/catalogo-academico/components/__tests__/esquemas-ambientes.test.ts`

**parsearRecursos**

- separa por comas y recorta
- quita los vacíos y los repetidos sin distinguir mayúsculas, y queda el primero
- reduce los espacios repetidos de cada recurso
- un texto vacío o solo con comas y espacios son ningún recurso
- ida y vuelta con el texto del campo
**esquema del ambiente**

- acepta los datos del modal de Figma y los normaliza
- la solicitud no lleva código
- el nombre es obligatorio, también con solo espacios
- el nombre admite hasta 80 caracteres, contados ya sin espacios sobrantes
- hay que elegir el tipo
- el aforo es obligatorio y entero
- el aforo debe ser mayor que 0
- el aforo cabe en la columna (hasta 32767)
- un aula regular admite como máximo 45, con el texto de Figma
- un espacio especial puede pasar de 45
- cada recurso admite hasta 60 caracteres
- los recursos son opcionales
**campoDelErrorDelBackend**

- el 409 de nombre repetido es del campo Nombre
- el 409 de aforo bajo la matrícula (RF-111) es del campo Aforo máximo
- cualquier otro error no es de un campo

#### Esquemas (4)

`frontend/src/features/catalogo-academico/components/__tests__/esquemas.test.ts`

- el nombre del área es obligatorio y la descripción vacía se envía como null
- el curso debe dictarse en al menos un grado
- los grados se muestran agrupados por nivel
- los rangos de la escala deben cubrir de 0 a 20 sin vacíos ni cruces

#### Plan curricular (4)

`frontend/src/features/catalogo-academico/components/__tests__/plan-curricular.test.tsx`

- muestra el resumen, las áreas y el detalle de la primera área
- la búsqueda filtra las áreas sin importar las tildes
- agrega un curso con los grados elegidos
- desactivar un curso pide confirmación con el texto de Figma

#### Plan evaluación (7)

`frontend/src/features/catalogo-academico/components/__tests__/plan-evaluacion.test.tsx`

- muestra el plan del año activo con sus tarjetas y cursos
- un grado sin cursos muestra el estado vacío y copia el plan elegido
- agrega un curso con las competencias marcadas
- muestra cuánto aporta cada curso a cada competencia del grado
- ajusta los pesos de una competencia y exige que sumen 100 %
- sin promedio ponderado los cursos aportan por igual y no hay pesos que editar
- en escala de letras los pesos se usan como votos y se pueden ajustar

## Base común (rutas, componentes, utilidades)

9 pruebas de backend y 188 de frontend.

### Backend

#### Integración modulos · integración (5)

`backend/src/test/java/com/galiledu/IntegracionModulosIT.java`

- El usuario estudiante llega a su matrícula sección y estructura horaria
- El grado del estudiante es del catálogo y ya no hay inicial
- Hay una sola estructura principal por nivel y año
- Una estructura no principal si se puede agregar
- El código de matrícula tiene el formato mat año número

#### Manejador errores · integración (3)

`backend/src/test/java/com/galiledu/ManejadorErroresIT.java`

- Una ruta que no existe responde404
- Un id con formato inválido responde400
- Un método no permitido responde405

#### Salud · integración (1)

`backend/src/test/java/com/galiledu/SaludIT.java`

- El chequeo de salud responde up sin autenticación

### Frontend

#### Rutas calificaciones (3)

`frontend/src/app/__tests__/rutas-calificaciones.test.tsx`

- el Personal administrativo abre Calificaciones desde su menú
- el Administrador ya no tiene Calificaciones en su menú
- el Administrador tampoco entra a la ruta del Personal administrativo

#### Rutas catálogo coordinador (3)

`frontend/src/app/__tests__/rutas-catalogo-coordinador.test.tsx`

- el Coordinador abre el plan curricular desde su menú
- el Coordinador abre la escala de calificación desde su menú
- un Docente que abre la ruta del Coordinador es llevado a su inicio

#### Rutas mis datos (3)

`frontend/src/app/__tests__/rutas-mis-datos.test.tsx`

- «Mis datos» conserva el menú lateral del rol activo
- no se entra a «Mis datos» bajo un rol que el usuario no tiene
- el apoderado ve «Familia» en el menú y abre «Datos de la Familia»

#### Rutas pensiones (3)

`frontend/src/app/__tests__/rutas-pensiones.test.tsx`

- /pensiones/configuracion abre el tarifario (P19), no el compromiso de un alumno llamado "configuracion"
- /pensiones/<id de estudiante> sigue abriendo el compromiso de pago (P8)
- /pensiones abre el estado de cuenta (P2), con el botón "Configuración" que lleva al tarifario

#### Rutas reservas (3)

`frontend/src/app/__tests__/rutas-reservas.test.tsx`

- el Coordinador ve la pantalla de reserva de espacios especiales con su historial
- un Docente que abre la ruta del Coordinador es llevado a su inicio y no ve la pantalla
- un Apoderado tampoco entra

#### Ambientes (6)

`frontend/src/app/routes/app/administrativo/__tests__/ambientes.test.tsx`

- muestra el encabezado de Figma: título, migas, descripción y el botón «Nuevo ambiente»
- muestra las tarjetas y la tabla de ambientes
- «Nuevo ambiente» abre el modal, registra, avisa y la lista se actualiza
- «Editar» del menú de la fila abre el modal con los datos y guarda los cambios
- abrir «Editar» en otro ambiente no arrastra los datos del anterior
- «Desactivar» pide confirmación, desactiva y avisa

#### Año escolar detalle (4)

`frontend/src/app/routes/app/administrativo/__tests__/anio-escolar-detalle.test.tsx`

- año ACTIVO: título con su estado, subtítulo, migas, "Cerrar año" y pestañas
- año PLANIFICADO: "Editar" y "Activar año" en la misma fila del título
- año CERRADO: sin acciones
- un año que no existe muestra el aviso y deja las migas para volver

#### Año escolar (1)

`frontend/src/app/routes/app/administrativo/__tests__/anio-escolar.test.tsx`

- muestra el encabezado de CI-01 con el botón "Nuevo año escolar" y la tabla de años

#### Matrícula (2)

`frontend/src/app/routes/app/administrativo/__tests__/matricula.test.tsx`

- sin parámetros muestra el listado de periodos de matrícula con su botón "Nuevo periodo"
- con ?matriculaId= sigue mostrando la tarjeta de P4, sin pedir ningún periodo

#### Matrículas período (2)

`frontend/src/app/routes/app/administrativo/__tests__/matriculas-periodo.test.tsx`

- HU-25: un periodo cerrado con permiso habilita Matrícula extemporánea
- HU-25: sin permiso el botón extemporáneo permanece deshabilitado

#### Pensiones configuración (1)

`frontend/src/app/routes/app/administrativo/__tests__/pensiones-configuracion.test.tsx`

- muestra el título y el subtítulo de Figma, con las migas Pensiones › Configuración › Tarifario y calendario

#### Período matrícula nuevo (1)

`frontend/src/app/routes/app/administrativo/__tests__/periodo-matricula-nuevo.test.tsx`

- muestra el título, el subtítulo de Figma y el camino de vuelta al listado

#### Período matrícula (2)

`frontend/src/app/routes/app/administrativo/__tests__/periodo-matricula.test.tsx`

- ver: el título es el nombre del periodo, con el subtítulo de Figma y su camino de vuelta
- editar: el título lleva el nombre del periodo y la miga vuelve a "Ver"

#### Content layout (4)

`frontend/src/components/layouts/__tests__/content-layout.test.tsx`

- sin migas muestra solo el título como página actual
- las migas de texto siguen sin enlace
- una miga con enlace lleva a su ruta, y la última nunca es enlace
- muestra el título con su etiqueta, el subtítulo y las acciones

#### Dashboard layout cuenta (1)

`frontend/src/components/layouts/__tests__/dashboard-layout-cuenta.test.tsx`

- la tarjeta del usuario lleva a «Mis datos»

#### Persona fields (7)

`frontend/src/components/personas/__tests__/persona-fields.test.tsx`

- nombres y apellidos no aceptan números
- con DNI el documento solo acepta 8 dígitos
- con carné de extranjería acepta hasta 12 letras y números
- celular y teléfono de casa solo aceptan números
- el número de documento espera a que se elija el tipo
- pide si es exalumno por defecto y lo oculta con sinExalumno
- apellido materno y teléfono de casa dicen "(opcional)"; el celular no

#### Persona schema (14)

`frontend/src/components/personas/__tests__/persona-schema.test.ts`

**personaSchema (RF-12)**

- acepta los datos mínimos y convierte vacíos a null
- el DNI debe tener 8 dígitos
- el carné de extranjería acepta hasta 12 caracteres alfanuméricos
- nombres y apellidos solo aceptan letras, espacios, apóstrofo y guion
- normaliza el celular "987 111 222" y valida 9 dígitos
**personaUsuarioSchema (HU-6, criterios CAHU6-*)**

- acepta los datos completos
- exige todos los campos salvo apellido materno, teléfono de casa y exalumno
- muestra el error del DNI aunque falten otros campos
- apellido materno y teléfono de casa son opcionales
- la fecha de nacimiento debe ser pasada
- rechaza el correo inválido %s
- el teléfono de casa solo acepta números
- guarda el correo en minúsculas
- personaAFormulario convierte nulls del backend a valores de formulario

#### Head (1)

`frontend/src/components/seo/__tests__/head.test.tsx`

- should add proper page title and meta description

#### Barra distribución (3)

`frontend/src/components/ui/__tests__/barra-distribucion.test.tsx`

- describe el porcentaje y la cantidad de cada nivel, de AD a C
- sin calificaciones queda solo la pista gris
- porcentajeDe redondea al entero y no divide entre cero

#### Calendario compacto (6)

`frontend/src/components/ui/__tests__/calendario-compacto.test.tsx`

- por defecto el calendario es el de Figma: título grande, celdas de 32 px y relleno de 24 px
- con size="sm" el título es más pequeño y las celdas y el relleno se reducen
- el calendario compacto sigue eligiendo días, meses y años
- DatePicker con size="sm" abre el calendario compacto; sin size, el de Figma
- la sombra del calendario es la negra de Figma (`shadow-card-elevation`), no la blanca
- el calendario desplegable no se limita al ancho del campo

#### Componentes figma (23)

`frontend/src/components/ui/__tests__/componentes-figma.test.tsx`

- Tabs: el clic y las flechas cambian de pestaña y saltan las deshabilitadas
- Accordion: muestra el contenido solo cuando está abierto
- TagField: Enter agrega una etiqueta sin repetir y la X la quita
- Calendar: elegir un día devuelve la fecha en formato YYYY-MM-DD y respeta los límites
- DatePicker: abre el calendario y muestra la fecha elegida
- Rating: se lee como texto y, editable, devuelve la estrella elegida
- Select: la lista de Figma elige la opción y avisa al formulario como el <select> nativo
- Input type="date": usa el calendario de Figma y el formulario recibe la fecha
- ProgressBar: muestra el porcentaje, expone el valor y lo limita entre 0 y 100
- ProgressBar: sin etiqueta visible conserva el nombre accesible
- SelectableTag readOnly: es una pastilla informativa, no un botón, con el relleno claro de Figma
- SelectableTag readOnly ignora "selected": sigue siendo informativa
- SelectableTag sin readOnly sigue siendo un botón que se marca
- Textarea con showCount: el contador sigue al valor de un campo controlado
- Input con startAdornment: muestra el prefijo dentro del campo, sin ensuciar su nombre ni su valor
- Input con startAdornment y endAdornment a la vez deja lugar a los dos lados
- Textarea con showCount: en un campo no controlado cuenta lo que se escribe y respeta maxLength
- Textarea: sin showCount o sin maxLength no muestra contador, y llama a su onChange
- Textarea con react-hook-form: el contador cuenta y el valor llega al formulario
- StatCard: iconColor cambia el color del cuadro del ícono y, sin él, conserva el de siempre
- ScheduleBlock: el bloque asignado solo es un botón con onSelect y la hora libre solo se asigna con onAssign
- ScheduleBlock: con un cruce se marca como advertencia y muestra su mensaje
- CardTitle: por defecto es el título de Figma (h6, regular) y con size="body-1" es Body/Body-1/Medium

#### Notificaciones sobre modal (1)

`frontend/src/components/ui/__tests__/notificaciones-sobre-modal.test.tsx`

- la capa de avisos queda por encima de la de los modales

#### Schedule block claves (1)

`frontend/src/components/ui/__tests__/schedule-block-claves.test.tsx`

- ScheduleBlock: dos cursos con el mismo nombre se dibujan los dos y no repiten la clave

#### Segmented control (2)

`frontend/src/components/ui/__tests__/segmented-control.test.tsx`

- es un grupo de opciones únicas que avisa la elegida
- sin marcar y con error, marca el grupo como inválido

#### Select lista flotante (9)

`frontend/src/components/ui/__tests__/select-lista-flotante.test.tsx`

- sin valor muestra solo la etiqueta (el campo vacío), y elegir una opción llena el campo
- la lista cuelga del body, con posición fija, pegada al campo y sin escalar su ancho
- si no cabe abajo y arriba hay más sitio, se abre hacia arriba
- el alto de la lista nunca pasa del espacio disponible ni de 16 rem
- un clic fuera la cierra y un clic en una opción no cuenta como "fuera"
- el teclado sigue funcionando: flechas para moverse y Enter para elegir
- dentro de un modal: la lista queda fuera del diálogo, elegir no lo cierra y los clics responden
- un Select deshabilitado no abre la lista
- la rueda y el gesto táctil sobre la lista no suben hasta el documento (un modal de Radix los bloquearía)

#### Dialog (1)

`frontend/src/components/ui/dialog/__tests__/dialog.test.tsx`

- should handle basic dialog flow

#### Confirmation dialog (1)

`frontend/src/components/ui/dialog/confirmation-dialog/__tests__/confirmation-dialog.test.tsx`

- should handle confirmation flow

#### Drawer (1)

`frontend/src/components/ui/drawer/__tests__/drawer.test.tsx`

- should handle basic drawer flow

#### Código verificación (4)

`frontend/src/components/ui/form/__tests__/codigo-verificacion.test.tsx`

- escribir avanza de casilla y solo acepta números
- pegar el código completo llena todas las casillas
- Backspace borra y retrocede
- con error pinta las casillas y muestra el mensaje

#### Form (2)

`frontend/src/components/ui/form/__tests__/form.test.tsx`

- should render and submit a basic Form component
- should fail submission if validation fails

#### Input etiqueta flotante (29)

`frontend/src/components/ui/form/__tests__/input-etiqueta-flotante.test.tsx`

- la etiqueta va dentro del campo y sigue nombrando al input
- sin placeholder propio el campo usa uno vacío para saber si está vacío
- un placeholder propio se respeta
- con un placeholder propio la etiqueta se queda arriba y sin estilos de reposo
- sin placeholder propio la etiqueta puede descansar dentro del campo
- con un adorno al inicio la etiqueta se queda arriba para no taparlo
- en reposo la etiqueta no tiene fondo blanco y al enfocar lo recupera
- con type="time" la etiqueta se queda arriba aunque el campo esté vacío
- con type="time" el campo sigue nombrándose con su etiqueta y guarda la hora escrita
- en reposo la etiqueta larga se recorta con puntos suspensivos y al enfocar se ve completa
- la etiqueta no se parte en dos líneas
- sin etiqueta no hay nada flotando ni placeholder agregado
- el texto escrito queda en el campo
- con error la etiqueta se pone roja y el campo queda inválido
- la ayuda va debajo del campo
**campo de fecha**

- tiene la misma etiqueta flotante que los demás campos
- el botón del calendario no cambia de nombre accesible al descansar la etiqueta
- vacío y sin placeholder, la etiqueta descansa dentro y "DD/MM/AAAA" no se ve
- con una fecha elegida la etiqueta sube y se ve la fecha
- sin etiqueta sigue mostrando "DD/MM/AAAA"
- con placeholder propio la etiqueta queda arriba y se ve el placeholder
**Select y Textarea**

- Select sin nada elegido ni placeholder: la etiqueta descansa dentro
- Select con placeholder propio: la etiqueta se queda arriba
- Select con valor elegido: la etiqueta sube
- Textarea con placeholder propio: la etiqueta se queda arriba, sin estilos de reposo
- Textarea sin placeholder: la etiqueta descansa a la altura del relleno y cambia con foco y hover
- Select vaciable con placeholder: la etiqueta queda arriba y "Selecciona" se ve como opción
- Textarea con contador: sigue mostrando "n/máximo" con la etiqueta flotante
- Textarea: la etiqueta flota y el campo conserva su nombre accesible

#### Select opciones deshabilitadas (3)

`frontend/src/components/ui/form/__tests__/select-opciones-deshabilitadas.test.tsx`

- una opción deshabilitada se ve marcada y no se puede elegir
- las demás opciones se siguen pudiendo elegir
- el teclado salta las opciones deshabilitadas

#### Select (2)

`frontend/src/components/ui/form/__tests__/select.test.tsx`

- un Select vaciable permite volver a "Selecciona" después de elegir una opción
- un Select sin "vaciable" no ofrece "Selecciona" como opción

#### Notifications (3)

`frontend/src/components/ui/notifications/__tests__/notifications.test.ts`

- se cierra con la X: primero se desvanece y luego se quita
- desaparece sola: 5 s el éxito y 7 s el error
- no se acumulan: con una cuarta, la más antigua se va

#### Ubigeo (6)

`frontend/src/config/__tests__/ubigeo.test.ts`

**catálogo de ubigeo (INEI)**

- trae los 25 departamentos, 196 provincias y 1893 distritos
- no repite distritos dentro de una provincia ni deja listas vacías
- los departamentos llevan tilde y Callao es un departamento
- las provincias dependen del departamento elegido
- los distritos dependen de la provincia elegida
- solo los nacidos en el Perú usan este catálogo

#### Use debounced value (4)

`frontend/src/hooks/__tests__/use-debounced-value.test.ts`

- al inicio devuelve el valor de inmediato
- un cambio no se ve hasta que pasa la espera
- si el valor sigue cambiando, la espera se reinicia y solo llega el último
- la espera por defecto es de 400 ms

#### Use disclosure (4)

`frontend/src/hooks/__tests__/use-disclosure.test.ts`

- should open the state
- should close the state
- should toggle the state
- should define initial state

#### Api client (4)

`frontend/src/lib/__tests__/api-client.test.ts`

- un 401 de un endpoint con la sesión vigente no manda al login (evita el bucle)
- un 401 con la sesión vencida manda al login y vuelve a la misma pantalla
- acepta cualquier tipo de contenido cuando descarga un archivo
- mantiene JSON como contenido esperado en las consultas normales

#### Auth (1)

`frontend/src/lib/__tests__/auth.test.ts`

- después del login solo se vuelve a pantallas internas de la app

#### Authorization (4)

`frontend/src/lib/__tests__/authorization.test.tsx`

- muestra el recurso si el usuario tiene el rol permitido
- acepta usuarios con varios roles (docente + tutor)
- muestra el fallback si el rol no coincide
- respeta policyCheck

#### Restringir entrada (13)

`frontend/src/utils/__tests__/restringir-entrada.test.ts`

**soloDecimal**

- solo deja números y un punto (la coma no es separador)
- respeta el formato: peso ###.#, estatura #.##
- pone el punto solo cuando sobran enteros
**completarDecimales**

- completa con ceros los decimales que faltan
- sin parte entera antepone el 0
- un valor vacío queda vacío
**soloDigitos**

- quita letras, espacios y símbolos
- respeta el máximo de dígitos
**soloAlfanumerico**

- acepta letras y números, sin espacios ni símbolos
- respeta el máximo de caracteres
**soloLetras**

- quita números y símbolos
- acepta tildes, ñ, espacios, apóstrofo y guion
- respeta el máximo de caracteres
