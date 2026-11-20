package com.galiledu.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PoliticaContrasenaTests {

	@ParameterizedTest
	@ValueSource(strings = {"Passw1234#", "Aa1@aaaa", "Clave$2026", "Ñandú#2026"})
	void aceptaLasQueCumplenLaReglaDeFigma(String contrasena) {
		assertThat(PoliticaContrasena.problema(contrasena)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {"passw123", "PASSW123#", "passw123#", "Password#", "Pass1#", "Password12"})
	void rechazaLasQueNoCumplen(String contrasena) {
		assertThat(PoliticaContrasena.problema(contrasena)).contains(PoliticaContrasena.MENSAJE);
	}

	@Test
	void noAdmiteEspaciosNiMasDe64Caracteres() {
		assertThat(PoliticaContrasena.problema("Clave #2026")).contains("La contraseña no puede tener espacios.");
		assertThat(PoliticaContrasena.problema("Aa1#" + "a".repeat(61))).get().asString().contains("64");
	}

	@Test
	void unaVaciaONulaNoCumple() {
		assertThat(PoliticaContrasena.problema(null)).isPresent();
		assertThat(PoliticaContrasena.problema("")).isPresent();
	}
}
