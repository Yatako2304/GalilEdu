package com.galiledu.estudiantes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/** HU-7: inscripción del estudiante con padres o apoderado. */
class InscripcionIT extends PruebaIntegracion {
	/** Luis Paredes: padre de Mateo en los datos de prueba, con usuario U20260003. */
	private static final String PERSONA_LUIS = "00000000-0000-0000-0000-000000000003";

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String inscripcion(String padre, String madre, String apoderado, String responsable) {
		return """
				{"estudiante":{"persona":{"tipoDocumento":"DNI","numeroDocumento":"78945699","apellidoPaterno":"Paredes",
				  "apellidoMaterno":"Rojas","nombres":"Lucía","fechaNacimiento":"2015-05-10","sexo":"FEMENINO"},
				  "anioAcademico":2027,"nivel":"PRIMARIA","grado":"6.° grado"},
				 "fichaMedica":{"tipoSangre":"O+","tieneAlergias":true,"alergiasAlimentarias":"Maní"},
				 "padre":%s,"madre":%s,"apoderado":%s,
				 "contactos":[{"nombreCompleto":"Rosa Medina","parentesco":"Abuela","celular":"987654321"},
				              {"nombreCompleto":"Pedro Rojas","parentesco":"Tío","celular":"976543210"}],
				 "modoSalida":"PERSONA_AUTORIZADA","autorizados":[],
				 "responsable":"%s"}"""
				.formatted(padre, madre, apoderado, responsable);
	}

	private static final String PADRE_EXISTENTE = """
			{"noRegistra":false,"personaId":"%s","viveConEstudiante":true,"tienePatriaPotestad":true}"""
			.formatted(PERSONA_LUIS);
	private static final String MADRE_NUEVA = """
			{"noRegistra":false,"persona":{"tipoDocumento":"DNI","numeroDocumento":"42345678","apellidoPaterno":"Rojas",
			"nombres":"Carla","celular":"987333444","correo":"carla.rojas@gmail.com"},"viveConEstudiante":true}""";
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
				.andExpect(jsonPath("$.familia.length()").value(2))
				.andExpect(jsonPath("$.fichaMedica.alergiasAlimentarias").value("Maní"))
				.andExpect(jsonPath("$.contactos.length()").value(2))
				// Mateo comparte padre: aparece como hermano.
				.andExpect(jsonPath("$.hermanos[0].nombreCompleto").value("Mateo Alonso Paredes Rojas"));
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
	void elDocenteNoPuedeInscribir() throws Exception {
		mvc.perform(conToken(get("/estudiantes"), token(DOCENTE))).andExpect(status().isForbidden());
	}

	private static final String APODERADO_NUEVO = """
			{"noRegistra":false,"persona":{"tipoDocumento":"DNI","numeroDocumento":"45678912","apellidoPaterno":"Fernández",
			"nombres":"Lucía","celular":"999888777","correo":"lucia.fernandez@gmail.com"},"relacion":"Amiga de la familia"}""";
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
