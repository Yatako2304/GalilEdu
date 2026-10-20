package com.galiledu.academico.aplicacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.galiledu.academico.dominio.Area;
import com.galiledu.academico.dominio.Competencia;
import com.galiledu.academico.dominio.Consolidacion;
import com.galiledu.academico.dominio.Curso;
import com.galiledu.academico.dominio.Escala;
import com.galiledu.academico.dominio.EstadoArea;
import com.galiledu.academico.dominio.EstadoCurso;
import com.galiledu.academico.dominio.EstadoVersion;
import com.galiledu.academico.dominio.NivelLogro;
import com.galiledu.academico.dominio.RangoNivel;
import com.galiledu.academico.dominio.ReglaNota;
import com.galiledu.academico.dominio.TablaConversion;

/** Contratos JSON del catálogo académico. Deben coincidir con frontend/src/types/api.ts. */
public final class DtosAcademico {
	private DtosAcademico() {
	}

	// ---------- Plan curricular (HU-52, HU-53, HU-54) ----------

	/** Figma CI-14: tarjetas "Áreas activas", "Competencias activas" y "Cursos activos". */
	public record ResumenPlanCurricular(long areasActivas, long areasRegistradas, long competenciasActivas,
			long competenciasRegistradas, long cursosActivos, long cursosRegistrados, Integer anioActivo) {
	}

	/** Fila de la lista de áreas: "4 competencias · 5 cursos". */
	public record AreaResumen(UUID id, String nombre, String descripcion, EstadoArea estado, long competencias,
			long cursos) {
		static AreaResumen de(Area a, long competencias, long cursos) {
			return new AreaResumen(a.getId(), a.getNombre(), a.getDescripcion(), a.getEstado(), competencias, cursos);
		}
	}

	public record AreaDetalle(UUID id, String nombre, String descripcion, EstadoArea estado,
			List<CompetenciaResumen> competencias, List<CursoResumen> cursos) {
	}

	public record CompetenciaResumen(UUID id, UUID areaId, String nombre, String descripcion, int orden,
			EstadoArea estado) {
		static CompetenciaResumen de(Competencia c) {
			return new CompetenciaResumen(c.getId(), c.getAreaId(), c.getNombre(), c.getDescripcion(), c.getOrden(),
					c.getEstado());
		}
	}

	/** grados: códigos del catálogo en el orden del catálogo (PRI-1…SEC-5). */
	public record CursoResumen(UUID id, UUID areaId, String nombre, EstadoCurso estado, List<String> grados) {
		static CursoResumen de(Curso c, List<String> ordenGrados) {
			return new CursoResumen(c.getId(), c.getAreaId(), c.getNombre(), c.getEstado(),
					ordenGrados.stream().filter(c.getGrados()::contains).toList());
		}
	}

	/** Modal "Nueva área curricular". La descripción es opcional en Figma (RF-126 la pedía obligatoria). */
	public record SolicitudArea(
			@NotBlank(message = "Ingresa el nombre del área.") @Size(max = 80, message = "El nombre admite hasta 80 caracteres.") String nombre,
			@Size(max = 500, message = "La descripción admite hasta 500 caracteres.") String descripcion) {
	}

	/** Modal "Nueva competencia". */
	public record SolicitudCompetencia(
			@NotBlank(message = "Ingresa el nombre de la competencia.") @Size(max = 150, message = "El nombre admite hasta 150 caracteres.") String nombre,
			@Size(max = 500, message = "La descripción admite hasta 500 caracteres.") String descripcion) {
	}

	/** Modal "Nuevo curso": nombre y grados de primaria y secundaria en los que se dicta. */
	public record SolicitudCurso(
			@NotBlank(message = "Ingresa el nombre del curso.") @Size(max = 80, message = "El nombre admite hasta 80 caracteres.") String nombre,
			@NotEmpty(message = "Elige al menos un grado.") Set<String> grados) {
	}

	// ---------- Escala de calificación (HU-55) ----------

	public record RangoNivelDetalle(NivelLogro nivel, String significado, int notaDesde, int notaHasta) {
		static RangoNivelDetalle de(RangoNivel r) {
			return new RangoNivelDetalle(r.getNivel(), r.getSignificado(), r.getNotaDesde(), r.getNotaHasta());
		}
	}

	/** Una versión de la tabla. niveles: de mayor a menor (AD, A, B, C), como la tabla de Figma CI-23. */
	public record TablaConversionDetalle(UUID id, LocalDate vigenteDesde, EstadoVersion estado,
			List<RangoNivelDetalle> niveles) {
		static TablaConversionDetalle de(TablaConversion t, EstadoVersion estado) {
			return new TablaConversionDetalle(t.getId(), t.getVigenteDesde(), estado,
					t.nivelesDeMayorAMenor().stream().map(RangoNivelDetalle::de).toList());
		}
	}

	public record SolicitudRango(
			@NotNull(message = "Falta el nivel.") NivelLogro nivel,
			@NotNull(message = "Completa la nota \"desde\" de cada nivel.") @Min(value = 0, message = "Las notas van de 0 a 20.") @Max(value = 20, message = "Las notas van de 0 a 20.") Integer notaDesde,
			@NotNull(message = "Completa la nota \"hasta\" de cada nivel.") @Min(value = 0, message = "Las notas van de 0 a 20.") @Max(value = 20, message = "Las notas van de 0 a 20.") Integer notaHasta) {
	}

