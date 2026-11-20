package com.galiledu.academico;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Ayudas de las pruebas de HU-60, HU-63, HU-64 y HU-69 sobre la demo compartida: 1.° A de secundaria 2026, María
 * Fernández (Álgebra, Aritmética y Geometría) y sus tres estudiantes. «Resuelve problemas de cantidad» la evalúan
 * Aritmética (60 %) y Geometría (40 %); el año usa promedio ponderado y promedio de periodos.
 */
final class DatosNotasPrueba {
	static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	static final String MARIA_FERNANDEZ = "10000000-0000-0000-0000-000000000004";
	static final String CANTIDAD = "Resuelve problemas de cantidad";

	private final JdbcClient jdbc;

	DatosNotasPrueba(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	String id(String sql, Object... params) {
		return jdbc.sql(sql).params(params).query(String.class).single();
	}

	String competencia(String nombre) {
		return id("SELECT id::text FROM academico.competencia WHERE nombre = ?", nombre);
	}

	String plan(String curso) {
		return id("""
				SELECT pc.id::text FROM academico.plan_curso pc JOIN academico.curso c ON c.id = pc.curso_id
				WHERE pc.anio_escolar_id = ?::uuid AND pc.grado_codigo = 'SEC-1' AND c.nombre = ?""", ANIO_2026,
				curso);
	}

	String seccionA() {
		return id("""
				SELECT id::text FROM configuracion.seccion
				WHERE anio_escolar_id = ?::uuid AND grado_codigo = 'SEC-1' AND nombre = 'A'""", ANIO_2026);
	}

	String periodo(int numero) {
		return id("""
				SELECT id::text FROM configuracion.periodo_academico
				WHERE anio_escolar_id = ?::uuid AND numero = ?""", ANIO_2026, numero);
	}

	String matriculaDe(String nombres) {
		return id("""
				SELECT m.id::text FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				WHERE m.seccion_id = ?::uuid AND p.nombres = ?""", seccionA(), nombres);
	}

	String estudianteDe(String nombres) {
		return id("""
				SELECT e.id::text FROM estudiantes.estudiante e JOIN personas.persona p ON p.id = e.persona_id
				WHERE p.nombres = ?""", nombres);
	}

	/** Un ítem del curso en el periodo que apunta solo a la competencia, con peso 100. */
	String item(String curso, int numeroPeriodo, String competenciaId, String nombre) {
		String item = id("""
				INSERT INTO academico.item_evaluacion
				    (plan_curso_id, anio_escolar_id, grado_codigo, seccion_id, periodo_academico_id,
				     nombre, descripcion, tipo, creado_por)
				VALUES (?::uuid, ?::uuid, 'SEC-1', ?::uuid, ?::uuid, ?, 'Ítem de la prueba', 'EXAMEN', ?::uuid)
				RETURNING id::text""", plan(curso), ANIO_2026, seccionA(), periodo(numeroPeriodo), nombre,
				MARIA_FERNANDEZ);
		jdbc.sql("""
				INSERT INTO academico.item_competencia (item_evaluacion_id, plan_curso_id, competencia_id,
				                                         peso_porcentaje)
				VALUES (?::uuid, ?::uuid, ?::uuid, 100)""").params(item, plan(curso), competenciaId).update();
		return item;
	}

	void nota(String item, String matriculaId, double nota) {
		jdbc.sql("""
				INSERT INTO academico.calificacion_item
				    (item_evaluacion_id, seccion_id, matricula_id, nota_numerica, registrado_por)
				VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?::uuid)""")
				.params(item, seccionA(), matriculaId, nota, MARIA_FERNANDEZ).update();
	}
}
