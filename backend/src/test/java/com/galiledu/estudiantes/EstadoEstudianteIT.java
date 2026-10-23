package com.galiledu.estudiantes;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-7, paso 1: un documento solo se puede inscribir si está libre o si era de un estudiante eliminado (sus datos
 * se actualizan). Un estudiante activo o retirado (inactivo) no se puede inscribir de nuevo. También las acciones
 * "Inactivar", "Reactivar" y "Eliminar" del listado.
 */
class EstadoEstudianteIT extends PruebaIntegracion {
	/** Luis Paredes: padre de Mateo en los datos de prueba, con usuario U20260003. */
	private static final String PERSONA_LUIS = "00000000-0000-0000-0000-000000000003";
	private static final String DOCUMENTO = "78945699";
	/** Documento de Luis Paredes: es un padre, no un estudiante. */
	private static final String DOCUMENTO_DE_UN_PADRE = "70000003";

	@Autowired
	private EntityManager entityManager;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static String inscripcion(String documento, String nombres, String grado) {
		return """
				{"estudiante":%s,
				 "fichaMedica":%s,
				 "padre":{"noRegistra":false,"personaId":"%s","viveConEstudiante":true,"tienePatriaPotestad":true},
				 "madre":{"noRegistra":true,"motivo":"Fallecido(a)"},"apoderado":null,
				 "contactos":[{"nombreCompleto":"Rosa Medina","parentesco":"Abuela","celular":"987654321"},
				              {"nombreCompleto":"Pedro Rojas","parentesco":"Tío","celular":"976543210"}],
				 "modoSalida":"PERSONA_AUTORIZADA","autorizados":[],
				 "responsable":"PADRE"}"""
				.formatted(InscripcionPrueba.estudiante(documento, nombres, grado), InscripcionPrueba.FICHA_MEDICA,
						PERSONA_LUIS);
	}

	/** Inscribe y devuelve el id del estudiante. */
	private String inscribir(String documento, String nombres, String grado) throws Exception {
		String r = mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(documento, nombres, grado)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(r, "$.id");
	}

	/**
	 * La prueba corre en una sola transacción y el estado "inscrito / matriculado / retirado" es una fórmula SQL:
	 * se limpia la caché de Hibernate para leer lo que de verdad quedó en la base, como en una petición nueva.
	 */
	private ResultActions ficha(String id) throws Exception {
		entityManager.flush();
		entityManager.clear();
		return mvc.perform(conToken(get("/estudiantes/" + id), personal, null));
	}

	private void verificar(String documento, String estado, boolean permitido) throws Exception {
		mvc.perform(conToken(get("/estudiantes/documentos?documento=" + documento), personal, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value(estado))
				.andExpect(jsonPath("$.permitido").value(permitido));
	}

	@Test
	void unDocumentoSinRegistrarEstaLibre() throws Exception {
		verificar("11112222", "LIBRE", true);
	}

	@Test
	void unEstudianteActivoNoSePuedeInscribirDeNuevo() throws Exception {
		inscribir(DOCUMENTO, "Lucía", "PRI-6");

		verificar(DOCUMENTO, "ESTUDIANTE_ACTIVO", false);
		// El mensaje no dice de quién es el documento.
		mvc.perform(conToken(get("/estudiantes/documentos?documento=" + DOCUMENTO), personal, null))
				.andExpect(jsonPath("$.mensaje").value("El documento ya existe"));
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, inscripcion(DOCUMENTO, "Lucía", "PRI-6")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El documento ya existe"));
	}

	@Test
	void unEstudianteRetiradoNoSePuedeInscribirComoNuevoHastaReactivarlo() throws Exception {
		String id = inscribir(DOCUMENTO, "Lucía", "PRI-6");

		mvc.perform(conToken(post("/estudiantes/" + id + "/retirar"), personal, null))
				.andExpect(status().isNoContent());
		ficha(id)
				.andExpect(jsonPath("$.estado").value("RETIRADO"));
		verificar(DOCUMENTO, "ESTUDIANTE_RETIRADO", false);
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal, inscripcion(DOCUMENTO, "Lucía", "PRI-6")))
				.andExpect(status().isConflict());
		// Ya está inactivo.
		mvc.perform(conToken(post("/estudiantes/" + id + "/retirar"), personal, null))
				.andExpect(status().isConflict());

