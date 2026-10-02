package com.galiledu.usuarios.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneradorNombreUsuarioTests {
	@Test
	void formateaCuatroDigitosPorAnio() {
		assertThat(GeneradorNombreUsuario.generar(2026, 1)).isEqualTo("U20260001");
		assertThat(GeneradorNombreUsuario.generar(2026, 42)).isEqualTo("U20260042");
		assertThat(GeneradorNombreUsuario.generar(2027, 1)).isEqualTo("U20270001");
	}

	@Test
	void noAceptaCorrelativosFueraDelFormato() {
		assertThatThrownBy(() -> GeneradorNombreUsuario.generar(2026, 0))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> GeneradorNombreUsuario.generar(2026, 10000))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
