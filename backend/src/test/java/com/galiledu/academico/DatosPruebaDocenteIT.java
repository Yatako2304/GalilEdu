package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/** Comprueba la demo versionada que Flyway carga para probar las calificaciones de María. */
class DatosPruebaDocenteIT extends PruebaIntegracion {
	@Autowired
	private JdbcClient jdbc;

	@Test
	void mariaPuedeCalificarATresAlumnosEnTresCursos() throws Exception {
		String docente = token(DOCENTE);
		List<Map<String, Object>> cursos = cursosDemo();
		assertThat(cursos).hasSize(3);
		assertThat(cursos).extracting(c -> c.get("curso")).containsExactly("Aritmética", "Geometría", "Álgebra");
		for (var curso : cursos) {
			String plan = curso.get("plan_id").toString();
			String seccion = curso.get("seccion_id").toString();
			var items = jdbc.sql("""
					SELECT ie.id::text FROM academico.item_evaluacion ie
					JOIN configuracion.periodo_academico p ON p.id = ie.periodo_academico_id
					WHERE ie.plan_curso_id = ?::uuid AND ie.seccion_id = ?::uuid AND p.numero = 4 ORDER BY ie.nombre""")
					.params(plan, seccion).query(String.class).list();
			assertThat(items).hasSize(2);
			mvc.perform(conToken(get("/calificaciones/docente/items/" + items.get(0) + "/calificaciones"), docente))
					.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
		}
	}

	@Test
	void volverACargarLaDemoNoDuplicaDatosNiSobrescribeNotasOItems() throws Exception {
		var curso = cursosDemo().get(0);
		String item = jdbc.sql("""
				SELECT ie.id::text FROM academico.item_evaluacion ie
				WHERE ie.plan_curso_id = ?::uuid AND ie.seccion_id = ?::uuid ORDER BY ie.nombre LIMIT 1""")
				.params(curso.get("plan_id").toString(), curso.get("seccion_id").toString()).query(String.class)
				.single();
		jdbc.sql("UPDATE academico.item_evaluacion SET nombre = 'Práctica editada por el docente' WHERE id = ?::uuid")
				.param(item).update();
		jdbc.sql(
				"""
						INSERT INTO academico.calificacion_item (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, registrado_por)
						SELECT ?::uuid, m.seccion_id, m.id, 18, '10000000-0000-0000-0000-000000000004'
						FROM matricula.matricula m WHERE m.estudiante_id = md5('demo-calificaciones-maria-estudiante-1')::uuid""")
				.param(item).update();
		int correlativo = jdbc.sql("SELECT ultimo FROM matricula.correlativo_matricula WHERE anio = 2026")
				.query(Integer.class).single();
		String archivo = new ClassPathResource("db/datos-prueba/R__datos_prueba.sql")
				.getContentAsString(StandardCharsets.UTF_8);
		int inicio = archivo.indexOf("DO $$", archivo.indexOf("-- HU-57, HU-59 y HU-62 · Demo"));
		String bloque = archivo.substring(inicio, archivo.indexOf("-- FIN DEMO CALIFICACIONES DOCENTE", inicio));
		jdbc.sql(bloque).update();
		jdbc.sql(bloque).update();
		assertThat(cursosDemo()).hasSize(3);
		assertThat(jdbc.sql(
				"SELECT count(*) FROM estudiantes.estudiante WHERE id IN (SELECT md5('demo-calificaciones-maria-estudiante-' || n)::uuid FROM generate_series(1,3) n)")
				.query(Integer.class).single()).isEqualTo(3);
		assertThat(jdbc.sql("SELECT ultimo FROM matricula.correlativo_matricula WHERE anio = 2026").query(Integer.class)
				.single()).isEqualTo(correlativo);
		assertThat(jdbc.sql("SELECT nombre FROM academico.item_evaluacion WHERE id = ?::uuid").param(item)
				.query(String.class).single())
				.isEqualTo("Práctica editada por el docente");
		assertThat(jdbc.sql("SELECT nota_numerica FROM academico.calificacion_item WHERE item_evaluacion_id = ?::uuid")
				.param(item).query(Integer.class).single())
				.isEqualTo(18);
	}

	private List<Map<String, Object>> cursosDemo() {
		return jdbc.sql("""
				SELECT c.nombre AS curso, pc.id AS plan_id, ds.seccion_id
				FROM academico.docente_seccion ds JOIN academico.plan_curso pc ON pc.id = ds.plan_curso_id
				JOIN academico.curso c ON c.id = pc.curso_id JOIN configuracion.seccion s ON s.id = ds.seccion_id
				WHERE ds.docente_id = '10000000-0000-0000-0000-000000000004'
				AND pc.anio_escolar_id = '30000000-0000-0000-0000-000000002026'
				AND pc.grado_codigo = 'SEC-1' AND s.nombre = 'A' ORDER BY c.nombre COLLATE "C"
				""")
				.query().listOfRows();
	}
}
