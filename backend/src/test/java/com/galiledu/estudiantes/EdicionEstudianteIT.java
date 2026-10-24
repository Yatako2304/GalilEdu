package com.galiledu.estudiantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;
import com.galiledu.usuarios.infraestructura.NotificadorCredenciales;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

/** "Editar" del listado de estudiantes: GET /estudiantes/{id}/edicion y PUT /estudiantes/{id}. */
class EdicionEstudianteIT extends PruebaIntegracion {
	/** Sofía Paredes: matriculada en 4.° de secundaria 2026 (datos de prueba). */
	private static final String SOFIA_MATRICULADA = "20000000-0000-0000-0000-000000000002";
	private static final String CORREO_MADRE = "carla.rojas@gmail.com";
	private static final String CORREO_APODERADA = "lucia.fernandez@gmail.com";

	/** Para comprobar a qué correo se envían las credenciales. */
	@MockitoSpyBean
	private NotificadorCredenciales notificador;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	/** Padre que no registra, madre y apoderada nuevas (con todos sus datos); la madre es la responsable. */
	private String inscribir() throws Exception {
		String json = """
				{"estudiante":%s,"fichaMedica":%s,
				 "padre":{"noRegistra":true,"motivo":"Fallecido(a)"},
				 "madre":{"noRegistra":false,"persona":%s,"viveConEstudiante":true,"tienePatriaPotestad":true},
				 "apoderado":{"noRegistra":false,"persona":%s,"relacion":"Tía"},
				 "contactos":[{"nombreCompleto":"Rosa Medina","parentesco":"Abuela","celular":"987654321"},
				              {"nombreCompleto":"Pedro Rojas","parentesco":"Tío","celular":"976543210"}],
				 "modoSalida":"PERSONA_AUTORIZADA","autorizados":[],
				 "responsable":"MADRE","correoResponsable":"%s","celularResponsable":"987333444"}"""
				.formatted(InscripcionPrueba.estudiante("78945699", "Lucía", "PRI-6"), InscripcionPrueba.FICHA_MEDICA,
						InscripcionPrueba.persona("42345678", "Rojas", "Carla", "987333444", CORREO_MADRE),
						InscripcionPrueba.persona("45678912", "Fernández", "Lucía", "999888777", CORREO_APODERADA),
						CORREO_MADRE);
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(r, "$.id");
	}

	/** Carga la edición, aplica los cambios a los datos y los envía con PUT. */
	private ResultActions editar(String id, Consumer<DocumentContext> cambios) throws Exception {
		String r = mvc.perform(conToken(get("/estudiantes/" + id + "/edicion"), personal))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		DocumentContext datos = JsonPath.parse((Object) JsonPath.read(r, "$.datos"));
		cambios.accept(datos);
		return mvc.perform(conToken(put("/estudiantes/" + id), personal, datos.jsonString()));
	}

	private String estadoCuenta(String correo) throws Exception {
		String r = mvc.perform(conToken(get("/usuarios").param("q", correo), token(ADMIN)))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(r, "$.data[0].estado");
	}

