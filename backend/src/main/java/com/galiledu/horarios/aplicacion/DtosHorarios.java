package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Contratos JSON del horario de una sección (HU-45). Las horas viajan como texto "HH:mm" y los días como LUN…SAB.
 * Son distintos de los resultados de los puertos de lectura ({@link DatosDeSeccion}, {@link PlanDeSeccion}…).
 */
public final class DtosHorarios {
	private DtosHorarios() {
	}

	/** Una persona o un ambiente: su id y su nombre. */
	public record Referencia(UUID id, String nombre) {
	}

	public record SeccionDelHorario(UUID id, String nombre, String gradoNombre, String nivel, UUID anioEscolarId,
			UUID ambienteId, Referencia tutor) {
	}

	public record EstructuraDelHorario(UUID id, String codigo, boolean principal) {
	}

	public record BloqueJornada(int numero, String inicio, String fin) {
	}

	public record RecesoJornada(int despuesDeBloque, String inicio, String fin) {
	}

	/** La jornada de un día; es igual cada día de {@code dias}. */
	public record JornadaDelDia(List<DiaSemana> dias, List<BloqueJornada> bloques, List<RecesoJornada> recesos) {
	}

	/** Un bloque asignado. {@code docente} es null si el curso ya no tiene docente en la sección. */
	public record CeldaDelHorario(DiaSemana dia, int numeroBloque, UUID planCursoId, String curso,
			Referencia docente, Referencia ambiente, List<CruceDetectado> cruces) {
	}

	/**
	 * GET /horarios/secciones/{seccionId}. {@code version} es null si la sección aún no tiene horario guardado;
	 * {@code editable} es false con el año CERRADO.
	 */
	public record HorarioDeSeccion(SeccionDelHorario seccion, Long version, boolean editable,
			EstructuraDelHorario estructura, JornadaDelDia jornada, List<CeldaDelHorario> celdas) {
	}

	/** Un curso del modal "Asignar bloque horario": con las horas del plan y los bloques ya asignados. */
	public record CursoOpcion(UUID planCursoId, String nombre, int horasSemanales, long horasAsignadas,
			Referencia docente) {
	}

	public record AmbienteOpcion(UUID id, String nombre, String tipo, int aforoMaximo) {
	}

	/** GET /horarios/secciones/{seccionId}/opciones. */
	public record OpcionesDeHorario(UUID ambientePorDefectoId, List<CursoOpcion> cursos,
			List<AmbienteOpcion> ambientes) {
	}
}
