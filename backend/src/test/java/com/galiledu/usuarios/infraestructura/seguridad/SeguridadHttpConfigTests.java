package com.galiledu.usuarios.infraestructura.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadHttpConfigTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PasswordEncoder codificador;

	@Test
	void saludEsPublica() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void otrasRutasEstanCerradas() throws Exception {
		mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
	}

	@Test
	void contrasenasSeCodificanConBcryptCostoDiez() {
		String hash = codificador.encode("clave-de-prueba");
		assertThat(hash).matches("^\\$2[aby]\\$10\\$.*");
		assertThat(codificador.matches("clave-de-prueba", hash)).isTrue();
		assertThat(codificador.matches("incorrecta", hash)).isFalse();
	}
}
