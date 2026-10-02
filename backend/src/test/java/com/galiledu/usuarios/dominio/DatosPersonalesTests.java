package com.galiledu.usuarios.dominio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatosPersonalesTests {
	private static DatosPersonales crear(TipoDocumento tipo, String documento) {
		return new DatosPersonales("María", "López", "Quispe", tipo, documento, "maria@example.com", "987654321");
	}

	@Test
	void aceptaDniYCarneDeExtranjeria() {
		assertThat(crear(TipoDocumento.DNI, "12345678").numeroDocumento()).isEqualTo("12345678");
		assertThat(crear(TipoDocumento.CARNET_EXTRANJERIA, "AB1234567890").numeroDocumento())
			.isEqualTo("AB1234567890");
	}

	@Test
	void rechazaDocumentosFueraDeFormato() {
		assertThatThrownBy(() -> crear(TipoDocumento.DNI, "1234567"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> crear(TipoDocumento.DNI, "A2345678"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> crear(TipoDocumento.CARNET_EXTRANJERIA, "AB12345678901"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rechazaNombresNumericosYCamposObligatoriosVacios() {
		assertThatThrownBy(() -> new DatosPersonales("María2", "López", "Quispe", TipoDocumento.DNI,
			"12345678", "maria@example.com", "987654321"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DatosPersonales("  ", "López", "Quispe", TipoDocumento.DNI,
			"12345678", "maria@example.com", "987654321"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rechazaCorreoYTelefonoInvalidos() {
		assertThatThrownBy(() -> new DatosPersonales("María", "López", "Quispe", TipoDocumento.DNI,
			"12345678", "correo-invalido", "987654321"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new DatosPersonales("María", "López", "Quispe", TipoDocumento.DNI,
			"12345678", "maria@example.com", "98765432"))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
