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
			LocalDate fechaLimiteNotas, String estado, boolean registroNotasAbierto, int competencias, int items) {
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

	public record FilaCalificacion(UUID id, UUID matriculaId, UUID estudianteId, String estudiante,
			BigDecimal notaNumerica, String nivelLogro, EvidenciaResumen evidencia, String estado) {
	}

	public record RegistroCalificacion(@NotNull UUID matriculaId,
			@DecimalMin("0.00") @DecimalMax("20.00") BigDecimal notaNumerica,
			@Pattern(regexp = "AD|A|B|C") String nivelLogro) {
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
	 * esperado», «En proceso», «En inicio») o «Pendiente» sin nota; parcial: faltan notas que influyen.
	 */
	public record NotaResultado(String nota, String nivel, String logro, boolean parcial) {
	}

	/** Lo que aporta un curso a la competencia en un periodo ("—" si aún no tiene notas). */
	public record AporteCurso(UUID planCursoId, String nota) {
	}

	public record NotaPeriodoCompetencia(UUID periodoId, String nota, String nivel, String logro, boolean parcial,
			List<AporteCurso> aportes) {
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
