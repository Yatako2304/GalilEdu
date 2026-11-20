package com.galiledu.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("Login correcto devuelve token y roles")
	@Test
	void loginCorrectoDevuelveTokenYRoles() throws Exception {
		login(ADMIN, CONTRASENA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jwt").isNotEmpty())
				.andExpect(jsonPath("$.user.username").value(ADMIN))
				.andExpect(jsonPath("$.user.roles[0]").value("ADMINISTRADOR"));
	}

	@DisplayName("El usuario no distingue mayúsculas")
	@Test
	void elUsuarioNoDistingueMayusculas() throws Exception {
		login("u20260001", CONTRASENA).andExpect(status().isOk());
	}

	@DisplayName("Contraseña incorrecta da401 con mensaje de figma")
	@Test
	void contrasenaIncorrectaDa401ConMensajeDeFigma() throws Exception {
		login(ADMIN, "incorrecta")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("El usuario o contraseña son incorrectos."));
	}

	@DisplayName("Usuario inexistente da el mismo mensaje")
	@Test
	void usuarioInexistenteDaElMismoMensaje() throws Exception {
		login("U20999999", CONTRASENA)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("El usuario o contraseña son incorrectos."));
	}

	@DisplayName("Cuenta inactiva no puede entrar")
	@Test
	void cuentaInactivaNoPuedeEntrar() throws Exception {
		login("U20260007", CONTRASENA)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("desactivada")));
	}

	@DisplayName("Se bloquea al quinto intento fallido")
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

	@DisplayName("Campos vacios dan400")
	@Test
	void camposVaciosDan400() throws Exception {
		login("", "").andExpect(status().isBadRequest());
	}

	@DisplayName("Me sin token da401")
	@Test
	void meSinTokenDa401() throws Exception {
		mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
	}

	@DisplayName("Me con token devuelve el usuario")
	@Test
	void meConTokenDevuelveElUsuario() throws Exception {
		mvc.perform(conToken(get("/auth/me"), token(DOCENTE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value(DOCENTE))
				.andExpect(jsonPath("$.data.roles.length()").value(2));
	}
}
