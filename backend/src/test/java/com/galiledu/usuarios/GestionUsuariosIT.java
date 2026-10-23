package com.galiledu.usuarios;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/** HU-6 (registro), HU-9 (edición), HU-12 (desactivación) y permisos. */
class GestionUsuariosIT extends PruebaIntegracion {
	private static final String ID_ADMIN = "10000000-0000-0000-0000-000000000001";
	private static final String ID_MARIA = "10000000-0000-0000-0000-000000000004";
	private static final String ID_ANA = "10000000-0000-0000-0000-000000000002";
	/** Luis Paredes, apoderado (padre) de dos estudiantes en los datos de prueba. */
	private static final String ID_LUIS = "10000000-0000-0000-0000-000000000003";
	private static final String ESTUDIANTE_2027 = "20000000-0000-0000-0000-000000000001";

	@Autowired
	private EntityManager entityManager;

	private String admin;

	@BeforeEach
	void iniciarSesion() throws Exception {
		admin = token(ADMIN);
	}

	/** Solicitud con todos los datos que exige el formulario de usuario (CAHU6). */
	private static String usuario(String dni, String nombres, String correo, String roles) {
		return usuario(dni, nombres, correo, "\"Docente\"", roles);
	}

	private static String usuario(String dni, String nombres, String correo, String profesion, String roles) {
		return """
				{"persona":{"tipoDocumento":"DNI","numeroDocumento":"%s","apellidoPaterno":"Pérez",
				"apellidoMaterno":"Quispe","nombres":"%s","fechaNacimiento":"1990-04-12","nacionalidad":"Peruana",
				"lugarNacimiento":"Perú","sexo":"FEMENINO","estadoCivil":"Soltero(a)","gradoInstruccion":"Superior",
				"celular":"999888777","correo":"%s","telefonoCasa":"014445555","profesion":%s,
				"centroTrabajo":"Colegio Galileo","cargo":"Docente","distrito":"Lince","direccion":"Av. Arequipa 2500"},
				"roles":%s}""".formatted(dni, nombres, correo, profesion, roles);
	}

	private static String maria(String roles) {
		return usuario("70000004", "María", "maria.fernandez@galiledu.edu.pe", roles);
	}