	@Test
	void laEdicionTraeLosDatosConLaFormaDeLaInscripcion() throws Exception {
		String id = inscribir();
		mvc.perform(conToken(get("/estudiantes/" + id + "/edicion"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matriculado").value(false))
				.andExpect(jsonPath("$.datos.estudiante.persona.numeroDocumento").value("78945699"))
				.andExpect(jsonPath("$.datos.fichaMedica.tipoSangre").value("O+"))
				.andExpect(jsonPath("$.datos.padre.noRegistra").value(true))
				.andExpect(jsonPath("$.datos.madre.personaId").isNotEmpty())
				.andExpect(jsonPath("$.datos.madre.persona.profesion").value("Diseñadora"))
				.andExpect(jsonPath("$.datos.apoderado.relacion").value("Tía"))
				.andExpect(jsonPath("$.datos.responsable").value("MADRE"))
				.andExpect(jsonPath("$.datos.correoResponsable").value(CORREO_MADRE))
				.andExpect(jsonPath("$.conCuenta[0]").value("MADRE"));
	}

	@Test
	void editaLosDatosDelMismoEstudianteYDeSusFamiliares() throws Exception {
		String id = inscribir();
		editar(id, d -> d.set("$.estudiante.colegioProcedencia", "IE Santa Rosa")
				.set("$.estudiante.persona.numeroDocumento", "78945600")
				.set("$.madre.persona.profesion", "Arquitecta"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));

		mvc.perform(conToken(get("/estudiantes/" + id + "/edicion"), personal))
				.andExpect(jsonPath("$.datos.estudiante.colegioProcedencia").value("IE Santa Rosa"))
				.andExpect(jsonPath("$.datos.estudiante.persona.numeroDocumento").value("78945600"))
				.andExpect(jsonPath("$.datos.madre.persona.profesion").value("Arquitecta"));
	}

	@Test
	void aplicaLasMismasValidacionesQueLaInscripcion() throws Exception {
		String id = inscribir();
		editar(id, d -> d.set("$.estudiante.colegioProcedencia", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el colegio de procedencia"));
		editar(id, d -> d.set("$.madre.persona.celular", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Madre: Ingresa el celular"));
	}

	@Test
	void noSePuedeUsarElDocumentoDeOtraPersona() throws Exception {
		String id = inscribir();
		// 70000002: Ana Torres (usuaria de los datos de prueba).
		editar(id, d -> d.set("$.estudiante.persona.numeroDocumento", "70000002"))
				.andExpect(status().isConflict());
	}

	@Test
	void unEstudianteMatriculadoNoCambiaDeGradoNiDeAnio() throws Exception {
		editar(SOFIA_MATRICULADA, d -> d.set("$.estudiante.gradoCodigo", "SEC-5"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value("El estudiante está matriculado: no se puede cambiar el grado ni el año académico"));
	}

	@Test
	void alCambiarElResponsableElNuevoTieneCuentaYLaDelAnteriorQuedaInactiva() throws Exception {
		String id = inscribir();
		editar(id, d -> d.set("$.responsable", "APODERADO")
				.set("$.correoResponsable", CORREO_APODERADA)
				.set("$.celularResponsable", "999888777"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuentaNueva").value(true));

		assertThat(estadoCuenta(CORREO_APODERADA)).isEqualTo("ACTIVO");
		assertThat(estadoCuenta(CORREO_MADRE)).isEqualTo("INACTIVO");

		// Si vuelve a ser responsable, su cuenta se reactiva.
		editar(id, d -> d.set("$.responsable", "MADRE")
				.set("$.correoResponsable", CORREO_MADRE)
				.set("$.celularResponsable", "987333444"))
				.andExpect(status().isOk());
		assertThat(estadoCuenta(CORREO_MADRE)).isEqualTo("ACTIVO");
	}

	@Test
	void alCambiarElCorreoDeUnFamiliarConCuentaSuCuentaUsaElNuevo() throws Exception {
		String id = inscribir();
		editar(id, d -> d.set("$.madre.persona.correo", "carla.nueva@gmail.com")
				.set("$.correoResponsable", "carla.nueva@gmail.com"))
				.andExpect(status().isOk());

		assertThat(estadoCuenta("carla.nueva@gmail.com")).isEqualTo("ACTIVO");
		// Contraseña nueva enviada al correo nuevo, con el mismo código de usuario.
		verify(notificador).enviar(eq("carla.nueva@gmail.com"), eq("Carla"), any(), any());
		mvc.perform(conToken(get("/estudiantes/correos").param("correo", CORREO_MADRE), personal))
				.andExpect(jsonPath("$.disponible").value(true));
	}

	@Test
	void conservaElDocumentoDelApoderadoSiEsLaMismaPersona() throws Exception {
		String id = inscribir();
		mvc.perform(conToken(multipart("/estudiantes/" + id + "/apoderado/documento")
				.file(new MockMultipartFile("archivo", "carta.pdf", "application/pdf", "%PDF-1.4".getBytes()))
				.param("tipo", "Carta poder notarial"), personal))
				.andExpect(status().isOk());

		editar(id, d -> d.set("$.estudiante.colegioProcedencia", "IE Santa Rosa")).andExpect(status().isOk());

		mvc.perform(conToken(get("/estudiantes/" + id + "/edicion"), personal))
				.andExpect(jsonPath("$.acreditacion.nombreArchivo").value("carta.pdf"));
		mvc.perform(conToken(get("/estudiantes/" + id + "/apoderado/documento"), personal))
				.andExpect(status().isOk());
	}
}
