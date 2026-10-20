package com.galiledu.academico.aplicacion;

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
import com.galiledu.academico.dominio.Curso;
import com.galiledu.academico.dominio.EstadoArea;
import com.galiledu.academico.dominio.EstadoCurso;
import com.galiledu.academico.dominio.EstadoVersion;
import com.galiledu.academico.dominio.NivelLogro;
import com.galiledu.academico.dominio.RangoNivel;
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
}
