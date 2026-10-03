package com.galiledu.academica.aplicacion;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CargaAcademica;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CompetenciaConfigurada;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.PeriodoAcademico;

/** Comprobaciones de contexto y autorización compartidas por los casos de uso académicos. */
final class VerificacionesAcademicas {
	private final ConsultaContextoAcademico contexto;

	VerificacionesAcademicas(ConsultaContextoAcademico contexto) {
		this.contexto = Objects.requireNonNull(contexto);
	}

	ActorAcademico actor(String nombreUsuario) {
		if (nombreUsuario == null || nombreUsuario.isBlank()) {
			throw new AccesoAcademicoDenegadoException("Se requiere una cuenta autenticada");
		}
		return contexto.buscarActor(nombreUsuario)
			.orElseThrow(() -> new AccesoAcademicoDenegadoException("La cuenta no está activa"));
	}

	CargaAcademica cargaActiva(UUID cargaAcademicaId) {
		return contexto.buscarCarga(Objects.requireNonNull(cargaAcademicaId, "La carga es obligatoria"))
			.filter(CargaAcademica::activa)
			.orElseThrow(() -> new NoSuchElementException("Carga académica no encontrada"));
	}

	/** La competencia debe pertenecer a la misma oferta de curso que la carga. */
	CompetenciaConfigurada competenciaDeCarga(UUID configuracionCompetenciaId, CargaAcademica carga) {
		CompetenciaConfigurada competencia = contexto.buscarCompetencia(
				Objects.requireNonNull(configuracionCompetenciaId, "La competencia es obligatoria"))
			.filter(CompetenciaConfigurada::activa)
			.orElseThrow(() -> new NoSuchElementException("Competencia configurada no encontrada"));
		if (!competencia.ofertaCursoId().equals(carga.ofertaCursoId())) {
			throw new SolicitudAcademicaInvalidaException("La competencia no corresponde al curso de la carga");
		}
		return competencia;
	}

	/** El periodo debe pertenecer al año escolar de la carga. */
	PeriodoAcademico periodoDeCarga(UUID periodoAcademicoId, CargaAcademica carga) {
		PeriodoAcademico periodo = contexto.buscarPeriodo(
				Objects.requireNonNull(periodoAcademicoId, "El periodo es obligatorio"))
			.filter(PeriodoAcademico::activo)
			.orElseThrow(() -> new NoSuchElementException("Periodo académico no encontrado"));
		if (!periodo.anioId().equals(carga.anioId())) {
			throw new SolicitudAcademicaInvalidaException("El periodo no corresponde al año escolar de la carga");
		}
		return periodo;
	}

	void exigirDocenteDeCarga(ActorAcademico actor, CargaAcademica carga) {
		if (!actor.personaId().equals(carga.docenteId())) {
			throw new AccesoAcademicoDenegadoException("Solo el docente de la carga puede realizar esta operación");
		}
	}

	void exigirDocenteOAdministrador(ActorAcademico actor, CargaAcademica carga) {
		if (!actor.esAdministrador()) exigirDocenteDeCarga(actor, carga);
	}

	void exigirLecturaDeCarga(ActorAcademico actor, CargaAcademica carga) {
		if (actor.esAdministrador() || actor.personaId().equals(carga.docenteId())
			|| contexto.coordinaOferta(actor.personaId(), carga.ofertaCursoId())) {
			return;
		}
		throw new AccesoAcademicoDenegadoException("La cuenta no tiene acceso a esta carga académica");
	}
}
