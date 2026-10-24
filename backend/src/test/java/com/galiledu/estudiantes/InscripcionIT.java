package com.galiledu.estudiantes;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/** HU-7: inscripción del estudiante con padres o apoderado. */
class InscripcionIT extends PruebaIntegracion {
	/** Luis Paredes: padre de Mateo en los datos de prueba, con usuario U20260003. */
	private static final String PERSONA_LUIS = "00000000-0000-0000-0000-000000000003";

	private String personal;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String inscripcion(String padre, String madre, String apoderado, String responsable) {
		return """
				{"estudiante":%s,
				 "fichaMedica":%s,
				 "padre":%s,"madre":%s,"apoderado":%s,
				 "contactos":[{"nombreCompleto":"Rosa Medina","parentesco":"Abuela","celular":"987654321"},
				              {"nombreCompleto":"Pedro Rojas","parentesco":"Tío","celular":"976543210"}],
				 "modoSalida":"PERSONA_AUTORIZADA","autorizados":[],
				 "responsable":"%s",%s}"""
				.formatted(InscripcionPrueba.estudiante("78945699", "Lucía", "PRI-6"), InscripcionPrueba.FICHA_MEDICA,
						padre, madre, apoderado, responsable, contactoResponsable(responsable));
	}

	/** Paso 5: el correo y el celular de cada responsable (los mismos del Paso 3; el correo es único). */
	private static String contactoResponsable(String responsable) {
		String correo = switch (responsable) {
			case "PADRE" -> "luis.paredes@galiledu.edu.pe";
			case "MADRE" -> "carla.rojas@gmail.com";
			default -> "lucia.fernandez@gmail.com";
		};
		return "\"correoResponsable\":\"%s\",\"celularResponsable\":\"911222333\"".formatted(correo);
	}

	private static final String PADRE_EXISTENTE = """
			{"noRegistra":false,"personaId":"%s","viveConEstudiante":true,"tienePatriaPotestad":true}"""
			.formatted(PERSONA_LUIS);
	private static final String MADRE_NUEVA = """
			{"noRegistra":false,"persona":%s,"viveConEstudiante":true}"""
			.formatted(InscripcionPrueba.persona("42345678", "Rojas", "Carla", "987333444", "carla.rojas@gmail.com"));
	private static final String NO_REGISTRA = "{\"noRegistra\":true,\"motivo\":\"Fallecido(a)\"}";

