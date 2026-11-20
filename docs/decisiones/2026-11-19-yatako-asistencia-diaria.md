# Asistencia diaria del tutor (HU-65)

- **Autor:** Yatako2304 (con asistencia de IA)
- **Fecha:** 2026-11-19
- **HU / tarea:** HU-65 (RF-143)
- **Estado:** terminado

## Qué hice

Agregué el módulo `asistencia` (esquema, backend y frontend). El tutor o cotutor registra la asistencia del día de su sección y la corrige solo ese mismo día, siguiendo las pantallas AS-01 a AS-06 de Figma. Ficha: [asistencia.md](../modulos/asistencia.md).

## Archivos principales

- `backend/src/main/resources/db/migration/V25__asistencia.sql`
- `backend/src/main/java/com/galiledu/asistencia/` (`ServicioAsistencia`, `RegistroAsistencia`, `ReglaDiaEscolar`, `AsistenciaController`)
- `frontend/src/features/asistencia/` y `frontend/src/components/ui/segmented-control/`

## Decisiones y por qué

Las acordamos en el equipo antes de empezar (19/11), porque ni la documentación ni Figma las definían:

- **Decisión:** la corrección no pide motivo. Se guarda quién corrigió, cuándo, y el estado y la hora de antes y después.
  **Por qué:** el modal "Confirmar corrección de asistencia" de Figma no tiene ese campo, y la HU y el RF-143 no lo piden. El aviso de AS-06 decía "con la hora y el motivo": se dejó "con la hora".
  **Alternativas descartadas:** agregar un campo de motivo que no está en Figma.
- **Decisión:** la hora de llegada es obligatoria en la tardanza y no se guarda en los demás estados. La BD también lo exige (`ck_asistencia_estudiante_hora`).
- **Decisión:** un día es escolar si el año está ACTIVO, la fecha cae dentro del año y de un periodo académico, y es día lectivo de la estructura horaria principal del nivel.
  **Por qué:** revisamos `main` y las ramas remotas y no hay calendario de feriados. Es la misma regla de las reservas de Horarios (HU-48), que lo deja como limitación conocida.
  **Alternativas descartadas:** permitir cualquier fecha del año, incluidos los fines de semana.
- **Decisión:** "hoy" sale del `Clock` de la aplicación (America/Lima). Solo se registra y corrige la fecha de hoy; las demás se consultan.
- **Decisión:** se listan las matrículas **MATRICULADO** de la sección, por apellidos. La sección del usuario sale de la misma condición que `VinculosUsuarioSql.tutorDe` (Usuarios): sección ACTIVA del año ACTIVO, como tutor o cotutor.
  **Por qué:** RF-143 da los mismos permisos al tutor y al cotutor. Una matrícula en proceso (reservada, en validación) todavía no asiste a clases.
- **Decisión:** el registro es uno por sección y día (`UNIQUE`), con `version`. Si el tutor y el cotutor guardan a la vez, el segundo recibe 409 y vuelve a cargar.
- **Decisión:** el "Selector de asistencia" de Figma se creó como `SegmentedControl` en `components/ui`, con `role="radiogroup"`, colores por opción y estado de error.
  **Por qué:** Figma lo dibuja en la pantalla y no está en Componentes. La guía pide crear en `components/ui` lo que falta.
- **Decisión:** en corrección, la fila con un cambio sin guardar se resalta y muestra "Modificado", como en AS-06. Después de guardar, "Modificado" queda en las filas que tienen historial.
- **Decisión:** el botón de la fecha todavía no abre un selector, y "Reporte mensual" está deshabilitado ("Disponible pronto"). Llegan con HU-67 y HU-68.

## Pendientes / problemas conocidos

- [ ] HU-67 y HU-68 (ver la ficha).
- [ ] Si un cotutor no tiene el rol Tutor, no puede entrar. La asignación automática del rol es de Usuarios.

## Notas para quien siga

- Las pruebas de integración usan `PruebaIntegracionConfiguracion`: el reloj está fijo en el martes 15/09/2026, en el III Bimestre. Para otro día, usa `reloj.fijarEn(...)`.
- Al corregir, el frontend manda la lista completa de los estudiantes y el backend guarda solo los cambios. Un estudiante matriculado después del registro se agrega sin contar como corrección.
