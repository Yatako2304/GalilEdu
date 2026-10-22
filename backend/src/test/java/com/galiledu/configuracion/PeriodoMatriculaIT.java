package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * HU-23: el periodo de matrícula (pantalla 13) por HTTP, con sus permisos. El reloj empieza el 15/09/2026 a las 10:00
 * (Lima): el periodo 2027 de los datos de prueba (15/09 al 31/10) está ABIERTO; con {@code reloj.fijarEn} antes del
 * 15/09
 * está PROGRAMADO y después del 31/10, CERRADO. El 2026 está CERRADO. Para crear se usa un año 2028 insertado por SQL,
 * con
 * el reloj ya pasado el cierre del 2027 y el tarifario confirmado por la API.
 */
class PeriodoMatriculaIT extends PruebaIntegracionConfiguracion {
	private static final String ID_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ID_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String RUTA = "/configuracion/periodos-matricula";
	private static final byte[] CSV = "nombre,dni\nAna,1\n".getBytes();

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private RelojPrueba reloj;

	private String personal;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personal = token(PERSONAL_ADMINISTRATIVO);
	}

	private static LocalDate fecha(int anio, int mes, int dia) {
		return LocalDate.of(anio, mes, dia);
	}

	private String periodoDe(String anioId) {
		return jdbc.sql("SELECT id FROM matricula.periodo_matricula WHERE anio_escolar_id = ?")
				.param(UUID.fromString(anioId)).query(UUID.class).single().toString();
	}

	private int periodosDelAnio(String anioId) {
		return jdbc.sql("SELECT count(*) FROM matricula.periodo_matricula WHERE anio_escolar_id = ?")
				.param(UUID.fromString(anioId)).query(Integer.class).single();
	}

	private String documentoDe(String periodoId) {
		return jdbc.sql("SELECT id FROM matricula.documento_requerido WHERE periodo_matricula_id = ? ORDER BY orden")
				.param(UUID.fromString(periodoId)).query(UUID.class).list().getFirst().toString();
	}

	/** Un año PLANIFICADO sin periodo, con secciones A de 1.° a 5.° de secundaria (capacidad 30). */
	private String anioConSecciones(int anio) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (id, anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (:id, :anio, :inicio, :fin, 'TRIMESTRAL', 'PLANIFICADO')""")
				.param("id", id).param("anio", anio).param("inicio", fecha(anio, 3, 1))
				.param("fin", fecha(anio, 12, 15)).update();
		entityManager.clear();
		jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				SELECT :id, g.codigo, 'A', 30 FROM configuracion.grado g WHERE g.nivel = 'SECUNDARIA'""")
				.param("id", id).update();
		return id.toString();
	}

	private void confirmarTarifario(String anioId) throws Exception {
		mvc.perform(conToken(post("/configuracion/tarifarios/" + anioId + "/confirmar"), personal, """
				{"montoCuotaIngreso":500,"montoMatricula":450,"montoPension":620,"diasGracia":5,
				"recargoPorcentaje":2,"topeRecargoPorcentaje":10}""")).andExpect(status().isOk());
	}

	/** Las ids de sección del formulario nuevo del año, en el orden del formulario (1.° A a 5.° A). */
	private String seccionDe(String anioId, int indice) throws Exception {
		String respuesta = mvc.perform(conToken(get(RUTA + "/nuevo").param("anioEscolarId", anioId), personal))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.secciones[" + indice + "].seccionId");
	}

	private static String cuerpo(String anioId, String inicio, String cierre, String vacantes, String documentos) {
		return """
				{"anioEscolarId":%s,"fechaInicio":%s,"horaInicio":"10:00","fechaCierre":%s,"horaCierre":"10:00",
				"vacantes":%s,"documentos":%s}""".formatted(texto(anioId), texto(inicio), texto(cierre), vacantes,
				documentos);
	}

	private static String texto(String valor) {
		return valor == null ? "null" : "\"" + valor + "\"";
	}

	private static String cuerpoValido(String anioId) {
		return cuerpo(anioId, "2026-12-01", "2027-02-15", "[]", """
				[{"nombre":"Ficha de matrícula"},{"nombre":"DNI del estudiante","descripcion":"Ambas caras"}]""");
	}

	/** Deja listo el 2028 (secciones, tarifario confirmado y el 2027 cerrado) y lo devuelve. */
	private String anio2028ListoParaCrear() throws Exception {
		reloj.fijarEn(fecha(2026, 11, 15));
		String id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);
		return id2028;
	}

	/** Crea el periodo 2028 con dos documentos y devuelve su id. */
	private String crearPeriodo2028(String id2028) throws Exception {
		String respuesta = mvc.perform(conToken(post(RUTA), personal, cuerpoValido(id2028)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(respuesta, "$.id");
	}

	private ResultActions subir(String periodoId, String documentoId, String nombre, byte[] contenido)
			throws Exception {
		return mvc.perform(conToken(multipart(RUTA + "/" + periodoId + "/documentos/" + documentoId + "/plantilla")
				.file(new MockMultipartFile("archivo", nombre, "application/octet-stream", contenido)), personal));
	}

	// --- GET listado, vigente, años disponibles ---

	@Test
	void elListadoTraeLosPeriodosDelMasRecienteAlMasAntiguoConSuEstado() throws Exception {
		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.meta.page").value(1))
				.andExpect(jsonPath("$.meta.total").value(2))
				.andExpect(jsonPath("$.data[0].nombre").value("Matrícula 2027"))
				.andExpect(jsonPath("$.data[0].anio").value(2027))
				.andExpect(jsonPath("$.data[0].estado").value("ABIERTO"))
				.andExpect(jsonPath("$.data[0].vigente").value(true))
				.andExpect(jsonPath("$.data[0].matriculas").value(1))
				.andExpect(jsonPath("$.data[0].inicio").value("2026-09-15T13:00:00Z"))
				.andExpect(jsonPath("$.data[1].nombre").value("Matrícula 2026"))
				.andExpect(jsonPath("$.data[1].estado").value("CERRADO"))
				.andExpect(jsonPath("$.data[1].vigente").value(false));
	}

	@Test
	void elListadoSeFiltraPorAnioYPorEstadoYSePagina() throws Exception {
		mvc.perform(conToken(get(RUTA).param("anio", "2026"), personal))
				.andExpect(jsonPath("$.meta.total").value(1))
				.andExpect(jsonPath("$.data[0].anio").value(2026));
		mvc.perform(conToken(get(RUTA).param("estado", "CERRADO"), personal))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].anio").value(2026));
		mvc.perform(conToken(get(RUTA).param("estado", "PROGRAMADO"), personal))
				.andExpect(jsonPath("$.data.length()").value(0))
				.andExpect(jsonPath("$.meta.totalPages").value(1));
		mvc.perform(conToken(get(RUTA).param("size", "1").param("page", "2"), personal))
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].anio").value(2026))
				.andExpect(jsonPath("$.meta.totalPages").value(2));
	}

	@Test
	void vigenteEsElPeriodoAbiertoYDa404SiNoHayNinguno() throws Exception {
		mvc.perform(conToken(get(RUTA + "/vigente"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.anio").value(2027))
				.andExpect(jsonPath("$.estado").value("ABIERTO"))
				.andExpect(jsonPath("$.bloquearPorMorosidad").value(true))
				.andExpect(jsonPath("$.permitirExtemporanea").value(true))
				.andExpect(jsonPath("$.vacantesTotales").value(300))
				.andExpect(jsonPath("$.matriculasRegistradas").value(1));

		reloj.fijarEn(fecha(2026, 9, 1));
		mvc.perform(conToken(get(RUTA + "/vigente"), personal))
				.andExpect(jsonPath("$.estado").value("PROGRAMADO"));

		reloj.fijarEn(fecha(2026, 11, 15));
		mvc.perform(conToken(get(RUTA + "/vigente"), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("No hay un periodo de matrícula vigente."));
	}

	@Test
	void losAniosDisponiblesSonLosQueAunNoTienenPeriodo() throws Exception {
		mvc.perform(conToken(get(RUTA + "/anios-disponibles"), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		anioConSecciones(2028);
		mvc.perform(conToken(get(RUTA + "/anios-disponibles"), personal))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].anio").value(2028))
				.andExpect(jsonPath("$[0].estado").value("PLANIFICADO"));
	}

	// --- GET /nuevo y GET /{id} ---

	@Test
	void elFormularioNuevoTraeLasSeccionesDelAnio() throws Exception {
		String id2028 = anioConSecciones(2028);

		mvc.perform(conToken(get(RUTA + "/nuevo").param("anioEscolarId", id2028), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(nullValue()))
				.andExpect(jsonPath("$.titulo").value("Matrícula 2028"))
				.andExpect(jsonPath("$.estado").value(nullValue()))
				.andExpect(jsonPath("$.editable").value(true))
				.andExpect(jsonPath("$.tarifarioConfirmado").value(false))
				.andExpect(jsonPath("$.vacantesTotales").value(150))
				.andExpect(jsonPath("$.cantidadSecciones").value(5))
				.andExpect(jsonPath("$.resumenSecciones").value("1.° a 5.°, A"))
				.andExpect(jsonPath("$.secciones[0].etiqueta").value("1.° A"))
				.andExpect(jsonPath("$.secciones[0].capacidad").value(30))
				.andExpect(jsonPath("$.documentos.length()").value(0));
	}

	@Test
	void elFormularioNuevoPideElAnioYRechazaUnoQueYaTienePeriodo() throws Exception {
		mvc.perform(conToken(get(RUTA + "/nuevo"), personal))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Indica el año escolar."));
		mvc.perform(conToken(get(RUTA + "/nuevo").param("anioEscolarId", ID_2027), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un periodo de matrícula para el año 2027."));
		mvc.perform(conToken(get(RUTA + "/nuevo").param("anioEscolarId", UUID.randomUUID().toString()), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El año escolar no existe."));
	}

	@Test
	void unPeriodoAbiertoSeVeEnSoloLecturaConElResumenDelModal() throws Exception {
		mvc.perform(conToken(get(RUTA + "/" + periodoDe(ID_2027)), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Matrícula 2027"))
				.andExpect(jsonPath("$.estado").value("ABIERTO"))
				.andExpect(jsonPath("$.editable").value(false))
				.andExpect(jsonPath("$.eliminable").value(false))
				.andExpect(jsonPath("$.fechaInicio").value("2026-09-15"))
				.andExpect(jsonPath("$.horaInicio").value("08:00:00"))
				.andExpect(jsonPath("$.fechaCierre").value("2026-10-31"))
				.andExpect(jsonPath("$.horaCierre").value("23:59:00"))
				.andExpect(jsonPath("$.diasDelPeriodo").value(46))
				.andExpect(jsonPath("$.vacantesTotales").value(300))
				.andExpect(jsonPath("$.cantidadSecciones").value(10))
				.andExpect(jsonPath("$.resumenSecciones").value("1.° a 5.°, A y B"))
				.andExpect(jsonPath("$.matriculasRegistradas").value(1))
				.andExpect(jsonPath("$.documentos.length()").value(4))
				.andExpect(jsonPath("$.documentos[0].nombre").value("Ficha de matrícula firmada"))
				.andExpect(jsonPath("$.documentos[0].tienePlantilla").value(false));

		mvc.perform(conToken(get(RUTA + "/" + periodoDe(ID_2026)), personal))
				.andExpect(jsonPath("$.estado").value("CERRADO"))
				.andExpect(jsonPath("$.editable").value(false));
		mvc.perform(conToken(get(RUTA + "/" + UUID.randomUUID()), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El periodo de matrícula no existe."));
	}

	// --- POST ---

	@Test
	void creaElPeriodoConSusVacantesYDocumentos() throws Exception {
		String id2028 = anio2028ListoParaCrear();
		String seccion1 = seccionDe(id2028, 0);

		mvc.perform(conToken(post(RUTA), personal, cuerpo(id2028, "2026-12-01", "2027-02-15",
				"[{\"seccionId\":\"" + seccion1 + "\",\"capacidad\":25}]",
				"""
						[{"nombre":"Ficha de matrícula"},{"nombre":"DNI del estudiante","descripcion":"Ambas caras"}]""")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.estado").value("PROGRAMADO"))
				.andExpect(jsonPath("$.editable").value(true))
				.andExpect(jsonPath("$.eliminable").value(true))
				.andExpect(jsonPath("$.fechaInicio").value("2026-12-01"))
				.andExpect(jsonPath("$.diasDelPeriodo").value(76))
				.andExpect(jsonPath("$.montoMatricula").value(450.0))
				.andExpect(jsonPath("$.bloquearPorMorosidad").value(true))
				.andExpect(jsonPath("$.permitirExtemporanea").value(true))
				.andExpect(jsonPath("$.vacantesTotales").value(145))
				.andExpect(jsonPath("$.secciones[0].capacidad").value(25))
				.andExpect(jsonPath("$.documentos.length()").value(2))
				.andExpect(jsonPath("$.documentos[1].descripcion").value("Ambas caras"));

		mvc.perform(conToken(get(RUTA + "/vigente"), personal))
				.andExpect(jsonPath("$.anio").value(2028))
				.andExpect(jsonPath("$.estado").value("PROGRAMADO"));
		mvc.perform(conToken(get(RUTA), personal))
				.andExpect(jsonPath("$.meta.total").value(3))
				.andExpect(jsonPath("$.data[0].anio").value(2028));
	}

	@Test
	void sinTarifarioConfirmadoNoSeHabilitaLaMatricula() throws Exception {
		reloj.fijarEn(fecha(2026, 11, 15));
		String id2028 = anioConSecciones(2028);

		mvc.perform(conToken(post(RUTA), personal, cuerpoValido(id2028)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Confirma el tarifario 2028 antes de habilitar la matrícula."));
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	@Test
	void noSeCreaUnPeriodoMientrasOtroSigueSinCerrar() throws Exception {
		String id2028 = anioConSecciones(2028);
		confirmarTarifario(id2028);

		mvc.perform(conToken(post(RUTA), personal, cuerpoValido(id2028))) // el 2027 está ABIERTO
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Ya existe un periodo de matrícula 2027 sin cerrar."));
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	@Test
	void unaVacanteInvalidaDa400YNoCambiaNada() throws Exception {
		String id2028 = anio2028ListoParaCrear();
		String seccion1 = seccionDe(id2028, 0);
		String seccion2 = seccionDe(id2028, 1);

		mvc.perform(conToken(post(RUTA), personal, cuerpo(id2028, "2026-12-01", "2027-02-15",
				"[{\"seccionId\":\"" + seccion2 + "\",\"capacidad\":20},{\"seccionId\":\"" + seccion1
						+ "\",\"capacidad\":46}]",
				"[{\"nombre\":\"Ficha\"}]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Las vacantes de la sección 1.° A deben estar entre 1 y 45."));

		assertThat(periodosDelAnio(id2028)).isZero();
		mvc.perform(conToken(get(RUTA + "/nuevo").param("anioEscolarId", id2028), personal))
				.andExpect(jsonPath("$.vacantesTotales").value(150))
				.andExpect(jsonPath("$.secciones[1].capacidad").value(30));
	}

	@Test
	void losDatosInvalidosDan400() throws Exception {
		String id2028 = anio2028ListoParaCrear();

		mvc.perform(conToken(post(RUTA), personal,
				cuerpo(id2028, null, "2027-02-15", "[]", "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa la fecha y la hora de inicio."));
		mvc.perform(conToken(post(RUTA), personal,
				cuerpo(id2028, "2026-12-01", "2026-12-01", "[]", "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La fecha de cierre debe ser posterior a la de inicio."));
		mvc.perform(conToken(post(RUTA), personal,
				cuerpo(id2028, "2026-12-01", "2027-02-15", "[]", "[{\"nombre\":\"DNI\"},{\"nombre\":\"dni\"}]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ya hay un documento llamado \"dni\" en este periodo."));
		mvc.perform(conToken(post(RUTA), personal, cuerpo(null, "2026-12-01", "2027-02-15", "[]", "[]")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Elige el año académico."));
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	// --- PUT /{id} ---

	@Test
	void editaUnPeriodoProgramado() throws Exception {
		reloj.fijarEn(fecha(2026, 9, 1)); // el 2027 está PROGRAMADO
		confirmarTarifario(ID_2027); // el de los datos de prueba está EN_CONFIGURACION
		String periodo = periodoDe(ID_2027);
		String ficha = mvc.perform(conToken(get(RUTA + "/" + periodo), personal)).andReturn().getResponse()
				.getContentAsString();
		String seccion1 = JsonPath.read(ficha, "$.secciones[0].seccionId");
		// los documentos del 2027 ya tienen entregas: se conservan (con su id) y se agrega uno nuevo
		StringBuilder documentos = new StringBuilder("[");
		for (int i = 0; i < 4; i++) {
			documentos.append("{\"id\":\"").append(JsonPath.<String>read(ficha, "$.documentos[" + i + "].id"))
					.append("\",\"nombre\":\"").append(JsonPath.<String>read(ficha, "$.documentos[" + i + "].nombre"))
					.append("\"},");
		}
		documentos.append("{\"nombre\":\"Ficha nueva\"}]");

		mvc.perform(conToken(put(RUTA + "/" + periodo), personal, cuerpo(null, "2026-09-20", "2026-11-30",
				"[{\"seccionId\":\"" + seccion1 + "\",\"capacidad\":40}]", documentos.toString())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PROGRAMADO"))
				.andExpect(jsonPath("$.fechaInicio").value("2026-09-20"))
				.andExpect(jsonPath("$.fechaCierre").value("2026-11-30"))
				.andExpect(jsonPath("$.vacantesTotales").value(310))
				.andExpect(jsonPath("$.documentos.length()").value(5))
				.andExpect(jsonPath("$.documentos[4].nombre").value("Ficha nueva"));
	}

	@Test
	void unPeriodoAbiertoOCerradoNoSeEdita() throws Exception {
		mvc.perform(conToken(put(RUTA + "/" + periodoDe(ID_2027)), personal,
				cuerpo(null, "2026-09-20", "2026-11-30", "[]", "[]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Una vez habilitado, el periodo no se puede editar; solo deshabilitar."));
		mvc.perform(conToken(put(RUTA + "/" + periodoDe(ID_2026)), personal,
				cuerpo(null, "2026-09-20", "2026-11-30", "[]", "[]")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("El periodo de matrícula ya cerró: no se puede editar."));
		mvc.perform(conToken(put(RUTA + "/" + UUID.randomUUID()), personal,
				cuerpo(null, "2026-09-20", "2026-11-30", "[]", "[]")))
				.andExpect(status().isNotFound());
	}

	// --- DELETE /{id} ---

	@Test
	void eliminaUnPeriodoProgramadoSinMatriculas() throws Exception {
		String id2028 = anio2028ListoParaCrear();
		String periodo = crearPeriodo2028(id2028);

		mvc.perform(conToken(delete(RUTA + "/" + periodo), personal)).andExpect(status().isNoContent());

		mvc.perform(conToken(get(RUTA + "/" + periodo), personal)).andExpect(status().isNotFound());
		assertThat(periodosDelAnio(id2028)).isZero();
	}

	@Test
	void noSeEliminaUnPeriodoProgramadoSiElAnioYaTieneMatriculas() throws Exception {
		reloj.fijarEn(fecha(2026, 9, 1));

		mvc.perform(conToken(delete(RUTA + "/" + periodoDe(ID_2027)), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("No se puede eliminar el periodo de matrícula: el año 2027 ya tiene matrículas."));
		assertThat(periodosDelAnio(ID_2027)).isEqualTo(1);
	}

	@Test
	void unPeriodoAbiertoNoSeEliminaYUnoInexistenteDa404() throws Exception {
		mvc.perform(conToken(delete(RUTA + "/" + periodoDe(ID_2027)), personal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se puede eliminar un periodo de matrícula programado."));
		mvc.perform(conToken(delete(RUTA + "/" + UUID.randomUUID()), personal)).andExpect(status().isNotFound());
	}

	// --- Plantillas ---

	@Test
	void subeDescargaYQuitaLaPlantillaDeUnDocumento() throws Exception {
		reloj.fijarEn(fecha(2026, 9, 1));
		String periodo = periodoDe(ID_2027);
		String documento = documentoDe(periodo);
		String ruta = RUTA + "/" + periodo + "/documentos/" + documento + "/plantilla";

		subir(periodo, documento, "ficha.csv", CSV)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(documento))
				.andExpect(jsonPath("$.tienePlantilla").value(true))
				.andExpect(jsonPath("$.plantillaNombreArchivo").value("ficha.csv"));

		mvc.perform(conToken(get(ruta), personal))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/csv"))
				.andExpect(header().string("Content-Disposition", containsString("attachment")))
				.andExpect(header().string("Content-Disposition", containsString("ficha.csv")))
				.andExpect(content().bytes(CSV));
		mvc.perform(conToken(get(RUTA + "/" + periodo), personal))
				.andExpect(jsonPath("$.documentos[0].tienePlantilla").value(true));

		mvc.perform(conToken(delete(ruta), personal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tienePlantilla").value(false));
		mvc.perform(conToken(get(ruta), personal))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El documento no tiene plantilla."));
	}

	@Test
	void unaPlantillaInvalidaDa400YNoSeGuarda() throws Exception {
		reloj.fijarEn(fecha(2026, 9, 1));
		String periodo = periodoDe(ID_2027);
		String documento = documentoDe(periodo);

		subir(periodo, documento, "ficha.pdf", CSV)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La plantilla debe ser un archivo XLSX, XLS o CSV."));
		subir(periodo, documento, "ficha.xlsx", CSV)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El contenido del archivo no corresponde a un XLSX válido."));
		subir(periodo, documento, "vacio.csv", new byte[0])
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Selecciona un archivo para la plantilla."));
		mvc.perform(conToken(multipart(RUTA + "/" + periodo + "/documentos/" + documento + "/plantilla"), personal))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Selecciona un archivo para la plantilla."));

		mvc.perform(conToken(get(RUTA + "/" + periodo), personal))
				.andExpect(jsonPath("$.documentos[0].tienePlantilla").value(false));
	}

	@Test
	void laPlantillaDeUnPeriodoAbiertoSoloSeDescarga() throws Exception {
		reloj.fijarEn(fecha(2026, 9, 1));
		String periodo = periodoDe(ID_2027);
		String documento = documentoDe(periodo);
		subir(periodo, documento, "ficha.csv", CSV).andExpect(status().isOk());

		reloj.fijarEn(fecha(2026, 10, 1)); // ABIERTO
		subir(periodo, documento, "otra.csv", CSV)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Solo se puede cambiar la plantilla de un periodo programado."));
		mvc.perform(conToken(delete(RUTA + "/" + periodo + "/documentos/" + documento + "/plantilla"), personal))
				.andExpect(status().isConflict());
		mvc.perform(conToken(get(RUTA + "/" + periodo + "/documentos/" + documento + "/plantilla"), personal))
				.andExpect(status().isOk())
				.andExpect(content().bytes(CSV));
	}

	@Test
	void laPlantillaDeUnDocumentoAjenoOInexistenteDa404() throws Exception {
		reloj.fijarEn(fecha(2026, 9, 1));
		String periodo = periodoDe(ID_2027);
		String documento2026 = documentoDe(periodoDe(ID_2026));

		subir(periodo, documento2026, "ficha.csv", CSV)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El documento no existe."));
		mvc.perform(conToken(get(RUTA + "/" + periodo + "/documentos/" + UUID.randomUUID() + "/plantilla"), personal))
				.andExpect(status().isNotFound());
		subir(UUID.randomUUID().toString(), documento2026, "ficha.csv", CSV)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El periodo de matrícula no existe."));
	}

	// --- Permisos ---

	@Test
	void soloElPersonalAdministrativoConfiguraLaMatricula() throws Exception {
		String periodo = periodoDe(ID_2027);
		String documento = documentoDe(periodo);
		String plantilla = RUTA + "/" + periodo + "/documentos/" + documento + "/plantilla";
		String cuerpo = cuerpoValido(ID_2027);

		for (String usuario : new String[]{DOCENTE, ADMIN}) {
			String token = token(usuario);
			mvc.perform(conToken(get(RUTA), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/vigente"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/anios-disponibles"), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/nuevo").param("anioEscolarId", ID_2027), token))
					.andExpect(status().isForbidden());
			mvc.perform(conToken(get(RUTA + "/" + periodo), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(post(RUTA), token, cuerpo)).andExpect(status().isForbidden());
			mvc.perform(conToken(put(RUTA + "/" + periodo), token, cuerpo)).andExpect(status().isForbidden());
			mvc.perform(conToken(delete(RUTA + "/" + periodo), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(multipart(plantilla).file(new MockMultipartFile("archivo", "f.csv", "text/csv", CSV)),
					token)).andExpect(status().isForbidden());
			mvc.perform(conToken(get(plantilla), token)).andExpect(status().isForbidden());
			mvc.perform(conToken(delete(plantilla), token)).andExpect(status().isForbidden());
		}
	}

	@Test
	void sinTokenTodoDa401() throws Exception {
		String periodo = periodoDe(ID_2027);
		String documento = documentoDe(periodo);
		String plantilla = RUTA + "/" + periodo + "/documentos/" + documento + "/plantilla";

		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA + "/vigente")).andExpect(status().isUnauthorized());
		mvc.perform(get(RUTA + "/" + periodo)).andExpect(status().isUnauthorized());
		mvc.perform(post(RUTA).contentType("application/json").content(cuerpoValido(ID_2027)))
				.andExpect(status().isUnauthorized());
		mvc.perform(put(RUTA + "/" + periodo).contentType("application/json").content(cuerpoValido(ID_2027)))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete(RUTA + "/" + periodo)).andExpect(status().isUnauthorized());
		mvc.perform(multipart(plantilla).file(new MockMultipartFile("archivo", "f.csv", "text/csv", CSV)))
				.andExpect(status().isUnauthorized());
		mvc.perform(get(plantilla)).andExpect(status().isUnauthorized());
		mvc.perform(delete(plantilla)).andExpect(status().isUnauthorized());
	}
}
