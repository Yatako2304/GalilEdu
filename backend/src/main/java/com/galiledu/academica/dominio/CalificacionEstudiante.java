package com.galiledu.academica.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Nota registrada de un estudiante (vía su matrícula) en un ítem de evaluación. */
public final class CalificacionEstudiante {
	private final UUID id;
	private final UUID itemEvaluacionId;
	private final UUID matriculaId;
	private final Calificacion valor;
	private final Instant fechaRegistro;
	private final Instant fechaModificacion;

	public CalificacionEstudiante(UUID id, UUID itemEvaluacionId, UUID matriculaId, Calificacion valor,
		Instant fechaRegistro, Instant fechaModificacion) {
		this.id = Objects.requireNonNull(id, "El id de la calificación es obligatorio");
		this.itemEvaluacionId = Objects.requireNonNull(itemEvaluacionId, "El ítem es obligatorio");
		this.matriculaId = Objects.requireNonNull(matriculaId, "La matrícula es obligatoria");
		this.valor = Objects.requireNonNull(valor, "El valor es obligatorio");
		this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "La fecha de registro es obligatoria");
		if (fechaModificacion != null && fechaModificacion.isBefore(fechaRegistro)) {
			throw new IllegalArgumentException("La modificación no puede ser anterior al registro");
		}
		this.fechaModificacion = fechaModificacion;
	}

	public static CalificacionEstudiante nueva(UUID itemEvaluacionId, UUID matriculaId, Calificacion valor,
		Instant ahora) {
		return new CalificacionEstudiante(UUID.randomUUID(), itemEvaluacionId, matriculaId, valor, ahora, null);
	}

	public CalificacionEstudiante corregir(Calificacion nuevoValor, Instant ahora) {
		Objects.requireNonNull(nuevoValor, "El nuevo valor es obligatorio");
		if (nuevoValor.escala() != valor.escala()) {
			throw new IllegalArgumentException("La corrección debe usar la misma escala");
		}
		return new CalificacionEstudiante(id, itemEvaluacionId, matriculaId, nuevoValor, fechaRegistro,
			Objects.requireNonNull(ahora, "El instante es obligatorio"));
	}

	public UUID id() {
		return id;
	}

	public UUID itemEvaluacionId() {
		return itemEvaluacionId;
	}

	public UUID matriculaId() {
		return matriculaId;
	}

	public Calificacion valor() {
		return valor;
	}

	public Instant fechaRegistro() {
		return fechaRegistro;
	}

	public Instant fechaModificacion() {
		return fechaModificacion;
	}

	@Override
	public String toString() {
		return "CalificacionEstudiante[id=" + id + ", itemEvaluacionId=" + itemEvaluacionId
			+ ", matriculaId=" + matriculaId + "]";
	}
}
