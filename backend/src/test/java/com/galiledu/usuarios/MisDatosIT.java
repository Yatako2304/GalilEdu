package com.galiledu.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * Ampliación de HU-9 (no está en el backlog de Notion, que solo cubre la edición por el administrador): cualquier
 * usuario, sin importar el rol, edita sus propios datos personales desde «Mis datos» (tarjeta de usuario del menú
 * lateral). Mismas reglas de formato y duplicados que la edición del administrador; no toca roles ni el estado de la
 * cuenta.
 */
class MisDatosIT extends PruebaIntegracion {
	/** Usuario U20260001 (administrador) de los datos de prueba. */
	private static final String ID_ADMIN = "10000000-0000-0000-0000-000000000001";

	private static String datos(String dni, String nombres, String correo) {
		return """
				{"tipoDocumento":"DNI","numeroDocumento":"%s","apellidoPaterno":"Flores","apellidoMaterno":"Rojas",
				"nombres":"%s","fechaNacimiento":"1988-06-20","nacionalidad":"Peruana","lugarNacimiento":"Perú",
				"sexo":"FEMENINO","estadoCivil":"Soltero(a)","gradoInstruccion":"Superior","celular":"987654321",
				"correo":"%s","telefonoCasa":"014445555","profesion":"Docente","centroTrabajo":"Colegio Galileo",
				"cargo":"Docente","distrito":"Lince","direccion":"Av. Arequipa 2500"}
				""".formatted(dni, nombres, correo);
	}

	@Test
	void cualquierRolVeSusPropiosDatos() throws Exception {
		String docente = token(DOCENTE);
		mvc.perform(conToken(get("/auth/mis-datos"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroDocumento").exists());
	}

	@Test
	void cualquierRolActualizaSusPropiosDatosSinTocarRoles() throws Exception {
		String docente = token(DOCENTE);
		String nuevos = datos("88888881", "María Actualizada", "docente.actualizado@galiledu.edu.pe");
		mvc.perform(conToken(put("/auth/mis-datos"), docente, nuevos))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("María Actualizada"))
				.andExpect(jsonPath("$.roles[*]", hasItems("DOCENTE", "TUTOR")));

		mvc.perform(conToken(get("/auth/mis-datos"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombres").value("María Actualizada"))
				.andExpect(jsonPath("$.correo").value("docente.actualizado@galiledu.edu.pe"));
	}

	@Test
	void funcionaParaOtroRolDistintoDeAdministrador() throws Exception {
		String coordinador = token(COORDINADOR);
		String nuevos = datos("88888882", "Carlos Actualizado", "coordinador.actualizado@galiledu.edu.pe");
		mvc.perform(conToken(put("/auth/mis-datos"), coordinador, nuevos))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Carlos Actualizado"));
	}

	@Test
	void exigeLosDatosCompletosComoEnHU9() throws Exception {
		String docente = token(DOCENTE);
		String incompleto = """
				{"tipoDocumento":"DNI","numeroDocumento":"88888883","apellidoPaterno":"Flores","nombres":"María"}
				""";
		mvc.perform(conToken(put("/auth/mis-datos"), docente, incompleto))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rechazaDocumentoYaRegistradoAOtraPersona() throws Exception {
		String docente = token(DOCENTE);
		String admin = token(ADMIN);
		String detalleAdmin = mvc.perform(conToken(get("/usuarios/" + ID_ADMIN), admin)).andReturn().getResponse()
				.getContentAsString();
		String documentoAdmin = JsonPath.read(detalleAdmin, "$.persona.numeroDocumento");

		String conDocumentoAjeno = datos(documentoAdmin, "María", "otro.correo.libre@galiledu.edu.pe");
		mvc.perform(conToken(put("/auth/mis-datos"), docente, conDocumentoAjeno))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString(documentoAdmin)));
	}

	@Test
	void rechazaCorreoYaRegistradoAOtraPersona() throws Exception {
		String docente = token(DOCENTE);
		// selene.curo@galiledu.edu.pe es el correo del administrador en los datos de prueba.
		String conCorreoAjeno = datos("88888884", "María", "selene.curo@galiledu.edu.pe");
		mvc.perform(conToken(put("/auth/mis-datos"), docente, conCorreoAjeno))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("ya está registrado")));
	}

	@Test
	void sinSesionNoPuedeVerNiEditar() throws Exception {
		mvc.perform(get("/auth/mis-datos")).andExpect(status().isUnauthorized());
		String cuerpo = datos("88888885", "María", "x@galiledu.edu.pe");
		mvc.perform(put("/auth/mis-datos").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isUnauthorized());
	}
}
