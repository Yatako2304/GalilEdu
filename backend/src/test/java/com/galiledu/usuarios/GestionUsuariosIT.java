package com.galiledu.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/** HU-6 (registro), HU-9 (edición), HU-12 (desactivación) y permisos. */
class GestionUsuariosIT extends PruebaIntegracion {
	private static final String ID_ADMIN = "10000000-0000-0000-0000-000000000001";
	private static final String ID_MARIA = "10000000-0000-0000-0000-000000000004";
	private static final String ID_ANA = "10000000-0000-0000-0000-000000000002";

	private String admin;

	@BeforeEach
	void iniciarSesion() throws Exception {
		admin = token(ADMIN);
	}

	private static String usuario(String dni, String nombres, String correo, String roles) {
		return """
				{"persona":{"tipoDocumento":"DNI","numeroDocumento":"%s","apellidoPaterno":"Pérez",
				"apellidoMaterno":"Quispe","nombres":"%s","celular":"999888777","correo":"%s"},
				"roles":%s}""".formatted(dni, nombres, correo, roles);
	}

	private String crear(String json) throws Exception {
		return mvc.perform(conToken(post("/usuarios"), admin, json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void creaUsuarioConNombreGeneradoYVariosRoles() throws Exception {
		String r = crear(usuario("45678912", "Ana Paula", "aperez@galiledu.edu.pe", "[\"DOCENTE\",\"TUTOR\"]"));
		String id = JsonPath.read(r, "$.id");
		org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(r, "$.username")).matches("U\\d{8}");

		mvc.perform(conToken(get("/usuarios/" + id), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.roles.length()").value(2))
				.andExpect(jsonPath("$.persona.apellidoPaterno").value("Pérez"));
	}

	@Test
	void rechazaDocumentoDuplicado() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin, usuario("70000002", "Otra", "otra@gmail.com", "[\"DOCENTE\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("70000002")));
	}

	@Test
	void rechazaCorreoDuplicadoSinImportarMayusculas() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678900", "Otra", "ANA.TORRES@galiledu.edu.pe", "[\"DOCENTE\"]")))
				.andExpect(status().isConflict());
	}

	@Test
	void validaFormatoDeDatos() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin, usuario("4567", "Ana", "a@b.pe", "[\"DOCENTE\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El DNI debe tener 8 dígitos"));
		mvc.perform(conToken(post("/usuarios"), admin, usuario("45678913", "Ana 2", "a@b.pe", "[\"DOCENTE\"]")))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(post("/usuarios"), admin, usuario("45678914", "Ana", "a@b.pe", "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Asigna al menos un rol"));
	}

	@Test
	void editaDatosYRoles() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, """
				{"persona":{"tipoDocumento":"DNI","numeroDocumento":"70000004","apellidoPaterno":"Fernández",
				"nombres":"María","celular":"911222333","correo":"maria.fernandez@galiledu.edu.pe"},
				"roles":["DOCENTE"]}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.persona.celular").value("911222333"))
				.andExpect(jsonPath("$.roles.length()").value(1));
	}

	@Test
	void editarConCorreoDeOtroUsuarioDa409() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, """
				{"persona":{"tipoDocumento":"DNI","numeroDocumento":"70000004","apellidoPaterno":"Fernández",
				"nombres":"María","celular":"911222333","correo":"ana.torres@galiledu.edu.pe"},
				"roles":["DOCENTE"]}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void desactivadoNoPuedeEntrarYReactivadoSi() throws Exception {
		mvc.perform(conToken(post("/usuarios/" + ID_MARIA + "/desactivar"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVO"));
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(DOCENTE, CONTRASENA)))
				.andExpect(status().isUnauthorized());

		mvc.perform(conToken(post("/usuarios/" + ID_MARIA + "/reactivar"), admin))
				.andExpect(jsonPath("$.estado").value("ACTIVO"));
		token(DOCENTE);
	}

	@Test
	void noPuedeDesactivarseNiEliminarseASiMismo() throws Exception {
		mvc.perform(conToken(post("/usuarios/" + ID_ADMIN + "/desactivar"), admin)).andExpect(status().isBadRequest());
		mvc.perform(conToken(delete("/usuarios/" + ID_ADMIN), admin)).andExpect(status().isBadRequest());
	}

	@Test
	void eliminarEsLogicoYDesapareceDeLaLista() throws Exception {
		mvc.perform(conToken(delete("/usuarios/" + ID_MARIA), admin)).andExpect(status().isNoContent());
		mvc.perform(conToken(get("/usuarios/" + ID_MARIA), admin)).andExpect(status().isNotFound());
		mvc.perform(conToken(get("/usuarios").param("q", "fernandez"), admin))
				.andExpect(jsonPath("$.meta.total").value(0));
	}

	@Test
	void listaFiltraPorRolEstadoYTexto() throws Exception {
		mvc.perform(conToken(get("/usuarios").param("rol", "TUTOR"), admin))
				.andExpect(jsonPath("$.meta.total").value(1))
				.andExpect(jsonPath("$.data[0].username").value(DOCENTE));
		mvc.perform(conToken(get("/usuarios").param("estado", "INACTIVO"), admin))
				.andExpect(jsonPath("$.data[0].username").value("U20260007"));
		mvc.perform(conToken(get("/usuarios").param("q", "ana.torres"), admin))
				.andExpect(jsonPath("$.meta.total").value(1));
	}

	@Test
	void resumenDelInicio() throws Exception {
		mvc.perform(conToken(get("/usuarios/resumen"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inactivos").value(1))
				.andExpect(jsonPath("$.cuentasInactivas[0].username").value("U20260007"));
	}

	@Test
	void desactivarCortaLaSesionAbiertaDeInmediato() throws Exception {
		String tokenAna = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isOk());

		mvc.perform(conToken(post("/usuarios/" + ID_ANA + "/desactivar"), admin)).andExpect(status().isOk());
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isUnauthorized());

		mvc.perform(conToken(post("/usuarios/" + ID_ANA + "/reactivar"), admin)).andExpect(status().isOk());
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isOk());
	}

	@Test
	void eliminarCortaLaSesionAbierta() throws Exception {
		String tokenAna = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(delete("/usuarios/" + ID_ANA), admin)).andExpect(status().isNoContent());
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isUnauthorized());
	}

	@Test
	void soloElAdministradorGestionaUsuarios() throws Exception {
		mvc.perform(conToken(get("/usuarios"), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(conToken(get("/usuarios"), token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isForbidden());
		mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
	}
}
