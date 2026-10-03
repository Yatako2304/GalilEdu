package com.galiledu.academica.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/** Tramo de la tabla que convierte una nota vigesimal en un nivel cualitativo. */
public record RangoConversion(BigDecimal notaDesde, BigDecimal notaHasta, NivelCualitativo valor) {
	public RangoConversion {
		Objects.requireNonNull(notaDesde, "notaDesde es obligatoria");
		Objects.requireNonNull(notaHasta, "notaHasta es obligatoria");
		Objects.requireNonNull(valor, "El valor cualitativo es obligatorio");
		if (notaDesde.signum() < 0 || notaHasta.compareTo(new BigDecimal("20")) > 0
			|| notaHasta.compareTo(notaDesde) < 0) {
			throw new IllegalArgumentException("El rango debe estar entre 0 y 20 y ser ascendente");
		}
	}

	public boolean contiene(BigDecimal nota) {
		return nota.compareTo(notaDesde) >= 0 && nota.compareTo(notaHasta) <= 0;
	}
}
