package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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

	/** Una celda del PUT. El docente no viaja: es el de academico.docente_seccion para (plan_curso_id, seccion_id). */
	public record SolicitudCelda(@NotNull(message = "Elige el día de cada bloque.") DiaSemana dia,
			@NotNull(message = "Elige el número de cada bloque.") @Min(value = 1, message = "El número de bloque empieza en 1.") Integer numeroBloque,
			@NotNull(message = "Elige el curso de cada bloque.") UUID planCursoId,
			@NotNull(message = "Elige el ambiente de cada bloque.") UUID ambienteId) {
	}

	/**
	 * Cuerpo del PUT /horarios/secciones/{seccionId}: la matriz COMPLETA deseada (no un delta). {@code version} es la
	 * que devolvió el GET: null si la sección aún no tenía horario guardado.
	 */
	public record SolicitudHorario(Long version,
			@NotNull(message = "Envía las celdas del horario.") @Valid List<@NotNull(message = "Hay una celda vacía.") @Valid SolicitudCelda> celdas) {
	}

	// ---------- Selectores de año, grado y sección (el Coordinador no entra a /configuracion) ----------

	/** GET /horarios/anios-escolares: lo que el selector de año necesita. */
	public record AnioParaHorarios(UUID id, int anio, String estado) {
	}

	public record SeccionParaHorarios(UUID id, String nombre, String estado) {
	}

	/**
	 * GET /horarios/anios-escolares/{anioId}/secciones: los grados del catálogo con las secciones que tienen en el año
	 * (con la misma forma que los grados de Configuración, pero solo con lo que usa el selector).
	 */
	public record GradoParaHorarios(String codigo, String nombre, String nivel, int numero,
			int cantidadSeccionesActivas, List<SeccionParaHorarios> secciones) {
	}
}
