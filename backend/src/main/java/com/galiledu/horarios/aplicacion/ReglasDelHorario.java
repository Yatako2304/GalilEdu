package com.galiledu.horarios.aplicacion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.horarios.aplicacion.DatosDeSeccion.DatosSeccion;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.PlanDeSeccion.CursoDelPlan;

/**
 * Las reglas de las celdas que guarda el PUT, sin acceso a datos (HU-45). Las de forma responden 400 y las que
 * dependen del estado del plan, 409. Se aplican en este orden y se informa la primera que falla:
 * <ol>
 * <li>no hay dos celdas en el mismo (día, bloque);</li>
 * <li>el día es lectivo y el bloque existe en la jornada de la estructura de la sección;</li>
 * <li>el curso es del plan del mismo año y grado que la sección;</li>
 * <li>el ambiente existe y está activo;</li>
 * <li>el curso tiene docente en la sección (409);</li>
 * <li>los bloques del curso no superan sus horas semanales, con 1 hora = 1 bloque (409).</li>
 * </ol>
 * Los cruces de docente y de ambiente no son de aquí: los detecta {@link ValidadorCruces}.
 */
@Component
public class ReglasDelHorario {
	/**
	 * @param plan
	 *            los cursos del plan del grado y año de la sección, por id, con su docente
	 * @param ambientesActivos
	 *            los ids de los ambientes activos
	 */
	public void validar(DatosSeccion seccion, JornadaEstructura jornada, List<CeldaHorario> celdas,
			Map<UUID, CursoDelPlan> plan, Set<UUID> ambientesActivos) {
		Set<String> posiciones = new HashSet<>();
		for (CeldaHorario celda : celdas) {
			if (!posiciones.add(celda.dia() + ":" + celda.numeroBloque())) {
				throw ExcepcionNegocio.invalido(
						"El horario tiene dos cursos en el mismo bloque: " + celda.dia() + ", bloque "
								+ celda.numeroBloque() + ".");
			}
		}

		Set<Integer> bloquesDeLaJornada = new HashSet<>();
		jornada.bloques().forEach(b -> bloquesDeLaJornada.add(b.numero()));
		for (CeldaHorario celda : celdas) {
			if (!jornada.dias().contains(celda.dia())) {
				throw ExcepcionNegocio.invalido(
						"El día " + celda.dia() + " no es lectivo en la estructura horaria de la sección.");
			}
			// Los recesos no tienen número de bloque: un número que no es de la jornada tampoco puede ser un receso.
			if (!bloquesDeLaJornada.contains(celda.numeroBloque())) {
				throw ExcepcionNegocio.invalido(
						"El bloque " + celda.numeroBloque() + " no existe en la jornada de la sección.");
			}
		}

		for (CeldaHorario celda : celdas) {
			if (!plan.containsKey(celda.planCursoId())) {
				throw ExcepcionNegocio.invalido(
						"El curso elegido no pertenece al plan de " + seccion.gradoNombre() + " de este año.");
			}
		}

		for (CeldaHorario celda : celdas) {
			if (!ambientesActivos.contains(celda.ambienteId())) {
				throw ExcepcionNegocio.invalido("El ambiente elegido no existe o no está activo.");
			}
		}

		for (CeldaHorario celda : celdas) {
			CursoDelPlan curso = plan.get(celda.planCursoId());
			if (curso.docenteId() == null) {
				throw ExcepcionNegocio.conflicto("Asigna un docente a " + curso.nombre()
						+ " en Plan de evaluación antes de ubicarlo en el horario.");
			}
		}

		Map<UUID, Integer> bloquesPorCurso = new HashMap<>();
		for (CeldaHorario celda : celdas) {
			CursoDelPlan curso = plan.get(celda.planCursoId());
			int total = bloquesPorCurso.merge(celda.planCursoId(), 1, Integer::sum);
			if (total > curso.horasSemanales()) {
				throw ExcepcionNegocio.conflicto(
						curso.nombre() + " ya tiene sus " + curso.horasSemanales() + " horas semanales asignadas.");
			}
		}
	}
}
