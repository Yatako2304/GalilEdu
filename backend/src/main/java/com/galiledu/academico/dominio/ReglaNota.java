package com.galiledu.academico.dominio;

/**
 * Cómo se agregan las notas de los ítems de una competencia (Figma: "Agregación de ítems"). Cada regla vale para una
 * escala (ck_plan_competencia_regla).
 */
public enum ReglaNota {
	PROMEDIO_SIMPLE(Escala.VIGESIMAL), PROMEDIO_PONDERADO(Escala.VIGESIMAL), NIVEL_MAS_FRECUENTE(
			Escala.CUALITATIVA), ULTIMO_NIVEL(Escala.CUALITATIVA);

	private final Escala escala;

	ReglaNota(Escala escala) {
		this.escala = escala;
	}

	public boolean sirvePara(Escala otra) {
		return escala == otra;
	}
}
