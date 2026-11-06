package com.galiledu.academico;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/** Flujo del usuario Docente para HU-57, HU-59 y HU-62. */
class CalificacionesIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;

	private String docente;
	private String plan;
	private String seccion;
	private String periodo;
	private String item;

	@BeforeEach
	void preparar() throws Exception {
		docente = token(DOCENTE);
		plan = jdbc.sql("""
				SELECT pc.id::text FROM academico.plan_curso pc
				JOIN academico.curso c ON c.id = pc.curso_id
				WHERE c.nombre = 'Álgebra' AND pc.grado_codigo = 'SEC-1'""").query(String.class).single();
		seccion = jdbc.sql("""
				SELECT id::text FROM configuracion.seccion
				WHERE grado_codigo = 'SEC-1' AND nombre = 'A' AND anio_escolar_id =
				      '30000000-0000-0000-0000-000000002026'""").query(String.class).single();
		periodo = jdbc.sql("""
				SELECT id::text FROM configuracion.periodo_academico
				WHERE anio_escolar_id = '30000000-0000-0000-0000-000000002026' AND numero = 4""")
				.query(String.class).single();
		item = "80000000-0000-0000-0000-000000000001";
		prepararEscenarioCalificaciones();
	}

	private void prepararEscenarioCalificaciones() {
		jdbc.sql("""
				INSERT INTO personas.persona
				    (id, tipo_documento, numero_documento, apellido_paterno, apellido_materno, nombres,
				     fecha_nacimiento, nacionalidad, sexo, distrito, direccion)
				VALUES
				    ('00000000-0000-0000-0000-000000000104', 'DNI', '71000004', 'García', 'López',
				     'Ana Lucía', '2013-04-10', 'Peruana', 'FEMENINO', 'Pueblo Libre', 'Jr. Castilla 120'),
				    ('00000000-0000-0000-0000-000000000105', 'DNI', '71000005', 'Quispe', 'Mendoza',
				     'Diego José', '2013-08-22', 'Peruana', 'MASCULINO', 'San Miguel', 'Av. Universitaria 450')""")
				.update();
		jdbc.sql("""
				INSERT INTO estudiantes.estudiante
				    (id, persona_id, pais_nacimiento, lengua_materna, anio_academico, grado_codigo,
				     forma_ingreso, modo_salida, estado)
				VALUES
				    ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000104',
				     'Perú', 'Castellano', 2026, 'SEC-1', 'Edad normativa', 'SOLO', 'ACTIVO'),
				    ('20000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000105',
				     'Perú', 'Castellano', 2026, 'SEC-1', 'Edad normativa', 'SOLO', 'ACTIVO')""")
				.update();
		jdbc.sql("""
				INSERT INTO matricula.matricula
				    (id, estudiante_id, anio_escolar_id, grado_codigo, seccion_id, tipo_estudiante,
				     estado, codigo, confirmado_en, creado_por)
				VALUES
				    ('50000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004',
				     '30000000-0000-0000-0000-000000002026', 'SEC-1', ?::uuid, 'CONTINUADOR',
				     'MATRICULADO', 'MAT-2026-0003', '2026-02-11 09:00-05',
				     '10000000-0000-0000-0000-000000000002'),
				    ('50000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005',
				     '30000000-0000-0000-0000-000000002026', 'SEC-1', ?::uuid, 'CONTINUADOR',
				     'MATRICULADO', 'MAT-2026-0004', '2026-02-11 09:00-05',
				     '10000000-0000-0000-0000-000000000002')""")
				.params(seccion, seccion).update();
		jdbc.sql("""
				INSERT INTO academico.item_evaluacion
				    (id, plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
				     nombre, descripcion, tipo, creado_por)
				VALUES (?::uuid, ?::uuid, '30000000-0000-0000-0000-000000002026', 'SEC-1', ?::uuid,
				        ?::uuid, 'Práctica 1',
				        'Resuelve ecuaciones lineales aplicando propiedades de la igualdad.', 'PRACTICA',
				        '10000000-0000-0000-0000-000000000004')""")
				.params(item, plan, seccion, periodo).update();
		jdbc.sql("""
				INSERT INTO academico.item_competencia
				    (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
				SELECT ?::uuid, ?::uuid, competencia_id, 100
				FROM academico.plan_competencia WHERE plan_curso_id = ?::uuid""")
				.params(item, plan, plan).update();
		jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, registrado_por)
				VALUES
				    (?::uuid, ?::uuid, '50000000-0000-0000-0000-000000000004', 17,
				     '10000000-0000-0000-0000-000000000004'),
				    (?::uuid, ?::uuid, '50000000-0000-0000-0000-000000000005', 14,
				     '10000000-0000-0000-0000-000000000004')""")
				.params(item, seccion, item, seccion).update();
	}

	@Test
	void listaSoloLosCursosAsignadosAlDocente() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/cursos"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].curso").value("Álgebra"))
				.andExpect(jsonPath("$[0].grado").value("1.° de secundaria"))
				.andExpect(jsonPath("$[0].seccion").value("A"))
				.andExpect(jsonPath("$[0].escala").value("VIGESIMAL"))
				.andExpect(jsonPath("$[0].periodos[3].items").value(1));
	}

	@Test
	void registraUnItemConLaCompetenciaConfiguradaParaAlgebra() throws Exception {
		jdbc.sql("UPDATE academico.item_competencia SET peso_porcentaje = 50 WHERE item_evaluacion_id = ?::uuid")
				.param(item).update();
		String base = "/calificaciones/docente/cursos/" + plan + "/secciones/" + seccion + "/periodos/" + periodo;
		String competencia = jdbc.sql("""
				SELECT competencia_id::text FROM academico.plan_competencia
				WHERE plan_curso_id = ?::uuid""").param(plan).query(String.class).single();

		mvc.perform(conToken(post(base + "/items"), docente, """
				{
				  "nombre": "Prueba de regularidad",
				  "descripcion": "Operaciones algebraicas",
				  "tipo": "PRACTICA",
				  "competencias": [
				    {"competenciaId": "%s", "pesoPorcentaje": 50}
				  ]
				}""".formatted(competencia)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("Prueba de regularidad"))
				.andExpect(jsonPath("$.competencias[0].competenciaId").value(competencia))
				.andExpect(jsonPath("$.competencias[0].pesoPorcentaje").value(50));
	}

	@Test
	void rechazaUnItemCuandoLaCompetenciaYaTieneCienPorCiento() throws Exception {
		String competencia = jdbc
				.sql("SELECT competencia_id::text FROM academico.item_competencia WHERE item_evaluacion_id = ?::uuid")
				.param(item).query(String.class).single();
		String base = "/calificaciones/docente/cursos/" + plan + "/secciones/" + seccion + "/periodos/" + periodo;
		mvc.perform(conToken(post(base + "/items"), docente, """
				{"nombre":"Examen adicional","descripcion":"Operaciones algebraicas","tipo":"EXAMEN",
				 "competencias":[{"competenciaId":"%s","pesoPorcentaje":1}]}
				""".formatted(competencia)))
				.andExpect(status().isBadRequest())
				.andExpect(
						jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no puede superar el 100 %")));
	}

	@Test
	void editarNoDuplicaElPesoDelPropioItem() throws Exception {
		String competencia = jdbc
				.sql("SELECT competencia_id::text FROM academico.item_competencia WHERE item_evaluacion_id = ?::uuid")
				.param(item).query(String.class).single();
		mvc.perform(conToken(put("/calificaciones/docente/items/" + item), docente, """
				{"nombre":"Práctica corregida","descripcion":"Operaciones algebraicas","tipo":"PRACTICA",
				 "competencias":[{"competenciaId":"%s","pesoPorcentaje":100}]}
				""".formatted(competencia)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias[0].pesoPorcentaje").value(100));
	}

	@ParameterizedTest
	@ValueSource(strings = {"PROMEDIO_SIMPLE", "NIVEL_MAS_FRECUENTE", "ULTIMO_NIVEL"})
	void registraMasItemsSinPesoEnLosAniosQueNoSonPonderados(String regla) throws Exception {
		usarRegla(regla);
		String competencia = competenciaDelItem();
		String base = "/calificaciones/docente/cursos/" + plan + "/secciones/" + seccion + "/periodos/" + periodo;
		// El escenario ya tiene un ítem de 100 %: no debe impedir nuevas asociaciones sin peso.
		for (int numero = 1; numero <= 3; numero++) {
			mvc.perform(conToken(post(base + "/items"), docente, """
					{"nombre":"Ítem sin porcentaje %s","descripcion":"Criterio de evaluación","tipo":"PRACTICA",
					 "competencias":[{"competenciaId":"%s"}]}""".formatted(numero, competencia)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.competencias[0].pesoPorcentaje").value(org.hamcrest.Matchers.nullValue()));
		}
		org.assertj.core.api.Assertions.assertThat(jdbc.sql("""
				SELECT count(*) FROM academico.item_competencia ic
				JOIN academico.item_evaluacion ie ON ie.id = ic.item_evaluacion_id
				WHERE ie.plan_curso_id = ?::uuid AND ie.seccion_id = ?::uuid
				AND ie.periodo_academico_id = ?::uuid AND ic.peso_porcentaje IS NULL""")
				.params(plan, seccion, periodo).query(Integer.class).single()).isEqualTo(3);
	}

	@Test
	void editaUnItemSimpleSinConservarUnPorcentajeSinUso() throws Exception {
		usarRegla("PROMEDIO_SIMPLE");
		mvc.perform(conToken(put("/calificaciones/docente/items/" + item), docente, """
				{"nombre":"Práctica sin peso","descripcion":"Criterio de evaluación","tipo":"PRACTICA",
				 "competencias":[{"competenciaId":"%s","pesoPorcentaje":150}]}""".formatted(competenciaDelItem())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencias[0].pesoPorcentaje").value(org.hamcrest.Matchers.nullValue()));
		org.assertj.core.api.Assertions.assertThat(jdbc.sql("""
				SELECT count(*) FROM academico.calificacion_item WHERE item_evaluacion_id = ?::uuid""")
				.param(item).query(Integer.class).single()).isEqualTo(2);
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "0", "-1", "100.01", "0.001"})
	void exigeUnPesoValidoCuandoElAnioEsPonderado(String peso) throws Exception {
		String base = "/calificaciones/docente/cursos/" + plan + "/secciones/" + seccion + "/periodos/" + periodo;
		mvc.perform(conToken(post(base + "/items"), docente, """
				{"nombre":"Práctica con peso inválido","descripcion":"Criterio de evaluación","tipo":"PRACTICA",
				 "competencias":[{"competenciaId":"%s","pesoPorcentaje":%s}]}""".formatted(competenciaDelItem(), peso)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Ingresa un peso entre 0,01 y 100 % para cada competencia."));
	}

	@Test
	void laBasePermitePesoNuloYConservaElRangoDeLosPorcentajes() {
		jdbc.sql("UPDATE academico.item_competencia SET peso_porcentaje = NULL WHERE item_evaluacion_id = ?::uuid")
				.param(item).update();
		org.assertj.core.api.Assertions.assertThat(jdbc.sql("""
				SELECT count(*) FROM academico.item_competencia
				WHERE item_evaluacion_id = ?::uuid AND peso_porcentaje IS NULL""")
				.param(item).query(Integer.class).single()).isEqualTo(1);
		rechazaPor("ck_item_competencia_peso", () -> jdbc.sql("""
				UPDATE academico.item_competencia SET peso_porcentaje = 0 WHERE item_evaluacion_id = ?::uuid""")
				.param(item).update());
	}

	@Test
	void elPromedioSimpleYElPdfFuncionanConItemsCompartidosSinPeso() throws Exception {
		usarRegla("PROMEDIO_SIMPLE");
		String competencias = asociarSegundaCompetencia();
		jdbc.sql("UPDATE academico.item_competencia SET peso_porcentaje = NULL WHERE item_evaluacion_id = ?::uuid")
				.param(item).update();
		mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", competencias), docente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.filas[0].valores.resultado_general").value("17"));
		mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar-pdf").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", competencias), docente))
				.andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
	}

	private String competenciaDelItem() {
		return jdbc
				.sql("SELECT competencia_id::text FROM academico.item_competencia WHERE item_evaluacion_id = ?::uuid")
				.param(item).query(String.class).single();
	}

	private void usarRegla(String regla) {
		jdbc.sql("""
				UPDATE configuracion.anio_escolar SET regla_nota = ?, escala_evaluacion = ?
				WHERE id = (SELECT anio_escolar_id FROM academico.plan_curso WHERE id = ?::uuid)""")
				.params(regla, regla.startsWith("PROMEDIO") ? "VIGESIMAL" : "CUALITATIVA", plan).update();
	}

	@Test
	void consolidaLosItemsSeleccionadosConPromedioOpcional() throws Exception {
		String competencia = jdbc
				.sql("SELECT competencia_id::text FROM academico.item_competencia WHERE item_evaluacion_id = ?::uuid")
				.param(item).query(String.class).single();
		mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("objetivoId", competencia).param("itemIds", item).param("incluirPromedio", "true"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columnas[1].titulo").value("Promedio final"))
				.andExpect(jsonPath("$.filas[0].valores.resultado").value("17"))
				.andExpect(jsonPath("$.filas[0].valores.resultado_estado").value("Aprobado"));
		mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("objetivoId", competencia).param("itemIds", item).param("incluirPromedio", "false"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columnas.length()").value(1))
				.andExpect(jsonPath("$.filas[0].valores.resultado").doesNotExist());
	}

	@Test
	void consultaElItemLasNotasYElReporte() throws Exception {
		String base = "/calificaciones/docente/cursos/" + plan + "/secciones/" + seccion + "/periodos/" + periodo;
		mvc.perform(conToken(get(base + "/items"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Práctica 1"))
				.andExpect(jsonPath("$[0].competencias[0].pesoPorcentaje").value(100));

		mvc.perform(conToken(get("/calificaciones/docente/items/" + item + "/calificaciones"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.estudiante =~ /.*Ana Lucía.*/)].notaNumerica").value(
						org.hamcrest.Matchers.contains(17.0)));

		mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "ITEM")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("objetivoId", item), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Práctica 1"))
				.andExpect(jsonPath("$.filas.length()").value(2));
	}

	@Test
	void consolidaVariasCompetenciasYExportaLosMismosPromedios() throws Exception {
		String competencias = asociarSegundaCompetencia();
		mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", competencias).param("itemIds", item), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columnas.length()").value(5))
				.andExpect(jsonPath("$.columnas[0].competenciaId").value(competencias.split(",")[0]))
				.andExpect(jsonPath("$.columnas[2].competenciaId").value(competencias.split(",")[1]))
				.andExpect(jsonPath("$.columnas[4].clave").value("resultado_general"))
				.andExpect(jsonPath("$.filas[0].valores.resultado_general").value("17"))
				.andExpect(jsonPath("$.filas[1].valores.resultado_general").value("14"));
		mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", competencias).param("itemIds", item), docente))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(" · Práctica 1")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Promedio general")))
				.andExpect(
						content().string(org.hamcrest.Matchers.containsString(",\"17\",\"17\",\"17\",\"17\",\"17\"")));
	}

	@Test
	void variasCompetenciasSinPromedioMuestranSoloLosItems() throws Exception {
		String competencias = asociarSegundaCompetencia();
		mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", competencias).param("incluirPromedio", "false"), docente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.columnas.length()").value(2))
				.andExpect(jsonPath("$.filas[0].valores.resultado_general").doesNotExist());
	}

	@Test
	void rechazaListasInvalidasYDatosAjenosEnElConsolidadoMultiple() throws Exception {
		String competencias = asociarSegundaCompetencia();
		for (String noPermitida : new String[]{"no-es-un-uuid", java.util.UUID.randomUUID().toString()}) {
			mvc.perform(conToken(get("/calificaciones/docente/reportes").param("tipo", "COMPETENCIA")
					.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
					.param("competenciaIds", noPermitida), docente))
					.andExpect(status().isBadRequest());
		}
		mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", competencias).param("itemIds", java.util.UUID.randomUUID().toString()),
				docente))
				.andExpect(status().isBadRequest());
	}

	private String asociarSegundaCompetencia() {
		String primera = jdbc
				.sql("SELECT competencia_id::text FROM academico.plan_competencia WHERE plan_curso_id = ?::uuid")
				.param(plan).query(String.class).single();
		String segunda = jdbc.sql("""
				SELECT id::text FROM academico.competencia
				WHERE area_id = (SELECT area_id FROM academico.competencia WHERE id = ?::uuid)
				AND id <> ?::uuid ORDER BY orden LIMIT 1""").params(primera, primera).query(String.class).single();
		jdbc.sql("INSERT INTO academico.plan_competencia (plan_curso_id, competencia_id) VALUES (?::uuid, ?::uuid)")
				.params(plan, segunda).update();
		jdbc.sql(
				"""
						INSERT INTO academico.item_competencia (item_evaluacion_id, plan_curso_id, competencia_id, peso_porcentaje)
						VALUES (?::uuid, ?::uuid, ?::uuid, 100)""")
				.params(item, plan, segunda).update();
		return primera + "," + segunda;
	}

	@Test
	void exportaElMismoReporteEnCsv() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar").param("tipo", "ITEM")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("objetivoId", item), docente))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ana Lucía")));
	}

	@Test
	void unRolAdministrativoNoPuedeUsarElFlujoDelDocente() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/cursos"), token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}

	@Test
	void descargaElConsolidadoComoPdfConLosMismosResultados() throws Exception {
		String competencias = asociarSegundaCompetencia();
		var respuesta = mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar-pdf")
				.param("tipo", "COMPETENCIA").param("planCursoId", plan).param("seccionId", seccion)
				.param("periodoId", periodo).param("competenciaIds", competencias).param("itemIds", item), docente))
				.andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Content-Disposition", org.hamcrest.Matchers.containsString("consolidado-notas.pdf")))
				.andReturn().getResponse().getContentAsByteArray();
		try (var pdf = org.apache.pdfbox.Loader.loadPDF(respuesta)) {
			org.assertj.core.api.Assertions.assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf))
					.contains("Ana Lucía", "Diego José", "17/20", "14/20", "Promedio general", "Álgebra",
							"IV Bimestre");
		}
	}

	@Test
	void elPdfValidaElRolYLasCompetenciasAntesDeGenerarse() throws Exception {
		mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar-pdf").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo)
				.param("competenciaIds", java.util.UUID.randomUUID().toString()), docente))
				.andExpect(status().isBadRequest());
		mvc.perform(conToken(get("/calificaciones/docente/reportes/exportar-pdf").param("tipo", "COMPETENCIA")
				.param("planCursoId", plan).param("seccionId", seccion).param("periodoId", periodo),
				token(PERSONAL_ADMINISTRATIVO)))
				.andExpect(status().isForbidden());
	}

	@Test
	void laBaseImpideCalificarUnaMatriculaDeOtraSeccion() {
		String matriculaOtraSeccion = "50000000-0000-0000-0000-000000000001";
		rechazaPor("fk_calificacion_matricula_seccion", () -> jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, registrado_por)
				VALUES (?::uuid, ?::uuid, ?::uuid, 18, '10000000-0000-0000-0000-000000000004')""")
				.params(item, seccion, matriculaOtraSeccion).update());
	}
}
