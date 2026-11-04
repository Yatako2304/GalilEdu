package com.galiledu.academico.dominio;

/** Regla global del año escolar para calcular la nota. Cada regla corresponde a una escala. */
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
