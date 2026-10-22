package com.galiledu.configuracion.dominio;

/** Días en que puede haber clases, en el orden de la semana. Se guardan por su código de 3 letras (LUN…SAB). */
public enum DiaSemana {
	LUN, MAR, MIE, JUE, VIE, SAB;

	public static boolean esCodigoValido(String codigo) {
		for (DiaSemana dia : values()) {
			if (dia.name().equals(codigo)) {
				return true;
			}
		}
		return false;
	}
}
