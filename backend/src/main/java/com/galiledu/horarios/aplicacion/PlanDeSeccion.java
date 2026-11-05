package com.galiledu.horarios.aplicacion;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Los cursos del plan de evaluación de Académico que dicta una sección, con su docente. Es de solo lectura. Hoy lo
 * resuelve {@code PlanDeSeccionSql}.
 */
public interface PlanDeSeccion {
	/** Un curso del plan, con las horas semanales del plan y el docente de la sección (null si aún no tiene). */
	record CursoDelPlan(UUID planCursoId, String nombre, int horasSemanales, UUID docenteId, String docenteNombre) {
	}

	/** Los cursos del plan del grado y año de la sección, por orden alfabético. */
	List<CursoDelPlan> cursosDelPlan(UUID seccionId);

	/** Los cursos indicados (por id del plan) con el docente que tienen en esa sección. Ignora los ids desconocidos. */
	List<CursoDelPlan> cursosPorId(UUID seccionId, Collection<UUID> planCursoIds);
}
