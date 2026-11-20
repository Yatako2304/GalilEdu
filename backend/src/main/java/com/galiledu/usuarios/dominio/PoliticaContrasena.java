package com.galiledu.usuarios.dominio;

import java.util.Optional;

/**
 * Regla de Figma para las contraseñas que elige el usuario (HU-4 y HU-5): «Mínimo 8 caracteres, con al menos una
 * mayúscula, una minúscula, un número y un carácter especial (@, #, $)». El frontend aplica la misma regla en
 * {@code src/lib/contrasena.ts}.
 */
public final class PoliticaContrasena {
	public static final String MENSAJE = "Mínimo 8 caracteres, con al menos una mayúscula, una minúscula, un número y "
			+ "un carácter especial (@, #, $).";
	/** BCrypt solo usa los primeros 72 bytes: se limita para que toda la contraseña cuente. */
	public static final int MAXIMO = 64;

	private PoliticaContrasena() {
	}

	/** El mensaje de error, o vacío si la contraseña cumple la regla. */
	public static Optional<String> problema(String contrasena) {
		if (contrasena == null || contrasena.length() < 8 || !contrasena.chars().anyMatch(Character::isUpperCase)
				|| !contrasena.chars().anyMatch(Character::isLowerCase)
				|| !contrasena.chars().anyMatch(Character::isDigit)
				|| contrasena.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
			return Optional.of(MENSAJE);
		}
		if (contrasena.length() > MAXIMO) {
			return Optional.of("La contraseña puede tener como máximo " + MAXIMO + " caracteres.");
		}
		if (contrasena.chars().anyMatch(Character::isWhitespace)) {
			return Optional.of("La contraseña no puede tener espacios.");
		}
		return Optional.empty();
	}
}
