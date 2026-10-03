package com.galiledu.academica.dominio;

import java.util.Objects;

/** Resultado calculado de una competencia, con su equivalente cualitativo si es vigesimal. */
public record NotaConsolidada(Calificacion valor, NivelCualitativo equivalenteCualitativo) {
	public NotaConsolidada {
		Objects.requireNonNull(valor, "El valor es obligatorio");
		if (equivalenteCualitativo != null && valor.escala() != EscalaCalificacion.VIGESIMAL) {
			throw new IllegalArgumentException("Solo una nota vigesimal tiene equivalente cualitativo");
		}
	}
}
