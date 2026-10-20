package com.galiledu.academico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Tablas del catálogo académico y ambientes (V6): las relaciones con año, grado y sección, y las reglas que guarda la
 * propia BD.
 */
class EsquemaAcademicoIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String MARIA_FERNANDEZ = "10000000-0000-0000-0000-000000000004";

	@Autowired
	private JdbcClient jdbc;

	@Test
	void elDocenteLlegaASusCursosSeccionesCompetenciasYAula() {
		var clases = jdbc.sql("""
				SELECT cu.nombre AS curso, g.nombre AS grado, s.nombre AS seccion, am.codigo AS aula,
				       pc.horas_semanales AS horas, count(pco.competencia_id) AS competencias
				FROM academico.docente_seccion ds
				JOIN academico.plan_curso pc ON pc.id = ds.plan_curso_id
				JOIN academico.curso cu ON cu.id = pc.curso_id
				JOIN configuracion.grado g ON g.codigo = pc.grado_codigo
				JOIN configuracion.seccion s ON s.id = ds.seccion_id
				LEFT JOIN configuracion.ambiente am ON am.id = s.ambiente_id
				LEFT JOIN academico.plan_competencia pco ON pco.plan_curso_id = pc.id
				WHERE ds.docente_id = ?::uuid
				GROUP BY cu.nombre, g.nombre, s.nombre, am.codigo, pc.horas_semanales""").param(MARIA_FERNANDEZ)
				.query().listOfRows();
		assertThat(clases).hasSize(1);
		assertThat(clases.get(0)).containsEntry("curso", "Álgebra")
				.containsEntry("grado", "1.° de secundaria")
				.containsEntry("seccion", "A")
				.containsEntry("aula", "A-101");
		assertThat(((Number) clases.get(0).get("competencias")).intValue()).isEqualTo(1);
	}

	@Test
	void elPlanDe1roDeSecundariaTiene16Cursos() {
		assertThat(jdbc.sql("""
				SELECT count(*) FROM academico.plan_curso WHERE anio_escolar_id = ?::uuid AND grado_codigo = 'SEC-1'""")
				.param(ANIO_2026).query(Integer.class).single()).isEqualTo(16);
	}

	@Test
	void noSePlanificaUnCursoEnUnGradoDondeNoSeDicta() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				SELECT ?::uuid, 'PRI-1', id, 3 FROM academico.curso WHERE nombre = 'Álgebra'""").param(ANIO_2026)
				.update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unaCompetenciaCualitativaNoUsaReglasNumericas() {
		assertThatThrownBy(() -> jdbc.sql("""
				UPDATE academico.plan_competencia SET escala = 'CUALITATIVA'
				WHERE regla_nota = 'PROMEDIO_PONDERADO'""").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void losRangosDeLaTablaDeConversionVanDe0A20() {
		assertThatThrownBy(() -> jdbc.sql("""
				UPDATE academico.tabla_conversion_nivel SET nota_hasta = 21 WHERE nivel = 'AD'""").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unAulaRegularAdmiteComoMaximo45() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo)
				VALUES ('A-999', 'Aula grande', 'AULA_REGULAR', 46)""").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void elCodigoDeUnAmbienteNoSeRepite() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo)
				VALUES ('LAB-01', 'Laboratorio de robótica', 'ESPACIO_ESPECIAL', 25)""").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
