package com.galiledu.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.galiledu.compartido.web.ExcepcionNegocio;

/** HU-3: sin las dos llaves de Google el inicio de sesión con Google no está disponible. */
class CuentasGoogleOAuthTests {

	@Test
	void conIdDeClienteYClaveSecretaEstaConfigurado() {
		var google = new CuentasGoogleOAuth(" id.apps.googleusercontent.com ", "GOCSPX-secreto");
		assertThat(google.configurado()).isTrue();
		assertThat(google.idCliente()).isEqualTo("id.apps.googleusercontent.com");
	}

	@Test
	void sinAlgunaDeLasLlavesNoEstaConfigurado() {
		assertThat(new CuentasGoogleOAuth("", "GOCSPX-secreto").configurado()).isFalse();
		assertThat(new CuentasGoogleOAuth("id.apps.googleusercontent.com", " ").configurado()).isFalse();
		assertThat(new CuentasGoogleOAuth(null, null).configurado()).isFalse();
	}

	@Test
	void sinConfigurarNoLlamaAGoogle() {
		var google = new CuentasGoogleOAuth("", "");
		assertThatThrownBy(() -> google.cuentaDe("codigo"))
				.isInstanceOfSatisfying(ExcepcionNegocio.class,
						e -> assertThat(e.estado()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
	}
}
