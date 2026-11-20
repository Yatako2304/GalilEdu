package com.galiledu.academico.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class DtosCalificaciones {
	private DtosCalificaciones() {
	}

	public record CursoDocente(UUID planCursoId, UUID seccionId, UUID anioEscolarId, int anio, String curso,
			String area, String gradoCodigo, String grado, String seccion, String escala, String reglaNota,
			String consolidacion, List<PeriodoCurso> periodos) {
	}

	public record PeriodoCurso(UUID id, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, String estado, boolean registroNotasAbierto, boolean modificacionHabilitada,
			int competencias, int items) {
	}

	public record CompetenciaOpcion(UUID id, String nombre) {
	}

	public record CompetenciaItem(UUID competenciaId, String competencia, BigDecimal pesoPorcentaje) {
	}

	public record ItemEvaluacion(UUID id, String nombre, String descripcion, String tipo, Instant actualizadoEn,
			List<CompetenciaItem> competencias, boolean editable) {
	}

	public record SolicitudCompetenciaItem(@NotNull UUID competenciaId, BigDecimal pesoPorcentaje) {
	}

	public record SolicitudItem(@NotBlank @Size(max = 120) String nombre,
			@NotBlank @Size(max = 500) String descripcion,
			@NotBlank @Pattern(regexp = "PRACTICA|EXAMEN|PROYECTO|TRABAJO_INDIVIDUAL|EXPOSICION|PORTAFOLIO|OTRO") String tipo,
			@NotEmpty List<@Valid SolicitudCompetenciaItem> competencias) {
	}

	public record EvidenciaResumen(String nombreArchivo, String tipoMime, int tamanoBytes, Instant subidoEn) {
	}

	/**
	 * Una celda de «Calificar alumnos». estado: PENDIENTE (falta registrar), CALIFICADO o NO_EVALUADO (resuelta sin
	 * nota, con motivo: ese ítem no cuenta para el estudiante).
	 */
	public record FilaCalificacion(UUID id, UUID matriculaId, UUID estudianteId, String estudiante,
			BigDecimal notaNumerica, String nivelLogro, EvidenciaResumen evidencia, String estado,
			int modificaciones, boolean noEvaluado, String motivoNoEvaluado) {
	}

	public record RegistroCalificacion(@NotNull UUID matriculaId,
			@DecimalMin("0.00") @DecimalMax("20.00") BigDecimal notaNumerica,
			@Pattern(regexp = "AD|A|B|C") String nivelLogro) {
	}

	/**
	 * HU-60: cambiar una nota ya registrada. Un solo valor según la escala del año y un motivo obligatorio que queda en
	 * el historial.
	 */
	public record SolicitudModificacion(@DecimalMin("0.00") @DecimalMax("20.00") BigDecimal notaNumerica,
			@Pattern(regexp = "AD|A|B|C") String nivelLogro, Boolean noEvaluado, @Size(max = 500) String motivo) {
		public boolean marcaNoEvaluado() {
			return Boolean.TRUE.equals(noEvaluado);
		}
	}

	/**
	 * Un cambio del historial de una nota (HU-60). Anterior vacío y sin «No evaluado» = se registró fuera de plazo;
	 * nuevo vacío y sin «No evaluado» = la nota se quitó en el registro.
	 */
	public record ModificacionCalificacion(BigDecimal notaAnterior, String nivelAnterior, boolean noEvaluadoAnterior,
			BigDecimal notaNueva, String nivelNuevo, boolean noEvaluadoNuevo, String motivo, String modificadoPor,
			Instant modificadoEn) {
	}

	public record SolicitudCalificaciones(@NotEmpty List<@Valid RegistroCalificacion> calificaciones) {
	}

	public record ColumnaReporte(String clave, String titulo, UUID competenciaId, String competencia,
			boolean promedio) {
		public ColumnaReporte(String clave, String titulo) {
			this(clave, titulo, null, null, "resultado".equals(clave));
		}
	}

	public record FilaReporte(UUID matriculaId, String estudiante, Map<String, String> valores) {
	}

	public record ReporteCalificaciones(String titulo, String detalle, List<ColumnaReporte> columnas,
			List<FilaReporte> filas) {
	}

	public record ArchivoEvidencia(String nombreArchivo, String tipoMime, byte[] contenido) {
	}

	// ---------- Nota de la competencia (una por periodo y una final, entre todos los cursos del grado) ----------

	public record PeriodoNotas(UUID id, int numero, String nombre) {
	}

	/** Un curso que evalúa la competencia. pesoCurso es el del plan; solo se usa con promedio ponderado. */
	public record CursoCompetencia(UUID planCursoId, String curso, BigDecimal pesoCurso) {
	}

	/**
	 * Una competencia elegida. usaPesos: la regla del año usa el peso de los cursos (promedio ponderado o escala
	 * cualitativa). Con usaPesos y pesos incompletos no se
	 * calcula la nota de la competencia (solo los aportes de cada curso).
	 */
	public record CompetenciaNotas(UUID competenciaId, String competencia, boolean usaPesos, boolean pesosCompletos,
			List<CursoCompetencia> cursos) {
	}

	/**
	 * nota es "—" si todavía no hay; nivel: AD, A, B o C (null sin nota), en la escala vigesimal sale de redondear la
	 * nota al entero y ubicarla en la tabla de conversión; logro: significado del nivel («Logro destacado», «Logro
	 * esperado», «En proceso», «En inicio») o «Pendiente» sin nota; pendiente: falta
	 * registrar algún ítem que cuenta, y entonces no hay nota (calificadas de total). Una nota oficial existe solo
	 * cuando está completa.
	 */
	public record NotaResultado(String nota, String nivel, String logro, boolean pendiente, int calificadas,
			int total) {
	}

	/**
	 * Lo que aporta un curso a la competencia en un periodo. nota: el promedio de lo registrado ("—" sin notas);
	 * pendiente: falta registrar algún ítem, y entonces no es oficial todavía.
	 */
	public record AporteCurso(UUID planCursoId, String nota, boolean pendiente, int calificadas, int total) {
	}

	public record NotaPeriodoCompetencia(UUID periodoId, String nota, String nivel, String logro, boolean pendiente,
			int calificadas, int total, List<AporteCurso> aportes) {
	}

	public record NotasEstudianteCompetencia(UUID competenciaId, List<NotaPeriodoCompetencia> periodos,
			NotaResultado notaFinal) {
	}

	public record FilaNotasCompetencia(UUID matriculaId, String estudiante,
			List<NotasEstudianteCompetencia> competencias) {
	}

	/** Notas oficiales de las competencias de una sección, con los parámetros del año que las produjeron. */
	public record NotasCompetencias(String escala, String reglaNota, String consolidacion,
			List<PeriodoNotas> periodos, List<CompetenciaNotas> competencias, List<FilaNotasCompetencia> filas) {
	}
}