	/** Modal "Nueva versión de la tabla de conversión". */
	public record SolicitudTablaConversion(
			@NotNull(message = "Elige la fecha desde la que rige la versión.") LocalDate vigenteDesde,
			@NotNull(message = "Define un rango para cada nivel: AD, A, B y C.") List<@Valid SolicitudRango> niveles) {
	}

	// ---------- Plan de evaluación (HU-56) ----------

	/** Selector "Año escolar 2027 · Planificado" de CI-17/CI-20. */
	public record AnioEscolarOpcion(UUID id, int anio, String estado) {
	}

	public record DocenteOpcion(UUID id, String nombre) {
	}

	/** Sección del grado con su tutor (Figma: "Sección 1.° A · tutor: Héctor Ramos Cruz"). */
	public record SeccionPlan(UUID id, String nombre, String tutor) {
	}

	/**
	 * Fila de la tabla de CI-17. escala: VIGESIMAL, CUALITATIVA, MIXTA (si sus competencias usan las dos) o null (sin
	 * configurar).
	 */
	public record PlanCursoResumen(UUID id, UUID cursoId, String curso, String area, int horasSemanales,
			int competencias, String escala, int docentesAsignados) {
	}

	/** Plan de un grado en un año (CI-17), con las tarjetas de la cabecera. */
	public record PlanGrado(UUID anioEscolarId, int anio, String estadoAnio, String gradoCodigo, String grado,
			Integer bloquesDisponibles, int horasAsignadas, int competenciasEvaluadas, int cursosConfigurados,
			int docentesAsignados, int docentesRequeridos, List<SeccionPlan> secciones,
			List<PlanCursoResumen> cursos) {
	}

	/** Competencia del área en el modal de configuración; las no seleccionadas traen los valores por defecto. */
	public record CompetenciaPlan(UUID competenciaId, String nombre, EstadoArea estado, boolean seleccionada,
			Escala escala, ReglaNota reglaNota, Consolidacion consolidacion) {
	}

	/** Modal "Aritmética · 1.° de secundaria". */
	public record PlanCursoDetalle(UUID id, UUID cursoId, String curso, String area, int anio, String grado,
			int horasSemanales, Instant actualizadoEn, List<CompetenciaPlan> competencias) {
	}

	/** Opción del modal "Agregar curso": cursos activos que se dictan en el grado y aún no están en su plan. */
	public record CursoDisponible(UUID id, String nombre, String area, List<CompetenciaResumen> competencias) {
	}

	/** Plan que se puede copiar (modal "Copiar plan"): "3.° de secundaria · 2026 (16 cursos)". */
	public record OrigenPlan(UUID anioEscolarId, int anio, String gradoCodigo, String grado, int cursos, int horas,
			int competencias) {
	}

	public record SolicitudCompetenciaPlan(
			@NotNull(message = "Falta la competencia.") UUID competenciaId,
			@NotNull(message = "Elige la escala de cada competencia.") Escala escala,
			@NotNull(message = "Elige cómo se agregan los ítems de cada competencia.") ReglaNota reglaNota,
			@NotNull(message = "Elige cómo se consolidan los periodos de cada competencia.") Consolidacion consolidacion) {
	}

	/** Modal "Agregar curso a 3.° de secundaria". */
	public record SolicitudPlanCurso(
			@NotNull(message = "Falta el año escolar.") UUID anioEscolarId,
			@NotBlank(message = "Falta el grado.") String gradoCodigo,
			@NotNull(message = "Elige el curso.") UUID cursoId,
			@NotNull(message = "Ingresa las horas semanales.") @Min(value = 1, message = "Las horas semanales van de 1 a 20.") @Max(value = 20, message = "Las horas semanales van de 1 a 20.") Integer horasSemanales,
			@NotNull(message = "Falta la lista de competencias.") List<@Valid SolicitudCompetenciaPlan> competencias) {
	}

	/** Modal "<curso> · <grado>": horas y competencias de un curso que ya está en el plan. */
	public record SolicitudConfigurarCurso(
			@NotNull(message = "Ingresa las horas semanales.") @Min(value = 1, message = "Las horas semanales van de 1 a 20.") @Max(value = 20, message = "Las horas semanales van de 1 a 20.") Integer horasSemanales,
			@NotNull(message = "Falta la lista de competencias.") List<@Valid SolicitudCompetenciaPlan> competencias) {
	}

	/** docenteId null deja la sección sin docente. */
	public record AsignacionDocente(@NotNull(message = "Falta la sección.") UUID seccionId, UUID docenteId) {
	}

	/** Modal "Docentes de Álgebra". */
	public record SolicitudDocentes(
			@NotNull(message = "Falta la lista de secciones.") List<@Valid AsignacionDocente> asignaciones) {
	}

	/** Respuesta del modal de docentes: una fila por sección del grado. */
	public record DocenteSeccion(UUID seccionId, String seccion, String tutor, UUID docenteId) {
	}

	/** Modal "Copiar plan a 3.° de secundaria 2027". */
	public record SolicitudCopiaPlan(
			@NotNull(message = "Falta el año escolar.") UUID anioEscolarId,
			@NotBlank(message = "Falta el grado.") String gradoCodigo,
			@NotNull(message = "Elige el plan que quieres copiar.") UUID origenAnioEscolarId,
			@NotBlank(message = "Elige el plan que quieres copiar.") String origenGradoCodigo) {
	}
}
