package com.galiledu.usuarios.dominio;

/** Formato de RF-15; la asignación atómica del correlativo queda a cargo de persistencia. */
public final class GeneradorNombreUsuario {
	private GeneradorNombreUsuario() {
	}

	public static String generar(int anioIngreso, int correlativo) {
		if (anioIngreso < 1000 || anioIngreso > 9999) {
			throw new IllegalArgumentException("El año de ingreso debe tener cuatro dígitos");
		}
		if (correlativo < 1 || correlativo > 9999) {
			throw new IllegalArgumentException("El correlativo debe estar entre 1 y 9999");
		}
		return "U%d%04d".formatted(anioIngreso, correlativo);
	}
}
