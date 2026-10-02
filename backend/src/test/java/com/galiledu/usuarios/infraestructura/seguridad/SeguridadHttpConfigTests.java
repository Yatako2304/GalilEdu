package com.galiledu.usuarios.infraestructura.seguridad;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.galiledu.configuracion.aplicacion.GestionCatalogoCurricular;
import com.galiledu.configuracion.aplicacion.puertos.RepositorioCatalogoCurricular;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.infraestructura.web.CredencialesController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadHttpConfigTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private PasswordEncoder codificador;

	@Autowired
	private ServicioContrasenas servicioContrasenas;

	@MockitoBean
	private GestionCatalogoCurricular gestionCatalogo;

	@Test
	void saludEsPublica() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void tokenCsrfEsPublico() throws Exception {
		mvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.headerName").isNotEmpty())
			.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void otrasRutasEstanCerradas() throws Exception {
		mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void catalogoCurricularSoloAdmitePersonalAdministrativoOAdministrador() throws Exception {
		var ruta = "/api/configuracion/areas-curriculares";
		mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
		mvc.perform(get(ruta).with(user("docente").roles("DOCENTE")))
			.andExpect(status().isForbidden());
		mvc.perform(get(ruta).with(user("coordinador").roles("COORDINADOR")))
			.andExpect(status().isForbidden());
		mvc.perform(get(ruta).with(user("administrativo").roles("PERSONAL_ADMINISTRATIVO")))
			.andExpect(status().isOk());
		mvc.perform(get(ruta).with(user("administrador").roles("ADMINISTRADOR")))
			.andExpect(status().isOk());
		mvc.perform(post(ruta).with(user("administrativo").roles("PERSONAL_ADMINISTRATIVO")))
			.andExpect(status().isForbidden());
	}

	@Test
	void altaAreaCurricularRespondeCreatedSinDuplicarRutaPorRol() throws Exception {
		UUID id = UUID.randomUUID();
		when(gestionCatalogo.crearArea(any())).thenReturn(
			new RepositorioCatalogoCurricular.AreaCurricular(id, "Matemática", "Área curricular", true));
		mvc.perform(post("/api/configuracion/areas-curriculares")
			.with(user("administrativo").roles("PERSONAL_ADMINISTRATIVO"))
			.with(csrf())
			.contentType("application/json")
			.content("{\"nombre\":\"Matemática\",\"descripcion\":\"Área curricular\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(id.toString()))
			.andExpect(jsonPath("$.activo").value(true));
	}

	@Test
	void loginRestExigeCsrfYNoFiltraClavesEnDepuracion() throws Exception {
		mvc.perform(post("/api/auth/login")).andExpect(status().isForbidden());
		var solicitud = new com.galiledu.usuarios.infraestructura.web.InicioSesionController
			.SolicitudAcceso("usuario", "clave-secreta");
		assertThat(solicitud.toString()).doesNotContain("clave-secreta");
	}

	@ParameterizedTest
	@ValueSource(strings = {"DOCENTE", "ESTUDIANTE", "APODERADO", "TUTOR",
		"COORDINADOR", "PERSONAL_ADMINISTRATIVO"})
	void otrosRolesVenSuCuentaPeroNoLaGestionAdministrativa(String rol) throws Exception {
		mvc.perform(get("/api/auth/me").with(user("cuenta").roles(rol)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombreUsuario").value("cuenta"));
		mvc.perform(get("/api/personas/00000000-0000-0000-0000-000000000001")
			.with(user("cuenta").roles(rol)))
			.andExpect(status().isForbidden());
		mvc.perform(put("/api/auth/password").with(user("cuenta").roles(rol)))
			.andExpect(status().isForbidden());
	}

	@Test
	void contrasenasSeCodificanConBcryptCostoDiez() {
		String hash = codificador.encode("clave-de-prueba");
		assertThat(hash).matches("^\\$2[aby]\\$10\\$.*");
		assertThat(codificador.matches("clave-de-prueba", hash)).isTrue();
		assertThat(codificador.matches("incorrecta", hash)).isFalse();
	}

	@Test
	void adaptadorDeContrasenasImplementaElPuertoDeAplicacion() {
		String hash = servicioContrasenas.codificar("clave-de-prueba");
		assertThat(servicioContrasenas.coincide("clave-de-prueba", hash)).isTrue();
		assertThat(servicioContrasenas.coincide("incorrecta", hash)).isFalse();
	}

	@Test
	void solicitudHttpNoExponeClavesEnTextoDeDepuracion() {
		var solicitud = new CredencialesController.CambioContrasenaRequest("anterior", "nueva");
		assertThat(solicitud.toString()).doesNotContain("anterior", "nueva");
	}
}