		mvc.perform(conToken(post("/estudiantes/" + id + "/reactivar"), personal, null))
				.andExpect(status().isNoContent());
		ficha(id)
				.andExpect(jsonPath("$.estado").value("INSCRITO"));
		verificar(DOCUMENTO, "ESTUDIANTE_ACTIVO", false);
		// Ya está activo.
		mvc.perform(conToken(post("/estudiantes/" + id + "/reactivar"), personal, null))
				.andExpect(status().isConflict());
	}

	@Test
	void unEstudianteEliminadoSeReinscribeYSusDatosSeActualizan() throws Exception {
		String id = inscribir(DOCUMENTO, "Lucía", "PRI-6");

		mvc.perform(conToken(delete("/estudiantes/" + id), personal, null)).andExpect(status().isNoContent());
		// Eliminado: ya no está en el listado ni se puede abrir su ficha.
		mvc.perform(conToken(get("/estudiantes?q=" + DOCUMENTO), personal, null))
				.andExpect(jsonPath("$.data[*].id", not(hasItem(id))));
		ficha(id).andExpect(status().isNotFound());
		verificar(DOCUMENTO, "REUTILIZABLE", true);
		// Sin mensaje: quien inscribe no ve que se reutiliza el registro.
		mvc.perform(conToken(get("/estudiantes/documentos?documento=" + DOCUMENTO), personal, null))
				.andExpect(jsonPath("$.mensaje").isEmpty());

		// Se inscribe de nuevo con otros datos: es el mismo registro, activo y con lo nuevo.
		String nuevo = inscribir(DOCUMENTO, "Lucía Fernanda", "SEC-1");
		assertEquals(id, nuevo);
		ficha(id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INSCRITO"))
				.andExpect(jsonPath("$.persona.nombres").value("Lucía Fernanda"))
				.andExpect(jsonPath("$.grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.familia.length()").value(2))
				.andExpect(jsonPath("$.contactos.length()").value(2));
		verificar(DOCUMENTO, "ESTUDIANTE_ACTIVO", false);
	}

	@Test
	void unEstudianteEliminadoYRetiradoTambienSeReinscribe() throws Exception {
		String id = inscribir(DOCUMENTO, "Lucía", "PRI-6");
		mvc.perform(conToken(post("/estudiantes/" + id + "/retirar"), personal, null))
				.andExpect(status().isNoContent());
		mvc.perform(conToken(delete("/estudiantes/" + id), personal, null)).andExpect(status().isNoContent());

		verificar(DOCUMENTO, "REUTILIZABLE", true);
		inscribir(DOCUMENTO, "Lucía", "PRI-6");
		ficha(id)
				.andExpect(jsonPath("$.estado").value("INSCRITO"));
	}

	@Test
	void elDocumentoDeUnaPersonaQueNoEsEstudianteSeRechaza() throws Exception {
		verificar(DOCUMENTO_DE_UN_PADRE, "OTRA_PERSONA", false);
		mvc.perform(conToken(post("/estudiantes/inscripciones"), personal,
				inscripcion(DOCUMENTO_DE_UN_PADRE, "Lucía", "PRI-6")))
				.andExpect(status().isConflict());
	}

	@Test
	void soloAdministracionPuedeCambiarElEstado() throws Exception {
		String id = inscribir(DOCUMENTO, "Lucía", "PRI-6");
		String docente = token(DOCENTE);
		mvc.perform(conToken(post("/estudiantes/" + id + "/retirar"), docente, null)).andExpect(status().isForbidden());
		mvc.perform(conToken(delete("/estudiantes/" + id), docente, null)).andExpect(status().isForbidden());
	}
}