	private String crear(String json) throws Exception {
		return mvc.perform(conToken(post("/usuarios"), admin, json))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void creaUsuarioConNombreGeneradoYVariosRoles() throws Exception {
		String r = crear(usuario("45678912", "Ana Paula", "aperez@galiledu.edu.pe", "[\"DOCENTE\",\"COORDINADOR\"]"));
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
	void detalleDelApoderadoTraeSusEstudiantesACargo() throws Exception {
		mvc.perform(conToken(get("/usuarios/" + ID_LUIS), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tutorDe.length()").value(0))
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(2))
				// Del año académico más reciente al más antiguo.
				.andExpect(jsonPath("$.estudiantesACargo[0].estudianteId").value(ESTUDIANTE_2027))
				.andExpect(jsonPath("$.estudiantesACargo[0].grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$.estudiantesACargo[0].anioAcademico").value(2027))
				.andExpect(jsonPath("$.estudiantesACargo[0].estado").value("INSCRITO"))
				.andExpect(jsonPath("$.estudiantesACargo[0].parentesco").value("PADRE"))
				.andExpect(jsonPath("$.estudiantesACargo[0].viveConEstudiante").value(true))
				.andExpect(jsonPath("$.estudiantesACargo[0].esResponsable").value(true))
				.andExpect(jsonPath("$.estudiantesACargo[0].acreditacion").isEmpty())
				.andExpect(jsonPath("$.estudiantesACargo[1].anioAcademico").value(2026));
	}

	@Test
	void unEstudianteEliminadoYaNoSaleACargo() throws Exception {
		mvc.perform(conToken(delete("/estudiantes/" + ESTUDIANTE_2027), admin)).andExpect(status().isNoContent());
		// La prueba corre en una sola transacción: se escribe el borrado para que lo vea la consulta SQL.
		entityManager.flush();
		mvc.perform(conToken(get("/usuarios/" + ID_LUIS), admin))
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(1));
	}

	@Test
	void detalleDelTutorTraeSusSeccionesDelAnioEnCurso() throws Exception {
		mvc.perform(conToken(get("/usuarios/" + ID_MARIA), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(0))
				.andExpect(jsonPath("$.tutorDe.length()").value(1))
				.andExpect(jsonPath("$.tutorDe[0].seccion").value("1.° de secundaria A"))
				.andExpect(jsonPath("$.tutorDe[0].cargo").value("TUTOR"));
	}

	@Test
	void tutoriaIncluyeCotutoriaYExcluyeSeccionesInactivasYOtrosAnios() throws Exception {
		// 2026 (año ACTIVO): cotutora de 2.° A y tutora de 3.° A, pero esa sección está inactiva.
		entityManager.createNativeQuery("""
				UPDATE configuracion.seccion SET cotutor_id = CAST(:maria AS uuid)
				WHERE grado_codigo = 'SEC-2' AND nombre = 'A'
				  AND anio_escolar_id = '30000000-0000-0000-0000-000000002026'""")
				.setParameter("maria", ID_MARIA).executeUpdate();
		entityManager.createNativeQuery("""
				UPDATE configuracion.seccion SET tutor_id = CAST(:maria AS uuid), estado = 'INACTIVA'
				WHERE grado_codigo = 'SEC-3' AND nombre = 'A'
				  AND anio_escolar_id = '30000000-0000-0000-0000-000000002026'""")
				.setParameter("maria", ID_MARIA).executeUpdate();
		// 2027 (PLANIFICADO): no es el año en curso.
		entityManager.createNativeQuery("""
				UPDATE configuracion.seccion SET tutor_id = CAST(:maria AS uuid), cotutor_id = NULL
				WHERE grado_codigo = 'SEC-1' AND nombre = 'A'
				  AND anio_escolar_id = '30000000-0000-0000-0000-000000002027'""")
				.setParameter("maria", ID_MARIA).executeUpdate();

		mvc.perform(conToken(get("/usuarios/" + ID_MARIA), admin))
				.andExpect(jsonPath("$.tutorDe.length()").value(2))
				.andExpect(jsonPath("$.tutorDe[0].seccion").value("1.° de secundaria A"))
				.andExpect(jsonPath("$.tutorDe[0].cargo").value("TUTOR"))
				.andExpect(jsonPath("$.tutorDe[1].seccion").value("2.° de secundaria A"))
				.andExpect(jsonPath("$.tutorDe[1].cargo").value("COTUTOR"));
	}

	@Test
	void unEstudianteRetiradoSaleACargoComoRetirado() throws Exception {
		mvc.perform(conToken(post("/estudiantes/" + ESTUDIANTE_2027 + "/retirar"), admin))
				.andExpect(status().isNoContent());
		entityManager.flush();
		mvc.perform(conToken(get("/usuarios/" + ID_LUIS), admin))
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(2))
				.andExpect(jsonPath("$.estudiantesACargo[0].estado").value("RETIRADO"));
	}

	@Test
	void exigeTodosLosDatosDelUsuario() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678915", "Ana", "ana@gmail.com", "null", "[\"DOCENTE\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa la profesión u ocupación"));
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, """
				{"persona":{"tipoDocumento":"DNI","numeroDocumento":"70000004","apellidoPaterno":"Fernández",
				"nombres":"María","celular":"911222333","correo":"maria.fernandez@galiledu.edu.pe"},
				"roles":["DOCENTE"]}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa la fecha de nacimiento"));
	}

	@Test
	void validaTelefonoDeCasaYCorreoConExtension() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678916", "Ana", "ana@colegio", "[\"DOCENTE\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa un correo válido"));
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678917", "Ana", "ana@gmail.com", "[\"DOCENTE\"]").replace("014445555", "01-444")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El teléfono de casa solo puede contener números"));
	}

	@Test
	void editaDatosYRoles() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin,
				maria("[\"DOCENTE\",\"TUTOR\",\"COORDINADOR\"]").replace("999888777", "911222333")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.persona.celular").value("911222333"))
				.andExpect(jsonPath("$.roles.length()").value(3));
	}

	@Test
	void apoderadoTutorYEstudianteNoSeAsignanDesdeElFormulario() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678918", "Ana", "ana@gmail.com", "[\"DOCENTE\",\"APODERADO\"]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El rol Apoderado se asigna al inscribir a un estudiante"));
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678918", "Ana", "ana@gmail.com", "[\"ESTUDIANTE\"]")))
				.andExpect(status().isBadRequest());
		// María ya es Tutor: se conserva aunque no venga, y no se le puede agregar Apoderado.
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"DOCENTE\"]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles[?(@ == 'TUTOR')]").exists());
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"DOCENTE\",\"APODERADO\"]")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registrarElDocumentoDeUnEliminadoReactivaSuCuenta() throws Exception {
		mvc.perform(conToken(delete("/usuarios/" + ID_MARIA), admin)).andExpect(status().isNoContent());
		String r = crear(maria("[\"COORDINADOR\"]"));
		org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(r, "$.id")).isEqualTo(ID_MARIA);
		org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(r, "$.username")).isEqualTo(DOCENTE);
		mvc.perform(conToken(get("/usuarios/" + ID_MARIA), admin))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.roles.length()").value(1));
	}

	@Test
	void editarConCorreoDeOtroUsuarioDa409() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin,
				maria("[\"DOCENTE\"]").replace("maria.fernandez@", "ana.torres@")))
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
