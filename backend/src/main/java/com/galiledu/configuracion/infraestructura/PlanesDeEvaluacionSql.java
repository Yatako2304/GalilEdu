package com.galiledu.configuracion.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.configuracion.aplicacion.PlanesDeEvaluacion;

/**
 * Lee academico.plan_curso por SQL de solo lectura. No importa nada del módulo académico: cuando exista un servicio
 * equivalente, solo se reemplaza esta clase.
 */
@Component
public class PlanesDeEvaluacionSql implements PlanesDeEvaluacion {
	private final JdbcClient jdbc;

	public PlanesDeEvaluacionSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<GradoExcedido> gradoQueExcede(UUID anioEscolarId, String nivel, int bloquesSemanales) {
		// Las horas de un grado son la suma de las de sus cursos planificados, como valida Académico.
		return jdbc.sql("""
				SELECT g.nombre AS grado_nombre, sum(pc.horas_semanales) AS horas
				FROM academico.plan_curso pc
				JOIN configuracion.grado g ON g.codigo = pc.grado_codigo
				WHERE pc.anio_escolar_id = :anioEscolarId AND g.nivel = :nivel
				GROUP BY g.codigo, g.nombre, g.orden
				HAVING sum(pc.horas_semanales) > :bloquesSemanales
				ORDER BY sum(pc.horas_semanales) DESC, g.orden
				LIMIT 1""")
				.param("anioEscolarId", anioEscolarId).param("nivel", nivel).param("bloquesSemanales", bloquesSemanales)
				.query((resultado, fila) -> new GradoExcedido(resultado.getString("grado_nombre"),
						resultado.getInt("horas")))
				.optional();
	}

	@Override
	public boolean hayPlanes(UUID anioEscolarId, String nivel) {
		return jdbc.sql("""
				SELECT EXISTS (
				    SELECT 1 FROM academico.plan_curso pc
				    JOIN configuracion.grado g ON g.codigo = pc.grado_codigo
				    WHERE pc.anio_escolar_id = :anioEscolarId AND g.nivel = :nivel)""")
				.param("anioEscolarId", anioEscolarId).param("nivel", nivel)
				.query(Boolean.class).single();
	}
}
