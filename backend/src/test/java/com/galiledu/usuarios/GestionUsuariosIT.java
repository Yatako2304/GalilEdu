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
import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("Crea usuario con nombre generado y varios roles")
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

	@DisplayName("Rechaza documento duplicado")
	@Test
	void rechazaDocumentoDuplicado() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin, usuario("70000002", "Otra", "otra@gmail.com", "[\"DOCENTE\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("70000002")));
	}

	@DisplayName("Rechaza correo duplicado sin importar mayúsculas")
	@Test
	void rechazaCorreoDuplicadoSinImportarMayusculas() throws Exception {
		mvc.perform(conToken(post("/usuarios"), admin,
				usuario("45678900", "Otra", "ANA.TORRES@galiledu.edu.pe", "[\"DOCENTE\"]")))
				.andExpect(status().isConflict());
	}

	@DisplayName("Válida formato de datos")
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

	@DisplayName("Detalle del apoderado trae sus estudiantes a cargo")
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

	@DisplayName("Un estudiante eliminado ya no sale a cargo")
	@Test
	void unEstudianteEliminadoYaNoSaleACargo() throws Exception {
		mvc.perform(conToken(delete("/estudiantes/" + ESTUDIANTE_2027), admin)).andExpect(status().isNoContent());
		// La prueba corre en una sola transacción: se escribe el borrado para que lo vea la consulta SQL.
		entityManager.flush();
		mvc.perform(conToken(get("/usuarios/" + ID_LUIS), admin))
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(1));
	}

	@DisplayName("Detalle del tutor trae sus secciones del año en curso")
	@Test
	void detalleDelTutorTraeSusSeccionesDelAnioEnCurso() throws Exception {
		mvc.perform(conToken(get("/usuarios/" + ID_MARIA), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(0))
				.andExpect(jsonPath("$.tutorDe.length()").value(1))
				.andExpect(jsonPath("$.tutorDe[0].seccion").value("1.° de secundaria A"))
				.andExpect(jsonPath("$.tutorDe[0].cargo").value("TUTOR"));
	}

	@DisplayName("Tutoria incluye cotutoria y excluye secciones inactivas y otros años")
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

	@DisplayName("Un estudiante retirado sale a cargo como retirado")
	@Test
	void unEstudianteRetiradoSaleACargoComoRetirado() throws Exception {
		mvc.perform(conToken(post("/estudiantes/" + ESTUDIANTE_2027 + "/retirar"), admin))
				.andExpect(status().isNoContent());
		entityManager.flush();
		mvc.perform(conToken(get("/usuarios/" + ID_LUIS), admin))
				.andExpect(jsonPath("$.estudiantesACargo.length()").value(2))
				.andExpect(jsonPath("$.estudiantesACargo[0].estado").value("RETIRADO"));
	}

	@DisplayName("Exige todos los datos del usuario")
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

	@DisplayName("Válida teléfono de casa y correo con extensión")
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

	@DisplayName("Edita datos y roles")
	@Test
	void editaDatosYRoles() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin,
				maria("[\"DOCENTE\",\"TUTOR\",\"COORDINADOR\"]").replace("999888777", "911222333")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.persona.celular").value("911222333"))
				.andExpect(jsonPath("$.roles.length()").value(3));
	}

	@DisplayName("Apoderado tutor y estudiante no se asignan desde el formulario")
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

	@DisplayName("Registrar el documento de un eliminado reactiva su cuenta")
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

	@DisplayName("Editar con correo de otro usuario da409")
	@Test
	void editarConCorreoDeOtroUsuarioDa409() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin,
				maria("[\"DOCENTE\"]").replace("maria.fernandez@", "ana.torres@")))
				.andExpect(status().isConflict());
	}

	@DisplayName("Desactivado no puede entrar y reactivado si")
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

	@DisplayName("No puede desactivarse ni eliminarse a si mismo")
	@Test
	void noPuedeDesactivarseNiEliminarseASiMismo() throws Exception {
		mvc.perform(conToken(post("/usuarios/" + ID_ADMIN + "/desactivar"), admin)).andExpect(status().isBadRequest());
		mvc.perform(conToken(delete("/usuarios/" + ID_ADMIN), admin)).andExpect(status().isBadRequest());
	}

	@DisplayName("Eliminar es logico y desaparece de la lista")
	@Test
	void eliminarEsLogicoYDesapareceDeLaLista() throws Exception {
		mvc.perform(conToken(delete("/usuarios/" + ID_MARIA), admin)).andExpect(status().isNoContent());
		mvc.perform(conToken(get("/usuarios/" + ID_MARIA), admin)).andExpect(status().isNotFound());
		mvc.perform(conToken(get("/usuarios").param("q", "fernandez"), admin))
				.andExpect(jsonPath("$.meta.total").value(0));
	}

	@DisplayName("Lista filtra por rol estado y texto")
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

	@DisplayName("Resumen del inicio")
	@Test
	void resumenDelInicio() throws Exception {
		mvc.perform(conToken(get("/usuarios/resumen"), admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inactivos").value(1))
				.andExpect(jsonPath("$.cuentasInactivas[0].username").value("U20260007"));
	}

	@DisplayName("Desactivar corta la sesión abierta de inmediato")
	@Test
	void desactivarCortaLaSesionAbiertaDeInmediato() throws Exception {
		String tokenAna = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isOk());

		mvc.perform(conToken(post("/usuarios/" + ID_ANA + "/desactivar"), admin)).andExpect(status().isOk());
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isUnauthorized());

		mvc.perform(conToken(post("/usuarios/" + ID_ANA + "/reactivar"), admin)).andExpect(status().isOk());
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isOk());
	}

	/** HU-10: el cambio de rol vale desde la siguiente petición, aunque el token se haya emitido antes. */
	@DisplayName("Cambiar el rol cambia los permisos del token ya emitido")
	@Test
	void cambiarElRolCambiaLosPermisosDelTokenYaEmitido() throws Exception {
		String tokenMaria = token(DOCENTE);
		mvc.perform(conToken(get("/usuarios"), tokenMaria)).andExpect(status().isForbidden());

		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"ADMINISTRADOR\"]")))
				.andExpect(status().isOk());
		mvc.perform(conToken(get("/usuarios"), tokenMaria)).andExpect(status().isOk());

		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"DOCENTE\"]")))
				.andExpect(status().isOk());
		mvc.perform(conToken(get("/usuarios"), tokenMaria)).andExpect(status().isForbidden());
	}

	@DisplayName("Quitar el rol administrador corta sus permisos de inmediato")
	@Test
	void quitarElRolAdministradorCortaSusPermisosDeInmediato() throws Exception {
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"ADMINISTRADOR\"]")))
				.andExpect(status().isOk());
		String tokenMaria = token(DOCENTE);
		mvc.perform(conToken(get("/usuarios"), tokenMaria)).andExpect(status().isOk());

		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"DOCENTE\"]")))
				.andExpect(status().isOk());
		mvc.perform(conToken(get("/usuarios"), tokenMaria)).andExpect(status().isForbidden());
	}

	@DisplayName("Los roles solo se cambian en usuarios activos")
	@Test
	void losRolesSoloSeCambianEnUsuariosActivos() throws Exception {
		mvc.perform(conToken(post("/usuarios/" + ID_MARIA + "/desactivar"), admin)).andExpect(status().isOk());

		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"DOCENTE\",\"COORDINADOR\"]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se pueden cambiar los roles de un usuario activo"));

		// Los datos personales sí se editan (HU-9) mientras los roles queden igual.
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin,
				maria("[\"DOCENTE\"]").replace("999888777", "911222333")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.persona.celular").value("911222333"));

		mvc.perform(conToken(post("/usuarios/" + ID_MARIA + "/reactivar"), admin)).andExpect(status().isOk());
		mvc.perform(conToken(put("/usuarios/" + ID_MARIA), admin, maria("[\"DOCENTE\",\"COORDINADOR\"]")))
				.andExpect(status().isOk());
	}

	@DisplayName("Eliminar corta la sesión abierta")
	@Test
	void eliminarCortaLaSesionAbierta() throws Exception {
		String tokenAna = token(PERSONAL_ADMINISTRATIVO);
		mvc.perform(conToken(delete("/usuarios/" + ID_ANA), admin)).andExpect(status().isNoContent());
		mvc.perform(conToken(get("/estudiantes"), tokenAna)).andExpect(status().isUnauthorized());
	}

	@DisplayName("Solo el administrador gestiona usuarios")
	@Test
	void soloElAdministradorGestionaUsuarios() throws Exception {
		mvc.perform(conToken(get("/usuarios"), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(conToken(get("/usuarios"), token(PERSONAL_ADMINISTRATIVO))).andExpect(status().isForbidden());
		mvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
	}
}
