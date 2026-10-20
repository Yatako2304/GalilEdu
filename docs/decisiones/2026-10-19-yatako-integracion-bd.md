# Integración de la BD entre módulos (V7)

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-10-19
- **HU / tarea:** BD de la Iteración 1 (V4 a V7)
- **Estado:** terminado
- **Reemplaza en parte a:** `2026-10-19-yatako-usuarios-y-acceso.md`, en el punto "el estudiante no tiene usuario"

## Qué hice

Revisé las tablas de todos los módulos contra Figma y corregí lo que estaba duplicado o desconectado. La migración `V7__ajustes_integracion.sql` hace cinco cosas:
- enlaza el grado del estudiante con el catálogo de grados;
- separa el estado del estudiante del estado de su matrícula;
- une las estructuras horarias con el nivel;
- pasa el descuento aplicado al estudiante, para que pueda durar varios años;
- fija el formato del código de matrícula.

## Archivos principales

- `backend/src/main/resources/db/migration/V7__ajustes_integracion.sql`
- `backend/src/main/java/com/galiledu/estudiantes/dominio/Estudiante.java` (grado del catálogo y situación calculada)
- `frontend/src/config/catalogos.ts` y `frontend/src/features/estudiantes/components/inscripcion/` (el grado se envía como código)
- `backend/src/test/java/com/galiledu/IntegracionModulosIT.java`

## Decisiones y por qué

- **Decisión:** el colegio tiene **primaria y secundaria** (11 grados). Ya no se inscribe en inicial.
  **Por qué:** es lo que muestra Figma (CI-06: "11 grados"). Antes se podía inscribir a un niño de inicial que después no se podía matricular.
  **Alternativas descartadas:** solo secundaria; agregar inicial al catálogo.
- **Decisión:** el estudiante guarda su grado como código del catálogo (`SEC-1`), no como texto.
  **Por qué:** la inscripción y la matrícula usaban formatos distintos ("1.° grado" frente a "1.° de secundaria").
- **Decisión:** el estudiante solo guarda **ACTIVO / RETIRADO**. "Inscrito / Matriculado" se calcula desde su matrícula del año académico, y el listado y la ficha lo muestran igual que antes.
  **Por qué:** guardar el mismo estado en dos tablas permitía que se desfasaran.
  **Alternativa descartada:** mantener las dos columnas y sincronizarlas desde el servicio.
- **Decisión:** el **estudiante tiene cuenta propia**, que se crea al quedar matriculado; las credenciales van al correo del apoderado. Se une por la persona (`seguridad.usuario.persona_id = estudiantes.estudiante.persona_id`).
  **Por qué:** Figma tiene la página "Estudiante", y HU-50 y HU-61 son para alumnos.
  **Alternativas descartadas:** crear la cuenta al inscribir; crearla a mano.
- **Decisión:** cada estructura horaria indica su **nivel**, y la principal del nivel (`es_principal`) es la que usan sus secciones.
  **Por qué:** Figma tiene EH-PRI-26 y EH-SEC-26, pero nada decía cuál usa cada sección. Las estructuras extra, como "talleres de tarde", se marcan como no principales.
  **Alternativa descartada:** que cada sección elija su estructura.
- **Decisión:** el **descuento aplicado es del estudiante**, con un rango de vigencia que puede durar varios años, y cada cuota rebajada guarda qué descuento la rebajó.
  **Por qué:** el modal "Añadir descuento" permite "3 años, termina en 2029", y el compromiso de pago es de un solo año.
  **Alternativa descartada:** copiar el descuento en cada compromiso anual.
- **Decisión:** el código de matrícula es `MAT-AAAA-NNNN`, correlativo por año, y la sección se reserva al crear la matrícula (es obligatoria).
  **Por qué:** Figma mostraba dos formatos y dos momentos distintos. El código con la sección (`MAT-2027-05A-0142`) quedaría desactualizado si el alumno cambia de sección.

## Pendientes / problemas conocidos

- [ ] Crear la cuenta del estudiante al matricularlo (lo hace el servicio de Matrícula).
- [ ] Contradicciones de Figma que no cambian la BD y quedan para definir al programar: el monto de matrícula (S/ 450 frente a S/ 850) y el día de vencimiento de la pensión (fin de mes frente al día 5).

## Notas para quien siga

- `Estudiante.getSituacion()` usa un `@Formula` que consulta `matricula.matricula`. Se puede filtrar por ella en el listado (`?estado=MATRICULADO`).
- Si en el servidor hay estudiantes de inicial, V7 se detiene con un mensaje claro. Primero hay que corregirles el nivel.
