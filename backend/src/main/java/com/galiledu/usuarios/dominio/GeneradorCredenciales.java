package com.galiledu.usuarios.dominio;

import java.security.SecureRandom;

/** Formato de usuario de RF-15 y contraseña aleatoria de RF-16. */
public final class GeneradorCredenciales {
	private static final SecureRandom AZAR = new SecureRandom();
	private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
	private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
	private static final String DIGITOS = "23456789";
	private static final String SIMBOLOS = "@#$%&*!?";
	private static final int LONGITUD = 10;

	private GeneradorCredenciales() {
	}

	/** U{año}{correlativo de 4 dígitos}, por ejemplo U20260008. */
	public static String nombreUsuario(int anio, int correlativo) {
		if (anio < 1000 || anio > 9999) {
			throw new IllegalArgumentException("El año debe tener cuatro dígitos");
		}
		if (correlativo < 1 || correlativo > 9999) {
			throw new IllegalArgumentException("El correlativo debe estar entre 1 y 9999");
		}
		return "U%d%04d".formatted(anio, correlativo);
	}

	/** Contraseña de 10 caracteres con al menos una mayúscula, minúscula, dígito y símbolo. */
	public static String contrasena() {
		String todos = MAYUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;
		char[] c = new char[LONGITUD];
		c[0] = elegir(MAYUSCULAS);
		c[1] = elegir(MINUSCULAS);
		c[2] = elegir(DIGITOS);
		c[3] = elegir(SIMBOLOS);
		for (int i = 4; i < LONGITUD; i++) {
			c[i] = elegir(todos);
		}
		for (int i = LONGITUD - 1; i > 0; i--) {
			int j = AZAR.nextInt(i + 1);
			char t = c[i];
			c[i] = c[j];
			c[j] = t;
		}
		return new String(c);
	}

	private static char elegir(String fuente) {
		return fuente.charAt(AZAR.nextInt(fuente.length()));
	}
}
