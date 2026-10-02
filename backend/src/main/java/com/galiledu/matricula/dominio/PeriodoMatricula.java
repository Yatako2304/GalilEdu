package com.galiledu.matricula.dominio;

import java.time.LocalDate;
import java.util.Objects;

/** Rango regular de matrícula de un año escolar (HU-23, RF-51). */
public record PeriodoMatricula(int anioEscolar, LocalDate inicio, LocalDate fin) {
	public PeriodoMatricula {
		if (anioEscolar < 1) {
			throw new IllegalArgumentException("El año escolar debe ser positivo");
		}
		Objects.requireNonNull(inicio, "La fecha de inicio es obligatoria");
		Objects.requireNonNull(fin, "La fecha de fin es obligatoria");
		if (inicio.isAfter(fin)) {
			throw new IllegalArgumentException("El periodo termina antes de comenzar");
		}
	}

	public boolean habilitadoEn(LocalDate fecha) {
		Objects.requireNonNull(fecha, "La fecha de consulta es obligatoria");
		return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
	}
}
