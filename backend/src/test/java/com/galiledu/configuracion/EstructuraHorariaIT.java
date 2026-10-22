package com.galiledu.configuracion;

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
import com.jayway.jsonpath.JsonPath;

/**
 * HU-44: la API de estructuras horarias (CI-12 y el modal "Nueva estructura horaria") y sus permisos. Datos de prueba:
 * el año 2026 (ACTIVO) tiene EH-PRI-26 (8 bloques, 40 por semana, sale a las 14:20) y EH-SEC-26 (9 bloques, 45 por
 * semana, sale a las 14:50), cada una principal de su nivel. El año 2027 (PLANIFICADO) no tiene estructuras y es donde
 * se crean y cambian. Los planes de evaluación se arman por SQL en 2027.
 */
class EstructuraHorariaIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String EH_PRI_26 = "31000000-0000-0000-0000-000000000001";
	private static final String UUID_INEXISTENTE = "00000000-0000-0000-0000-000000000000";
	private static final String ANIO_NO_PLANIFICADO = "Solo se puede modificar la estructura horaria de un año planificado.";
	private static final String DIAS_LUN_A_VIE = "[\"LUN\",\"MAR\",\"MIE\",\"JUE\",\"VIE\"]";
	private static final String MENSAJE_28_HORAS_CON_20_BLOQUES = "No se puede reducir la jornada a 20 bloques por semana: 1.° de secundaria ya tiene 28 horas de cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos 28 bloques.";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private String personalAdministrativo;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personalAdministrativo = token(PERSONAL_ADMINISTRATIVO);
	}

	// ---------- Ayudas ----------

	private static String rutaDelAnio(String anioId) {
		return "/configuracion/anios-escolares/" + anioId + "/estructuras-horarias";
	}

	private static String rutaEstructura(Object estructuraId) {
		return "/configuracion/estructuras-horarias/" + estructuraId;
	}

	/** Lunes a viernes, entrada 08:00, 6 bloques de 45 min y sin recesos: 30 bloques por semana, salida 12:30. */
	private static String estructura(String codigo, String nivel, boolean esPrincipal) {
		return estructura(codigo, "Prueba", nivel, esPrincipal, DIAS_LUN_A_VIE, "08:00", 6, 45, "[]");
	}

	private static String estructura(String codigo, String nivel, boolean esPrincipal, int bloquesPorDia) {
		return estructura(codigo, "Prueba", nivel, esPrincipal, DIAS_LUN_A_VIE, "08:00", bloquesPorDia, 45, "[]");
	}

	private static String estructura(String codigo, String descripcion, String nivel, boolean esPrincipal,
			String dias, String horaEntrada, int bloquesPorDia, int duracionBloqueMin, String recesos) {
		return """
				{"codigo":"%s","descripcion":"%s","nivel":"%s","esPrincipal":%b,"diasLectivos":%s,"horaEntrada":"%s",\
				"bloquesPorDia":%d,"duracionBloqueMin":%d,"recesos":%s}"""
				.formatted(codigo, descripcion, nivel, esPrincipal, dias, horaEntrada, bloquesPorDia,
						duracionBloqueMin, recesos);
	}

	/** Cuerpo de editar: lo mismo que crear sin el nivel. */
	private static String edicion(String codigo, boolean esPrincipal, int bloquesPorDia) {
		return edicion(codigo, esPrincipal, DIAS_LUN_A_VIE, "08:00", bloquesPorDia, 45, "[]");
	}

	private static String edicion(String codigo, boolean esPrincipal, String dias, String horaEntrada,
			int bloquesPorDia, int duracionBloqueMin, String recesos) {
		return """
				{"codigo":"%s","descripcion":"Prueba","esPrincipal":%b,"diasLectivos":%s,"horaEntrada":"%s",\
				"bloquesPorDia":%d,"duracionBloqueMin":%d,"recesos":%s}"""
				.formatted(codigo, esPrincipal, dias, horaEntrada, bloquesPorDia, duracionBloqueMin, recesos);
	}

	/** Crea por la API en 2027 y devuelve el id de la estructura. */
	private String crear(String cuerpo) throws Exception {
		String respuesta = mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo, cuerpo))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.estructura.id");
	}

	/** Un curso planificado en 2027. 1.° de secundaria con 10 + 10 + 8 horas tiene 28 horas por semana. */
	private void planificarEn2027(String gradoCodigo, String curso, int horas) {
		UUID cursoId = jdbc.sql("SELECT id FROM academico.curso WHERE nombre = :nombre").param("nombre", curso)
				.query(UUID.class).single();
		jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				VALUES (:anioId, :gradoCodigo, :cursoId, :horas)""")
				.param("anioId", UUID.fromString(ANIO_2027)).param("gradoCodigo", gradoCodigo)
				.param("cursoId", cursoId).param("horas", horas).update();
		entityManager.clear();
	}

	private void planificar28HorasEnPrimeroDeSecundaria() {
		planificarEn2027("SEC-1", "Álgebra", 10);
		planificarEn2027("SEC-1", "Geometría", 10);
		planificarEn2027("SEC-1", "Aritmética", 8);
	}

	/** Crear con ese cuerpo en 2027 debe dar ese estado y ese mensaje. */
	private void exigeQueCrearDe(String cuerpo, int estado, String mensaje) throws Exception {
		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo, cuerpo))
				.andExpect(status().is(estado))
				.andExpect(jsonPath("$.message").value(mensaje));
	}

	// ---------- Listar y detalle ----------

	@Test
	void listaLasEstructurasDelAnioConSusIndicadores() throws Exception {
		mvc.perform(conToken(get(rutaDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anioModificable").value(false))
				.andExpect(jsonPath("$.estructuras", hasSize(2)))
				.andExpect(jsonPath("$.estructuras[0].codigo").value("EH-PRI-26"))
				.andExpect(jsonPath("$.estructuras[0].nivel").value("PRIMARIA"))
				.andExpect(jsonPath("$.estructuras[0].esPrincipal").value(true))
				.andExpect(jsonPath("$.estructuras[0].diasLectivos", hasSize(5)))
				.andExpect(jsonPath("$.estructuras[0].horaEntrada").value("07:45"))
				.andExpect(jsonPath("$.estructuras[0].horaSalida").value("14:20"))
				.andExpect(jsonPath("$.estructuras[0].bloquesPorDia").value(8))
				.andExpect(jsonPath("$.estructuras[0].duracionBloqueMin").value(45))
				.andExpect(jsonPath("$.estructuras[0].bloquesPorSemana").value(40))
				.andExpect(jsonPath("$.estructuras[0].recesos", hasSize(2)))
				.andExpect(jsonPath("$.estructuras[0].recesos[0].despuesDeBloque").value(3))
				.andExpect(jsonPath("$.estructuras[0].recesos[0].duracionMin").value(20))
				.andExpect(jsonPath("$.estructuras[1].codigo").value("EH-SEC-26"))
				.andExpect(jsonPath("$.estructuras[1].bloquesPorSemana").value(45))
				.andExpect(jsonPath("$.estructuras[1].horaSalida").value("14:50"));
	}

	@Test
	void unAnioPlanificadoSinEstructurasSeMarcaModificable() throws Exception {
		mvc.perform(conToken(get(rutaDelAnio(ANIO_2027)), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anioModificable").value(true))
				.andExpect(jsonPath("$.estructuras", hasSize(0)));
	}

	@Test
	void elDetalleTraeLaJornadaCalculadaFilaPorFila() throws Exception {
		mvc.perform(conToken(get(rutaEstructura(EH_PRI_26)), personalAdministrativo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("EH-PRI-26"))
				.andExpect(jsonPath("$.anioModificable").value(false))
				.andExpect(jsonPath("$.bloquesPorSemana").value(40))
				.andExpect(jsonPath("$.jornada", hasSize(10)))
				.andExpect(jsonPath("$.jornada[0].tipo").value("BLOQUE"))
				.andExpect(jsonPath("$.jornada[0].numero").value(1))
				.andExpect(jsonPath("$.jornada[0].inicio").value("07:45"))
				.andExpect(jsonPath("$.jornada[0].fin").value("08:30"))
				.andExpect(jsonPath("$.jornada[0].duracionMin").value(45))
				.andExpect(jsonPath("$.jornada[3].tipo").value("RECESO"))
				.andExpect(jsonPath("$.jornada[3].numero").doesNotExist())
				.andExpect(jsonPath("$.jornada[3].inicio").value("10:00"))
				.andExpect(jsonPath("$.jornada[3].fin").value("10:20"))
				.andExpect(jsonPath("$.jornada[3].duracionMin").value(20))
				.andExpect(jsonPath("$.jornada[9].numero").value(8))
				.andExpect(jsonPath("$.jornada[9].fin").value("14:20"));
	}

	// ---------- Crear ----------

	@Test
	void creaUnaEstructuraYLaPrimeraDelNivelNaceComoPrincipal() throws Exception {
		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo,
				estructura("  eh-pri-27 ", "primaria", false)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estructura.id").isNotEmpty())
				.andExpect(jsonPath("$.estructura.codigo").value("EH-PRI-27"))
				.andExpect(jsonPath("$.estructura.nivel").value("PRIMARIA"))
				.andExpect(jsonPath("$.estructura.esPrincipal").value(true))
				.andExpect(jsonPath("$.estructura.anioModificable").value(true))
				.andExpect(jsonPath("$.estructura.bloquesPorSemana").value(30))
				.andExpect(jsonPath("$.estructura.horaSalida").value("12:30"))
				.andExpect(jsonPath("$.estructura.jornada", hasSize(6)))
				.andExpect(jsonPath("$.advertencias", hasSize(0)));

		mvc.perform(conToken(get(rutaDelAnio(ANIO_2027)), personalAdministrativo))
				.andExpect(jsonPath("$.estructuras", hasSize(1)));
	}

	@Test
	void marcarComoPrincipalAlCrearBajaLaAnteriorYAvisa() throws Exception {
		crear(estructura("EH-A", "PRIMARIA", false));

		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo,
				estructura("EH-B", "PRIMARIA", true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estructura.esPrincipal").value(true))
				.andExpect(jsonPath("$.advertencias", hasSize(1)))
				.andExpect(jsonPath("$.advertencias[0]")
						.value("EH-B pasa a ser la estructura principal de primaria; EH-A deja de serlo."));

		mvc.perform(conToken(get(rutaDelAnio(ANIO_2027)), personalAdministrativo))
				.andExpect(jsonPath("$.estructuras[?(@.codigo=='EH-A')].esPrincipal", contains(false)))
				.andExpect(jsonPath("$.estructuras[?(@.codigo=='EH-B')].esPrincipal", contains(true)));
	}

	@Test
	void laJornadaPuedeTerminarALas1800ExactasYNoUnMinutoDespues() throws Exception {
		String recesoDe15TrasElSegundo = "[{\"despuesDeBloque\":2,\"duracionMin\":15}]";
		String tresDias = "[\"LUN\",\"MIE\",\"VIE\"]";

		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo, estructura("EH-TAR-26",
				"Secundaria · talleres de tarde", "SECUNDARIA", false, tresDias, "14:45", 4, 45,
				recesoDe15TrasElSegundo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estructura.horaSalida").value("18:00"))
				.andExpect(jsonPath("$.estructura.bloquesPorSemana").value(12));
		exigeQueCrearDe(estructura("EH-TAR-27", "Prueba", "SECUNDARIA", false, tresDias, "14:46", 4, 45,
				recesoDe15TrasElSegundo), 400, "La jornada debe terminar como máximo a las 18:00.");
	}

	@Test
	void rechazaConSuMensajeCadaReglaDeFormaDelFormulario() throws Exception {
		String receso = "[{\"despuesDeBloque\":%d,\"duracionMin\":%d}]";
		exigeQueCrearDe(estructura("   ", "PRIMARIA", false), 400, "Ingresa el código de la estructura.");
		exigeQueCrearDe(estructura("A".repeat(21), "PRIMARIA", false), 400, "El código admite hasta 20 caracteres.");
		exigeQueCrearDe(estructura("EH-A", "D".repeat(101), "PRIMARIA", false, DIAS_LUN_A_VIE, "08:00", 6, 45, "[]"),
				400, "La descripción admite hasta 100 caracteres.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, "[]", "08:00", 6, 45, "[]"), 400,
				"Elige al menos un día lectivo.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, "[\"LUN\",\"LUN\"]", "08:00", 6, 45, "[]"),
				400, "No repitas los días lectivos.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, "[\"LUN\",\"DOM\"]", "08:00", 6, 45, "[]"),
				400, "Día lectivo no válido: DOM.");
		exigeQueCrearDe(estructura("EH-A", "PRIMARIA", false, 13), 400, "Los bloques por día van de 1 a 12.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, DIAS_LUN_A_VIE, "08:00", 6, 19, "[]"), 400,
				"La duración del bloque va de 20 a 120 minutos.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, DIAS_LUN_A_VIE, "08:00", 6, 45,
				receso.formatted(3, 4)), 400, "El receso dura de 5 a 60 minutos.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, DIAS_LUN_A_VIE, "08:00", 6, 45,
				receso.formatted(6, 15)), 400, "Un receso va después de un bloque entre 1 y 5.");
		exigeQueCrearDe(estructura("EH-A", "Prueba", "PRIMARIA", false, DIAS_LUN_A_VIE, "08:00", 6, 45,
				"[{\"despuesDeBloque\":3,\"duracionMin\":10},{\"despuesDeBloque\":3,\"duracionMin\":15}]"), 400,
				"No puede haber dos recesos después del mismo bloque.");
		exigeQueCrearDe(estructura("EH-A", "INICIAL", false), 400, "Elige el nivel: primaria o secundaria.");
	}

	@Test
	void elCodigoRepetidoDa409AunqueVengaConEspaciosOEnMinusculas() throws Exception {
		crear(estructura("EH-A", "PRIMARIA", false));

		exigeQueCrearDe(estructura(" eh-a ", "SECUNDARIA", false), 409,
				"Ya existe una estructura con el código EH-A en este año.");
	}

	// ---------- Vista previa ----------

	@Test
	void laVistaPreviaCalculaLaJornadaDelModalYNoGuardaNada() throws Exception {
		String cuerpo = estructura("EH-TAR-26", "Secundaria · talleres de tarde", "SECUNDARIA", false,
				"[\"LUN\",\"MIE\",\"VIE\"]", "14:45", 4, 45, "[{\"despuesDeBloque\":2,\"duracionMin\":15}]");

		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027) + "/vista-previa"), personalAdministrativo, cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valida").value(true))
				.andExpect(jsonPath("$.errores", hasSize(0)))
				.andExpect(jsonPath("$.jornada.horaSalida").value("18:00"))
				.andExpect(jsonPath("$.jornada.bloquesPorSemana").value(12))
				.andExpect(jsonPath("$.jornada.filas", hasSize(5)))
				.andExpect(jsonPath("$.jornada.filas[2].tipo").value("RECESO"))
				.andExpect(jsonPath("$.jornada.filas[2].inicio").value("16:15"))
				.andExpect(jsonPath("$.jornada.filas[2].fin").value("16:30"));

		mvc.perform(conToken(get(rutaDelAnio(ANIO_2027)), personalAdministrativo))
				.andExpect(jsonPath("$.estructuras", hasSize(0)));
	}

	@Test
	void laVistaPreviaListaLosErroresYMuestraLaSalidaRealPasadaLas1800() throws Exception {
		String cuerpo = estructura("EH-TAR-26", "Prueba", "SECUNDARIA", false, "[\"LUN\"]", "14:46", 4, 45,
				"[{\"despuesDeBloque\":2,\"duracionMin\":15}]");

		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027) + "/vista-previa"), personalAdministrativo, cuerpo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valida").value(false))
				.andExpect(jsonPath("$.errores", hasSize(1)))
				.andExpect(jsonPath("$.errores[0]").value("La jornada debe terminar como máximo a las 18:00."))
				.andExpect(jsonPath("$.jornada.horaSalida").value("18:01"));
	}

	@Test
	void laVistaPreviaDeUnFormularioVacioResponde200ConLosErrores() throws Exception {
		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027) + "/vista-previa"), personalAdministrativo, "{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valida").value(false))
				.andExpect(jsonPath("$.errores", hasSize(6)))
				.andExpect(jsonPath("$.errores[0]").value("Ingresa el código de la estructura."))
				.andExpect(jsonPath("$.jornada").doesNotExist());
	}

	@Test
	void laVistaPreviaFuncionaEnUnAnioActivoPorqueNoGuarda() throws Exception {
		mvc.perform(conToken(post(rutaDelAnio(ANIO_2026) + "/vista-previa"), personalAdministrativo,
				estructura("EH-NUEVA", "PRIMARIA", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valida").value(true));
	}

	// ---------- Editar ----------

	@Test
	void editaLosDatosYRegeneraLosRecesos() throws Exception {
		String id = crear(estructura("EH-A", "PRIMARIA", false));

		mvc.perform(conToken(put(rutaEstructura(id)), personalAdministrativo, edicion("  eh-a2 ", true,
				"[\"LUN\",\"MAR\"]", "09:00", 4, 50, "[{\"despuesDeBloque\":2,\"duracionMin\":30}]")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.id").value(id))
				.andExpect(jsonPath("$.estructura.codigo").value("EH-A2"))
				.andExpect(jsonPath("$.estructura.nivel").value("PRIMARIA"))
				.andExpect(jsonPath("$.estructura.bloquesPorSemana").value(8))
				.andExpect(jsonPath("$.estructura.horaEntrada").value("09:00"))
				.andExpect(jsonPath("$.estructura.recesos", hasSize(1)))
				.andExpect(jsonPath("$.estructura.recesos[0].duracionMin").value(30))
				.andExpect(jsonPath("$.advertencias", hasSize(0)));

		mvc.perform(conToken(get(rutaEstructura(id)), personalAdministrativo))
				.andExpect(jsonPath("$.codigo").value("EH-A2"))
				.andExpect(jsonPath("$.recesos", hasSize(1)));
	}

	@Test
	void editarConDatosInvalidosDa400YNoCambiaNada() throws Exception {
		String id = crear(estructura("EH-A", "PRIMARIA", false));

		mvc.perform(conToken(put(rutaEstructura(id)), personalAdministrativo, edicion("EH-A", true, 13)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Los bloques por día van de 1 a 12."));

		mvc.perform(conToken(get(rutaEstructura(id)), personalAdministrativo))
				.andExpect(jsonPath("$.bloquesPorDia").value(6));
	}

	@Test
	void editarConElCodigoDeOtraEstructuraDa409() throws Exception {
		String id = crear(estructura("EH-A", "PRIMARIA", false));
		crear(estructura("EH-B", "SECUNDARIA", false));

		mvc.perform(conToken(put(rutaEstructura(id)), personalAdministrativo, edicion(" eh-b ", true, 6)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe una estructura con el código EH-B en este año."));
	}

	@Test
	void noSePuedeDesmarcarLaPrincipalYMarcarOtraLaCambiaYAvisa() throws Exception {
		String principal = crear(estructura("EH-A", "PRIMARIA", false));
		String otra = crear(estructura("EH-B", "PRIMARIA", false));

		mvc.perform(conToken(put(rutaEstructura(principal)), personalAdministrativo, edicion("EH-A", false, 6)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No se puede quitar la marca de estructura principal. "
						+ "Marca otra estructura de primaria como principal."));

		mvc.perform(conToken(put(rutaEstructura(otra)), personalAdministrativo, edicion("EH-B", true, 6)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.esPrincipal").value(true))
				.andExpect(jsonPath("$.advertencias[0]")
						.value("EH-B pasa a ser la estructura principal de primaria; EH-A deja de serlo."));
		mvc.perform(conToken(get(rutaEstructura(principal)), personalAdministrativo))
				.andExpect(jsonPath("$.esPrincipal").value(false));
	}

	// ---------- Planes de evaluación ----------

	@Test
	void reducirLaPrincipalPorDebajoDeLasHorasPlanificadasDa409() throws Exception {
		String principal = crear(estructura("EH-SEC", "SECUNDARIA", false));
		planificar28HorasEnPrimeroDeSecundaria();

		mvc.perform(conToken(put(rutaEstructura(principal)), personalAdministrativo, edicion("EH-SEC", true, 5)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No se puede reducir la jornada a 25 bloques por semana: "
						+ "1.° de secundaria ya tiene 28 horas de cursos planificadas. Reduce las horas del plan de "
						+ "evaluación o conserva al menos 28 bloques."));

		mvc.perform(conToken(get(rutaEstructura(principal)), personalAdministrativo))
				.andExpect(jsonPath("$.bloquesPorSemana").value(30));
	}

	@Test
	void cambiarLosBloquesDeLaPrincipalConPlanesAvisaQueCambiaLaPlanificacion() throws Exception {
		String principal = crear(estructura("EH-SEC", "SECUNDARIA", false));
		planificar28HorasEnPrimeroDeSecundaria();

		mvc.perform(conToken(put(rutaEstructura(principal)), personalAdministrativo, edicion("EH-SEC", true, 7)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estructura.bloquesPorSemana").value(35))
				.andExpect(jsonPath("$.advertencias", hasSize(1)))
				.andExpect(jsonPath("$.advertencias[0]").value("La jornada de secundaria pasa de 30 a 35 bloques por "
						+ "semana. Revisa el plan de evaluación: 1.° de secundaria tiene 28 horas planificadas."));
	}

	@Test
	void marcarComoPrincipalUnaConMenosBloquesQueLosPlanesDa409() throws Exception {
		crear(estructura("EH-SEC", "SECUNDARIA", false));
		String chica = crear(estructura("EH-TAR", "SECUNDARIA", false, 4));
		planificar28HorasEnPrimeroDeSecundaria();

		mvc.perform(conToken(put(rutaEstructura(chica)), personalAdministrativo, edicion("EH-TAR", true, 4)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(MENSAJE_28_HORAS_CON_20_BLOQUES));
		exigeQueCrearDe(estructura("EH-OTRA", "SECUNDARIA", true, 4), 409, MENSAJE_28_HORAS_CON_20_BLOQUES);
	}

	// ---------- Eliminar ----------

	@Test
	void eliminaUnaEstructuraYResponde204() throws Exception {
		crear(estructura("EH-A", "PRIMARIA", false));
		String otra = crear(estructura("EH-B", "PRIMARIA", false));

		mvc.perform(conToken(delete(rutaEstructura(otra)), personalAdministrativo))
				.andExpect(status().isNoContent());

		mvc.perform(conToken(get(rutaEstructura(otra)), personalAdministrativo))
				.andExpect(status().isNotFound());
		mvc.perform(conToken(get(rutaDelAnio(ANIO_2027)), personalAdministrativo))
				.andExpect(jsonPath("$.estructuras", hasSize(1)));
	}

	@Test
	void laPrincipalConOtrasDelNivelNoSeEliminaHayQueMarcarOtra() throws Exception {
		String principal = crear(estructura("EH-A", "PRIMARIA", false));
		crear(estructura("EH-B", "PRIMARIA", false));

		mvc.perform(conToken(delete(rutaEstructura(principal)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No se puede eliminar la estructura principal de primaria: "
						+ "quedan otras estructuras del nivel. Marca otra como principal primero."));
	}

	@Test
	void laUnicaDelNivelConPlanesNoSePuedeEliminarPeroSinPlanesSi() throws Exception {
		String secundaria = crear(estructura("EH-SEC", "SECUNDARIA", false));
		String primaria = crear(estructura("EH-PRI", "PRIMARIA", false));
		planificar28HorasEnPrimeroDeSecundaria();

		mvc.perform(conToken(delete(rutaEstructura(secundaria)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("No se puede eliminar la estructura principal de secundaria: "
						+ "hay cursos planificados que dependen de ella. Marca otra como principal primero."));
		// Los planes son de secundaria: la única de primaria sí se elimina.
		mvc.perform(conToken(delete(rutaEstructura(primaria)), personalAdministrativo))
				.andExpect(status().isNoContent());
	}

	// ---------- Año ACTIVO ----------

	@Test
	void unAnioActivoRechazaCrearEditarYEliminar() throws Exception {
		mvc.perform(conToken(post(rutaDelAnio(ANIO_2026)), personalAdministrativo,
				estructura("EH-NUEVA", "PRIMARIA", false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(ANIO_NO_PLANIFICADO));
		mvc.perform(conToken(put(rutaEstructura(EH_PRI_26)), personalAdministrativo, edicion("EH-PRI-26", true, 8)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(ANIO_NO_PLANIFICADO));
		mvc.perform(conToken(delete(rutaEstructura(EH_PRI_26)), personalAdministrativo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(ANIO_NO_PLANIFICADO));

		mvc.perform(conToken(get(rutaDelAnio(ANIO_2026)), personalAdministrativo))
				.andExpect(jsonPath("$.estructuras", hasSize(2)));
	}

	// ---------- 404 ----------

	@Test
	void loQueNoExisteDa404ConSuMensaje() throws Exception {
		String anioInexistente = "El año escolar no existe.";
		String estructuraInexistente = "La estructura horaria no existe.";

		mvc.perform(conToken(get(rutaDelAnio(UUID_INEXISTENTE)), personalAdministrativo))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(anioInexistente));
		mvc.perform(conToken(post(rutaDelAnio(UUID_INEXISTENTE)), personalAdministrativo,
				estructura("EH-A", "PRIMARIA", false)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(anioInexistente));
		mvc.perform(conToken(post(rutaDelAnio(UUID_INEXISTENTE) + "/vista-previa"), personalAdministrativo,
				estructura("EH-A", "PRIMARIA", false)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(anioInexistente));
		mvc.perform(conToken(get(rutaEstructura(UUID_INEXISTENTE)), personalAdministrativo))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(estructuraInexistente));
		mvc.perform(conToken(put(rutaEstructura(UUID_INEXISTENTE)), personalAdministrativo,
				edicion("EH-A", true, 6)))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(estructuraInexistente));
		mvc.perform(conToken(delete(rutaEstructura(UUID_INEXISTENTE)), personalAdministrativo))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(estructuraInexistente));
	}

	// ---------- Permisos ----------

	@Test
	void soloElPersonalAdministrativoPuedeUsarLasEstructurasHorarias() throws Exception {
		for (String usuario : new String[]{DOCENTE, ADMIN, COORDINADOR}) {
			String token = token(usuario);
			mvc.perform(conToken(get(rutaDelAnio(ANIO_2026)), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(rutaEstructura(EH_PRI_26)), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), token, estructura("EH-A", "PRIMARIA", false)))
					.andExpect(status().isForbidden());
			mvc.perform(conToken(post(rutaDelAnio(ANIO_2027) + "/vista-previa"), token,
					estructura("EH-A", "PRIMARIA", false))).andExpect(status().isForbidden());
			mvc.perform(conToken(put(rutaEstructura(EH_PRI_26)), token, edicion("EH-PRI-26", true, 8)))
					.andExpect(status().isForbidden());
			mvc.perform(conToken(delete(rutaEstructura(EH_PRI_26)), token)).andExpect(status().isForbidden());
		}
	}

	// ---------- Entrada inválida ----------

	@Test
	void unUuidMalEscritoEnLaRutaDevuelveElCodigoQueDaElManejadorCompartido() throws Exception {
		// ManejadorErrores trata MethodArgumentTypeMismatchException: un UUID mal escrito es un error del cliente (400),
		// no un error inesperado del servidor (antes respondía 500).
		mvc.perform(conToken(get(rutaEstructura("no-es-un-uuid")), personalAdministrativo))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(get(rutaDelAnio("no-es-un-uuid")), personalAdministrativo))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unJsonIlegibleDa400() throws Exception {
		// Observado: HttpMessageNotReadableException la trata el manejador compartido con 400.
		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo, "{\"codigo\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
	}

	@Test
	void unaHoraMalEscritaDa400() throws Exception {
		// Observado: "25:99" no se puede leer como hora y llega como HttpMessageNotReadableException: 400.
		String cuerpo = estructura("EH-A", "Prueba", "PRIMARIA", false, DIAS_LUN_A_VIE, "25:99", 6, 45, "[]");

		mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo, cuerpo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La solicitud tiene un formato inválido"));
	}
}
