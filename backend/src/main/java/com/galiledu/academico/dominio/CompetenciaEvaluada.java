package com.galiledu.academico.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Competencia que evalúa un curso del plan, con su escala y reglas (tabla academico.plan_competencia, V6). */
@Embeddable
public class CompetenciaEvaluada {
	@Column(name = "competencia_id", nullable = false)
	private UUID competenciaId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Escala escala;
	@Enumerated(EnumType.STRING)
	@Column(name = "regla_nota", nullable = false)
	private ReglaNota reglaNota;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Consolidacion consolidacion;

	protected CompetenciaEvaluada() {
	}

	public CompetenciaEvaluada(UUID competenciaId, Escala escala, ReglaNota reglaNota, Consolidacion consolidacion) {
		this.competenciaId = competenciaId;
		this.escala = escala;
		this.reglaNota = reglaNota;
		this.consolidacion = consolidacion;
	}

	public UUID getCompetenciaId() {
		return competenciaId;
	}
	public Escala getEscala() {
		return escala;
	}
	public ReglaNota getReglaNota() {
		return reglaNota;
	}
	public Consolidacion getConsolidacion() {
		return consolidacion;
	}
}
