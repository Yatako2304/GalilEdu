package com.galiledu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Ajustes de integración entre módulos (V7). */
class IntegracionModulosIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String MATEO = "20000000-0000-0000-0000-000000000001";
	private static final String VALERIA_USUARIO = "10000000-0000-0000-0000-000000000006";

	@Autowired
	private JdbcClient jdbc;

	@Test
	void elUsuarioEstudianteLlegaASuMatriculaSeccionYEstructuraHoraria() {
		// La cuenta y la estudiante son la misma persona; la sección usa la estructura principal de su nivel.
		var fila = jdbc.sql("""
				SELECT g.nombre AS grado, s.nombre AS seccion, m.codigo, eh.codigo AS horario
				FROM seguridad.usuario u
				JOIN estudiantes.estudiante e ON e.persona_id = u.persona_id
				JOIN matricula.matricula m ON m.estudiante_id = e.id
				JOIN configuracion.seccion s ON s.id = m.seccion_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.estructura_horaria eh
				     ON eh.anio_escolar_id = s.anio_escolar_id AND eh.nivel = g.nivel AND eh.es_principal
				WHERE u.id = ?::uuid""").param(VALERIA_USUARIO).query().singleRow();
		assertThat(fila).containsEntry("grado", "5.° de secundaria")
				.containsEntry("seccion", "A")
				.containsEntry("codigo", "MAT-2026-0002")
				.containsEntry("horario", "EH-SEC-26");
	}

	@Test
	void elGradoDelEstudianteEsDelCatalogoYYaNoHayInicial() {
		assertThatThrownBy(() -> jdbc.sql("UPDATE estudiantes.estudiante SET grado_codigo = 'INI-5' WHERE id = ?::uuid")
				.param(MATEO).update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void hayUnaSolaEstructuraPrincipalPorNivelYAnio() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria (anio_escolar_id, codigo, dias_lectivos, hora_entrada,
				                                              bloques_por_dia, duracion_bloque_min, nivel)
				VALUES (?::uuid, 'EH-SEC-OTRA', ARRAY['LUN'], '08:00', 6, 45, 'SECUNDARIA')""").param(ANIO_2026)
				.update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unaEstructuraNoPrincipalSiSePuedeAgregar() {
		// P. ej. "Secundaria · talleres de tarde" (Modal - Nueva estructura horaria).
		jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria (anio_escolar_id, codigo, dias_lectivos, hora_entrada,
				                                              bloques_por_dia, duracion_bloque_min, nivel, es_principal)
				VALUES (?::uuid, 'EH-TAR-26', ARRAY['LUN'], '14:45', 4, 45, 'SECUNDARIA', FALSE)""").param(ANIO_2026)
				.update();
	}

	@Test
	void elCodigoDeMatriculaTieneElFormatoMatAnioNumero() {
		assertThatThrownBy(() -> jdbc.sql("""
				UPDATE matricula.matricula SET codigo = 'MAT-2026-05A-0142'
				WHERE codigo = 'MAT-2026-0002'""").update()).isInstanceOf(DataIntegrityViolationException.class);
	}
}
