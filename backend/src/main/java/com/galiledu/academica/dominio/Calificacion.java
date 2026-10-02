package com.galiledu.academica.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/** Nota en una sola escala; no calcula equivalencias ni promedios. */
public record Calificacion(EscalaCalificacion escala, BigDecimal vigesimal,
	NivelCualitativo cualitativa) {
	public Calificacion {
		Objects.requireNonNull(escala, "La escala es obligatoria");
		if (escala == EscalaCalificacion.VIGESIMAL) {
			Objects.requireNonNull(vigesimal, "La nota vigesimal es obligatoria");
			if (cualitativa != null || vigesimal.compareTo(BigDecimal.ZERO) < 0
				|| vigesimal.compareTo(new BigDecimal("20")) > 0) {
				throw new IllegalArgumentException("La nota vigesimal debe estar entre 0 y 20");
			}
		} else if (cualitativa == null || vigesimal != null) {
			throw new IllegalArgumentException("Se requiere un valor cualitativo AD, A, B o C");
		}
	}

	public static Calificacion vigesimal(BigDecimal nota) {
		return new Calificacion(EscalaCalificacion.VIGESIMAL, nota, null);
	}

	public static Calificacion cualitativa(NivelCualitativo nivel) {
		return new Calificacion(EscalaCalificacion.CUALITATIVA, null, nivel);
	}
}
