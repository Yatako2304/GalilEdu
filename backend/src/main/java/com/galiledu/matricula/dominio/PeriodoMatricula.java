package com.galiledu.matricula.dominio;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Rango regular de matrícula de un año escolar (HU-23, RF-51). */
public record PeriodoMatricula(UUID anioEscolarId, LocalDate inicio, LocalDate fin) {
	public PeriodoMatricula {
		if (anioEscolarId == null || inicio == null || fin == null) {
			throw new IllegalArgumentException("El año escolar y las fechas de matrícula son obligatorios");
		}
		if (inicio.isAfter(fin)) {
			throw new IllegalArgumentException("El periodo termina antes de comenzar");
		}
	}

	public boolean habilitadoEn(LocalDate fecha) {
		Objects.requireNonNull(fecha, "La fecha de consulta es obligatoria");
		return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
	}
}
