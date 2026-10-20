# Catálogo académico: plan curricular, escala de calificación y plan de evaluación

- **Autor:** Wil (Galleta159). Entrada redactada por Yatako2304 (con asistencia de IA) a partir de su código, sus PR y la ficha del módulo
- **Fecha:** 2026-10-20
- **HU / tarea:** HU-52, HU-53, HU-54, HU-55, HU-56 (PR #14 a #17)
- **Estado:** terminado (falta HU-43, de la iteración 3)

## Qué hice

Backend y frontend del Catálogo académico sobre las tablas de V6. El Personal administrativo:
- mantiene áreas, competencias y cursos (con sus grados);
- versiona la tabla de conversión de vigesimal a AD/A/B/C;
- arma el plan de evaluación de cada grado y año: cursos y horas, competencias con escala y regla, y docente por sección.

El detalle de cada regla está en la ficha [catalogo-academico.md](../modulos/catalogo-academico.md).

## Archivos principales

- `backend/src/main/java/com/galiledu/academico/aplicacion/` (`ServicioPlanCurricular`, `ServicioEscalaCalificacion`, `ServicioPlanEvaluacion`)
- `backend/src/main/java/com/galiledu/academico/dominio/TablaConversion.java` (validación de rangos)
- `backend/src/main/java/com/galiledu/academico/infraestructura/ConsultasCatalogo.java` (lecturas de configuración y usuarios)
- `frontend/src/features/catalogo-academico/`

## Decisiones y por qué

- **Decisión:** las horas semanales de un grado no pueden superar los **bloques semanales de la estructura horaria principal de su nivel** (bloques por día × días lectivos). Si el año todavía no tiene estructura horaria, no hay límite.
  **Por qué:** Figma muestra "Disponibles: 45" en el modal del curso. Así el plan de evaluación nunca pide más horas de las que Horarios podrá repartir.
  **Afecta a:** Configuración (la estructura horaria debe existir para que haya límite) y Horarios (iteración 2).
- **Decisión:** las versiones de la tabla de conversión **no se editan**. Cada cambio es una versión nueva con su fecha "Vigente desde", que no puede ser anterior a hoy. El estado (vigente, programada o anterior) se **calcula con la fecha** y no se guarda.
  **Por qué:** una nota ya convertida no debe cambiar si alguien corrige la tabla después. Calcular el estado evita un proceso que lo actualice cada día.
  **Afecta a:** Calificaciones (iteración 3), que debe convertir cada nota con la versión **vigente en la fecha de esa nota**, no con la de hoy.
- **Decisión:** desactivar un área, competencia o curso **no** desactiva lo que depende de él, y los planes que ya lo usaban lo conservan. Lo inactivo solo deja de ofrecerse para planes nuevos.
  **Por qué:** Figma: "Ya no podrá agregarse a nuevos planes de evaluación". Quitarlo de planes en curso alteraría notas ya registradas.
- **Decisión:** "Copiar plan" solo copia a un grado **sin cursos**, solo copia cursos y competencias activos, y **no copia los docentes**.
  **Por qué:** Figma: "Los docentes se asignan después". Copiar sobre un plan con cursos mezclaría dos configuraciones.
- **Decisión:** un año escolar **CERRADO** es de solo lectura en el plan de evaluación (responde 409).
  **Por qué:** el plan de un año cerrado es la base de sus notas finales.
- **Decisión:** el docente de una sección debe ser un usuario **activo con rol DOCENTE**. Tener solo el rol Tutor no basta.
  **Por qué:** el plan asigna quién **dicta** el curso; la tutoría ya está en `configuracion.seccion.tutor_id`.
- **Decisión:** cuatro diferencias con Figma en la interfaz:
  - "Otras versiones" bajo la tabla vigente;
  - "Agregación de ítems" también en el modal de configurar;
  - estado "Configurado / Sin configurar";
  - "Copiar plan" solo con el grado vacío.

  **Por qué:** CI-23 no muestra dónde ver la versión programada ni el historial que menciona su modal, y la BD exige `regla_nota`, así que el modal debe pedirla. Cada caso está explicado en la ficha.

## Pendientes / problemas conocidos

- [ ] HU-43: ambientes físicos (iteración 3). Al bajar el aforo de un aula no puede quedar por debajo de la matrícula de sus secciones (RF-111).
- [ ] Calificaciones (iteración 3) debe usar la versión de la tabla de conversión vigente en la fecha de cada nota.

## Notas para quien siga

- `ConsultasCatalogo` lee con SQL tablas de Configuración (años, secciones, estructura horaria) y de Seguridad (docentes), porque esos módulos todavía no exponen un servicio. Cuando lo tengan, conviene reemplazar esas lecturas por sus servicios.
- La validación de rangos de la tabla de conversión está tanto en el backend (`TablaConversion`) como en el frontend (`esquemas.ts`). Si cambia la regla, hay que cambiarla en los dos.
