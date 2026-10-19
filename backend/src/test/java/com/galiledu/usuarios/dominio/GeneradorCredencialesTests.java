package com.galiledu.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GeneradorCredencialesTests {

	@Test
	void nombreUsuarioSigueElFormatoDeRf15() {
		assertThat(GeneradorCredenciales.nombreUsuario(2026, 8)).isEqualTo("U20260008");
		assertThat(GeneradorCredenciales.nombreUsuario(2026, 1234)).isEqualTo("U20261234");
	}

	@Test
	void rechazaCorrelativosFueraDeRango() {
		assertThatThrownBy(() -> GeneradorCredenciales.nombreUsuario(2026, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> GeneradorCredenciales.nombreUsuario(2026, 10000)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void contrasenaTieneMayusculaMinusculaDigitoYSimbolo() {
		for (int i = 0; i < 50; i++) {
			String c = GeneradorCredenciales.contrasena();
			assertThat(c).hasSize(10).matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[0-9].*").matches(".*[@#$%&*!?].*");
		}
	}
}
