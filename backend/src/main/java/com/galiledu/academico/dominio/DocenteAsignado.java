package com.galiledu.academico.dominio;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Docente que dicta un curso del plan en una sección (tabla academico.docente_seccion, V6). */
@Embeddable
public class DocenteAsignado {
	@Column(name = "seccion_id", nullable = false)
	private UUID seccionId;
	@Column(name = "docente_id", nullable = false)
	private UUID docenteId;

	protected DocenteAsignado() {
	}

	public DocenteAsignado(UUID seccionId, UUID docenteId) {
		this.seccionId = seccionId;
		this.docenteId = docenteId;
	}

	public UUID getSeccionId() {
		return seccionId;
	}
	public UUID getDocenteId() {
		return docenteId;
	}
}
