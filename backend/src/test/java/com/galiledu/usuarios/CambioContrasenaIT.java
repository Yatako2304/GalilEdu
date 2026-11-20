package com.galiledu.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/** HU-5 (Figma › Autenticación › Cambiar contraseña): contraseña actual → nueva → se cierran las sesiones. */
class CambioContrasenaIT extends PruebaIntegracion {
	private static final String NUEVA = "Nueva#2026";

	private ResultActions verificar(String token, String actual) throws Exception {
		return mvc.perform(conToken(post("/auth/cambiar-contrasena/verificar"), token)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"contrasenaActual":"%s"}""".formatted(actual)));
	}

	private ResultActions cambiar(String token, String actual, String nueva) throws Exception {
		return mvc.perform(conToken(post("/auth/cambiar-contrasena"), token)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"contrasenaActual":"%s","contrasenaNueva":"%s"}""".formatted(actual, nueva)));
	}

	private ResultActions login(String contrasena) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"username":"%s","password":"%s"}""".formatted(DOCENTE, contrasena)));
	}

	@DisplayName("Con la contraseña actual correcta se puede continuar")
	@Test
	void conLaContrasenaActualCorrectaSePuedeContinuar() throws Exception {
		verificar(token(DOCENTE), CONTRASENA).andExpect(status().isOk());
	}

	@DisplayName("Una contraseña actual incorrecta da el mensaje de figma")
	@Test
	void unaContrasenaActualIncorrectaDaElMensajeDeFigma() throws Exception {
		verificar(token(DOCENTE), "Incorrecta#1").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Contraseña incorrecta."));
	}

	@DisplayName("Al cambiarla entra con la nueva y se cierran las sesiones abiertas")
	@Test
	void alCambiarlaEntraConLaNuevaYSeCierranLasSesionesAbiertas() throws Exception {
		String anterior = token(DOCENTE);
		// La fecha de emisión del token está en segundos: así el cambio queda claramente después.
		Thread.sleep(1100);
		cambiar(anterior, CONTRASENA, NUEVA).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Su contraseña ha sido actualizada correctamente."));

		mvc.perform(conToken(get("/auth/me"), anterior)).andExpect(status().isUnauthorized());
		login(CONTRASENA).andExpect(status().isUnauthorized());
		login(NUEVA).andExpect(status().isOk());
	}

	@DisplayName("La nueva debe cumplir la regla y ser distinta de la actual")
	@Test
	void laNuevaDebeCumplirLaReglaYSerDistintaDeLaActual() throws Exception {
		String token = token(DOCENTE);
		cambiar(token, CONTRASENA, "passw123").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(Matchers.startsWith("Mínimo 8 caracteres")));
		cambiar(token, CONTRASENA, CONTRASENA).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La nueva contraseña debe ser diferente de la actual."));
		login(CONTRASENA).andExpect(status().isOk());
	}

	@DisplayName("Cambiar vuelve a comprobar la contraseña actual")
	@Test
	void cambiarVuelveAComprobarLaContrasenaActual() throws Exception {
		cambiar(token(DOCENTE), "Incorrecta#1", NUEVA).andExpect(status().isBadRequest());
		login(NUEVA).andExpect(status().isUnauthorized());
	}

	@DisplayName("Al quinto intento fallido se bloquea como en el inicio de sesión")
	@Test
	void alQuintoIntentoFallidoSeBloqueaComoEnElInicioDeSesion() throws Exception {
		String token = token(DOCENTE);
		for (int i = 1; i <= 4; i++) {
			verificar(token, "Incorrecta#1").andExpect(status().isBadRequest());
		}
		verificar(token, "Incorrecta#1").andExpect(status().isLocked());
		login(CONTRASENA).andExpect(status().isLocked());
	}

	@DisplayName("Sin sesión no se puede")
	@Test
	void sinSesionNoSePuede() throws Exception {
		mvc.perform(post("/auth/cambiar-contrasena/verificar").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contrasenaActual":"x"}""")).andExpect(status().isUnauthorized());
	}
}
