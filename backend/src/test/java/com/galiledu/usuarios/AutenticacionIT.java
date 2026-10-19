package com.galiledu.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/** HU-1: inicio de sesión (RF-1, RF-2, RF-3). */
class AutenticacionIT extends PruebaIntegracion {

	private ResultActions login(String usuario, String contrasena) throws Exception {
		return mvc.perform(post("/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"username":"%s","password":"%s"}""".formatted(usuario, contrasena)));
	}

	@Test
	void loginCorrectoDevuelveTokenYRoles() throws Exception {
		login(ADMIN, CONTRASENA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.jwt").isNotEmpty())
			.andExpect(jsonPath("$.user.username").value(ADMIN))
			.andExpect(jsonPath("$.user.roles[0]").value("ADMINISTRADOR"));
	}

	@Test
	void elUsuarioNoDistingueMayusculas() throws Exception {
		login("u20260001", CONTRASENA).andExpect(status().isOk());
	}

	@Test
	void contrasenaIncorrectaDa401ConMensajeDeFigma() throws Exception {
		login(ADMIN, "incorrecta")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("El usuario o contraseña son incorrectos."));
	}

	@Test
	void usuarioInexistenteDaElMismoMensaje() throws Exception {
		login("U20999999", CONTRASENA)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("El usuario o contraseña son incorrectos."));
	}

	@Test
	void cuentaInactivaNoPuedeEntrar() throws Exception {
		login("U20260007", CONTRASENA)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("desactivada")));
	}

	@Test
	void seBloqueaAlQuintoIntentoFallido() throws Exception {
		for (int i = 0; i < 4; i++) {
			login("U20260005", "mal").andExpect(status().isUnauthorized());
		}
		login("U20260005", "mal").andExpect(status().isLocked());
		// Bloqueada aunque ahora la contraseña sea correcta.
		login("U20260005", CONTRASENA)
			.andExpect(status().isLocked())
			.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("15 minutos")));
	}

	@Test
	void camposVaciosDan400() throws Exception {
		login("", "").andExpect(status().isBadRequest());
	}

	@Test
	void meSinTokenDa401() throws Exception {
		mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void meConTokenDevuelveElUsuario() throws Exception {
		mvc.perform(conToken(get("/auth/me"), token(DOCENTE)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.username").value(DOCENTE))
			.andExpect(jsonPath("$.data.roles.length()").value(2));
	}
}
