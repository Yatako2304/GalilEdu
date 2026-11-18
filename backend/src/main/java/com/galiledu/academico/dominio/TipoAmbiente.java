package com.galiledu.academico.dominio;

/**
 * Tipo de ambiente físico (ck_ambiente_tipo). El prefijo es el del código que genera el sistema: A-001 para un aula
 * regular, E-001 para un espacio especial.
 */
public enum TipoAmbiente {
	AULA_REGULAR("A"), ESPACIO_ESPECIAL("E");

	/** "Un aula regular admite como máximo 45" (ck_ambiente_aforo). */
	public static final int AFORO_MAXIMO_AULA_REGULAR = 45;

	private final String prefijo;

	TipoAmbiente(String prefijo) {
		this.prefijo = prefijo;
	}

	public String prefijo() {
		return prefijo;
	}

	public boolean esAulaRegular() {
		return this == AULA_REGULAR;
	}
}
