package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.DtosHorarios.JornadaDelDia;

/**
 * Contratos JSON del horario de clases que consulta el apoderado (HU-51). Son de solo lectura: no traen versión,
 * cruces ni ids internos del plan. Las horas viajan como texto "HH:mm" y los días como LUN…SAB.
 */
public final class DtosHorarioApoderado {
	private DtosHorarioApoderado() {
	}

	/** Un año en que el hijo está matriculado, con su grado y sección. */
	public record MatriculaDelHijo(int anio, String grado, String nivel, String seccion) {
	}

	/**
	 * GET /horarios/apoderado/estudiantes: un hijo matriculado, con sus matrículas del año más reciente al más antiguo.
	 */
	public record HijoConHorario(UUID estudianteId, String nombreCompleto, List<MatriculaDelHijo> matriculas) {
	}

	/** El tutor de la sección. Celular y correo pueden faltar. */
	public record TutorDelHorario(String nombre, String celular, String correo) {
	}

	/** Un bloque con clase: curso, docente y aula (nombres). El docente es null si el curso no lo tiene asignado. */
	public record ClaseDelHorario(DiaSemana dia, int numeroBloque, String curso, String docente, String aula) {
	}

	/**
	 * GET /horarios/apoderado/estudiantes/{id}/horario. {@code disponible} es false si el Coordinador aún no ha
	 * guardado el horario de la sección: entonces {@code jornada} es null y {@code clases} está vacía.
	 */
	public record HorarioDelEstudiante(UUID estudianteId, String nombreCompleto, int anio, String grado,
			String nivel, String seccion, TutorDelHorario tutor, boolean disponible, JornadaDelDia jornada,
			List<ClaseDelHorario> clases) {
	}
}
