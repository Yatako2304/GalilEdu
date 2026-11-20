package com.galiledu.academico.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaPeriodoCompetencia;
import com.galiledu.academico.aplicacion.DtosCalificaciones.NotaResultado;

/**
 * Contratos JSON de los reportes de notas (HU-63 por curso y grado, HU-64 institucional) y de la consulta del
 * apoderado (HU-69). Son de solo lectura y las notas se calculan al consultarlas.
 */
public final class DtosReportesNotas {
	private DtosReportesNotas() {
	}

	// ---------- Opciones de los filtros ----------

	public record PeriodoOpcion(UUID id, int numero, String nombre) {
	}

	public record CursoOpcion(UUID planCursoId, String curso) {
	}

	public record GradoOpcion(String codigo, String nombre, List<CursoOpcion> cursos) {
	}

	/** Un año ACTIVO o CERRADO con sus parámetros, periodos y grados con plan de evaluación. */
	public record AnioReporte(UUID id, int anio, String estado, String escala, String reglaNota,
			String consolidacion, List<PeriodoOpcion> periodos, List<GradoOpcion> grados) {
	}

	// ---------- HU-63 · Reporte por curso y grado ----------

	/**
	 * Resumen de un grupo de estudiantes en una competencia. promedio: promedio simple en vigesimal (p. ej. "14.2") o
	 * nivel más frecuente en la cualitativa; "—" sin notas. nivel y logro: los del promedio. conNota: estudiantes con
	 * nota; logroEsperado: de ellos, con A o AD; porcentajeLogro: null sin notas; pendientes: estudiantes con la nota
	 * pendiente porque falta registrar algún ítem.
	 */
	public record CeldaResumen(String promedio, String nivel, String logro, int conNota, int logroEsperado,
			Integer porcentajeLogro, int pendientes) {
	}

	public record SeccionReporte(UUID id, String nombre, int estudiantes) {
	}

	/**
	 * Una competencia: una celda por sección (en el orden de {@code secciones}) y la del grado completo. calculable es
	 * false si la nota oficial no se puede calcular porque los pesos de sus cursos no suman 100 %.
	 */
	public record FilaReporteGrado(UUID competenciaId, String competencia, String area, boolean calculable,
			List<CeldaResumen> secciones, CeldaResumen general) {
	}

	/**
	 * curso es null cuando el reporte usa la nota oficial de la competencia (todos los cursos del grado); con un curso,
	 * cada celda resume lo que ese curso aporta a la competencia. periodo es el nombre del periodo o «Nota final».
	 */
	public record ReporteGrado(int anio, String grado, String curso, String periodo, String escala, String reglaNota,
			List<SeccionReporte> secciones, List<FilaReporteGrado> filas) {
	}

	/**
	 * Detalle de una sección para el Coordinador (desde HU-63): la nota oficial de cada competencia del grado para cada
	 * estudiante, por periodo y la final, con los mismos datos que la tarjeta del docente.
	 */
	public record NotasSeccion(UUID seccionId, int anio, String grado, String seccion,
			DtosCalificaciones.NotasCompetencias notas) {
	}

	// ---------- HU-64 · Reporte institucional ----------

	/**
	 * Calificaciones de competencia por nivel de logro. calificaciones: las completas (AD + A + B + C), base de los
	 * porcentajes. porCompletar: a las que les falta registrar algún ítem; no se cuentan hasta que se completen (la
	 * coordinación puede pedírselo al docente). porcentajeLogro: A o AD sobre calificaciones; null si no hay ninguna.
	 */
	public record Distribucion(int calificaciones, int ad, int a, int b, int c, int porCompletar,
			Integer porcentajeLogro) {
	}

	/** Una sección de un grado; con seccion null, el total del grado. */
	public record FilaInstitucional(String gradoCodigo, String grado, String seccion, int estudiantes,
			Distribucion distribucion) {
	}

	public record AreaInstitucional(String area, Distribucion distribucion) {
	}

	/** Competencia de un grado que no se puede calcular porque los pesos de sus cursos no suman 100 %. */
	public record CompetenciaSinCalcular(String grado, String competencia) {
	}

	/**
	 * Cada nota oficial de competencia de cada estudiante cuenta una vez (una calificación), en el periodo elegido o en
	 * la nota final. estudiantes: los de las secciones de grados con plan de evaluación. Las áreas van todas, también
	 * las que aún no tienen calificaciones.
	 */
	public record ReporteInstitucional(int anio, String periodo, String escala, int estudiantes,
			List<FilaInstitucional> filas, List<AreaInstitucional> areas, Distribucion total,
			List<CompetenciaSinCalcular> sinCalcular) {
	}

	// ---------- HU-69 · Consulta de notas por el apoderado ----------

	public record MatriculaHijo(int anio, String grado, String seccion) {
	}

	/** Un hijo matriculado con sus matrículas, del año más reciente al más antiguo. */
	public record HijoCalificaciones(UUID estudianteId, String estudiante, List<MatriculaHijo> matriculas) {
	}

	/** estado: PLANIFICADO, EN_CURSO o CERRADO, calculado con las fechas y el estado del año. */
	public record PeriodoApoderado(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			String estado) {
	}

	/** Un ítem calificado o pendiente. nota: "15.5", "A" o "—"; nivel: AD, A, B, C o null. */
	public record ItemApoderado(UUID itemId, String item, String tipo, String nota, String nivel) {
	}

	/** Lo que el curso aporta a una competencia en un periodo, con los ítems que lo forman. */
	public record AportePeriodo(UUID periodoId, NotaResultado aporte, List<ItemApoderado> items) {
	}

	public record CompetenciaCurso(UUID competenciaId, String competencia, List<AportePeriodo> periodos) {
	}

	/** Resumen referencial del curso en un periodo: todos sus ítems con la regla del año. No es una nota oficial. */
	public record ResumenCursoPeriodo(UUID periodoId, NotaResultado resultado) {
	}

	public record CursoApoderado(UUID planCursoId, String curso, String area, String docente,
			List<ResumenCursoPeriodo> periodos, List<CompetenciaCurso> competencias) {
	}

	/**
	 * Nota oficial de una competencia: una por periodo y la final. calculable: los pesos de sus cursos están completos.
	 */
	public record CompetenciaApoderado(UUID competenciaId, String competencia, String area, List<String> cursos,
			boolean calculable, List<NotaPeriodoCompetencia> periodos, NotaResultado notaFinal) {
	}

	/** finalizado: el año está cerrado o ya terminó su último periodo (vista «Resultado final»). */
	public record NotasApoderado(UUID estudianteId, String estudiante, int anio, String estadoAnio,
			boolean finalizado, String grado, String seccion, String escala, String reglaNota, String consolidacion,
			List<PeriodoApoderado> periodos, List<CursoApoderado> cursos, List<CompetenciaApoderado> competencias) {
	}
}
