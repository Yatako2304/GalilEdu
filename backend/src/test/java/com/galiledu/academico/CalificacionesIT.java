package com.galiledu.academico;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
				    {"competenciaId": "%s", "pesoPorcentaje": 100}
				  ]
				}""".formatted(competencia)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("Prueba de regularidad"))
				.andExpect(jsonPath("$.competencias[0].competenciaId").value(competencia))
				.andExpect(jsonPath("$.competencias[0].pesoPorcentaje").value(100));
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
	void laBaseImpideCalificarUnaMatriculaDeOtraSeccion() {
		String matriculaOtraSeccion = "50000000-0000-0000-0000-000000000001";
		rechazaPor("fk_calificacion_matricula_seccion", () -> jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, registrado_por)
				VALUES (?::uuid, ?::uuid, ?::uuid, 18, '10000000-0000-0000-0000-000000000004')""")
				.params(item, seccion, matriculaOtraSeccion).update());
	}
}
