package com.galiledu.configuracion.dominio;

/** Regla única con la que se obtiene la nota de cada competencia durante el año escolar. */
public enum ReglaNota {
	PROMEDIO_SIMPLE(EscalaEvaluacion.VIGESIMAL), PROMEDIO_PONDERADO(EscalaEvaluacion.VIGESIMAL), NIVEL_MAS_FRECUENTE(
			EscalaEvaluacion.CUALITATIVA), ULTIMO_NIVEL(EscalaEvaluacion.CUALITATIVA);

	private final EscalaEvaluacion escala;

	ReglaNota(EscalaEvaluacion escala) {
		this.escala = escala;
	}

	public boolean sirvePara(EscalaEvaluacion escala) {
		return this.escala == escala;
	}
}
