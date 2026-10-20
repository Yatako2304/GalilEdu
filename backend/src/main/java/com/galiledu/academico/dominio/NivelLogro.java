package com.galiledu.academico.dominio;

/**
 * Niveles de logro de la escala cualitativa (ck_tabla_conversion_nivel), de menor a mayor, con el significado que
 * muestra Figma CI-23.
 */
public enum NivelLogro {
	C("En inicio"), B("En proceso"), A("Logro esperado"), AD("Logro destacado");

	private final String significado;

	NivelLogro(String significado) {
		this.significado = significado;
	}

	public String significado() {
		return significado;
	}
}
