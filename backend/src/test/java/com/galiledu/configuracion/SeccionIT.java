package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * HU-75: secciones de cada grado, sus tutores y permisos. Datos de prueba: el año 2026 (ACTIVO) tiene 1.° a 5.° de
 * secundaria con las secciones A y B de capacidad 30; María Fernández es tutora de 1.° A y Sofía está matriculada en
 * 4.° A. El año 2027 (PLANIFICADO) tiene las mismas secciones, con una matrícula reservada de Mateo en 1.° A.
 * <p>
 * Los grados llegan en orden: PRI-1 a PRI-6 son los índices 0 a 5 y SEC-1 a SEC-5 son los índices 6 a 10; dentro de un
 * grado, la sección A va antes que la B.
 */
class SeccionIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String MARIA_FERNANDEZ = "10000000-0000-0000-0000-000000000004";
	private static final String LUIS_PAREDES_APODERADO = "10000000-0000-0000-0000-000000000003";
	private static final String SOFIA_MATRICULA = "50000000-0000-0000-0000-000000000001";
	private static final String NOMBRE_DE_MARIA = "María Fernández Ríos";
	private static final String UUID_INEXISTENTE = "00000000-0000-0000-0000-000000000000";
	private static final String MENSAJE_ANIO_CERRADO = "El año escolar está cerrado: no se pueden modificar sus secciones.";
	private static final String MENSAJE_SECCION_CON_HISTORIAL = "La sección tiene matrículas o cambios de sección registrados. Inactívala para que no reciba más alumnos.";

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;

	private String personalAdministrativo;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personalAdministrativo = token(PERSONAL_ADMINISTRATIVO);
	}

	// ---------- Ayudas ----------

	private static String rutaSeccionesDelAnio(String anioId) {
		return "/configuracion/anios-escolares/" + anioId + "/secciones";
	}

	private static String rutaSeccion(Object seccionId) {
		return "/configuracion/secciones/" + seccionId;
	}

	private static String solicitudSeccion(String gradoCodigo, String nombre, int capacidadMaxima) {
		return """
				{"gradoCodigo":"%s","nombre":"%s","capacidadMaxima":%d}""".formatted(gradoCodigo, nombre,
				capacidadMaxima);
	}

	private static String solicitudCapacidad(int capacidadMaxima) {
		return """
				{"capacidadMaxima":%d}""".formatted(capacidadMaxima);
	}

	private static String solicitudTutores(Object tutorId, Object cotutorId) {
		String cotutor = cotutorId == null ? "null" : "\"" + cotutorId + "\"";
		return """
				{"tutorId":"%s","cotutorId":%s}""".formatted(tutorId, cotutor);
	}

	private UUID seccionId(String anioId, String gradoCodigo, String nombre) {
		return jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = :anioId AND grado_codigo = :gradoCodigo AND nombre = :nombre""")
				.param("anioId", UUID.fromString(anioId)).param("gradoCodigo", gradoCodigo).param("nombre", nombre)
				.query(UUID.class).single();
	}

	/** Un docente nuevo. Las pruebas usan documentos 8000000X, que no chocan con los datos de prueba. */
	private UUID crearUsuario(String numeroDocumento, String nombres, String estado, boolean eliminado,
			String... roles) {
		UUID personaId = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, nombres)
				VALUES ('DNI', :numeroDocumento, 'Prueba', :nombres) RETURNING id""")
				.param("numeroDocumento", numeroDocumento).param("nombres", nombres).query(UUID.class).single();
		UUID usuarioId = jdbc.sql("""
				INSERT INTO seguridad.usuario (persona_id, nombre_usuario, contrasena_hash, estado, eliminado)
				VALUES (:personaId, :nombreUsuario, 'sin-acceso', :estado, :eliminado) RETURNING id""")
				.param("personaId", personaId).param("nombreUsuario", "U" + numeroDocumento)
				.param("estado", estado).param("eliminado", eliminado).query(UUID.class).single();
		for (String rol : roles) {
			jdbc.sql("INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo) VALUES (:usuarioId, :rol)")
					.param("usuarioId", usuarioId).param("rol", rol).update();
		}
		return usuarioId;
	}

	private UUID crearDocente(String numeroDocumento, String nombres) {
		return crearUsuario(numeroDocumento, nombres, "ACTIVO", false, "DOCENTE");
	}

	/** Año 2025 CERRADO con una sección de 1.° de primaria; devuelve el id de esa sección. */
	private UUID crearAnioCerradoConSeccion() {
		UUID anioId = jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (2025, '2025-03-03', '2025-12-19', 'BIMESTRAL', 'CERRADO') RETURNING id""")
				.query(UUID.class).single();
		return jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				VALUES (:anioId, 'PRI-1', 'A', 30) RETURNING id""")
				.param("anioId", anioId).query(UUID.class).single();
	}

	/** Matrícula de Mateo en 2026 dentro de la sección indicada, con el estado indicado. */
	private void matricularAMateoEn2026(UUID seccionId, String estado) {
		jdbc.sql("""
				INSERT INTO matricula.matricula (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
				                                 tipo_estudiante, estado)
				VALUES ('50000000-0000-0000-0000-000000000099', '20000000-0000-0000-0000-000000000001',
				        :anioId, 'SEC-4', :seccionId, 'NUEVO', :estado)""")
				.param("anioId", UUID.fromString(ANIO_2026)).param("seccionId", seccionId).param("estado", estado)
				.update();
	}

	private void retirarMatricula(String matriculaId, String codigo) {
		jdbc.sql("""
				UPDATE matricula.matricula
				SET estado = 'RETIRADO', fecha_retiro = current_date, codigo = :codigo
				WHERE id = :matriculaId""")
				.param("codigo", codigo).param("matriculaId", UUID.fromString(matriculaId)).update();
	}

	/**
	 * Las peticiones comparten la sesión de la prueba: lo que se cambió por SQL no se ve en una sección ya cargada. Se
	 * vacía la caché para que la siguiente petición lea el estado actual, como lo haría en producción.
	 */
	private void releerDesdeLaBaseDeDatos() {
		entityManager.flush();
		entityManager.clear();
	}

	// ---------- Listar ----------

	@Test
	void listaLosOnceGradosDelAnioIncluidosLosQueNoTienenSecciones() throws Exception {
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(11))
				.andExpect(jsonPath("$[0].codigo").value("PRI-1"))
				.andExpect(jsonPath("$[0].cantidadSeccionesActivas").value(0))
				.andExpect(jsonPath("$[0].secciones.length()").value(0))
				.andExpect(jsonPath("$[6].codigo").value("SEC-1"))
				.andExpect(jsonPath("$[6].nombre").value("1.° de secundaria"))
				.andExpect(jsonPath("$[6].nivel").value("SECUNDARIA"))
				.andExpect(jsonPath("$[6].numero").value(1))
				.andExpect(jsonPath("$[6].cantidadSeccionesActivas").value(2))
				.andExpect(jsonPath("$[6].secciones.length()").value(2));
	}

	@Test
	void cadaSeccionTraeCapacidadAlumnosQueOcupanVacanteTutorYEstado() throws Exception {
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				// 1.° A de secundaria: sin alumnos y con María como tutora, sin cotutor.
				.andExpect(jsonPath("$[6].secciones[0].nombre").value("A"))
				.andExpect(jsonPath("$[6].secciones[0].gradoCodigo").value("SEC-1"))
				.andExpect(jsonPath("$[6].secciones[0].capacidadMaxima").value(30))
				.andExpect(jsonPath("$[6].secciones[0].alumnosOcupanVacante").value(0))
				.andExpect(jsonPath("$[6].secciones[0].tutorId").value(MARIA_FERNANDEZ))
				.andExpect(jsonPath("$[6].secciones[0].tutor").value(NOMBRE_DE_MARIA))
				.andExpect(jsonPath("$[6].secciones[0].cotutor").doesNotExist())
				.andExpect(jsonPath("$[6].secciones[0].estado").value("ACTIVA"))
				// 1.° B: sin tutor.
				.andExpect(jsonPath("$[6].secciones[1].nombre").value("B"))
				.andExpect(jsonPath("$[6].secciones[1].tutor").doesNotExist())
				// 4.° A: la matrícula de Sofía; 5.° A: la otra matrícula de prueba.
				.andExpect(jsonPath("$[9].secciones[0].alumnosOcupanVacante").value(1))
				.andExpect(jsonPath("$[9].secciones[1].alumnosOcupanVacante").value(0))
				.andExpect(jsonPath("$[10].secciones[0].alumnosOcupanVacante").value(1));
	}

	@Test
	void filtraPorNivelSinImportarMayusculasNiMinusculas() throws Exception {
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026) + "?nivel=PRIMARIA"), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(6))
				.andExpect(jsonPath("$[0].codigo").value("PRI-1"))
				.andExpect(jsonPath("$[5].codigo").value("PRI-6"));
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026) + "?nivel=secundaria"), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].codigo").value("SEC-1"));
	}

	@Test
	void rechazaUnNivelQueNoExiste() throws Exception {
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026) + "?nivel=INICIAL"), personalAdministrativo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El nivel debe ser PRIMARIA o SECUNDARIA."));
	}

	@Test
	void listarDeUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(get(rutaSeccionesDelAnio(UUID_INEXISTENTE)), personalAdministrativo))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@Test
	void unAnioCerradoSePuedeConsultar() throws Exception {
		crearAnioCerradoConSeccion();
		UUID anioCerradoId = jdbc.sql("SELECT id FROM configuracion.anio_escolar WHERE anio = 2025")
				.query(UUID.class).single();

		mvc.perform(conToken(get(rutaSeccionesDelAnio(anioCerradoId.toString())), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].secciones.length()").value(1));
	}

	// ---------- Crear ----------

	@Test
	void creaUnaSeccionActivaSinTutoresYElNombreSeGuardaEnMayusculas() throws Exception {
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("PRI-2", " c ", 28)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("C"))
				.andExpect(jsonPath("$.gradoCodigo").value("PRI-2"))
				.andExpect(jsonPath("$.capacidadMaxima").value(28))
				.andExpect(jsonPath("$.alumnosOcupanVacante").value(0))
				.andExpect(jsonPath("$.estado").value("ACTIVA"))
				.andExpect(jsonPath("$.tutor").doesNotExist());

		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(jsonPath("$[1].codigo").value("PRI-2"))
				.andExpect(jsonPath("$[1].cantidadSeccionesActivas").value(1))
				.andExpect(jsonPath("$[1].secciones[0].nombre").value("C"));
	}

	@Test
	void unaSeccionNuevaPuedeLlamarseComoLaDeOtroGradoOLaDeOtroAnio() throws Exception {
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("PRI-1", "A", 30))).andExpect(status().isCreated());
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2027)), personalAdministrativo,
				solicitudSeccion("PRI-1", "A", 30))).andExpect(status().isCreated());
	}

	@Test
	void rechazaUnNombreRepetidoEnElMismoGradoYAnio() throws Exception {
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("SEC-1", "A", 30)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe la sección A en 1.° de secundaria."));
		// "a " es la misma sección que "A": se compara el nombre ya normalizado.
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("SEC-1", "a ", 30)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe la sección A en 1.° de secundaria."));
	}

	@Test
	void validaElNombreLaCapacidadYElGradoDeLaSeccionNueva() throws Exception {
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("PRI-2", "  ", 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa el nombre de la sección."));
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("PRI-2", "ABCDEF", 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El nombre de la sección admite hasta 5 caracteres."));
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("PRI-2", "A-1", 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El nombre de la sección solo admite letras y números."));
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("PRI-2", "C", 46)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Entre 1 y 45 alumnos."));
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				"""
						{"gradoCodigo":"PRI-2","nombre":"C"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa la capacidad máxima."));
		mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo,
				solicitudSeccion("SEC-9", "C", 30)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El grado no existe."));
	}

	@Test
	void crearEnUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(post(rutaSeccionesDelAnio(UUID_INEXISTENTE)), personalAdministrativo,
				solicitudSeccion("PRI-2", "C", 30)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	// ---------- Cambiar la capacidad ----------

	@Test
	void cambiaLaCapacidadDeUnaSeccion() throws Exception {
		UUID seccionId = seccionId(ANIO_2026, "SEC-2", "A");

		mvc.perform(conToken(put(rutaSeccion(seccionId)), personalAdministrativo, solicitudCapacidad(25)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(seccionId.toString()))
				.andExpect(jsonPath("$.nombre").value("A"))
				.andExpect(jsonPath("$.capacidadMaxima").value(25));
	}

	@Test
	void laCapacidadNoBajaDeLosAlumnosQueOcupanVacante() throws Exception {
		UUID cuartoA = seccionId(ANIO_2026, "SEC-4", "A");
		matricularAMateoEn2026(cuartoA, "PENDIENTE_PAGO"); // 4.° A queda con 2 alumnos: Sofía y Mateo.
		releerDesdeLaBaseDeDatos();

		mvc.perform(conToken(put(rutaSeccion(cuartoA)), personalAdministrativo, solicitudCapacidad(1)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("La capacidad no puede ser menor que los alumnos que ocupan vacante (2)."));
		mvc.perform(conToken(put(rutaSeccion(cuartoA)), personalAdministrativo, solicitudCapacidad(2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.capacidadMaxima").value(2))
				.andExpect(jsonPath("$.alumnosOcupanVacante").value(2));
	}

	@Test
	void unaMatriculaRetiradaNoImpideBajarLaCapacidad() throws Exception {
		UUID cuartoA = seccionId(ANIO_2026, "SEC-4", "A");
		matricularAMateoEn2026(cuartoA, "PENDIENTE_PAGO");
		retirarMatricula("50000000-0000-0000-0000-000000000099", "MAT-2026-9001"); // Mateo se retira: queda Sofía.
		releerDesdeLaBaseDeDatos();

		mvc.perform(conToken(put(rutaSeccion(cuartoA)), personalAdministrativo, solicitudCapacidad(1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.capacidadMaxima").value(1))
				.andExpect(jsonPath("$.alumnosOcupanVacante").value(1));
	}

	@Test
	void validaElRangoDeLaCapacidad() throws Exception {
		UUID seccionId = seccionId(ANIO_2026, "SEC-2", "A");

		mvc.perform(conToken(put(rutaSeccion(seccionId)), personalAdministrativo, solicitudCapacidad(0)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Entre 1 y 45 alumnos."));
		mvc.perform(conToken(put(rutaSeccion(seccionId)), personalAdministrativo, solicitudCapacidad(46)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Entre 1 y 45 alumnos."));
		mvc.perform(conToken(put(rutaSeccion(seccionId)), personalAdministrativo, "{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa la capacidad máxima."));
	}

	// ---------- Asignar tutores ----------

	@Test
	void asignaTutorYCotutorYElCotutorSePuedeQuitar() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		UUID rosa = crearDocente("80000001", "Rosa");
		UUID jorge = crearDocente("80000002", "Jorge");

		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(rosa, jorge)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tutorId").value(rosa.toString()))
				.andExpect(jsonPath("$.tutor").value("Rosa Prueba"))
				.andExpect(jsonPath("$.cotutorId").value(jorge.toString()))
				.andExpect(jsonPath("$.cotutor").value("Jorge Prueba"));
		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(rosa, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tutor").value("Rosa Prueba"))
				.andExpect(jsonPath("$.cotutor").doesNotExist());
	}

	@Test
	void elTutorEsObligatorioYElCotutorDebeSerDistinto() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		UUID rosa = crearDocente("80000001", "Rosa");

		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				"""
						{"cotutorId":"%s"}""".formatted(rosa)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige un tutor."));
		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(rosa, rosa)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El cotutor debe ser distinto del tutor."));
	}

	@Test
	void tutorIgualAlCotutorDa400AunqueElTutorYaTengaOtraSeccion() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");

		// María ya es tutora de 1.° A, pero el error de la solicitud (mismo usuario en ambos cargos) se informa primero.
		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, MARIA_FERNANDEZ)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El cotutor debe ser distinto del tutor."));
	}

	@Test
	void tutorYCotutorDebenSerDocentesOTutoresActivos() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		UUID rosa = crearDocente("80000001", "Rosa");
		UUID docenteInactivo = crearUsuario("80000002", "Inactivo", "INACTIVO", false, "DOCENTE");
		UUID docenteEliminado = crearUsuario("80000003", "Eliminado", "ACTIVO", true, "DOCENTE");

		for (Object noElegible : new Object[]{LUIS_PAREDES_APODERADO, docenteInactivo, docenteEliminado}) {
			mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
					solicitudTutores(noElegible, null)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message")
							.value("El tutor debe ser un usuario activo con rol Docente o Tutor."));
			mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
					solicitudTutores(rosa, noElegible)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message")
							.value("El cotutor debe ser un usuario activo con rol Docente o Tutor."));
		}
	}

	@Test
	void unUsuarioConRolTutorSinRolDocenteTambienPuedeSerTutor() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		UUID soloTutor = crearUsuario("80000001", "Tutora", "ACTIVO", false, "TUTOR");

		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(soloTutor, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tutor").value("Tutora Prueba"));
	}

	@Test
	void unDocenteEsTutorDeUnaSolaSeccionPorAnio() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");

		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value(NOMBRE_DE_MARIA + " ya tiene la tutoría de 1.° A este año. Elige otro tutor."));
	}

	@Test
	void elTutorDeUnaSeccionPuedeConfirmarseEnLaMismaSeccionYSerTutorEnOtroAnio() throws Exception {
		UUID primeroA2026 = seccionId(ANIO_2026, "SEC-1", "A");
		UUID primeroA2027 = seccionId(ANIO_2027, "SEC-1", "A");

		mvc.perform(conToken(put(rutaSeccion(primeroA2026) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null))).andExpect(status().isOk());
		mvc.perform(conToken(put(rutaSeccion(primeroA2027) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null))).andExpect(status().isOk());
	}

	@Test
	void laReglaDelTutorUnicoNoAplicaAlCotutor() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		UUID rosa = crearDocente("80000001", "Rosa");

		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(rosa, MARIA_FERNANDEZ)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cotutor").value(NOMBRE_DE_MARIA));
	}

	@Test
	void reactivarConElTutorOcupadoEnOtraSeccionSeRechazaYSeResuelveCambiandoElTutor() throws Exception {
		UUID primeroA = seccionId(ANIO_2026, "SEC-1", "A"); // María es su tutora.
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		UUID rosa = crearDocente("80000001", "Rosa");

		mvc.perform(conToken(post(rutaSeccion(primeroA) + "/inactivar"), personalAdministrativo))
				.andExpect(status().isOk());
		// Mientras 1.° A está inactiva, María puede ser tutora de otra sección.
		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null))).andExpect(status().isOk());

		mvc.perform(conToken(post(rutaSeccion(primeroA) + "/reactivar"), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(NOMBRE_DE_MARIA
						+ " ya tiene la tutoría de 2.° A este año. Asígnale otro tutor a esta sección o inactiva la otra, y vuelve a reactivar."));
		// El rechazo no cambió nada: 1.° A sigue inactiva y con María como tutora.
		releerDesdeLaBaseDeDatos();
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(jsonPath("$[6].secciones[0].estado").value("INACTIVA"))
				.andExpect(jsonPath("$[6].secciones[0].tutor").value(NOMBRE_DE_MARIA));

		// Se asigna otro tutor a la sección inactiva y entonces sí se reactiva.
		mvc.perform(conToken(put(rutaSeccion(primeroA) + "/tutores"), personalAdministrativo,
				solicitudTutores(rosa, null))).andExpect(status().isOk());
		mvc.perform(conToken(post(rutaSeccion(primeroA) + "/reactivar"), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"))
				.andExpect(jsonPath("$.tutor").value("Rosa Prueba"));
	}

	@Test
	void unaAsignacionRechazadaNoDejaCambiosEnLaSeccion() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");

		mvc.perform(conToken(put(rutaSeccion(segundoA) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null))).andExpect(status().isConflict());

		releerDesdeLaBaseDeDatos();
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(jsonPath("$[7].secciones[0].nombre").value("A"))
				.andExpect(jsonPath("$[7].secciones[0].tutor").doesNotExist());
	}

	// ---------- Docentes elegibles ----------

	@Test
	void listaLosDocentesYTutoresActivosMarcandoQuienesYaSonTutoresDelAnio() throws Exception {
		UUID rosa = crearDocente("80000001", "Rosa");
		crearUsuario("80000002", "Inactivo", "INACTIVO", false, "DOCENTE");

		mvc.perform(conToken(get("/configuracion/secciones/tutores-disponibles?anioEscolarId=" + ANIO_2026),
				personalAdministrativo))
				.andExpect(status().isOk())
				// María tiene los roles DOCENTE y TUTOR y aparece una sola vez, marcada como tutora.
				.andExpect(jsonPath("$[?(@.id=='%s')]".formatted(MARIA_FERNANDEZ), hasSize(1)))
				.andExpect(jsonPath("$[?(@.id=='%s')].nombre".formatted(MARIA_FERNANDEZ), contains(NOMBRE_DE_MARIA)))
				.andExpect(jsonPath("$[?(@.id=='%s')].esTutorEsteAnio".formatted(MARIA_FERNANDEZ), contains(true)))
				.andExpect(jsonPath("$[?(@.id=='%s')].esTutorEsteAnio".formatted(rosa), contains(false)))
				// No aparecen un apoderado ni un docente inactivo.
				.andExpect(jsonPath("$[?(@.id=='%s')]".formatted(LUIS_PAREDES_APODERADO), hasSize(0)))
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void elTutorDeUnaSeccionInactivaNoApareceComoTutorDelAnio() throws Exception {
		UUID primeroA = seccionId(ANIO_2026, "SEC-1", "A");
		mvc.perform(conToken(post(rutaSeccion(primeroA) + "/inactivar"), personalAdministrativo))
				.andExpect(status().isOk());

		mvc.perform(conToken(get("/configuracion/secciones/tutores-disponibles?anioEscolarId=" + ANIO_2026),
				personalAdministrativo))
				.andExpect(jsonPath("$[?(@.id=='%s')].esTutorEsteAnio".formatted(MARIA_FERNANDEZ), contains(false)));
	}

	@Test
	void docentesElegiblesDeUnAnioQueNoExisteDa404() throws Exception {
		mvc.perform(conToken(get("/configuracion/secciones/tutores-disponibles?anioEscolarId=" + UUID_INEXISTENTE),
				personalAdministrativo))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@Test
	void docentesElegiblesSinElAnioEscolarDa400() throws Exception {
		mvc.perform(conToken(get("/configuracion/secciones/tutores-disponibles"), personalAdministrativo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Indica el año escolar."));
	}

	// ---------- Inactivar y reactivar ----------

	@Test
	void inactivaYReactivaUnaSeccionSinPerderSusAlumnos() throws Exception {
		UUID cuartoA = seccionId(ANIO_2026, "SEC-4", "A"); // Tiene la matrícula de Sofía.

		mvc.perform(conToken(post(rutaSeccion(cuartoA) + "/inactivar"), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("INACTIVA"))
				.andExpect(jsonPath("$.alumnosOcupanVacante").value(1));
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				// La inactiva sigue en la lista, pero ya no cuenta como sección activa del grado.
				.andExpect(jsonPath("$[9].secciones.length()").value(2))
				.andExpect(jsonPath("$[9].cantidadSeccionesActivas").value(1));
		mvc.perform(conToken(post(rutaSeccion(cuartoA) + "/reactivar"), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	@Test
	void noSeInactivaUnaSeccionInactivaNiSeReactivaUnaActiva() throws Exception {
		UUID segundoB = seccionId(ANIO_2026, "SEC-2", "B");

		mvc.perform(conToken(post(rutaSeccion(segundoB) + "/reactivar"), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La sección ya está activa."));
		mvc.perform(conToken(post(rutaSeccion(segundoB) + "/inactivar"), personalAdministrativo))
				.andExpect(status().isOk());
		mvc.perform(conToken(post(rutaSeccion(segundoB) + "/inactivar"), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("La sección ya está inactiva."));
	}

	// ---------- Eliminar ----------

	@Test
	void eliminaUnaSeccionSinMatriculasEnUnAnioActivoYSusDocentesDelPlanSeBorranEnCascada() throws Exception {
		UUID primeroA = seccionId(ANIO_2026, "SEC-1", "A");
		int docentesAsignadosAntes = jdbc.sql("SELECT count(*) FROM academico.docente_seccion WHERE seccion_id = :id")
				.param("id", primeroA).query(Integer.class).single();
		assertThat(docentesAsignadosAntes).as("María dicta un curso en 1.° A").isEqualTo(1);

		mvc.perform(conToken(delete(rutaSeccion(primeroA)), personalAdministrativo))
				.andExpect(status().isNoContent());

		releerDesdeLaBaseDeDatos();
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.seccion WHERE id = :id").param("id", primeroA)
				.query(Integer.class).single()).isZero();
		assertThat(jdbc.sql("SELECT count(*) FROM academico.docente_seccion WHERE seccion_id = :id")
				.param("id", primeroA).query(Integer.class).single()).isZero();
		// El plan del grado no se toca: solo se quitó la asignación de esa sección.
		assertThat(jdbc.sql("""
				SELECT count(*) FROM academico.plan_curso
				WHERE anio_escolar_id = :anioId AND grado_codigo = 'SEC-1'""")
				.param("anioId", UUID.fromString(ANIO_2026)).query(Integer.class).single()).isEqualTo(16);
		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(jsonPath("$[6].secciones.length()").value(1));
	}

	@Test
	void eliminaUnaSeccionDeUnAnioPlanificado() throws Exception {
		UUID segundoA2027 = seccionId(ANIO_2027, "SEC-2", "A");

		mvc.perform(conToken(delete(rutaSeccion(segundoA2027)), personalAdministrativo))
				.andExpect(status().isNoContent());
	}

	@Test
	void noEliminaUnaSeccionConMatriculas() throws Exception {
		UUID cuartoA = seccionId(ANIO_2026, "SEC-4", "A"); // Matrícula MATRICULADO de Sofía.

		mvc.perform(conToken(delete(rutaSeccion(cuartoA)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(MENSAJE_SECCION_CON_HISTORIAL));
	}

	@Test
	void unaMatriculaRetiradaYaNoOcupaVacantePeroImpideEliminarLaSeccion() throws Exception {
		UUID cuartoA = seccionId(ANIO_2026, "SEC-4", "A");
		retirarMatricula(SOFIA_MATRICULA, "MAT-2026-0001");
		releerDesdeLaBaseDeDatos();

		mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(jsonPath("$[9].secciones[0].alumnosOcupanVacante").value(0));
		mvc.perform(conToken(delete(rutaSeccion(cuartoA)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(MENSAJE_SECCION_CON_HISTORIAL));
	}

	@Test
	void unCambioDeSeccionImpideEliminarLaSeccionDeOrigenYLaDeDestino() throws Exception {
		UUID segundoB = seccionId(ANIO_2026, "SEC-2", "B"); // Sin matrículas.
		UUID tercerB = seccionId(ANIO_2026, "SEC-3", "B"); // Sin matrículas.
		jdbc.sql("""
				INSERT INTO matricula.cambio_seccion (matricula_id, seccion_origen_id, seccion_destino_id)
				VALUES (:matriculaId, :origenId, :destinoId)""")
				.param("matriculaId", UUID.fromString(SOFIA_MATRICULA)).param("origenId", segundoB)
				.param("destinoId", tercerB).update();
		releerDesdeLaBaseDeDatos();

		mvc.perform(conToken(delete(rutaSeccion(segundoB)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(MENSAJE_SECCION_CON_HISTORIAL));
		mvc.perform(conToken(delete(rutaSeccion(tercerB)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(MENSAJE_SECCION_CON_HISTORIAL));
	}

	// ---------- Año cerrado ----------

	@Test
	void unAnioCerradoNoAdmiteNingunCambioEnSusSecciones() throws Exception {
		UUID seccionId = crearAnioCerradoConSeccion();
		UUID anioCerradoId = jdbc.sql("SELECT id FROM configuracion.anio_escolar WHERE anio = 2025")
				.query(UUID.class).single();
		releerDesdeLaBaseDeDatos();

		mvc.perform(conToken(post(rutaSeccionesDelAnio(anioCerradoId.toString())), personalAdministrativo,
				solicitudSeccion("PRI-2", "B", 30)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE_ANIO_CERRADO));
		mvc.perform(conToken(put(rutaSeccion(seccionId)), personalAdministrativo, solicitudCapacidad(20)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE_ANIO_CERRADO));
		mvc.perform(conToken(put(rutaSeccion(seccionId) + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE_ANIO_CERRADO));
		mvc.perform(conToken(post(rutaSeccion(seccionId) + "/inactivar"), personalAdministrativo))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE_ANIO_CERRADO));
		mvc.perform(conToken(post(rutaSeccion(seccionId) + "/reactivar"), personalAdministrativo))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE_ANIO_CERRADO));
		mvc.perform(conToken(delete(rutaSeccion(seccionId)), personalAdministrativo))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(MENSAJE_ANIO_CERRADO));
	}

	// ---------- 404 y permisos ----------

	@Test
	void unaSeccionQueNoExisteDa404EnCadaOperacion() throws Exception {
		String ruta = rutaSeccion(UUID_INEXISTENTE);
		String mensaje = "La sección no existe.";

		mvc.perform(conToken(put(ruta), personalAdministrativo, solicitudCapacidad(30)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(mensaje));
		mvc.perform(conToken(put(ruta + "/tutores"), personalAdministrativo,
				solicitudTutores(MARIA_FERNANDEZ, null)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(mensaje));
		mvc.perform(conToken(delete(ruta), personalAdministrativo))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(mensaje));
		mvc.perform(conToken(post(ruta + "/inactivar"), personalAdministrativo))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(mensaje));
		mvc.perform(conToken(post(ruta + "/reactivar"), personalAdministrativo))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(mensaje));
	}

	@Test
	void soloElPersonalAdministrativoPuedeUsarLasSecciones() throws Exception {
		UUID segundoA = seccionId(ANIO_2026, "SEC-2", "A");
		String ruta = rutaSeccion(segundoA);
		for (String usuario : new String[]{DOCENTE, ADMIN, COORDINADOR}) {
			String token = token(usuario);
			mvc.perform(conToken(get(rutaSeccionesDelAnio(ANIO_2026)), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(rutaSeccionesDelAnio(ANIO_2026)), token,
					solicitudSeccion("PRI-2", "C", 30))).andExpect(status().isForbidden());
			mvc.perform(conToken(put(ruta), token, solicitudCapacidad(30))).andExpect(status().isForbidden());
			mvc.perform(conToken(put(ruta + "/tutores"), token, solicitudTutores(MARIA_FERNANDEZ, null)))
					.andExpect(status().isForbidden());
			mvc.perform(conToken(delete(ruta), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(ruta + "/inactivar"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(ruta + "/reactivar"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get("/configuracion/secciones/tutores-disponibles?anioEscolarId=" + ANIO_2026),
					token)).andExpect(status().isForbidden());
		}
		mvc.perform(get(rutaSeccionesDelAnio(ANIO_2026))).andExpect(status().isUnauthorized());
		mvc.perform(delete(ruta)).andExpect(status().isUnauthorized());
	}
}
