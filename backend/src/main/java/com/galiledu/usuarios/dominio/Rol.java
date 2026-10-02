package com.galiledu.usuarios.dominio;

/** Identificador de un rol del catálogo; el catálogo persistente verificará su existencia. */
public record Rol(String codigo) {
	public Rol {
		if (codigo == null || codigo.isBlank()) {
			throw new IllegalArgumentException("El código de rol es obligatorio");
		}
		codigo = codigo.strip();
	}
}