	@Test
	void inscribeConPadreExistenteYCreaCuentaALaMadreResponsable() throws Exception {
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "MADRE")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cuentaNueva").value(true))
				.andExpect(jsonPath("$.usuarioResponsable").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(r, "$.id");

		mvc.perform(conToken(get("/estudiantes/" + id), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INSCRITO"))
				.andExpect(jsonPath("$.grado").value("6.° de primaria"))
				.andExpect(jsonPath("$.familia.length()").value(2))
				.andExpect(jsonPath("$.fichaMedica.alergiasAlimentarias").value("Maní"))
				.andExpect(jsonPath("$.contactos.length()").value(2))
				// Comparte padre con Mateo y Sofía: aparecen como hermanos.
				.andExpect(jsonPath("$.hermanos[*].nombreCompleto", hasItem("Mateo Alonso Paredes Rojas")));
	}

	@Test
	void siElResponsableYaTieneCuentaSoloSeVincula() throws Exception {
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cuentaNueva").value(false))
				.andExpect(jsonPath("$.usuarioResponsable").value("U20260003"));
	}

	@Test
	void sinPadresExigeApoderado() throws Exception {
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(NO_REGISTRA, NO_REGISTRA, "null", "PADRE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("apoderado")));
	}

	/** Inscripción válida con un dato cambiado; debe responder 400 con el mensaje dado. */
	private void rechaza(String original, String cambio, String mensaje) throws Exception {
		String json = inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE");
		org.assertj.core.api.Assertions.assertThat(json).contains(original);
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json.replace(original, cambio)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(mensaje));
	}

	@Test
	void exigeLosDatosObligatoriosDelPaso1() throws Exception {
		rechaza("\"sexo\":\"FEMENINO\"", "\"sexo\":null", "Selecciona el sexo");
		rechaza("\"fechaNacimiento\":\"2015-05-10\"", "\"fechaNacimiento\":null",
				"Ingresa la fecha de nacimiento");
		rechaza("\"direccion\":\"Calle 123\"", "\"direccion\":\" \"", "Ingresa la dirección");
		rechaza("\"tieneInternet\":true", "\"tieneInternet\":null",
				"Selecciona si tiene acceso a internet / dispositivo");
		rechaza("\"lenguaMaterna\":\"Castellano\"", "\"lenguaMaterna\":\"\"", "Selecciona la lengua materna");
		rechaza("\"colegioProcedencia\":\"IE San José\"", "\"colegioProcedencia\":\"\"",
				"Ingresa el colegio de procedencia");
		rechaza("\"gestionProcedencia\":\"Pública\"", "\"gestionProcedencia\":null", "Selecciona la gestión");
		rechaza("\"formaIngreso\":\"Edad normativa\"", "\"formaIngreso\":\"\"",
				"Selecciona la forma de ingreso");
		rechaza("\"tieneDiscapacidad\":false", "\"tieneDiscapacidad\":true", "Indica cuál es la discapacidad");
	}

	@Test
	void validaLugarDeNacimientoNacionalidadYNombres() throws Exception {
		rechaza("\"departamentoNacimiento\":\"Lima\"", "\"departamentoNacimiento\":null",
				"Selecciona el departamento");
		rechaza("\"provinciaNacimiento\":\"Lima\"", "\"provinciaNacimiento\":\"\"", "Selecciona la provincia");
		rechaza("\"distritoNacimiento\":\"Magdalena del Mar\"", "\"distritoNacimiento\":null",
				"Selecciona el distrito de nacimiento");
		rechaza("\"distrito\":\"Magdalena del Mar\"", "\"distrito\":null", "Selecciona el distrito");
		rechaza("\"paisNacimiento\":\"Perú\"", "\"paisNacimiento\":\"Chile\"",
				"Departamento, provincia y distrito de nacimiento solo se indican si nació en el Perú");
		rechaza("\"nacionalidad\":\"Peruana\"", "\"nacionalidad\":\"Chilena\"",
				"La nacionalidad del estudiante debe ser Peruana o Extranjera");
		rechaza("\"nombres\":\"Lucía\"", "\"nombres\":\"Lucía#\"", "Los nombres solo pueden contener letras");
		rechaza("\"apellidoMaterno\":\"Rojas\"", "\"apellidoMaterno\":\"Rojas2\"",
				"El apellido solo puede contener letras");

		// Nacido fuera del Perú: sin departamento, provincia ni distrito.
		String extranjero = inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")
				.replace(
						"\"paisNacimiento\":\"Perú\",\"departamentoNacimiento\":\"Lima\",\"provinciaNacimiento\":\"Lima\",",
						"\"paisNacimiento\":\"Chile\",")
				.replace("\"distritoNacimiento\":\"Magdalena del Mar\",", "");
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, extranjero))
				.andExpect(status().isCreated());
	}

	@Test
	void unFamiliarNuevoNecesitaLosMismosDatosQueUnUsuario() throws Exception {
		// La madre es nueva: cada dato obligatorio de CAHU6 dice de quién es.
		rechaza("\"profesion\":\"Diseñadora\"", "\"profesion\":\"\"", "Madre: Ingresa la profesión u ocupación");
		rechaza("\"lugarNacimiento\":\"Perú\"", "\"lugarNacimiento\":null", "Madre: Selecciona el país de nacimiento");
		rechaza("\"estadoCivil\":\"Casado(a)\"", "\"estadoCivil\":null", "Madre: Selecciona el estado civil");
		rechaza("\"celular\":\"987333444\"", "\"celular\":\"\"", "Madre: Ingresa el celular");
		// Formatos de CAHU6 (celular, correo y nombres) también para los familiares.
		rechaza("\"celular\":\"987333444\"", "\"celular\":\"98733\"", "El celular debe tener 9 dígitos");
		rechaza("\"correo\":\"carla.rojas@gmail.com\"", "\"correo\":\"carla@gmail\"", "Ingresa un correo válido");
		rechaza("\"nombres\":\"Carla\"", "\"nombres\":\"Carla2\"", "Los nombres solo pueden contener letras");
	}

