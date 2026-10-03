package com.galiledu.academica.aplicacion.puertos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.academica.aplicacion.ActorAcademico;
import com.galiledu.academica.dominio.ConfiguracionCompetencia;
import com.galiledu.academica.dominio.EstadoPeriodo;
import com.galiledu.academica.dominio.TablaConversion;

/**
 * Datos de solo lectura que Académico necesita de Seguridad, Configuración y Matrícula
 * (ICargaAcademica, IPeriodosAcademicos, IMatriculas). No modifica esos módulos.
 */
public interface ConsultaContextoAcademico {
	Optional<ActorAcademico> buscarActor(String nombreUsuario);

	Optional<CargaAcademica> buscarCarga(UUID cargaAcademicaId);

	Optional<CompetenciaConfigurada> buscarCompetencia(UUID configuracionCompetenciaId);

	Optional<PeriodoAcademico> buscarPeriodo(UUID periodoAcademicoId);

	List<PeriodoAcademico> listarPeriodosDelAnio(UUID anioId);

	Optional<MatriculaAcademica> buscarMatricula(UUID matriculaId);

	/** Matrículas vigentes de la sección en el año de la carga. */
	List<UUID> listarMatriculasVigentes(UUID seccionId, UUID anioId);

	Optional<TablaConversion> buscarTablaConversionVigente();

	boolean coordinaOferta(UUID docenteId, UUID ofertaCursoId);

	/** Estudiante titular, apoderado vinculado o tutor vigente de la sección de la matrícula. */
	boolean puedeConsultarMatricula(UUID personaId, UUID matriculaId);

	record CargaAcademica(UUID id, UUID ofertaCursoId, UUID seccionId, UUID docenteId, UUID anioId,
		boolean activa) {}

	record CompetenciaConfigurada(UUID id, UUID ofertaCursoId, UUID competenciaId,
		ConfiguracionCompetencia configuracion, boolean activa) {}

	record PeriodoAcademico(UUID id, UUID anioId, int orden, EstadoPeriodo estado,
		LocalDateTime fechaLimiteNotas, boolean activo) {
		/** RF-133/134: las notas se registran mientras el periodo está en curso y antes del límite. */
		public boolean admiteNotasEn(LocalDateTime ahora) {
			return activo && estado == EstadoPeriodo.EN_CURSO && !ahora.isAfter(fechaLimiteNotas);
		}
	}

	record MatriculaAcademica(UUID id, UUID seccionId, UUID anioId, boolean vigente) {}
}
