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
}
