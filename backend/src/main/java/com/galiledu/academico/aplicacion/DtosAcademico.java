package com.galiledu.academico.aplicacion;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.galiledu.academico.dominio.Area;
import com.galiledu.academico.dominio.Competencia;
import com.galiledu.academico.dominio.Curso;
import com.galiledu.academico.dominio.EstadoArea;
import com.galiledu.academico.dominio.EstadoCurso;

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
}
