package com.galiledu.academico.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Competencia que evalúa un curso. La escala y las reglas pertenecen al año escolar desde V9. */
@Embeddable
public class CompetenciaEvaluada {
	@Column(name = "competencia_id", nullable = false)
	private UUID competenciaId;

	protected CompetenciaEvaluada() {
	}

	public CompetenciaEvaluada(UUID competenciaId) {
		this.competenciaId = competenciaId;
	}

	public UUID getCompetenciaId() {
		return competenciaId;
	}
}
