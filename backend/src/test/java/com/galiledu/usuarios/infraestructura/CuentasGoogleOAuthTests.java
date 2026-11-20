package com.galiledu.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.galiledu.compartido.web.ExcepcionNegocio;

/** HU-3: sin las dos llaves de Google el inicio de sesión con Google no está disponible. */
class CuentasGoogleOAuthTests {

	@DisplayName("Con id de cliente y clave secreta está configurado")
	@Test
	void conIdDeClienteYClaveSecretaEstaConfigurado() {
		var google = new CuentasGoogleOAuth(" id.apps.googleusercontent.com ", "GOCSPX-secreto");
		assertThat(google.configurado()).isTrue();
		assertThat(google.idCliente()).isEqualTo("id.apps.googleusercontent.com");
	}

	@DisplayName("Sin alguna de las llaves no está configurado")
	@Test
	void sinAlgunaDeLasLlavesNoEstaConfigurado() {
		assertThat(new CuentasGoogleOAuth("", "GOCSPX-secreto").configurado()).isFalse();
		assertThat(new CuentasGoogleOAuth("id.apps.googleusercontent.com", " ").configurado()).isFalse();
		assertThat(new CuentasGoogleOAuth(null, null).configurado()).isFalse();
	}

	@DisplayName("Sin configurar no llama a google")
	@Test
	void sinConfigurarNoLlamaAGoogle() {
		var google = new CuentasGoogleOAuth("", "");
		assertThatThrownBy(() -> google.cuentaDe("codigo"))
				.isInstanceOfSatisfying(ExcepcionNegocio.class,
						e -> assertThat(e.estado()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
	}
}
