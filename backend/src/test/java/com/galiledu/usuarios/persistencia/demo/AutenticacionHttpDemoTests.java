package com.galiledu.usuarios.persistencia.demo;

import java.time.Clock;
import java.time.Year;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.CuentaUsuario;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.GeneradorNombreUsuario;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.dominio.TipoDocumento;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "DEMO_ADMIN_TEMP_PASSWORD=ClaveDePrueba123!")
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class AutenticacionHttpDemoTests {
	@Autowired
	private MockMvc mvc;

	@Autowired
	private RepositorioUsuariosDemo usuarios;

	@Autowired
	private ServicioContrasenas contrasenas;

	@Autowired
	private Clock reloj;

	private String nombreUsuario;

	@BeforeEach
	void prepararCuenta() {
		nombreUsuario = GeneradorNombreUsuario.generar(Year.now(reloj).getValue(), 1);
		DatosPersonales datos = new DatosPersonales("Admin", "Demostración", "Local",
			TipoDocumento.DNI, "00000000", "admin@demo.invalid", "000000000");
		usuarios.reiniciar(CuentaUsuario.nueva(datos, nombreUsuario,
			contrasenas.codificar("temporal"), Set.of(new Rol("ADMINISTRADOR"))));
	}

	@Test
	void loginCambioObligatorioYConsultaDeCuenta() throws Exception {
		var respuestaCsrf = mvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk()).andReturn();
		MockHttpSession sesion = (MockHttpSession) respuestaCsrf.getRequest().getSession(false);
		String token = JsonPath.read(respuestaCsrf.getResponse().getContentAsString(), "$.token");
		var login = mvc.perform(post("/api/auth/login").session(sesion)
			.header("X-CSRF-TOKEN", token)
			.contentType("application/json")
			.content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"temporal\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cambioContrasenaObligatorio").value(true))
			.andReturn();
		sesion = (MockHttpSession) login.getRequest().getSession(false);

		mvc.perform(get("/api/auth/me").session(sesion))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cambioContrasenaObligatorio").value(true));
		mvc.perform(post("/api/auth/initial-password").session(sesion).with(csrf())
			.contentType("application/json")
			.content("{\"contrasenaTemporal\":\"temporal\",\"nuevaContrasena\":\"definitiva\"}"))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/auth/me").session(sesion))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.cambioContrasenaObligatorio").value(false));
	}

	@Test
	void loginNecesitaTokenCsrf() throws Exception {
		mvc.perform(post("/api/auth/login").contentType("application/json")
			.content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"temporal\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void cincoFallosBloqueanLaCuenta() throws Exception {
		for (int intento = 1; intento <= 4; intento++) {
			mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
				.content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"erronea\"}"))
				.andExpect(status().isUnauthorized());
		}
		mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
			.content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasena\":\"erronea\"}"))
			.andExpect(status().isLocked())
			.andExpect(jsonPath("$.segundosRestantes").value(900));
	}
}