	@Test
	void validaContactosYPersonasAutorizadasDelPaso4() throws Exception {
		rechaza("\"celular\":\"987654321\"}", "\"celular\":\"\"}", "Ingresa el celular del contacto");
		rechaza("\"celular\":\"987654321\"}", "\"celular\":\"98765\"}", "El celular debe tener 9 dígitos");
		rechaza("\"nombreCompleto\":\"Rosa Medina\"", "\"nombreCompleto\":\"Rosa 2\"",
				"El nombre solo puede contener letras");
		rechaza("\"parentesco\":\"Abuela\"", "\"parentesco\":\"Abuela!\"", "La relación solo puede contener letras");
		rechaza("\"celular\":\"987654321\"}", "\"celular\":\"987654321\",\"telefonoFijo\":\"01-452\"}",
				"El teléfono solo puede contener números");
		rechaza("\"autorizados\":[]", "\"autorizados\":[{\"nombreCompleto\":\"\",\"relacion\":\"Abuela\"}]",
				"Ingresa el nombre de la persona autorizada");
	}

	@Test
	void exigeElCorreoYElCelularDelResponsableEnElPaso5() throws Exception {
		rechaza("\"correoResponsable\":\"luis.paredes@galiledu.edu.pe\"", "\"correoResponsable\":\"\"",
				"Ingresa el correo de acceso");
		rechaza("\"correoResponsable\":\"luis.paredes@galiledu.edu.pe\"", "\"correoResponsable\":\"luis@galiledu\"",
				"Ingresa un correo válido");
		rechaza("\"celularResponsable\":\"911222333\"", "\"celularResponsable\":null",
				"Ingresa el celular del responsable");
		rechaza("\"celularResponsable\":\"911222333\"", "\"celularResponsable\":\"91122\"",
				"El celular debe tener 9 dígitos");
	}

