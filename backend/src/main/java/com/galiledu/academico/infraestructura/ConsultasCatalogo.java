package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Datos que el catálogo solo lee: el catálogo de grados y el año escolar activo (configuracion, V4), y los grados en
 * los que un curso ya está planificado (academico.plan_curso).
 */
@Component
public class ConsultasCatalogo {
	private final JdbcClient jdbc;

	public ConsultasCatalogo(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Códigos de grado en el orden del catálogo (PRI-1…PRI-6, SEC-1…SEC-5). */
	public List<String> codigosDeGrado() {
		return jdbc.sql("SELECT codigo FROM configuracion.grado ORDER BY orden").query(String.class).list();
	}

	/** Nombre del grado ("1.° de secundaria"). */
	public String nombreDeGrado(String codigo) {
		return jdbc.sql("SELECT nombre FROM configuracion.grado WHERE codigo = ?").param(codigo)
				.query(String.class).optional().orElse(codigo);
	}

	public Optional<Integer> anioActivo() {
		return jdbc.sql("SELECT anio FROM configuracion.anio_escolar WHERE estado = 'ACTIVO'")
				.query(Integer.class).optional();
	}

	/** Grados en los que el curso está en algún plan de evaluación: no se pueden quitar del curso. */
	public List<String> gradosPlanificados(UUID cursoId) {
		return jdbc.sql("SELECT DISTINCT grado_codigo FROM academico.plan_curso WHERE curso_id = ?")
				.param(cursoId).query(String.class).list();
	}
}
