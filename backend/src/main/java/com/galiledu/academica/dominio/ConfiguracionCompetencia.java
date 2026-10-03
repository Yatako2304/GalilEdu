package com.galiledu.academica.dominio;

import java.util.Objects;

/** Escala y reglas de cálculo elegidas para una competencia de una oferta de curso. */
public record ConfiguracionCompetencia(EscalaCalificacion escala, ReglaAgregacionItems reglaAgregacion,
	ReglaConsolidacionPeriodos reglaConsolidacion) {
	public ConfiguracionCompetencia {
		Objects.requireNonNull(escala, "La escala es obligatoria");
		Objects.requireNonNull(reglaAgregacion, "La regla de agregación es obligatoria");
		Objects.requireNonNull(reglaConsolidacion, "La regla de consolidación es obligatoria");
	}

	/** Un promedio de niveles AD/A/B/C requeriría una equivalencia numérica no aprobada. */
	public boolean admiteCalculo() {
		return escala == EscalaCalificacion.VIGESIMAL
			|| (reglaAgregacion != ReglaAgregacionItems.PROMEDIO_SIMPLE
				&& reglaAgregacion != ReglaAgregacionItems.PROMEDIO_PONDERADO
				&& reglaConsolidacion != ReglaConsolidacionPeriodos.PROMEDIO_PERIODOS);
	}
}