	@Test
	void consultaSiUnCorreoYaEstaAsignadoSinImportarMayusculas() throws Exception {
		mvc.perform(conToken(get("/estudiantes/correos").param("correo", "LUIS.PAREDES@galiledu.edu.pe"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponible").value(false));
		mvc.perform(conToken(get("/estudiantes/correos").param("correo", "nadie@gmail.com"), personal))
				.andExpect(jsonPath("$.disponible").value(true));
	}

	@Test
	void elApoderadoNecesitaSuRelacionConElEstudiante() throws Exception {
		String json = inscripcion(NO_REGISTRA, NO_REGISTRA,
				APODERADO_NUEVO.replace("\"relacion\":\"Amiga de la familia\"", "\"relacion\":\"\""), "APODERADO");
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Apoderado: Selecciona la relación"));
	}

	@Test
	void exigeLosDatosObligatoriosDelPaso2() throws Exception {
		rechaza("\"fichaMedica\":{", "\"fichaMedicaX\":{", "Completa la ficha médica");
		rechaza("\"tipoSangre\":\"O+\"", "\"tipoSangre\":\"\"", "Selecciona el tipo de sangre");
		rechaza("\"pesoKg\":22", "\"pesoKg\":null", "Ingresa el peso");
		rechaza("\"estaturaM\":1.18", "\"estaturaM\":null", "Ingresa la estatura");
		rechaza("\"tipoSeguro\":\"EsSalud\"", "\"tipoSeguro\":null", "Selecciona el tipo de seguro");
		rechaza("\"fiebreMaxima\":\"39\"", "\"fiebreMaxima\":\"\"", "Indica hasta cuántos grados de fiebre ha llegado");
		rechaza("\"alergiasAlimentarias\":\"Maní\"", "\"alergiasAlimentarias\":\" \"",
				"Indica al menos una alergia (alimentaria o medicamentosa)");
		rechaza("\"tieneEnfermedad\":false", "\"tieneEnfermedad\":true", "Indica cuál es la enfermedad");
		rechaza("\"tomaMedicamento\":false", "\"tomaMedicamento\":true", "Indica cuál es el medicamento");
	}

	@Test
	void validaRangosYFormatosDelPaso2() throws Exception {
		rechaza("\"pesoKg\":22", "\"pesoKg\":4.9", "El peso debe estar entre 5 y 200 kg");
		rechaza("\"pesoKg\":22", "\"pesoKg\":200.5", "El peso debe estar entre 5 y 200 kg");
		rechaza("\"estaturaM\":1.18", "\"estaturaM\":118", "La estatura debe estar entre 0.50 y 2.50 m");
		rechaza("\"pesoKg\":22", "\"pesoKg\":22.55", "El peso admite 1 decimal (###.#)");
		rechaza("\"estaturaM\":1.18", "\"estaturaM\":1.185", "La estatura admite 2 decimales (#.##)");
		rechaza("\"fiebreMaxima\":\"39\"", "\"fiebreMaxima\":\"39,5\"", "Ingresa un número (ej. 39 o 39.5)");
		rechaza("\"fiebreMaxima\":\"39\"", "\"fiebreMaxima\":\"mucha\"", "Ingresa un número (ej. 39 o 39.5)");
	}

	@Test
	void losDetallesDeUnaRespuestaNoNoSeGuardan() throws Exception {
		String json = inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")
				.replace("\"tieneAlergias\":true", "\"tieneAlergias\":false")
				.replace("\"tieneEnfermedad\":false", "\"tieneEnfermedad\":false,\"enfermedadDetalle\":\"Asma\"")
				.replace("\"tomaMedicamento\":false",
						"\"tomaMedicamento\":false,\"medicamentoDetalle\":\"Salbutamol\"");
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		mvc.perform(conToken(get("/estudiantes/" + JsonPath.read(r, "$.id")), personal))
				.andExpect(jsonPath("$.fichaMedica.pesoKg").value(22))
				.andExpect(jsonPath("$.fichaMedica.fiebreMaxima").value("39"))
				.andExpect(jsonPath("$.fichaMedica.alergiasAlimentarias").isEmpty())
				.andExpect(jsonPath("$.fichaMedica.enfermedadDetalle").isEmpty())
				.andExpect(jsonPath("$.fichaMedica.medicamentoDetalle").isEmpty());
	}

	@Test
	void sinDiscapacidadNoGuardaElDetalle() throws Exception {
		String json = inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")
				.replace("\"tieneDiscapacidad\":false",
						"\"tieneDiscapacidad\":false,\"discapacidadDetalle\":\"Visual\"");
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		mvc.perform(conToken(get("/estudiantes/" + JsonPath.read(r, "$.id")), personal))
				.andExpect(jsonPath("$.datos.tieneDiscapacidad").value(false))
				.andExpect(jsonPath("$.datos.discapacidadDetalle").isEmpty());
	}

	@Test
	void unTextoMasLargoQueSuColumnaDa400() throws Exception {
		String json = inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")
				.replace("\"colegioProcedencia\":\"IE San José\"",
						"\"colegioProcedencia\":\"" + "x".repeat(151) + "\"");
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json))
				.andExpect(status().isBadRequest());
	}

	@Test
	void exigeDosContactosDeEmergencia() throws Exception {
		String json = inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")
				.replaceAll("\\{\"nombreCompleto\":\"Pedro[^}]*},?", "")
				.replace("\"celular\":\"987654321\"},", "\"celular\":\"987654321\"}");
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json))
				.andExpect(status().isBadRequest());
	}

	@Test
	void buscaPersonaPorDocumento() throws Exception {
		mvc.perform(conToken(get("/estudiantes/personas").param("documento", "70000003"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.personaId").value(PERSONA_LUIS))
				.andExpect(jsonPath("$.tieneCuenta").value(true));
		mvc.perform(conToken(get("/estudiantes/personas").param("documento", "11111111"), personal))
				.andExpect(status().isNotFound());
	}

	@Test
	void elListadoFiltraPorLaSituacionQueSaleDeLaMatricula() throws Exception {
		// Sofía y Valeria están matriculadas en 2026; Mateo solo tiene sección reservada para 2027.
		mvc.perform(conToken(get("/estudiantes").param("estado", "MATRICULADO"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].nombreCompleto", hasItem("Sofía Paredes Rojas")))
				.andExpect(jsonPath("$.data[*].nombreCompleto", not(hasItem("Mateo Alonso Paredes Rojas"))))
				.andExpect(jsonPath("$.data[*].estado", everyItem(is("MATRICULADO"))));
		mvc.perform(conToken(get("/estudiantes").param("estado", "INSCRITO"), personal))
				.andExpect(jsonPath("$.data[*].nombreCompleto", hasItem("Mateo Alonso Paredes Rojas")));
	}

	@Test
	void elDocenteNoPuedeInscribir() throws Exception {
		mvc.perform(conToken(get("/estudiantes"), token(DOCENTE))).andExpect(status().isForbidden());
	}

	private static final String APODERADO_NUEVO = """
			{"noRegistra":false,"persona":%s,"relacion":"Amiga de la familia"}"""
			.formatted(InscripcionPrueba.persona("45678912", "Fernández", "Lucía", "999888777",
					"lucia.fernandez@gmail.com"));
	private static final byte[] PDF = "%PDF-1.4 carta poder".getBytes();

	private String inscribirConApoderado() throws Exception {
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(NO_REGISTRA, NO_REGISTRA, APODERADO_NUEVO, "APODERADO")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(r, "$.id");
	}

	private org.springframework.test.web.servlet.ResultActions subir(String estudianteId, String nombre, String mime,
			byte[] contenido) throws Exception {
		return mvc.perform(conToken(multipart("/estudiantes/" + estudianteId + "/apoderado/documento")
				.file(new MockMultipartFile("archivo", nombre, mime, contenido))
				.param("tipo", "Carta poder notarial"), personal));
	}

	@Test
	void subeYDescargaElDocumentoDelApoderado() throws Exception {
		String id = inscribirConApoderado();

		subir(id, "carta_poder.pdf", "application/pdf", PDF)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("Carta poder notarial"))
				.andExpect(jsonPath("$.nombreArchivo").value("carta_poder.pdf"));

		mvc.perform(conToken(get("/estudiantes/" + id), personal))
				.andExpect(jsonPath("$.familia[?(@.parentesco == 'APODERADO')].acreditacion.nombreArchivo")
						.value("carta_poder.pdf"));

		mvc.perform(conToken(get("/estudiantes/" + id + "/apoderado/documento"), personal))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/pdf"))
				.andExpect(content().bytes(PDF));
	}

	/** El detalle del usuario (módulo usuarios) lee el vínculo y la acreditación que guardó la inscripción. */
	@Test
	void elUsuarioDelApoderadoMuestraAlEstudianteYSuAcreditacion() throws Exception {
		String id = inscribirConApoderado();
		subir(id, "carta_poder.pdf", "application/pdf", PDF).andExpect(status().isOk());
		// La prueba corre en una sola transacción: se escribe lo guardado para que lo vea la consulta SQL.
		entityManager.flush();

		String admin = token(ADMIN);
		String lista = mvc.perform(conToken(get("/usuarios").param("q", "lucia.fernandez@gmail.com"), admin))
				.andReturn().getResponse().getContentAsString();
		String usuarioId = JsonPath.read(lista, "$.data[0].id");
		mvc.perform(conToken(get("/usuarios/" + usuarioId), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(1))
				.andExpect(jsonPath("$.estudiantesACargo[0].estudianteId").value(id))
				.andExpect(jsonPath("$.estudiantesACargo[0].parentesco").value("APODERADO"))
				.andExpect(jsonPath("$.estudiantesACargo[0].relacion").value("Amiga de la familia"))
				.andExpect(jsonPath("$.estudiantesACargo[0].acreditacion.tipo").value("Carta poder notarial"))
				.andExpect(jsonPath("$.estudiantesACargo[0].acreditacion.nombreArchivo").value("carta_poder.pdf"))
				.andExpect(jsonPath("$.estudiantesACargo[0].acreditacion.subidoEn").isNotEmpty());
	}

	@Test
	void rechazaArchivosQueNoSonPdfNiImagen() throws Exception {
		String id = inscribirConApoderado();
		subir(id, "virus.exe", "application/octet-stream", PDF)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El documento debe ser PDF, JPG o PNG"));
	}

	@Test
	void sinApoderadoNoSePuedeSubirDocumento() throws Exception {
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(PADRE_EXISTENTE, MADRE_NUEVA, "null", "PADRE")))
				.andReturn().getResponse().getContentAsString();
		subir(JsonPath.read(r, "$.id"), "carta.pdf", "application/pdf", PDF).andExpect(status().isBadRequest());
	}
}
